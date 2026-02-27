package com.i2medier.financialpro.adapter.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
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

        holder.chip.chipBackgroundColor = ContextCompat.getColorStateList(
            holder.itemView.context,
            if (isSelected) R.color.nav_blue else R.color.colorWhite
        )
        holder.chip.chipStrokeWidth = 0f
        holder.chip.elevation = if (isSelected) 0f else 6f
        holder.chip.setTextColor(
            ContextCompat.getColor(
                holder.itemView.context,
                if (isSelected) R.color.colorWhite else R.color.colorDark
            )
        )
        holder.chip.isChecked = isSelected

        holder.chip.setOnClickListener {
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
        val chip: Chip = itemView.findViewById(R.id.chip)
    }
}
