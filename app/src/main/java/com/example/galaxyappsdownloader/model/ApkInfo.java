package com.example.galaxyappsdownloader.model;

/**
 * Model class representing APK information retrieved from Samsung servers.
 * Implements the Value Object pattern for immutable data representation.
 * Contains all relevant APK metadata for download operations.
 *
 * @author Samsung APK Downloader Team
 * @since Phase 2.0
 */
public class ApkInfo {

    private final String packageName;
    private final String versionCode;
    private final String versionName;
    private final String downloadUri;

    /**
     * Creates a new ApkInfo instance with the provided details.
     * All parameters are required and cannot be null.
     *
     * @param packageName The Android package name (e.g., "com.sec.android.app.myfiles")
     * @param versionCode The version code as string (e.g., "1150403081")
     * @param versionName The human-readable version name (e.g., "11.5.04.81")
     * @param downloadUri The direct download URI from Samsung servers
     * @throws IllegalArgumentException if any parameter is null or empty
     */
    public ApkInfo(String packageName, String versionCode, String versionName, String downloadUri) {
        if (packageName == null || packageName.trim().isEmpty()) {
            throw new IllegalArgumentException("Package name cannot be null or empty");
        }
        if (versionCode == null || versionCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Version code cannot be null or empty");
        }
        if (versionName == null || versionName.trim().isEmpty()) {
            throw new IllegalArgumentException("Version name cannot be null or empty");
        }
        if (downloadUri == null || downloadUri.trim().isEmpty()) {
            throw new IllegalArgumentException("Download URI cannot be null or empty");
        }

        this.packageName = packageName.trim();
        this.versionCode = versionCode.trim();
        this.versionName = versionName.trim();
        this.downloadUri = downloadUri.trim();
    }

    /**
     * Gets the Android package name.
     *
     * @return The package name (e.g., "com.sec.android.app.myfiles")
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * Gets the version code as string.
     * Version code is the internal version number used by Android.
     *
     * @return The version code (e.g., "1150403081")
     */
    public String getVersionCode() {
        return versionCode;
    }

    /**
     * Gets the human-readable version name.
     * This is the version string displayed to users.
     *
     * @return The version name (e.g., "11.5.04.81")
     */
    public String getVersionName() {
        return versionName;
    }

    /**
     * Gets the direct download URI from Samsung servers.
     * This URI is used to download the actual APK file.
     *
     * @return The download URI
     */
    public String getDownloadUri() {
        return downloadUri;
    }

    /**
     * Gets the version code as an integer.
     * Useful for version comparisons.
     *
     * @return Version code as integer, or -1 if parsing fails
     */
    public int getVersionCodeAsInt() {
        try {
            return Integer.parseInt(versionCode);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Gets the expected filename for this APK.
     * Follows the format: packageName-versionCode.apk
     *
     * @return Expected filename for the APK
     */
    public String getExpectedFilename() {
        return packageName + "-" + versionCode + ".apk";
    }

    /**
     * Checks if this APK info represents a valid downloadable APK.
     * Validates that all required fields are present and download URI is accessible.
     *
     * @return true if this represents a valid APK, false otherwise
     */
    public boolean isValid() {
        return !packageName.isEmpty() &&
                !versionCode.isEmpty() &&
                !versionName.isEmpty() &&
                !downloadUri.isEmpty() &&
                downloadUri.startsWith("http");
    }

    /**
     * Gets a human-readable display string for this APK.
     * Format: "Package Name v{versionName} ({versionCode})"
     *
     * @return Display string for the APK
     */
    public String getDisplayString() {
        return String.format("%s v%s (%s)", packageName, versionName, versionCode);
    }

    /**
     * Gets a short display string showing only version information.
     * Format: "v{versionName} ({versionCode})"
     *
     * @return Short version display string
     */
    public String getVersionDisplayString() {
        return String.format("v%s (%s)", versionName, versionCode);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ApkInfo apkInfo = (ApkInfo) obj;

        return packageName.equals(apkInfo.packageName) &&
                versionCode.equals(apkInfo.versionCode) &&
                versionName.equals(apkInfo.versionName) &&
                downloadUri.equals(apkInfo.downloadUri);
    }

    @Override
    public int hashCode() {
        int result = packageName.hashCode();
        result = 31 * result + versionCode.hashCode();
        result = 31 * result + versionName.hashCode();
        result = 31 * result + downloadUri.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ApkInfo{" +
                "packageName='" + packageName + '\'' +
                ", versionCode='" + versionCode + '\'' +
                ", versionName='" + versionName + '\'' +
                ", downloadUri='" + downloadUri + '\'' +
                '}';
    }
}