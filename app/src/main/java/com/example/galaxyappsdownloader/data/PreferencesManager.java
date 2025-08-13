package com.example.galaxyappsdownloader.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Singleton class for managing application preferences and persistent data.
 * Implements the Singleton pattern to ensure single instance across the application.
 * Follows the Single Responsibility Principle by handling only preference management.
 *
 * @author Samsung APK Downloader Team
 * @since Phase 2.0
 */
public class PreferencesManager {

    // Singleton instance
    private static PreferencesManager instance;

    // SharedPreferences keys
    private static final String PREF_NAME = "SamsungApkDownloaderPrefs";
    private static final String KEY_STORAGE_LOCATION = "storage_location";
    private static final String KEY_FIRST_LAUNCH_COMPLETED = "first_launch_completed";
    private static final String KEY_LAST_DEVICE_MODEL = "last_device_model";
    private static final String KEY_LAST_SDK_VERSION = "last_sdk_version";

    private final SharedPreferences preferences;

    /**
     * Private constructor to prevent direct instantiation.
     * Follows Singleton pattern implementation.
     *
     * @param context Application context for accessing SharedPreferences
     */
    private PreferencesManager(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Gets the singleton instance of PreferencesManager.
     * Thread-safe implementation using double-checked locking.
     *
     * @param context Application context
     * @return Singleton instance of PreferencesManager
     */
    public static synchronized PreferencesManager getInstance(Context context) {
        if (instance == null) {
            instance = new PreferencesManager(context);
        }
        return instance;
    }

    /**
     * Sets the user-selected storage location for APK downloads.
     *
     * @param uriString The storage location URI as string
     */
    public void setStorageLocation(String uriString) {
        preferences.edit()
                .putString(KEY_STORAGE_LOCATION, uriString)
                .apply();
    }

    /**
     * Gets the currently configured storage location.
     *
     * @return Storage location URI string, or null if not set
     */
    public String getStorageLocation() {
        return preferences.getString(KEY_STORAGE_LOCATION, null);
    }

    /**
     * Marks the first launch setup as completed.
     *
     * @param completed true if first launch setup is completed
     */
    public void setFirstLaunchCompleted(boolean completed) {
        preferences.edit()
                .putBoolean(KEY_FIRST_LAUNCH_COMPLETED, completed)
                .apply();
    }

    /**
     * Checks if the first launch setup has been completed.
     *
     * @return true if first launch setup is completed, false otherwise
     */
    public boolean hasCompletedFirstLaunch() {
        return preferences.getBoolean(KEY_FIRST_LAUNCH_COMPLETED, false);
    }

    /**
     * Saves the last used device model for convenience.
     *
     * @param deviceModel The device model to save
     */
    public void setLastDeviceModel(String deviceModel) {
        preferences.edit()
                .putString(KEY_LAST_DEVICE_MODEL, deviceModel)
                .apply();
    }

    /**
     * Gets the last used device model.
     *
     * @return Last used device model, or null if none saved
     */
    public String getLastDeviceModel() {
        return preferences.getString(KEY_LAST_DEVICE_MODEL, null);
    }

    /**
     * Saves the last used SDK version for convenience.
     *
     * @param sdkVersion The SDK version to save
     */
    public void setLastSdkVersion(String sdkVersion) {
        preferences.edit()
                .putString(KEY_LAST_SDK_VERSION, sdkVersion)
                .apply();
    }

    /**
     * Gets the last used SDK version.
     *
     * @return Last used SDK version, or empty string if none saved
     */
    public String getLastSdkVersion() {
        return preferences.getString(KEY_LAST_SDK_VERSION, "");
    }

    /**
     * Clears all stored preferences.
     * Used for reset functionality or testing.
     */
    public void clearAllPreferences() {
        preferences.edit().clear().apply();
    }

    /**
     * Checks if storage location has been configured.
     *
     * @return true if storage location is set, false otherwise
     */
    public boolean hasStorageLocation() {
        String location = getStorageLocation();
        return location != null && !location.trim().isEmpty();
    }
}