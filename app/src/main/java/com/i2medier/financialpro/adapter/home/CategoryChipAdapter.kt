package com.i2medier.financialpro.adapter.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.i2medier.financialpro.R

data class CalculatorCategory(
    val id: String,
    val label: String
)

class CategoryChipAdapter(
    private val items: List<CalculatorCategory>,
    private val onItemClick: (CalculatorCategory) -> Unit
) : RecyclerView.Adapter<CategoryChipAdapter.Holder>() {

    private var selectedPosition: Int = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_chip, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.chip.text = item.label

        val isSelected = position == selectedPosition
        val density = holder.itemView.resources.displayMetrics.density

        holder.chip.setBackgroundResource(
            if (isSelected) R.drawable.bg_chip_selected_blue else R.drawable.bg_home_chip_white
        )
        holder.chip.elevation = if (isSelected) 4f * density else 2f * density
        holder.chip.setTextColor(
            ContextCompat.getColor(
                holder.itemView.context,
                if (isSelected) R.color.colorWhite else R.color.colorDark
            )
        )

        holder.itemView.setOnClickListener {
            val newPosition = holder.adapterPosition
            if (newPosition == RecyclerView.NO_POSITION) return@setOnClickListener
            val oldPosition = selectedPosition
            selectedPosition = newPosition
            notifyItemChanged(oldPosition)
            notifyItemChanged(selectedPosition)
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val chip: TextView = itemView.findViewById(R.id.chipText)
    }
}
