package com.example.blescanner.ui.radar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.example.blescanner.model.ScannedDevice
import kotlin.random.Random

class RadarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var devices: List<ScannedDevice> = emptyList()

    // Simpan sudut (angle) untuk setiap MAC agar posisi dot tidak loncat-loncat saat RSSI update
    private val deviceAngles = mutableMapOf<String, Double>()

    private val radarPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    private val dotPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 30f
        isAntiAlias = true
    }

    fun updateDevices(newDevices: List<ScannedDevice>) {
        this.devices = newDevices
        // Assign random angle untuk device baru
        newDevices.forEach { device ->
            if (!deviceAngles.containsKey(device.mac)) {
                deviceAngles[device.mac] =
                    Random.nextDouble(0.0, 2 * Math.PI) // 0 - 360 derajat dalam radian
            }
        }
        invalidate() // Perintahkan view untuk menggambar ulang (memanggil onDraw)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val maxRadius = (Math.min(width, height) / 2f) * 0.9f // 90% dari setengah ukuran terkecil

    // Gambar lingkaran radar 3 Lapis

        canvas.drawCircle(centerX, centerY,  maxRadius * 0.33f, radarPaint)
        canvas.drawCircle(centerX, centerY, maxRadius * 0.66f, radarPaint)
        canvas.drawCircle(centerX, centerY, maxRadius, radarPaint)

        // Gambar garis silang radar
        canvas.drawLine(centerX,0f,centerX,height.toFloat(),radarPaint)
        canvas.drawLine(0f,centerY,width.toFloat(),centerY,radarPaint)

        //Plot setiap perangkat
        for (device in devices) {
            val angle = deviceAngles[device.mac] ?: continue

            // Konversi RSSI ke jarak (Semakin mendekati 0 = semakin dekat ke center)
            // Asumsi RSSI terlemah -100 (pinggir), terkuat -30 (tengah)

            var normalizedRssi = (device.rssi + 100) / 70f // Range 0.0 (jauh) ke 1.0 (dekat)
            if (normalizedRssi < 0f) normalizedRssi = 0f
            if (normalizedRssi > 1f) normalizedRssi = 1f

            // Hitung kordinat X dan Y menggunakan Trigonometri
            val x = centerX + normalizedRssi * maxRadius * Math.cos(angle).toFloat()
            val y = centerY + normalizedRssi * maxRadius * Math.sin(angle).toFloat()

        // Gambar titik perangkat
            canvas.drawCircle(x, y, 10f, dotPaint)

            //Tulis nama perangkat/MAC di sekitar titik
            val displayNAme =device.name.ifBlank { "Unknown" }
            canvas.drawText(displayNAme, x + 20f, y, textPaint)
        }

    }
}

