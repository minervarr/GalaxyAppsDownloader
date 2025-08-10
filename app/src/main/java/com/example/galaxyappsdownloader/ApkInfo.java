package com.example.galaxyappsdownloader;

/**
 * Data Transfer Object for APK information retrieved from Samsung servers
 *
 * This class encapsulates all relevant information about a Samsung APK
 * including download URL, version details, and error handling.
 * Follows the Value Object pattern for immutable data representation.
 *
 * @author Your Name
 * @since 1.0
 */
public class ApkInfo {

    private final boolean valid;
    private final String downloadUrl;
    private final String versionCode;
    private final String versionName;
    private final String errorMessage;

    /**
     * Private constructor to enforce factory method usage
     *
     * @param valid true if the APK info is valid, false if error occurred
     * @param downloadUrl Direct download URL for the APK file
     * @param versionCode Numeric version code from Samsung
     * @param versionName Human-readable version name
     * @param errorMessage Error message if operation failed
     */
    private ApkInfo(boolean valid, String downloadUrl, String versionCode,
                    String versionName, String errorMessage) {
        this.valid = valid;
        this.downloadUrl = downloadUrl;
        this.versionCode = versionCode;
        this.versionName = versionName;
        this.errorMessage = errorMessage;
    }

    /**
     * Factory method to create a valid ApkInfo instance
     *
     * @param downloadUrl The direct download URL for the APK
     * @param versionCode The numeric version code
     * @param versionName The human-readable version name
     * @return A valid ApkInfo instance
     */
    public static ApkInfo createValid(String downloadUrl, String versionCode, String versionName) {
        if (downloadUrl == null || downloadUrl.trim().isEmpty()) {
            return createError("Download URL is empty");
        }
        if (versionCode == null || versionCode.trim().isEmpty()) {
            return createError("Version code is empty");
        }
        if (versionName == null || versionName.trim().isEmpty()) {
            return createError("Version name is empty");
        }

        return new ApkInfo(true, downloadUrl.trim(), versionCode.trim(), versionName.trim(), null);
    }

    /**
     * Factory method to create an error ApkInfo instance
     *
     * @param errorMessage The error message describing what went wrong
     * @return An error ApkInfo instance
     */
    public static ApkInfo createError(String errorMessage) {
        String message = (errorMessage != null && !errorMessage.trim().isEmpty())
                ? errorMessage.trim()
                : "Unknown error occurred";

        return new ApkInfo(false, null, null, null, message);
    }

    /**
     * Checks if this ApkInfo represents valid APK data
     *
     * @return true if the APK information is valid and can be downloaded
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Gets the direct download URL for the APK file
     *
     * @return The download URL, or null if this is an error instance
     */
    public String getDownloadUrl() {
        return downloadUrl;
    }

    /**
     * Gets the numeric version code from Samsung servers
     *
     * @return The version code, or null if this is an error instance
     */
    public String getVersionCode() {
        return versionCode;
    }

    /**
     * Gets the human-readable version name
     *
     * @return The version name, or null if this is an error instance
     */
    public String getVersionName() {
        return versionName;
    }

    /**
     * Gets the error message if this represents an error state
     *
     * @return The error message, or null if this is a valid instance
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Returns a string representation of this ApkInfo
     *
     * @return String representation for debugging purposes
     */
    @Override
    public String toString() {
        if (valid) {
            return String.format("ApkInfo{valid=true, versionCode='%s', versionName='%s', downloadUrl='%s'}",
                    versionCode, versionName, downloadUrl);
        } else {
            return String.format("ApkInfo{valid=false, errorMessage='%s'}", errorMessage);
        }
    }

    /**
     * Checks equality based on all fields
     *
     * @param obj The object to compare with
     * @return true if objects are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ApkInfo apkInfo = (ApkInfo) obj;

        if (valid != apkInfo.valid) return false;
        if (downloadUrl != null ? !downloadUrl.equals(apkInfo.downloadUrl) : apkInfo.downloadUrl != null) return false;
        if (versionCode != null ? !versionCode.equals(apkInfo.versionCode) : apkInfo.versionCode != null) return false;
        if (versionName != null ? !versionName.equals(apkInfo.versionName) : apkInfo.versionName != null) return false;
        return errorMessage != null ? errorMessage.equals(apkInfo.errorMessage) : apkInfo.errorMessage == null;
    }

    /**
     * Generates hash code based on all fields
     *
     * @return Hash code for this object
     */
    @Override
    public int hashCode() {
        int result = (valid ? 1 : 0);
        result = 31 * result + (downloadUrl != null ? downloadUrl.hashCode() : 0);
        result = 31 * result + (versionCode != null ? versionCode.hashCode() : 0);
        result = 31 * result + (versionName != null ? versionName.hashCode() : 0);
        result = 31 * result + (errorMessage != null ? errorMessage.hashCode() : 0);
        return result;
    }
}