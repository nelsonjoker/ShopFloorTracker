package com.joker.shopfloortracker;

import android.app.Activity;
import android.support.design.widget.CollapsingToolbarLayout;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.joker.shopfloortracker.model.AlertMessageContent;
import com.joker.shopfloortracker.model.AlertMessageType;
import com.joker.shopfloortracker.model.AppDatabase;
import com.joker.shopfloortracker.model.Article;
import com.joker.shopfloortracker.model.Job;
import com.joker.shopfloortracker.model.Plan;
import com.joker.shopfloortracker.model.WorkMaterial;
import com.joker.shopfloortracker.model.Workcenter;
import com.joker.shopfloortracker.model.Workstation;
import com.joker.shopfloortracker.model.view.ArticleAdapter;
import com.joker.shopfloortracker.model.view.WorkcenterAdapter;
import com.joker.shopfloortracker.model.view.WorkstationAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment representing a single AlertMessage detail screen.
 * This fragment is either contained in a {@link AlertMessageListActivity}
 * in two-pane mode (on tablets) or a {@link AlertMessageDetailActivity}
 * on handsets.
 */
public class AlertMessageDetailFragment extends Fragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_TYPE = "alert_type";
    public static final String ARG_JOB = "job";
    public static final String ARG_PLANS = "plans";

    /**
     * The dummy content this fragment is presenting.
     */
    private AlertMessageType mItem;

    private Job mJob;
    private ArrayList<Plan> mPlans;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public AlertMessageDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle arg = getArguments();
        mItem = null;
        if (arg.containsKey(ARG_TYPE)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            mItem = arg.getParcelable(ARG_TYPE); // AlertMessageContent.ITEM_MAP.get(arg.getInt(ARG_ITEM_ID));

            Activity activity = this.getActivity();
            CollapsingToolbarLayout appBarLayout = (CollapsingToolbarLayout) activity.findViewById(R.id.toolbar_layout);
            if (appBarLayout != null) {
                appBarLayout.setTitle(mItem.getDescription());
            }
        }
        mJob = null;
        mPlans = null;

        if(arg.containsKey(ARG_JOB))
            mJob = arg.getParcelable(ARG_JOB);
        if(arg.containsKey(ARG_PLANS))
            mPlans = arg.getParcelableArrayList(ARG_PLANS);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        switch((int)mItem.getId()){

            case AlertMessageType.ALERT_MESSAGE_TYPE_BOM:
                return onCreateBomView(inflater, container, savedInstanceState);
            case AlertMessageType.ALERT_MESSAGE_TYPE_OPERATION:
                return onCreateOpeView(inflater, container, savedInstanceState);
            case AlertMessageType.ALERT_MESSAGE_TYPE_APPLICATION:
                return onCreateApplicationView(inflater, container, savedInstanceState);
            default:
                View rootView = inflater.inflate(R.layout.alertmessage_detail, container, false);
                if (mItem != null) {
                    ((TextView) rootView.findViewById(R.id.alertmessage_detail)).setText(mItem.getDescription());
                }
                return rootView;
        }

    }


    protected View onCreateBomView(LayoutInflater inflater, ViewGroup container,
                                Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.alertmessage_detail_bom, container, false);
        View linearLayoutForm = rootView.findViewById(R.id.linearLayoutForm);
        View linearLayoutWarning = rootView.findViewById(R.id.linearLayoutWarning);


        linearLayoutWarning.setVisibility(View.GONE);
        linearLayoutForm.setVisibility(View.GONE);

        if(mPlans != null && mPlans.size() > 0){

            List<Article> items;
            List<String> itmrefs = new ArrayList<>();
            for(Plan p : mPlans){
                if(!itmrefs.contains(p.getItmref())){
                    itmrefs.add(p.getItmref());
                }
            }

            Article.ArticleDAO aDao = AppDatabase.getInstance(getContext()).getArticleDao();
            items = aDao.get(itmrefs.toArray(new String[itmrefs.size()]));

            Spinner itm = rootView.findViewById(R.id.spinnerItmref);
            ArticleAdapter adapter = new ArticleAdapter(getContext(), items);
            itm.setAdapter(adapter);


            WorkMaterial.WorkMaterialDAO wmDao = AppDatabase.getInstance(getContext()).getWorkMaterialDao();
            items = null;
            itmrefs.clear();
            List<String> mfgnums = new ArrayList<>(mPlans.size());
            for(Plan p : mPlans){
                if(!mfgnums.contains(p.getMfgnum()))
                    mfgnums.add(p.getMfgnum());
            }

            List<WorkMaterial> mats = wmDao.getAll(mfgnums.toArray(new String[mfgnums.size()]));
            for(WorkMaterial m : mats){
                if(!itmrefs.contains(m.getItmref())){
                    itmrefs.add(m.getItmref());
                }
            }
            items = aDao.get(itmrefs.toArray(new String[itmrefs.size()]));

            itm = rootView.findViewById(R.id.spinnerComponent);
            adapter = new ArticleAdapter(getContext(), items);
            itm.setAdapter(adapter);

            Article mat = (Article)itm.getSelectedItem();

            TextView stu = rootView.findViewById(R.id.textViewSTU);
            stu.setText(mat != null ? mat.getStu() : "UN");

            itm.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                    Article mat = (Article)adapterView.getSelectedItem();
                    stu.setText(mat != null ? mat.getStu() : "UN");

                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

            linearLayoutWarning.setVisibility(View.GONE);
            linearLayoutForm.setVisibility(View.VISIBLE);
        }else{
            linearLayoutWarning.setVisibility(View.VISIBLE);
            linearLayoutForm.setVisibility(View.GONE);
        }


        // Show the dummy content as text in a TextView.
        if (mItem != null) {
            //((TextView) rootView.findViewById(R.id.alertmessage_detail)).setText(mItem.Description);
        }
        //spinnerItmref
        //editTextQty
        //textViewSTU
        //spinnerComponent





        return  rootView;
    }


    protected View onCreateOpeView(LayoutInflater inflater, ViewGroup container,
                                   Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.alertmessage_detail_ope, container, false);
        View linearLayoutForm = rootView.findViewById(R.id.linearLayoutForm);
        View linearLayoutWarning = rootView.findViewById(R.id.linearLayoutWarning);


        linearLayoutWarning.setVisibility(View.GONE);
        linearLayoutForm.setVisibility(View.GONE);

        if(mPlans != null && mPlans.size() > 0){

            List<Article> items;
            List<String> itmrefs = new ArrayList<>();
            for(Plan p : mPlans){
                if(!itmrefs.contains(p.getItmref())){
                    itmrefs.add(p.getItmref());
                }
            }

            Article.ArticleDAO aDao = AppDatabase.getInstance(getContext()).getArticleDao();
            items = aDao.get(itmrefs.toArray(new String[itmrefs.size()]));

            TextView textViewOperationNumber = rootView.findViewById(R.id.textViewOperationNumber);
            textViewOperationNumber.setText(getString(R.string.operation_n_for_article_f, mPlans.get(0).getOpenum()));

            Spinner itm = rootView.findViewById(R.id.spinnerItmref);
            ArticleAdapter adapter = new ArticleAdapter(getContext(), items);
            itm.setAdapter(adapter);




            Spinner workcenterSpinner =  rootView.findViewById(R.id.spinnerWcr);
            Workcenter.WorkcenterDAO wcrDAO = AppDatabase.getInstance(getContext()).getWorkcenterDao();
            List<Workcenter> workcenters = wcrDAO.getAll();
            WorkcenterAdapter wcrAdapter = new WorkcenterAdapter(getContext(), workcenters);
            workcenterSpinner.setAdapter(wcrAdapter);

            Workstation.WorkstationDAO wstDAO = AppDatabase.getInstance(getContext()).getWorkstationDao();
            List<Workstation> workstations = wstDAO.getAll();

            Spinner workstationSpinner =  rootView.findViewById(R.id.spinnerWst);

            workcenterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                    Workcenter wcr = workcenters.get(position);
                    List<Workstation> filter = new ArrayList<>();
                    for(Workstation wst : workstations){
                        if(wst.getWcr().equals(wcr.getCode())){
                            filter.add(wst);
                        }
                    }

                    WorkstationAdapter wstAdapter = new WorkstationAdapter(getContext(), filter);
                    workstationSpinner.setAdapter(wstAdapter);

                    Plan opePlan = (Plan) workcenterSpinner.getTag();
                    if(opePlan != null){

                        int wstIndex = -1;
                        for(int w = 0; w < filter.size(); w++){
                            Workstation wst = filter.get(w);
                            if(opePlan.getWst().equals(wst.getCode())){
                                wstIndex = w;
                                break;
                            }
                        }
                        if(wstIndex >= 0) {
                            workstationSpinner.setSelection(wstIndex);
                        }

                    }


                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });


            itm.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                    Article a = (Article) adapterView.getItemAtPosition(position);
                    Plan opePlan = mPlans.get(0);
                    for(Plan p : mPlans){
                        if(a.getItmref().equals(p.getItmref())){
                            opePlan = p;
                            break;
                        }
                    }
                    textViewOperationNumber.setText(getString(R.string.operation_n_for_article_f, opePlan.getOpenum()));


                    int wcrIndex = -1;
                    for(int w = 0; w < workcenters.size(); w++){
                        Workcenter wcr = workcenters.get(w);
                        if(opePlan.getWcr().equals(wcr.getCode())){
                            wcrIndex = w;
                            break;
                        }
                    }
                    if(wcrIndex >= 0) {
                        workcenterSpinner.setTag(opePlan);//attach the selected plan to the spinner
                        workcenterSpinner.setSelection(wcrIndex);
                    }

                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                    textViewOperationNumber.setText(getString(R.string.operation_n_for_article_f, 0));
                }
            });


            linearLayoutWarning.setVisibility(View.GONE);
            linearLayoutForm.setVisibility(View.VISIBLE);
        }else{
            linearLayoutWarning.setVisibility(View.VISIBLE);
            linearLayoutForm.setVisibility(View.GONE);
        }


        // Show the dummy content as text in a TextView.
        if (mItem != null) {
            //((TextView) rootView.findViewById(R.id.alertmessage_detail)).setText(mItem.Description);
        }
        //spinnerItmref
        //editTextQty
        //textViewSTU
        //spinnerComponent





        return  rootView;
    }


    protected View onCreateApplicationView(LayoutInflater inflater, ViewGroup container,
                                   Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.alertmessage_detail_application, container, false);


        return  rootView;

    }

    public AlertMessageContent getMessage(){
        switch((int)mItem.getId()){

            case AlertMessageType.ALERT_MESSAGE_TYPE_BOM:
                return getMessageBOM(mItem);
            case AlertMessageType.ALERT_MESSAGE_TYPE_OPERATION:
                return getMessageOPE(mItem);
            case AlertMessageType.ALERT_MESSAGE_TYPE_APPLICATION:
                return getMessageApplication(mItem);
            default:
                return null;
        }
    }

    private AlertMessageContent getMessageBOM(AlertMessageType type) {

        View rootView = getView();

        Spinner spinnerItmref = rootView.findViewById(R.id.spinnerItmref);
        Spinner spinnerComponent = rootView.findViewById(R.id.spinnerComponent);
        EditText editTextQty = rootView.findViewById(R.id.editTextQty);
        EditText editTextComment = rootView.findViewById(R.id.editTextComment);

        Article itm = (Article) spinnerItmref.getSelectedItem();
        Article component = (Article) spinnerComponent.getSelectedItem();
        double qty = editTextQty.getText().length()  > 0 ? Double.parseDouble(editTextQty.getText().toString()) : 0;

        AlertMessageContent message = AlertMessageContent.createMessageBOM(type, mJob, itm, component, qty);
        message.setComment(editTextComment.getText().toString());

        return message;

    }


    private AlertMessageContent getMessageOPE(AlertMessageType type) {

        View rootView = getView();

        TextView textViewOperationNumber = rootView.findViewById(R.id.textViewOperationNumber);
        Spinner itm = rootView.findViewById(R.id.spinnerItmref);
        Spinner workcenterSpinner =  rootView.findViewById(R.id.spinnerWcr);
        Spinner workstationSpinner =  rootView.findViewById(R.id.spinnerWst);
        EditText editTextComment = rootView.findViewById(R.id.editTextComment);

        //Article a = (Article) itm.getAdapter().getItem(itm.getSelectedItemPosition());
        Article a = (Article) itm.getSelectedItem();
        Plan opePlan = mPlans.get(0);
        for(Plan p : mPlans){
            if(a.getItmref().equals(p.getItmref())){
                opePlan = p;
                break;
            }
        }

        Workcenter targetWCR = (Workcenter) workcenterSpinner.getSelectedItem();
        Workstation targetWST = (Workstation) workstationSpinner.getSelectedItem();

        AlertMessageContent message = AlertMessageContent.createMessageOPE(type, mJob, opePlan, a, targetWCR, targetWST);
        message.setComment(editTextComment.getText().toString());

        return message;

    }


    private AlertMessageContent getMessageApplication(AlertMessageType type) {

        View rootView = getView();

        EditText editTextComment = rootView.findViewById(R.id.editTextComment);

        AlertMessageContent message = AlertMessageContent.createMessageApplication(type, mJob);
        message.setComment(editTextComment.getText().toString());
        return message;

    }

}
