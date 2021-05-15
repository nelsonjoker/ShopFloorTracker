package com.joker.shopfloortracker.jobpage;

import android.os.Bundle;

import com.joker.shopfloortracker.model.Job;

/**
 * Created by Nelson on 03/04/2018.
 */

public class JobPageFactory {




    private static final JobPageFactory ourInstance = new JobPageFactory();

    public static JobPageFactory getInstance() {
        return ourInstance;
    }

    private JobPageFactory() {
    }


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param j data for the workorder filtering
     * @return A new instance of fragment JobPageFragment.
     */
    public AbstractJobPage newPage(Job j) {
        AbstractJobPage fragment = null;

        if("PIN".equals(j.getWcr()) && "LINPINT".equals(j.getWst())){
            fragment = new JobPageFragmentPaint();
        }else{
            fragment = new JobPageFragment();
        }

        fragment.setJob(j);
        Bundle args = new Bundle();
        args.putParcelable(AbstractJobPage.ARG_JOB, j);
        fragment.setArguments(args);

        return fragment;
    }
}
