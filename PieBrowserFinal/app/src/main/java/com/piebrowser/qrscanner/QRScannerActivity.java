package com.piebrowser.qrscanner;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.piebrowser.R;
import com.piebrowser.browser.BrowserActivity;
import com.piebrowser.ui.theme.ThemeManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * QRScannerActivity — scan QR codes and barcodes with the camera.
 *
 * Supports:
 * - URL QR codes → open in browser
 * - Text QR codes → copy or search
 * - WiFi QR codes → show credentials
 * - Contact QR codes → show info
 * - Any barcode format
 */
public class QRScannerActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 200;
    public static final String RESULT_URL = "scanned_url";

    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;
    private boolean resultHandled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Scan QR Code");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        previewView   = findViewById(R.id.qrPreviewView);
        cameraExecutor = Executors.newSingleThreadExecutor();

        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build();
        barcodeScanner = BarcodeScanning.getClient(options);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                analysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    if (resultHandled) { imageProxy.close(); return; }

                    InputImage image = InputImage.fromMediaImage(
                            imageProxy.getImage(),
                            imageProxy.getImageInfo().getRotationDegrees());

                    barcodeScanner.process(image)
                            .addOnSuccessListener(barcodes -> {
                                if (!barcodes.isEmpty() && !resultHandled) {
                                    resultHandled = true;
                                    handleBarcode(barcodes.get(0));
                                }
                            })
                            .addOnCompleteListener(t -> imageProxy.close());
                });

                CameraSelector selector = CameraSelector.DEFAULT_BACK_CAMERA;
                provider.unbindAll();
                provider.bindToLifecycle(this, selector, preview, analysis);

            } catch (Exception e) {
                Toast.makeText(this, "Camera error: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void handleBarcode(Barcode barcode) {
        runOnUiThread(() -> {
            String raw = barcode.getRawValue();
            if (raw == null) { resultHandled = false; return; }

            // Vibrate for feedback
            android.os.Vibrator v = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v != null) v.vibrate(100);

            switch (barcode.getValueType()) {
                case Barcode.TYPE_URL:
                    showUrlDialog(barcode.getUrl().getUrl());
                    break;
                case Barcode.TYPE_WIFI:
                    showWifiDialog(barcode.getWifi());
                    break;
                case Barcode.TYPE_CONTACT_INFO:
                    showContactDialog(barcode.getContactInfo());
                    break;
                default:
                    showTextDialog(raw);
                    break;
            }
        });
    }

    private void showUrlDialog(String url) {
        new AlertDialog.Builder(this)
            .setTitle("Open URL")
            .setMessage(url)
            .setPositiveButton("Open in Browser", (d, w) -> {
                Intent i = new Intent(this, BrowserActivity.class);
                i.setAction(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
                finish();
            })
            .setNeutralButton("Copy", (d, w) -> copyToClipboard(url))
            .setNegativeButton("Cancel", (d, w) -> resultHandled = false)
            .show();
    }

    private void showTextDialog(String text) {
        new AlertDialog.Builder(this)
            .setTitle("Scanned Text")
            .setMessage(text)
            .setPositiveButton("Copy", (d, w) -> copyToClipboard(text))
            .setNeutralButton("Search", (d, w) -> {
                Intent i = new Intent(this, BrowserActivity.class);
                i.setAction(Intent.ACTION_VIEW);
                i.setData(Uri.parse("https://duckduckgo.com/?q=" +
                        Uri.encode(text)));
                startActivity(i);
                finish();
            })
            .setNegativeButton("Cancel", (d, w) -> resultHandled = false)
            .show();
    }

    private void showWifiDialog(Barcode.WiFi wifi) {
        String info = "Network: " + wifi.getSsid() +
                "\nPassword: " + wifi.getPassword() +
                "\nType: " + (wifi.getEncryptionType() == Barcode.WiFi.TYPE_WPA ? "WPA" : "WEP");
        new AlertDialog.Builder(this)
            .setTitle("WiFi Network")
            .setMessage(info)
            .setPositiveButton("Copy Password", (d, w) -> copyToClipboard(wifi.getPassword()))
            .setNegativeButton("Close", (d, w) -> resultHandled = false)
            .show();
    }

    private void showContactDialog(Barcode.ContactInfo contact) {
        StringBuilder sb = new StringBuilder();
        if (contact.getName() != null) sb.append(contact.getName().getFormattedName()).append("\n");
        for (Barcode.Phone phone : contact.getPhones()) sb.append("📞 ").append(phone.getNumber()).append("\n");
        for (Barcode.Email email : contact.getEmails()) sb.append("✉️ ").append(email.getAddress()).append("\n");
        new AlertDialog.Builder(this)
            .setTitle("Contact")
            .setMessage(sb.toString().trim())
            .setPositiveButton("Copy", (d, w) -> copyToClipboard(sb.toString()))
            .setNegativeButton("Close", (d, w) -> resultHandled = false)
            .show();
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("Scanned", text));
        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            @NonNull String[] permissions, @NonNull int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == CAMERA_PERMISSION_REQUEST &&
                results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this, "Camera permission required for QR scanning",
                    Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        barcodeScanner.close();
    }
}
