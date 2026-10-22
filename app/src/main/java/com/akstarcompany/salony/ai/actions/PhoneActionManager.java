package com.akstarcompany.salony.ai.actions;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.provider.Settings;
import android.widget.Toast;

public class PhoneActionManager {

    private static boolean isTorchOn = false;

    public static boolean isTorchActive() {
        return isTorchOn;
    }

    public static String setTorch(Context context, boolean enable) {
        CameraManager cm = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        if (cm == null) return "Torch hardware unavailable";

        try {
            String cameraId = null;
            for (String id : cm.getCameraIdList()) {
                CameraCharacteristics characteristics = cm.getCameraCharacteristics(id);
                Boolean hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (hasFlash != null && hasFlash && facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraId = id;
                    break;
                }
            }
            if (cameraId == null && cm.getCameraIdList().length > 0) {
                cameraId = cm.getCameraIdList()[0];
            }

            if (cameraId != null) {
                cm.setTorchMode(cameraId, enable);
                isTorchOn = enable;
                return enable ? "Torch turned ON 💡" : "Torch turned OFF 🌑";
            } else {
                return "Flashlight not supported on this device";
            }
        } catch (CameraAccessException e) {
            return "Torch error: " + e.getMessage();
        } catch (Exception e) {
            return "Torch error: " + e.getMessage();
        }
    }

    public static String openYouTube(Context context, String query) {
        try {
            Uri uri;
            if (query != null && !query.trim().isEmpty()) {
                uri = Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(query.trim()));
            } else {
                uri = Uri.parse("https://www.youtube.com");
            }

            Intent appIntent = new Intent(Intent.ACTION_VIEW, uri);
            appIntent.setPackage("com.google.android.youtube");
            appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            try {
                context.startActivity(appIntent);
                return "YouTube open kar diya jaan! 🎬";
            } catch (Exception e) {
                Intent webIntent = new Intent(Intent.ACTION_VIEW, uri);
                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(webIntent);
                return "YouTube browser me open kar diya jaan! 🎬";
            }
        } catch (Exception e) {
            return "YouTube open nahi ho paya: " + e.getMessage();
        }
    }

    public static String openWhatsApp(Context context, String message) {
        try {
            Intent intent;
            if (message != null && !message.trim().isEmpty()) {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(message.trim())));
            } else {
                intent = context.getPackageManager().getLaunchIntentForPackage("com.whatsapp");
                if (intent == null) {
                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/"));
                }
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return "WhatsApp open kar diya jaan! 💬";
        } catch (Exception e) {
            return "WhatsApp open nahi ho paya: " + e.getMessage();
        }
    }

    public static String openDialer(Context context, String phoneNumber) {
        try {
            Uri uri = (phoneNumber != null && !phoneNumber.trim().isEmpty())
                    ? Uri.parse("tel:" + phoneNumber.trim())
                    : Uri.parse("tel:");
            Intent intent = new Intent(Intent.ACTION_DIAL, uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return "Dialer open kar diya jaan! 📞";
        } catch (Exception e) {
            return "Dialer open nahi ho paya: " + e.getMessage();
        }
    }

    public static String openSettings(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return "Settings open kar di hai jaan! ⚙️";
        } catch (Exception e) {
            return "Settings open nahi ho paya: " + e.getMessage();
        }
    }

    public static String getBatteryStatus(Context context) {
        try {
            BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            if (bm != null) {
                int level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                boolean isCharging = bm.isCharging();
                if (level >= 0) {
                    return "Aapke phone ki battery " + level + "% hai" + (isCharging ? " aur charge ho rahi hai ⚡" : " 🔋") + ", jaan!";
                }
            }
            return "Battery status normal hai jaan! 🔋";
        } catch (Exception e) {
            return "Battery status check nahi ho paya jaan.";
        }
    }

    public static void copyToClipboard(Context context, String text) {
        try {
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                ClipData clip = ClipData.newPlainText("Salony AI", text);
                cm.setPrimaryClip(clip);
                Toast.makeText(context, "Text copy ho gaya! 📋", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception ignored) {}
    }

    public static void shareText(Context context, String text) {
        try {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, text);
            sendIntent.setType("text/plain");
            sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            Intent shareIntent = Intent.createChooser(sendIntent, "Share with Salony");
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(shareIntent);
        } catch (Exception ignored) {}
    }
}
