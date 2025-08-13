package com.example.galaxyappsdownloader.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Repository class for managing device models using the Repository pattern.
 * Provides an abstraction layer over data persistence for device models.
 * Implements Singleton pattern and follows Single Responsibility Principle.
 *
 * @author Samsung APK Downloader Team
 * @since Phase 2.0
 */
public class DeviceModelRepository {

    // Singleton instance
    private static DeviceModelRepository instance;

    // SharedPreferences configuration
    private static final String PREF_NAME = "DeviceModelsPrefs";
    private static final String KEY_SAVED_MODELS = "saved_models";

    // Default Samsung device models for initial population
    private static final String[] DEFAULT_MODELS = {
            "SM-G970F", "SM-G973F", "SM-G975F", // Galaxy S10 series
            "SM-G980F", "SM-G981B", "SM-G985F", // Galaxy S20 series
            "SM-G991B", "SM-G996B", "SM-G998B", // Galaxy S21 series
            "SM-G781B", "SM-G780F",             // Galaxy S20 FE
            "SM-N970F", "SM-N975F",             // Galaxy Note 10 series
            "SM-N980F", "SM-N985F", "SM-N986B", // Galaxy Note 20 series
            "SM-A515F", "SM-A525F", "SM-A715F"  // Galaxy A series
    };

    private final SharedPreferences preferences;

    /**
     * Private constructor implementing Singleton pattern.
     *
     * @param context Application context for SharedPreferences access
     */
    private DeviceModelRepository(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Initialize with default models if no models are saved
        if (getSavedModels().isEmpty()) {
            initializeDefaultModels();
        }
    }

    /**
     * Gets the singleton instance of DeviceModelRepository.
     * Thread-safe implementation using synchronized method.
     *
     * @param context Application context
     * @return Singleton instance of DeviceModelRepository
     */
    public static synchronized DeviceModelRepository getInstance(Context context) {
        if (instance == null) {
            instance = new DeviceModelRepository(context);
        }
        return instance;
    }

    /**
     * Retrieves all saved device models.
     * Returns a copy to prevent external modification of internal data.
     *
     * @return List of saved device models
     */
    public List<String> getSavedModels() {
        Set<String> modelSet = preferences.getStringSet(KEY_SAVED_MODELS, new HashSet<>());
        List<String> models = new ArrayList<>(modelSet);

        // Sort models alphabetically for consistent display
        models.sort(String::compareTo);

        return models;
    }

    /**
     * Adds a new device model to the saved models.
     * Prevents duplicate entries and validates input.
     *
     * @param deviceModel The device model to add (e.g., "SM-G970F")
     * @return true if model was added successfully, false if it already exists
     */
    public boolean addModel(String deviceModel) {
        if (deviceModel == null || deviceModel.trim().isEmpty()) {
            return false;
        }

        String normalizedModel = deviceModel.trim().toUpperCase();
        Set<String> currentModels = new HashSet<>(preferences.getStringSet(KEY_SAVED_MODELS, new HashSet<>()));

        boolean wasAdded = currentModels.add(normalizedModel);

        if (wasAdded) {
            preferences.edit()
                    .putStringSet(KEY_SAVED_MODELS, currentModels)
                    .apply();
        }

        return wasAdded;
    }

    /**
     * Removes a device model from saved models.
     *
     * @param deviceModel The device model to remove
     * @return true if model was removed successfully, false if it didn't exist
     */
    public boolean removeModel(String deviceModel) {
        if (deviceModel == null || deviceModel.trim().isEmpty()) {
            return false;
        }

        String normalizedModel = deviceModel.trim().toUpperCase();
        Set<String> currentModels = new HashSet<>(preferences.getStringSet(KEY_SAVED_MODELS, new HashSet<>()));

        boolean wasRemoved = currentModels.remove(normalizedModel);

        if (wasRemoved) {
            preferences.edit()
                    .putStringSet(KEY_SAVED_MODELS, currentModels)
                    .apply();
        }

        return wasRemoved;
    }