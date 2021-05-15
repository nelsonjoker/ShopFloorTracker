package com.joker.shopfloortracker.jobpage;

import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.util.DiffUtil;
import android.support.v7.util.ListUpdateCallback;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.joker.shopfloortracker.ArticleDetailsActivity;
import com.joker.shopfloortracker.R;
import com.joker.shopfloortracker.model.AppDatabase;
import com.joker.shopfloortracker.model.Article;
import com.joker.shopfloortracker.model.Plan;
import com.joker.shopfloortracker.model.UpcomingPlan;
import com.joker.shopfloortracker.model.UpcomingPlanGroup;
import com.joker.shopfloortracker.model.WorkItem;
import com.joker.shopfloortracker.model.WorkMaterial;
import com.joker.shopfloortracker.model.sync.SyncService;
import com.joker.shopfloortracker.model.view.PlanViewModel;
import com.joker.shopfloortracker.ui.SpinnerLineAdapter;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link JobPageFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class JobPageFragment extends AbstractJobPage {

    private static final String TAG = JobPageFragment.class.getSimpleName();






    private OnFragmentInteractionListener mListener;



    private PlanViewModel mPlanViewModel;
    private PlanRecyclerViewAdapter planRecyclerViewAdapter;
    private RecyclerView planRecyclerView;
    //private ProgressBar progressBarLoading;

    //private ProgressDialog progressBar;

    private ArrayAdapter<String> mPjtSpinnerAdapter;

    private Spinner spinnerPjt;
    private Spinner spinnerEpoxy;

    private int mSavedPjtPosition = -1;
    private int mSavedEpoxyPosition = -1;


    private class SpinnerEpoxyItem implements SpinnerLineAdapter.SpinnerLineAdapterItem{

        private Article mEpoxy;

        public Article getArticle() { return mEpoxy; }

        @Override
        public String getTitle() {
            return mEpoxy != null && mEpoxy.getItmref() != null ? mEpoxy.getItmref() : "";
        }

        @Override
        public String getDescription() {
            return mEpoxy.getItmdes();
        }

        public SpinnerEpoxyItem(Article e){
            mEpoxy = e;
        }

        @Override
        public String toString() {
            return getTitle();
        }
    }
    private SpinnerLineAdapter<SpinnerEpoxyItem> mEpoxySpinnerAdapter;


    final Messenger mMessenger = new Messenger(new IncomingHandler());


    @Override
    protected Messenger getMessenger() {
        return mMessenger;
    }




    public JobPageFragment() {
        super();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPjtSpinnerAdapter = new ArrayAdapter<>(
                getActivity(), android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        mEpoxySpinnerAdapter = new SpinnerLineAdapter<>(getContext(), new ArrayList<>());
    }

    /**
     * Handler of incoming messages from service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            //https://stackoverflow.com/questions/15868635/thread-handler-error-the-specified-message-queue-synchronization-barrier-token

            //FIXED: disable because we are not handling single plan update
//            this.obtainMessage();
//            Bundle data = msg.getData();
//            switch (msg.what) {
//                case SyncService.MSG_UPDATE_PLAN:
//                    //a plan needs to refreshed
//
//                    if(data != null && data.containsKey(SyncService.ARG_OBJ)){
//                        List<Plan> plans = data.getParcelableArrayList(SyncService.ARG_OBJ);
//
//                        planRecyclerViewAdapter.updateItems(plans);
//
//                    }
//                    break;
//                case SyncService.MSG_COMMIT_TRACKING:
//                    //not really used, we actually have activity result doing this
//                    if(data != null && data.containsKey(SyncService.ARG_OBJ)){
//                        List<Plan> plans = data.getParcelableArrayList(SyncService.ARG_OBJ);
//
//                        planRecyclerViewAdapter.updateItems(plans);
//
//
//                    }
//
//                    break;
//                default:
//                    super.handleMessage(msg);
//            }
        }
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_job_page, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view = getView();
        planRecyclerView = view.findViewById(R.id.list_view_plans);
        //planRecyclerView.setVisibility(View.GONE);
        planRecyclerViewAdapter = new PlanRecyclerViewAdapter(new ArrayList<>(), this);
        planRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        planRecyclerView.setAdapter(planRecyclerViewAdapter);
        mPlanViewModel = ViewModelProviders.of(this).get(PlanViewModel.class);




        spinnerPjt = (Spinner) mToolbar.getMenu().findItem(R.id.spinnerPjt).getActionView();
        spinnerEpoxy = (Spinner) mToolbar.getMenu().findItem(R.id.spinnerEpoxy).getActionView();

        spinnerPjt.setAdapter(mPjtSpinnerAdapter);
        spinnerEpoxy.setAdapter(mEpoxySpinnerAdapter);



        AdapterView.OnItemSelectedListener onFilterChangedLister = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                onPauseUpdates();
                onResumeUpdates();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                onPauseUpdates();
                onResumeUpdates();

            }
        };

        spinnerPjt.setOnItemSelectedListener(onFilterChangedLister);
        spinnerEpoxy.setOnItemSelectedListener(onFilterChangedLister);

        /*
        progressBar = new ProgressDialog(getContext());
        progressBar.setCancelable(true);
        progressBar.setMessage(String.format("A atualizar a lista (%s) ... ", mJob.getName()));
        progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressBar.setIndeterminate(true);
        */



        //AppDatabase.getInstance(getContext()).exportDB();


        if(savedInstanceState != null){
            if(savedInstanceState.containsKey("selected_pjt_position")){
                int pos = savedInstanceState.getInt("selected_pjt_position");
                mSavedPjtPosition = pos;
            }
            if(savedInstanceState.containsKey("selected_epoxy_position")){
                int pos = savedInstanceState.getInt("selected_epoxy_position");
                mSavedEpoxyPosition = pos;
            }
        }

    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        //https://stackoverflow.com/questions/30549722/onsaveinstancestate-is-not-getting-called-after-screen-rotation
        super.onSaveInstanceState(outState);

        int pos = spinnerPjt.getSelectedItemPosition();
        outState.putInt("selected_pjt_position", pos);
        pos = spinnerEpoxy.getSelectedItemPosition();
        outState.putInt("selected_epoxy_position", pos);


    }



    @Override
    public void onPauseUpdates(){
        mReloadPrescaleHandler.removeCallbacksAndMessages(null);
        mSkipFirstUpdate = true;
        Log.d(TAG, "pausing updates " + getJob().getName());
    }



    @Override
    public List<Plan> getAssignedPlanList(){
        return planRecyclerViewAdapter.getAssignedItems();
    }
    public int getAssignedCount(){
        //return itemRecyclerViewAdapter.getItemCount();
        return planRecyclerViewAdapter.getAssignedItemCount();
        //return 0;
    }

    private  Handler mReloadPrescaleHandler = new Handler();
    private long mPlanTaskCounter = 0;
    private long mPlanTaskLastExecution = 0;
    private boolean mSkipFirstUpdate = false;


    @Override
    protected void onSearchFilterChanged(String filter) {
        mSkipFirstUpdate = false;
        onResumeUpdates();
    }

    @Override
    public void onResumeUpdates(){

        mReloadPrescaleHandler.removeCallbacksAndMessages(null);

        Runnable tick = new Runnable() {
            @Override
            public void run() {
                mPlanTaskCounter++; //contingency to avoid multiple tasks
                FilterPlansTask task = new FilterPlansTask(mPlanTaskCounter);
                task.execute();

                mReloadPrescaleHandler.postDelayed(this, 30000);

            }
        };

        mReloadPrescaleHandler.post(tick);


    }



    public class FilterPlansTask extends AsyncTask<Void, Void, List<UpcomingPlanGroup>> {

        private List<UpcomingPlanGroup> OldPlans;
        private String AssignmentCode;
        private DiffUtil.DiffResult mDiffResult;
        private long mCounter;
        private String[] mMaterialItmrefFilters;
        private String mSearchFilterText;
        private String mWCR;
        private String mWST;
        private List<String> mPjts;
        private List<Article> mEpoxies;

        private String mPjtFilter;
        private String mEpoxyFilter;

        public FilterPlansTask(long counter) {
            mCounter = counter;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d(TAG, "resuming updates " + getJob().getName());
            setLoading(true);
            //progressBar.show();
            //progressBarLoading.setVisibility(View.VISIBLE);
            setLoading(true);
            mDiffResult = null;
            AssignmentCode = getJob().getAssignmentCode();
            int pCount = planRecyclerViewAdapter.getItemCount();
            OldPlans = new ArrayList<>(pCount);
            for(int i = 0; i < pCount; i++){
                OldPlans.add((UpcomingPlanGroup)planRecyclerViewAdapter.getItem(i));
            }

/*
            Set<String> filter = materialRecyclerViewAdapter.getMaterialFilter();
            mMaterialItmrefFilters = null;
            if(filter.size() > 0) {
                mMaterialItmrefFilters = filter.toArray(new String[filter.size()]);
            }
*/
            mSearchFilterText = getSearchFilterText();

            mWCR = getJob().getWcr();
            mWST = getJob().getWst();


            mPjts = new ArrayList<>();
            mPjts.add("---");
            mEpoxies = new ArrayList<>();
            Article dummy = new Article();
            dummy.setItmref("---");
            dummy.setItmdes("Qualquer cor");
            mEpoxies.add(dummy);


            mPjtFilter = spinnerPjt.getSelectedItem() != null ?  spinnerPjt.getSelectedItem().toString() : "";
            mEpoxyFilter = spinnerEpoxy.getSelectedItem() != null ?  spinnerEpoxy.getSelectedItem().toString() : "";


        }

        @Override
        protected List<UpcomingPlanGroup> doInBackground(Void... args) {


            if(isCancelationPending())
                return null;
/*
            for(int i = 0; i < plans.length; i++){
                Plan p = plans[i];
                for(int j = i + 1; j < plans.length; j++){
                    Plan o = plans[j];
                    if(p.equals(o)){
                        Log.e(TAG, "repeated plan detected");
                    }
                }
            }
*/

            List<UpcomingPlan> plans = null;

            assert(!Looper.getMainLooper().equals(Looper.myLooper()));

            String filter = mSearchFilterText;
            if(filter == null || "".equals(filter))
                filter = "";
            else
                filter = "%"+filter+"%";

            if(mMaterialItmrefFilters == null || mMaterialItmrefFilters.length == 0)
                plans = AppDatabase.getInstance(getActivity()).getUpcomingPlanDao().getUpcoming_(mWCR, mWST, AssignmentCode, 50000, filter);
            else
                plans = AppDatabase.getInstance(getActivity()).getUpcomingPlanDao().getUpcoming_(mWCR, mWST, AssignmentCode, 50000, filter, mMaterialItmrefFilters);


            HashMap<String, UpcomingPlanGroup> groups = new HashMap<>();
            HashMap<String, Double> unfilteredQtys = new HashMap<>();

            List<UpcomingPlanGroup> filtered = new ArrayList<>(50);
            /*
            try {
                mPlanTaskFilter.acquire();
                */


                //planRecyclerView.setVisibility(View.VISIBLE);
                //mProgressBar.setVisibility(View.VISIBLE);

                //this is triggered every time the plan table is updated
                //https://medium.com/google-developers/7-pro-tips-for-room-fbadea4bfbd1
                //so we check for changes before reloading the UI

                //Plan.PlanDAO dao = AppDatabase.getInstance(getActivity()).getPlanDao();

                //load a maximum of 50 ops or 8H of work


            List<String> uniqueEpoxies = new ArrayList<>();
                double sumT = 0;
                //for(int i = 0; i < plans.length && filtered.size() < 50 && (sumT < 8*3600 || filtered.size() <= 10); i++){
                for(int i = 0; i < plans.size() ; i++){
                    UpcomingPlan p = plans.get(i);
                    if(p.isAssigned() && !p.getAssignmentCode().equals(AssignmentCode))
                        continue;

                    Double q = unfilteredQtys.containsKey(p.getItmref()) ? unfilteredQtys.get(p.getItmref()) : 0.0;
                    unfilteredQtys.put(p.getItmref(), q + (p.getExtqty()-p.getCplqty()));

                    if(! mPjts.contains(p.getPjt()))
                        mPjts.add(p.getPjt());
                    List<WorkItem> wis = AppDatabase.getInstance(getActivity()).getCachedItems(p.getMfgnum());
                    for(WorkItem it : wis){
                        String epoxy = it.getEpxitmref();
                        if(!uniqueEpoxies.contains(epoxy)){
                            uniqueEpoxies.add(epoxy);
                            Article e = AppDatabase.getInstance(getActivity()).getCachedArticle(epoxy);
                            if(e != null) {
                                //String t = String.format("%s - %s", e.getItmref(), e.getItmdes());
                                mEpoxies.add(e);
                            }
                        }
                    }

                    if(!"---".equals(mPjtFilter) && !mPjtFilter.equals(p.getPjt())){
                        continue;
                    }



                    if(!"---".equals(mEpoxyFilter) ){
                        boolean accept = false;
                        for(WorkItem it : wis){
                            if(mEpoxyFilter.equals(it.getEpxitmref())){
                                accept = true;
                                break;
                            }
                        }
                        if(!accept)
                            continue;
                    }


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
                    //filtered.add(p);

                    UpcomingPlanGroup ex = null;
                    if(groups.containsKey(p.getItmref()))
                        ex = groups.get(p.getItmref());
                    else{
                        ex = new UpcomingPlanGroup(p);
                        groups.put(p.getItmref(), ex);
                        filtered.add(ex);
                    }

                    ex.add(p);

                    if(isCancelationPending())
                        return null;
                    Thread.yield();
                }

                for(UpcomingPlanGroup g : filtered){
                    Double q = unfilteredQtys.containsKey(g.getItmref()) ? unfilteredQtys.get(g.getItmref()) : 0.0;
                    g.setUnfilteredQty(q);
                }

//            long now = System.currentTimeMillis();

                //now do some local optimization by overriding start date ordering
                //with equal itmref ordering
//                List<UpcomingPlan> sorted = new ArrayList<>(filtered.size());
//                while(filtered.size() > 0){
//                    UpcomingPlan p = filtered.remove(0);
//                    sorted.add(p);
//                    for(int j = 0 ; j < filtered.size() ; j++){
//                        UpcomingPlan n = filtered.get(j);
//                        if(p.getItmref().equals(n.getItmref())){
//                            sorted.add(n);
//                            filtered.remove(j);
//                            j--;//discount this iteration
//                        }
//                    }
//                    if(isCancelationPending())
//                        return null;
//                    Thread.yield();
//                }
//
//                filtered = sorted;
//            Log.d(TAG, String.format("Diff result takes %d ms to complete", System.currentTimeMillis() - now));


//                for(Plan p : filtered){
//                    //warm up the cache
//                    List<WorkItem> wis = AppDatabase.getInstance(getActivity()).getCachedItems(p.getMfgnum());
//                    AppDatabase.getInstance(getActivity()).getCachedMaterials(p.getMfgnum());
//                    if(isCancelationPending())
//                        return null;
//                    Thread.yield();
//                }


                if(isCancelationPending())
                    return null;

                mDiffResult = DiffUtil.calculateDiff(new PlanCompareCallback<>(OldPlans, filtered));

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

        private class MockListUpdateCallback implements ListUpdateCallback{

            private boolean hasChanges = false;
            public boolean hasChanges(){ return hasChanges; }

            public MockListUpdateCallback(){
                hasChanges = false;
            }


            public void reset(){
                hasChanges = false;
            }

            @Override
            public void onInserted(int position, int count) {
                hasChanges = true;
            }

            @Override
            public void onRemoved(int position, int count) {
                hasChanges = true;
            }

            @Override
            public void onMoved(int fromPosition, int toPosition) {
                hasChanges = true;
            }

            @Override
            public void onChanged(int position, int count, Object payload) {
                hasChanges = true;
            }
        };


        @Override
        protected void onPostExecute(List<UpcomingPlanGroup> plans) {

            if(plans == null || !isAdded()) {
                return;
            }

            //if we had a new task posted, discard this result
            if(isCancelationPending())
                return;
            long now = System.currentTimeMillis();
            mPlanTaskLastExecution = now;

            Log.d(TAG, String.format("refreshing list with %d plans", plans.size()));


            Object o = spinnerPjt.getSelectedItem();
            mPjtSpinnerAdapter.clear();
            mPjtSpinnerAdapter.addAll(mPjts);
            if(mSavedPjtPosition >= 0){
                spinnerPjt.setSelection(mSavedPjtPosition);
                mSavedPjtPosition = -1;
            }else {
                spinnerPjt.setSelection(mPjts.indexOf(o));
            }

            o = spinnerEpoxy.getSelectedItem();
            List<SpinnerEpoxyItem> nValues = new ArrayList<>(mEpoxySpinnerAdapter.getCount());
            int selection = -1;
            for(int i =0; i < mEpoxies.size(); i++){
                Article e = mEpoxies.get(i);
                nValues.add(new SpinnerEpoxyItem(e));
                if(o != null && e.equals(((SpinnerEpoxyItem)o).getArticle()))
                    selection = i;
            }
            mEpoxySpinnerAdapter.setValue(nValues);

            if(mSavedEpoxyPosition >= 0){
                spinnerEpoxy.setSelection(mSavedEpoxyPosition);
                mSavedEpoxyPosition = -1;
            }else {
                spinnerEpoxy.setSelection(selection);
            }


            //planRecyclerViewAdapter.setItems(plans, false);
            //mDiffResult.dispatchUpdatesTo(planRecyclerViewAdapter);
            MockListUpdateCallback cb = new MockListUpdateCallback();
            mDiffResult.dispatchUpdatesTo(cb);
            if(cb.hasChanges()) {
                planRecyclerViewAdapter.setItems(plans, true);


                //planRecyclerView.setVisibility(View.VISIBLE);
                //mProgressBar.setVisibility(View.GONE);
                planRecyclerView.setVisibility(View.VISIBLE);
                //progressBar.dismiss();
                //progressBarLoading.setVisibility(View.GONE);
            }
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

        //TODO: if we are to use this need to add completion_ratio on UpcomingPlan
        private class PlanCompareCallback<T extends UpcomingPlan>  extends DiffUtil.Callback {

            private List<T> mOldList;
            private List<T> mNewList;

            public PlanCompareCallback(List<T> oldList, List<T> newList){
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
                else if(Math.abs(n.getCplqty() - o.getCplqty()) > 0.001) {
                    data.putDouble("cplqty", n.getCplqty());
                    data.putDouble("extqty", n.getExtqty());
                    data.putString("stu", n.getItmstu());
                    data.putDouble("remaining", n.getRemainingQty());
                }else if(Math.abs(n.getExtqty() - o.getExtqty()) > 0.001) {
                    data.putDouble("cplqty", n.getCplqty());
                    data.putDouble("extqty", n.getExtqty());
                    data.putString("stu", n.getItmstu());
                    data.putDouble("remaining", n.getRemainingQty());
                }else if(Math.abs(n.getRemainingQty() - o.getRemainingQty()) > 0.001) {
                    data.putDouble("cplqty", n.getCplqty());
                    data.putDouble("extqty", n.getExtqty());
                    data.putString("stu", n.getItmstu());
                    data.putDouble("remaining", n.getRemainingQty());
                }else if(n.isCompleted() != o.isCompleted())
                    data.putBoolean("completed", n.isCompleted());

                return data.size() > 0 ? data : null;

            }
        }

    }


    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode){
            case RESULT_TRACKING_CONFIRM:
                if(resultCode == Activity.RESULT_OK) {
                    if(data != null && data.hasExtra(SyncService.ARG_OBJ)){
                        List<Plan> plans = data.getParcelableArrayListExtra(SyncService.ARG_OBJ);
                        planRecyclerViewAdapter.updateItems(plans);
                    }
                }
                break;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }



    @Override
    protected boolean assign(Plan p) throws Exception {

        for(int i = 0; i < planRecyclerViewAdapter.getItemCount(); i++){
            UpcomingPlanGroup g = (UpcomingPlanGroup)planRecyclerViewAdapter.getItem(i);
            if(g.equals(p)){
                if(!g.isAssigned()) {

                    List<UpcomingPlan> plans = g.getPlans();
                    for(UpcomingPlan up : plans){
                        if(!up.isAssigned())
                            super.assign(up);
                    }
                    g.updateTotals();
                    planRecyclerViewAdapter.updateItem(g);
                }
            } else if(g.isAssigned()){
                List<UpcomingPlan> plans = g.getPlans();
                for(UpcomingPlan up : plans){
                    if(!up.isAssigned())
                        super.deassign(up);
                }
                g.updateTotals();
                planRecyclerViewAdapter.updateItem(g);
            }
        }

        /*
        if(super.assign(p)){
            planRecyclerViewAdapter.updateItem(p);
            return true;
        }
        */

        return false;

    }

    @Override
    protected boolean deassign(Plan p) throws Exception {

        for(int i = 0; i < planRecyclerViewAdapter.getItemCount(); i++){
            UpcomingPlanGroup g = (UpcomingPlanGroup)planRecyclerViewAdapter.getItem(i);
            if(g.equals(p)){
                if(g.isAssigned()) {

                    List<UpcomingPlan> plans = g.getPlans();
                    for(UpcomingPlan up : plans){
                        if(up.isAssigned())
                            super.deassign(up);
                    }
                    g.updateTotals();
                    planRecyclerViewAdapter.updateItem(g);
                }
            } else if(g.isAssigned()){
                List<UpcomingPlan> plans = g.getPlans();
                for(UpcomingPlan up : plans){
                    if(!up.isAssigned())
                        super.deassign(up);
                }
                g.updateTotals();
                planRecyclerViewAdapter.updateItem(g);
            }
        }
        /*
        if(super.deassign(p)){
            planRecyclerViewAdapter.updateItem(p);
            return true;
        }
        */
        return false;


    }




    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }



    private class PlanRecyclerViewAdapter extends RecyclerView.Adapter<PlanRecyclerViewAdapter.RecyclerViewHolder>
        implements View.OnLongClickListener, View.OnClickListener{




        private OnPlanSelectedListener mSelectedListener;
        private List<UpcomingPlanGroup> mPlanList;
        private Plan mContextPlan;

        public PlanRecyclerViewAdapter(List<UpcomingPlanGroup> plans, OnPlanSelectedListener planListener) {
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
        public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new RecyclerViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.plan_list_item, parent, false));
        }



        @Override
        public void onBindViewHolder(RecyclerViewHolder holder, int position, List<Object> payloads) {
            //super.onBindViewHolder(holder, position, payloads);

            if (payloads.isEmpty()){
                super.onBindViewHolder(holder, position, payloads);
            }else {
                //Plan p = mPlanList.get(position);

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
                    if ("extqty".equals(key) || "cplqty".equals(key) || "remaining".equals(key)) {
                        updateQty = true;
                    }
                    if ("stu".equals(key)) {
                        holder.textViewCount.setText(o.getString("stu"));
                    }
                }
                if(updateQty){
                    double extqty = o.getDouble("extqty", 0);
                    double cplqty = o.getDouble("cplqty", 0);
                    double avaqty = o.getDouble("remaining", 0);
                    String stu = o.getString("stu", "");
                    double qty = Math.max(0, extqty - cplqty);

                    if(avaqty < qty){
                        holder.textViewCount.setText(String.format("%.2f (%.2f) %s", avaqty, qty, stu));
                        holder.textViewCount.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_dark ));

                    }else{
                        holder.textViewCount.setText(String.format("%.2f %s", avaqty, stu));
                        holder.textViewCount.setBackgroundColor(getResources().getColor(android.R.color.transparent ));
                    }

                }
            }
        }

        @Override
        public void onBindViewHolder(final RecyclerViewHolder holder, int position) {
            UpcomingPlanGroup p = mPlanList.get(position);
            boolean pending = false;
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
            double qty = Math.max(0, p.getExtqty() - p.getCplqty());
            double avaqty = p.getRemainingQty();
            String stu = p.getItmstu();
            boolean assigned = p.isAssigned();

            //holder.imageViewPending.setVisibility(pending ? View.VISIBLE : View.GONE);
            holder.layoutBackground.setBackgroundColor(getResources().getColor(assigned ? android.R.color.holo_orange_light : android.R.color.transparent));

            holder.textViewTitle.setText(itmref);
            holder.textViewPjt.setText(p.getPjt());
            holder.textViewDescription.setText(itmdes);
            //holder.textViewCount.setText(String.format("%.2f %s", qty, stu));

            if(avaqty < qty){
                holder.textViewCount.setText(String.format("%.2f (%.2f) %s", avaqty, qty, stu));
                holder.textViewCount.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_dark ));

            }else{
                holder.textViewCount.setText(String.format("%.2f/%.2f %s", avaqty, p.getUnfilteredQty(), stu));
                holder.textViewCount.setBackgroundColor(getResources().getColor(android.R.color.transparent ));
            }

            //holder.textViewCount.setText(String.format("%.2f (%.2f) %s", avaqty, qty, stu));
            //holder.textViewCount.setBackgroundColor(getResources().getColor(p.getPredecessorCompletionRatio() < 0.999 ? android.R.color.holo_orange_dark : android.R.color.transparent));

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
                JobPageFragment.super.onCreateContextMenu(contextMenu, view, contextMenuInfo);
                Plan p = (Plan) view.getTag();
                mContextPlan = p;

                MenuInflater inflater = getActivity().getMenuInflater();
                inflater.inflate(R.menu.menu_ctx_article, contextMenu);
                MenuItem itm = contextMenu.findItem(R.id.menu_itmref);
                itm.setOnMenuItemClickListener(mOnMyActionClickListener);

                //MenuItem sohnumList = contextMenu.findItem(R.id.sohnumList);
                contextMenu.add("test");


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
        public int getAssignedItemCount() {
            int count = 0;
            for(Plan p : mPlanList){
                if(p.isAssigned())
                    count++;
            }
            return count;
        }
        public List<Plan> getAssignedItems(){
            List<Plan> plans = new ArrayList<>();
            for(UpcomingPlanGroup p : mPlanList){
                if(p.isAssigned())
                    plans.addAll(p.getPlans());
            }
            return plans;
        }


        public void setItems(List<UpcomingPlanGroup> plans){
            setItems(plans, true);
        }
        public void setItems(List<UpcomingPlanGroup> plans, boolean notify) {
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
            //private ImageView imageViewPending;



            RecyclerViewHolder(View view) {
                super(view);
                layoutBackground = view.findViewById(R.id.layoutBackground);
                textViewTitle = (TextView) view.findViewById(R.id.title);
                textViewPjt = view.findViewById(R.id.pjt);
                textViewDescription = (TextView) view.findViewById(R.id.description);
                textViewCount = (TextView) view.findViewById(R.id.count);
                //imageViewPending = view.findViewById(R.id.imageViewPending);
            }



        }
    }

    public interface OnMaterialFilterChanged{

        public void onMaterialFitlerChanged(Set<String> filter);
    }
    private class MaterialRecyclerViewAdapter extends RecyclerView.Adapter<MaterialRecyclerViewAdapter.RecyclerViewHolder>{


        public class MaterialRecord{
            public String Itmref;
            public String Itmdes;
            //according to operation
            public double Qty;
            //according to WO material tracking
            public double MfmRequiredQty;
            public String STU;

            public List<Plan> Plans;

            public boolean FilterByMaterial;

            public MaterialRecord(){
                Plans = new ArrayList<>();
                FilterByMaterial = false;
            }


        }



        private List<MaterialRecord> mRecords;
        private MaterialRecord mContextRecord;

        private Set<String> mMaterialFilter;
        public Set<String> getMaterialFilter(){ return mMaterialFilter; }
        private OnMaterialFilterChanged mOnMaterialFilterChangedListener;
        public void setOnMaterialFilterChangedListener(OnMaterialFilterChanged l) {
            mOnMaterialFilterChangedListener = l;
        }

        private void OnMaterialFilterChanged(){
            if(mOnMaterialFilterChangedListener != null)
                mOnMaterialFilterChangedListener.onMaterialFitlerChanged(mMaterialFilter);
        }

        private Map<Plan, Double> mPlanRatioMap;

        public MaterialRecyclerViewAdapter() {
            this.mRecords = new ArrayList<>();
            mMaterialFilter = new HashSet<>();
            mPlanRatioMap = new HashMap<>();
            mOnMaterialFilterChangedListener = null;
        }



        @Override
        public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new RecyclerViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.material_list_item, parent, false));
        }

        @Override
        public void onBindViewHolder(final RecyclerViewHolder holder, int position) {
            MaterialRecord rec = mRecords.get(position);
            String itmref = rec.Itmref;
            String itmdes = rec.Itmdes;
            double qty = rec.Qty;
            String stu = rec.STU;

            Article mat = AppDatabase.getInstance(getContext()).getArticleDao().get(itmref);

            boolean available = true;
            if(mat != null){
                if(mat.getPhysto() < rec.MfmRequiredQty){
                    available = false;
                }
            }


            //holder.imageViewPending.setVisibility(available ? View.GONE : View.VISIBLE);

            if(mMaterialFilter.contains(rec.Itmref)){
                holder.imageViewFilter.setVisibility( View.VISIBLE );
            }else{
                holder.imageViewFilter.setVisibility( View.GONE );
                //holder.relativeLayoutMaterial.setBackgroundResource(android.R.drawable.list_selector_background);
            }


            holder.textViewTitle.setText(itmref);
            holder.textViewDescription.setText(itmdes);
            holder.textViewCount.setText(String.format("%.2f %s", qty, stu));
            holder.itemView.setTag(rec);

            holder.itemView.setOnCreateContextMenuListener(mOnCreateContextMenuListener);

        }

        private final View.OnCreateContextMenuListener mOnCreateContextMenuListener = new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
                JobPageFragment.super.onCreateContextMenu(contextMenu, view, contextMenuInfo);
                MaterialRecord rec = (MaterialRecord) view.getTag();
                mContextRecord = rec;

                Article a = AppDatabase.getInstance(getContext()).getCachedArticle(rec.Itmref);
                MenuInflater inflater = getActivity().getMenuInflater();
                //if(a.getReocod() == 3){
                    //manufacturing article
                //inflater.inflate(R.menu.menu_ctx_material, contextMenu);
                //}else{
                    //purchased component
                    inflater.inflate(R.menu.menu_ctx_material_pur, contextMenu);
                    MenuItem itm = contextMenu.findItem(R.id.menu_itmref_filter);
                    itm.setChecked(mMaterialFilter.contains(mContextRecord.Itmref));
                    itm.setOnMenuItemClickListener(mOnMyActionClickListener);
                //}

                itm = contextMenu.findItem(R.id.menu_itmref);
                itm.setOnMenuItemClickListener(mOnMyActionClickListener);



            }

        };


        private final MenuItem.OnMenuItemClickListener mOnMyActionClickListener = new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch(item.getItemId()) {
                    case R.id.menu_itmref:
                        Intent intent = new Intent(getContext(), ArticleDetailsActivity.class);
                        intent.putExtra("itmref", mContextRecord.Itmref);
                        startActivity(intent);
                        break;
                    case R.id.menu_itmref_filter:
                        item.setChecked(!item.isChecked());
                        mContextRecord.FilterByMaterial = item.isChecked();
                        notifyItemChanged(mRecords.indexOf(mContextRecord));
                        if(mContextRecord.FilterByMaterial)
                            mMaterialFilter.add(mContextRecord.Itmref);
                        else
                            mMaterialFilter.remove(mContextRecord.Itmref);
                        OnMaterialFilterChanged();
                        break;
                }
                return true;
            }
        };

        @Override
        public void onViewRecycled(MaterialRecyclerViewAdapter.RecyclerViewHolder holder) {
            holder.itemView.setTag(null);
            holder.itemView.setOnLongClickListener(null);
            holder.itemView.setOnClickListener(null);
            holder.itemView.setOnCreateContextMenuListener(null);
            super.onViewRecycled(holder);


        }

        @Override
        public int getItemCount() {
            return mRecords.size();
        }

        public boolean addPlan(Plan plan) {

            WorkMaterial.WorkMaterialDAO dao = AppDatabase.getInstance(getActivity()).getWorkMaterialDao();
            addPlan(plan, dao);
            notifyDataSetChanged();
            return true;
        }

        public boolean setPlans(List<Plan> plans) {
            mRecords.clear();
            WorkMaterial.WorkMaterialDAO dao = AppDatabase.getInstance(getActivity()).getWorkMaterialDao();
            for(Plan p : plans) {
                addPlan(p, dao);
            }
            notifyDataSetChanged();
            return true;
        }

        private boolean addPlan(Plan plan, WorkMaterial.WorkMaterialDAO dao) {

            //List<WorkMaterial> woMat = dao.getAll(plan.getMfgnum());
            List<WorkMaterial> woMat = AppDatabase.getInstance(getContext()).getCachedMaterials(plan.getMfgnum());

            if(woMat == null || woMat.size() <= 0){
                return false;
            }

            List<WorkMaterial> filter = new ArrayList<>(woMat.size());
            for(WorkMaterial m : woMat){
                if(m.getBomope() == plan.getOpenum()){
                    filter.add(m);
                }

            }
            if(filter.size() == 0)
                return false;

            woMat = filter;
            filter = null;

            double ratio = plan.getAssignedQty() > 0 ? (plan.getAssignedQty() / plan.getExtqty()) : 1.0;
            for(WorkMaterial m : woMat) {

                MaterialRecord ex = null;
                for (MaterialRecord rec : mRecords) {
                    if (rec.Itmref.equals(m.getItmref())) {
                        ex = rec;
                        break;
                    }
                }
                if (ex == null) {
                    ex = new MaterialRecord();
                    ex.Itmref = m.getItmref();
                    ex.Itmdes = m.getItmdes();
                    ex.STU = m.getStu();
                    ex.Qty = 0;
                    mRecords.add(ex);
                }
                //in case we have split plans we must consider the partial execution qty
                //double ratio = plan.getCplqty() > 0 ? 1.0 - (plan.getCplqty() / plan.getExtqty()) : 1.0;


                ex.Qty += ratio * m.getRetqty();
                ex.MfmRequiredQty += ratio*(m.getRetqty() - m.getUseqty());

                ex.Plans.add(plan);

            }
            mPlanRatioMap.put(plan, ratio);

            return true;
        }

        public boolean removePlan(Plan plan) {

            WorkMaterial.WorkMaterialDAO dao = AppDatabase.getInstance(getActivity()).getWorkMaterialDao();
            if(removePlan(plan, dao)) {
                notifyDataSetChanged();
            }
            return true;
        }
        private boolean removePlan(Plan plan, WorkMaterial.WorkMaterialDAO dao) {

            //List<WorkMaterial> woMat = dao.getAll(plan.getMfgnum());
            List<WorkMaterial> woMat = AppDatabase.getInstance(getContext()).getCachedMaterials(plan.getMfgnum());

            if(woMat == null || woMat.size() <= 0){
                return false;
            }

            double ratio = mPlanRatioMap.containsKey(plan) ? mPlanRatioMap.get(plan) : 0;
            for(WorkMaterial m : woMat) {

                MaterialRecord ex = null;
                for (MaterialRecord rec : mRecords) {
                    if (rec.Itmref.equals(m.getItmref())) {
                        ex = rec;
                        break;
                    }
                }
                if (ex == null) {
                    continue;
                }

                //double ratio = plan.getCplqty() > 0 ? 1.0 - (plan.getCplqty() / plan.getExtqty()) : 1.0;

                ex.Qty -= ratio * m.getRetqty();
                ex.MfmRequiredQty -= ratio*(m.getRetqty() - m.getUseqty());
                ex.Plans.remove(plan);

                if(ex.Qty <= 0.001) {
                    mRecords.remove(ex);
                    mMaterialFilter.remove(ex.Itmref);
                    OnMaterialFilterChanged();
                }

            }
            mPlanRatioMap.remove(plan);

            return true;
        }


        class RecyclerViewHolder extends RecyclerView.ViewHolder {
            private TextView textViewTitle;
            private TextView textViewDescription;
            private TextView textViewCount;
            //private ImageView imageViewPending;
            private ImageView imageViewFilter;
            private View relativeLayoutMaterial;


            RecyclerViewHolder(View view) {
                super(view);
                relativeLayoutMaterial = view.findViewById((R.id.relativeLayoutMaterial));
                textViewTitle = (TextView) view.findViewById(R.id.title);
                textViewDescription = (TextView) view.findViewById(R.id.description);
                textViewCount = (TextView) view.findViewById(R.id.count);
                //imageViewPending = view.findViewById(R.id.imageViewPending);
                imageViewFilter = view.findViewById(R.id.imageViewFilter);

            }




        }
    }


    private class ItemRecyclerViewAdapter extends RecyclerView.Adapter<ItemRecyclerViewAdapter.RecyclerViewHolder>{

        public class ItemRecord{
            public String Itmref;
            public String Itmdes;
            public double Qty;
            public String STU;

            public List<Plan> Plans;

            public ItemRecord(){
                Plans = new ArrayList<>();
            }


        }


        private List<ItemRecord> mRecords;
        private ArrayList<Plan> mPlans;
        private ItemRecord mContextRecord;
        private Map<Plan, Double> mPlanAssignedQtys;


        public ItemRecyclerViewAdapter() {

            this.mRecords = new ArrayList<>();
            this.mPlans = new ArrayList<>();
            mContextRecord = null;
            mPlanAssignedQtys = new HashMap<>();
        }

        @Override
        public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new RecyclerViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_list_item, parent, false));
        }

        @Override
        public void onBindViewHolder(final RecyclerViewHolder holder, int position) {
            ItemRecord rec = mRecords.get(position);
            String itmref = rec.Itmref;
            String itmdes = rec.Itmdes;
            double qty = rec.Qty;
            String stu = rec.STU;


            holder.textViewTitle.setText(itmref);
            holder.textViewDescription.setText(itmdes);
            holder.textViewCount.setText(String.format("%.2f %s", qty, stu));
            //holder.nameTextView.setText(borrowModel.getPersonName());
            //holder.dateTextView.setText(borrowModel.getBorrowDate().toLocaleString().substring(0, 11));
            holder.itemView.setTag(rec);
            holder.itemView.setOnCreateContextMenuListener(mOnCreateContextMenuListener);
        }

        private final View.OnCreateContextMenuListener mOnCreateContextMenuListener = new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
                JobPageFragment.super.onCreateContextMenu(contextMenu, view, contextMenuInfo);
                ItemRecord rec = (ItemRecord) view.getTag();
                mContextRecord = rec;

                MenuInflater inflater = getActivity().getMenuInflater();
                inflater.inflate(R.menu.menu_ctx_article, contextMenu);
                MenuItem itm = contextMenu.findItem(R.id.menu_itmref);
                itm.setOnMenuItemClickListener(mOnMyActionClickListener);
            }

        };



        private final MenuItem.OnMenuItemClickListener mOnMyActionClickListener = new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent(getContext(), ArticleDetailsActivity.class);
                intent.putExtra("itmref", mContextRecord.Itmref);
                startActivity(intent);
                return true;
            }
        };


        @Override
        public void onViewRecycled(ItemRecyclerViewAdapter.RecyclerViewHolder holder) {
            holder.itemView.setTag(null);
            holder.itemView.setOnLongClickListener(null);
            holder.itemView.setOnClickListener(null);
            holder.itemView.setOnCreateContextMenuListener(null);
            super.onViewRecycled(holder);
        }

        @Override
        public int getItemCount() {
            return mRecords.size();
        }

        public ArrayList<Plan> getPlans(){
            return mPlans;
        }

        public boolean addPlan(Plan plan) {

            WorkItem.WorkItemDAO dao = AppDatabase.getInstance(getActivity()).getWorkItemDao();
            addPlan(plan, dao);
            notifyDataSetChanged();
            return true;
        }

        public boolean setPlans(List<Plan> plans) {
            mRecords.clear();
            mPlans.clear();
            WorkItem.WorkItemDAO dao = AppDatabase.getInstance(getActivity()).getWorkItemDao();
            for(Plan p : plans) {
                addPlan(p, dao);
            }
            notifyDataSetChanged();
            return true;
        }

        private boolean addPlan(Plan plan, WorkItem.WorkItemDAO dao) {

            //List<WorkItem> woItem = dao.getAll(plan.getMfgnum());
            List<WorkItem> woItem = AppDatabase.getInstance(getContext()).getCachedItems(plan.getMfgnum());

            if(woItem == null || woItem.size() <= 0){
                return false;
            }


            for(WorkItem i : woItem) {

                ItemRecord ex = null;
                for (ItemRecord rec : mRecords) {
                    if (rec.Itmref.equals(i.getItmref())) {
                        ex = rec;
                        break;
                    }
                }
                if (ex == null) {
                    ex = new ItemRecord();
                    ex.Itmref = i.getItmref();
                    ex.Itmdes = i.getItmdes();
                    ex.STU = i.getStu();
                    ex.Qty = 0;
                    mRecords.add(ex);
                }

                //ex.Qty += (plan.getExtqty() - plan.getCplqty());
                ex.Qty += plan.getAssignedQty();
                mPlanAssignedQtys.put(plan, plan.getAssignedQty());
                ex.Plans.add(plan);
            }
            mPlans.add(plan);

            return true;
        }

        public boolean removePlan(Plan plan) {

            WorkItem.WorkItemDAO dao = AppDatabase.getInstance(getActivity()).getWorkItemDao();
            if(removePlan(plan, dao)) {
                notifyDataSetChanged();
            }
            return true;
        }
        private boolean removePlan(Plan plan, WorkItem.WorkItemDAO dao) {

            //List<WorkItem> woItem = dao.getAll(plan.getMfgnum());
            List<WorkItem> woItem = AppDatabase.getInstance(getContext()).getCachedItems(plan.getMfgnum());

            if(woItem == null || woItem.size() <= 0){
                return false;
            }


            for(WorkItem i : woItem) {

                ItemRecord ex = null;
                for (ItemRecord rec : mRecords) {
                    if (rec.Itmref.equals(i.getItmref())) {
                        ex = rec;
                        break;
                    }
                }
                if (ex == null) {
                    continue;
                }

                //ex.Qty -= (plan.getExtqty() - plan.getCplqty());
                ex.Qty -= mPlanAssignedQtys.get(plan);
                mPlanAssignedQtys.remove(plan);
                ex.Plans.remove(plan);
                if(ex.Qty <= 0.01) {
                    mRecords.remove(ex);

                }

            }

            mPlans.remove(plan);

            return true;
        }


        class RecyclerViewHolder extends RecyclerView.ViewHolder {
            private TextView textViewTitle;
            private TextView textViewDescription;
            private TextView textViewCount;


            RecyclerViewHolder(View view) {
                super(view);
                textViewTitle = (TextView) view.findViewById(R.id.title);
                textViewDescription = (TextView) view.findViewById(R.id.description);
                textViewCount = (TextView) view.findViewById(R.id.count);
            }
        }
    }

}
