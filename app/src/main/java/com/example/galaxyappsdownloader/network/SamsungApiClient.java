package com.example.galaxyappsdownloader.network;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import com.example.galaxyappsdownloader.model.ApkInfo;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client class for communicating with Samsung's APK download servers.
 * Handles API communication, response parsing, and file downloading to custom locations.
 *
 * @author Samsung APK Downloader Team
 * @since Phase 2.0
 */
public class SamsungApiClient {

    private static final String TAG = "SamsungApiClient";

    // Samsung API endpoint URL format (same as Python script)
    private static final String API_URL_FORMAT =
            "https://vas.samsungapps.com/stub/stubDownload.as?appId=%s&deviceId=%s" +
                    "&mcc=425&mnc=01&csc=ILO&sdkVer=%s&pd=0&systemId=1608665720954" +
                    "&callerId=com.sec.android.app.samsungapps&abiType=64&extuk=0191d6627f38685f";

    // Regex pattern for parsing Samsung API XML response
    private static final String RESPONSE_PATTERN =
            "resultCode>([0-9]+)</resultCode>" +
                    ".*?<resultMsg>([^<]*)</resultMsg>" +
                    ".*?<downloadURI><!\\[CDATA\\[([^\\]]*)]></downloadURI>" +
                    ".*?<versionCode>([0-9]+)</versionCode>" +
                    ".*?<versionName>([^<]*)</versionName>";

    // HTTP connection timeout settings
    private static final int CONNECTION_TIMEOUT = 15000; // 15 seconds
    private static final int READ_TIMEOUT = 30000; // 30 seconds
    private static final int BUFFER_SIZE = 8192;

    private final Context context;
    private String userDownloadPath;

    /**
     * Constructor for SamsungApiClient.
     *
     * @param context Application context
     */
    public SamsungApiClient(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Downloads APK file from Samsung servers to user's chosen location.
     *
     * @param deviceModel The Samsung device model (e.g., "SM-G970F")
     * @param sdkVersion The Android SDK version
     * @param packageName The package name to download
     * @param downloadPath The user's chosen download path URI
     * @param callback Callback interface for progress and completion notifications
     */
    public void downloadApk(String deviceModel, String sdkVersion, String packageName,
                            String downloadPath, DownloadCallback callback) {

        this.userDownloadPath = downloadPath;
        Log.d(TAG, "Starting download process for: " + packageName);
        Log.d(TAG, "User download path: " + downloadPath);

        new DownloadTask(deviceModel, sdkVersion, packageName, callback).execute();
    }

    /**
     * Main download task that handles both API query and file download.
     */
    private class DownloadTask extends AsyncTask<Void, Integer, DownloadResult> {

        private final String deviceModel;
        private final String sdkVersion;
        private final String packageName;
        private final DownloadCallback callback;

        public DownloadTask(String deviceModel, String sdkVersion, String packageName,
                            DownloadCallback callback) {
            this.deviceModel = deviceModel;
            this.sdkVersion = sdkVersion;
            this.packageName = packageName;
            this.callback = callback;
        }

        @Override
        protected DownloadResult doInBackground(Void... voids) {
            try {
                // Step 1: Query Samsung API for APK information
                ApkInfo apkInfo = queryApkInfo();
                if (apkInfo == null) {
                    return DownloadResult.error("Failed to get APK information from Samsung servers");
                }

                Log.d(TAG, "APK info retrieved: " + apkInfo.toString());

                // Step 2: Download the APK file
                String localFilePath = downloadApkFile(apkInfo);
                if (localFilePath != null) {
                    return DownloadResult.success(apkInfo, localFilePath);
                } else {
                    return DownloadResult.error("Failed to download APK file");
                }

            } catch (Exception e) {
                Log.e(TAG, "Download failed with exception", e);
                return DownloadResult.error("Download failed: " + e.getMessage());
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (callback != null && values.length > 0) {
                callback.onDownloadProgress(values[0]);
            }
        }

        @Override
        protected void onPostExecute(DownloadResult result) {
            if (callback != null) {
                if (result.isSuccess()) {
                    callback.onDownloadStarted(result.getApkInfo());
                    callback.onDownloadCompleted(result.getFilePath());
                } else {
                    callback.onDownloadFailed(result.getErrorMessage());
                }
            }
        }

        /**
         * Queries Samsung API for APK information.
         */
        private ApkInfo queryApkInfo() throws IOException {
            String apiUrl = String.format(API_URL_FORMAT, packageName,
                    deviceModel.toUpperCase(), sdkVersion);

            Log.d(TAG, "Samsung API URL: " + apiUrl);

            HttpURLConnection connection = null;
            try {
                URL url = new URL(apiUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(CONNECTION_TIMEOUT);
                connection.setReadTimeout(READ_TIMEOUT);

                connection.setRequestProperty("User-Agent", "SamsungApkDownloader/2.0");
                connection.setRequestProperty("Accept", "*/*");

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Samsung API response code: " + responseCode);

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new IOException("HTTP error code: " + responseCode);
                }

                String response = readInputStream(connection.getInputStream());
                Log.d(TAG, "Response length: " + response.length());

                return parseApiResponse(response);

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        /**
         * Parses Samsung API XML response.
         */
        private ApkInfo parseApiResponse(String xmlResponse) {
            try {
                // Extract fields individually for robustness
                String resultCode = extractXmlField(xmlResponse, "resultCode");
                String resultMsg = extractXmlField(xmlResponse, "resultMsg");
                String downloadUri = extractCDataField(xmlResponse, "downloadURI");
                String versionCode = extractXmlField(xmlResponse, "versionCode");
                String versionName = extractXmlField(xmlResponse, "versionName");

                Log.d(TAG, "Parsed response - Code: " + resultCode + ", Message: " + resultMsg);

                // Accept both "1" and "1000" as success codes
                if (resultCode == null || (!resultCode.equals("1") && !resultCode.equals("1000"))) {
                    Log.e(TAG, "Samsung API error - Code: " + resultCode + ", Message: " + resultMsg);
                    return null;
                }

                if (downloadUri == null || downloadUri.trim().isEmpty()) {
                    Log.e(TAG, "Download URI is empty");
                    return null;
                }

                if (versionCode == null || versionCode.trim().isEmpty()) {
                    Log.e(TAG, "Version code is empty");
                    return null;
                }

                if (versionName == null || versionName.trim().isEmpty()) {
                    versionName = "Unknown";
                }

                return new ApkInfo(packageName, versionCode.trim(), versionName.trim(), downloadUri.trim());

            } catch (Exception e) {
                Log.e(TAG, "Error parsing Samsung API response", e);
                return null;
            }
        }

        /**
         * Downloads the APK file to user's chosen location.
         */
        private String downloadApkFile(ApkInfo apkInfo) {
            HttpURLConnection connection = null;
            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                Log.d(TAG, "Starting APK download from: " + apkInfo.getDownloadUri());

                URL downloadUrl = new URL(apkInfo.getDownloadUri());
                connection = (HttpURLConnection) downloadUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(CONNECTION_TIMEOUT);
                connection.setReadTimeout(READ_TIMEOUT);

                connection.setRequestProperty("User-Agent", "SamsungApkDownloader/2.0");

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "Download failed with HTTP code: " + responseCode);
                    return null;
                }

                int fileLength = connection.getContentLength();
                Log.d(TAG, "File size: " + fileLength + " bytes");

                String fileName = apkInfo.getExpectedFilename();
                String outputPath = createOutputFile(fileName);

                if (outputPath == null) {
                    Log.e(TAG, "Failed to create output file");
                    return null;
                }

                // Open streams
                inputStream = new BufferedInputStream(connection.getInputStream());

                if (outputPath.startsWith("content://")) {
                    // Use ContentResolver for DocumentFile
                    outputStream = context.getContentResolver().openOutputStream(Uri.parse(outputPath));
                } else {
                    // Use FileOutputStream for regular files
                    outputStream = new FileOutputStream(outputPath);
                }

                if (outputStream == null) {
                    Log.e(TAG, "Failed to open output stream");
                    return null;
                }

                // Download with progress
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                int totalBytesRead = 0;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;

                    if (fileLength > 0) {
                        int progress = (int) ((totalBytesRead * 100L) / fileLength);
                        publishProgress(progress);
                    }
                }

                outputStream.flush();
                Log.d(TAG, "Download completed: " + outputPath);
                return outputPath;

            } catch (Exception e) {
                Log.e(TAG, "Error downloading APK", e);
                return null;
            } finally {
                if (outputStream != null) {
                    try { outputStream.close(); } catch (IOException ignored) {}
                }
                if (inputStream != null) {
                    try { inputStream.close(); } catch (IOException ignored) {}
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        /**
         * Creates output file in user's chosen location.
         */
        private String createOutputFile(String fileName) {
            try {
                // Try user's chosen location first
                if (userDownloadPath != null && !userDownloadPath.isEmpty() &&
                        userDownloadPath.startsWith("content://")) {

                    Uri treeUri = Uri.parse(userDownloadPath);
                    DocumentFile documentDir = DocumentFile.fromTreeUri(context, treeUri);

                    if (documentDir != null && documentDir.exists() && documentDir.canWrite()) {
                        DocumentFile apkFile = documentDir.createFile("application/vnd.android.package-archive", fileName);
                        if (apkFile != null) {
                            Log.d(TAG, "Created file in user location: " + apkFile.getUri());
                            return apkFile.getUri().toString();
                        }
                    }

                    Log.w(TAG, "Failed to create file in user location, falling back to Downloads");
                }

                // Fallback to Downloads folder
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File outputFile = new File(downloadsDir, fileName);
                Log.d(TAG, "Using Downloads folder: " + outputFile.getAbsolutePath());
                return outputFile.getAbsolutePath();

            } catch (Exception e) {
                Log.e(TAG, "Error creating output file", e);
                return null;
            }
        }

        /**
         * Reads InputStream to String.
         */
        private String readInputStream(InputStream inputStream) throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            StringBuilder result = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }

            return result.toString();
        }

        /**
         * Extracts simple XML field.
         */
        private String extractXmlField(String xml, String fieldName) {
            Pattern pattern = Pattern.compile("<" + fieldName + ">([^<]*)</" + fieldName + ">");
            Matcher matcher = pattern.matcher(xml);
            return matcher.find() ? matcher.group(1) : null;
        }

        /**
         * Extracts CDATA field.
         */
        private String extractCDataField(String xml, String fieldName) {
            Pattern pattern = Pattern.compile("<" + fieldName + "><!\\[CDATA\\[([^\\]]*)\\]\\]></" + fieldName + ">");
            Matcher matcher = pattern.matcher(xml);
            if (matcher.find()) {
                return matcher.group(1);
            }
            return extractXmlField(xml, fieldName);
        }
    }

    /**
     * Result class for download operations.
     */
    private static class DownloadResult {
        private final boolean success;
        private final ApkInfo apkInfo;
        private final String filePath;
        private final String errorMessage;

        private DownloadResult(boolean success, ApkInfo apkInfo, String filePath, String errorMessage) {
            this.success = success;
            this.apkInfo = apkInfo;
            this.filePath = filePath;
            this.errorMessage = errorMessage;
        }

        public static DownloadResult success(ApkInfo apkInfo, String filePath) {
            return new DownloadResult(true, apkInfo, filePath, null);
        }

        public static DownloadResult error(String errorMessage) {
            return new DownloadResult(false, null, null, errorMessage);
        }

        public boolean isSuccess() { return success; }
        public ApkInfo getApkInfo() { return apkInfo; }
        public String getFilePath() { return filePath; }
        public String getErrorMessage() { return errorMessage; }
    }
}