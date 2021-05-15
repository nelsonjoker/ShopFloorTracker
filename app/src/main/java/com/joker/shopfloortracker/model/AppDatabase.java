package com.joker.shopfloortracker.model;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;
import android.content.Context;
import android.os.Environment;
import android.support.annotation.Nullable;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.List;

import com.joker.shopfloortracker.BuildConfig;

/**
 * Created by Nelson on 05/12/2017.
 */

@Database(entities = {Workcenter.class , Workstation.class, Job.class, Article.class ,
        Plan.class, Workorder.class, WorkItem.class, WorkMaterial.class, Operation.class,
        AlertMessageType.class},
        version = 39)
@TypeConverters( Converters.class )
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase theSingleton;

    public static synchronized AppDatabase getInstance(Context ctx){

        if(theSingleton == null){
            theSingleton = Room.databaseBuilder(ctx,
                    AppDatabase.class, BuildConfig.DEBUG ? "shopfloor-tracker-database-debug" : "shopfloor-tracker-database" )
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build();
            theSingleton.initialize(ctx);
        }
        return theSingleton;
    }

    public abstract Workcenter.WorkcenterDAO getWorkcenterDao();
    public abstract Workstation.WorkstationDAO getWorkstationDao();
    public abstract Job.JobDAO getJobDao();
    public abstract Article.ArticleDAO getArticleDao();
    public abstract Plan.PlanDAO getPlanDao();
    public abstract Workorder.WorkorderDAO getWorkorderDao();
    public abstract WorkItem.WorkItemDAO getWorkItemDao();
    public abstract WorkMaterial.WorkMaterialDAO getWorkMaterialDao();
    public abstract Operation.OperationDAO getOperationDao();
    public abstract AlertMessageType.AlertMessageTypeDAO getAlertMessageTypeDao();
    public abstract UpcomingPlan.UpcomingPlanDAO getUpcomingPlanDao();


    private static final int CACHE_SIZE = 10000;



    private class WorkorderCacheRecord{
        public List<WorkItem> items;
        public List<WorkMaterial> materials;
        public long LastUpdate;
    }

    private HashMap<String, WorkorderCacheRecord> mWorkorderCache;

    private class ArticleCacheRecord{
        public Article Item;
        public long LastUpdate;
    }

    private HashMap<String, ArticleCacheRecord> mArticleCache;

    public AppDatabase(){
        mWorkorderCache = new HashMap<>(CACHE_SIZE);
        mArticleCache = new HashMap<>(CACHE_SIZE);


    }


    public void initialize(Context ctx){
        //we don't actually need the context we just
        //want to make sure it exists
        LiveData<List<String>> mChangedMfgnums = getWorkorderDao().getAllChanges(1000);
        mChangedMfgnums.observeForever(new Observer<List<String>>() {
            @Override
            public void onChanged(@Nullable List<String> s) {
                for(String mfgnum : s){
                    synchronized (mWorkorderCache) {
                        mWorkorderCache.remove(mfgnum);
                    }
                }
            }
        });


        LiveData<List<String>> mChangedItmrefs = getArticleDao().getAllChanges(1000);
        mChangedItmrefs.observeForever(new Observer<List<String>>() {
            @Override
            public void onChanged(@Nullable List<String> s) {
                for(String itmref : s){
                    synchronized (mArticleCache) {
                        mArticleCache.remove(itmref);
                    }
                }
            }
        });

    }


    public List<WorkItem> getCachedItems(String mfgnum) {
        WorkorderCacheRecord r = getCachedRecord(mfgnum);
        return r.items;
    }
    public List<WorkMaterial> getCachedMaterials(String mfgnum) {
        WorkorderCacheRecord r = getCachedRecord(mfgnum);
        return r.materials;
    }

    private WorkorderCacheRecord getCachedRecord(String mfgnum){

        WorkorderCacheRecord r = null;
        synchronized (mWorkorderCache) {
            r = mWorkorderCache.get(mfgnum);

        }

        if(r == null || (System.currentTimeMillis() - r.LastUpdate > 5*60*1000)){
            r = new WorkorderCacheRecord();
            r.items = getWorkItemDao().getAll(mfgnum);
            r.materials = getWorkMaterialDao().getAll(mfgnum);
            r.LastUpdate = System.currentTimeMillis();

            synchronized (mWorkorderCache) {
                if(mWorkorderCache.size() >= CACHE_SIZE){
                    String oldest = null;
                    long old = System.currentTimeMillis();
                    for(String k : mWorkorderCache.keySet()){
                        WorkorderCacheRecord rec = mWorkorderCache.get(k);
                        if(rec.LastUpdate < old){
                            old = rec.LastUpdate;
                            oldest = k;
                        }
                    }
                    mWorkorderCache.remove(oldest);
                }
                mWorkorderCache.put(mfgnum, r);
            }
        }

        return r;


    }


    public Article getCachedArticle(String itmref){

        ArticleCacheRecord r = null;
        synchronized (mArticleCache) {
            r = mArticleCache.get(itmref);

        }

        if(r == null || (System.currentTimeMillis() - r.LastUpdate > 5*60*1000)){
            r = new ArticleCacheRecord();
            r.Item = getArticleDao().get(itmref);
            r.LastUpdate = System.currentTimeMillis();

            synchronized (mArticleCache) {
                if(mArticleCache.size() >= CACHE_SIZE){
                    String oldest = null;
                    long old = System.currentTimeMillis();
                    for(String k : mArticleCache.keySet()){
                        ArticleCacheRecord rec = mArticleCache.get(k);
                        if(rec.LastUpdate < old){
                            old = rec.LastUpdate;
                            oldest = k;
                        }
                    }
                    mArticleCache.remove(oldest);
                }
                mArticleCache.put(itmref, r);
            }
        }

        return r.Item;


    }


    public void exportDB() {
        // TODO Auto-generated method stub




        try {
            File sd = Environment.getExternalStorageDirectory();
            File data = Environment.getDataDirectory();

            if (sd.canWrite()) {
                String  currentDBPath= this.getOpenHelper().getWritableDatabase().getPath();
                String backupDBPath  = "/backups/db.sqlite";
                File currentDB = new File(currentDBPath);
                File backupDB = new File(sd, backupDBPath);

                FileChannel src = new FileInputStream(currentDB).getChannel();
                FileChannel dst = new FileOutputStream(backupDB).getChannel();
                dst.transferFrom(src, 0, src.size());
                src.close();
                dst.close();

            }
        } catch (Exception e) {

            e.printStackTrace();

        }
    }


}