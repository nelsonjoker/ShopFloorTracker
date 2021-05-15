package com.joker.shopfloortracker;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import android.support.v7.app.ActionBar;
import android.view.MenuItem;


import com.joker.shopfloortracker.model.AlertMessageContent;
import com.joker.shopfloortracker.model.AlertMessageType;
import com.joker.shopfloortracker.model.AppDatabase;
import com.joker.shopfloortracker.model.Job;
import com.joker.shopfloortracker.model.Plan;
import com.joker.shopfloortracker.model.sync.SyncService;

import java.util.ArrayList;
import java.util.List;

import static android.support.v4.app.NavUtils.navigateUpFromSameTask;

/**
 * An activity representing a list of AlertMessages. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link AlertMessageDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class AlertMessageListActivity extends AppCompatActivity {

    private static final String TAG = AlertMessageListActivity.class.getSimpleName();

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;


    /** Messenger for communicating with service. */
    Messenger mService = null;
    /** Flag indicating whether we have called bind on the service. */
    boolean mIsBound = false;
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new AlertMessageListActivity.IncomingHandler());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alertmessage_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());


        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (findViewById(R.id.alertmessage_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }

        View recyclerView = findViewById(R.id.alertmessage_list);
        assert recyclerView != null;
        setupRecyclerView((RecyclerView) recyclerView);


        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mTwoPane) {
                    Fragment f = getSupportFragmentManager().findFragmentById(R.id.alertmessage_detail_container);
                    AlertMessageDetailFragment al = (AlertMessageDetailFragment) f;

                    AlertMessageContent alert = al.getMessage();
                    if(alert != null){
                        Message msg = Message.obtain(null, SyncService.MSG_SEND_ALERT);
                        msg.getData().putParcelable(SyncService.ARG_OBJ, alert);
                        try {
                            mService.send(msg);

                            setResult(Activity.RESULT_OK);
                            finish();

                        } catch (RemoteException e) {
                            Log.e(TAG, e.getMessage());
                            e.printStackTrace();
                            Snackbar.make(view, e.getLocalizedMessage(), Snackbar.LENGTH_LONG)
                                    .setAction("Erro", null).show();
                        }


                    }
                }

            }
        });

    }

    @Override
    public boolean onNavigateUp() {
        onBackPressed();
        return true;
        //return super.onNavigateUp();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
        //return super.onSupportNavigateUp();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            //navigateUpFromSameTask(this);
            setResult(RESULT_CANCELED);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {

        AlertMessageType.AlertMessageTypeDAO dao = AppDatabase.getInstance(this).getAlertMessageTypeDao();
        List<AlertMessageType> types = dao.getAll();

        recyclerView.setAdapter(new SimpleItemRecyclerViewAdapter(this, types, mTwoPane));
    }



    @Override
    public void onStart() {
        super.onStart();
        // Bind to LocalService
        doBindService();
    }

    @Override
    public void onStop() {
        super.onStop();
        doUnbindService();
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
            this.obtainMessage();
            Bundle data = msg.getData();
            switch (msg.what) {

                default:
                    super.handleMessage(msg);
            }
        }
    }




    public static class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private final AlertMessageListActivity mParentActivity;
        private final List<AlertMessageType> mValues;
        private final boolean mTwoPane;
        private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertMessageType item = (AlertMessageType) view.getTag();

                Intent i = mParentActivity.getIntent();
                Job job = i.getParcelableExtra("job");
                ArrayList<Plan> plans = i.getParcelableArrayListExtra("plans");

                if (mTwoPane) {
                    Bundle arguments = new Bundle();
                    arguments.putParcelable(AlertMessageDetailFragment.ARG_TYPE, item);
                    arguments.putParcelable(AlertMessageDetailFragment.ARG_JOB, job);
                    arguments.putParcelableArrayList(AlertMessageDetailFragment.ARG_PLANS, plans);

                    AlertMessageDetailFragment fragment = new AlertMessageDetailFragment();
                    fragment.setArguments(arguments);
                    mParentActivity.getSupportFragmentManager().beginTransaction()
                            .replace(R.id.alertmessage_detail_container, fragment)
                            .commit();
                } else {
                    Context context = view.getContext();
                    Intent intent = new Intent(context, AlertMessageDetailActivity.class);
                    intent.putExtra(AlertMessageDetailFragment.ARG_TYPE, (Parcelable) item);
                    intent.putExtra(AlertMessageDetailFragment.ARG_JOB, (Parcelable)job);
                    intent.putParcelableArrayListExtra(AlertMessageDetailFragment.ARG_PLANS, plans);

                    context.startActivity(intent);
                }
            }
        };

        SimpleItemRecyclerViewAdapter(AlertMessageListActivity parent,
                                      List<AlertMessageType> items,
                                      boolean twoPane) {
            mValues = items;
            mParentActivity = parent;
            mTwoPane = twoPane;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.alertmessage_list_content, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            AlertMessageType t = mValues.get(position);

            holder.textViewTitle.setText(t.getTitle());
            holder.textViewDescription.setText(t.getDescription());

            holder.itemView.setTag(t);
            holder.itemView.setOnClickListener(mOnClickListener);
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView textViewTitle;
            final TextView textViewDescription;

            ViewHolder(View view) {
                super(view);
                textViewTitle = view.findViewById(R.id.textViewTitle);
                textViewDescription = view.findViewById(R.id.textViewDescription);
            }
        }
    }
}
