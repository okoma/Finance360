package com.i2medier.financialpro.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.i2medier.financialpro.R
import com.i2medier.financialpro.model.MonthModel
import com.i2medier.financialpro.util.Utils

class SimpleInterestAdapter(
    private val context: Context,
    private var monthModels: ArrayList<MonthModel>
) : RecyclerView.Adapter<SimpleInterestAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_simple_intrest, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = monthModels[position]
        holder.txtYear.text = Utils.decimalFormat.format(model.year)
        holder.txtInterest.text = Utils.decimalFormat.format(model.Interest)
        holder.txtBalance.text = Utils.decimalFormat.format(model.Balance)
        holder.llMain.setBackgroundColor(
            if (position % 2 == 0) ContextCompat.getColor(context, R.color.colorWhite)
            else ContextCompat.getColor(context, R.color.colorLight)
        )
    }

    override fun getItemCount(): Int = monthModels.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val llMain: LinearLayout = itemView.findViewById(R.id.llMain)
        val txtYear: TextView = itemView.findViewById(R.id.txtYear)
        val txtInterest: TextView = itemView.findViewById(R.id.txtInterest)
        val txtBalance: TextView = itemView.findViewById(R.id.txtBalance)
    }
}
