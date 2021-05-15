package com.joker.shopfloortracker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import com.joker.shopfloortracker.model.Article;
import com.joker.shopfloortracker.model.Plan;
import com.joker.shopfloortracker.model.UpcomingPlan;

public class PaintDetailsActivity extends AppCompatActivity {


    private Article mEpoxy;
    private ArrayList<UpcomingPlan> mPlans;
    private RecyclerView listViewItems;
    private PlanRecyclerViewAdapter planListViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paint_details);

        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        //setTitle(getString(R.string.activity_paint_details_title));

        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }


        Intent intent = getIntent();
        mEpoxy = intent.getParcelableExtra("epoxy");
        mPlans = intent.getParcelableArrayListExtra("plans");
        //every plan here is assigned so we are repurposing the assigned flag to mark
        //selected items
        for(Plan p : mPlans)
            p.setAssigned(false);

        listViewItems = findViewById(R.id.list_view_items);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        listViewItems.setLayoutManager(linearLayoutManager);

        planListViewAdapter = new PlanRecyclerViewAdapter(this, mPlans);
        listViewItems.setAdapter(planListViewAdapter);



        //planListViewAdapter.setItems(plans);





    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_paint_details, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {



        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {


        switch(item.getItemId()){
            case R.id.action_check_all:
                planListViewAdapter.selectAll();
                return true;
            case R.id.action_delete:
                planListViewAdapter.deleteSelected();
                return true;
            case R.id.action_save:
                save();
                return true;
            case android.R.id.home:
                //navigateUpFromSameTask(this);
                setResult(Activity.RESULT_CANCELED);
                finish();
                return true;


        }

        return super.onOptionsItemSelected(item);

    }


    private void save(){

        for(Plan p : mPlans){
            p.setAssigned(true); //restore repurposed field
        }

        Intent data = new Intent();
        data.putExtra("epoxy", mEpoxy);
        data.putParcelableArrayListExtra("plans", mPlans);
        setResult(RESULT_OK, data);
        finish();

    }


    private class PlanRecyclerViewAdapter extends RecyclerView.Adapter<PlanRecyclerViewAdapter.RecyclerViewHolder> implements View.OnClickListener {



        private List<UpcomingPlan> mPlanList;

        public PlanRecyclerViewAdapter(Context ctx, List<UpcomingPlan> plans) {
            this.mPlanList = plans;
            setHasStableIds(true);
        }

        @Override
        public long getItemId(int position) {
            Plan p = getItem(position);
            return p.getId().hashCode();
        }

        @Override
        public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new RecyclerViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.plan_list_item, parent, false));
        }



        @Override
        public void onBindViewHolder(final RecyclerViewHolder holder, int position) {
            UpcomingPlan p = mPlanList.get(position);
            String epx = "";


            String itmref = p.getItmref();
            String itmdes = p.getItmdes();
            double qty = Math.max(0, p.getExtqty() - p.getCplqty() - p.getAssignedQty());
            String stu = p.getItmstu();
            boolean assigned = p.isAssigned();

            holder.imageViewPending.setVisibility(View.GONE);
            holder.layoutBackground.setBackgroundColor(getResources().getColor(assigned ? android.R.color.holo_orange_light : android.R.color.transparent));

            holder.textViewTitle.setText(itmref);
            holder.textViewPjt.setText(p.getPjt());
            holder.textViewDescription.setText(itmdes);
            holder.textViewCount.setText(String.format("%.2f / %.2f / %.2f %s", qty, p.getAssignedQty(), p.getExtqty(), stu));

            //holder.nameTextView.setText(borrowModel.getPersonName());
            //holder.dateTextView.setText(borrowModel.getBorrowDate().toLocaleString().substring(0, 11));
            holder.itemView.setTag(p);
            //holder.itemView.setOnLongClickListener(this);
            holder.itemView.setOnClickListener(this);

        }


        @Override
        public void onViewRecycled(RecyclerViewHolder holder) {
            holder.itemView.setTag(null);
            holder.itemView.setOnLongClickListener(null);
            holder.itemView.setOnClickListener(null);
            holder.itemView.setOnCreateContextMenuListener(null);
            super.onViewRecycled(holder);
        }


        @Override
        public int getItemCount() {
            return mPlanList.size();
        }
        public List<Plan> getItems(){
            return new ArrayList<Plan>( mPlanList );
        }



        public void setItems(List<UpcomingPlan> plans){
            setItems(plans, true);
        }
        public void setItems(List<UpcomingPlan> plans, boolean notify) {
            this.mPlanList = plans;
            if(notify)
                notifyDataSetChanged();
        }
        public Plan getItem(int pos){
            return mPlanList.get(pos);
        }

        public void selectAll(){
            for(Plan p : mPlanList){
                p.setAssigned(true);
            }
            notifyDataSetChanged();
        }

        public void deleteSelected(){
            List<Plan> blackList = new ArrayList<>(mPlanList.size());
            for(Plan p : mPlanList){
                if(p.isAssigned()){
                    blackList.add(p);
                }
            }
            mPlanList.removeAll(blackList);
            notifyDataSetChanged();
        }

        @Override
        public void onClick(View view) {

            Plan p = (Plan)view.getTag();
            p.setAssigned(!p.isAssigned());
            notifyItemChanged(mPlanList.indexOf(p));

        }


        class RecyclerViewHolder extends RecyclerView.ViewHolder {
            private View layoutBackground;
            private TextView textViewTitle;
            private TextView textViewPjt;
            private TextView textViewDescription;
            private TextView textViewCount;
            private ImageView imageViewPending;



            RecyclerViewHolder(View view) {
                super(view);

                layoutBackground = view.findViewById(R.id.layoutBackground);
                textViewTitle = view.findViewById(R.id.title);
                textViewPjt = view.findViewById(R.id.pjt);
                textViewDescription = view.findViewById(R.id.description);
                textViewCount = view.findViewById(R.id.count);
                imageViewPending = view.findViewById(R.id.imageViewPending);
            }
        }
    }

}
