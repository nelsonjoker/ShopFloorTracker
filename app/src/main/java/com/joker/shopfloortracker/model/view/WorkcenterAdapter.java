package com.joker.shopfloortracker.model.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.joker.shopfloortracker.model.Workcenter;

import java.util.List;


/**
 * Created by Nelson on 15/03/2018.
 */
public class WorkcenterAdapter extends ArrayAdapter<Workcenter> {

    private Context mContext;
    private List<Workcenter> mWorkcenter;

    public WorkcenterAdapter(@NonNull Context context, List<Workcenter> list) {
        super(context, android.R.layout.simple_dropdown_item_1line, list);
        mContext = context;
        mWorkcenter = list;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        //return super.getDropDownView(position, convertView, parent);
        View listItem = convertView;
        if (listItem == null)
            listItem = LayoutInflater.from(mContext).inflate(android.R.layout.simple_dropdown_item_1line, parent, false);

        Workcenter wcr = mWorkcenter.get(position);

        TextView txt = (TextView) listItem.findViewById(android.R.id.text1);
        txt.setText(wcr.getCode() + " - " + wcr.getDescription().trim());


        txt.setTag(wcr);

        return listItem;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if (listItem == null)
            listItem = LayoutInflater.from(mContext).inflate(android.R.layout.simple_dropdown_item_1line, parent, false);

        Workcenter wcr = mWorkcenter.get(position);

        TextView txt = (TextView) listItem.findViewById(android.R.id.text1);
        txt.setText(wcr.getCode() + " - " + wcr.getDescription().trim());


        txt.setTag(wcr);

        return listItem;
    }
}
