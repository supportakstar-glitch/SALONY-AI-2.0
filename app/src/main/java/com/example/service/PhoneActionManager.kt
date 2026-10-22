package com.example.service

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.Settings
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PhoneActionManager(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private var isTorchOn = false

    fun isTorchActive(): Boolean = isTorchOn

    fun setTorch(enable: Boolean): Pair<Boolean, String> {
        val cm = cameraManager ?: return Pair(false, "Camera hardware unavailable")
        return try {
            val cameraId = cm.cameraIdList.firstOrNull { id ->
                val characteristics = cm.getCameraCharacteristics(id)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK
            } ?: cm.cameraIdList.firstOrNull()

            if (cameraId != null) {
                cm.setTorchMode(cameraId, enable)
                isTorchOn = enable
                Pair(true, if (enable) "Torch turned ON 💡" else "Torch turned OFF 🌑")
            } else {
                Pair(false, "No flashlight hardware found")
            }
        } catch (e: CameraAccessException) {
            Pair(false, "Torch error: ${e.localizedMessage}")
        } catch (e: Exception) {
            Pair(false, "Torch error: ${e.localizedMessage}")
        }
    }

    fun openYouTube(query: String? = null): Pair<Boolean, String> {
        return try {
            val intent = if (!query.isNullOrBlank()) {
                val encoded = Uri.encode(query.trim())
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encoded")).apply {
                    setPackage("com.google.android.youtube")
                }
            } else {
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                    setPackage("com.google.android.youtube")
                }
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            // Try launching YouTube app directly
            try {
                context.startActivity(intent)
                Pair(true, "YouTube open kar diya jaan! 🎬")
            } catch (e: Exception) {
                // Fallback to web browser
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(if (!query.isNullOrBlank()) "https://www.youtube.com/results?search_query=${Uri.encode(query.trim())}" else "https://www.youtube.com")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                Pair(true, "YouTube browser me open kar diya jaan! 🎬")
            }
        } catch (e: Exception) {
            Pair(false, "YouTube open karne me problem aayi: ${e.localizedMessage}")
        }
    }

    fun openWhatsApp(message: String? = null): Pair<Boolean, String> {
        return try {
            val intent = if (!message.isNullOrBlank()) {
                Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(message.trim())}")).apply {
                    setPackage("com.whatsapp")
                }
            } else {
                val pm = context.packageManager
                pm.getLaunchIntentForPackage("com.whatsapp") ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/")).apply {
                    setPackage("com.whatsapp")
                }
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            try {
                context.startActivity(intent)
                Pair(true, "WhatsApp open kar diya jaan! 💬")
            } catch (e: Exception) {
                // Fallback to web WhatsApp / Play store
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                Pair(true, "WhatsApp open kar diya! 💬")
            }
        } catch (e: Exception) {
            Pair(false, "WhatsApp open nahi ho paya: ${e.localizedMessage}")
        }
    }

    fun openDialer(phoneNumber: String? = null): Pair<Boolean, String> {
        return try {
            val uri = if (!phoneNumber.isNullOrBlank()) {
                Uri.parse("tel:${phoneNumber.trim()}")
            } else {
                Uri.parse("tel:")
            }
            val intent = Intent(Intent.ACTION_DIAL, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "Dialer open kar diya jaan! 📞")
        } catch (e: Exception) {
            Pair(false, "Dialer open nahi ho saka: ${e.localizedMessage}")
        }
    }

    fun openSettings(): Pair<Boolean, String> {
        return try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "Settings open kar diya jaan! ⚙️")
        } catch (e: Exception) {
            Pair(false, "Settings open nahi ho paya: ${e.localizedMessage}")
        }
    }

    fun getBatteryInfo(): String {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            val isCharging = bm?.isCharging ?: false
            if (level >= 0) {
                val statusText = if (isCharging) "Charging ho raha hai ⚡" else "Battery ${level}% hai 🔋"
                "Aapke phone ki battery $level% hai${if (isCharging) " aur charge ho rahi hai" else ""}, jaan!"
            } else {
                "Phone battery status normal hai jaan! 🔋"
            }
        } catch (e: Exception) {
            "Battery status check nahi ho paya jaan."
        }
    }

    fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("EEEE, d MMMM yyyy, hh:mm a", Locale("hi", "IN"))
        return sdf.format(Date())
    }

    fun copyToClipboard(text: String) {
        try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("Salony AI", text)
            cm?.setPrimaryClip(clip)
            Toast.makeText(context, "Text copy ho gaya! 📋", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // ignore
        }
    }

    fun shareText(text: String) {
        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val shareIntent = Intent.createChooser(sendIntent, "Share with Salony").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            // ignore
        }
    }
}
