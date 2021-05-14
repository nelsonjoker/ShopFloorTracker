package pt.sotubo.shopfloortracker.jobpage;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import pt.sotubo.shopfloortracker.R;
import pt.sotubo.shopfloortracker.TrackingConfirmActivity;
import pt.sotubo.shopfloortracker.model.Job;
import pt.sotubo.shopfloortracker.model.Plan;
import pt.sotubo.shopfloortracker.model.UpcomingPlan;
import pt.sotubo.shopfloortracker.model.UpcomingPlanGroup;
import pt.sotubo.shopfloortracker.model.sync.SyncService;


/**
 * Created by Nelson on 03/04/2018.
 */


interface OnPlanSelectedListener{

    void OnItemClicked(Plan p);
    void OnItemSelected(Plan p);

}

public abstract class AbstractJobPage extends Fragment implements OnPlanSelectedListener{

    public static final String ARG_JOB = "job";
    protected static final String ARG_TICKING = "ticking";
    protected static final String ARG_PAUSED = "paused";
    protected static final String ARG_DURATION = "duration";

    protected final static int RESULT_TRACKING_CONFIRM = 100;

    private Job mJob;
    public Job getJob() { return mJob; }
    public void setJob(Job j) { mJob = j; }
    public String getTitle() { return  mJob.getName(); }

    private View viewSeparator;

    protected Toolbar mToolbar;
    private SearchView mSearchFilter;


    /** Messenger for communicating with service. */
    Messenger mService = null;
    /** Flag indicating whether we have called bind on the service. */
    boolean mIsBound = false;
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */

    protected abstract Messenger getMessenger();

    private boolean isSelected;
    public boolean isSelected(){ return isSelected; }
    public void setSelected(boolean s ) {

        isSelected = s;
        if(!isSelected)
            onPauseUpdates();
        else{
            if(isResumed()) {
                onResumeUpdates();
            }
        }

    }

    protected String mSearchFilterText;
    protected String getSearchFilterText() { return mSearchFilter.getQuery().toString(); }
    protected abstract void onSearchFilterChanged(String filter);


    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if(isVisibleToUser){
            //onResumeUpdates();
        }else{
            //onPauseUpdates();
        }

    }

    private  Handler mResumeUpdatesHandler;

    public AbstractJobPage(){
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mJob = getArguments().getParcelable(ARG_JOB);
        }



        mResumeUpdatesHandler = new Handler( getActivity().getMainLooper());


    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("mSearchFilterText", getSearchFilterText());

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewSeparator = view.findViewById(R.id.viewSeparator);



        mToolbar = view.findViewById(R.id.toolbar_plan);
        mToolbar.inflateMenu(R.menu.menu_plan);

        mSearchFilter = (SearchView) mToolbar.getMenu().findItem(R.id.item_search).getActionView();
        mSearchFilter.setInputType(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        mSearchFilterText = ""; //mSearchFilter.getQuery().toString();
        mSearchFilter.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mSearchFilterText = query;
                onSearchFilterChanged(getSearchFilterText());
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                onPauseUpdates();
                if(!mSearchFilterText.equalsIgnoreCase(newText) && "".equals(newText)){
                    mSearchFilterText = newText;
                    onSearchFilterChanged(getSearchFilterText());
                    onResumeUpdates();
                }
                return false;
            }
        });

        mToolbar.setOnMenuItemClickListener(item -> {

            switch (item.getItemId()) {


                case R.id.item_save_tracking:

                    if (getAssignedCount() <= 0) {
                        Snackbar.make(getView(), "Nenhuma operação seleccionada", Snackbar.LENGTH_LONG)
                                .setAction("Erro", null).show();
                    } else {

                        Intent intent = new Intent(getActivity(), TrackingConfirmActivity.class);

                        List<Plan> selected = getAssignedPlanList();

                        int c =  selected.size();
                        List<String> plans = new ArrayList<>(c);
                        for (int i = 0; i < c; i++) {
                            Plan p = selected.get(i);
                            if (p.isAssigned() && p.getAssignmentCode().equals(getJob().getAssignmentCode())) {
                                plans.add(p.getId());
                            }
                        }
                        intent.putExtra("plan_ids", plans.toArray(new String[plans.size()]));
                        intent.putExtra("duration", -1);
                        intent.putExtra("wcr", getJob().getWcr());
                        intent.putExtra("wst", getJob().getWst());
                        intent.putExtra("operator", getJob().getOperator());

                        startActivityForResult(intent, RESULT_TRACKING_CONFIRM);
                    }
                    break;

            }

            return true;
        });
/*
        if(savedInstanceState != null){
            if(savedInstanceState.containsKey("mSearchFilterText")){
                mSearchFilterText = savedInstanceState.getString("mSearchFilterText");
                onSearchFilterChanged(mSearchFilterText.toUpperCase());
            }
        }
*/

    }

    @Override
    public void onStart() {



        //Log.d(TAG, "started "+mJob.getName());
        super.onStart();
        // Bind to LocalService
        doBindService();
    }


    @Override
    public void onPause() {
        mResumeUpdatesHandler.removeCallbacks(delayedResumeUpdates);
        onPauseUpdates();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(isSelected()) {
            onResumeUpdates();
        }
    }



    public abstract void onResumeUpdates();
    public abstract void onPauseUpdates();

    private Runnable delayedResumeUpdates = new Runnable() {
        @Override
        public void run() {
            if(isSelected()) {
                onResumeUpdates();
            }
        }
    };

    public void onPauseUpdates(long resumeBackoff){
        mResumeUpdatesHandler.removeCallbacks(delayedResumeUpdates);
        onPauseUpdates();
        mResumeUpdatesHandler.postDelayed(delayedResumeUpdates, resumeBackoff);

    }



    @Override
    public void onStop() {
        //Log.d(TAG, "stoped "+mJob.getName());
        super.onStop();
        doUnbindService();
    }
    protected void doBindService() {

        if(mIsBound)
            return;

        // Establish a connection with the service.  We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        getActivity().bindService(new Intent(getContext(),
                SyncService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    protected void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null,
                            SyncService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = getMessenger();
                    mService.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
            }

            // Detach our existing connection.
            getActivity().unbindService(mConnection);
            mIsBound = false;
        }
    }
    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            mService = new Messenger(service);

            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                Message msg = Message.obtain(null,
                        SyncService.MSG_REGISTER_CLIENT);
                msg.replyTo = getMessenger();
                mService.send(msg);

            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
        }
    };


    public abstract List<Plan> getAssignedPlanList();
    public abstract int getAssignedCount();

    protected void setLoading(boolean l){
        if(l){
            viewSeparator.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_dark));
        }else{
            viewSeparator.setBackgroundColor(getResources().getColor(android.R.color.black));
        }
    }








    @Override
    public void OnItemClicked(Plan p) {
        try {
            if (p.isAssigned())
                deassign(p);
            else
                assign(p);
        } catch (Exception e) {
            e.printStackTrace();
            Snackbar.make(getView(), e.getMessage(), Snackbar.LENGTH_LONG)
                    .setAction("Erro", null).show();
        }
    }

    @Override
    public void OnItemSelected(Plan p) {
        //item long pressed



    }

    protected boolean reloadArticle(String itmref) throws Exception {


        if (mIsBound) {

            Message msg = Message.obtain(null, SyncService.MSG_RELOAD_ARTICLE);
            msg.getData().putString(SyncService.ARG_KEY, itmref);
            msg.replyTo = null;
            mService.send(msg); //TODO: not blocking!! if not we may have concurrency from view model


            return true;

        } else {
            throw new Exception("De momento não é possível realizar a operação, tente mais tarde");
        }

    }

    protected boolean assign(Plan p) throws Exception {
        double mx = p.getExtqty() - p.getCplqty() - p.getAssignedQty();

        if(p instanceof UpcomingPlan){
            UpcomingPlan up = (UpcomingPlan)p;
            mx = up.getRemainingQty() - p.getAssignedQty();
        }
        return assign(p, mx);
    }

    protected boolean assign(Plan p, double qty) throws Exception {


        if (mIsBound) {
            onPauseUpdates(5000);

            p.assign(getJob().getAssignmentCode(), qty);
            //mPlanViewModel.updatePlan(p);


            Message msg = Message.obtain(null, SyncService.MSG_UPDATE_PLAN);
            msg.getData().putParcelable(SyncService.ARG_OBJ, p);
            msg.replyTo = getMessenger();
            mService.send(msg); //TODO: not blocking!! if not we may have concurrency from view model


            return true;

        } else {
            throw new Exception("De momento não é possível realizar a operação, tente mais tarde");
        }

    }

    protected boolean deassign(Plan p) throws Exception {

        if (mIsBound) {
            onPauseUpdates(5000);
            p.deassing();

            Message msg = Message.obtain(null, SyncService.MSG_UPDATE_PLAN);
            msg.getData().putParcelable(SyncService.ARG_OBJ, p);
            msg.replyTo = getMessenger();
            mService.send(msg); //TODO: this is not bocking... we may have concurrency from view model


            return true;

        } else {
            throw new Exception("De momento não é possível realizar a operação, tente mais tarde");
        }


    }





}
