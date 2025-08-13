package com.example.galaxyappsdownloader.validation;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;

/**
 * Utility class for input validation following the Single Responsibility Principle.
 * Provides both real-time and complex validation methods as specified in Phase 2.
 *
 * @author Samsung APK Downloader Team
 * @since Phase 2.0
 */
public class InputValidator {

    // Android SDK version bounds for validation - removed upper limit
    private static final int MIN_SDK_VERSION = 19; // Android 4.4 KitKat
    private static final int MAX_SDK_VERSION = 99; // Future-proof upper limit

    // Samsung device model pattern
    private static final String SAMSUNG_MODEL_PATTERN = "^SM-[A-Z0-9]{4,6}[A-Z]?$";

    // Package name pattern (standard Android package naming)
    private static final String PACKAGE_NAME_PATTERN = "^[a-z][a-z0-9_]*(\\.[a-z0-9_]+)+[0-9a-z_]$";

    /**
     * Validates SDK version for real-time feedback.
     * Used for immediate user feedback during typing.
     *
     * @param sdkVersionString The SDK version string to validate
     * @return true if valid SDK version, false otherwise
     */
    public boolean isValidSdkVersion(String sdkVersionString) {
        if (TextUtils.isEmpty(sdkVersionString)) {
            return false;
        }

        try {
            int sdkVersion = Integer.parseInt(sdkVersionString.trim());
            return sdkVersion >= MIN_SDK_VERSION && sdkVersion <= MAX_SDK_VERSION;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validates device model format for Samsung devices.
     * Checks against Samsung's standard naming convention.
     *
     * @param deviceModel The device model to validate
     * @return true if valid Samsung device model format, false otherwise
     */
    public boolean isValidDeviceModel(String deviceModel) {
        if (TextUtils.isEmpty(deviceModel)) {
            return false;
        }

        String model = deviceModel.trim().toUpperCase();
        return model.matches(SAMSUNG_MODEL_PATTERN);
    }

    /**
     * Validates Android package name format.
     * Used for complex validation before download attempt.
     *
     * @param packageName The package name to validate
     * @return true if valid package name format, false otherwise
     */
    public boolean isValidPackageName(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }

        String pkg = packageName.trim().toLowerCase();

        // Check basic format requirements
        if (!pkg.matches(PACKAGE_NAME_PATTERN)) {
            return false;
        }

        // Additional checks for package name validity
        return isValidPackageNameStructure(pkg);
    }

    /**
     * Performs detailed package name structure validation.
     * Checks for common issues in package naming.
     *
     * @param packageName The package name to validate (assumed lowercase)
     * @return true if structure is valid, false otherwise
     */
    private boolean isValidPackageNameStructure(String packageName) {
        // Split into components
        String[] components = packageName.split("\\.");

        // Must have at least 2 components (domain.app)
        if (components.length < 2) {
            return false;
        }

        // Each component must be valid
        for (String component : components) {
            if (!isValidPackageComponent(component)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Validates individual package name components.
     *
     * @param component A single component of the package name
     * @return true if component is valid, false otherwise
     */
    private boolean isValidPackageComponent(String component) {
        if (TextUtils.isEmpty(component)) {
            return false;
        }

        // Must start with letter
        if (!Character.isLetter(component.charAt(0))) {
            return false;
        }

        // Can only contain letters, numbers, and underscores
        for (char c : component.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && c != '_') {
                return false;
            }
        }

        // Cannot be a Java keyword
        return !isJavaKeyword(component);
    }

    /**
     * Checks if a string is a Java reserved keyword.
     * Package components cannot be Java keywords.
     *
     * @param word The word to check
     * @return true if it's a Java keyword, false otherwise
     */
    private boolean isJavaKeyword(String word) {
        String[] javaKeywords = {
                "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
                "class", "const", "continue", "default", "do", "double", "else", "enum",
                "extends", "final", "finally", "float", "for", "goto", "if", "implements",
                "import", "instanceof", "int", "interface", "long", "native", "new",
                "package", "private", "protected", "public", "return", "short", "static",
                "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
                "transient", "try", "void", "volatile", "while"
        };

        for (String keyword : javaKeywords) {
            if (keyword.equals(word)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks network connectivity for download operations.
     * Used in complex validation before attempting download.
     *
     * @param context Application context for accessing ConnectivityManager
     * @return true if network is available, false otherwise
     */
    public boolean hasNetworkConnectivity(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) {
            return false;
        }

        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * Validates all input fields together for comprehensive validation.
     * Used before initiating download process.
     *
     * @param deviceModel The device model
     * @param sdkVersion The SDK version
     * @param packageName The package name
     * @return ValidationResult containing validation status and error messages
     */
    public ValidationResult validateAllInputs(String deviceModel, String sdkVersion, String packageName) {
        ValidationResult result = new ValidationResult();

        // Validate device model
        if (!isValidDeviceModel(deviceModel)) {
            result.addError("Device model must follow Samsung format (SM-XXXXX)");
        }

        // Validate SDK version
        if (!isValidSdkVersion(sdkVersion)) {
            result.addError("SDK version must be between " + MIN_SDK_VERSION + " and " + MAX_SDK_VERSION);
        }

        // Validate package name
        if (!isValidPackageName(packageName)) {
            result.addError("Package name must follow standard Android format (com.company.app)");
        }

        return result;
    }

    /**
     * Gets the valid SDK version range as a human-readable string.
     *
     * @return String describing valid SDK version range
     */
    public String getValidSdkVersionRange() {
        return MIN_SDK_VERSION + " and above (Android 4.4+)";
    }

    /**
     * Gets an example of valid Samsung device model format.
     *
     * @return Example device model string
     */
    public String getDeviceModelExample() {
        return "SM-G970F (Galaxy S10e)";
    }

    /**
     * Gets an example of valid package name format.
     *
     * @return Example package name string
     */
    public String getPackageNameExample() {
        return "com.sec.android.app.myfiles";
    }
}