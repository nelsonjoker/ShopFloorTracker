package pt.sotubo.shopfloortracker.model.sync;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import pt.sotubo.shopfloortracker.BuildConfig;
import pt.sotubo.shopfloortracker.model.AlertMessageContent;
import pt.sotubo.shopfloortracker.model.AlertMessageType;
import pt.sotubo.shopfloortracker.model.AppDatabase;
import pt.sotubo.shopfloortracker.model.Article;
import pt.sotubo.shopfloortracker.model.Job;
import pt.sotubo.shopfloortracker.model.Operation;
import pt.sotubo.shopfloortracker.model.Plan;
import pt.sotubo.shopfloortracker.model.WorkItem;
import pt.sotubo.shopfloortracker.model.WorkMaterial;
import pt.sotubo.shopfloortracker.model.Workcenter;
import pt.sotubo.shopfloortracker.model.Workorder;
import pt.sotubo.shopfloortracker.model.Workstation;

/**
 * Created by Nelson on 05/12/2017.
 */
/*
    TODO: fallback mechanism to update always
    provided data is unsorted (rethinkdb limitation) so we may receive a big update value
    before all previous are processed.
    so we should periodically sync them all
 */

public class SyncService extends Service {
    private static final String TAG = "SyncService";

    private final int NUMBER_OF_CORES = Math.max(1, Runtime.getRuntime().availableProcessors());
    private final String SERVER_URL = "http://sig.sotubo.pt:8889";
    //private final String SERVER_URL = BuildConfig.DEBUG ? "http://192.168.0.77:8888" : "http://sig.sotubo.pt:8889";


    /** Keeps track of all current registered clients. */
    ArrayList<Messenger> mClients = new ArrayList<>();
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    /**
     * Command to the service to register a client, receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client where callbacks should be sent.
     */
    public static final int MSG_REGISTER_CLIENT = 1;

    /**
     * Command to the service to unregister a client, ot stop receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client as previously given with MSG_REGISTER_CLIENT.
     */
    public static final int MSG_UNREGISTER_CLIENT = 2;

    /**
     * perform a full resync on plan database
     */
    public static final int MSG_RESYNC_PLAN = 3;
    /**
     * update a plan
     */
    public static final int MSG_UPDATE_PLAN = 4;
    /**
     * Post tracking data
     */
    public static final int  MSG_COMMIT_TRACKING = 5;
    /**
     * Send an alert message
     */
    public static final int  MSG_SEND_ALERT = 6;


    /**
     * save current settings to server
     */
    public static final int MSG_SAVE_SETTINGS = 7;
    public static final int MSG_RESTORE_SETTINGS = 8;

    public static final int MSG_RELOAD_ARTICLE = 9;

    public static final String ARG_OBJ = "obj";
    public static final String ARG_RESULT = "result";
    public static final String ARG_MESSAGE = "message";
    public static final String ARG_KEY = "key";



    private Socket mSocket;
    //private ThreadPoolExecutor mThreadPool;
    private List<EmitterListener> mListeners;
    private Object mWorkerWaitHandle = new Object();
    private boolean mWorkerRunning;
    private Thread mBackgroundWorker;
    private long mLastPlanUpdate;

    public SyncService() {
        super();
        mWorkerRunning = false;
        mBackgroundWorker = null;
        mLastPlanUpdate = 0;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mListeners = new LinkedList<>();
        /*
        mThreadPool = new ThreadPoolExecutor(
                10,
                Math.max(10,NUMBER_OF_CORES * 1),
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());
        */
        try {

            if(mSocket == null || !mSocket.connected()) {

                EmitterListener workcenterListener =  new EmitterListener() {

                    @Override
                    public void sync(List<JSONObject> objs) {
                        try {
                            syncWorkcenters(objs);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                };
                mListeners.add(workcenterListener);

                EmitterListener workstationListener =  new EmitterListener() {

                    @Override
                    public void sync(List<JSONObject> objs) {
                        try {
                            syncWorkstation(objs);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                };
                mListeners.add(workstationListener);

                EmitterListener articleListener =  new EmitterListener() {

                    @Override
                    public void sync(List<JSONObject> objs) {
                        try {
                            syncArticles(objs);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                };
                mListeners.add(articleListener);

                EmitterListener planListener =  new EmitterListener() {

                    @Override
                    public void sync(List<JSONObject> objs) {
                        try {
                            syncPlans(objs);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                };
                mListeners.add(planListener);


                EmitterListener woListener =  new EmitterListener() {

                    @Override
                    public void sync(List<JSONObject> objs) {
                        try {
                            syncWorkorders(objs);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                };
                mListeners.add(woListener);




                IO.Options options = new IO.Options();
                //By default, a single connection is used when connecting to different namespaces (to minimize resources):
                //That behaviour can be disabled with the forceNew option:
                //options.forceNew = true;
                //options.reconnection = true;
                //options.reconnectionAttempts = 999999;
                options.reconnectionDelay = 1000;
                options.reconnection = true;
                options.reconnectionAttempts = 10;

                if(mSocket != null){
                    mSocket.disconnect();
                }


                mSocket = IO.socket(SERVER_URL, options);

                mSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

                    @Override
                    public void call(Object... args) {

                        AppDatabase db = AppDatabase.getInstance(SyncService.this);


                        mSocket.emit("subscribe:workcenter");
                        mSocket.emit("subscribe:workstation");

                        try {
                            Article.ArticleDAO dao = db.getArticleDao();
                            Long u = dao.getMaxUpdate();
                            if(u == null || u < System.currentTimeMillis() - 10*24*3600*1000L) {
                                dao.truncate();
                                u = -1L;
                            }
                            JSONObject arg = new JSONObject();
                            arg.put("update", u != null && u > 0 ? u - 1 : -1 );
                            mSocket.emit("subscribe:itmmaster", arg);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        try {
                            Plan.PlanDAO dao = db.getPlanDao();
                            //List<Plan> test = dao.getAllDebug();
                            Long u = dao.getMaxUpdate();
                            if(u == null || u < System.currentTimeMillis() - 10*24*3600*1000L) {
                                dao.truncate();
                                u = -1L;
                            }

                            JSONObject arg = new JSONObject();
                            //edge case :
                            // set u-1 so that we can listen for changes on the last item
                            // without changing
                            // update timestamp uppon starting
                            arg.put("update", u != null && u > 0 ? u - 1 : -1 );
                            mSocket.emit("subscribe:plan", arg);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                        try {
                            Workorder.WorkorderDAO dao = db.getWorkorderDao();
                            Long u = dao.getMaxUpdate();
                            if(u == null || u < System.currentTimeMillis() - 10*24*3600*1000L) {
                                dao.truncate();
                                u = -1L;
                            }
                            JSONObject arg = new JSONObject();
                            arg.put("update", u != null && u > 0 ? u - 1 : -1 );
                            mSocket.emit("subscribe:workorder", arg);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }

                })

                        .on("alert_message_type", new Emitter.Listener() {
                            @Override
                            public void call(Object... args) {
                                if(args != null && args.length > 0 && args[0] instanceof  JSONArray) {
                                    JSONArray arr = (JSONArray)args[0];
                                    List<JSONObject> objs = new ArrayList<>(arr.length());
                                    try {
                                        for (int i = 0; i < arr.length(); i++) {
                                            objs.add(arr.getJSONObject(i));
                                        }
                                        syncAlertMessageType(objs);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        })

                        .on("workcenter:create", workcenterListener)
                        .on("workcenter:update", workcenterListener)
                        .on("workcenter:delete", workcenterListener)

                        .on("workstation:create", workstationListener)
                        .on("workstation:update", workstationListener)
                        .on("workstation:delete", workstationListener)

                        .on("itmmaster:create", articleListener)
                        .on("itmmaster:update", articleListener)
                        .on("itmmaster:delete", articleListener)

                        .on("plan:create", planListener)
                        .on("plan:update", planListener)
                        .on("plan:delete", planListener)



                        .on("workorder:create", woListener)
                        .on("workorder:update", woListener)
                        .on("workorder:delete", woListener)

                        .on("plan:reload", new Emitter.Listener() {
                            @Override
                            public void call(Object... args) {
                                Plan.PlanDAO dao = AppDatabase.getInstance(SyncService.this).getPlanDao();
                                dao.deleteObsolete();
                            }
                        })



                        .on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {

                            @Override
                            public void call(Object... args) {
                                Log.e(TAG, "Socket connect error, terminate connection");
                                if(args != null && args.length > 0 && args[0] instanceof Exception){
                                    ((Exception)args[0]).printStackTrace();
                                }
                                if (mSocket.connected()) {
                                    mSocket.disconnect();
                                }
                            }

                        })

                        .on(Socket.EVENT_ERROR, new Emitter.Listener() {

                            @Override
                            public void call(Object... args) {
                                Log.e(TAG, "Socket io error, reconnecting");
                                if(args != null && args.length > 0 && args[0] instanceof Exception){
                                    ((Exception)args[0]).printStackTrace();
                                }
                                if (mSocket.connected()) {
                                    mSocket.disconnect();
                                }
                            }

                        }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

                            @Override
                            public void call(Object... args) {
                                Log.e(TAG, "Socket io connection error , reconnecting");
                                if(args != null && args.length > 0 && args[0] instanceof Exception){
                                    ((Exception)args[0]).printStackTrace();
                                }

                                if (mSocket.connected()) {
                                    mSocket.disconnect();
                                }
                            }

                        })


                ;


                //Let the background worker do it's magic
                //mSocket.connect();
            }



        }  catch (URISyntaxException e) {
            e.printStackTrace();
        }

        mBackgroundWorker = new Thread(new Runnable() {
            @Override
            public void run() {
                backgroundWorker();
            }
        });
        mBackgroundWorker.setName(TAG+" : backgroundWorker");
        mWorkerRunning = true;
        mBackgroundWorker.start();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        synchronized (mWorkerWaitHandle) {
            mWorkerRunning = false;
            mWorkerWaitHandle.notifyAll();
        }
        try {
            mBackgroundWorker.join(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for(EmitterListener l : mListeners)
            l.stop();

        if(mSocket != null && mSocket.connected()) {
            mSocket.disconnect();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    private static int theRunningEmitterThreadCounter = 0;
    private abstract class EmitterListener implements Emitter.Listener, Runnable{

        private class NullJsonObject extends JSONObject{

        }

        private final int BUFFER_SIZE = 50;
        private final int BATCH_SIZE = 100;

        private BlockingQueue<JSONObject> mQueue;
        private boolean mRunning;
        private  Thread mWorker;

        public EmitterListener(){
            mQueue = new LinkedBlockingQueue<JSONObject>(BUFFER_SIZE);
            mRunning = false;
            mWorker = null;
            start();
        }

        private void start(){
            assert mRunning = false;
            mRunning = true;
            mWorker = new Thread(this);
            mWorker.setName("EmitterListener thread - " + theRunningEmitterThreadCounter);
            mWorker.setPriority(Thread.MIN_PRIORITY);
            mWorker.start();
            theRunningEmitterThreadCounter++;
        }

        private void stop(){
            mRunning = false;
            NullJsonObject n = new NullJsonObject();
            try {
                mQueue.put(n);
                mWorker.join(10*60000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mQueue.clear();

        }

        @Override
        public void run(){

            List<JSONObject> objs = new ArrayList<>(BUFFER_SIZE+1);
            while(mRunning) {
                objs.clear();


                try {

                    JSONObject o;
                    while((o = mQueue.poll(500, TimeUnit.MILLISECONDS)) != null && objs.size() < BATCH_SIZE){
                        if(o instanceof  NullJsonObject)
                            break;
                        if (o != null)
                            objs.add(o);
                    }

                    /*
                    while (!mQueue.isEmpty() || objs.size() == 0) {
                        JSONObject o = null;
                        try {
                            o = mQueue.take();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if(o instanceof  NullJsonObject)
                            break;
                        if (o != null)
                            objs.add(o);
                    }
                    */

                    if (objs.size() > 0)
                        sync(objs);

                }catch (Exception e){
                    try {
                        e.printStackTrace();
                        Thread.sleep(30000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }

            }

        }


        @Override
        public void call(Object... args) {
            if(!mRunning)
                return;
            int i = 0;
            for( ; i < args.length && args[i] instanceof JSONObject; i++) {
                JSONObject obj = (JSONObject) args[i];
                try {
                    mQueue.put(obj);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if(i < args.length && args[i] instanceof Ack) {
                Ack ack = (Ack) args[i];
                if (ack != null) {
                    JSONObject a = new JSONObject();
                    try {
                        a.put("window", (BUFFER_SIZE - mQueue.size()));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    ack.call(a);
                }
            }

        }

        public abstract void sync(List<JSONObject> objs);


    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    private void sleepWait(long milis){
        synchronized (mWorkerWaitHandle){
            try {
                mWorkerWaitHandle.wait(milis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void backgroundWorker(){

        int state = 0;
        int backoff = 5000;
        long lastFullReload = System.currentTimeMillis();
        while(mWorkerRunning){

            try {
                switch (state) {
                    case 0:
                        if (!mSocket.connected()) {
                            Log.d(TAG, "No connection to server, retrying connection...");
                            mSocket.disconnect();
                            mSocket.connect();
                            backoff += 1000;
                            state = 1;
                        } else {
                            backoff = 5000;
                            state = 10;
                        }
                        break;
                    case 1:
                        sleepWait(Math.min(30000, backoff));
                        state = 0;
                        break;
                    case 10:
                        //cannot reduce wait because we delete it on server callbacks
                        //and sendTrackingFiles() returns immediately so we would try sending
                        //it twice
                        sendTrackingFiles();
                        sleepWait(30000);
                        Random r = new Random(System.currentTimeMillis());
                        if(System.currentTimeMillis() - mLastPlanUpdate > (30 + 5*r.nextDouble() )*60*1000 || System.currentTimeMillis() - lastFullReload > 3*3600*1000)
                            state = 20;   //TODO: unresolved concurrency issues
                            //state = 0;
                        else
                            state = 0;
                        break;
                    case 20:
                        requestFullPlanUpdate();
                        mLastPlanUpdate = System.currentTimeMillis();   //ensure reset
                        lastFullReload = System.currentTimeMillis();
                        state = 0;
                        break;
                }
            }catch(Exception e){
                Log.e(TAG, "background worker ran into an error :"+e.getMessage());
                e.printStackTrace();
            }

        }
        Log.d(TAG, "background worker checking out");


    }


    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Bundle data = null;
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                case MSG_RESYNC_PLAN: {
                        ResyncPlanTask task = new ResyncPlanTask();
                        task.execute();
                    }
                    break;
                case MSG_UPDATE_PLAN:
                    data = msg.getData();
                    if(data != null && data.containsKey(ARG_OBJ)){
                        Plan p = data.getParcelable(ARG_OBJ);
                        UpdatePlanTask task = new UpdatePlanTask(msg.replyTo);
                        task.execute(p);
                    }
                    break;
                case MSG_COMMIT_TRACKING:
                    data = msg.getData();
                    final Messenger listener = msg.replyTo;
                    if(data != null && data.containsKey("duration") && data.containsKey("operations")){

                        CommitTrackingTask task = new CommitTrackingTask(listener);
                        task.execute(data);

                    }else{
                        Message resp = Message.obtain(null, SyncService.MSG_COMMIT_TRACKING);
                        resp.getData().putBoolean(SyncService.ARG_RESULT, false);
                        resp.getData().putString(SyncService.ARG_MESSAGE, "Pedido inválido");
                        try {
                            listener.send(resp);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }


                    break;
                case MSG_SEND_ALERT:
                    data = msg.getData();
                    if(data != null && data.containsKey(ARG_OBJ)) {
                        AlertMessageContent content = data.getParcelable(ARG_OBJ);
                        JSONObject alert = new JSONObject();
                        try {
                            alert.put("type", content.getAlertType().getId());
                            alert.put("comment", content.getComment());
                            Bundle payload = content.getPayload();
                            for(String k : payload.keySet()){
                                alert.put(k, payload.get(k));
                            }

                            mSocket.emit("insert:alert_message", alert);


                        } catch (JSONException e) {
                            Log.e(TAG, e.getMessage());
                            e.printStackTrace();
                        }


                    }
                    break;
                case MSG_SAVE_SETTINGS:
                    data = msg.getData();
                    if(data != null && data.containsKey(ARG_KEY)) {
                        saveSettings(data.getString(ARG_KEY));
                    }
                    break;
                case MSG_RESTORE_SETTINGS:
                    data = msg.getData();
                    if(data != null && data.containsKey(ARG_KEY)) {
                        restoreSettings(data.getString(ARG_KEY), msg.replyTo);
                    }
                    break;
                case MSG_RELOAD_ARTICLE:
                    data = msg.getData();
                    if(data != null && data.containsKey(ARG_KEY)) {
                        String itmref = data.getString(ARG_KEY);
                        JSONObject jo = new JSONObject();
                        try {
                            jo.put("itmref", itmref);
                            mSocket.emit("reload:article", jo);

                        } catch (JSONException e) {
                            Log.e(TAG, e.getMessage());
                            e.printStackTrace();
                        }


                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private class UpdatePlanTask extends AsyncTask<Plan, Void, Message> {

        private Messenger mListener;

        public UpdatePlanTask(Messenger listener){
            mListener = listener;
        }

        @Override
        protected Message doInBackground(Plan... plans) {
            Plan p = plans[0];
            if(p != null){
                Plan.PlanDAO dao = AppDatabase.getInstance(SyncService.this).getPlanDao();
                p.setDirty(true);
                dao.updateAll(p);
                JSONObject obj = new JSONObject();
                try {
                    obj.put("id", p.getId());
                    obj.put("assigned", p.isAssigned());
                    obj.put("assignment_code", p.getAssignmentCode());
                    obj.put("assigned_qty", p.getAssignedQty());
                    obj.put("start", p.getStart());
                    obj.put("end", p.getEnd());
                    /////
                    obj.put("completed", p.isCompleted());
                    obj.put("cplqty", p.getCplqty());


                    //obj.put("update", System.currentTimeMillis());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mSocket.emit("update:plan", obj);

            }
            Message resp = Message.obtain(null, SyncService.MSG_UPDATE_PLAN);
            Bundle dt = resp.getData();
            dt.putBoolean(SyncService.ARG_RESULT, true); //true-ishh..
            ArrayList<Plan> objs = new ArrayList<>(1);
            objs.add(p);
            dt.putParcelableArrayList(SyncService.ARG_OBJ, objs);
            return resp;
        }
        @Override
        protected void onPostExecute(Message res) {
            super.onPostExecute(res);
            if(mListener != null) {
                try {
                    mListener.send(res);
                } catch (RemoteException re) {
                    re.printStackTrace();
                }
            }

        }


    }

    private class ResyncPlanTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            Plan.PlanDAO dao = AppDatabase.getInstance(SyncService.this).getPlanDao();
            dao.resetUpdateCounters();
            //dao.truncate();
            mLastPlanUpdate = System.currentTimeMillis(); //try to avoid duplicate sync
            requestFullPlanUpdate();

            return null;
        }



    }

    /**
     * perform full update on local database
     * @deprecated
     */
    private class ReloadPlanTask extends AsyncTask<JSONArray, Void, Void> {

        @Override
        protected Void doInBackground(JSONArray... plans) {

            JSONArray arr = plans[0];
            long start = System.currentTimeMillis();
            try {
                List<String> jids = new ArrayList<>(arr.length());
                List<JSONObject> objs = new LinkedList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject p = arr.getJSONObject(i);
                    objs.add(p);
                    jids.add(p.getString("id"));
                    p = null;
                }
                arr = null;
                plans[0] = null;
                System.gc();

                Plan.PlanDAO dao = AppDatabase.getInstance(SyncService.this).getPlanDao();
                List<String> ids = dao.getAllIds();
                ids.removeAll(jids);
                while(ids.size() > 0) {
                    List<String> i = ids.subList(0, Math.min(ids.size(), 50));
                    dao.deleteAll(i.toArray(new String[i.size()]));
                    i.clear();

                }
                ids.clear();
                jids.clear();

                ids = null;
                jids = null;
                System.gc();

                //for memory managment we batch it 500 at a time
                while(objs.size() > 0){
                    List<JSONObject> l = objs.subList(0, Math.min(objs.size(), 500));
                    syncPlans(l);
                    l.clear();
                    System.gc();
                    Log.d(TAG, "full plan reload "+objs.size()+" remaining");
                }


                long elapsed = System.currentTimeMillis() - start;
                Log.d(TAG, "full plan reload in "+elapsed+" ms");



            } catch (JSONException e) {
                e.printStackTrace();
            }



            return null;
        }



    }

    private class CommitTrackingTask extends AsyncTask<Bundle, Integer, Message> {

        private Messenger mListener;

        public CommitTrackingTask(Messenger listener){
            mListener = listener;
        }


        @Override
        protected Message doInBackground(Bundle... bundles) {

            Message res = null;

            Bundle data = bundles[0];



            String WCR = data.getString("wcr");
            String WST = data.getString("wst");
            String operator = data.getString("operator");
            long duration = data.getLong("duration");
            ArrayList<Plan> allSelectedPlans = data.getParcelableArrayList("plans");
            ArrayList<WorkMaterial> allConsumedMaterials = data.getParcelableArrayList("materials");
            ArrayList<WorkItem> allProducedItems = data.getParcelableArrayList("items");
            ArrayList<Operation> allTrackedOperations = data.getParcelableArrayList("operations");


            List<String> mfgnums = new ArrayList<>(allTrackedOperations.size());
            double sumOfOpetim = 0;
            for(Operation o : allTrackedOperations){
                sumOfOpetim += o.getExtopetim();
                if(!mfgnums.contains(o.getMfgnum()))
                    mfgnums.add(o.getMfgnum());
            }

            Workorder.WorkorderDAO woDAO = AppDatabase.getInstance(getApplicationContext()).getWorkorderDao();
            WorkItem.WorkItemDAO itemDAO = AppDatabase.getInstance(getApplicationContext()).getWorkItemDao();
            WorkMaterial.WorkMaterialDAO materialDAO = AppDatabase.getInstance(getApplicationContext()).getWorkMaterialDao();
            Operation.OperationDAO operationDAO = AppDatabase.getInstance(getApplicationContext()).getOperationDao();

            /**
             * when tracking we need to account for X3 direct material tracking
             * let:
             * OP current operation completed qtd
             * ITM current item tracked qtd
             * MAT current material tracked qtd
             *
             * dITM current item tracking
             * dMAT current material tracking
             *
             * tOP qty to use for operation tracking
             * tITM  qty to use for item tracking
             * tMAT qty to use for material tracking
             *
             * tOP = dITM
             * tITM = MAX(0 , dITM + OP - ITM)
             * r = tITM / dITM
             * tMAT = dMAT * r
             *
             *
             */
            try {
                for(Plan plan : allSelectedPlans) {

                    String mfgnum = plan.getMfgnum();
                    ArrayList<WorkMaterial> consumedMaterials = new ArrayList<>();
                    ArrayList<WorkItem> producedItems = new ArrayList<>();
                    ArrayList<Operation> trackedOperations = new ArrayList<>();

                    for(WorkMaterial m : allConsumedMaterials){
                        if(mfgnum.equals(m.getMfgnum()))
                            consumedMaterials.add(m);
                    }
                    for(WorkItem i : allProducedItems){
                        if(mfgnum.equals(i.getMfgnum()))
                            producedItems.add(i);
                    }
                    for(Operation o : allTrackedOperations){
                        if(mfgnum.equals(o.getMfgnum()))
                            trackedOperations.add(o);
                    }



                    Workorder wo = woDAO.get(mfgnum);
                    List<WorkItem> items = itemDAO.getAll(mfgnum);
                    List<WorkMaterial> materials = materialDAO.getAll(mfgnum);
                    List<Operation> operations = operationDAO.getAll(mfgnum);

                    //filter operations/items/materials not referenced
                    for(int i = operations.size() - 1 ; i >= 0; i--){
                        boolean found = false;
                        Operation t = operations.get(i);
                        for(Operation o : trackedOperations){
                            if(o.getMfgnum().equals(t.getMfgnum()) && (o.getOpenum() == t.getOpenum())){
                                found = true;
                                break;
                            }
                        }
                        if(!found)
                            operations.remove(i);
                    }
                    for(int i = items.size() - 1 ; i >= 0; i--){
                        boolean found = false;
                        WorkItem t = items.get(i);
                        for(WorkItem it : producedItems){
                            if(it.getItmref().equals(t.getItmref()) ){
                                found = true;
                                break;
                            }
                        }
                        if(!found)
                            items.remove(i);
                    }

                    for(int i = materials.size() - 1 ; i >= 0; i--){
                        boolean found = false;
                        WorkMaterial t = materials.get(i);
                        for(WorkMaterial mat : consumedMaterials){
                            if(mat.getItmref().equals(t.getItmref()) ){
                                found = true;
                                break;
                            }
                        }
                        if(!found)
                            materials.remove(i);
                    }


                    JSONObject tracking = new JSONObject();

                    tracking.put("type", "tracking");
                    tracking.put("status", "new");
                    tracking.put("mfgtrknum", "");
                    tracking.put("wcr", WCR);
                    tracking.put("wst", WST);
                    tracking.put("operator", operator);
                    tracking.put("mfgnum", mfgnum);
                    tracking.put("mfgfcy", wo.getMfgfcy());
                    tracking.put("time", System.currentTimeMillis());

                    int OPETRKFLG = 1;
                    int ITMTRKFLG = 1;
                    int MATTRKFLG = 1;

                    double dITM = 0;
                    double rejcplqty = 0;
                    for(WorkItem it : items){
                        for(WorkItem pr : producedItems){
                            if(it.getItmref().equals(pr.getItmref())){
                                dITM = Math.max(dITM, pr.getCplqty());
                            }
                        }
                    }

                    JSONArray joperations = new JSONArray();
                    Operation terminalOp = null; //tracking items only if last operation
                    assert(operations.size() == 1);
                    for(Operation op : operations){
                        //TODO: assuming one item per wo


                        double secs = duration * (op.getExtopetim()/sumOfOpetim)/1000;

                        double CPLOPETIM = (double)(op.getTimuomcod() == 1 ? secs / 3600 : secs / 60);

                        JSONObject otrk = new JSONObject();
                        otrk.put("openum", (long)op.getOpenum());
                        otrk.put("cplwst", WST);
                        otrk.put("cplqty", dITM);
                        otrk.put("extqty", op.getExtqty());
                        otrk.put("rejcplqty", rejcplqty);
                        otrk.put("opeuom", op.getOpeuom());
                        otrk.put("timuomcod", (long)op.getTimuomcod());
                        otrk.put("cplsettim", (double)0);
                        otrk.put("cplopetim", CPLOPETIM);
                        otrk.put("cplunttim", dITM > 0 ? CPLOPETIM / dITM : CPLOPETIM);
                        otrk.put("duration", (double)secs);
                        otrk.put("mfgnum", mfgnum);
                        otrk.put("extunttim", op.getExtunttim());
                        otrk.put("extsettim", op.getExtsettim());
                        otrk.put("extwst", op.getExtwst());

                        //adjust object values so they can be used later on
                        op.setCplqty(op.getCplqty() + dITM);
                        op.setCplopetim(op.getCplopetim() + CPLOPETIM);

                        if (op.getCplqty() >= op.getExtqty())
                        {
                            op.setMfotrkflg(5);
                            otrk.put("cleflg", 2);
                        }
                        else
                        {
                            op.setMfotrkflg(4);
                            otrk.put("cleflg", 1);
                        }

                        joperations.put(otrk);

                        OPETRKFLG = 2;



                        if(op.getNexopenum() <= 0)
                            terminalOp = op;


                        double q = dITM;
                        assert(mfgnum.equals(plan.getMfgnum()) && op.getOpenum() == plan.getOpenum());
                        //for(Plan p : selectedPlans){
                        //if(mfgnum.equals(p.getMfgnum()) && op.getOpenum() == p.getOpenum()){
                        double rem = plan.getExtqty() - plan.getCplqty();
                        if(rem <= 0)
                            continue;
                        rem = q > rem ? rem : q;
                        plan.setCplqty(plan.getCplqty() + rem);
                        plan.setAssignedQty(Math.max(0, plan.getAssignedQty() - rem));
                        q -= rem;
                        if(plan.getCplqty() >= plan.getExtqty()) {
                            plan.setAssignedQty(0);
                            plan.setCompleted(true);
                        }

                        //p.setDirty(true);

                        //}

                        //}


                    }
                    operationDAO.updateAll(operations.toArray(new Operation[operations.size()]));
                    tracking.put("operations", joperations);

                    //we need to trim down the produced qty so we are compatible
                    //with manual tracking so this registers the ratio between what
                    //we declared produced and what we can register as delta
                    //if this is not a terminal op there is not much we can do
                    //to estimate this so by default we set it to 1
                    double productionTrimRatio = 1;

                    JSONArray jitems = new JSONArray();
                    if(terminalOp != null) {
                        for (WorkItem it : items) {
                            WorkItem prod = null;
                            for (WorkItem pr : producedItems) {
                                if (it.getItmref().equals(pr.getItmref())) {
                                    prod = pr;
                                    break;
                                }
                            }
                            assert (prod != null);

                            //mfgitm may have been tracked already
                            //double qty = terminalOp.getCplqty() - it.getCplqty();
                            //double qty = prod.getCplqty() - it.getCplqty();
                            //double tITM = dITM + terminalOp.getCplqty() - dITM - it.getCplqty();
                            double tITM = terminalOp.getCplqty() - it.getCplqty();
                            if(tITM <= 0) {
                                productionTrimRatio = 0;
                                continue;
                            }
                            productionTrimRatio = tITM / dITM;

                            JSONObject itrk = new JSONObject();
                            itrk.put("extqty", it.getExtqty());
                            itrk.put("uomextqty", it.getUomextqty());
                            itrk.put("cplqty", tITM);
                            itrk.put("uomcplqty", tITM);
                            itrk.put("itmref", it.getItmref());
                            itrk.put("mfgnum", mfgnum);
                            itrk.put("uom", it.getStu());
                            itrk.put("mfglin", it.getMfglin());

                            it.setCplqty(it.getCplqty() + tITM);
                            if (it.getCplqty() >= it.getExtqty()) {
                                it.setMfitrkflg(5);
                                itrk.put("cleflg", 2);
                            } else {
                                it.setMfitrkflg(4);
                                itrk.put("cleflg", 1);
                            }

                            //item.Add("CLEFLG", 2);
                            jitems.put(itrk);
                            ITMTRKFLG = 2;

                            itemDAO.updateAll(it);



                        }
                    }

                    tracking.put("items", jitems);


                    JSONArray jmaterials = new JSONArray();
                    for(WorkMaterial mat : materials) {

                        //double check that this material was consumed on one of the operations
                        Operation consumer = null;
                        for(Operation op : operations){
                            if(op.getOpenum() == mat.getBomope()){
                                consumer = op;
                                break;
                            }
                        }
                        assert(consumer != null);
                        if(consumer == null)
                            continue;


                        WorkMaterial cons = null;
                        for(WorkMaterial c : consumedMaterials){
                            if(mat.getItmref().equals(c.getItmref())){
                                cons = c;
                                break;
                            }
                        }
                        assert(cons != null);

                                    /*
                                    double consRatio = consumer.getCplqty()/consumer.getExtqty();
                                    double useqty = mat.getRetqty() * consRatio;
                                    useqty = useqty - mat.getUseqty();
                                    */
                        double useqty = cons.getUseqty(); // - mat.getUseqty();
                        useqty *= productionTrimRatio;
                        //we can either avoid extra consumption or under consumption
                        //in case this is not a terminal op
                        if(terminalOp == null)
                            useqty = Math.min(useqty, mat.getRetqty());

                        if(useqty <= 0)
                            continue;

                        JSONObject mtrk = new JSONObject();
                        mtrk.put("cplwst", WST);
                        mtrk.put("mfmitmref", mat.getItmref());
                        mtrk.put("mfgnum", mfgnum);
                        mtrk.put("stu", mat.getStu());
                        mtrk.put("useqty", useqty);
                        mtrk.put("retqty", mat.getRetqty());
                        mtrk.put("bomope", mat.getBomope());
                        mtrk.put("bomseq", mat.getBomseq());
                        mtrk.put("mfglin", mat.getMfglin());

                        mat.setUseqty(mat.getUseqty() + useqty);
                        if (mat.getUseqty() >= mat.getRetqty())
                        {
                            mat.setMfmtrkflg(5);
                            mtrk.put("cleflg", 2);
                        }
                        else
                        {
                            mat.setMfmtrkflg(4);
                            mtrk.put("cleflg", 1);
                        }

                        //item.Add("CLEFLG", 2);
                        jmaterials.put(mtrk);
                        MATTRKFLG = 2;

                        materialDAO.updateAll(mat);
                    }

                    tracking.put("components", jmaterials);



                    tracking.put("opetrkflg", OPETRKFLG);
                    tracking.put("itmtrkflg", ITMTRKFLG);
                    tracking.put("mattrkflg", MATTRKFLG);
                    final String uuid = UUID.randomUUID().toString();
                    tracking.put("uuid", uuid);
                    tracking.put("id", uuid);

                    saveTrackingToFile(tracking, uuid + ".json");
                    //actually we keep trying to send, but the on reconnect event
                    //hash will hopefully throw this away
                    Plan.PlanDAO dao = AppDatabase.getInstance(getApplicationContext()).getPlanDao();

                    //optimistic approach
                    dao.updateAll(allSelectedPlans.toArray(new Plan[allSelectedPlans.size()]));



                    //obj.put("update", System.currentTimeMillis());

                    publishProgress(0);

                }

                if (mListener != null) {
                    Message resp = Message.obtain(null, SyncService.MSG_COMMIT_TRACKING);
                    Bundle dt = resp.getData();
                    dt.putBoolean(SyncService.ARG_RESULT, true); //true-ishh..
                    dt.putParcelableArrayList(SyncService.ARG_OBJ, allSelectedPlans);
                    res = resp;

                }

            } catch (Exception e) {
                e.printStackTrace();
                Message resp = Message.obtain(null, SyncService.MSG_COMMIT_TRACKING);
                resp.getData().putBoolean(SyncService.ARG_RESULT, false);
                resp.getData().putString(SyncService.ARG_MESSAGE, e.getMessage());
                publishProgress(0);
                res = resp;
            }
            return res;
        }


        @Override
        protected void onPostExecute(Message res) {
            super.onPostExecute(res);
            try {
                mListener.send(res);
            } catch (RemoteException re) {
                re.printStackTrace();
            }
            synchronized (mWorkerWaitHandle){
                //flush it now
                mWorkerWaitHandle.notifyAll();
            }
        }
    }


    private int broadcastMessage(Message msg){
        int res = 0;
        for(Messenger m : mClients){
            try {
                m.send(msg);
                res ++;
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return res;
    }

    private synchronized void syncAlertMessageType(List<JSONObject> objs) throws JSONException {


        List<AlertMessageType> deleteList = new ArrayList<>();
        List<AlertMessageType> updateList = new ArrayList<>();
        List<AlertMessageType> insertList = new ArrayList<>();
        AlertMessageType.AlertMessageTypeDAO dao = AppDatabase.getInstance(this).getAlertMessageTypeDao();
        List<AlertMessageType> types = dao.getAll();

        for(JSONObject obj : objs) {
            Long id = obj.getLong("id");
            String title = obj.getString("title");
            String description = obj.getString("description");
            String emails = obj.getString("emails");
            boolean deleted = obj.has("deleted") ? obj.getBoolean("deleted") : false;


            AlertMessageType ex = null;
            for(AlertMessageType w : types){
                if(id == w.getId()){
                    ex = w;
                    break;
                }
            }
            if (deleted) {
                if (ex != null) {
                    Log.d(TAG, "deleting alert message type " + id);
                    //dao.deleteAll(ex);
                    deleteList.add(ex);
                }
                continue;
            }



            if(ex == null){
                ex = new AlertMessageType();
                ex.setId(id);
                ex.setTitle(title);
                ex.setDescription(description);
                ex.setEmails(emails);
                insertList.add(ex);
                Log.d(TAG, "inserting alert message type " + id);

            }else {
                types.remove(ex);
                boolean changes = false;
                if(!title.equals(ex.getTitle())) {
                    ex.setTitle(title);
                    changes = true;
                }else if(!description.equals(ex.getDescription())) {
                    ex.setDescription(description);
                    changes = true;
                }else if(!emails.equals(ex.getEmails())) {
                    ex.setEmails(emails);
                    changes = true;
                }

                if(changes) {
                    updateList.add(ex);
                    Log.d(TAG, "updating alert message type " + id);
                }
            }
        }

        dao.deleteAll(deleteList.toArray(new AlertMessageType[deleteList.size()]));
        dao.insertAll(insertList.toArray(new AlertMessageType[insertList.size()]));
        dao.updateAll(updateList.toArray(new AlertMessageType[updateList.size()]));

    }

    private synchronized void syncWorkstation(List<JSONObject> objs) throws JSONException {


        List<Workstation> deleteList = new ArrayList<>();
        List<Workstation> updateList = new ArrayList<>();
        List<Workstation> insertList = new ArrayList<>();
        Workstation.WorkstationDAO dao = AppDatabase.getInstance(this).getWorkstationDao();
        List<Workstation> workstations = dao.getAll();

        for(JSONObject obj : objs) {
            String code = obj.getString("wst");
            String wcr = obj.getString("wcr");
            String description = obj.getString("name");
            boolean deleted = obj.has("deleted") ? obj.getBoolean("deleted") : false;


            Workstation ex = null;
            for(Workstation w : workstations){
                if(code.equals(w.getCode())){
                    ex = w;
                    break;
                }
            }
            if (deleted) {
                if (ex != null) {
                    Log.d(TAG, "deleting workstation " + code);
                    //dao.deleteAll(ex);
                    deleteList.add(ex);
                }
                continue;
            }



            if(ex == null){

                ex = new Workstation();
                ex.setCode(code);
                ex.setWcr(wcr);
                ex.setDescription(description);
                insertList.add(ex);
                Log.d(TAG, "inserting workstation " + code);

            }else {
                workstations.remove(ex);
                boolean changes = false;
                if(!code.equals(ex.getCode())) {
                    ex.setCode(code);
                    changes = true;
                }else if(!wcr.equals(ex.getWcr())) {
                    ex.setWcr(wcr);
                    changes = true;
                }else if(!description.equals(ex.getDescription())) {
                    ex.setDescription(description);
                    changes = true;
                }

                if(changes) {
                    updateList.add(ex);
                    Log.d(TAG, "updating workstation " + code);
                }
            }
        }

        dao.deleteAll(deleteList.toArray(new Workstation[deleteList.size()]));
        dao.insertAll(insertList.toArray(new Workstation[insertList.size()]));
        dao.updateAll(updateList.toArray(new Workstation[updateList.size()]));

    }

    private synchronized void syncWorkcenters(List<JSONObject> objs) throws JSONException {


        List<Workcenter> deleteList = new ArrayList<>();
        List<Workcenter> updateList = new ArrayList<>();
        List<Workcenter> insertList = new ArrayList<>();
        Workcenter.WorkcenterDAO dao = AppDatabase.getInstance(this).getWorkcenterDao();
        List<Workcenter> workcenters = dao.getAll();

        for(JSONObject obj : objs) {
            String code = obj.getString("wcr");
            String name = obj.getString("name");
            String description = obj.getString("name");
            boolean deleted = obj.has("deleted") ? obj.getBoolean("deleted") : false;


            Workcenter ex = null;
            for(Workcenter w : workcenters){
                if(code.equals(w.getCode())){
                    ex = w;
                    break;
                }
            }
            if (deleted) {
                if (ex != null) {
                    Log.d(TAG, "deleting workcenters " + code);
                    //dao.deleteAll(ex);
                    deleteList.add(ex);
                }
                continue;
            }



            if(ex == null){

                ex = new Workcenter();
                ex.setCode(code);
                ex.setName(name);
                ex.setDescription(description);
                insertList.add(ex);
                Log.d(TAG, "inserting workcenter " + code);

            }else {
                workcenters.remove(ex);
                boolean changes = false;
                if(!code.equals(ex.getCode())) {
                    ex.setCode(code);
                    changes = true;
                }else if(!name.equals(ex.getName())) {
                    ex.setName(name);
                    changes = true;
                }else if(!description.equals(ex.getDescription())) {
                    ex.setDescription(description);
                    changes = true;
                }
                if(changes) {
                    updateList.add(ex);
                    Log.d(TAG, "updating workcenter " + code);
                }
            }
        }

        dao.deleteAll(deleteList.toArray(new Workcenter[deleteList.size()]));
        dao.insertAll(insertList.toArray(new Workcenter[insertList.size()]));
        dao.updateAll(updateList.toArray(new Workcenter[updateList.size()]));

    }


    private synchronized void syncArticles(List<JSONObject> objs) throws JSONException {


        List<Article> deleteList = new ArrayList<>();
        List<Article> updateList = new ArrayList<>();
        List<Article> insertList = new ArrayList<>();
        Article.ArticleDAO dao = AppDatabase.getInstance(this).getArticleDao();

        for(JSONObject obj : objs) {
            String itmref = obj.getString("itmref");
            boolean deleted = obj.has("deleted") ? obj.getBoolean("deleted") : false;


            Article ex = dao.get(itmref);

            if (deleted) {
                if (ex != null) {
                    Log.d(TAG, "deleting article " + itmref);
                    //dao.deleteAll(ex);
                    deleteList.add(ex);
                }
                continue;
            }


            String fcy = obj.getString("fcy");
            String itmdes = obj.getString("itmdes1");
            int itmsta = obj.getInt("itmsta");
            double phyall = obj.getDouble("phyall");
            double physto = obj.getDouble("physto");
            int reocod = obj.getInt("reocod");
            String stu = obj.has("stu") ? obj.getString("stu") : "UN";
            String tclcod = obj.getString("tclcod");
            String tsicod0 = obj.getString("tsicod0");
            long update = obj.getLong("update");

            boolean insert = ex == null;
            boolean changes = false;
            if (ex == null) {
                ex = new Article();
                ex.setItmref(itmref);
                changes = true;
            }


            if (!fcy.equals(ex.getFcy())) {
                ex.setFcy(fcy);
                changes = true;
            }
            if (!itmdes.equals(ex.getItmdes())) {
                ex.setItmdes(itmdes);
                changes = true;
            }
            if (itmsta != ex.getItmsta()) {
                ex.setItmsta(itmsta);
                changes = true;
            }
            if (Math.abs(phyall - ex.getPhyall()) > 0.001) {
                ex.setPhyall(phyall);
                changes = true;
            }
            if (Math.abs(physto - ex.getPhysto()) > 0.001) {
                ex.setPhysto(physto);
                changes = true;
            }
            if (reocod != ex.getReocod()) {
                ex.setReocod(reocod);
                changes = true;
            }
            if (!stu.equals(ex.getStu())) {
                ex.setStu(stu);
                changes = true;
            }
            if (!tclcod.equals(ex.getTclcod())) {
                ex.setTclcod(tclcod);
                changes = true;
            }
            if (!tsicod0.equals(ex.getTsicod0())) {
                ex.setTsicod0(tsicod0);
                changes = true;
            }

            if (update != ex.getUpdate()) {
                ex.setUpdate(update);
                changes = true;
            }

            if (insert) {
                Log.d(TAG, "inserting article " + itmref);
                insertList.add(ex);
                //dao.insertAll(ex);
            } else if (changes) {
                Log.d(TAG, "updating article " + itmref);
                updateList.add(ex);
                //dao.updateAll(ex);
            }
        }

        dao.deleteAll(deleteList.toArray(new Article[deleteList.size()]));
        dao.insertAll(insertList.toArray(new Article[insertList.size()]));
        dao.updateAll(updateList.toArray(new Article[updateList.size()]));
        mLastPlanUpdate = System.currentTimeMillis();

    }


    private long mPlanSyncCounter = 0;
    private long mPlanSyncCounterLastTime = 0;

    private synchronized void syncPlans(List<JSONObject> objs) throws JSONException {



        long elapsed = System.currentTimeMillis() - mPlanSyncCounterLastTime;
        if(elapsed >= 60*1000){

            double rate = mPlanSyncCounter / (elapsed/1000.0);
            Log.d(TAG, String.format("syncPlans rate : %.2f plans/sec", rate));
            mPlanSyncCounterLastTime = System.currentTimeMillis();
            mPlanSyncCounter = 0;
        }
        mPlanSyncCounter += objs != null ? objs.size() : 0;

        //must use ArrayList for removeRange otherwise concurrency exception
        ArrayList<Plan> deleteList = new ArrayList<>();
        ArrayList<Plan> insertList = new ArrayList<>();
        ArrayList<Plan> updateList = new ArrayList<>();
        Plan.PlanDAO dao = AppDatabase.getInstance(this).getPlanDao();

        String[] ids = new String[objs.size()];
        for(int i = 0 ; i < objs.size(); i++){
            JSONObject o = objs.get(i);
            String id = o.getString("id");
            ids[i] = id;
        }

        List<Plan> existent = dao.get(ids);
        Map<String, Plan> existentMap = new HashMap<>(existent.size());
        for(Plan p : existent){
            existentMap.put(p.getId(), p);
        }

        for(JSONObject obj : objs) {

            String id = obj.getString("id");
            boolean deleted = obj.has("deleted") ? obj.getBoolean("deleted") : false;


            //Plan ex = dao.get(id);
            Plan ex = existentMap.get(id);

            if (deleted) {
                if (ex != null) {
                    Log.d(TAG, "deleting plan " + id);
                    //dao.deleteAll(ex);
                    deleteList.add(ex);
                }
                continue;
            }


            //String id = obj.getString("id");
            boolean assigned = obj.getBoolean("assigned");
            String assignment_code = obj.getString("assignment_code");
            String assignment_group = obj.getString("assignment_group");
            boolean completed = obj.getBoolean("completed");
            String plannerId = obj.getString("planner_id");
            String operation = obj.getString("operation");
            int openum = obj.getInt("openum");
            String mfgnum = obj.getString("mfgnum");
            String nextmfgnum = obj.getString("nextmfgnum");
            int nextopenum = obj.getInt("nextopenum");
            String wcr = obj.getString("workcenter");
            String wst = obj.getString("workstation");
            int workstationNumber = obj.getInt("workstation_number");
            long start = obj.getLong("start");
            long end = obj.getLong("end");
            long duration = obj.getLong("duration");
            long update = obj.getLong("update");
            long updateCounter = obj.getLong("update_counter");
            String itmref = obj.getString("itmref");
            String itmdes = obj.getString("itmdes");
            double itmqty = obj.getDouble("itmqty");
            String itmstu = obj.getString("itmstu");
            String vcrnumori = obj.getString("vcrnumori");
            int vcrlinori = obj.getInt("vcrlinori");
            double extqty = obj.getDouble("extqty");
            double cplqty = obj.getDouble("cplqty");
            double assigned_qty = obj.getDouble("assigned_qty");

            boolean insert = ex == null;
            boolean changes = false;
            if (insert) {
                ex = new Plan();
                ex.setId(id);
                ex.setUpdate(0);
                changes = true;
            }
            //the first time we get an update is the server
            //acknowledging the assignment so everything goes smooth
            //but in case we missed the update on the server and got a downlink update
            //we need to handle conflict and re-send the server update
            if(ex.isDirty()){
                //we may get ourselves a conflict
                boolean conflict = false;
                if (assigned != ex.isAssigned()) {
                    conflict = true;
                }
                if (!assignment_code.equals(ex.getAssignmentCode())) {
                    conflict = true;
                }
                if(Math.abs(assigned_qty - ex.getAssignedQty()) > 0.001){
                    conflict = true;
                }
                //if(start != ex.getStart())
                //    conflict = true;
                //if(end != ex.getEnd())
                //    conflict = true;

                //if we are tracking offline we need to assume whatever server sends back
                //so we do local update on the plan and commit the tracking for the server
                //when the server comes back with data we assume it's right


                if(completed != ex.isCompleted()){
                    conflict = true;
                }
                if(Math.abs(cplqty - ex.getCplqty()) > 0.001){
                    conflict = true;
                }


                if(conflict) {
                    //server missed our previous update
                    Log.d(TAG, "resolving conflict for plan " + ex.getId());
                    JSONObject dt = new JSONObject();
                    try {
                        dt.put("id", ex.getId());
                        dt.put("assigned", ex.isAssigned());
                        dt.put("assignment_code", ex.getAssignmentCode());
                        dt.put("assigned_qty", ex.getAssignedQty());
                        //dt.put("start", ex.getStart());
                        //dt.put("end", ex.getEnd());
                        //dt.put("completed", ex.isCompleted());
                        //dt.put("cplqty", ex.getCplqty());
                        //obj.put("update", System.currentTimeMillis());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    mSocket.emit("update:plan", dt);
                    assigned = ex.isAssigned();
                    assignment_code = ex.getAssignmentCode();
                    assigned_qty = ex.getAssignedQty();
                    //start = ex.getStart();
                    //end = ex.getEnd();
                    //completed = ex.isCompleted();
                    //cplqty = ex.getCplqty();
                }else{
                    ex.setDirty(false);
                    changes = true;
                }
            }else if(update <= ex.getUpdate()){
                continue; //move along
            }


            if (assigned != ex.isAssigned()) {
                ex.setAssigned(assigned);
                changes = true;
            }

            if (!assignment_code.equals(ex.getAssignmentCode())) {
                ex.setAssignmentCode(assignment_code);
                changes = true;
            }

            if (!assignment_group.equals(ex.getAssignmentGroup())) {
                ex.setAssignmentGroup(assignment_group);
                changes = true;
            }


            if (completed != ex.isCompleted()) {
                ex.setCompleted(completed);
                changes = true;
            }

            if (!plannerId.equals(ex.getPlannerId())) {
                ex.setPlannerId(plannerId);
                changes = true;
            }
            if (!operation.equals(ex.getOperation())) {
                ex.setOperation(operation);
                changes = true;
            }
            if (openum != ex.getOpenum()) {
                ex.setOpenum(openum);
                changes = true;
            }
            if (!mfgnum.equals(ex.getMfgnum())) {
                ex.setMfgnum(mfgnum);
                changes = true;
            }
            if (!nextmfgnum.equals(ex.getNextmfgnum())) {
                ex.setNextmfgnum(nextmfgnum);
                changes = true;
            }
            if (nextopenum != ex.getNextopenum()) {
                ex.setNextopenum(nextopenum);
                changes = true;
            }

            if (!wcr.equals(ex.getWcr())) {
                ex.setWcr(wcr);
                changes = true;
            }
            if (!wst.equals(ex.getWst())) {
                ex.setWst(wst);
                changes = true;
            }
            if (workstationNumber != ex.getWorkstationNumber()) {
                ex.setWorkstationNumber(workstationNumber);
                changes = true;
            }
            if (start != ex.getStart()) {
                ex.setStart(start);
                changes = true;
            }
            if (end != ex.getEnd()) {
                ex.setEnd(end);
                changes = true;
            }
            if (duration != ex.getDuration()) {
                ex.setDuration(duration);
                changes = true;
            }
            if (update != ex.getUpdate()) {
                ex.setUpdate(update);
                changes = true;
            }
            if (updateCounter != ex.getUpdateCounter()) {
                ex.setUpdateCounter(updateCounter);
                changes = true;
            }

            if (!itmref.equals(ex.getItmref())) {
                ex.setItmref(itmref);
                changes = true;
            }
            if (!itmdes.equals(ex.getItmdes())) {
                ex.setItmdes(itmdes);
                changes = true;
            }

            if(Math.abs(itmqty - ex.getItmqty()) > 0.001){
                ex.setItmqty(itmqty);
                changes = true;
            }
            if (!itmstu.equals(ex.getItmstu())) {
                ex.setItmstu(itmstu);
                changes = true;
            }
            if (!vcrnumori.equals(ex.getVcrnumori())) {
                ex.setVcrnumori(vcrnumori);
                changes = true;
            }
            if (vcrlinori != ex.getVcrlinori()) {
                ex.setVcrlinori(vcrlinori);
                changes = true;
            }
            if(Math.abs(extqty - ex.getExtqty()) > 0.001){
                ex.setExtqty(extqty);
                changes = true;
            }
            if(Math.abs(cplqty - ex.getCplqty()) > 0.001){
                ex.setCplqty(cplqty);
                changes = true;
            }
            if(Math.abs(assigned_qty - ex.getAssignedQty()) > 0.001){
                ex.setAssignedQty(assigned_qty);
                changes = true;
            }


            if (insert) {
                Log.d(TAG, "inserting plan " + ex.getOperation());
                //dao.insertAll(ex);
                insertList.add(ex);
            } else if (changes) {
                Log.d(TAG, "updating plan " + ex.getOperation());
                //dao.updateAll(ex);
                updateList.add(ex);
            }
        }

        if(deleteList.size() > 0)
            dao.deleteAll(deleteList.toArray(new Plan[deleteList.size()]));
        if(insertList.size() > 0)
            dao.insertAll(insertList.toArray(new Plan[insertList.size()]));
        if(updateList.size() > 0)
            dao.updateAll(updateList.toArray(new Plan[updateList.size()]));

        /*
        while(deleteList.size() > 0){
            List<Plan> l = deleteList.subList(0, Math.min(deleteList.size(), 20));
            dao.deleteAll(l.toArray(new Plan[l.size()]));
            mLastPlanUpdate = System.currentTimeMillis();
            l.clear();
        }
        while(insertList.size() > 0){
            List<Plan> l = insertList.subList(0, Math.min(insertList.size(), 20));
            dao.insertAll(l.toArray(new Plan[l.size()]));
            mLastPlanUpdate = System.currentTimeMillis();
            l.clear();
        }
        while(updateList.size() > 0){
            List<Plan> l = updateList.subList(0, Math.min(updateList.size(), 20));
            dao.updateAll(l.toArray(new Plan[l.size()]));
            mLastPlanUpdate = System.currentTimeMillis();
            l.clear();
        }
        */
        //https://stackoverflow.com/questions/2289183/why-is-javas-abstractlists-removerange-method-protected


        //FIXED: this was causing a deadlock
        /*
        if(updateList.size() > 0){

            Handler h = new Handler(getMainLooper());
            h.postDelayed(() -> {
                Message msg = Message.obtain(new Handler(), SyncService.MSG_UPDATE_PLAN);
                msg.getData().putParcelableArrayList(ARG_OBJ, updateList);
                broadcastMessage(msg);

            }, 10);


        }
        */



    }


    private synchronized void syncWorkorders(List<JSONObject> objs) throws JSONException {


        //we may have duplicate updates on the same batch
        //because we may have 2 updates on the same order and we would end up trying
        //to insert same data twice
        {
            List<JSONObject> filter = new ArrayList<>(objs.size());
            List<String> uniqueMfgnum = new ArrayList<>(objs.size());
            for (int i = objs.size() - 1; i >= 0; i--) {
                JSONObject o = objs.get(i);
                String mfgnum = o.getString("mfgnum");
                if (!uniqueMfgnum.contains(mfgnum)) {
                    filter.add(o);
                    uniqueMfgnum.add(mfgnum);
                }
                objs.remove(i);
            }
            Collections.reverse(filter);
            objs = filter;
            filter = null;
        }



        List<Workorder> deleteWoList = new ArrayList<>();
        List<Workorder> insertWoList = new ArrayList<>();
        List<Workorder> updateWoList = new ArrayList<>();

        List<WorkItem> deleteWiList = new ArrayList<>();
        List<WorkItem> insertWiList = new ArrayList<>();
        List<WorkItem> updateWiList = new ArrayList<>();

        List<WorkMaterial> deleteWmList = new ArrayList<>();
        List<WorkMaterial> insertWmList = new ArrayList<>();
        List<WorkMaterial> updateWmList = new ArrayList<>();

        List<Operation> deleteOpList = new ArrayList<>();
        List<Operation> insertOpList = new ArrayList<>();
        List<Operation> updateOpList = new ArrayList<>();

        Workorder.WorkorderDAO dao = AppDatabase.getInstance(this).getWorkorderDao();
        WorkItem.WorkItemDAO wiDao = AppDatabase.getInstance(this).getWorkItemDao();
        WorkMaterial.WorkMaterialDAO wmDao = AppDatabase.getInstance(this).getWorkMaterialDao();
        Operation.OperationDAO opDao = AppDatabase.getInstance(this).getOperationDao();

        String[] mfgnums = new String[objs.size()];
        for(int i = 0; i < objs.size(); i++){
            JSONObject o = objs.get(i);
            mfgnums[i] = o.getString("mfgnum");
        }

        List<Workorder> wos = dao.getAll(mfgnums);
        List<WorkItem> cachedItems  = wiDao.getAll(mfgnums);
        List<WorkMaterial> cachedMaterials  = wmDao.getAll(mfgnums);
        List<Operation> cachedOperations  = opDao.getAll(mfgnums);

        Map<String, Workorder> cachedWos = new HashMap<>(wos.size());
        for(int i = 0; i < wos.size() ; i ++){
            Workorder w = wos.get(i);
            w.setItems(new ArrayList<>(1));
            w.setMaterials(new ArrayList<>(5));
            w.setOperations(new ArrayList<>(2));
            cachedWos.put(w.getMfgnum(), w);
        }
        for(int i = 0; i < cachedItems.size() ; i ++){
            WorkItem itm = cachedItems.get(i);
            Workorder w = cachedWos.get(itm.getMfgnum());
            if(itm != null && w != null)
                w.getItems().add(itm);
        }
        for(int i = 0; i < cachedMaterials.size() ; i ++){
            WorkMaterial mat = cachedMaterials.get(i);
            Workorder w = cachedWos.get(mat.getMfgnum());
            if(mat != null && w != null)
                w.getMaterials().add(mat);
        }
        for(int i = 0; i < cachedOperations.size() ; i ++){
            Operation o = cachedOperations.get(i);
            Workorder w = cachedWos.get(o.getMfgnum());
            if(o != null && w != null)
                w.getOperations().add(o);
        }

        List<String> dbgUniqueMfgnum = new ArrayList<>(objs.size());




        for(JSONObject obj : objs) {

            String mfgnum = obj.getString("mfgnum");
            boolean deleted = obj.has("deleted") ? obj.getBoolean("deleted") : false;

            if(dbgUniqueMfgnum.contains(mfgnum)){
                Log.e(TAG, "repeated WorkOrder update");
            }
            dbgUniqueMfgnum.add(mfgnum);

            /*
            Workorder ex = dao.get(mfgnum);
            if (ex != null) {

                List<WorkItem> items = wiDao.getAll(mfgnum);
                List<WorkMaterial> mats = wmDao.getAll(mfgnum);
                List<Operation> ops = opDao.getAll(mfgnum);

                ex.setItems(items);
                ex.setMaterials(mats);
                ex.setOperations(ops);
            }
            */
            Workorder ex = cachedWos.get(mfgnum);

            if (deleted) {
                if (ex != null) {
                    Log.d(TAG, "deleting workorder " + mfgnum);

                    if (ex.getItems() != null && ex.getItems().size() > 0)
                        deleteWiList.addAll(ex.getItems());
                        //wiDao.deleteAll(ex.getItems().toArray(new WorkItem[ex.getItems().size()]));
                    if (ex.getMaterials() != null && ex.getMaterials().size() > 0)
                        deleteWmList.addAll(ex.getMaterials());
                        //wmDao.deleteAll(ex.getMaterials().toArray(new WorkMaterial[ex.getMaterials().size()]));
                    if (ex.getOperations() != null && ex.getOperations().size() > 0)
                        deleteOpList.addAll(ex.getOperations());
                        //opDao.deleteAll(ex.getOperations().toArray(new Operation[ex.getOperations().size()]));
                    deleteWoList.add(ex);
                    //dao.deleteAll(ex);
                }
                continue;
            }

            String mfgfcy = obj.has("mfgfcy") ? obj.getString("mfgfcy") : "001";
            int mfgsta = obj.getInt("mfgsta");
            int mfgtrkflg = obj.getInt("mfgtrkflg");
            long update = obj.getLong("update");

            JSONArray jitems = obj.getJSONArray("items");
            JSONArray jmaterials = obj.getJSONArray("components");
            JSONArray joperations = obj.getJSONArray("operations");

            boolean insert = ex == null;
            boolean changes = false;
            if (ex == null) {
                ex = new Workorder();
                ex.setMfgnum(mfgnum);
                ex.setMfgfcy(mfgfcy);
                ex.setItems(new ArrayList<>());
                ex.setMaterials(new ArrayList<>());
                ex.setOperations(new ArrayList<>());
                changes = true;
            }

            if(!mfgfcy.equals(ex.getMfgfcy())){
                ex.setMfgfcy(mfgfcy);
                changes = true;
            }

            if (mfgsta != ex.getMfgsta()) {
                ex.setMfgsta(mfgsta);
                changes = true;
            }
            if (mfgtrkflg != ex.getMfgtrkflg()) {
                ex.setMfgtrkflg(mfgtrkflg);
                changes = true;
            }
            if (update != ex.getUpdate()) {
                ex.setUpdate(update);
                changes = true;
            }


            if (insert) {
                Log.d(TAG, "inserting workorder " + ex.getMfgnum());
                insertWoList.add(ex);
                //dao.insertAll(ex);
            } else if (changes) {
                Log.d(TAG, "updating workorder " + ex.getMfgnum());
                updateWoList.add(ex);
                //dao.updateAll(ex);
            }
            if (true) {
                List<WorkItem> items = ex.getItems();
                List<WorkItem> batchInsertItems = new ArrayList<>();
                List<WorkItem> batchUpdateItems = new ArrayList<>();

                for (int i = 0; i < jitems.length(); i++) {
                    JSONObject jitem = jitems.getJSONObject(i);
                    int mfglin = jitem.getInt("mfglin");
                    String itmref = jitem.getString("itmref");
                    String itmdes = jitem.getString("itmdes1");
                    String jmfgnum = jitem.getString("mfgnum");
                    double cplqty = jitem.getDouble("cplqty");
                    String epxitmref = jitem.getString("epxitmref");
                    double extqty = jitem.getDouble("extqty");
                    int itmsta = jitem.getInt("itmsta");
                    String matitmref = jitem.getString("matitmref");
                    int mfitrkflg = jitem.getInt("mfitrkflg");
                    String pjt = jitem.getString("pjt");
                    String soqtext = jitem.getString("soqtext");
                    String stu = jitem.getString("stu");
                    double uomextqty = jitem.getDouble("uomextqty");
                    double uomstucoe = jitem.getDouble("uomstucoe");
                    String vcritmref = jitem.getString("vcritmref");
                    int vcrlinori = jitem.getInt("vcrlinori");
                    String vcrnumori = jitem.getString("vcrnumori");

                    WorkItem wex = null;
                    for (WorkItem it : items) {
                        if (itmref.equals(it.getItmref()) && mfglin == it.getMfglin()) {
                            wex = it;
                            break;
                        }
                    }
                    insert = false;
                    changes = false;
                    if (wex == null) {
                        wex = new WorkItem();
                        insert = true;
                    }

                    if (!mfgnum.equals(wex.getMfgnum())) {
                        wex.setMfgnum(mfgnum);
                        changes = true;
                    }

                    if (mfglin != wex.getMfglin()) {
                        wex.setMfglin(mfglin);
                        changes = true;
                    }

                    if (!itmref.equals(wex.getItmref())) {
                        wex.setItmref(itmref);
                        changes = true;
                    }
                    if (!itmdes.equals(wex.getItmdes())) {
                        wex.setItmdes(itmdes);
                        changes = true;
                    }
                    if (Math.abs(cplqty - wex.getCplqty()) > .001) {
                        wex.setCplqty(cplqty);
                        changes = true;
                    }
                    if (!epxitmref.equals(wex.getEpxitmref())) {
                        wex.setEpxitmref(epxitmref);
                        changes = true;
                    }
                    if (Math.abs(extqty - wex.getExtqty()) > .001) {
                        wex.setExtqty(extqty);
                        changes = true;
                    }
                    if (itmsta != wex.getItmsta()) {
                        wex.setItmsta(itmsta);
                        changes = true;
                    }
                    if (!matitmref.equals(wex.getMatitmref())) {
                        wex.setMatitmref(matitmref);
                        changes = true;
                    }
                    if (mfitrkflg != wex.getMfitrkflg()) {
                        wex.setMfitrkflg(mfitrkflg);
                        changes = true;
                    }
                    if (!pjt.equals(wex.getPjt())) {
                        wex.setPjt(pjt);
                        changes = true;
                    }
                    if (!soqtext.equals(wex.getSoqtext())) {
                        wex.setSoqtext(soqtext);
                        changes = true;
                    }
                    if (!stu.equals(wex.getStu())) {
                        wex.setStu(stu);
                        changes = true;
                    }
                    if (Math.abs(uomextqty - wex.getUomextqty()) > .001) {
                        wex.setUomextqty(uomextqty);
                        changes = true;
                    }
                    if (Math.abs(uomstucoe - wex.getUomstucoe()) > .001) {
                        wex.setUomstucoe(uomstucoe);
                        changes = true;
                    }
                    if (!vcritmref.equals(wex.getVcritmref())) {
                        wex.setVcritmref(vcritmref);
                        changes = true;
                    }
                    if (vcrlinori != wex.getVcrlinori()) {
                        wex.setVcrlinori(vcrlinori);
                        changes = true;
                    }
                    if (!vcrnumori.equals(wex.getVcrnumori())) {
                        wex.setVcrnumori(vcrnumori);
                        changes = true;
                    }


                    if (insert) {
                        batchInsertItems.add(wex);
                    } else if (changes) {
                        items.remove(wex);
                        batchUpdateItems.add(wex);
                    } else {
                        items.remove(wex);
                    }

                }

                if (batchInsertItems.size() > 0)
                    insertWiList.addAll(batchInsertItems);
                    //wiDao.insertAll(batchInsertItems.toArray(new WorkItem[batchInsertItems.size()]));
                if (batchUpdateItems.size() > 0)
                    updateWiList.addAll(batchUpdateItems);
                    //wiDao.updateAll(batchUpdateItems.toArray(new WorkItem[batchUpdateItems.size()]));
                if (items.size() > 0)
                    deleteWiList.addAll(items);
                    //wiDao.deleteAll(items.toArray(new WorkItem[items.size()]));
            }
            //do materials
            if (true) {
                List<WorkMaterial> materials = ex.getMaterials();
                List<WorkMaterial> batchInsertItems = new ArrayList<>();
                List<WorkMaterial> batchUpdateItems = new ArrayList<>();

                for (int i = 0; i < jmaterials.length(); i++) {
                    JSONObject jitem = jmaterials.getJSONObject(i);
                    int mfglin = jitem.getInt("mfglin");
                    String itmref = jitem.getString("itmref");
                    String itmdes = jitem.getString("itmdes1");
                    String jmfgnum = jitem.getString("mfgnum");
                    int bomope = jitem.getInt("bomope");
                    int bomseq = jitem.getInt("bomseq");
                    int mfmtrkflg = jitem.getInt("mfmtrkflg");
                    double allqty = jitem.getDouble("allqty");
                    double avaqty = jitem.getDouble("avaqty");
                    double shtqty = jitem.getDouble("shtqty");
                    double useqty = jitem.getDouble("useqty");
                    double retqty = jitem.getDouble("retqty");
                    String stu = jitem.getString("stu");

                    WorkMaterial wex = null;
                    for (WorkMaterial it : materials) {
                        if (mfglin == it.getMfglin() && bomseq == it.getBomseq() && itmref.equals(it.getItmref())) {
                            wex = it;
                            break;
                        }
                    }
                    insert = false;
                    changes = false;
                    if (wex == null) {
                        wex = new WorkMaterial();
                        insert = true;
                    }
                    if (!mfgnum.equals(wex.getMfgnum())) {
                        wex.setMfgnum(mfgnum);
                        changes = true;
                    }

                    if (mfglin != wex.getMfglin()) {
                        wex.setMfglin(mfglin);
                        changes = true;
                    }

                    if (!itmref.equals(wex.getItmref())) {
                        wex.setItmref(itmref);
                        changes = true;
                    }
                    if (!itmdes.equals(wex.getItmdes())) {
                        wex.setItmdes(itmdes);
                        changes = true;
                    }
                    if (bomope != wex.getBomope()) {
                        wex.setBomope(bomope);
                        changes = true;
                    }
                    if (bomseq != wex.getBomseq()) {
                        wex.setBomseq(bomseq);
                        changes = true;
                    }
                    if (mfmtrkflg != wex.getMfmtrkflg()) {
                        wex.setMfmtrkflg(mfmtrkflg);
                        changes = true;
                    }


                    if (Math.abs(allqty - wex.getAllqty()) > .001) {
                        wex.setAllqty(allqty);
                        changes = true;
                    }
                    if (Math.abs(avaqty - wex.getAvaqty()) > .001) {
                        wex.setAvaqty(avaqty);
                        changes = true;
                    }
                    if (Math.abs(shtqty - wex.getShtqty()) > .001) {
                        wex.setShtqty(shtqty);
                        changes = true;
                    }
                    if (Math.abs(useqty - wex.getUseqty()) > .001) {
                        wex.setUseqty(useqty);
                        changes = true;
                    }
                    if (Math.abs(retqty - wex.getRetqty()) > .001) {
                        wex.setRetqty(retqty);
                        changes = true;
                    }

                    if (!stu.equals(wex.getStu())) {
                        wex.setStu(stu);
                        changes = true;
                    }


                    if (insert) {
                        batchInsertItems.add(wex);
                    } else if (changes) {
                        materials.remove(wex);
                        batchUpdateItems.add(wex);
                    } else {
                        materials.remove(wex);
                    }

                }

                if (batchInsertItems.size() > 0)
                    insertWmList.addAll(batchInsertItems);
                if (batchUpdateItems.size() > 0)
                    updateWmList.addAll(batchUpdateItems);
                if (materials.size() > 0)
                    deleteWmList.addAll(materials);
                /*
                if (batchInsertItems.size() > 0)
                    wmDao.insertAll(batchInsertItems.toArray(new WorkMaterial[batchInsertItems.size()]));
                if (batchUpdateItems.size() > 0)
                    wmDao.updateAll(batchUpdateItems.toArray(new WorkMaterial[batchUpdateItems.size()]));
                if (materials.size() > 0)
                    wmDao.deleteAll(materials.toArray(new WorkMaterial[materials.size()]));
                */
            }
            //do operations
            if (true) {
                List<Operation> operations = ex.getOperations();
                List<Operation> batchInsertOperations = new ArrayList<>();
                List<Operation> batchUpdateOperations = new ArrayList<>();

                for (int i = 0; i < joperations.length(); i++) {
                    JSONObject jitem = joperations.getJSONObject(i);

                    String wcr = jitem.getString("wcr");
                    String labwcr = jitem.getString("labwcr");
                    String jmfgnum = jitem.getString("mfgnum");
                    String extlab = jitem.getString("extlab");
                    int extlabnbr = jitem.getInt("extlabnbr");
                    String extwst = jitem.getString("extwst");
                    int extwstnbr = jitem.getInt("extwstnbr");
                    long opestr = jitem.getLong("opestr");
                    long opeend = jitem.getLong("opeend");
                    int mfotrkflg = jitem.getInt("mfotrkflg");
                    int nexopenum = jitem.getInt("nexopenum");
                    int openum = jitem.getInt("openum");
                    int opesplnum = jitem.getInt("opesplnum");
                    int opesta = jitem.getInt("opesta");
                    String opeuom = jitem.getString("opeuom");
                    double cplqty = jitem.getDouble("cplqty");
                    double extqty = jitem.getDouble("extqty");
                    double extopetim = jitem.getDouble("extopetim");
                    double extsettim = jitem.getDouble("extsettim");
                    double extunttim = jitem.getDouble("extunttim");
                    double cplopetim = jitem.has("cplopetim") ? jitem.getDouble("cplopetim") : 0;
                    double cplsettim = jitem.has("cplsettim") ? jitem.getDouble("cplsettim") : 0;
                    int timuomcod = jitem.getInt("timuomcod");

                    Operation wex = null;
                    for (Operation it : operations) {
                        if (openum == it.getOpenum() && opesplnum == it.getOpesplnum()) {
                            wex = it;
                            break;
                        }
                    }
                    insert = false;
                    changes = false;
                    if (wex == null) {
                        wex = new Operation();
                        insert = true;
                    }
                    if (!mfgnum.equals(wex.getMfgnum())) {
                        wex.setMfgnum(mfgnum);
                        changes = true;
                    }
                    if (!wcr.equals(wex.getWcr())) {
                        wex.setWcr(wcr);
                        changes = true;
                    }
                    if (!labwcr.equals(wex.getLabwcr())) {
                        wex.setLabwcr(labwcr);
                        changes = true;
                    }
                    if (!extlab.equals(wex.getExtlab())) {
                        wex.setExtlab(extlab);
                        changes = true;
                    }
                    if (extlabnbr != wex.getExtlabnbr()) {
                        wex.setExtlabnbr(extlabnbr);
                        changes = true;
                    }
                    if (!extwst.equals(wex.getExtwst())) {
                        wex.setExtwst(extwst);
                        changes = true;
                    }


                    if (extwstnbr != wex.getExtwstnbr()) {
                        wex.setExtlabnbr(extwstnbr);
                        changes = true;
                    }
                    if (opestr != wex.getOpestr()) {
                        wex.setOpestr(opestr);
                        changes = true;
                    }
                    if (opeend != wex.getOpeend()) {
                        wex.setOpeend(opeend);
                        changes = true;
                    }
                    if (mfotrkflg != wex.getMfotrkflg()) {
                        wex.setMfotrkflg(mfotrkflg);
                        changes = true;
                    }
                    if (nexopenum != wex.getNexopenum()) {
                        wex.setNexopenum(nexopenum);
                        changes = true;
                    }
                    if (openum != wex.getOpenum()) {
                        wex.setOpenum(openum);
                        changes = true;
                    }
                    if (opesplnum != wex.getOpesplnum()) {
                        wex.setOpesplnum(opesplnum);
                        changes = true;
                    }
                    if (opesta != wex.getOpesta()) {
                        wex.setOpesta(opesta);
                        changes = true;
                    }
                    if (!opeuom.equals(wex.getOpeuom())) {
                        wex.setOpeuom(opeuom);
                        changes = true;
                    }


                    if (Math.abs(cplqty - wex.getCplqty()) > .001) {
                        wex.setCplqty(cplqty);
                        changes = true;
                    }
                    if (Math.abs(extqty - wex.getExtqty()) > .001) {
                        wex.setExtqty(extqty);
                        changes = true;
                    }

                    if (Math.abs(extsettim - wex.getExtsettim()) > .001) {
                        wex.setExtsettim(extsettim);
                        changes = true;
                    }

                    if (Math.abs(extunttim - wex.getExtunttim()) > .001) {
                        wex.setExtunttim(extunttim);
                        changes = true;
                    }

                    if (Math.abs(extopetim - wex.getExtopetim()) > .001) {
                        wex.setExtopetim(extopetim);
                        changes = true;
                    }

                    if (Math.abs(cplopetim - wex.getCplopetim()) > .001) {
                        wex.setCplopetim(cplopetim);
                        changes = true;
                    }
                    if (Math.abs(cplsettim - wex.getCplsettim()) > .001) {
                        wex.setCplsettim(cplsettim);
                        changes = true;
                    }

                    if (timuomcod != wex.getTimuomcod()) {
                        wex.setTimuomcod(timuomcod);
                        changes = true;
                    }


                    if (insert) {
                        batchInsertOperations.add(wex);
                    } else if (changes) {
                        operations.remove(wex);
                        batchUpdateOperations.add(wex);
                    } else {
                        operations.remove(wex);
                    }

                }

                if (batchInsertOperations.size() > 0)
                    insertOpList.addAll(batchInsertOperations);
                if (batchUpdateOperations.size() > 0)
                    updateOpList.addAll(batchUpdateOperations);
                if (operations.size() > 0)
                    deleteOpList.addAll(operations);
                /*
                if (batchInsertItems.size() > 0)
                    opDao.insertAll(batchInsertItems.toArray(new Operation[batchInsertItems.size()]));
                if (batchUpdateItems.size() > 0)
                    opDao.updateAll(batchUpdateItems.toArray(new Operation[batchUpdateItems.size()]));
                if (operations.size() > 0)
                    opDao.deleteAll(operations.toArray(new Operation[operations.size()]));
                */
            }
        }

        dao.deleteAll(deleteWoList.toArray(new Workorder[deleteWoList.size()]));
        opDao.deleteAll(deleteOpList.toArray(new Operation[deleteOpList.size()]));
        wiDao.deleteAll(deleteWiList.toArray(new WorkItem[deleteWiList.size()]));
        wmDao.deleteAll(deleteWmList.toArray(new WorkMaterial[deleteWmList.size()]));

        dao.updateAll(updateWoList.toArray(new Workorder[updateWoList.size()]));
        opDao.updateAll(updateOpList.toArray(new Operation[updateOpList.size()]));
        wiDao.updateAll(updateWiList.toArray(new WorkItem[updateWiList.size()]));
        wmDao.updateAll(updateWmList.toArray(new WorkMaterial[updateWmList.size()]));

        dao.insertAll(insertWoList.toArray(new Workorder[insertWoList.size()]));
        opDao.insertAll(insertOpList.toArray(new Operation[insertOpList.size()]));
        wiDao.insertAll(insertWiList.toArray(new WorkItem[insertWiList.size()]));
        wmDao.insertAll(insertWmList.toArray(new WorkMaterial[insertWmList.size()]));

        mLastPlanUpdate = System.currentTimeMillis();

    }


    private boolean saveTrackingToFile(JSONObject tracking, String fname){

        try {
            FileOutputStream fos = openFileOutput(fname, Context.MODE_PRIVATE);
            if (tracking != null) {
                fos.write(tracking.toString().getBytes());
            }
            fos.close();
            return true;
        } catch (FileNotFoundException fileNotFound) {
            return false;
        } catch (IOException ioException) {
            return false;
        }

    }

    private void deleteTrackingFile(String fname){
        File[] trackings = this.getFilesDir().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.equals(fname);
            }
        });

        if(trackings != null && trackings.length > 0){
            for(File f : trackings){
                f.delete();
            }
        }


    }

    private synchronized int sendTrackingFiles(){

        int res = 0;
        File[] trackings = this.getFilesDir().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.endsWith(".json");
            }
        });

        if(trackings != null && trackings.length > 0){
            for(File f : trackings){

                try {
                    FileInputStream fis = openFileInput(f.getName());
                    InputStreamReader isr = new InputStreamReader(fis);
                    BufferedReader bufferedReader = new BufferedReader(isr);
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        sb.append(line);
                    }

                    JSONObject tracking = new JSONObject(sb.toString());
                    mSocket.emit("insert:tracking", tracking, new Ack() {
                        @Override
                        public void call(Object... args) {

                            boolean result = false;

                            if (args.length > 0 && args[args.length - 1] instanceof JSONObject) {
                                JSONObject ack = (JSONObject) args[args.length - 1];
                                try {
                                    result = ack.getBoolean("result");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                            if(result) {
                                f.delete();
                            }else{
                                Log.e(TAG, "Tracking file rejected by server "+f.getName());
                                f.renameTo(new File(f.getAbsolutePath()+".rej"));
                            }
                        }
                    });
                    res++;


                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Unable to process backup file "+f.getName());
                    f.renameTo(new File(f.getAbsolutePath()+".err"));
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Unable to parse json "+f.getName());
                    f.renameTo(new File(f.getAbsolutePath()+".jerr"));
                }
            }
        }

        return res;

    }


    private void saveSettings(String id) {

        Job.JobDAO dao = AppDatabase.getInstance(this).getJobDao();
        List<Job> dbJobs = dao.getAll();


        JSONObject settings = new JSONObject();
        try {

            JSONArray jobs = new JSONArray();


            for (Job j : dbJobs) {

                JSONObject o = new JSONObject();
                o.put("id", j.getId());
                o.put("assignment_code", j.getAssignmentCode());
                o.put("operator", j.getOperator());
                o.put("wcr", j.getWcr());
                o.put("wst", j.getWst());
                o.put("index", j.getIndex());
                o.put("name", j.getName());
                o.put("visible", j.getVisible());
                //o.put("parameters", j.getFilterParameters());

                jobs.put(o);

            }

            settings.put("jobs", jobs);
            settings.put("id", id);
            settings.put("type", "client");

            mSocket.emit("replace:settings", settings);


        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }
    }

    private int restoreSettings(String id, final Messenger replyTo) {


        JSONObject key = new JSONObject();
        try {
            key.put("id", id);
            key.put("type", "client");
        } catch (JSONException e) {
            e.printStackTrace();
            return 0;
        }

        mSocket.emit("read:settings", key, new Ack() {
            @Override
            public void call(Object... args) {

                int res = 0;

                JSONObject settings = args != null && args.length > 0 && args[0] instanceof JSONObject
                        ? (JSONObject) args[0]
                        : null;

                if(settings != null && settings.has("jobs")){
                    try{
                        JSONArray jobs = (JSONArray) settings.get("jobs");
                        if(jobs != null && jobs.length() > 0) {

                            Job.JobDAO dao = AppDatabase.getInstance(SyncService.this).getJobDao();


                            Job[] batchInsert = new Job[jobs.length()];

                            for(int i = 0; i < jobs.length(); i++){
                                JSONObject j = jobs.getJSONObject(i);

                                Job nj = new Job();
                                nj.setAssignmentCode(j.getString("assignment_code"));
                                nj.setOperator(j.getString("operator"));
                                nj.setWcr(j.getString("wcr"));
                                nj.setWst(j.getString("wst"));
                                nj.setIndex(j.getInt("index"));
                                nj.setName(j.getString("name"));
                                nj.setVisible(j.getBoolean("visible"));

                                batchInsert[i] = nj;
                            }

                            dao.truncate();
                            dao.insertAll(batchInsert);
                            res = batchInsert.length;

                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        res = 0;
                    }
                }


                if(replyTo != null) {
                    Message resp = Message.obtain(null, SyncService.MSG_RESTORE_SETTINGS);
                    resp.getData().putBoolean(SyncService.ARG_RESULT, res > 0);
                    resp.getData().putString(SyncService.ARG_KEY, id);
                    try {
                        replyTo.send(resp);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

            }
        });

        return 0;
    }


    private void requestFullPlanUpdate(){

        if(!mSocket.connected())
            return;

        try {
            JSONObject req = new JSONObject();
            req.put("entity", "plan");
            mSocket.emit("reload:plan", req);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }


    }

}