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

class RefinanceAdapter(
    private val context: Context,
    var monthModels: ArrayList<MonthModel>
) : RecyclerView.Adapter<RefinanceAdapter.ViewHolder>() {

    fun setList(arrayList: ArrayList<MonthModel>) {
        this.monthModels = arrayList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_month, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = monthModels[position]
        holder.txtMonth.text = model.year.toString()
        holder.txtPrincipal.text = Utils.decimalFormat.format(model.PrincipalAmount)
        holder.txtInterest.text = Utils.decimalFormat.format(model.Interest)
        holder.txtPaid.text = Utils.decimalFormat.format(model.TotalPaid)
        holder.txtBalance.text = Utils.decimalFormat.format(model.Balance)
        holder.llMain.setBackgroundColor(
            if (position % 2 == 0) ContextCompat.getColor(context, R.color.colorWhite)
            else ContextCompat.getColor(context, R.color.colorLight)
        )
    }

    override fun getItemCount(): Int = monthModels.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val llMain: LinearLayout = itemView.findViewById(R.id.llMain)
        val txtMonth: TextView = itemView.findViewById(R.id.txtMonth)
        val txtPrincipal: TextView = itemView.findViewById(R.id.txtPrincipal)
        val txtInterest: TextView = itemView.findViewById(R.id.txtInterest)
        val txtPaid: TextView = itemView.findViewById(R.id.txtPaid)
        val txtBalance: TextView = itemView.findViewById(R.id.txtBalance)
    }
}
