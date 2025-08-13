package com.example.galaxyappsdownloader.network;

import com.example.galaxyappsdownloader.model.ApkInfo;

/**
 * Callback interface for download operations implementing the Observer pattern.
 * Allows decoupling between download logic and UI updates.
 * Follows the Interface Segregation Principle by providing specific download events.
 *
 * @author Samsung APK Downloader Team
 * @since Phase 2.0
 */
public interface DownloadCallback {

    /**
     * Called when download process starts.
     * Provides APK information retrieved from Samsung servers.
     *
     * @param apkInfo Information about the APK being downloaded
     */
    void onDownloadStarted(ApkInfo apkInfo);

    /**
     * Called to report download progress.
     * Progress is reported as percentage (0-100).
     *
     * @param progress Download progress percentage (0-100)
     */
    void onDownloadProgress(int progress);

    /**
     * Called when download completes successfully.
     * Provides the local file path where APK was saved.
     *
     * @param filePath Local file path of the downloaded APK
     */
    void onDownloadCompleted(String filePath);

    /**
     * Called when download fails.
     * Provides error message describing the failure reason.
     *
     * @param error Error message describing what went wrong
     */
    void onDownloadFailed(String error);
}