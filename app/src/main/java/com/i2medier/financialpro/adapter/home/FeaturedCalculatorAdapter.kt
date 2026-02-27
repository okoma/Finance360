package com.i2medier.financialpro.adapter.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.card.MaterialCardView
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.i2medier.financialpro.R
import com.i2medier.financialpro.ui.CalculatorRegistry

class FeaturedCalculatorAdapter(
    private val items: List<CalculatorRegistry.Item>,
    private val onItemClick: (CalculatorRegistry.Item) -> Unit
) : RecyclerView.Adapter<FeaturedCalculatorAdapter.Holder>() {

    // Define gradient resources to rotate through
    private val gradients = listOf(
        R.drawable.bg_calculator_card_gradient_blue,
        R.drawable.bg_calculator_card_gradient_orange,
        R.drawable.bg_calculator_card_gradient_cyan,
        R.drawable.bg_calculator_card_gradient_indigo
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_featured_calculator, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.subtitle.text = item.subtitle
        holder.icon.setImageResource(item.iconRes)

        // Apply gradient - rotates through different colors
        val gradientRes = gradients[position % gradients.size]
        holder.gradientBackground.setBackgroundResource(gradientRes)

        holder.card.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView.findViewById(R.id.cardCalculator)
        val gradientBackground: View = itemView.findViewById(R.id.calculatorGradientBackground)
        val icon: ImageView = itemView.findViewById(R.id.ivCalculatorIcon)
        val title: TextView = itemView.findViewById(R.id.tvCalculatorTitle)
        val subtitle: TextView = itemView.findViewById(R.id.tvCalculatorSubtitle)
    }
}