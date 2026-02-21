package com.i2medier.financialpro.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.i2medier.financialpro.R;
import com.i2medier.financialpro.Interface.RemoveEditText;

import java.util.ArrayList;


public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {
    Context context;
    ArrayList<String> doubleArrayList;
    RemoveEditText removeEditText;

    public RecyclerAdapter(Context context, ArrayList<String> arrayList, RemoveEditText removeEditText) {
        this.context = context;
        this.doubleArrayList = arrayList;
        this.removeEditText = removeEditText;
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(this.context).inflate(R.layout.recycler_item, viewGroup, false), new MyCustomEditTextListener());
    }

    @Override
    @SuppressLint({"SetTextI18n"})
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        TextView textView = viewHolder.txtYear;
        textView.setText("Year " + (i + 1));
        viewHolder.myCustomEditTextListener.updatePosition(viewHolder.getAdapterPosition());
        viewHolder.etYear.setText(String.valueOf(this.doubleArrayList.get(viewHolder.getAdapterPosition())));
    }

    @Override
    public int getItemCount() {
        return this.doubleArrayList.size();
    }



    public class ViewHolder extends RecyclerView.ViewHolder {
        EditText etYear;
        LinearLayout linYear;
        MyCustomEditTextListener myCustomEditTextListener;
        ImageView remove;
        TextView txtYear;

        public ViewHolder(@NonNull View view, MyCustomEditTextListener myCustomEditTextListener) {
            super(view);
            this.linYear = (LinearLayout) view.findViewById(R.id.linYear);
            this.txtYear = (TextView) view.findViewById(R.id.txtYear);
            this.etYear = (EditText) view.findViewById(R.id.etYear);
            this.remove = (ImageView) view.findViewById(R.id.remove);
            this.myCustomEditTextListener = myCustomEditTextListener;
            this.etYear.addTextChangedListener(myCustomEditTextListener);
            this.remove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view2) {
                    RecyclerAdapter.this.removeEditText.removePosition(ViewHolder.this.getAdapterPosition());
                }
            });
        }
    }



    public class MyCustomEditTextListener implements TextWatcher {
        private int position;

        @Override
        public void afterTextChanged(Editable editable) {
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        private MyCustomEditTextListener() {
        }

        public void updatePosition(int i) {
            this.position = i;
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            try {
                RecyclerAdapter.this.doubleArrayList.set(this.position, charSequence.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
