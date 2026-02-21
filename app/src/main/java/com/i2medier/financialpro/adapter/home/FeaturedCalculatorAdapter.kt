package com.i2medier.financialpro.adapter.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.i2medier.financialpro.R
import com.i2medier.financialpro.ui.CalculatorRegistry

class FeaturedCalculatorAdapter(
    private val items: List<CalculatorRegistry.Item>,
    private val onItemClick: (CalculatorRegistry.Item) -> Unit
) : RecyclerView.Adapter<FeaturedCalculatorAdapter.Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_featured_calculator, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.txtName.text = item.title
        holder.imgIcon.setImageResource(item.iconRes)
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgIcon: ImageView = itemView.findViewById(R.id.imgIcon)
        val txtName: TextView = itemView.findViewById(R.id.txtName)
    }
}
