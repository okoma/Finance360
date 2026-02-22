package com.i2medier.financialpro.ui.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import com.i2medier.financialpro.util.CurrencyManager

class NetWorthLineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class DataPoint(val label: String, val value: Float)

    private var lineColor = Color.parseColor("#10B981")
    private var fillStartColor = Color.parseColor("#A7F3D0")

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = lineColor
        strokeWidth = 6f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = lineColor
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E2E8F0")
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#64748B")
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    private var points: List<DataPoint> = emptyList()

    fun setData(data: List<DataPoint>) {
        points = data
        invalidate()
    }

    fun setTrendPositive(value: Boolean) {
        lineColor = if (value) Color.parseColor("#10B981") else Color.parseColor("#EF4444")
        fillStartColor = if (value) Color.parseColor("#A7F3D0") else Color.parseColor("#FECACA")
        linePaint.color = lineColor
        pointPaint.color = lineColor
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (points.isEmpty()) return

        val left = paddingLeft + 60f
        val right = width - paddingRight - 40f
        val top = paddingTop + 40f
        val bottom = height - paddingBottom - 60f
        val chartWidth = right - left
        val chartHeight = bottom - top
        if (chartWidth <= 0f || chartHeight <= 0f) return

        val maxValue = points.maxOf { it.value }
        val minValue = points.minOf { it.value }
        val valueRange = (maxValue - minValue).takeIf { it > 0f } ?: 1f

        val ySteps = 3
        for (i in 0..ySteps) {
            val y = top + chartHeight - (chartHeight / ySteps * i)
            canvas.drawLine(left, y, right, y, gridPaint)
            val labelValue = minValue + (valueRange / ySteps * i)
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(formatAxisLabel(labelValue), left - 10f, y + 10f, textPaint)
        }

        val plotPoints = mutableListOf<PointF>()
        points.forEachIndexed { index, point ->
            val x = if (points.size > 1) left + (chartWidth / (points.size - 1)) * index else left + chartWidth / 2f
            val normalizedValue = (point.value - minValue) / valueRange
            val y = top + chartHeight - (chartHeight * normalizedValue)
            plotPoints += PointF(x, y)
        }

        if (plotPoints.isNotEmpty()) {
            val fillPath = Path().apply {
                moveTo(plotPoints.first().x, bottom)
                lineTo(plotPoints.first().x, plotPoints.first().y)
                for (i in 0 until plotPoints.size - 1) {
                    val current = plotPoints[i]
                    val next = plotPoints[i + 1]
                    val controlX = (current.x + next.x) / 2f
                    cubicTo(controlX, current.y, controlX, next.y, next.x, next.y)
                }
                lineTo(plotPoints.last().x, bottom)
                close()
            }
            fillPaint.shader = LinearGradient(
                0f,
                top,
                0f,
                bottom,
                fillStartColor,
                Color.WHITE,
                Shader.TileMode.CLAMP
            )
            canvas.drawPath(fillPath, fillPaint)
        }

        val path = Path()
        if (plotPoints.isNotEmpty()) {
            path.moveTo(plotPoints.first().x, plotPoints.first().y)
            for (i in 0 until plotPoints.size - 1) {
                val current = plotPoints[i]
                val next = plotPoints[i + 1]
                val controlX = (current.x + next.x) / 2f
                path.cubicTo(controlX, current.y, controlX, next.y, next.x, next.y)
            }
        }
        canvas.drawPath(path, linePaint)

        if (plotPoints.isNotEmpty()) {
            val lastPoint = plotPoints.last()
            canvas.drawCircle(lastPoint.x, lastPoint.y, 12f, pointPaint)
            val innerPointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawCircle(lastPoint.x, lastPoint.y, 6f, innerPointPaint)
        }

        textPaint.textAlign = Paint.Align.CENTER
        points.forEachIndexed { index, point ->
            val x = if (points.size > 1) left + (chartWidth / (points.size - 1)) * index else left + chartWidth / 2f
            canvas.drawText(point.label, x, bottom + 40f, textPaint)
        }
    }

    private fun formatAxisLabel(value: Float): String {
        val abs = kotlin.math.abs(value)
        val sign = if (value < 0f) "-" else ""
        val symbol = CurrencyManager.getCurrencySymbol(context)
        return if (abs >= 1000f) {
            "${sign}${symbol}${(abs / 1000f).toInt()}k"
        } else {
            "${sign}${symbol}${abs.toInt()}"
        }
    }
}
