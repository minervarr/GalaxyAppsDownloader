package com.example.galaxyappsdownloader.network;

import android.os.AsyncTask;

import com.example.galaxyappsdownloader.model.ApkInfo;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client class for communicating with Samsung's APK download servers.
 * Implements the network layer following Single Responsibility Principle.
 * Handles API communication, response parsing, and file downloading.
 *
 * @author Samsung APK Downloader Team
 * @since Phase 2.0
 */
public class SamsungApiClient {

    // Samsung API endpoint URL format (same as Python script)
    private static final String API_URL_FORMAT =
            "https://vas.samsungapps.com/stub/stubDownload.as?appId=%s&deviceId=%s" +
                    "&mcc=425&mnc=01&csc=ILO&sdkVer=%s&pd=0&systemId=1608665720954" +
                    "&callerId=com.sec.android.app.samsungapps&abiType=64&extuk=0191d6627f38685f";

    // Regex pattern for parsing Samsung API XML response (same as Python script)
    private static final String RESPONSE_PATTERN =
            "resultCode>([0-9]+)</resultCode>" +
                    ".*<resultMsg>([^']*)</resultMsg>" +
                    ".*<downloadURI><!\\[CDATA\\[([^']*)]></downloadURI>" +
                    ".*<versionCode>([0-9]+)</versionCode>" +
                    ".*<versionName>([^']*)</versionName>";

    // HTTP connection timeout settings
    private static final int CONNECTION_TIMEOUT = 10000; // 10 seconds
    private static final int READ_TIMEOUT = 30000; // 30 seconds

    // Download buffer size
    private static final int BUFFER_SIZE = 8192;

    /**
     * Downloads APK file from Samsung servers.
     * Performs the complete flow: API call, response parsing, and file download.
     *
     * @param deviceModel The Samsung device model (e.g., "SM-G970F")
     * @param sdkVersion The Android SDK version
     * @param packageName The package name to download
     * @param downloadPath The local path to save the APK file
     * @param callback Callback interface for progress and completion notifications
     */
    public void downloadApk(String deviceModel, String sdkVersion, String packageName,
                            String downloadPath, DownloadCallback callback) {

        new DownloadTask(deviceModel, sdkVersion, packageName, downloadPath, callback)
                .execute();
    }

    /**
     * AsyncTask implementation for handling APK download in background thread.
     * Prevents blocking the UI thread during network operations.
     */
    private static class DownloadTask extends AsyncTask<Void, Integer, DownloadResult> {

        private final String deviceModel;
        private final String sdkVersion;
        private final String packageName;
        private final String downloadPath;
        private final DownloadCallback callback;

        /**
         * Constructor for DownloadTask.
         *
         * @param deviceModel Samsung device model
         * @param sdkVersion Android SDK version
         * @param packageName Package name to download
         * @param downloadPath Local download path
         * @param callback Progress callback
         */
        public DownloadTask(String deviceModel, String sdkVersion, String packageName,
                            String downloadPath, DownloadCallback callback) {
            this.deviceModel = deviceModel;
            this.sdkVersion = sdkVersion;
            this.packageName = packageName;
            this.downloadPath = downloadPath;
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

                // Notify callback that download is starting
                if (callback != null) {
                    callback.onDownloadStarted(apkInfo);
                }

                // Step 2: Download the APK file
                String localFilePath = downloadApkFile(apkInfo);
                if (localFilePath != null) {
                    return DownloadResult.success(localFilePath);
                } else {
                    return DownloadResult.error("Failed to download APK file");
                }

            } catch (Exception e) {
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
                    callback.onDownloadCompleted(result.getFilePath());
                } else {
                    callback.onDownloadFailed(result.getErrorMessage());
                }
            }
        }

        /**
         * Queries Samsung API for APK information.
         *
         * @return ApkInfo object with download details, or null if failed
         * @throws IOException if network operation fails
         */
        private ApkInfo queryApkInfo() throws IOException {
            String apiUrl = String.format(API_URL_FORMAT, packageName,
                    deviceModel.toUpperCase(), sdkVersion);

            HttpURLConnection connection = null;
            try {
                URL url = new URL(apiUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(CONNECTION_TIMEOUT);
                connection.setReadTimeout(READ_TIMEOUT);
                connection.setRequestProperty("User-Agent", "SamsungApkDownloader/2.0");

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new IOException("HTTP error code: " + responseCode);
                }

                // Read response
                String response = readInputStream(connection.getInputStream());

                // Parse response using regex (same logic as Python script)
                return parseApiResponse(response);

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        /**
         * Parses Samsung API XML response to extract APK information.
         *
         * @param xmlResponse The XML response from Samsung API
         * @return ApkInfo object with parsed data, or null if parsing failed
         */
        private ApkInfo parseApiResponse(String xmlResponse) {
            Pattern pattern = Pattern.compile(RESPONSE_PATTERN, Pattern.MULTILINE | Pattern.DOTALL);
            Matcher matcher = pattern.matcher(xmlResponse);

            if (!matcher.find()) {
                // Try to extract error message from response
                Pattern errorPattern = Pattern.compile("resultMsg>(.*)</resultMsg>");
                Matcher errorMatcher = errorPattern.matcher(xmlResponse);
                if (errorMatcher.find()) {
                    String errorMsg = errorMatcher.group(1);
                    // This will be handled as null return, error logged in calling method
                }
                return null;
            }

            try {
                String resultCode = matcher.group(1);
                String resultMsg = matcher.group(2);
                String downloadUri = matcher.group(3);
                String versionCode = matcher.group(4);
                String versionName = matcher.group(5);

                // Check if Samsung API returned success
                if (!"1000".equals(resultCode)) {
                    // Samsung API returned an error
                    return null;
                }

                return new ApkInfo(packageName, versionCode, versionName, downloadUri);

            } catch (Exception e) {
                return null;
            }
        }

        /**
         * Downloads the APK file from the provided download URI.
         *
         * @param apkInfo APK information containing download URI
         * @return Local file path of downloaded APK, or null if failed
         */
        private String downloadApkFile(ApkInfo apkInfo) {
            HttpURLConnection connection = null;
            FileOutputStream fileOutput = null;
            InputStream inputStream = null;

            try {
                URL downloadUrl = new URL(apkInfo.getDownloadUri());
                connection = (HttpURLConnection) downloadUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(CONNECTION_TIMEOUT);
                connection.setReadTimeout(READ_TIMEOUT);

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    return null;
                }

                // Get file size for progress calculation
                int fileLength = connection.getContentLength();

                // Create local file path
                String fileName = packageName + "-" + apkInfo.getVersionCode() + ".apk";
                String localFilePath = downloadPath + "/" + fileName;

                // Setup streams
                inputStream = new BufferedInputStream(connection.getInputStream());
                fileOutput = new FileOutputStream(localFilePath);

                // Download with progress updates
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                int totalBytesRead = 0;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fileOutput.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;

                    // Update progress
                    if (fileLength > 0) {
                        int progress = (int) ((totalBytesRead * 100L) / fileLength);
                        publishProgress(progress);
                    }
                }

                fileOutput.flush();
                return localFilePath;

            } catch (Exception e) {
                return null;
            } finally {
                // Clean up resources
                if (fileOutput != null) {
                    try { fileOutput.close(); } catch (IOException ignored) {}
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
         * Reads an InputStream and returns its content as a String.
         *
         * @param inputStream The InputStream to read
         * @return String content of the stream
         * @throws IOException if reading fails
         */
        private String readInputStream(InputStream inputStream) throws IOException {
            StringBuilder result = new StringBuilder();
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                result.append(new String(buffer, 0, bytesRead, "UTF-8"));
            }

            return result.toString();
        }
    }

    /**
     * Result class for download operations.
     * Encapsulates success/failure state and associated data.
     */
    private static class DownloadResult {
        private final boolean success;
        private final String filePath;
        private final String errorMessage;

        private DownloadResult(boolean success, String filePath, String errorMessage) {
            this.success = success;
            this.filePath = filePath;
            this.errorMessage = errorMessage;
        }

        public static DownloadResult success(String filePath) {
            return new DownloadResult(true, filePath, null);
        }

        public static DownloadResult error(String errorMessage) {
            return new DownloadResult(false, null, errorMessage);
        }

        public boolean isSuccess() { return success; }
        public String getFilePath() { return filePath; }
        public String getErrorMessage() { return errorMessage; }
    }
}