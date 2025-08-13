package com.example.galaxyappsdownloader;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.galaxyappsdownloader.data.PreferencesManager;
import com.example.galaxyappsdownloader.data.DeviceModelRepository;
import com.example.galaxyappsdownloader.network.DownloadCallback;
import com.example.galaxyappsdownloader.network.SamsungApiClient;
import com.example.galaxyappsdownloader.validation.InputValidator;
import com.example.galaxyappsdownloader.model.ApkInfo;

import java.util.List;

/**
 * Main activity for the Samsung APK Downloader application.
 * Implements Phase 2 enhanced features including smart input management,
 * validation, and persistent storage preferences.
 *
 * @author Samsung APK Downloader Team
 * @since Phase 2.0
 */
public class MainActivity extends AppCompatActivity implements DownloadCallback {

    // Request codes for permissions and activities
    private static final int REQUEST_STORAGE_PERMISSION = 1001;
    private static final int REQUEST_FOLDER_SELECTION = 1002;

    // UI Components
    private Spinner deviceModelSpinner;
    private EditText sdkVersionEditText;
    private EditText packageNameEditText;
    private Button downloadButton;
    private TextView statusTextView;
    private TextView storageLocationTextView;
    private ProgressBar progressBar;

    // Dependencies - following Dependency Injection principle
    private PreferencesManager preferencesManager;
    private DeviceModelRepository deviceModelRepository;
    private SamsungApiClient samsungApiClient;
    private InputValidator inputValidator;

    // State management
    private boolean isDownloading = false;
    private ApkInfo currentApkInfo;

    /**
     * Initializes the activity and sets up the user interface.
     * Implements first-launch setup flow as specified in Phase 2 requirements.
     *
     * @param savedInstanceState Previously saved instance state, or null if none exists
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeDependencies();
        initializeViews();
        setupEventListeners();

        // First launch setup - mandatory storage folder selection
        if (isFirstLaunch()) {
            showFirstLaunchSetup();
        } else {
            setupUserInterface();
        }
    }

    /**
     * Initializes dependency objects following the Dependency Injection principle.
     * This separation allows for easier testing and maintenance.
     */
    private void initializeDependencies() {
        preferencesManager = PreferencesManager.getInstance(this);
        deviceModelRepository = DeviceModelRepository.getInstance(this);
        samsungApiClient = new SamsungApiClient();
        inputValidator = new InputValidator();
    }

    /**
     * Binds UI components to their corresponding views.
     * Follows the Single Responsibility Principle by separating view binding logic.
     */
    private void initializeViews() {
        deviceModelSpinner = findViewById(R.id.deviceModelSpinner);
        sdkVersionEditText = findViewById(R.id.sdkVersionEditText);
        packageNameEditText = findViewById(R.id.packageNameEditText);
        downloadButton = findViewById(R.id.downloadButton);
        statusTextView = findViewById(R.id.statusTextView);
        storageLocationTextView = findViewById(R.id.storageLocationTextView);
        progressBar = findViewById(R.id.progressBar);
    }

    /**
     * Sets up event listeners for user interactions.
     * Implements real-time validation as specified in Phase 2 requirements.
     */
    private void setupEventListeners() {
        // Real-time SDK version validation
        sdkVersionEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateSdkVersionRealTime(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        downloadButton.setOnClickListener(v -> initiateDownload());

        storageLocationTextView.setOnClickListener(v -> showFolderSelectionDialog());
    }

    /**
     * Checks if this is the first launch of the application.
     *
     * @return true if first launch, false otherwise
     */
    private boolean isFirstLaunch() {
        return TextUtils.isEmpty(preferencesManager.getStorageLocation());
    }

    /**
     * Displays the mandatory first launch setup dialog.
     * Users cannot proceed without selecting a storage location.
     */
    private void showFirstLaunchSetup() {
        new AlertDialog.Builder(this)
                .setTitle("Welcome to Samsung APK Downloader")
                .setMessage("Please select a download folder to continue. This is required for the app to function.")
                .setCancelable(false)
                .setPositiveButton("Select Folder", (dialog, which) -> requestFolderSelection())
                .show();
    }

    /**
     * Sets up the main user interface after initial configuration.
     * Loads saved preferences and configures UI components.
     */
    private void setupUserInterface() {
        setupDeviceModelSpinner();
        loadStorageLocationDisplay();
        updateDownloadButtonState();
    }

    /**
     * Configures the device model spinner with saved models and "Add New" option.
     * Implements smart input management as specified in Phase 2.
     */
    private void setupDeviceModelSpinner() {
        List<String> savedModels = deviceModelRepository.getSavedModels();
        savedModels.add("Add New Model...");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, savedModels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        deviceModelSpinner.setAdapter(adapter);

        deviceModelSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                handleDeviceModelSelection(position, savedModels);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    /**
     * Handles device model selection from spinner.
     * Shows input dialog for "Add New Model" option.
     *
     * @param position Selected position in spinner
     * @param savedModels List of currently saved models
     */
    private void handleDeviceModelSelection(int position, List<String> savedModels) {
        if (position == savedModels.size() - 1) { // "Add New Model..." selected
            showAddNewModelDialog();
        }
        updateDownloadButtonState();
    }

    /**
     * Displays dialog for adding a new device model.
     * Validates input and saves to repository if valid.
     */
    private void showAddNewModelDialog() {
        EditText input = new EditText(this);
        input.setHint("SM-XXXXX");

        new AlertDialog.Builder(this)
                .setTitle("Add New Device Model")
                .setMessage("Enter device model in SM-XXXXX format:")
                .setView(input)
                .setPositiveButton("Add", (dialog, which) -> {
                    String newModel = input.getText().toString().trim().toUpperCase();
                    if (inputValidator.isValidDeviceModel(newModel)) {
                        deviceModelRepository.addModel(newModel);
                        setupDeviceModelSpinner(); // Refresh spinner
                        selectModelInSpinner(newModel);
                    } else {
                        Toast.makeText(this, "Invalid device model format. Use SM-XXXXX",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Selects a specific model in the spinner.
     *
     * @param model Model to select
     */
    private void selectModelInSpinner(String model) {
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) deviceModelSpinner.getAdapter();
        int position = adapter.getPosition(model);
        if (position >= 0) {
            deviceModelSpinner.setSelection(position);
        }
    }

    /**
     * Performs real-time validation for SDK version input.
     * Shows immediate feedback to user.
     *
     * @param sdkVersion The SDK version string to validate
     */
    private void validateSdkVersionRealTime(String sdkVersion) {
        if (TextUtils.isEmpty(sdkVersion)) {
            sdkVersionEditText.setError(null);
            return;
        }

        if (!inputValidator.isValidSdkVersion(sdkVersion)) {
            sdkVersionEditText.setError("SDK version must be a number between 19-34");
        } else {
            sdkVersionEditText.setError(null);
        }

        updateDownloadButtonState();
    }

    /**
     * Updates the download button state based on current input validation.
     * Enables button only when all inputs are valid.
     */
    private void updateDownloadButtonState() {
        boolean isValid = isCurrentInputValid();
        downloadButton.setEnabled(isValid && !isDownloading);
    }

    /**
     * Checks if current input is valid for download.
     *
     * @return true if all inputs are valid, false otherwise
     */
    private boolean isCurrentInputValid() {
        String selectedModel = getSelectedDeviceModel();
        String sdkVersion = sdkVersionEditText.getText().toString().trim();

        return !TextUtils.isEmpty(selectedModel) &&
                !selectedModel.equals("Add New Model...") &&
                inputValidator.isValidSdkVersion(sdkVersion) &&
                !TextUtils.isEmpty(preferencesManager.getStorageLocation());
    }

    /**
     * Gets the currently selected device model from spinner.
     *
     * @return Selected device model, or empty string if none selected
     */
    private String getSelectedDeviceModel() {
        Object selected = deviceModelSpinner.getSelectedItem();
        return selected != null ? selected.toString() : "";
    }

    /**
     * Initiates the APK download process with comprehensive validation.
     * Implements on-download validation as specified in Phase 2.
     */
    private void initiateDownload() {
        if (isDownloading) {
            return;
        }

        // Comprehensive validation before download
        String deviceModel = getSelectedDeviceModel();
        String sdkVersion = sdkVersionEditText.getText().toString().trim();
        String packageName = packageNameEditText.getText().toString().trim();

        // Validate all inputs
        if (!performComplexValidation(deviceModel, sdkVersion, packageName)) {
            return;
        }

        // Check permissions
        if (!hasStoragePermission()) {
            requestStoragePermission();
            return;
        }

        startDownload(deviceModel, sdkVersion, packageName);
    }

    /**
     * Performs complex validation that requires network connectivity and detailed checks.
     *
     * @param deviceModel The device model to validate
     * @param sdkVersion The SDK version to validate
     * @param packageName The package name to validate
     * @return true if all validations pass, false otherwise
     */
    private boolean performComplexValidation(String deviceModel, String sdkVersion, String packageName) {
        // Package name format validation
        if (!inputValidator.isValidPackageName(packageName)) {
            showError("Invalid package name format. Use com.company.app format.");
            return false;
        }

        // Network connectivity check
        if (!inputValidator.hasNetworkConnectivity(this)) {
            showError("No network connection available. Please check your internet connection.");
            return false;
        }

        // Storage location validation
        if (TextUtils.isEmpty(preferencesManager.getStorageLocation())) {
            showError("No download location selected. Please select a folder.");
            return false;
        }

        return true;
    }

    /**
     * Starts the actual download process.
     *
     * @param deviceModel The device model
     * @param sdkVersion The SDK version
     * @param packageName The package name
     */
    private void startDownload(String deviceModel, String sdkVersion, String packageName) {
        setDownloadingState(true);
        statusTextView.setText("Checking APK availability...");

        samsungApiClient.downloadApk(deviceModel, sdkVersion, packageName,
                preferencesManager.getStorageLocation(), this);
    }

    /**
     * Sets the downloading state and updates UI accordingly.
     *
     * @param downloading true if download is in progress, false otherwise
     */
    private void setDownloadingState(boolean downloading) {
        isDownloading = downloading;
        progressBar.setVisibility(downloading ? View.VISIBLE : View.GONE);
        downloadButton.setEnabled(!downloading && isCurrentInputValid());

        if (!downloading) {
            statusTextView.setText("Ready to download");
        }
    }

    /**
     * Displays error message to user.
     *
     * @param message Error message to display
     */
    private void showError(String message) {
        statusTextView.setText("Error: " + message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Loads and displays the current storage location.
     */
    private void loadStorageLocationDisplay() {
        String location = preferencesManager.getStorageLocation();
        if (!TextUtils.isEmpty(location)) {
            storageLocationTextView.setText("Download Location: " + getDisplayPath(location));
        } else {
            storageLocationTextView.setText("No download location selected");
        }
    }

    /**
     * Converts URI to human-readable display path.
     *
     * @param uriString The URI string to convert
     * @return Human-readable path
     */
    private String getDisplayPath(String uriString) {
        try {
            Uri uri = Uri.parse(uriString);
            String path = uri.getPath();
            if (path != null && path.contains("/tree/")) {
                return path.substring(path.lastIndexOf("/") + 1);
            }
            return "Custom folder";
        } catch (Exception e) {
            return "Custom folder";
        }
    }

    /**
     * Shows folder selection dialog with option to change current location.
     */
    private void showFolderSelectionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Download Location")
                .setMessage("Current: " + getDisplayPath(preferencesManager.getStorageLocation()) +
                        "\n\nDo you want to change the download location?")
                .setPositiveButton("Change Location", (dialog, which) -> requestFolderSelection())
                .setNegativeButton("Keep Current", null)
                .show();
    }

    /**
     * Requests folder selection from user using system folder picker.
     */
    private void requestFolderSelection() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI,
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toURI());
        startActivityForResult(intent, REQUEST_FOLDER_SELECTION);
    }

    /**
     * Checks if app has storage permission.
     *
     * @return true if permission granted, false otherwise
     */
    private boolean hasStoragePermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests storage permission from user.
     */
    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                REQUEST_STORAGE_PERMISSION);
    }

    // Activity result handling
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_FOLDER_SELECTION && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                // Persist permission
                getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                preferencesManager.setStorageLocation(uri.toString());
                loadStorageLocationDisplay();

                // If this was first launch, now setup the UI
                if (!preferencesManager.hasCompletedFirstLaunch()) {
                    preferencesManager.setFirstLaunchCompleted(true);
                    setupUserInterface();
                }

                updateDownloadButtonState();
                Toast.makeText(this, "Download location updated", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Permission handling
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, retry download if user was trying to download
                updateDownloadButtonState();
            } else {
                showError("Storage permission is required to download APK files.");
            }
        }
    }

    // Menu handling for settings
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // DownloadCallback implementation
    @Override
    public void onDownloadStarted(ApkInfo apkInfo) {
        runOnUiThread(() -> {
            currentApkInfo = apkInfo;
            statusTextView.setText(String.format("Downloading %s v%s...",
                    apkInfo.getVersionName(), apkInfo.getVersionCode()));
        });
    }

    @Override
    public void onDownloadProgress(int progress) {
        runOnUiThread(() -> {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                progressBar.setProgress(progress, true);
            } else {
                progressBar.setProgress(progress);
            }
        });
    }

    @Override
    public void onDownloadCompleted(String filePath) {
        runOnUiThread(() -> {
            setDownloadingState(false);
            statusTextView.setText("Download completed: " + filePath);
            Toast.makeText(this, "APK downloaded successfully!", Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void onDownloadFailed(String error) {
        runOnUiThread(() -> {
            setDownloadingState(false);
            showError(error);
        });
    }
}