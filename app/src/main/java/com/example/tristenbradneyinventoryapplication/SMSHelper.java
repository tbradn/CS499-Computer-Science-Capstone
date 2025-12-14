package com.example.tristenbradneyinventoryapplication;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

/**
 * SMSHelper class manages SMS notification functionality for the inventory application.
 * This class handles permission checking and sending SMS messages for low inventory alerts
 * using the device's own phone number or a fallback number.
 */
public class SMSHelper {

    private static final String TAG = "SMSHelper";

    // Fallback phone number for testing when device number can't be determined.
    private static final String FALLBACK_PHONE_NUMBER = "5551234567";

    private Activity activity;
    private SmsManager smsManager;

    /**
     * Constructor for SMSHelper
     * @param activity The calling activity that is needed for permission checks and context.
     */
    public SMSHelper(Activity activity) {
        this.activity = activity;
        this.smsManager = SmsManager.getDefault();
    }

    /**
     * Check if the app has SMS permission.
     * @return true if SMS permission is granted, false otherwise.
     */
    public boolean hasSMSPermission() {
        return activity.checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Get the device's phone number or emulator number.
     * @return The device's phone number, or fallback number if unavailable.
     */
    private String getDevicePhoneNumber() {
        try {

            // For emulators, use a standard emulator number.
            String deviceName = android.os.Build.FINGERPRINT;
            if (deviceName != null && (deviceName.contains("generic") || deviceName.contains("emulator"))) {
                Log.d(TAG, "Emulator detected, using emulator phone number");
                return "15551234567"; // Standard Android emulator number
            }

            // Try to get real device number.
            if (activity.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                TelephonyManager telephonyManager = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
                String phoneNumber = telephonyManager.getLine1Number();

                if (phoneNumber != null && !phoneNumber.isEmpty()) {
                    phoneNumber = phoneNumber.replaceAll("[^0-9]", "");
                    if (phoneNumber.length() >= 10) {
                        Log.d(TAG, "Device phone number detected");
                        return phoneNumber;
                    }
                }
            }

        } catch (Exception e) {
            Log.w(TAG, "Could not retrieve device phone number: " + e.getMessage());
        }

        Log.d(TAG, "Using fallback phone number");
        return FALLBACK_PHONE_NUMBER;
    }

    /**
     * Send a low inventory alert SMS notification.
     * @param itemName Name of the inventory item with low stock.
     * @param currentQuantity Current quantity of the item.
     * @param phoneNumber Phone number to send SMS to.
     */
    public void sendLowInventoryAlert(String itemName, int currentQuantity, String phoneNumber) {

        // Check if SMS permission is granted.
        if (!hasSMSPermission()) {
            Log.w(TAG, "SMS permission not granted. Cannot send notification.");
            Toast.makeText(activity, "SMS permission required for notifications", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use provided phone number, or get device number if none provided.
        String targetPhone = (phoneNumber != null && !phoneNumber.isEmpty()) ? phoneNumber : getDevicePhoneNumber();

        // Create the SMS message.
        String message = createLowInventoryMessage(itemName, currentQuantity);

        try {
            // Send the SMS.
            smsManager.sendTextMessage(targetPhone, null, message, null, null);

            Log.d(TAG, "Low inventory SMS sent successfully for item: " + itemName);
            Toast.makeText(activity, "Low inventory alert sent to " + formatPhoneNumber(targetPhone), Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Log.e(TAG, "Failed to send SMS notification: " + e.getMessage(), e);
            Toast.makeText(activity, "Failed to send SMS notification", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Send a general inventory notification.
     * @param message Custom message to send.
     * @param phoneNumber Phone number to send SMS to.
     */
    public void sendInventoryNotification(String message, String phoneNumber) {

        // Check if SMS permission is granted.
        if (!hasSMSPermission()) {
            Log.w(TAG, "SMS permission not granted. Cannot send notification.");
            Toast.makeText(activity, "SMS permission required for notifications", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use provided phone number, or get device number if none provided.
        String targetPhone = (phoneNumber != null && !phoneNumber.isEmpty()) ? phoneNumber : getDevicePhoneNumber();

        try {

            // Send the SMS.
            smsManager.sendTextMessage(targetPhone, null, message, null, null);

            Log.d(TAG, "Inventory notification SMS sent successfully");
            Toast.makeText(activity, "Notification sent to " + formatPhoneNumber(targetPhone), Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "Failed to send SMS notification: " + e.getMessage(), e);
            Toast.makeText(activity, "Failed to send SMS notification", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Create a formatted low inventory message.
     * @param itemName Name of the item with low stock.
     * @param currentQuantity Current quantity of the item.
     * @return Formatted SMS message string.
     */
    private String createLowInventoryMessage(String itemName, int currentQuantity) {
        return String.format("INVENTORY ALERT\n\n" +
                        "Item: %s\n" +
                        "Current Stock: %d\n" +
                        "Status: LOW INVENTORY\n\n" +
                        "Please restock soon.\n" +
                        "- Inventory Manager App",
                itemName, currentQuantity);
    }

    /**
     * Send a test SMS to verify SMS functionality.
     * @param phoneNumber Phone number to send test SMS to.
     */
    public void sendTestMessage(String phoneNumber) {

        // Check if SMS permission is granted.
        if (!hasSMSPermission()) {
            Log.w(TAG, "SMS permission not granted. Cannot send test message.");
            Toast.makeText(activity, "SMS permission required for testing", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use provided phone number, or get device number if none provided.
        String targetPhone = (phoneNumber != null && !phoneNumber.isEmpty()) ? phoneNumber : getDevicePhoneNumber();

        String testMessage = "TEST MESSAGE\n\n" +
                "This is a test message from your Inventory Manager app.\n\n" +
                "SMS notifications are working correctly!\n" +
                "Target Number: " + formatPhoneNumber(targetPhone) + "\n" +
                "- Inventory Manager App";

        try {

            // Send the test SMS.
            smsManager.sendTextMessage(targetPhone, null, testMessage, null, null);

            Log.d(TAG, "Test SMS sent successfully");
            Toast.makeText(activity, "Test SMS sent to " + formatPhoneNumber(targetPhone), Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Log.e(TAG, "Failed to send test SMS: " + e.getMessage(), e);
            Toast.makeText(activity, "Failed to send test SMS", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Check if the device supports SMS functionality.
     * @return true if SMS is supported, false otherwise.
     */
    public boolean isSMSSupported() {
        try {
            return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
        } catch (Exception e) {
            Log.e(TAG, "Error checking SMS support: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get SMS permission status as a user-friendly string.
     * @return String describing current SMS permission status.
     */
    public String getSMSPermissionStatus() {
        if (!isSMSSupported()) {
            return "SMS not supported on this device";
        } else if (hasSMSPermission()) {
            String deviceNumber = getDevicePhoneNumber();
            return "SMS notifications enabled\nSending to: " + formatPhoneNumber(deviceNumber);
        } else {
            return "SMS notifications disabled - permission required";
        }
    }

    /**
     * Format phone number for display.
     * @param phoneNumber Raw phone number.
     * @return Formatted phone number string.
     */
    public static String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return "No phone number";
        }

        // Remove all non-digit characters.
        String digitsOnly = phoneNumber.replaceAll("[^0-9]", "");

        // Format based on length.
        if (digitsOnly.length() == 10) {

            // US format: (XXX) XXX-XXXX.
            return String.format("(%s) %s-%s",
                    digitsOnly.substring(0, 3),
                    digitsOnly.substring(3, 6),
                    digitsOnly.substring(6));
        } else if (digitsOnly.length() == 11 && digitsOnly.startsWith("1")) {

            // US format with country code: +1 (XXX) XXX-XXXX.
            return String.format("+1 (%s) %s-%s",
                    digitsOnly.substring(1, 4),
                    digitsOnly.substring(4, 7),
                    digitsOnly.substring(7));
        } else if (digitsOnly.length() > 10) {

            // For longer numbers, like emulator, format as +X-XXX-XXX-XXXX.
            return String.format("+%s-%s-%s-%s",
                    digitsOnly.substring(0, 1),
                    digitsOnly.substring(1, 4),
                    digitsOnly.substring(4, 7),
                    digitsOnly.substring(7, Math.min(11, digitsOnly.length())));
        } else {

            // For shorter numbers, just add dashes.
            return digitsOnly.replaceAll("(.{3})", "$1-").replaceAll("-$", "");
        }
    }
}