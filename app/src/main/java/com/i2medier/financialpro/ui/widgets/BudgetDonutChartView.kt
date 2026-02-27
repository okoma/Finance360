package com.i2medier.financialpro.ui.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.math.min

class BudgetDonutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class ChartSegment(val percentage: Float, val color: Int)

    companion object {
        val UI_DONUT_SPENT_COLOR: Int = Color.parseColor("#FF9800")
        val UI_DONUT_SAVING_COLOR: Int = Color.parseColor("#4DB6AC")
        // Category colors matching skills design
        val UI_DONUT_COLORS: List<Int> = listOf(
            Color.parseColor("#4DB6AC"),  // Teal
            Color.parseColor("#81C784"),  // Green
            Color.parseColor("#FFD54F"),  // Yellow
            Color.parseColor("#FFB74D"),  // Orange
            Color.parseColor("#9575CD"),  // Purple
            Color.parseColor("#64B5F6"),  // Blue
            Color.parseColor("#4FC3F7"),  // Light Blue
            Color.parseColor("#AED581"),  // Lime
            Color.parseColor("#F06292")   // Pink
        )
        val UI_DONUT_REMAINING_COLOR: Int = Color.parseColor("#FF8A65")  // Orange/Red - Remaining/Unspent
        private val UI_PROGRESS_COLOR: Int = Color.parseColor("#42A5F5")
        private val UI_IDLE_SEGMENTS: List<ChartSegment> = listOf(
            ChartSegment(34.29f, UI_DONUT_COLORS[0]),   // Demo: Housing
            ChartSegment(12.86f, UI_DONUT_COLORS[1]),   // Demo: Food
            ChartSegment(5.71f, UI_DONUT_COLORS[2]),    // Demo: Transport
            ChartSegment(7.14f, UI_DONUT_COLORS[3]),    // Demo: Others
            ChartSegment(40f, UI_DONUT_REMAINING_COLOR) // Demo: Remaining
        )
    }

    private val strokeWidthPx = 40f  // Increased for better visibility
    private val insetPx = 20f

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E2E8F0")
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        strokeCap = Paint.Cap.ROUND
    }

    private val fgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        strokeCap = Paint.Cap.ROUND
    }

    private var segments: List<ChartSegment> = emptyList()
    private val drawRect = RectF()

    fun setProgress(value: Float) {
        segments = listOf(ChartSegment(value.coerceIn(0f, 1f) * 100f, UI_PROGRESS_COLOR))
        invalidate()
    }

    fun setData(items: List<ChartSegment>) {
        segments = if (items.isEmpty()) UI_IDLE_SEGMENTS else items
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        val left = ((width - size) / 2f) + insetPx
        val top = ((height - size) / 2f) + insetPx
        val right = width - left
        val bottom = height - top
        drawRect.set(left, top, right, bottom)
        canvas.drawArc(drawRect, -90f, 360f, false, bgPaint)

        val normalizedSegments = normalizedSegments()
        var start = -90f
        for (segment in normalizedSegments) {
            val sweep = (segment.percentage / 100f) * 360f
            if (sweep <= 0f) continue
            fgPaint.color = segment.color
            canvas.drawArc(drawRect, start, sweep, false, fgPaint)
            start += sweep
        }
    }

    private fun normalizedSegments(): List<ChartSegment> {
        val positive = segments
            .filter { it.percentage > 0f }
            .map { ChartSegment(it.percentage.coerceAtMost(100f), it.color) }
        if (positive.isEmpty()) return emptyList()

        val total = positive.sumOf { it.percentage.toDouble() }.toFloat()
        if (total <= 0f) return emptyList()

        if (total <= 100f) return positive

        return positive.map { segment ->
            val normalized = (segment.percentage / total) * 100f
            segment.copy(percentage = max(0f, normalized))
        }
    }
}
