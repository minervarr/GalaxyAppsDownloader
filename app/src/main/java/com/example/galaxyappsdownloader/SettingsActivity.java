package com.example.galaxyappsdownloader;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.galaxyappsdownloader.data.DeviceModelRepository;
import com.example.galaxyappsdownloader.data.PreferencesManager;

import java.util.List;

/**
 * Settings activity for managing application preferences and data.
 * Provides user interface for managing saved device models and storage location.
 * Follows Single Responsibility Principle by handling only settings management.
 *
 * @author Samsung APK Downloader Team
 * @since Phase 2.0
 */
public class SettingsActivity extends AppCompatActivity {

    // Request code for folder selection
    private static final int REQUEST_FOLDER_SELECTION = 2001;

    // UI Components
    private TextView storageLocationTextView;
    private ListView savedModelsListView;
    private Button changeStorageButton;
    private Button addModelButton;
    private Button resetModelsButton;
    private Button clearDataButton;

    // Dependencies
    private PreferencesManager preferencesManager;
    private DeviceModelRepository deviceModelRepository;

    // Data
    private ArrayAdapter<String> modelsAdapter;
    private List<String> savedModels;

    /**
     * Initializes the settings activity and sets up the user interface.
     *
     * @param savedInstanceState Previously saved instance state, or null if none exists
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setupToolbar();
        initializeDependencies();
        initializeViews();
        setupEventListeners();
        loadCurrentSettings();
    }

    /**
     * Sets up the toolbar with back navigation.
     */
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }
    }

    /**
     * Initializes dependency objects.
     */
    private void initializeDependencies() {
        preferencesManager = PreferencesManager.getInstance(this);
        deviceModelRepository = DeviceModelRepository.getInstance(this);
    }

    /**
     * Binds UI components to their corresponding views.
     */
    private void initializeViews() {
        storageLocationTextView = findViewById(R.id.storageLocationTextView);
        savedModelsListView = findViewById(R.id.savedModelsListView);
        changeStorageButton = findViewById(R.id.changeStorageButton);
        addModelButton = findViewById(R.id.addModelButton);
        resetModelsButton = findViewById(R.id.resetModelsButton);
        clearDataButton = findViewById(R.id.clearDataButton);
    }

    /**
     * Sets up event listeners for user interactions.
     */
    private void setupEventListeners() {
        changeStorageButton.setOnClickListener(v -> requestFolderSelection());
        addModelButton.setOnClickListener(v -> showAddModelDialog());
        resetModelsButton.setOnClickListener(v -> showResetModelsDialog());
        clearDataButton.setOnClickListener(v -> showClearDataDialog());

        // Long click on model items to delete
        savedModelsListView.setOnItemLongClickListener((parent, view, position, id) -> {
            showDeleteModelDialog(savedModels.get(position));
            return true;
        });
    }

    /**
     * Loads and displays current settings.
     */
    private void loadCurrentSettings() {
        loadStorageLocationDisplay();
        loadSavedModels();
    }

    /**
     * Loads and displays the current storage location.
     */
    private void loadStorageLocationDisplay() {
        String location = preferencesManager.getStorageLocation();
        if (location != null && !location.isEmpty()) {
            String displayPath = getDisplayPath(location);
            storageLocationTextView.setText("Current: " + displayPath);
        } else {
            storageLocationTextView.setText("No location selected");
        }
    }

    /**
     * Loads saved device models into the list view.
     */
    private void loadSavedModels() {
        savedModels = deviceModelRepository.getSavedModels();
        modelsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, savedModels);
        savedModelsListView.setAdapter(modelsAdapter);

        // Show instruction if no models
        if (savedModels.isEmpty()) {
            showEmptyModelsMessage();
        }
    }

    /**
     * Shows a message when no models are saved.
     */
    private void showEmptyModelsMessage() {
        Toast.makeText(this, "No saved device models. Add some using the + button.",
                Toast.LENGTH_LONG).show();
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
     * Requests folder selection from user using system folder picker.
     */
    private void requestFolderSelection() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI,
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toURI());
        startActivityForResult(intent, REQUEST_FOLDER_SELECTION);
    }

    /**
     * Shows dialog for adding a new device model.
     */
    private void showAddModelDialog() {
        AddModelDialogFragment dialog = new AddModelDialogFragment();
        dialog.setOnModelAddedListener(this::onModelAdded);
        dialog.show(getSupportFragmentManager(), "AddModelDialog");
    }

    /**
     * Callback for when a new model is added.
     *
     * @param deviceModel The newly added device model
     */
    private void onModelAdded(String deviceModel) {
        boolean added = deviceModelRepository.addModel(deviceModel);
        if (added) {
            loadSavedModels(); // Refresh the list
            Toast.makeText(this, "Model added: " + deviceModel, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Model already exists or invalid format", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Shows confirmation dialog for resetting device models to defaults.
     */
    private void showResetModelsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Reset Device Models")
                .setMessage("This will remove all your saved device models and restore the default ones. Continue?")
                .setPositiveButton("Reset", (dialog, which) -> {
                    deviceModelRepository.resetToDefaults();
                    loadSavedModels();
                    Toast.makeText(this, "Device models reset to defaults", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Shows confirmation dialog for deleting a specific device model.
     *
     * @param deviceModel The device model to delete
     */
    private void showDeleteModelDialog(String deviceModel) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Device Model")
                .setMessage("Delete " + deviceModel + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    boolean removed = deviceModelRepository.removeModel(deviceModel);
                    if (removed) {
                        loadSavedModels();
                        Toast.makeText(this, "Model deleted: " + deviceModel, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Shows confirmation dialog for clearing all application data.
     */
    private void showClearDataDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Clear All Data")
                .setMessage("This will remove all saved preferences, device models, and settings. " +
                        "You will need to set up the app again. Continue?")
                .setPositiveButton("Clear All", (dialog, which) -> {
                    clearAllData();
                    Toast.makeText(this, "All data cleared", Toast.LENGTH_SHORT).show();
                    finish(); // Return to main activity
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Clears all application data and resets to initial state.
     */
    private void clearAllData() {
        preferencesManager.clearAllPreferences();
        deviceModelRepository.clearAllModels();

        // Refresh display
        loadCurrentSettings();
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
                Toast.makeText(this, "Download location updated", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Toolbar back button handling
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}