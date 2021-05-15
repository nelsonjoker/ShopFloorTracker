package com.joker.shopfloortracker.jobpage;

import android.app.Activity;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.joker.shopfloortracker.ArticleDetailsActivity;
import com.joker.shopfloortracker.PaintDetailsActivity;
import com.joker.shopfloortracker.R;
import com.joker.shopfloortracker.model.AppDatabase;
import com.joker.shopfloortracker.model.Article;
import com.joker.shopfloortracker.model.Plan;
import com.joker.shopfloortracker.model.UpcomingPlan;
import com.joker.shopfloortracker.model.WorkMaterial;
import com.joker.shopfloortracker.model.sync.SyncService;
import com.joker.shopfloortracker.model.view.PlanViewModel;

/**
 * Created by Nelson on 01/04/2018.
 */

interface OnSelectedArticleChanging{
    boolean OnSelectedArticleChanging(Article target);
}

public class JobPageFragmentPaint extends AbstractJobPage implements OnSelectedArticleChanging {

    private static final String TAG = JobPageFragmentPaint.class.getSimpleName();


    private final static int RESULT_PAINT_LIST_EDIT = 100;

    private PlanViewModel mPlanViewModel;

    private PlanRecyclerViewAdapter planRecyclerViewAdapter;
    private RecyclerView planRecyclerView;

    private ColorGridViewAdapter mGridViewAdapter;
    private GridView mGridViewColors;

    private String mOnResumeSelectedItmref;

    final Messenger mMessenger = new Messenger(new IncomingHandler());
    @Override
    protected Messenger getMessenger() {
        return mMessenger;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPlanViewModel = ViewModelProviders.of(this).get(PlanViewModel.class);

        mOnResumeSelectedItmref = "";
        if(savedInstanceState != null){
            String selectedEpoxy = savedInstanceState.getString("selected_itmref");
            mOnResumeSelectedItmref = selectedEpoxy;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_job_page_paint, container, false);
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view = getView();
        planRecyclerView = view.findViewById(R.id.list_view_plans);
        //planRecyclerView.setVisibility(View.GONE);
        planRecyclerViewAdapter = new PlanRecyclerViewAdapter(new ArrayList<UpcomingPlan>(), this);
        planRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        planRecyclerView.setAdapter(planRecyclerViewAdapter);


        mGridViewColors = view.findViewById(R.id.gridColors);
        mGridViewAdapter = new ColorGridViewAdapter(getContext(), this);
        mGridViewColors.setAdapter(mGridViewAdapter);





    }


    @Override
    public void onPauseUpdates(){
        if(mLoadedPlanList != null){
            mReloadPrescaleHandler.removeCallbacksAndMessages(null);
            mLoadedPlanList.removeObservers(JobPageFragmentPaint.this);
            mLoadedPlanList = null;
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {

        Article sel = mGridViewAdapter.getSelectedArticle();
        outState.putString("selected_itmref", sel == null ? "" : sel.getItmref());
        super.onSaveInstanceState(outState);
    }

    @Override
    public List<Plan> getAssignedPlanList() {
        Article sel = mGridViewAdapter.getSelectedArticle();
        if(sel == null)
            return new ArrayList<>(0);
        else
            return new ArrayList<>( mGridViewAdapter.getPlansForArticle(sel) );
    }

    @Override
    public int getAssignedCount() {
        Article sel = mGridViewAdapter.getSelectedArticle();
        if(sel == null)
            return  0;
        else{
            List<UpcomingPlan> plans = mGridViewAdapter.getPlansForArticle(sel);
            return plans == null ? 0 : plans.size();
        }
    }



    /**
     * no such thing as deassign here
     * @param p
     */
    @Override
    public void OnItemClicked(Plan p) {
        try {
            assign(p);
        } catch (Exception e) {
            e.printStackTrace();
            Snackbar.make(getView(), e.getMessage(), Snackbar.LENGTH_LONG)
                    .setAction("Erro", null).show();
        }
    }

    @Override
    protected boolean assign(Plan p) throws Exception {

        double mx = p.getExtqty() - p.getCplqty() - p.getAssignedQty();

        if(p instanceof  UpcomingPlan){
            UpcomingPlan up = (UpcomingPlan)p;
            mx = up.getRemainingQty() - p.getAssignedQty();
        }

        final double max = mx;

        // Set up the input
        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.format("%.2f", max));
        new AlertDialog.Builder(getContext())
                .setTitle("Quantidade a incluir")
                .setMessage("Introduza a quantidade total disponível a incluir!")
                .setView(input)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        try {
                            String v = input.getText().toString();
                            if (v == null || v.length() <= 0) {
                                throw new Exception("É necessário preencher o campo quantidade");
                            }
                            double qty = Double.parseDouble(v.replace(',','.'));
                            if(qty <= 0)
                                throw new Exception("A quantidade tem de ser maior que 0");

                            if(qty > max)
                                throw new Exception(String.format("A quantidade tem de ser menor que %.2f", max));

                            JobPageFragmentPaint.super.assign(p, qty);

                        }catch(Exception e){
                            Snackbar.make(getView(), e.getMessage(), Snackbar.LENGTH_LONG)
                                    .setAction("Erro", null).show();
                        }



                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();

        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(requestCode == RESULT_PAINT_LIST_EDIT){
            if(resultCode == Activity.RESULT_OK){
                Article epoxy = data.getParcelableExtra("epoxy");
                List<Plan> plans = data.getParcelableArrayListExtra("plans");
                //perform reconciliation

                List<UpcomingPlan> existent = mGridViewAdapter.getPlansForArticle(epoxy);

                List<Plan> toRemove = new ArrayList<>();
                if(existent != null && existent.size() > 0) {
                    for (Plan e : existent) {
                        if (!plans.contains(e)) {
                            toRemove.add(e);
                        }
                    }
                }

                if(toRemove.size() > 0) {
                    doBindService();
                    for (Plan p : toRemove) {
                        try {
                            deassign(p);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e(TAG, e.getMessage());
                        }
                    }


                    onResumeUpdates();
                }



            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }



    @Override
    public boolean OnSelectedArticleChanging(Article target) {


        return false;
    }


    /**
     * Handler of incoming messages from service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            //https://stackoverflow.com/questions/15868635/thread-handler-error-the-specified-message-queue-synchronization-barrier-token
            this.obtainMessage();
            Bundle data = msg.getData();
            switch (msg.what) {
                case SyncService.MSG_UPDATE_PLAN:
                    //a plan needs to refreshed

                    if(data != null && data.containsKey(SyncService.ARG_OBJ)){
                        List<Plan> plans = data.getParcelableArrayList(SyncService.ARG_OBJ);

                        planRecyclerViewAdapter.updateItems(plans);

                    }
                    break;
                case SyncService.MSG_COMMIT_TRACKING:
                    //not really used, we actually have activity result doing this
                    if(data != null && data.containsKey(SyncService.ARG_OBJ)){
                        List<Plan> plans = data.getParcelableArrayList(SyncService.ARG_OBJ);

                        planRecyclerViewAdapter.updateItems(plans);


                    }

                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private LiveData<List<UpcomingPlan>> mLoadedPlanList;
    private Handler mReloadPrescaleHandler = new Handler();
    private long mPlanTaskCounter = 0;
    private long mPlanTaskLastExecution = 0;

    @Override
    protected void onSearchFilterChanged(String filter) {
        onResumeUpdates();
    }


    @Override
    public void onResumeUpdates(){

        if (mLoadedPlanList != null) {
            mReloadPrescaleHandler.removeCallbacksAndMessages(null);
            mLoadedPlanList.removeObservers(JobPageFragmentPaint.this);
            mLoadedPlanList = null;
        }

        //planRecyclerView.setVisibility(View.GONE);
        //progressBar.show();

        setLoading(true);


        mLoadedPlanList = mPlanViewModel.getRunningPlanList(getJob().getWcr(), getJob().getWst(), getJob().getAssignmentCode(), 500, getSearchFilterText());
        mLoadedPlanList.observe(JobPageFragmentPaint.this, new Observer<List<UpcomingPlan>>() {


            @Override
            public void onChanged(@Nullable List<UpcomingPlan> plans) {

                if(!isAdded())
                    return;

                long now = System.currentTimeMillis();
                if (now - mPlanTaskLastExecution < 1000) {
                    mReloadPrescaleHandler.removeCallbacksAndMessages(null);
                    mPlanTaskLastExecution = now;
                }


                mReloadPrescaleHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!isAdded()) {
                            return;
                        }

                        Log.d(TAG, "commit delayed update " + getJob().getName());
                        mPlanTaskCounter++;
                        FilterPlansTask task = new FilterPlansTask(mPlanTaskCounter);
                        if (Build.VERSION.SDK_INT >= 11) {
                            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, plans.toArray(new UpcomingPlan[plans.size()]));
                        } else {
                            task.execute(plans.toArray(new UpcomingPlan[plans.size()]));
                        }
                    }
                }, plans.size() > 0 ? 500 : 0);


            }


        });
    }




    public class FilterPlansTask extends AsyncTask<UpcomingPlan, Void, List<UpcomingPlan>> {

        private List<Plan> Assigned;
        private List<UpcomingPlan> OldPlans;
        private Map<Plan, Article> EpoxyMap;
        private Map<Article, List<UpcomingPlan>> ColorMap;
        private Map<Article, Double> QtyMap;
        private String AssignmentCode;
        private DiffUtil.DiffResult mDiffResult;
        private long mCounter;

        public FilterPlansTask(long counter) {
            mCounter = counter;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //progressBar.show();
            //progressBarLoading.setVisibility(View.VISIBLE);
            setLoading(true);
            //mDiffResult = null;
            ColorMap = null;
            EpoxyMap = null;
            QtyMap = null;
            AssignmentCode = getJob().getAssignmentCode();
            int pCount = planRecyclerViewAdapter.getItemCount();
            OldPlans = new ArrayList<>(pCount);
            for(int i = 0; i < pCount; i++){
                OldPlans.add(planRecyclerViewAdapter.getItem(i));
            }
        }

        @Override
        protected List<UpcomingPlan> doInBackground(UpcomingPlan... plans) {

            List<UpcomingPlan> filtered = new ArrayList<>(50);
            /*
            try {
                mPlanTaskFilter.acquire();
                */


            //planRecyclerView.setVisibility(View.VISIBLE);
            //mProgressBar.setVisibility(View.VISIBLE);

            //this is triggered every time the plan table is updated
            //https://medium.com/google-developers/7-pro-tips-for-room-fbadea4bfbd1
            //so we check for changes before reloading the UI

            //Plan.PlanDAO dao = AppDatabase.getInstance(getContext()).getPlanDao();

            //load a maximum of 50 ops or 8H of work

            double sumT = 0;
            for(int i = 0; i < plans.length && filtered.size() < 50 && sumT < 8*3600; i++){
                UpcomingPlan p = plans[i];
                if(p.isAssigned() && !p.getAssignmentCode().equals(AssignmentCode))
                    continue;
                    /*
                    List<Plan> predecessors = dao.getPredecessors(p.getMfgnum(), p.getOpenum());
                    double minCompletion = 1.0;
                    for(Plan prev : predecessors){
                        double c = prev.getCplqty() / prev.getExtqty();
                        if(c < minCompletion)
                            minCompletion = c;
                    }
                    if(minCompletion <= 0.001)
                        continue;
                    */
                sumT += p.getDuration();
                filtered.add(p);
            }

            if(isCancelationPending())
                return null;

            //now do some local optimization by overwriting start date ordering
            //with equal itmref ordering
            List<UpcomingPlan> sorted = new ArrayList<>(filtered.size());
            while(filtered.size() > 0){
                UpcomingPlan p = filtered.remove(0);
                sorted.add(p);
                for(int j = 0 ; j < filtered.size() ; j++){
                    UpcomingPlan n = filtered.get(j);
                    if(p.getItmref().equals(n.getItmref())){
                        sorted.add(n);
                        filtered.remove(j);
                        j--;//discount this iteration
                    }
                }
            }

            filtered = sorted;


            Map<Plan, Article> epoxyMap = new LinkedHashMap<>();
            Map<Article, List<UpcomingPlan>> inverseEpoxyMap = new LinkedHashMap<>();

            long maxEnd = 0;
            for(UpcomingPlan p : filtered){

                if(isCancelationPending())
                    return null;

                if(p.getEnd() > maxEnd)
                    maxEnd = p.getEnd();

                //warm up the cache, and create epoxy map
                List<WorkMaterial> materials = AppDatabase.getInstance(getContext()).getCachedMaterials(p.getMfgnum());

                Article epoxy = null;
                if(materials != null){
                    for(WorkMaterial m : materials){
                        Article a = AppDatabase.getInstance(getContext()).getCachedArticle(m.getItmref());
                        if(a != null) {
                            if ("1550".equals(a.getTsicod0()) || "4550".equals(a.getTsicod0())) {
                                epoxy = a;
                                break;
                            }
                        }
                    }
                }
                if(epoxy != null){
                    epoxyMap.put(p, epoxy);
                    if(p.isAssigned() && p.getAssignmentCode().equals(AssignmentCode) && p.getAssignedQty() > 0) {
                        List<UpcomingPlan> planList = inverseEpoxyMap.get(epoxy);
                        if (planList == null) {
                            planList = new ArrayList<>();
                            inverseEpoxyMap.put(epoxy, planList);
                        }
                        planList.add(p);
                    }
                }
            }

            EpoxyMap = epoxyMap;
            ColorMap = inverseEpoxyMap;


            QtyMap = new LinkedHashMap<>();
            maxEnd = (maxEnd / (24*3600*1000))*(24*3600*1000) + (24*3600*1000); //go to end of day
            String wcr = getJob().getWcr();
            String wst = getJob().getWst();
            for(Article a : ColorMap.keySet()){
                if(isCancelationPending())
                    return null;
                double qty = AppDatabase.getInstance(getContext()).getPlanDao().countConsumers(wcr, wst, a.getItmref(), maxEnd);
                QtyMap.put(a, qty);
            }




            Assigned = new ArrayList<Plan>(filtered.size()/3);
            for(Plan p : plans){
                if(p.isAssigned() && p.getAssignmentCode().equals(AssignmentCode)){
                    Assigned.add(p);
                }
            }



            List<UpcomingPlan> pending = new ArrayList<>(filtered.size());

            for(UpcomingPlan p : filtered){
                if(p.getAssignedQty() < p.getExtqty() - p.getCplqty())
                    pending.add(p);
            }

            filtered = pending;


            //mDiffResult = DiffUtil.calculateDiff(new PlanCompareCallback(OldPlans, filtered));
            /*
            } catch (InterruptedException e) {
                e.printStackTrace();
            }finally {
                mPlanTaskFilter.release();
            }
            */
            return filtered;
        }

        private boolean isCancelationPending(){
            long now = System.currentTimeMillis();
            if(now - mPlanTaskLastExecution < 10000) {
                if (mCounter < mPlanTaskCounter) {
                    Log.d(TAG, String.format("list refresh discarded %d < %d", mCounter, mPlanTaskCounter));
                    return true;
                }
            }
            return false;
        }


        @Override
        protected void onPostExecute(List<UpcomingPlan> plans) {

            if(plans == null || !isAdded()) {
                return;
            }

            //if we had a new task posted, discard this result

            if(isCancelationPending())
                return;
            long now = System.currentTimeMillis();
            mPlanTaskLastExecution = now;

            Log.d(TAG, String.format("refreshing list with %d plans", plans.size()));



            planRecyclerViewAdapter.setEpoxyMap(EpoxyMap);
            planRecyclerViewAdapter.setItems(plans, true);
            //mDiffResult.dispatchUpdatesTo(planRecyclerViewAdapter);

            mGridViewAdapter.setColorMap(ColorMap);
            mGridViewAdapter.setQuantityMap(QtyMap);

            if(mOnResumeSelectedItmref != null && mOnResumeSelectedItmref.length() > 0)
                mGridViewAdapter.setSelectedArticle(mOnResumeSelectedItmref);
            mOnResumeSelectedItmref = null;

            //need to also refresh materials and items
            //materialRecyclerViewAdapter.setPlans(Assigned);
            //itemRecyclerViewAdapter.setPlans(Assigned);


            //planRecyclerView.setVisibility(View.VISIBLE);
            //mProgressBar.setVisibility(View.GONE);
            planRecyclerView.setVisibility(View.VISIBLE);
            //progressBar.dismiss();
            //progressBarLoading.setVisibility(View.GONE);
            setLoading(false);

            Log.d(TAG, String.format("refresh took %d ms", System.currentTimeMillis() - now));


            //as this is running on the UI thread we have
            //exclusive access with mPlanTaskCounter so if by the end of this task
            //mPlanTaskCounter hasn't changed we can reset it
            //if(mCounter == mPlanTaskCounter)
            //    mPlanTaskCounter = 0;

                /*
                if(plans.size() > 0) {
                    planRecyclerView.setVisibility(View.VISIBLE);
                    mProgressBar.setVisibility(View.GONE);
                }else{
                    planRecyclerView.setVisibility(View.GONE);
                    mProgressBar.setVisibility(View.VISIBLE);
                }
                */
        }

        private class PlanCompareCallback  extends DiffUtil.Callback {

            private List<UpcomingPlan> mOldList;
            private List<UpcomingPlan> mNewList;

            public PlanCompareCallback(List<UpcomingPlan> oldList, List<UpcomingPlan> newList){
                mOldList = oldList;
                mNewList = newList;
            }

            @Override
            public int getOldListSize() {
                return mOldList.size();
            }

            @Override
            public int getNewListSize() {
                return mNewList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                Plan o = mOldList.get(oldItemPosition);
                Plan n = mNewList.get(newItemPosition);
                return o.equals(n);
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                UpcomingPlan o = mOldList.get(oldItemPosition);
                UpcomingPlan n = mNewList.get(newItemPosition);
                boolean changes = false;
                if(n.isAssigned() != o.isAssigned())
                    changes = true;
                else if(!n.getAssignmentCode().equals(o.getAssignmentCode()))
                    changes = true;
                else if(Math.abs(n.getCplqty() - o.getCplqty()) > 0.001)
                    changes = true;
                else if(Math.abs(n.getExtqty() - o.getExtqty()) > 0.001)
                    changes = true;
                else if(Math.abs(n.getRemainingQty() - o.getRemainingQty()) > 0.001)
                    changes = true;
                else if(n.isCompleted() != o.isCompleted())
                    changes = true;

                return !changes;
            }


            @Nullable
            @Override
            public Object getChangePayload(int oldItemPosition, int newItemPosition) {
                UpcomingPlan o = mOldList.get(oldItemPosition);
                UpcomingPlan n = mNewList.get(newItemPosition);


                Bundle data = new Bundle();

                if(n.isAssigned() != o.isAssigned())
                    data.putBoolean("assigned", n.isAssigned());
                else if(!n.getAssignmentCode().equals(o.getAssignmentCode()))
                    data.putString("assignment_code", n.getAssignmentCode());
                else if(Math.abs(n.getCplqty() - o.getCplqty()) > 0.001
                        || Math.abs(n.getExtqty() - o.getExtqty()) > 0.001
                        || Math.abs(n.getRemainingQty() - o.getRemainingQty()) > 0.001
                        || Math.abs(n.getAssignedQty() - o.getAssignedQty()) > 0.001) {
                    data.putDouble("cplqty", n.getCplqty());
                    data.putDouble("extqty", n.getExtqty());
                    data.putString("stu", n.getItmstu());
                    data.putDouble("remaining", n.getRemainingQty());
                    data.putDouble("assigned", n.getAssignedQty());
                }else if(n.isCompleted() != o.isCompleted())
                    data.putBoolean("completed", n.isCompleted());

                return data.size() > 0 ? data : null;

            }
        }

    }


    private class PlanRecyclerViewAdapter extends RecyclerView.Adapter<PlanRecyclerViewAdapter.RecyclerViewHolder>
            implements View.OnLongClickListener, View.OnClickListener{




        private OnPlanSelectedListener mSelectedListener;
        private List<UpcomingPlan> mPlanList;
        private Map<Plan, Article> mEpoxyMap;
        private Plan mContextPlan;

        public PlanRecyclerViewAdapter(List<UpcomingPlan> plans, OnPlanSelectedListener planListener) {
            this.mPlanList = plans;
            this.mSelectedListener = planListener;
            setHasStableIds(true);
        }

        @Override
        public long getItemId(int position) {
            Plan p = getItem(position);
            return p.getId().hashCode();
        }

        @Override
        public PlanRecyclerViewAdapter.RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new PlanRecyclerViewAdapter.RecyclerViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.plan_list_item_paint, parent, false));
        }

        @Override
        public void onBindViewHolder(PlanRecyclerViewAdapter.RecyclerViewHolder holder, int position, List<Object> payloads) {
            //super.onBindViewHolder(holder, position, payloads);

            if (payloads.isEmpty()){
                super.onBindViewHolder(holder, position, payloads);
            }else {
                //UpcomingPlan p = mPlanList.get(position);

                Bundle o = (Bundle) payloads.get(0);


                boolean updateQty = false;
                for (String key : o.keySet()) {
                    if ("assigned".equals(key)) {
                        boolean ass = o.getBoolean("assigned");
                        holder.layoutBackground.setBackgroundColor(getResources().getColor(ass ? android.R.color.holo_orange_light : android.R.color.transparent));
                    }
                    if ("itmref".equals(key)) {
                        holder.textViewTitle.setText(o.getString("itmref"));
                        holder.textViewPjt.setText(o.getString("pjt"));

                    }
                    if ("itmdes".equals(key)) {
                        holder.textViewDescription.setText(o.getString("itmdes"));
                    }
                    if ("extqty".equals(key) || "cplqty".equals(key) || "remaining".equals(key) || "assigned".equals(key)) {
                        updateQty = true;
                    }
                    if ("stu".equals(key)) {
                        holder.textViewCount.setText(o.getString("stu"));
                    }
                }
                if(updateQty){
                    double extqty = o.getDouble("extqty", 0);
                    double cplqty = o.getDouble("cplqty", 0);
                    double remaining = o.getDouble("remaining", 0);
                    double assigned = o.getDouble("assigned", 0);
                    String stu = o.getString("stu", "");
                    //double qty = Math.max(0, extqty - cplqty);
                    double qty = Math.max(0, remaining - assigned);
                    //holder.textViewCount.setText(String.format("%.2f %s", qty, stu));

                    holder.textViewCount.setText(String.format("%.2f / %.2f / %.2f %s", qty, assigned, extqty, stu));
                }
            }
        }

        @Override
        public void onBindViewHolder(final PlanRecyclerViewAdapter.RecyclerViewHolder holder, int position) {
            UpcomingPlan p = mPlanList.get(position);
            boolean pending = false;
            String epx = "";
            Article epoxy = mEpoxyMap.get(p);

            if(epoxy != null){
                epx = String.format("%s - %s", epoxy.getItmref(), epoxy.getItmdes());
            }

/*
            List<Plan> predecessors = AppDatabase.getInstance(getContext()).getPlanDao().getPredecessors(p.getMfgnum(), p.getOpenum());


            for(Plan pr : predecessors){
                if(!pr.isCompleted()){
                    pending = true;
                    break;
                }
            }
            */

            String itmref = p.getItmref();
            String itmdes = p.getItmdes();
            //double qty = Math.max(0, p.getExtqty() - p.getCplqty() - p.getAssignedQty());
            double qty = Math.max(0, p.getRemainingQty() - p.getAssignedQty());
            String stu = p.getItmstu();
            boolean assigned = p.isAssigned();

            holder.imageViewPending.setVisibility(pending ? View.VISIBLE : View.GONE);
            holder.textViewCount.setBackgroundColor(getResources().getColor(assigned ? android.R.color.holo_orange_light : android.R.color.transparent));

            holder.textViewTitle.setText(itmref);
            holder.textViewPjt.setText(p.getPjt());
            holder.textViewDescription.setText(itmdes);
            holder.textViewEpoxy.setText(epx);
            holder.textViewCount.setText(String.format("%.2f / %.2f / %.2f %s", qty, p.getAssignedQty(), p.getExtqty(), stu));

            //holder.nameTextView.setText(borrowModel.getPersonName());
            //holder.dateTextView.setText(borrowModel.getBorrowDate().toLocaleString().substring(0, 11));
            holder.itemView.setTag(p);
            //holder.itemView.setOnLongClickListener(this);
            holder.itemView.setOnClickListener(this);

            if(pending) {
                holder.itemView.setEnabled(false);
                holder.textViewTitle.setTextColor(Color.GRAY);
            }
            holder.itemView.setOnCreateContextMenuListener(mOnCreateContextMenuListener);

        }

        private final View.OnCreateContextMenuListener mOnCreateContextMenuListener = new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
                JobPageFragmentPaint.super.onCreateContextMenu(contextMenu, view, contextMenuInfo);
                Plan p = (Plan) view.getTag();
                mContextPlan = p;

                MenuInflater inflater = getActivity().getMenuInflater();
                inflater.inflate(R.menu.menu_ctx_article, contextMenu);
                MenuItem itm = contextMenu.findItem(R.id.menu_itmref);
                itm.setOnMenuItemClickListener(mOnMyActionClickListener);

            }

        };



        private final MenuItem.OnMenuItemClickListener mOnMyActionClickListener = new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                try {
                    reloadArticle(mContextPlan.getItmref()); //just in case
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Intent intent = new Intent(getContext(), ArticleDetailsActivity.class);
                intent.putExtra("itmref", mContextPlan.getItmref());
                startActivity(intent);
                return true;
            }
        };


        @Override
        public void onViewRecycled(PlanRecyclerViewAdapter.RecyclerViewHolder holder) {
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
        public int getAssignedItemCount() {
            int count = 0;
            for(Plan p : mPlanList){
                if(p.isAssigned())
                    count++;
            }
            return count;
        }


        public void setEpoxyMap(Map<Plan, Article> epoxies){
            mEpoxyMap = epoxies;
        }

        public void setItems(List<UpcomingPlan> plans){
            setItems(plans, true);
        }
        public void setItems(List<UpcomingPlan> plans, boolean notify) {
            this.mPlanList = plans;
            if(notify)
                notifyDataSetChanged();
        }
        public UpcomingPlan getItem(int pos){
            return mPlanList.get(pos);
        }


        public void updateItems(List<Plan> plans){
            for(Plan p : plans){
                updateItem(p);
            }
        }
        public void updateItem(Plan p){
            for(int pos = 0; pos < mPlanList.size(); pos++){
                Plan i = mPlanList.get(pos);
                if(i.equals(p)){    //equals uses id
                    p.copyTo(i);

                    if(i.isCompleted()){
                        mPlanList.remove(pos);
                        notifyItemRemoved(pos);
                    }else {
                        notifyItemChanged(pos);
                    }
                    break;  //there can be only one
                }
            }
        }

        @Override
        public void onClick(View view) {
            Plan p = (Plan)view.getTag();
            mSelectedListener.OnItemClicked(p);
        }

        @Override
        public boolean onLongClick(View view) {
            Plan p = (Plan)view.getTag();
            mSelectedListener.OnItemSelected(p);
            return false;
        }



        class RecyclerViewHolder extends RecyclerView.ViewHolder {
            private View layoutBackground;
            private TextView textViewTitle;
            private TextView textViewPjt;
            private TextView textViewDescription;
            private TextView textViewCount;
            private TextView textViewEpoxy;
            private ImageView imageViewPending;



            RecyclerViewHolder(View view) {
                super(view);
                layoutBackground = view.findViewById(R.id.layoutBackground);
                textViewTitle = view.findViewById(R.id.title);
                textViewPjt = view.findViewById(R.id.pjt);
                textViewDescription = view.findViewById(R.id.description);
                textViewEpoxy = view.findViewById(R.id.epoxy);
                textViewCount = view.findViewById(R.id.count);
                imageViewPending = view.findViewById(R.id.imageViewPending);
            }



        }
    }


    private class ColorGridViewAdapter extends BaseAdapter {




        private final Context mContext;
        private List<Article> mArticles; //used to index color map
        private Map<Article, List<UpcomingPlan>> mColorMap;
        private Map<Article, Double> mQuantityMap;
        private Article mSelectedArticle;
        private OnSelectedArticleChanging mOnSelectedArticleChanging;

        public void setOnSelectedArticleChangingListener(OnSelectedArticleChanging cb){
            mOnSelectedArticleChanging = cb;
        }

        // 1
        public ColorGridViewAdapter(Context context, OnSelectedArticleChanging cb) {
            this.mContext = context;
            mArticles = new ArrayList<>(0);
            mColorMap = new LinkedHashMap<>(0);
            mQuantityMap = new LinkedHashMap<>(0);
            mSelectedArticle = null;
            mOnSelectedArticleChanging = cb;
        }

        // 2
        @Override
        public int getCount() {
            return mArticles.size();
        }

        // 3
        @Override
        public long getItemId(int position) {
            return mArticles.get(position).getItmref().hashCode();
        }

        // 4
        @Override
        public Object getItem(int position) {
            return mArticles.get(position);
        }

        public Article getSelectedArticle(){
            return mSelectedArticle;
        }

        public void setSelectedArticle(String itmref){
            Article ex = null;
            for(Article a : mArticles){
                if(itmref.equals(a.getItmref())){
                    ex = a;
                    break;
                }
            }
            mSelectedArticle = ex;
            notifyDataSetChanged();
        }

        public void setColorMap(Map<Article,List<UpcomingPlan>> colorMap) {
            this.mColorMap = colorMap;
            Set<Article> articles =  mColorMap.keySet();
            mArticles = new ArrayList<>(articles);

            if(mSelectedArticle != null && !mArticles.contains(mSelectedArticle)){
                mSelectedArticle = null;
            }

            notifyDataSetChanged();
        }
        public void setQuantityMap(Map<Article,Double> quantityMap) {
            this.mQuantityMap = quantityMap;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // 1
            final Article a = mArticles.get(position);
            List<UpcomingPlan> plans = mColorMap.get(a);

            // 2
            if (convertView == null) {
                final LayoutInflater layoutInflater = LayoutInflater.from(mContext);
                convertView = layoutInflater.inflate(R.layout.plan_list_item_color, null);
                convertView.setOnCreateContextMenuListener(mOnCreateContextMenuListener);
                convertView.setOnClickListener(mOnClickListener);
            }

            boolean s = mSelectedArticle != null && a.equals(mSelectedArticle);
            convertView.setTag(a);
            convertView.setBackgroundColor(getResources().getColor(s ? android.R.color.holo_orange_light : android.R.color.transparent));

            // 3
            final com.github.lzyzsd.circleprogress.ArcProgress progress = convertView.findViewById(R.id.progressBarCompletion);
            final TextView itmref = convertView.findViewById(R.id.textViewItmref);
            final TextView itmdes = convertView.findViewById(R.id.textViewItmdes);


            Double max = mQuantityMap.get(a);
            if(max == null){
                double m = 0;
                for(Plan p : plans){
                    m += p.getExtqty() - p.getCplqty();
                }
                max = Double.valueOf(m);
            }



            // 4
            //progress.setMax(max.intValue());//plans.size());
            progress.setMax(100);
            double selected = 0;
            for(Plan p : plans){
                if(p.isAssigned())
                    selected += p.getAssignedQty();
            }
            int p = (int) Math.round(100.0*(selected/max.doubleValue()));
            progress.setProgress(p); //selected);
            progress.setBottomText(String.format("%d/%d", (int)selected, max.intValue()));

            itmref.setText(a.getItmref());
            itmdes.setText(a.getItmdes());

            return convertView;
        }

        private final View.OnCreateContextMenuListener mOnCreateContextMenuListener = new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
                JobPageFragmentPaint.super.onCreateContextMenu(contextMenu, view, contextMenuInfo);
                final Article a = (Article) view.getTag();
                final ArrayList<UpcomingPlan> assignedPlans = new ArrayList<>( mColorMap.get(a) );

                MenuInflater inflater = getActivity().getMenuInflater();
                inflater.inflate(R.menu.menu_ctx_paint_article, contextMenu);
                MenuItem itm = contextMenu.findItem(R.id.menu_article);
                itm.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        Intent intent = new Intent(getContext(), PaintDetailsActivity.class);
                        intent.putExtra("epoxy", a);
                        intent.putParcelableArrayListExtra("plans", assignedPlans);
                        startActivityForResult(intent, RESULT_PAINT_LIST_EDIT);
                        return true;
                    }
                });

            }

        };

        private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Article a = (Article) view.getTag();

                if(mSelectedArticle != null && a.equals(mSelectedArticle))
                    a = null;

                if(mOnSelectedArticleChanging == null || !mOnSelectedArticleChanging.OnSelectedArticleChanging(a))
                    return;

                mSelectedArticle = a;

                notifyDataSetChanged();


            }
        };

        public List<UpcomingPlan> getPlansForArticle(Article epoxy) {
            return mColorMap.get(epoxy);
        }


/*
        private final MenuItem.OnMenuItemClickListener mOnMyActionClickListener = new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent(getContext(), ArticleDetailsActivity.class);
                intent.putExtra("itmref", mContextPlan.getItmref());
                startActivity(intent);
                return true;
            }
        };
*/

    }


}
