package com.joker.shopfloortracker;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.WindowManager;

import net.danlew.android.joda.JodaTimeAndroid;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import com.joker.shopfloortracker.jobpage.AbstractJobPage;
import com.joker.shopfloortracker.jobpage.JobPageFactory;
import com.joker.shopfloortracker.jobpage.JobPageFragment;
import com.joker.shopfloortracker.model.AppDatabase;
import com.joker.shopfloortracker.model.Job;
import com.joker.shopfloortracker.model.Plan;
import com.joker.shopfloortracker.model.sync.SyncService;

public class MainActivity extends AppCompatActivity implements JobPageFragment.OnFragmentInteractionListener {

    private final static int RESULT_CREATE_JOB = 100;
    private final static int RESULT_EDIT_JOB = 101;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    private AppDatabase mDataBase;

    /** Messenger for communicating with service. */
    Messenger mService = null;
    /** Flag indicating whether we have called bind on the service. */
    boolean mIsBound = false;
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new MainActivity.IncomingHandler());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JodaTimeAndroid.init(this);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);



        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));
        tabLayout.setupWithViewPager(mViewPager);

        mDataBase = AppDatabase.getInstance(getApplicationContext());

        Intent mServiceIntent = new Intent(this, SyncService.class);
        this.startService(mServiceIntent);

        Job.JobDAO jDAO = mDataBase.getJobDao();
        List<Job> jobs = jDAO.getAll();

        if(jobs.size() > 0){
            for(Job j : jobs){
                //AbstractJobPage frag = AbstractJobPage.newPage(j);
                mSectionsPagerAdapter.addJobPage(j);
            }
        }


        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            AbstractJobPage frag = mSectionsPagerAdapter.getCurrentFragment();
            if(frag != null) {
                //TODO: should allow application error reporting
                Intent intent = new Intent(this, AlertMessageListActivity.class);
                intent.putExtra("job", (Parcelable) frag.getJob());
                ArrayList<Plan> arr = new ArrayList<>(frag.getAssignedPlanList());
                intent.putParcelableArrayListExtra("plans", arr);
                startActivity(intent);
            }
        });



    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        boolean pageSelected = mSectionsPagerAdapter.getCount() > 0;

        MenuItem edit = menu.findItem(R.id.action_edit_job);
        edit.setEnabled(pageSelected);
        MenuItem remove = menu.findItem(R.id.action_delete_job);
        remove.setEnabled(pageSelected);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_resync) {

            int currentItemIndex = mViewPager.getCurrentItem();


            if(currentItemIndex >= 0) {

                new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Recarregar dados?")
                        .setMessage("Esta operação irá descartar dados locais e recuperar todos os dados remotos, pode ser demorada. Deseja continuar?")
                        .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                if(mIsBound){
                                    Message msg = Message.obtain(null,
                                            SyncService.MSG_RESYNC_PLAN);
                                    try {
                                        mService.send(msg);
                                    } catch (RemoteException e) {
                                        e.printStackTrace();
                                    }
                                }else{

                                    Snackbar.make(mViewPager, "Não foi possível realizar a operação, tente mais tarde...", Snackbar.LENGTH_LONG)
                                            .setAction("Erro", null).show();
                                }

                            }

                        })
                        .setNegativeButton("Não", null)
                        .show();



            }
            return true;
        }else if(id == R.id.action_new_job){

            Intent intent = new Intent(this, JobDetailsActivity.class);

            startActivityForResult(intent, RESULT_CREATE_JOB);
  /*
            Job j = new Job();
            j.setName("test it");
            AbstractJobPage frag = AbstractJobPage.newPage(j);
            mSectionsPagerAdapter.addJobPage(frag);
*/
        }else if(id == R.id.action_edit_job){
            int currentItemIndex = mViewPager.getCurrentItem();
            if(currentItemIndex >= 0) {

                AbstractJobPage frag = (AbstractJobPage) mSectionsPagerAdapter.getCurrentFragment();
                if(frag.getAssignedCount() > 0) {

                    new AlertDialog.Builder(this)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle("Não é possível realizar a operação")
                            .setMessage("Não é possível alterar definições do separador enquanto houver operações seleccionadas.")
                            .setPositiveButton("OK", null)
                            .show();
                }else {

                    Job j = mSectionsPagerAdapter.getDataItem(currentItemIndex);
                    Intent intent = new Intent(this, JobDetailsActivity.class);
                    intent.putExtra("job", (Parcelable) j);
                    startActivityForResult(intent, RESULT_EDIT_JOB);
                }

            }

        }else if(id == R.id.action_delete_job){
            int currentItemIndex = mViewPager.getCurrentItem();

            //Fragment currentFrag = currentItemIndex >= 0
            //        ? mSectionsPagerAdapter.getItem(currentItemIndex)
            //        : null;

            //if(currentFrag != null && (currentFrag instanceof AbstractJobPage)) {
            if(currentItemIndex >= 0){

                AbstractJobPage frag = (AbstractJobPage) mSectionsPagerAdapter.getCurrentFragment();
                if(frag.getAssignedCount() > 0) {

                    new AlertDialog.Builder(this)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle("Não é possível realizar a operação")
                            .setMessage("Não é possível eliminar o separador enquanto houver operações seleccionadas.")
                            .setPositiveButton("OK", null)
                            .show();
                }else {
                    new AlertDialog.Builder(this)
                            .setIcon(android.R.drawable.ic_delete)
                            .setTitle("Eliminar separador?")
                            .setMessage("Confirma a eliminação deste separador?")
                            .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //AbstractJobPage p = (AbstractJobPage) currentFrag;
                                    //Job j = p.getJob();
                                    Job j = mSectionsPagerAdapter.getDataItem(currentItemIndex);
                                    j.setVisible(false);
                                    Job.JobDAO jDAO = mDataBase.getJobDao();
                                    jDAO.updateAll(j);
                                    mSectionsPagerAdapter.removeJobPage(j);

                                }

                            })
                            .setNegativeButton("Não", null)
                            .show();
                }
            }

        }else if(id == R.id.action_save_settings){
            saveSettings();
            Snackbar.make(mViewPager, "Pedido enviado...", Snackbar.LENGTH_LONG)
                    .setAction(android.R.string.ok, null).show();
        }else if(id == R.id.action_restore_settings){


            new AlertDialog.Builder(MainActivity.this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(android.R.string.dialog_alert_title)
                    .setMessage("Esta acção irá repor os seus separadores para o último estado guardado...")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            restoreSettings();
                            Snackbar.make(mViewPager, "Pedido enviado...", Snackbar.LENGTH_LONG)
                                    .setAction(android.R.string.ok, null).show();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        boolean saveSettings = false;

        if (resultCode == RESULT_OK) {
            if (requestCode == RESULT_EDIT_JOB) {
                Job j = data.getParcelableExtra("job");
                j.setAssignmentCode(String.format("%s_%s_%d", j.getWcr(), j.getWst(), j.getIndex()));
                Job.JobDAO jDAO = mDataBase.getJobDao();
                jDAO.updateAll(j);
                mSectionsPagerAdapter.refreshJobPage(j);
                saveSettings = true;
            }else if (requestCode == RESULT_CREATE_JOB) {
                Job j = data.getParcelableExtra("job");
                Calendar c = Calendar.getInstance();
                SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
                //j.setName(df.format(c.getTime()));
                j.setAssignmentCode(String.format("%s_%s_%d", j.getWcr(), j.getWst(), j.getIndex()));
                Job.JobDAO jDAO = mDataBase.getJobDao();
                jDAO.insertAll(j);
                mSectionsPagerAdapter.addJobPage(j);
                saveSettings = true;
            }
        }

        if(saveSettings){
            saveSettings();
        }

    }


    private void saveSettings(){
        String android_id = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);

        Message msg = Message.obtain(null, SyncService.MSG_SAVE_SETTINGS);
        msg.getData().putString(SyncService.ARG_KEY, android_id);
        msg.replyTo = mMessenger;
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    private void restoreSettings(){
        String android_id = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);

        Message msg = Message.obtain(null, SyncService.MSG_RESTORE_SETTINGS);
        msg.getData().putString(SyncService.ARG_KEY, android_id);
        msg.replyTo = mMessenger;
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onStart() {
        //Log.d(TAG, "started "+mJob.getName());
        super.onStart();
        // Bind to LocalService
        doBindService();
    }

    @Override
    public void onStop() {
        //Log.d(TAG, "stoped "+mJob.getName());
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
            super.handleMessage(msg);
            this.obtainMessage();
            Bundle data = msg.getData();
            switch (msg.what) {
                case SyncService.MSG_RESTORE_SETTINGS:
                    //service has finished restoring jobs

                    boolean result = data.getBoolean(SyncService.ARG_RESULT);
                    if(result) {
                        new AlertDialog.Builder(MainActivity.this)
                                .setIcon(android.R.drawable.ic_dialog_info)
                                .setTitle(android.R.string.ok)
                                .setMessage("As definições previamente submetidas foram repostas.")
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        recreate();
                                    }
                                })
                                .show();
                    }else{
                        new AlertDialog.Builder(MainActivity.this)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setTitle(android.R.string.dialog_alert_title)
                                .setMessage("Não foi possível carregar definições remotas...")
                                .setPositiveButton(android.R.string.ok,null)
                                .show();
                    }
                    break;

                default:
                    super.handleMessage(msg);
            }


        }

    }






    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        private final List<Job> mJobList;
        private AbstractJobPage mCurrentPrimaryItem = null;
        public AbstractJobPage getCurrentFragment() {
            return mCurrentPrimaryItem;
        }


        //private final List<AbstractJobPage> mFragmentList = new ArrayList<>();
        //private final List<AbstractJobPage> mInvalidatedFragmentList = new ArrayList<>();

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            mJobList = new ArrayList<>();
            mCurrentPrimaryItem = null;
        }

        @Override
        public Fragment getItem(int position) {
            return JobPageFactory.getInstance().newPage(mJobList.get(position));

            //return mFragmentList.size() > 0 ?  mFragmentList.get(position) : null;
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            //return PlaceholderFragment.newPage(position + 1);
            //Job test = new Job();
            //return AbstractJobPage.newPage(test);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            //return 3;
            //return mFragmentList.size();
            return mJobList.size();
        }

        public Job getDataItem(int position){
            return mJobList.get(position);
        }

        public void addJobPage(Job j) {
            //mFragmentList.add(fragment);
            mJobList.add(j);
            notifyDataSetChanged();
        }
        public void removeJobPage(Job j) {
            //mFragmentList.remove(fragment);
            mJobList.remove(j);
            notifyDataSetChanged();
        }
        public void refreshJobPage(Job j){
            int i = mJobList.indexOf(j);
            if(i >= 0){
                Job ex = mJobList.get(i);
                j.copyTo(ex);
                notifyDataSetChanged();
            }
            /*
            AbstractJobPage f = null;
            for(AbstractJobPage ex : mFragmentList){
                if(ex.getJob().equals(j)){
                    f = ex;
                    break;
                }
            }
            if(f != null){
                Job ex = f.getJob();
                j.copyTo(ex);
                f.reload();
                //mInvalidatedFragmentList.add(f);
            }
            notifyDataSetChanged();
            */
        }


        @Override
        public CharSequence getPageTitle(int position) {
            //return mFragmentList.get(position).getTitle();
            return mJobList.get(position).getName();
        }
        @Override
        public int getItemPosition(Object object) {

            //return mJobList.indexOf(object);

            return POSITION_NONE;

            /*
            Fragment oFragment=(Fragment)object;

            if(!mFragmentList.contains(oFragment))
                return POSITION_NONE;
            else {

                return POSITION_UNCHANGED;
            }
            */
        }


        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);

            AbstractJobPage n = (AbstractJobPage) object;
            if(!Objects.equals(n, mCurrentPrimaryItem)) {
                if (mCurrentPrimaryItem != null) {
                    mCurrentPrimaryItem.setSelected(false);
                }

                mCurrentPrimaryItem = n;

                if (mCurrentPrimaryItem != null) {
                    mCurrentPrimaryItem.setSelected(true);
                }
            }

        }




    }
}
