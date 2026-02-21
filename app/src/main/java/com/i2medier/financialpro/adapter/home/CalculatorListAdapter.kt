package com.i2medier.financialpro.adapter.home

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.i2medier.financialpro.AdAdmob
import com.i2medier.financialpro.R
import com.i2medier.financialpro.ui.CalculatorRegistry

class CalculatorListAdapter(
    private val items: List<CalculatorRegistry.Item>,
    private val onItemClick: (CalculatorRegistry.Item) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private sealed class Row {
        data class CalculatorRow(val item: CalculatorRegistry.Item) : Row()
        data class AdRow(val id: String) : Row()
    }

    private val rows = mutableListOf<Row>().apply {
        items.forEachIndexed { index, item ->
            add(Row.CalculatorRow(item))
            if ((index + 1) % 4 == 0 && index != items.lastIndex) {
                add(Row.AdRow("calc_ad_$index"))
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_CALCULATOR = 1
        private const val VIEW_TYPE_AD = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (rows[position]) {
            is Row.CalculatorRow -> VIEW_TYPE_CALCULATOR
            is Row.AdRow -> VIEW_TYPE_AD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_AD) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_inline_native_ad_calculator_list, parent, false)
            AdViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_calculator_list, parent, false)
            Holder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is Row.CalculatorRow -> (holder as Holder).bind(row.item)
            is Row.AdRow -> (holder as AdViewHolder).bind()
        }
    }

    override fun getItemCount(): Int = rows.size

    class AdViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val adFrame: FrameLayout = itemView.findViewById(R.id.nativeAdFrame)
        private var loaded = false

        fun bind() {
            if (loaded) return
            val activity = itemView.context as? Activity ?: return
            AdAdmob(activity).NativeAd(adFrame, activity)
            loaded = true
        }
    }

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgCalcIcon: ImageView = itemView.findViewById(R.id.imgCalcIcon)
        private val txtCalcName: TextView = itemView.findViewById(R.id.txtCalcName)
        private val txtCalcDesc: TextView = itemView.findViewById(R.id.txtCalcDesc)

        fun bind(item: CalculatorRegistry.Item) {
            txtCalcName.text = item.title
            txtCalcDesc.text = item.subtitle
            imgCalcIcon.setImageResource(item.iconRes)
            itemView.setOnClickListener { onItemClick(item) }
        }
    }
}
