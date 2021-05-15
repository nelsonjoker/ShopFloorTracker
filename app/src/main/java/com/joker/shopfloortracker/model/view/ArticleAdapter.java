package com.joker.shopfloortracker.model.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.joker.shopfloortracker.model.Article;

import java.util.List;


/**
 * Adapter for displaying articles in dropdowns
 */
public class ArticleAdapter extends ArrayAdapter<Article> {

    private Context mContext;
    private List<Article> mArticles;

    public ArticleAdapter(@NonNull Context context, List<Article> list) {
        super(context, android.R.layout.simple_list_item_1 , list);
        mContext = context;
        mArticles = list;
    }


    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        //return super.getDropDownView(position, convertView, parent);
        View listItem = convertView;
        if(listItem == null)
            listItem = LayoutInflater.from(mContext).inflate(android.R.layout.simple_list_item_2,parent,false);

        Article a = mArticles.get(position);

        TextView itmref = (TextView) listItem.findViewById(android.R.id.text1);
        itmref.setText(a.getItmref());

        TextView description = (TextView) listItem.findViewById(android.R.id.text2);
        description.setText(a.getItmdes());

        listItem.setTag(a);



        return listItem;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if(listItem == null)
            listItem = LayoutInflater.from(mContext).inflate(android.R.layout.simple_list_item_2,parent,false);

        Article a = mArticles.get(position);

        TextView itmref = (TextView) listItem.findViewById(android.R.id.text1);
        itmref.setText(a.getItmref());

        TextView description = (TextView) listItem.findViewById(android.R.id.text2);
        description.setText(a.getItmdes());

        return listItem;
    }
}
