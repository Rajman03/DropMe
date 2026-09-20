package com.example.testapp.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.EncodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QRCodeHelper {

    public interface QrCallback {
        void onGenerated(Bitmap bitmap);
    }

    public interface QrDecodeCallback {
        void onDecoded(String text);
        void onError(String error);
    }

    private static final ExecutorService executor = Executors.newFixedThreadPool(2);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Memory cache bounded by byte size (max 10MB) to prevent OutOfMemoryError
    private static final int MAX_CACHE_SIZE_BYTES = 10 * 1024 * 1024;
    private static final LruCache<String, Bitmap> memoryCache = new LruCache<String, Bitmap>(MAX_CACHE_SIZE_BYTES) {
        @Override
        protected int sizeOf(String key, Bitmap bitmap) {
            return bitmap != null ? bitmap.getByteCount() : 1;
        }
    };

    /**
     * Generates a 100% offline, local QR code bitmap using ZXing QRCodeWriter.
     * Caches to memory and internal disk storage so it's always available offline.
     */
    public static void generateQrCode(Context context, String data, String colorHex, QrCallback callback) {
        if (data == null || data.trim().isEmpty()) {
            if (callback != null) {
                mainHandler.post(() -> callback.onGenerated(null));
            }
            return;
        }

        final String finalData = data.trim();
        final String finalColor = (colorHex != null && !colorHex.isEmpty()) ? colorHex : "#0F172A";
        final String cacheKey = getCacheKey(finalData, finalColor);

        // 1. Check memory cache
        synchronized (memoryCache) {
            Bitmap memBmp = memoryCache.get(cacheKey);
            if (memBmp != null && !memBmp.isRecycled()) {
                callback.onGenerated(memBmp);
                return;
            }
        }

        executor.execute(() -> {
            // 2. Check disk cache
            File diskFile = (context != null) ? getDiskCacheFile(context, cacheKey) : null;
            if (diskFile != null && diskFile.exists()) {
                try {
                    Bitmap diskBmp = BitmapFactory.decodeFile(diskFile.getAbsolutePath());
                    if (diskBmp != null) {
                        synchronized (memoryCache) {
                            memoryCache.put(cacheKey, diskBmp);
                        }
                        mainHandler.post(() -> callback.onGenerated(diskBmp));
                        return;
                    }
                } catch (Exception ignored) {}
            }

            // 3. Generate locally using ZXing QRCodeWriter
            Bitmap generated = generateLocalQrBitmap(finalData, finalColor, 400);

            // 4. Save to memory and disk cache
            if (generated != null) {
                synchronized (memoryCache) {
                    memoryCache.put(cacheKey, generated);
                }
                if (diskFile != null) {
                    try {
                        diskFile.getParentFile().mkdirs();
                        FileOutputStream fos = new FileOutputStream(diskFile);
                        generated.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        fos.flush();
                        fos.close();
                    } catch (Exception ignored) {}
                }
            }

            final Bitmap finalResult = generated;
            mainHandler.post(() -> callback.onGenerated(finalResult));
        });
    }

    public static void generateQrCode(String data, String colorHex, QrCallback callback) {
        generateQrCode(null, data, colorHex, callback);
    }

    public static void generateQrCode(String data, QrCallback callback) {
        generateQrCode(null, data, "#0F172A", callback);
    }

    private static Bitmap generateLocalQrBitmap(String text, String colorHex, int size) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size, hints);
            int width = matrix.getWidth();
            int height = matrix.getHeight();

            int fgColor = Color.parseColor(colorHex != null ? colorHex : "#0F172A");
            int bgColor = Color.WHITE;

            // Zamiast wywoływać Bitmap.setPixel() dla każdego piksela (JNI per piksel),
            // wypełniamy tablicę i tworzymy bitmapę jednym wywołaniem.
            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                int rowOffset = y * width;
                for (int x = 0; x < width; x++) {
                    pixels[rowOffset + x] = matrix.get(x, y) ? fgColor : bgColor;
                }
            }
            return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Decodes a QR code from a given Bitmap locally using ZXing QRCodeReader.
     */
    public static void decodeQrBitmap(Bitmap bitmap, QrDecodeCallback callback) {
        if (bitmap == null) {
            if (callback != null) callback.onError("Brak obrazu do zeskanowania");
            return;
        }

        executor.execute(() -> {
            try {
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                int[] pixels = new int[width * height];
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

                LuminanceSource source = new RGBLuminanceSource(width, height, pixels);
                BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
                QRCodeReader reader = new QRCodeReader();
                com.google.zxing.Result result = reader.decode(binaryBitmap);

                if (result != null && result.getText() != null) {
                    mainHandler.post(() -> callback.onDecoded(result.getText()));
                } else {
                    mainHandler.post(() -> callback.onError("Nie wykryto kodu QR na zdjęciu"));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Błąd odczytu kodu QR: " + e.getMessage()));
            }
        });
    }

    private static String getCacheKey(String data, String color) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update((data + "_" + color).getBytes("UTF-8"));
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "qr_" + Math.abs((data + color).hashCode());
        }
    }

    private static File getDiskCacheFile(Context context, String key) {
        File dir = new File(context.getFilesDir(), "qr_cache");
        return new File(dir, key + ".png");
    }
}
