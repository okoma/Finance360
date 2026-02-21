package com.i2medier.financialpro.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.i2medier.financialpro.R;
import com.i2medier.financialpro.model.MonthModel;
import com.i2medier.financialpro.util.Utils;

import java.util.ArrayList;


public class RefinanceAdapter extends RecyclerView.Adapter<RefinanceAdapter.ViewHolder> {
    Context context;
    ArrayList<MonthModel> monthModels;

    public RefinanceAdapter(Context context, ArrayList<MonthModel> arrayList) {
        this.context = context;
        this.monthModels = arrayList;
    }

    public void setList(ArrayList<MonthModel> arrayList) {
        this.monthModels = arrayList;
        notifyDataSetChanged();
    }

    public ArrayList<MonthModel> getMonthModels() {
        return this.monthModels;
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_month, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        viewHolder.txtMonth.setText(String.valueOf(this.monthModels.get(i).getYear()));
        viewHolder.txtPrincipal.setText(Utils.decimalFormat.format(this.monthModels.get(i).getPrincipalAmount()));
        viewHolder.txtInterest.setText(Utils.decimalFormat.format(this.monthModels.get(i).getInterest()));
        viewHolder.txtPaid.setText(Utils.decimalFormat.format(this.monthModels.get(i).getTotalPaid()));
        viewHolder.txtBalance.setText(Utils.decimalFormat.format(this.monthModels.get(i).getBalance()));
        if (i % 2 == 0) {
            viewHolder.llMain.setBackgroundColor(this.context.getResources().getColor(R.color.colorWhite));
        } else {
            viewHolder.llMain.setBackgroundColor(this.context.getResources().getColor(R.color.colorLight));
        }
    }

    @Override
    public int getItemCount() {
        return this.monthModels.size();
    }


    public class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout llMain;
        TextView txtBalance;
        TextView txtInterest;
        TextView txtMonth;
        TextView txtPaid;
        TextView txtPrincipal;

        public ViewHolder(@NonNull View view) {
            super(view);
            this.llMain = (LinearLayout) view.findViewById(R.id.llMain);
            this.txtMonth = (TextView) view.findViewById(R.id.txtMonth);
            this.txtPrincipal = (TextView) view.findViewById(R.id.txtPrincipal);
            this.txtInterest = (TextView) view.findViewById(R.id.txtInterest);
            this.txtPaid = (TextView) view.findViewById(R.id.txtPaid);
            this.txtBalance = (TextView) view.findViewById(R.id.txtBalance);
        }
    }
}
