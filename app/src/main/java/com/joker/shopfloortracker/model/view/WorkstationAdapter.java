package com.joker.shopfloortracker.model.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.joker.shopfloortracker.model.Workstation;

import java.util.List;


/**
 * Created by Nelson on 15/03/2018.
 */
public class WorkstationAdapter extends ArrayAdapter<Workstation> {

    private Context mContext;
    private List<Workstation> mWorkstations;

    public WorkstationAdapter(@NonNull Context context, List<Workstation> list) {
        super(context, android.R.layout.simple_dropdown_item_1line, list);
        mContext = context;
        mWorkstations = list;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        //return super.getDropDownView(position, convertView, parent);
        View listItem = convertView;
        if (listItem == null)
            listItem = LayoutInflater.from(mContext).inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
        Workstation wst = mWorkstations.get(position);

        TextView txt = listItem.findViewById(android.R.id.text1);
        txt.setText(wst.getCode() + " - " + wst.getDescription().trim());


        txt.setTag(wst);

        return listItem;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if (listItem == null)
            listItem = LayoutInflater.from(mContext).inflate(android.R.layout.simple_dropdown_item_1line, parent, false);

        Workstation wst = mWorkstations.get(position);

        TextView txt = listItem.findViewById(android.R.id.text1);
        txt.setText(wst.getCode() + " - " + wst.getDescription().trim());


        txt.setTag(wst);

        return listItem;
    }
}
