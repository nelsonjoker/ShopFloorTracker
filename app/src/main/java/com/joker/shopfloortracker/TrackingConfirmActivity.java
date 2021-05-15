package com.joker.shopfloortracker;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.joker.shopfloortracker.model.AppDatabase;
import com.joker.shopfloortracker.model.Plan;
import com.joker.shopfloortracker.model.UpcomingPlan;
import com.joker.shopfloortracker.model.UpcomingPlanGroup;
import com.joker.shopfloortracker.model.sync.SyncService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TrackingConfirmActivity extends AppCompatActivity {


    private String mWCR;
    private String mWST;
    private String mOperator;

    private ArrayList<UpcomingPlanGroup> producedItems;
    private ArrayList<UpcomingPlan> mSelectedPlans;

    private TableLayout mItemTable;
    private ProgressDialog mProgressDialog;

    /** Messenger for communicating with service. */
    Messenger mService = null;
    /** Flag indicating whether we have called bind on the service. */
    boolean mIsBound = false;
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new TrackingConfirmActivity.IncomingHandler());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking_confirm);

        setTitle(R.string.title_activity_tracking_confirm);

        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mItemTable = findViewById(R.id.tableTrackingSummaryItems);



        Intent intent = getIntent();
        //setDuration(intent.getLongExtra("duration", 0L));
        String[] planIds = intent.getStringArrayExtra("plan_ids");
        List<UpcomingPlan> plans = AppDatabase.getInstance(this).getUpcomingPlanDao().getUpcoming(planIds);
        List<String> mfgnums = new ArrayList<>(plans.size());

        mSelectedPlans = new ArrayList<>(planIds.length);

        for(UpcomingPlan p : plans){
            if(!mfgnums.contains(p.getMfgnum()))
                mfgnums.add(p.getMfgnum());
            mSelectedPlans.add(p);
        }

        producedItems = new ArrayList<>();
        HashMap<String, UpcomingPlanGroup> groupingItems = new HashMap<>();
        for(UpcomingPlan p : plans){

            UpcomingPlanGroup ex = groupingItems.containsKey(p.getItmref()) ? groupingItems.get(p.getItmref()) : null;
            if(ex == null){
                ex = new UpcomingPlanGroup(p);
                producedItems.add(ex);
                groupingItems.put(p.getItmref(), ex);
            }
            ex.add(p);

        }

        for(UpcomingPlanGroup g : producedItems){
            g.setCplqty(g.getAssignedQty()); //default full clear
        }




        mWCR = intent.getStringExtra("wcr");
        mWST = intent.getStringExtra("wst");
        mOperator = intent.getStringExtra("operator");


        if(savedInstanceState != null){

            for(UpcomingPlanGroup i : producedItems){
                String k = "itm_cplqty_"+i.getItmref();
                if(savedInstanceState.containsKey(k)){
                    i.setCplqty(savedInstanceState.getDouble(k));
                }
            }
        }

        Button btnOK = findViewById(R.id.buttonOK);
        btnOK.setFocusable(true);
        btnOK.setFocusableInTouchMode(true);///add this line

        Button btnCancel = findViewById(R.id.buttonCancel);

        btnOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnOK.requestFocus(); //trigger those onFocusChangedListeners
                //post the results to the server
                if(validate()) {
                    if(commitTracking()) {
                        setResult(RESULT_OK);
                        finish();
                    }
                }
                //TODO: show some loading spinner
                //and wait for reply from service


            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        });




    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        //https://stackoverflow.com/questions/30549722/onsaveinstancestate-is-not-getting-called-after-screen-rotation
        super.onSaveInstanceState(outState);

        for(UpcomingPlanGroup i : producedItems){
            String k = "itm_cplqty_"+i.getItmref();
            outState.putDouble(k, i.getCplqty());
        }
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

    private void addItemRow(UpcomingPlanGroup itm){
        LayoutInflater inflater = this.getLayoutInflater();
        TableRow row = (TableRow)inflater.inflate(R.layout.tracking_material_row, null);
        row.setTag(itm);

        TextView itmref = row.findViewById(R.id.textViewItmref);
        TextView itmdes = row.findViewById(R.id.textViewItmdes);
        TextView retqty = row.findViewById(R.id.textViewRetqty);
        EditText useqty = row.findViewById(R.id.editTextUseqty);
        TextView stu = row.findViewById(R.id.textViewSTU);

        itmref.setText(itm.getItmref());
        itmdes.setText(itm.getItmdes());
        retqty.setText(String.format("%.2f", itm.getAssignedQty()));
        //retqty.setText(String.format("%.2f", itm.getCplqty()));

        useqty.setText(String.format("%.2f", itm.getCplqty()));//abusive repurpose of field

        useqty.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean focus) {
                if(!focus){
                    validate();
                }
            }
        });

        stu.setText(itm.getItmstu());

        mItemTable.addView(row);



    }

    protected boolean validate(){

        boolean res = true;
        int c = mItemTable.getChildCount();
        for(int i = 0; i < c; i++){
            View v = mItemTable.getChildAt(i);
            if(v instanceof TableRow){
                TableRow r = (TableRow)v;
                if(!validate(r))
                    res = false;
            }
        }
        return res;
    }

    protected boolean validate(TableRow r){

        UpcomingPlanGroup itm = (UpcomingPlanGroup) r.getTag();
        if(itm == null){
            return true; //header row ???
        }

        EditText t = (EditText)r.findViewById(R.id.editTextUseqty);
        String val = t.getText().toString();
        try {
            double v = Double.parseDouble(val.replace(',', '.'));
            double oldQty = itm.getCplqty();

            if(Math.abs(oldQty-v) < 0.001)
                return true;

            double rem = itm.getAssignedQty(); // mItemsCompletionRatio.get(itm);

            if(v > rem){

                t.setText(String.format("%.2f", oldQty));

                new AlertDialog.Builder(TrackingConfirmActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setTitle("Quantidade máxima excedida...")
                        .setMessage("Para aumentar a quantidade produzida seleccione operações adicionais...")
                        .setPositiveButton("OK", null)
                        .show();
            }else {


                itm.setCplqty(v);
                t.setText(String.format("%.2f", v));
                t.setBackgroundColor(getResources().getColor(android.R.color.transparent));


            }

        }catch(NumberFormatException e){
            t.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
        }

        return false;

    }


    private boolean commitTracking(){
        try{
            if(mIsBound){

                if(mProgressDialog != null)
                    mProgressDialog.dismiss();

                mProgressDialog = new ProgressDialog(this);
                mProgressDialog.setTitle("A atualizar");
                mProgressDialog.setMessage("Operação em curso ...");
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                mProgressDialog.setCanceledOnTouchOutside(false); // main method that force user cannot click outside
                mProgressDialog.setCancelable(false);
                mProgressDialog.show();

                for(UpcomingPlanGroup g : producedItems){
                    if(g.getCplqty() <= 0)
                        continue;

                    double q = g.getCplqty();
                    for(Plan p : g.getPlans()){
                        if(q <= 0)
                            continue;
                        double avail = p.getExtqty() - p.getCplqty();
                        avail = Math.min(avail, q);
                        if(avail <= 0)
                            continue;
                        q-= avail;
                        p.setCplqty(p.getCplqty() + avail);
                        p.setAssignedQty(Math.max(0, p.getAssignedQty() - avail));
                        if(p.getCplqty() >= p.getExtqty())
                            p.setCompleted(true);

                        Message msg = Message.obtain(null, SyncService.MSG_UPDATE_PLAN);
                        msg.getData().putParcelable(SyncService.ARG_OBJ, p);
                        msg.replyTo = null;
                        mService.send(msg);
                    }

                }

                mProgressDialog.dismiss();



            }else{
                throw new Exception("De momento não é possível realizar a operação, tente mais tarde");
            }

        } catch (Exception e) {
            e.printStackTrace();
            Snackbar.make(TrackingConfirmActivity.this.mItemTable, e.getMessage(), Snackbar.LENGTH_LONG)
                    .setAction("Erro", null).show();
        }
        return true;
    }




    @Override
    public void onStop() {
        super.onStop();
        doUnbindService();
    }


    @Override
    public void onStart() {
        super.onStart();
        // Bind to LocalService
        doBindService();



    }

    @Override
    protected void onResume() {
        super.onResume();

        mItemTable.removeAllViews();

        for(UpcomingPlanGroup i : producedItems){
            addItemRow(i);
        }

    }

    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        bindService(new Intent(this,
                SyncService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null,
                            SyncService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
            }

            // Detach our existing connection.
            unbindService(mConnection);
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
                msg.replyTo = mMessenger;
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



    /**
     * Handler of incoming messages from service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            //https://stackoverflow.com/questions/15868635/thread-handler-error-the-specified-message-queue-synchronization-barrier-token
//            this.obtainMessage();
//            Bundle data = msg.getData();
//            switch (msg.what) {
//                case SyncService.MSG_COMMIT_TRACKING:
//
//                        boolean result = false;
//                        if(data.containsKey(SyncService.ARG_RESULT)){
//                            result = data.getBoolean(SyncService.ARG_RESULT);
//                        }
//                        Log.d("TrackingConfirmActivity", "Result from tracking : "+result);
//
//                        if(mProgressDialog != null){
//                            mProgressDialog.dismiss();
//                        }
//                        if(result) {
//                            //make an optimistic gess that everything went OK and update plans
//                            //latter on the server will provide updated info
//                            Intent r = new Intent();
//                            r.putExtras(data);
//                            setResult(RESULT_OK, r);
//                            finish();
//                        }
//                        break;
//                default:
//                    super.handleMessage(msg);
//            }
        }
    }

}
