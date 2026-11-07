package org.miniopensource.miniclassisland

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageFormat
import android.hardware.Camera
import android.net.Uri
import android.os.Build
import android.view.Gravity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.EnumMap
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("DEPRECATION")
object QrScanner {
    const val REQ_CAMERA_PERM = 9101
    const val REQ_PICK_QR_IMAGE = 9102

    private var pendingCallback: ((String) -> Unit)? = null
    private var activeDialog: AlertDialog? = null
    private var activeCamera: Camera? = null

    private fun buildReader(): MultiFormatReader {
        val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java)
        hints[DecodeHintType.POSSIBLE_FORMATS] = listOf(BarcodeFormat.QR_CODE)
        hints[DecodeHintType.CHARACTER_SET] = "UTF-8"
        hints[DecodeHintType.TRY_HARDER] = true
        return MultiFormatReader().apply { setHints(hints) }
    }

    fun startScan(activity: MainActivity, onScanned: (String) -> Unit) {
        pendingCallback = onScanned
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            activity.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        ) {
            activity.requestPermissions(arrayOf(Manifest.permission.CAMERA), REQ_CAMERA_PERM)
            return
        }
        showScannerDialog(activity)
    }

    fun onRequestPermissionsResult(
        activity: MainActivity,
        requestCode: Int,
        grantResults: IntArray
    ) {
        if (requestCode != REQ_CAMERA_PERM) return
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            showScannerDialog(activity)
        } else {
            showScannerDialog(activity, cameraDenied = true)
        }
    }

    fun onActivityResult(
        activity: MainActivity,
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        if (requestCode != REQ_PICK_QR_IMAGE || resultCode != android.app.Activity.RESULT_OK) return
        val uri = data?.data ?: return
        decodeQrFromImageUri(activity, uri)
    }

    private fun decodeQrFromImageUri(activity: MainActivity, uri: Uri) {
        activity.runAsync(
            work = {
                try {
                    activity.contentResolver.openInputStream(uri)?.use { stream ->
                        val bmp = BitmapFactory.decodeStream(stream) ?: return@use null
                        val w = bmp.width
                        val h = bmp.height
                        val pixels = IntArray(w * h)
                        bmp.getPixels(pixels, 0, w, 0, 0, w, h)
                        val source = RGBLuminanceSource(w, h, pixels)
                        val bitmap = BinaryBitmap(HybridBinarizer(source))
                        buildReader().decodeWithState(bitmap)?.text
                    }
                } catch (_: Exception) {
                    null
                }
            },
            done = { decoded ->
                if (!decoded.isNullOrBlank()) {
                    dismissScanner()
                    val cb = pendingCallback
                    pendingCallback = null
                    cb?.invoke(decoded.trim())
                } else {
                    activity.setFeedback("未能从所选图片中识别出二维码", true)
                }
            }
        )
    }

    private fun dismissScanner() {
        try {
            activeCamera?.setOneShotPreviewCallback(null)
            activeCamera?.stopPreview()
            activeCamera?.release()
        } catch (_: Exception) {
        }
        activeCamera = null
        try {
            activeDialog?.dismiss()
        } catch (_: Exception) {
        }
        activeDialog = null
    }

    private fun showScannerDialog(activity: MainActivity, cameraDenied: Boolean = false) {
        dismissScanner()
        val decodedFlag = AtomicBoolean(false)
        val root = UiKit.vBox(activity, 16).apply {
            background = UiKit.roundedBg(UiKit.BG_CARD, 12, activity, UiKit.BORDER_COLOR, 1)
        }

        val head = UiKit.hBox(activity)
        head.addView(OfficialIcons.imageView(activity, MdsIcon.QR_CODE, UiKit.ACCENT_BLUE, 20).apply {
            (layoutParams as LinearLayout.LayoutParams).rightMargin = UiKit.dp(activity, 8)
        })
        head.addView(UiKit.label(activity, "扫描二维码", 15f, UiKit.TEXT_PRIMARY, true).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        head.addView(UiKit.iconButton(activity, MdsIcon.CLOSE, UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, 28) {
            dismissScanner()
        })
        root.addView(head)

        root.addView(UiKit.label(
            activity,
            "将取景框对准安全证书二维码或网页端登录二维码即可自动识别，也支持从相册选择二维码图片。",
            12f,
            UiKit.TEXT_SECONDARY
        ).apply {
            setPadding(0, UiKit.dp(activity, 4), 0, UiKit.dp(activity, 10))
        })

        if (!cameraDenied) {
            val previewWrap = FrameLayout(activity).apply {
                background = UiKit.roundedBg(Color.BLACK, 10, activity, UiKit.ACCENT_BLUE, 2)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    UiKit.dp(activity, 260)
                ).apply {
                    bottomMargin = UiKit.dp(activity, 12)
                }
            }
            val surfaceView = SurfaceView(activity)
            previewWrap.addView(
                surfaceView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            val aimBox = FrameLayout(activity).apply {
                background = UiKit.roundedBg(Color.TRANSPARENT, 12, activity, UiKit.ACCENT_BLUE, 2)
                layoutParams = FrameLayout.LayoutParams(
                    UiKit.dp(activity, 180),
                    UiKit.dp(activity, 180),
                    Gravity.CENTER
                )
            }
            previewWrap.addView(aimBox)
            root.addView(previewWrap)

            val reader = buildReader()
            val callback = object : Camera.PreviewCallback {
                override fun onPreviewFrame(data: ByteArray?, cam: Camera?) {
                    if (data == null || cam == null || decodedFlag.get()) return
                    val params = try {
                        cam.parameters
                    } catch (_: Exception) {
                        return
                    }
                    val size = params.previewSize ?: return
                    val w = size.width
                    val h = size.height
                    if (w <= 0 || h <= 0 || data.size < w * h) return

                    var resultText: String? = null
                    try {
                        val src = PlanarYUVLuminanceSource(data, w, h, 0, 0, w, h, false)
                        resultText = reader.decodeWithState(BinaryBitmap(HybridBinarizer(src)))?.text
                    } catch (_: Exception) {
                        reader.reset()
                    }
                    if (resultText == null) {
                        try {
                            val rotated = ByteArray(w * h)
                            for (y in 0 until h) {
                                for (x in 0 until w) {
                                    rotated[x * h + h - y - 1] = data[y * w + x]
                                }
                            }
                            val srcRot = PlanarYUVLuminanceSource(rotated, h, w, 0, 0, h, w, false)
                            resultText = reader.decodeWithState(BinaryBitmap(HybridBinarizer(srcRot)))?.text
                        } catch (_: Exception) {
                            reader.reset()
                        }
                    }

                    if (!resultText.isNullOrBlank() && decodedFlag.compareAndSet(false, true)) {
                        val finalText = resultText.trim()
                        activity.mainHandler.post {
                            dismissScanner()
                            val cb = pendingCallback
                            pendingCallback = null
                            cb?.invoke(finalText)
                        }
                    } else if (!decodedFlag.get()) {
                        try {
                            cam.setOneShotPreviewCallback(this)
                        } catch (_: Exception) {
                        }
                    }
                }
            }

            surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    try {
                        val cam = Camera.open()
                        activeCamera = cam
                        cam.setDisplayOrientation(90)
                        val p = cam.parameters
                        p.previewFormat = ImageFormat.NV21
                        if (p.supportedFocusModes?.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) == true) {
                            p.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                        }
                        cam.parameters = p
                        cam.setPreviewDisplay(holder)
                        cam.startPreview()
                        cam.setOneShotPreviewCallback(callback)
                    } catch (_: Exception) {
                    }
                }

                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    try {
                        activeCamera?.setOneShotPreviewCallback(null)
                        activeCamera?.stopPreview()
                        activeCamera?.release()
                    } catch (_: Exception) {
                    }
                    activeCamera = null
                }
            })
        } else {
            root.addView(UiKit.label(
                activity,
                "未授予相机权限，您可以从系统相册选择包含二维码的图片进行识别。",
                12f,
                UiKit.BADGE_DANGER_FG
            ).apply {
                setPadding(0, 0, 0, UiKit.dp(activity, 8))
            })
        }

        val actionRow = UiKit.hBox(activity)
        actionRow.addView(UiKit.iconTextButton(
            activity,
            MdsIcon.QR_CODE,
            "从相册选择二维码图片",
            UiKit.ACCENT_BLUE,
            Color.WHITE,
            true
        ) {
            try {
                val pick = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "image/*"
                }
                activity.startActivityForResult(pick, REQ_PICK_QR_IMAGE)
            } catch (_: Exception) {
                activity.setFeedback("无法打开系统相册", true)
            }
        }.apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = UiKit.dp(activity, 8)
            }
        })
        actionRow.addView(UiKit.button(activity, "取消", UiKit.BTN_MUTED, UiKit.TEXT_PRIMARY, true) {
            dismissScanner()
        })
        root.addView(actionRow)

        val dlg = AlertDialog.Builder(activity)
            .setView(root)
            .setOnDismissListener {
                try {
                    activeCamera?.setOneShotPreviewCallback(null)
                    activeCamera?.stopPreview()
                    activeCamera?.release()
                } catch (_: Exception) {
                }
                activeCamera = null
            }
            .create()
        dlg.window?.setBackgroundDrawableResource(android.R.color.transparent)
        activeDialog = dlg
        dlg.show()
    }
}
