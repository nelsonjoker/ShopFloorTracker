package com.joker.shopfloortracker.model;

import android.os.Parcel;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class UpcomingPlanGroup extends UpcomingPlan {

    private double mUnFilteredQty;
    public double getUnfilteredQty() { return mUnFilteredQty; }
    public void setUnfilteredQty(double q) { mUnFilteredQty = q; }

    private List<String> mPlanIDs;
    private List<UpcomingPlan> mPlans;

    public List<UpcomingPlan> getPlans() { return mPlans; }

    public UpcomingPlanGroup(){
        super();
    }

    public UpcomingPlanGroup(UpcomingPlan p){

        Parcel parcel = Parcel.obtain();
        p.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        parse(parcel);
        parcel.recycle();
        reset();

        mPlanIDs = new ArrayList<>(10);
        mPlans = new ArrayList<>(10);
    }

    public void add(UpcomingPlan p){
        if(p.isAssigned()) {
            setAssigned(true);
            setAssignmentCode(p.getAssignmentCode());
            setAssignmentGroup(p.getAssignmentGroup());
        }
        if(!p.isCompleted())
            setCompleted(false);

        setAssignedQty(getAssignedQty() + p.getAssignedQty());
        setCompletion(getCompletion() + p.getCompletion());
        setCplqty(getCplqty() + p.getCplqty());
        setExtqty(getExtqty() + p.getExtqty());
        setRemainingQty(getRemainingQty() + p.getRemainingQty());
        setItmqty(getItmqty() + p.getItmqty());

        mPlanIDs.add(p.getId());
        mPlans.add(p);

    }

    protected void reset(){
        setAssigned(false);
        setAssignedQty(0);
        setAssignmentCode("");
        setAssignmentGroup("");
        setCompleted(true); //<- must be reset if any plan is incomplete
        setCompletion(0);
        setCplqty(0);
        setExtqty(0);
        setItmqty(0);
        //ex.setId("");
        setRemainingQty(0);
    }

    public void updateTotals(){
        List<UpcomingPlan> prev = new ArrayList<>(mPlans);
        mPlans.clear();
        mPlanIDs.clear();
        reset();
        for(UpcomingPlan p : prev){
            add(p);
        }
    }


}
