package com.i2medier.financialpro.ui.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class BudgetDonutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class ChartSegment(val percentage: Float, val color: Int)

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E2E8F0")
        style = Paint.Style.STROKE
        strokeWidth = 28f
    }

    private val fgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 28f
        strokeCap = Paint.Cap.BUTT
    }

    private var segments: List<ChartSegment> = emptyList()

    fun setProgress(value: Float) {
        segments = listOf(ChartSegment(value.coerceIn(0f, 1f) * 100f, Color.parseColor("#0EA5E9")))
        invalidate()
    }

    fun setData(items: List<ChartSegment>) {
        segments = items
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        val pad = 24f
        val rect = RectF(pad, pad, size - pad, size - pad)
        canvas.drawArc(rect, -90f, 360f, false, bgPaint)

        var start = -90f
        for (segment in segments) {
            val sweep = (segment.percentage / 100f) * 360f
            if (sweep <= 0f) continue
            fgPaint.color = segment.color
            canvas.drawArc(rect, start, sweep, false, fgPaint)
            start += sweep
        }
    }
}
