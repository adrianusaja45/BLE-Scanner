package com.example.blescanner.ui.radar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.example.blescanner.model.ScannedDevice
import com.example.blescanner.utils.RssiConverter
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class RadarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private companion object {
        // Radius terluar memakai 90% dari setengah sisi terkecil supaya lingkaran
        // tidak menempel tepi view.
        const val MAX_RADIUS_RATIO = 0.9f

        // Lingkaran dalam dan tengah membagi radius menjadi tiga bagian sama besar.
        const val INNER_RING_RATIO = MAX_RADIUS_RATIO / 3f
        const val MIDDLE_RING_RATIO = INNER_RING_RATIO * 2f

        // Radius titik perangkat, dalam piksel.
        const val DOT_RADIUS_PX = 15f

        // Jarak teks nama dari titik perangkat, dalam piksel.
        const val LABEL_OFFSET_PX = 20f

        // RSSI terlemah dipetakan ke tepi luar, terkuat ke titik tengah.
        // Selisih keduanya menjadi rentang normalisasi sinyal.
        const val RSSI_FLOOR = -100
        const val RSSI_RANGE = 70f
    }

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
        val maxRadius = (Math.min(width, height) / 2f) * MAX_RADIUS_RATIO

        // Gambar lingkaran radar 3 Lapis
        canvas.drawCircle(centerX, centerY, maxRadius * INNER_RING_RATIO, radarPaint)
        canvas.drawCircle(centerX, centerY, maxRadius * MIDDLE_RING_RATIO, radarPaint)
        canvas.drawCircle(centerX, centerY, maxRadius, radarPaint)

        // Gambar garis silang radar
        canvas.drawLine(centerX, 0f, centerX, height.toFloat(), radarPaint)
        canvas.drawLine(0f, centerY, width.toFloat(), centerY, radarPaint)

        //Plot setiap perangkat
        for (device in devices) {
            val angle = deviceAngles[device.mac] ?: continue

            // Konversi RSSI ke jarak (Semakin mendekati 0 = semakin dekat ke center)
            // RSSI terlemah ke tepi luar, terkuat ke titik tengah.
            var normalizedRssi = (device.rssi - RSSI_FLOOR) / RSSI_RANGE
            if (normalizedRssi < 0f) normalizedRssi = 0f
            if (normalizedRssi > 1f) normalizedRssi = 1f

            // INVERSI JARAK: Sinyal kuat (1.0) -> jarak 0. Sinyal lemah (0.0) -> jarak maxRadius
            val distanceRadius = maxRadius * (1f - normalizedRssi)

            // Hitung kordinat X dan Y menggunakan Trigonometri
            val x = centerX + distanceRadius * cos(angle).toFloat()
            val y = centerY + distanceRadius * sin(angle).toFloat()

            //ambil warna dinamis dari helper berdasarkan nilai RSSI
            dotPaint.color = RssiConverter.getSignalColor(device.rssi)

            // Gambar titik perangkat
            canvas.drawCircle(x, y, DOT_RADIUS_PX, dotPaint)

            //Tulis nama perangkat/MAC di sekitar titik
            val displayName = device.name.ifBlank { "Unknown" }
            canvas.drawText(displayName, x + LABEL_OFFSET_PX, y, textPaint)
        }
    }
}
