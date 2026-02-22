package com.i2medier.financialpro.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.i2medier.financialpro.Interface.RemoveEditText
import com.i2medier.financialpro.R

class RecyclerAdapter(
    private val context: Context,
    private val doubleArrayList: ArrayList<String>,
    private val removeEditText: RemoveEditText
) : RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.recycler_item, parent, false)
        return ViewHolder(view, MyCustomEditTextListener())
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.txtYear.text = "Year ${position + 1}"
        holder.myCustomEditTextListener.updatePosition(holder.adapterPosition)
        holder.etYear.setText(doubleArrayList[holder.adapterPosition])
    }

    override fun getItemCount(): Int = doubleArrayList.size

    inner class ViewHolder(itemView: View, val myCustomEditTextListener: MyCustomEditTextListener) :
        RecyclerView.ViewHolder(itemView) {
        val linYear: LinearLayout = itemView.findViewById(R.id.linYear)
        val txtYear: TextView = itemView.findViewById(R.id.txtYear)
        val etYear: EditText = itemView.findViewById(R.id.etYear)
        val remove: ImageView = itemView.findViewById(R.id.remove)

        init {
            etYear.addTextChangedListener(myCustomEditTextListener)
            remove.setOnClickListener { removeEditText.removePosition(adapterPosition) }
        }
    }

    inner class MyCustomEditTextListener : TextWatcher {
        private var position: Int = 0

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            try {
                doubleArrayList[position] = s?.toString().orEmpty()
            } catch (_: Exception) {
            }
        }

        override fun afterTextChanged(s: Editable?) = Unit

        fun updatePosition(position: Int) {
            this.position = position
        }
    }
}
