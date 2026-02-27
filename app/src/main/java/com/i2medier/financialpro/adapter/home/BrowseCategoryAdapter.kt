package com.i2medier.financialpro.adapter.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.i2medier.financialpro.R

data class BrowseCategory(
    val id: String,
    val title: String,
    val subtitle: String,
    val toolCount: Int,
    val iconRes: Int,
    val colorRes: Int
)

class BrowseCategoryAdapter(
    private val items: List<BrowseCategory>,
    private val onItemClick: (BrowseCategory) -> Unit
) : RecyclerView.Adapter<BrowseCategoryAdapter.Holder>() {

    // Define gradient resources to rotate through
    private val gradients = listOf(
        R.drawable.bg_category_card_gradient_purple,
        R.drawable.bg_category_card_gradient_rose,
        R.drawable.bg_finance_card_gradient_layered,
        R.drawable.bg_calculator_card_gradient_indigo
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_browse_category, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.icon.setImageResource(item.iconRes)
        holder.title.text = item.title
        holder.subtitle.text = item.subtitle
        holder.toolCount.text = holder.itemView.context.getString(R.string.home_tools_count, item.toolCount)

        // Apply gradient - rotates through different colors
        val gradientRes = gradients[position % gradients.size]
        holder.gradientBackground.setBackgroundResource(gradientRes)

        // Note: colorRes from BrowseCategory is no longer used since we're using gradients
        // If you want to keep using custom colors per category, you can map them:
        // holder.gradientBackground.setBackgroundResource(getGradientForColor(item.colorRes))

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView.findViewById(R.id.cardCategory)
        val gradientBackground: View = itemView.findViewById(R.id.categoryGradientBackground)
        val icon: ImageView = itemView.findViewById(R.id.ivCategoryIcon)
        val title: TextView = itemView.findViewById(R.id.tvCategoryTitle)
        val subtitle: TextView = itemView.findViewById(R.id.tvCategorySubtitle)
        val toolCount: TextView = itemView.findViewById(R.id.tvToolCount)
    }
}