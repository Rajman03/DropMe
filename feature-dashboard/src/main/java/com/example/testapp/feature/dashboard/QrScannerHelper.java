package com.example.testapp.feature.dashboard;

import com.example.testapp.core.QRCodeHelper;
import com.example.testapp.core.SocialPlatform;
import com.example.testapp.core.UserQrManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;

public class QrScannerHelper {

    public static final int REQUEST_CODE_CAPTURE_CAMERA = 9001;
    public static final int REQUEST_CODE_PICK_GALLERY = 9002;
    public static final int REQUEST_CODE_CAMERA_PERMISSION = 9003;

    private static AlertDialog activeScannerDialog;
    private static LinearLayout activeResultLayout;
    private static TextView activeResultText;
    private static TextView activeResultTitle;
    private static Button activeBtnOpen;
    private static Button activeBtnSave;
    private static String lastDecodedContent = "";

    public static void launchCamera(Activity activity) {
        if (activity == null) return;
        try {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            activity.startActivityForResult(takePictureIntent, REQUEST_CODE_CAPTURE_CAMERA);
        } catch (android.content.ActivityNotFoundException anfe) {
            Toast.makeText(activity, "Nie znaleziono aplikacji aparatu na urządzeniu", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(activity, "Błąd uruchamiania aparatu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public static void showScanner(Activity activity, UserQrManager qrManager, Runnable onSavedCallback) {
        if (activity == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_qr_scanner, null, false);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        activeScannerDialog = dialog;
        activeResultLayout = view.findViewById(R.id.layoutScanResult);
        activeResultText = view.findViewById(R.id.tvScanResultText);
        activeResultTitle = view.findViewById(R.id.tvScanResultTitle);
        activeBtnOpen = view.findViewById(R.id.btnOpenScanUrl);
        activeBtnSave = view.findViewById(R.id.btnSaveScannedQr);

        Button btnLaunchCamera = view.findViewById(R.id.btnLaunchSystemScanner);
        Button btnPickGallery = view.findViewById(R.id.btnPickFromGallery);
        Button btnClose = view.findViewById(R.id.btnCloseScanner);

        if (btnLaunchCamera != null) {
            btnLaunchCamera.setOnClickListener(v -> launchCamera(activity));
        }

        if (btnPickGallery != null) {
            btnPickGallery.setOnClickListener(v -> {
                try {
                    Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    pickIntent.setType("image/*");
                    activity.startActivityForResult(pickIntent, REQUEST_CODE_PICK_GALLERY);
                } catch (Exception e) {
                    try {
                        Intent fallbackIntent = new Intent(Intent.ACTION_GET_CONTENT);
                        fallbackIntent.setType("image/*");
                        activity.startActivityForResult(fallbackIntent, REQUEST_CODE_PICK_GALLERY);
                    } catch (Exception ex) {
                        Toast.makeText(activity, "Brak galerii: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    public static void handleActivityResult(Activity activity, int requestCode, int resultCode, Intent data, UserQrManager qrManager, Runnable onSaved) {
        if (resultCode != Activity.RESULT_OK) return;

        Bitmap bitmap = null;
        try {
            if (requestCode == REQUEST_CODE_CAPTURE_CAMERA && data != null) {
                if (data.getExtras() != null && data.getExtras().get("data") instanceof Bitmap) {
                    bitmap = (Bitmap) data.getExtras().get("data");
                }
            } else if (requestCode == REQUEST_CODE_PICK_GALLERY && data != null && data.getData() != null) {
                Uri imageUri = data.getData();
                InputStream is = activity.getContentResolver().openInputStream(imageUri);
                bitmap = BitmapFactory.decodeStream(is);
                if (is != null) is.close();
            }
        } catch (Exception e) {
            Toast.makeText(activity, "Błąd wczytywania zdjęcia: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        if (bitmap != null) {
            Toast.makeText(activity, "Analizuję kod QR...", Toast.LENGTH_SHORT).show();
            QRCodeHelper.decodeQrBitmap(bitmap, new QRCodeHelper.QrDecodeCallback() {
                @Override
                public void onDecoded(String text) {
                    displayScanResult(activity, text, qrManager, onSaved);
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(activity, error, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    public static void displayScanResult(Activity activity, String text, UserQrManager qrManager, Runnable onSaved) {
        if (activity == null || text == null) return;
        lastDecodedContent = text.trim();

        if (activeResultLayout != null && activeResultText != null) {
            activeResultLayout.setVisibility(View.VISIBLE);
            activeResultText.setText(lastDecodedContent);

            SocialPlatform platform = SocialPlatform.detectFromUrl(lastDecodedContent);
            if (activeResultTitle != null) {
                activeResultTitle.setText("✓ Odczytano: " + platform.getDisplayName());
            }

            if (activeBtnOpen != null) {
                activeBtnOpen.setOnClickListener(v -> {
                    try {
                        String url = lastDecodedContent;
                        if (!url.startsWith("http://") && !url.startsWith("https://")) {
                            url = "https://" + url;
                        }
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        activity.startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(activity, "Nie można otworzyć linku: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            if (activeBtnSave != null) {
                activeBtnSave.setOnClickListener(v -> {
                    if (qrManager != null) {
                        String name = "Zeskanowany (" + platform.getDisplayName() + ")";
                        qrManager.addItem(name, platform.getKey(), lastDecodedContent);
                        Toast.makeText(activity, "Zapisano kod do Twoich Wizytówek!", Toast.LENGTH_SHORT).show();
                        if (onSaved != null) onSaved.run();
                        if (activeScannerDialog != null) activeScannerDialog.dismiss();
                    }
                });
            }
        } else {
            // If dialog was closed, show toast and copy
            ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("QR Code", lastDecodedContent));
            }
            Toast.makeText(activity, "Odczytano kod: " + lastDecodedContent + " (skopiowano)", Toast.LENGTH_LONG).show();
        }
    }

    public static void handlePermissionResult(Activity activity, int requestCode, String[] permissions, int[] grantResults) {
        // App requires zero dangerous permissions; system camera is used directly.
    }
}
