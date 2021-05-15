package com.joker.shopfloortracker.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import com.joker.shopfloortracker.R;


public class SpinnerLineAdapter<T extends SpinnerLineAdapter.SpinnerLineAdapterItem> extends ArrayAdapter<T> {

    public interface SpinnerLineAdapterItem{

        String getTitle();
        String getDescription();

    }


    private List<T> values;

    public SpinnerLineAdapter(Context ctx, List<T> values)
    {
        super(ctx, R.layout.spinner_item_line_main, values);
        this.values = values;
    }

    public void setValue(List<T> list)
    {
        this.values = list;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return values.size();
    }

    @Override
    public T getItem(int position) {
        return values.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }



    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View itemView = convertView;
        ViewHolder viewHolder;

        Context context = parent.getContext();

        if (convertView == null) {
            itemView = LayoutInflater.from(context).inflate(R.layout.spinner_item_line_main, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.textView = itemView.findViewById(R.id.text_view);
            viewHolder.textViewDescription = itemView.findViewById(R.id.text_view_description);

            itemView.setTag(viewHolder);

        } else {
            viewHolder = (ViewHolder) itemView.getTag();
        }

        T v = values.get(position);
        viewHolder.textView.setText(v.getTitle());
        viewHolder.textViewDescription.setText(v.getDescription().trim());

        return itemView;
    }

    @Override
    public View getDropDownView(int position, View convertView,ViewGroup parent) {
        View itemView = convertView;
        ViewHolder viewHolder;

        Context context = parent.getContext();

        if (convertView == null) {
            itemView = LayoutInflater.from(context).inflate(R.layout.spinner_item_line_drop, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.textView = itemView.findViewById(R.id.text_view);
            viewHolder.textViewDescription = itemView.findViewById(R.id.text_view_description);
            itemView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) itemView.getTag();
        }

        T v = values.get(position);
        viewHolder.textView.setText(v.getTitle());
        viewHolder.textViewDescription.setText(v.getDescription().trim());

        return itemView;
    }

    private static class ViewHolder {
        TextView textView;
        TextView textViewDescription;
    }
}