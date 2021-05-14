package pt.sotubo.shopfloortracker.model.view;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;

import java.util.List;

import pt.sotubo.shopfloortracker.model.AppDatabase;
import pt.sotubo.shopfloortracker.model.Plan;
import pt.sotubo.shopfloortracker.model.UpcomingPlan;

/**
 * Created by Nelson on 15/12/2017.
 */

public class PlanViewModel extends AndroidViewModel {

    private LiveData<List<UpcomingPlan>> mPlanList = null;


    public LiveData<List<UpcomingPlan>> getRunningPlanList(String wcr, String wst, String assignmentCode, int count) {
        return getRunningPlanList(wcr,wst,assignmentCode,count,"");
    }

    public LiveData<List<UpcomingPlan>> getRunningPlanList(String wcr, String wst, String assignmentCode, int count, String itmFilter) {

        if(itmFilter == null || "".equals(itmFilter))
            itmFilter = "";
        else
            itmFilter = "%"+itmFilter+"%";

        //if(mPlanList == null)
        {
            mPlanList = appDatabase.getUpcomingPlanDao().getUpcoming(wcr, wst, assignmentCode, count, itmFilter);
        }
        return mPlanList;


    }

    public LiveData<List<UpcomingPlan>> getRunningPlanList(String wcr, String wst, String assignmentCode, int count, String[] materials) {
        return getRunningPlanList(wcr,wst,assignmentCode,count,"",materials);
    }

    public LiveData<List<UpcomingPlan>> getRunningPlanList(String wcr, String wst, String assignmentCode, int count, String itmFilter, String[] materials) {

        if(itmFilter == null || "".equals(itmFilter))
            itmFilter = "";
        else
            itmFilter = "%"+itmFilter+"%";

        //if(mPlanList == null)
        {
            mPlanList = appDatabase.getUpcomingPlanDao().getUpcoming(wcr, wst, assignmentCode, count, itmFilter, materials);
        }
        return mPlanList;


    }
/*
    private Map<Plan, Workorder> mWorkordersMap;
    public Workorder getWorkorder(Plan p) {



        if(mWorkordersMap.containsKey(p)) {
            return mWorkordersMap.get(p);
        }

        return null;

    }
*/
    private AppDatabase appDatabase;

    public PlanViewModel(Application application) {
        super(application);


        appDatabase = AppDatabase.getInstance(this.getApplication());


//        mWorkordersMap = new HashMap<>();
//        Workorder.WorkorderDAO woDao = appDatabase.getWorkorderDao();
//        WorkItem.WorkItemDAO wiDao = appDatabase.getWorkItemDao();
//        WorkMaterial.WorkMaterialDAO wmDao = appDatabase.getWorkMaterialDao();
//        Operation.OperationDAO opDao = appDatabase.getOperationDao();
//
//        mPlanList.observeForever(new Observer<List<Plan>>() {
//            @Override
//            public void onChanged(@Nullable List<Plan> plans) {
//
//
//                Set<String> setMfgnums = new HashSet<>();
//
//                for(Plan p : plans) {
//                    if(mWorkordersMap.containsKey(p)) {
//                        continue;
//                    }
//                    setMfgnums.add(p.getMfgnum());
//                    /*
//                    Workorder w = woDao.get(p.getMfgnum());
//                    if (w != null) {
//                        List<WorkItem> items = wiDao.getAll(w.getMfgnum());
//                        List<WorkMaterial> mats = wmDao.getAll(w.getMfgnum());
//                        List<Operation> ops = opDao.getAll(w.getMfgnum());
//
//                        w.setItems(items);
//                        w.setMaterials(mats);
//                        w.setOperations(ops);
//
//                        mWorkordersMap.put(p, w);
//                    }
//                    */
//                }
//                List<String> sMfgnums = new ArrayList<>(setMfgnums);
//                setMfgnums.clear();
//                setMfgnums = null;
//                int batch_limit = 500;
//
//                List<Workorder> allWos = new ArrayList<>();
//                List<WorkItem> allItems = new ArrayList<>();
//                List<WorkMaterial> allMaterials = new ArrayList<>();
//                List<Operation> allOperations = new ArrayList<>();
//                int size = sMfgnums.size();
//                for(int i = 0; i < size; i+=batch_limit){
//                    List<String> mfgnums = sMfgnums.subList(i, Math.min(sMfgnums.size()-1, i+batch_limit));
//                    allWos.addAll(woDao.getAll(mfgnums.toArray(new String[mfgnums.size()])));
//                    allItems.addAll(wiDao.getAll(mfgnums.toArray(new String[mfgnums.size()])));
//                    allMaterials.addAll(wmDao.getAll(mfgnums.toArray(new String[mfgnums.size()])));
//                    allOperations.addAll(opDao.getAll(mfgnums.toArray(new String[mfgnums.size()])));
//                    System.gc();
//                }
//
//                Map<String, Workorder> allWosMap = new HashMap<String, Workorder>();
//                Map<String, List<WorkItem>> allItemsMap= new HashMap<>();
//                Map<String, List<WorkMaterial>> allMaterialsMap= new HashMap<>();
//                Map<String, List<Operation>> allOperationsMap= new HashMap<>();
//                for(Workorder wo  : allWos){
//                    allWosMap.put(wo.getMfgnum(), wo);
//                }
//                allWos.clear();
//                allWos = null;
//                for(WorkItem wi  : allItems){
//                    List<WorkItem> l = allItemsMap.get(wi.getMfgnum());
//                    if(l == null){
//                        l = new ArrayList<>();
//                        allItemsMap.put(wi.getMfgnum(), l);
//                    }
//                    l.add(wi);
//                }
//                allItems.clear();
//                allItems = null;
//
//                for(WorkMaterial wm  : allMaterials){
//                    List<WorkMaterial> l = allMaterialsMap.get(wm.getMfgnum());
//                    if(l == null){
//                        l = new ArrayList<>();
//                        allMaterialsMap.put(wm.getMfgnum(), l);
//                    }
//                    l.add(wm);
//                }
//                allMaterials.clear();
//                allMaterials = null;
//
//                for(Operation op  : allOperations){
//                    List<Operation> l = allOperationsMap.get(op.getMfgnum());
//                    if(l == null){
//                        l = new ArrayList<>();
//                        allOperationsMap.put(op.getMfgnum(), l);
//                    }
//                    l.add(op);
//                }
//                allOperations.clear();
//                allOperations = null;
//
//                for(Plan p : plans) {
//                    Workorder w = allWosMap.get(p.getMfgnum());
//                    if(w == null)
//                        continue;
//                    List<WorkItem> items = allItemsMap.get(p.getMfgnum());
//                    List<WorkMaterial> mats = allMaterialsMap.get(p.getMfgnum());
//                    List<Operation> ops = allOperationsMap.get(p.getMfgnum());
//
//                    if(items != null)
//                        w.setItems(items);
//                    if(mats != null)
//                        w.setMaterials(mats);
//                    if(ops != null)
//                        w.setOperations(ops);
//
//                    mWorkordersMap.put(p, w);
//
//
//                }
///*
//                Set<Plan> keys = mWorkordersMap.keySet();
//                for(Plan p : keys){
//                    if(!plans.contains(p)){
//                        mWorkordersMap.remove(p);
//                    }
//                }
//*/
//
//
//
//
//
//            }
//        });

    }

    public void updatePlan(Plan p) {
        new UpdateAsyncTask(appDatabase).execute(p);
    }

    private static class UpdateAsyncTask extends AsyncTask<Plan, Void, Void> {

        private AppDatabase db;

        public UpdateAsyncTask(AppDatabase appDatabase) {
            db = appDatabase;
        }

        @Override
        protected Void doInBackground(final Plan... params) {
            db.getPlanDao().updateAll(params);
            return null;
        }

    }

}