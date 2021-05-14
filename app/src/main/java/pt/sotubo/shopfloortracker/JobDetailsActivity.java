package pt.sotubo.shopfloortracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import pt.sotubo.shopfloortracker.model.AppDatabase;
import pt.sotubo.shopfloortracker.model.Job;
import pt.sotubo.shopfloortracker.model.Workcenter;
import pt.sotubo.shopfloortracker.model.Workstation;

public class JobDetailsActivity extends AppCompatActivity {

    private List<Workcenter> mWorkcenters;
    private List<Workstation> mWorkstations;
    private Job mJob;

    public CharSequence getName(){
        return ((TextView) findViewById(R.id.separator_name)).getText();
    }
    public CharSequence getOperatorCode(){
        return ((TextView) findViewById(R.id.operator_code)).getText();
    }
    public Workcenter getWorkcenter(){
        Spinner s = (Spinner)findViewById(R.id.workcenter);
        if(s.getSelectedItemPosition() >= 0)
            return mWorkcenters.get(s.getSelectedItemPosition());
        return null;
    }
    public Workstation getWorkstation(){
        Spinner s = (Spinner)findViewById(R.id.workstation);
        if(s.getSelectedItemPosition() >= 0)
            return mWorkstations.get(s.getSelectedItemPosition());
        return null;
    }
    public int getWorkstationIndex(){
        CharSequence txt = ((TextView) findViewById(R.id.workstation_index)).getText();
        int i = -1;
        if(txt != null && txt.length() > 0) {
            i = Integer.parseInt(txt.toString());
        }
        return i;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_details);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
        ((TextView)findViewById(R.id.separator_name)).setText(df.format(c.getTime()));

        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            mJob = extras.getParcelable("job");
            setTitle(R.string.title_activity_edit_job);
        }else{
            setTitle(R.string.title_activity_new_job);
        }


        Workcenter.WorkcenterDAO workcenterDAO = AppDatabase.getInstance(getApplicationContext())
                .getWorkcenterDao();
        mWorkcenters = workcenterDAO.getAll();
        String[] entries = new String[mWorkcenters.size()];

        for(int i = 0; i < mWorkcenters.size(); i++){
            Workcenter w = mWorkcenters.get(i);
            entries[i] = w.getCode()+" - "+w.getDescription().trim();
        }

        Spinner workcenterSpinner = (Spinner) findViewById(R.id.workcenter);

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, entries);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        workcenterSpinner.setAdapter(spinnerArrayAdapter);

        if(entries.length > 0)
            workcenterSpinner.setSelection(0);

        bindWorkstations( mWorkcenters.size() > 0 ? mWorkcenters.get(0) : null, mJob != null ?  mJob.getWst() : null );

        workcenterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Workcenter w = mWorkcenters.get(i);
                bindWorkstations(w, mJob != null ?  mJob.getWst() : null);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                bindWorkstations(null, mJob != null ?  mJob.getWst() : null);
            }
        });


        Button btnOk = (Button) findViewById(R.id.btn_save_job);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(validate()){

                    if(mJob == null){
                        mJob = new Job();
                    }
                    mJob.setName(getName().toString());
                    mJob.setOperator(getOperatorCode().toString());
                    mJob.setWcr(getWorkcenter().getCode());
                    mJob.setWst(getWorkstation().getCode());
                    mJob.setIndex(getWorkstationIndex());

                    Intent data = new Intent();
                    data.putExtra("job", (Parcelable) mJob);
                    setResult(RESULT_OK,data);
                    finish();
                }
            }
        });


        if(mJob != null){
            ((TextView)findViewById(R.id.separator_name)).setText(mJob.getName());
            ((TextView)findViewById(R.id.operator_code)).setText(mJob.getOperator());
            int i;
            for(i = 0; i < mWorkcenters.size(); i++){
                Workcenter w = mWorkcenters.get(i);
                if(w.getCode().equals(mJob.getWcr())){
                    break;
                }
            }
            if(i < mWorkcenters.size() && mWorkstations != null){
                workcenterSpinner.setSelection(i);
                bindWorkstations(mWorkcenters.get(i), mJob.getWst());
                //mWorkstations now has the elements on the drop down

            }
            ((TextView)findViewById(R.id.workstation_index)).setText(mJob.getIndex()+"");
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

    private boolean validate(){
        CharSequence v = null;
        try {
            v = getName();
            if (v == null || v.length() <= 0) {
                throw new Exception("É necessário indicar o nome do separador");
            }
            v = getOperatorCode();
            if (v == null || v.length() <= 0) {
                throw new Exception("É necessário indicar o código de operador");
            }
            Workcenter w = getWorkcenter();
            if (w == null)
                throw new Exception("É necessário seleccionar o centro de produção");
            Workstation s = getWorkstation();
            if (s == null)
                throw new Exception("É necessário seleccionar o posto de produção");

            if (getWorkstationIndex() < 0)
                throw new Exception("É necessário seleccionar o ID do posto");

        }catch(Exception e){

            Snackbar.make(findViewById(R.id.layout_job_details), e.getMessage(), Snackbar.LENGTH_LONG)
                .show();

            return false;
        }

        return true;
    }


    private void bindWorkstations(Workcenter wcr, String wstCode){
        mWorkstations = null;
        Spinner workstationSpinner = (Spinner) findViewById(R.id.workstation);
        String[] entries = {};
        if(wcr == null){
            ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(
                    this, android.R.layout.simple_spinner_item, entries);
            spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            workstationSpinner.setAdapter(spinnerArrayAdapter);
            return;
        }

        Workstation.WorkstationDAO workstationDAO = AppDatabase.getInstance(getApplicationContext())
                .getWorkstationDao();

        mWorkstations = workstationDAO.getAll(wcr.getCode());

        entries = new String[mWorkstations.size()];
        int selectedIndex = 0;
        for(int i = 0; i < mWorkstations.size(); i++){
            Workstation w = mWorkstations.get(i);
            entries[i] = w.getCode()+" - "+w.getDescription().trim();
            if(wstCode != null && w.getCode().equals(wstCode)){
                selectedIndex = i;
            }
        }


        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, entries);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        workstationSpinner.setAdapter(spinnerArrayAdapter);

        if(entries.length > 0)
            workstationSpinner.setSelection(selectedIndex);

    }

}
