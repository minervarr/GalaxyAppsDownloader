package com.example.galaxyappsdownloader;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main Activity for Samsung APK Downloader
 *
 * This activity provides a basic interface for downloading Samsung APK files
 * directly from Samsung servers. It replicates the functionality of the
 * Python script SamsungApkDownloader.py in Android.
 *
 * @author Your Name
 * @since 1.0
 */
public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final String SAMSUNG_API_URL_FORMAT =
            "https://vas.samsungapps.com/stub/stubDownload.as?appId=%s&deviceId=%s" +
                    "&mcc=425&mnc=01&csc=ILO&sdkVer=%s&pd=0&systemId=1608665720954" +
                    "&callerId=com.sec.android.app.samsungapps&abiType=64&extuk=0191d6627f38685f";

    // UI Components
    private EditText editTextDeviceModel;
    private EditText editTextSdkVersion;
    private EditText editTextPackageName;
    private Button buttonDownload;
    private TextView textViewStatus;
    private LinearLayout layoutVersionInfo;
    private TextView textViewVersionCode;
    private TextView textViewVersionName;
    private Button buttonConfirmDownload;

    // Business Logic Components
    private ExecutorService executorService;
    private SamsungApiClient apiClient;
    private String currentDownloadUrl;
    private String currentVersionCode;

    /**
     * Initializes the activity and sets up UI components
     *
     * @param savedInstanceState Bundle containing activity state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeComponents();
        setupEventHandlers();
        requestStoragePermission();
    }

    /**
     * Initializes UI components and business logic objects
     */
    private void initializeComponents() {
        // Initialize UI components
        editTextDeviceModel = findViewById(R.id.editTextDeviceModel);
        editTextSdkVersion = findViewById(R.id.editTextSdkVersion);
        editTextPackageName = findViewById(R.id.editTextPackageName);
        buttonDownload = findViewById(R.id.buttonDownload);
        textViewStatus = findViewById(R.id.textViewStatus);
        layoutVersionInfo = findViewById(R.id.layoutVersionInfo);
        textViewVersionCode = findViewById(R.id.textViewVersionCode);
        textViewVersionName = findViewById(R.id.textViewVersionName);
        buttonConfirmDownload = findViewById(R.id.buttonConfirmDownload);

        // Initialize business logic components
        executorService = Executors.newSingleThreadExecutor();
        apiClient = new SamsungApiClient();
    }

    /**
     * Sets up event handlers for UI interactions
     */
    private void setupEventHandlers() {
        buttonDownload.setOnClickListener(v -> handleDownloadRequest());
        buttonConfirmDownload.setOnClickListener(v -> handleConfirmDownload());
    }

    /**
     * Handles the initial download request by fetching APK information
     */
    private void handleDownloadRequest() {
        String deviceModel = editTextDeviceModel.getText().toString().trim();
        String sdkVersion = editTextSdkVersion.getText().toString().trim();
        String packageName = editTextPackageName.getText().toString().trim();

        if (deviceModel.isEmpty() || sdkVersion.isEmpty() || packageName.isEmpty()) {
            showStatus(getString(R.string.fill_all_fields));
            return;
        }

        showStatus(getString(R.string.fetching_apk_info));
        layoutVersionInfo.setVisibility(View.GONE);

        executorService.execute(() -> {
            try {
                String url = String.format(SAMSUNG_API_URL_FORMAT,
                        packageName, deviceModel.toUpperCase(), sdkVersion);

                ApkInfo apkInfo = apiClient.fetchApkInfo(url);

                runOnUiThread(() -> {
                    if (apkInfo.isValid()) {
                        displayApkInfo(apkInfo);
                    } else {
                        showStatus(getString(R.string.failed_prefix, apkInfo.getErrorMessage()));
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> showStatus(getString(R.string.error_prefix, e.getMessage())));
            }
        });
    }

    /**
     * Displays the fetched APK information and shows confirmation options
     *
     * @param apkInfo The APK information retrieved from Samsung servers
     */
    private void displayApkInfo(ApkInfo apkInfo) {
        textViewVersionCode.setText(getString(R.string.version_code_format, apkInfo.getVersionCode()));
        textViewVersionName.setText(getString(R.string.version_name_format, apkInfo.getVersionName()));

        currentDownloadUrl = apkInfo.getDownloadUrl();
        currentVersionCode = apkInfo.getVersionCode();

        layoutVersionInfo.setVisibility(View.VISIBLE);
        showStatus(getString(R.string.apk_info_retrieved));
    }

    /**
     * Handles the confirmed download action
     */
    private void handleConfirmDownload() {
        if (currentDownloadUrl == null || currentDownloadUrl.isEmpty()) {
            showStatus(getString(R.string.no_download_url));
            return;
        }

        // For modern Android versions, we can download directly
        if (hasStoragePermission()) {
            startApkDownload();
        } else {
            // Only request permission for older Android versions
            requestStoragePermission();
        }
    }

    /**
     * Initiates the actual APK file download using DownloadManager
     */
    private void startApkDownload() {
        try {
            String packageName = editTextPackageName.getText().toString().trim();
            String fileName = packageName + "-" + currentVersionCode + ".apk";

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(currentDownloadUrl));
            request.setTitle(getString(R.string.download_title));
            request.setDescription(getString(R.string.download_description, fileName));
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            long downloadId = downloadManager.enqueue(request);

            showStatus(getString(R.string.download_started));
            layoutVersionInfo.setVisibility(View.GONE);

        } catch (Exception e) {
            showStatus(getString(R.string.download_failed, e.getMessage()));
        }
    }

    /**
     * Updates the status text view with the provided message
     *
     * @param message The status message to display
     */
    private void showStatus(String message) {
        textViewStatus.setText(message);
    }

    /**
     * Checks if the app has storage permission
     * For Android 10+ (API 29+), we don't need WRITE_EXTERNAL_STORAGE for Downloads folder
     *
     * @return true if permission is granted or not needed, false otherwise
     */
    private boolean hasStoragePermission() {
        // For Android 10+ (API 29+), scoped storage allows writing to Downloads without permission
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            return true;
        }

        // For older versions, check WRITE_EXTERNAL_STORAGE permission
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests storage permission from the user
     * Only requests permission for Android versions that need it
     */
    private void requestStoragePermission() {
        // For Android 10+, we don't need to request WRITE_EXTERNAL_STORAGE for Downloads
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            Toast.makeText(this, "Storage permission not required for this Android version", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!hasStoragePermission()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * Handles the result of permission requests
     *
     * @param requestCode The request code passed to requestPermissions
     * @param permissions The requested permissions
     * @param grantResults The grant results for the corresponding permissions
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, getString(R.string.storage_permission_granted), Toast.LENGTH_SHORT).show();
                // Permission granted, proceed with download
                if (currentDownloadUrl != null && !currentDownloadUrl.isEmpty()) {
                    startApkDownload();
                }
            } else {
                Toast.makeText(this, getString(R.string.storage_permission_required), Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Cleans up resources when activity is destroyed
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}