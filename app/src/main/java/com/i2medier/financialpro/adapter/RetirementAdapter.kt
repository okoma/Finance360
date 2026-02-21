package com.i2medier.financialpro.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.i2medier.financialpro.R
import com.i2medier.financialpro.model.MonthModel
import com.i2medier.financialpro.util.Utils

class RetirementAdapter(
    private val context: Context,
    private var monthModels: ArrayList<MonthModel>,
    private val isMonthly: Boolean
) : RecyclerView.Adapter<RetirementAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_month, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = monthModels[position]
        holder.tvDate.text = if (isMonthly) model.getDate() else model.year.toString()
        holder.tvPrincipal.text = Utils.decimalFormat.format(model.PrincipalAmount)
        holder.tvInterest.text = Utils.decimalFormat.format(model.Interest)
        holder.tvPaid.text = Utils.decimalFormat.format(model.TotalPaid)
        holder.tvBalance.text = Utils.decimalFormat.format(model.Balance)
    }

    override fun getItemCount(): Int = monthModels.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.txtMonth)
        val tvPrincipal: TextView = itemView.findViewById(R.id.txtPrincipal)
        val tvInterest: TextView = itemView.findViewById(R.id.txtInterest)
        val tvPaid: TextView = itemView.findViewById(R.id.txtPaid)
        val tvBalance: TextView = itemView.findViewById(R.id.txtBalance)
    }
}
