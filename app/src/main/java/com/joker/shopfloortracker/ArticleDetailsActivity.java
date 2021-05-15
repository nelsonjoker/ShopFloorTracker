package com.joker.shopfloortracker;

import android.app.Activity;
import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.joker.shopfloortracker.model.AppDatabase;
import com.joker.shopfloortracker.model.Article;
import com.joker.shopfloortracker.model.rest.Api;
import com.joker.shopfloortracker.model.rest.ApiResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class ArticleDetailsActivity extends AppCompatActivity {

    private static final String TAG = ArticleDetailsActivity.class.getName();

    private GridView gridViewThumbnails;
    private ProgressBar progressBar;
    private GridViewAdapter mGridAdapter;
    private ArrayList<String> mGridData;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_details);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView itmdes = findViewById(R.id.textViewItmdes);


        Intent intent = getIntent();
        String itmref = intent.getStringExtra("itmref");
        Article.ArticleDAO dao = AppDatabase.getInstance(this).getArticleDao();
        Article a = dao.get(itmref);

        if(a == null){
            Log.e(TAG, "Error getting article data for "+itmref+" ( out of sync ? )");
            Snackbar.make(itmdes, "Artigo não encontrado", Snackbar.LENGTH_LONG)
                    .setAction("Erro", null).show();
            setResult(Activity.RESULT_CANCELED);
            finish();
            return;
        }

        setTitle(getString(R.string.activity_article_details_title_f, a.getItmref()));

        itmdes.setText(a.getItmdes());

        TextView physto = findViewById(R.id.textViewPhysto);
        physto.setText(getString(R.string.in_stock_f, a.getPhysto(), a.getStu()));


        LinearLayout linearLayoutWarning = findViewById(R.id.linearLayoutWarning);
        RelativeLayout relativeLayoutThumbnails = findViewById(R.id.relativeLayoutThumbnails);

        gridViewThumbnails = findViewById(R.id.gridViewThumbnails);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        //Initialize with empty data
        mGridData = new ArrayList<>();
        mGridAdapter = new GridViewAdapter(this, R.layout.thumbnail_article, mGridData);
        gridViewThumbnails.setAdapter(mGridAdapter);

        progressBar.setVisibility(View.VISIBLE);

        Api.getInstance(this).get(getString(R.string.api_candidates_url_format, a.getItmref()), new ApiResponse() {
            @Override
            public void onResponse(Object response, VolleyError err, boolean result) {
                boolean found = false;
                if(response != null && response instanceof JSONArray && result) {
                    try {

                        JSONArray jdata = (JSONArray) response;
                        for (int i = 0; i < jdata.length(); i++) {
                            JSONObject j = jdata.getJSONObject(i);
                            int id = j.getInt("id");
                            String itmref = j.getString("itmref");

                            mGridAdapter.add(itmref);

                        }
                        found = true;


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                if(!found){
                    relativeLayoutThumbnails.setVisibility(View.GONE);
                    linearLayoutWarning.setVisibility(View.VISIBLE);
                }
                progressBar.setVisibility(View.GONE);


            }
        }, true);


    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(Activity.RESULT_CANCELED);
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public class GridViewAdapter extends ArrayAdapter<String> {

        private Context mContext;
        private int layoutResourceId;
        private ArrayList<String> mGridData;

        public GridViewAdapter(Context mContext, int layoutResourceId, ArrayList<String> mGridData) {
            super(mContext, layoutResourceId, mGridData);
            this.layoutResourceId = layoutResourceId;
            this.mContext = mContext;
            this.mGridData = mGridData;
        }

/*
        @Override
        public void add(@Nullable String itmref) {

            mGridData.add(itmref);

            super.add(itmref);
            notifyDataSetChanged();
        }
*/
        /**
         * Updates grid data and refresh grid items.
         * @param data
         */
        public void setGridData(ArrayList<String> data) {
            this.mGridData = data;
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            ViewHolder holder;

            if (row == null) {
                LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
                row = inflater.inflate(layoutResourceId, parent, false);
                holder = new ViewHolder();
                holder.textViewItmref = row.findViewById(R.id.textViewItmref);
                holder.imageViewThumbnail = row.findViewById(R.id.imageViewThumbnail);
                row.setTag(holder);
            } else {
                holder = (ViewHolder) row.getTag();
            }

            String itmref = mGridData.get(position);
            holder.textViewItmref.setText(itmref);


            String url = Api.getInstance(mContext).url(getString(R.string.api_thumbnail_url_format, itmref));
            Api.getInstance(mContext).getImageLoader().get(url, new ImageLoader.ImageListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }

                @Override
                public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                    holder.imageViewThumbnail.setImageBitmap(response.getBitmap());
                }
            });

            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String url = Api.getInstance(ArticleDetailsActivity.this).url(getString(R.string.api_pdf_url_format, itmref));
                    //Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    //startActivity(browserIntent);
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    browserIntent.setDataAndType(Uri.parse(url), "application/pdf");

                    try{
                        startActivity(browserIntent);
                    }catch(ActivityNotFoundException e){
                        browserIntent.setDataAndType(Uri.parse(url), "application/pdf");
                        Intent chooser = Intent.createChooser(browserIntent, getString(R.string.chooser_title));
                        chooser.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // optional
                        startActivity(chooser);
                    }


                }
            });


            return row;
        }



        class ViewHolder {
            ImageView imageViewThumbnail;
            TextView textViewItmref;
        }
    }

}
