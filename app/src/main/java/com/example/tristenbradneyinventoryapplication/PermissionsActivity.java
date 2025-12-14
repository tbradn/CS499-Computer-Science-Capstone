package com.example.tristenbradneyinventoryapplication;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class PermissionsActivity extends Activity {

    private static final int SMS_PERMISSION_REQUEST_CODE = 1001;

    private Button grantPermissionButton;
    private Button denyPermissionButton;
    private Button continueButton;
    private LinearLayout permissionStatusLayout;
    private TextView permissionStatusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions);

        initializeViews();
        setupClickListeners();
        checkCurrentPermissionStatus();
    }

    private void initializeViews() {
        grantPermissionButton = findViewById(R.id.grant_permission_button);
        denyPermissionButton = findViewById(R.id.deny_permission_button);
        continueButton = findViewById(R.id.continue_button);
        permissionStatusLayout = findViewById(R.id.permission_status_layout);
        permissionStatusText = findViewById(R.id.permission_status_text);
    }

    private void setupClickListeners() {
        grantPermissionButton.setOnClickListener(v -> requestSMSPermission());
        denyPermissionButton.setOnClickListener(v -> handlePermissionDenied());
        continueButton.setOnClickListener(v -> proceedToInventory());
    }

    private void checkCurrentPermissionStatus() {
        if (checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            showPermissionGranted();
        }
    }

    private void requestSMSPermission() {
        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_REQUEST_CODE);
        } else {
            showPermissionGranted();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showPermissionGranted();
                Toast.makeText(this, getString(R.string.sms_notifications_enabled), Toast.LENGTH_SHORT).show();
            } else {
                handlePermissionDenied();
                Toast.makeText(this, getString(R.string.sms_notifications_disabled), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showPermissionGranted() {
        permissionStatusLayout.setVisibility(View.VISIBLE);
        permissionStatusText.setText(getString(R.string.permission_granted));
        permissionStatusText.setTextColor(0xFF4CAF50); // Green color

        grantPermissionButton.setVisibility(View.GONE);
        denyPermissionButton.setVisibility(View.GONE);
        continueButton.setVisibility(View.VISIBLE);
    }

    private void handlePermissionDenied() {
        permissionStatusLayout.setVisibility(View.VISIBLE);
        permissionStatusText.setText(getString(R.string.permission_denied));
        permissionStatusText.setTextColor(0xFFF44336);

        grantPermissionButton.setVisibility(View.GONE);
        denyPermissionButton.setVisibility(View.GONE);
        continueButton.setVisibility(View.VISIBLE);
    }

    private void proceedToInventory() {
        Intent intent = new Intent(this, InventoryActivity.class);
        startActivity(intent);
        finish();
    }
}
