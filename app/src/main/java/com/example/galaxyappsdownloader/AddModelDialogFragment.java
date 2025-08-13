package com.example.galaxyappsdownloader;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.galaxyappsdownloader.validation.InputValidator;

/**
 * Dialog fragment for adding new device models.
 * Provides real-time validation and user-friendly interface.
 * Follows the Single Responsibility Principle by handling only model addition.
 *
 * @author Samsung APK Downloader Team
 * @since Phase 2.0
 */
public class AddModelDialogFragment extends DialogFragment {

    // Interface for communicating with parent activity
    public interface OnModelAddedListener {
        void onModelAdded(String deviceModel);
    }

    // UI Components
    private EditText deviceModelEditText;
    private TextView validationMessageTextView;
    private AlertDialog dialog;

    // Dependencies
    private InputValidator inputValidator;
    private OnModelAddedListener listener;

    /**
     * Sets the listener for model addition events.
     *
     * @param listener The listener to notify when a model is added
     */
    public void setOnModelAddedListener(OnModelAddedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        inputValidator = new InputValidator();

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_model, null);

        initializeViews(view);
        setupValidation();

        dialog = new AlertDialog.Builder(requireActivity())
                .setTitle("Add Device Model")
                .setView(view)
                .setPositiveButton("Add", null) // Set to null initially
                .setNegativeButton("Cancel", null)
                .create();

        // Override positive button click to prevent auto-dismiss
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                if (validateAndAdd()) {
                    dismiss();
                }
            });

            // Initially disable the Add button
            updateAddButtonState();
        });

        return dialog;
    }

    /**
     * Initializes the dialog views.
     *
     * @param view The inflated dialog view
     */
    private void initializeViews(View view) {
        deviceModelEditText = view.findViewById(R.id.deviceModelEditText);
        validationMessageTextView = view.findViewById(R.id.validationMessageTextView);

        // Set initial focus and show examples
        deviceModelEditText.requestFocus();
        showExampleModels();
    }

    /**
     * Sets up real-time validation for the device model input.
     */
    private void setupValidation() {
        deviceModelEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateInput(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Shows example device models to help users.
     */
    private void showExampleModels() {
        validationMessageTextView.setText("Examples: SM-G970F, SM-G973F, SM-A515F\nEnter Samsung device model in SM-XXXXX format");
        validationMessageTextView.setTextColor(getResources().getColor(android.R.color.darker_gray, null));
    }

    /**
     * Validates the input in real-time and updates UI accordingly.
     *
     * @param input The current input text
     */
    private void validateInput(String input) {
        if (TextUtils.isEmpty(input)) {
            showExampleModels();
            updateAddButtonState();
            return;
        }

        String normalizedInput = input.trim().toUpperCase();

        if (inputValidator.isValidDeviceModel(normalizedInput)) {
            validationMessageTextView.setText("✓ Valid Samsung device model format");
            validationMessageTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark, null));
        } else {
            validationMessageTextView.setText("✗ Invalid format. Use SM-XXXXX (e.g., SM-G970F)");
            validationMessageTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark, null));
        }

        updateAddButtonState();
    }

    /**
     * Updates the state of the Add button based on input validation.
     */
    private void updateAddButtonState() {
        if (dialog != null) {
            String input = deviceModelEditText.getText().toString().trim();
            boolean isValid = !TextUtils.isEmpty(input) &&
                    inputValidator.isValidDeviceModel(input.toUpperCase());
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(isValid);
        }
    }

    /**
     * Validates the current input and adds the model if valid.
     *
     * @return true if model was added successfully, false otherwise
     */
    private boolean validateAndAdd() {
        String input = deviceModelEditText.getText().toString().trim().toUpperCase();

        if (TextUtils.isEmpty(input)) {
            validationMessageTextView.setText("✗ Please enter a device model");
            validationMessageTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark, null));
            return false;
        }

        if (!inputValidator.isValidDeviceModel(input)) {
            validationMessageTextView.setText("✗ Invalid device model format");
            validationMessageTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark, null));
            return false;
        }

        // Notify listener
        if (listener != null) {
            listener.onModelAdded(input);
        }

        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Show keyboard automatically
        if (deviceModelEditText != null) {
            deviceModelEditText.requestFocus();
        }
    }
}