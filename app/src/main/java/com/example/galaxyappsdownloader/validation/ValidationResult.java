package com.example.galaxyappsdownloader.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * Value object representing the result of input validation.
 * Follows the Value Object pattern for encapsulating validation results.
 * Immutable after construction to ensure data integrity.
 *
 * @author Samsung APK Downloader Team
 * @since Phase 2.0
 */
public class ValidationResult {

    private final List<String> errors;
    private final List<String> warnings;

    /**
     * Creates a new ValidationResult with empty error and warning lists.
     */
    public ValidationResult() {
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
    }

    /**
     * Adds an error message to the validation result.
     *
     * @param errorMessage The error message to add
     */
    public void addError(String errorMessage) {
        if (errorMessage != null && !errorMessage.trim().isEmpty()) {
            errors.add(errorMessage.trim());
        }
    }

    /**
     * Adds a warning message to the validation result.
     *
     * @param warningMessage The warning message to add
     */
    public void addWarning(String warningMessage) {
        if (warningMessage != null && !warningMessage.trim().isEmpty()) {
            warnings.add(warningMessage.trim());
        }
    }

    /**
     * Checks if the validation passed (no errors).
     *
     * @return true if validation passed, false if there are errors
     */
    public boolean isValid() {
        return errors.isEmpty();
    }

    /**
     * Checks if there are any validation errors.
     *
     * @return true if there are errors, false otherwise
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Checks if there are any validation warnings.
     *
     * @return true if there are warnings, false otherwise
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    /**
     * Gets a copy of all error messages.
     * Returns a copy to maintain immutability.
     *
     * @return List of error messages
     */
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    /**
     * Gets a copy of all warning messages.
     * Returns a copy to maintain immutability.
     *
     * @return List of warning messages
     */
    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }

    /**
     * Gets the first error message, if any.
     *
     * @return First error message, or null if no errors
     */
    public String getFirstError() {
        return errors.isEmpty() ? null : errors.get(0);
    }

    /**
     * Gets the first warning message, if any.
     *
     * @return First warning message, or null if no warnings
     */
    public String getFirstWarning() {
        return warnings.isEmpty() ? null : warnings.get(0);
    }

    /**
     * Gets all error messages as a single formatted string.
     *
     * @return Formatted error messages, or empty string if no errors
     */
    public String getFormattedErrors() {
        if (errors.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < errors.size(); i++) {
            if (i > 0) {
                sb.append("\n");
            }
            sb.append("• ").append(errors.get(i));
        }
        return sb.toString();
    }

    /**
     * Gets all warning messages as a single formatted string.
     *
     * @return Formatted warning messages, or empty string if no warnings
     */
    public String getFormattedWarnings() {
        if (warnings.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < warnings.size(); i++) {
            if (i > 0) {
                sb.append("\n");
            }
            sb.append("• ").append(warnings.get(i));
        }
        return sb.toString();
    }

    /**
     * Gets the total number of errors.
     *
     * @return Number of error messages
     */
    public int getErrorCount() {
        return errors.size();
    }

    /**
     * Gets the total number of warnings.
     *
     * @return Number of warning messages
     */
    public int getWarningCount() {
        return warnings.size();
    }

    /**
     * Clears all errors and warnings.
     * Used for resetting validation state.
     */
    public void clear() {
        errors.clear();
        warnings.clear();
    }

    /**
     * Merges another ValidationResult into this one.
     *
     * @param other The ValidationResult to merge
     */
    public void merge(ValidationResult other) {
        if (other != null) {
            errors.addAll(other.errors);
            warnings.addAll(other.warnings);
        }
    }

    @Override
    public String toString() {
        return "ValidationResult{" +
                "valid=" + isValid() +
                ", errorCount=" + getErrorCount() +
                ", warningCount=" + getWarningCount() +
                '}';
    }
}