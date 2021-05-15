package com.joker.shopfloortracker.model;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class AlertMessageContent implements Parcelable {







    private AlertMessageType mType;
    private Bundle mData;
    private String mComment;

    public AlertMessageType getAlertType(){
        return mType;
    }

    public Bundle getPayload(){
        return mData;
    }

    public String getComment(){ return mComment; }
    public void setComment(String v ) { mComment = v; }

    private AlertMessageContent(AlertMessageType type, Job j){
        mType = type;
        mComment = null;
        mData = new Bundle();
        if(j != null) {
            mData.putString("wcr", j.getWcr());
            mData.putString("wst", j.getWst());
            mData.putString("operator", j.getOperator());
            mData.putString("assignment_code", j.getAssignmentCode());
        }else{
            mData.putString("wcr", "");
            mData.putString("wst", "");
            mData.putString("operator", "");
            mData.putString("assignment_code", "");
        }
        mData.putLong("time", System.currentTimeMillis());
    }

    private AlertMessageContent(Parcel parcel){
        mType = new AlertMessageType(parcel);
        mComment = parcel.readString();
        mData =  parcel.readBundle();
    }




    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {

        mType.writeToParcel(parcel, flags);
        parcel.writeString(mComment);
        parcel.writeBundle(mData);
    }

    public static final Parcelable.Creator<AlertMessageContent>
            CREATOR = new Parcelable.Creator<AlertMessageContent>() {

        public AlertMessageContent createFromParcel(Parcel in) {
            return new AlertMessageContent(in);
        }

        public AlertMessageContent[] newArray(int size) {
            return new AlertMessageContent[size];
        }
    };


    public static AlertMessageContent createMessageBOM(AlertMessageType type, Job j, Article item, Article component, double qty){



        AlertMessageContent msg = new AlertMessageContent(type,j);
        if(item != null) {
            msg.mData.putString("itmref", item.getItmref());
            msg.mData.putString("itmdes", item.getItmdes());
        }
        if(component != null) {
            msg.mData.putString("cpnitmref", component.getItmref());
            msg.mData.putString("cpnitmdes", component.getItmdes());
        }
        msg.mData.putDouble("qty", qty);


        return msg;
    }
    public static AlertMessageContent createMessageOPE(AlertMessageType type, Job j, Plan opePlan, Article a, Workcenter targetWCR, Workstation targetWST) {

        AlertMessageContent msg = new AlertMessageContent(type,j);
        if(opePlan != null) {
            msg.mData.putInt("openum", opePlan.getOpenum());
        }
        if(a != null) {
            msg.mData.putString("itmref", a.getItmref());
            msg.mData.putString("itmdes", a.getItmdes());
        }

        if(targetWCR != null) {
            msg.mData.putString("wcr", targetWCR.getCode());
            msg.mData.putString("wcr_description", targetWCR.getDescription());
        }

        if(targetWST != null) {
            msg.mData.putString("wst", targetWST.getCode());
            msg.mData.putString("wst_description", targetWST.getDescription());
        }


        return msg;
    }

    public static AlertMessageContent createMessageApplication(AlertMessageType type, Job j) {
        AlertMessageContent msg = new AlertMessageContent(type,j);
        return msg;
    }
}
