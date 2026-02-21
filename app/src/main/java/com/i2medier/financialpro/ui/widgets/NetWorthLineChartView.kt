package com.i2medier.financialpro.ui.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

class NetWorthLineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class DataPoint(val label: String, val value: Float)

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1D4ED8")
        strokeWidth = 6f
        style = Paint.Style.STROKE
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1D4ED8")
        style = Paint.Style.FILL
    }

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CBD5E1")
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#334155")
        textSize = 28f
    }

    private var points: List<DataPoint> = emptyList()
    private var trendPositive: Boolean = true

    fun setData(data: List<DataPoint>) {
        points = data
        invalidate()
    }

    fun setTrendPositive(value: Boolean) {
        trendPositive = value
        linePaint.color = if (value) Color.parseColor("#16A34A") else Color.parseColor("#DC2626")
        pointPaint.color = linePaint.color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (points.isEmpty()) return

        val left = paddingLeft + 24f
        val right = width - paddingRight - 24f
        val top = paddingTop + 24f
        val bottom = height - paddingBottom - 48f

        canvas.drawLine(left, bottom, right, bottom, axisPaint)

        val maxValue = max(points.maxOf { it.value.toDouble() }, 1.0)
        val stepX = if (points.size > 1) (right - left) / (points.size - 1) else 0f

        val path = Path()
        points.forEachIndexed { index, point ->
            val x = left + index * stepX
            val y = bottom - ((point.value / maxValue) * (bottom - top)).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            canvas.drawCircle(x, y, 7f, pointPaint)
            canvas.drawText(point.label, x - 20f, bottom + 30f, textPaint)
        }

        canvas.drawPath(path, linePaint)
    }
}
