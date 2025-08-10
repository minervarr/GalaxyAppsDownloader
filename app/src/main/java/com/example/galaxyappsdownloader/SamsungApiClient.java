package com.example.galaxyappsdownloader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client for communicating with Samsung's APK download API
 *
 * This class handles HTTP requests to Samsung servers and parses
 * the XML responses to extract APK information. It replicates the
 * core networking functionality from the Python script.
 *
 * @author Your Name
 * @since 1.0
 */
public class SamsungApiClient {

    private static final int CONNECTION_TIMEOUT = 10000; // 10 seconds
    private static final int READ_TIMEOUT = 15000; // 15 seconds

    /**
     * Regex pattern for parsing Samsung API XML response
     *
     * This pattern extracts (using numbered groups):
     * - Group 1: resultCode (Status code from Samsung servers)
     * - Group 2: resultMsg (Status message)
     * - Group 3: downloadURI (Direct download link for the APK)
     * - Group 4: versionCode (Numeric version code)
     * - Group 5: versionName (Human-readable version name)
     */
    private static final String RESPONSE_PATTERN =
            "resultCode>(\\d+)</resultCode>" +
                    ".*?<resultMsg>([^<]*)</resultMsg>" +
                    ".*?<downloadURI><!\\[CDATA\\[([^\\]]*?)\\]\\]></downloadURI>" +
                    ".*?<versionCode>(\\d+)</versionCode>" +
                    ".*?<versionName>([^<]*)</versionName>";

    private static final Pattern responsePattern = Pattern.compile(RESPONSE_PATTERN,
            Pattern.MULTILINE | Pattern.DOTALL);

    /**
     * Fetches APK information from Samsung servers
     *
     * @param apiUrl The Samsung API URL with all parameters
     * @return ApkInfo object containing the parsed response
     * @throws IOException if network request fails
     */
    public ApkInfo fetchApkInfo(String apiUrl) throws IOException {
        String xmlResponse = makeHttpRequest(apiUrl);
        return parseXmlResponse(xmlResponse);
    }

    /**
     * Makes HTTP GET request to the specified URL
     *
     * @param urlString The URL to request
     * @return The response body as a string
     * @throws IOException if the request fails
     */
    private String makeHttpRequest(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);

            // Set user agent to mimic the original Python script behavior
            connection.setRequestProperty("User-Agent",
                    "Samsung Galaxy Store/4.5.49.7 (Android 9; SM-G950F)");

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + responseCode + ": " + connection.getResponseMessage());
            }

            return readResponseBody(connection);

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Reads the response body from an HTTP connection
     *
     * @param connection The HTTP connection to read from
     * @return The response body as a string
     * @throws IOException if reading fails
     */
    private String readResponseBody(HttpURLConnection connection) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream()))) {

            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            return response.toString();
        }
    }

    /**
     * Parses the XML response from Samsung servers
     *
     * @param xmlResponse The raw XML response string
     * @return ApkInfo object with parsed data
     */
    private ApkInfo parseXmlResponse(String xmlResponse) {
        // Debug: Log the response for troubleshooting
        System.out.println("Samsung API Response: " + xmlResponse);

        // Try the alternative parsing method which is more flexible
        ApkInfo result = parseXmlResponseAlternative(xmlResponse);
        if (result.isValid()) {
            return result;
        }

        // If alternative failed, try original method
        Matcher matcher = responsePattern.matcher(xmlResponse);

        if (!matcher.find()) {
            // Try to extract error message if regex fails
            String errorMessage = extractErrorMessage(xmlResponse);
            System.out.println("Regex failed to match. Error message: " + errorMessage);
            return ApkInfo.createError(errorMessage != null ? errorMessage : "Failed to parse response");
        }

        try {
            String status = matcher.group(1);      // resultCode
            String message = matcher.group(2);     // resultMsg
            String downloadUri = matcher.group(3); // downloadURI
            String versionCode = matcher.group(4);  // versionCode
            String versionName = matcher.group(5);  // versionName

            System.out.println("Parsed - Status: " + status + ", Message: " + message +
                    ", URI: " + downloadUri + ", Version: " + versionCode + "/" + versionName);

            // Check if Samsung returned an error status
            if (!"0".equals(status)) {
                return ApkInfo.createError("Samsung Servers: " + message);
            }

            return ApkInfo.createValid(downloadUri, versionCode, versionName);

        } catch (Exception e) {
            return ApkInfo.createError("Error parsing response: " + e.getMessage());
        }
    }

    /**
     * Attempts to extract error message from XML when regex parsing fails
     *
     * @param xmlResponse The XML response to parse
     * @return Error message if found, null otherwise
     */
    private String extractErrorMessage(String xmlResponse) {
        Pattern errorPattern = Pattern.compile("resultMsg>([^<]*)</resultMsg>");
        Matcher errorMatcher = errorPattern.matcher(xmlResponse);

        if (errorMatcher.find()) {
            return errorMatcher.group(1);
        }

        return null;
    }

    /**
     * Alternative parsing method using individual regex patterns
     * This mimics the Python script's approach more closely
     *
     * @param xmlResponse The raw XML response string
     * @return ApkInfo object with parsed data
     */
    private ApkInfo parseXmlResponseAlternative(String xmlResponse) {
        try {
            // Extract individual fields using separate patterns
            String resultCode = extractField(xmlResponse, "resultCode>(\\d+)</resultCode>");
            String resultMsg = extractField(xmlResponse, "resultMsg>([^<]*)</resultMsg>");
            String downloadURI = extractField(xmlResponse, "downloadURI><!\\[CDATA\\[([^\\]]*?)\\]\\]></downloadURI>");
            String versionCode = extractField(xmlResponse, "versionCode>(\\d+)</versionCode>");
            String versionName = extractField(xmlResponse, "versionName>([^<]*)</versionName>");

            System.out.println("Alternative parsing - Code: " + resultCode + ", Msg: " + resultMsg +
                    ", URI: " + downloadURI + ", Version: " + versionCode + "/" + versionName);

            // If we couldn't extract essential fields, return error
            if (resultCode == null) {
                return ApkInfo.createError("Could not extract result code from response");
            }

            // Check result code - Samsung uses different codes, not just 0/1
            if ("0".equals(resultCode) || "200".equals(resultCode) || downloadURI != null) {
                if (downloadURI != null && versionCode != null && versionName != null) {
                    return ApkInfo.createValid(downloadURI, versionCode, versionName);
                }
            }

            // Return error with Samsung's message
            String errorMsg = resultMsg != null ? resultMsg : "Unknown Samsung server error";
            return ApkInfo.createError("Samsung Servers: " + errorMsg);

        } catch (Exception e) {
            return ApkInfo.createError("Error parsing response: " + e.getMessage());
        }
    }

    /**
     * Extract a field using regex pattern
     *
     * @param text The text to search in
     * @param pattern The regex pattern with one capture group
     * @return The captured value or null if not found
     */
    private String extractField(String text, String pattern) {
        Pattern p = Pattern.compile(pattern, Pattern.MULTILINE | Pattern.DOTALL);
        Matcher m = p.matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }
}