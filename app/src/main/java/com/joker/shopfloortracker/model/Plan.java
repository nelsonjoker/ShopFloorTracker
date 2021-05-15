package com.joker.shopfloortracker.model;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.List;

/**
 * Created by Nelson on 07/12/2017.
 */

@Entity(tableName = "plan" ,
        indices = {
                @Index(value = {"mfgnum", "openum"}, name = "mfgnum_openum_idx"),
                @Index(value = {"nextmfgnum", "nextopenum", "itmref"}, name = "nextmfgnum_nextopenum_itmref_idx"),
                @Index(value = "start", name = "start_idx"),
                @Index(value = "nextmfgnum", name = "nextmfgnum_idx"),
                @Index(value = "nextid", name = "nextid_idx"),
                @Index(value = {"wcr", "wst"}, name = "wcr_wst_idx"),
                @Index(value = "assignment_code", name = "assignment_code_idx")})
public class Plan implements Parcelable{



    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;
    @ColumnInfo(name = "assigned")
    private boolean assigned;
    @ColumnInfo(name = "assignment_code")
    private String assignmentCode;
    @ColumnInfo(name = "assignment_group")
    private String assignmentGroup;
    @ColumnInfo(name = "completed")
    private boolean completed;
    @ColumnInfo(name = "planner_id")
    private String plannerId;
    @ColumnInfo(name = "operation")
    private String operation;
    @ColumnInfo(name = "openum")
    private int openum;
    @ColumnInfo(name = "mfgnum")
    private String mfgnum;
    @ColumnInfo(name = "mfglin")
    private int mfglin;
    @ColumnInfo(name = "wcr")
    private String wcr;
    @ColumnInfo(name = "wst")
    private String wst;
    @ColumnInfo(name = "workstation_number")
    private int workstationNumber;
    @ColumnInfo(name = "start")
    private long start;
    @ColumnInfo(name = "end")
    private long end;
    @ColumnInfo(name = "duration")
    private long duration;
    @ColumnInfo(name = "update")
    private long update;
    @ColumnInfo(name = "update_counter")
    private long updateCounter;

    @ColumnInfo(name = "itmref")
    private String itmref;
    @ColumnInfo(name = "itmdes")
    private String itmdes;
    @ColumnInfo(name = "itmqty")
    private double itmqty;
    @ColumnInfo(name = "itmstu")
    private String itmstu;
    @ColumnInfo(name = "vcrnumori")
    private String vcrnumori;
    @ColumnInfo(name = "vcrlinori")
    private int vcrlinori;
    @ColumnInfo(name = "extqty")
    private double extqty;
    @ColumnInfo(name = "cplqty")
    private double cplqty;
    @ColumnInfo(name = "assigned_qty")
    private double assignedQty;

    @ColumnInfo(name = "nextmfgnum")
    private String nextmfgnum;
    @ColumnInfo(name = "nextopenum")
    private int nextopenum;

    @ColumnInfo(name = "nextid")
    private String nextid;

    @ColumnInfo(name = "dirty")
    private boolean dirty;

    //completion is created for future use
    @ColumnInfo(name = "completion")
    private double completion;


    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public boolean isAssigned() {
        return assigned;
    }

    public void setAssigned(boolean assigned) {
        this.assigned = assigned;
    }

    public String getAssignmentCode() { return assignmentCode;}
    public void setAssignmentCode(String c) {
        this.assignmentCode = c;
    }
    public String getAssignmentGroup() { return assignmentGroup;}
    public void setAssignmentGroup(String c) {
        this.assignmentGroup = c;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getPlannerId() {
        return plannerId;
    }

    public void setPlannerId(String plannerId) {
        this.plannerId = plannerId;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public int getOpenum() {
        return openum;
    }

    public void setOpenum(int openum) {
        this.openum = openum;
    }

    public String getMfgnum() {
        return mfgnum;
    }

    public void setMfgnum(String mfgnum) {
        this.mfgnum = mfgnum;
    }

    public int getMfglin() {
        return mfglin;
    }
    public void setMfglin(int mfglin) {
        this.mfglin = mfglin;
    }


    public String getWcr() {
        return wcr;
    }

    public void setWcr(String wcr) {
        this.wcr = wcr;
    }

    public String getWst() {
        return wst;
    }

    public void setWst(String wst) {
        this.wst = wst;
    }

    public int getWorkstationNumber() {
        return workstationNumber;
    }

    public void setWorkstationNumber(int workstationNumber) {
        this.workstationNumber = workstationNumber;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getUpdate() {
        return update;
    }

    public void setUpdate(long u) {
        this.update = u;
    }

    public long getUpdateCounter() {
        return updateCounter;
    }
    public void setUpdateCounter(long update_counter) {
        this.updateCounter = update_counter;
    }


    public String getItmref() {
        return itmref;
    }
    public void setItmref(String v) {
        this.itmref = v;
    }

    public String getItmdes() {
        return itmdes;
    }
    public void setItmdes(String v) { this.itmdes = v; }

    public double getItmqty() {
        return itmqty;
    }
    public void setItmqty(double v) {
        this.itmqty = v;
    }

    public String getItmstu() {
        return itmstu;
    }
    public void setItmstu(String v) {
        this.itmstu = v;
    }

    public String getVcrnumori() {
        return vcrnumori;
    }
    public void setVcrnumori(String v) {
        this.vcrnumori = v;
    }

    public int getVcrlinori() {
        return vcrlinori;
    }
    public void setVcrlinori(int v) {
        this.vcrlinori = v;
    }

    public double getExtqty() { return extqty; }
    public void setExtqty(double v) { this.extqty = v; }
    public double getCplqty() { return cplqty; }
    public void setCplqty(double v) { this.cplqty = v; }

    public double getAssignedQty() { return assignedQty; }
    public void setAssignedQty(double v) { this.assignedQty = v; }


    public String getNextmfgnum() { return nextmfgnum; }
    public void setNextmfgnum(String v) { this.nextmfgnum = v; }
    public int getNextopenum() { return nextopenum; }
    public void setNextopenum(int v) { this.nextopenum = v; }
    public String getNextid() { return nextid; }
    public void setNextid(String id) { this.nextid = id; }


    public boolean isDirty() { return dirty ;}
    public void setDirty(boolean v) { this.dirty = v; }

    public double getCompletion() { return completion; }
    public void setCompletion(double c) { completion = c; }


    public Plan(){

        id = "";
        assigned = false;
        assignmentCode = "";
        assignmentGroup = "";
        completed = false;
        plannerId = "";
        operation = "";
        openum = 0;
        mfgnum = "";
        mfglin = 0;
        wcr = "";
        wst = "";
        workstationNumber = 0;
        start = System.currentTimeMillis();
        end = System.currentTimeMillis();
        duration = 0;
        update = 0;
        updateCounter = 0;
        itmref = null;
        itmdes = null;
        itmqty = 0;
        itmstu = null;
        vcrnumori = null;
        vcrlinori = 0;
        extqty = 0;
        cplqty = 0;
        assignedQty = 0;
        nextmfgnum = "";
        nextopenum = 0;
        nextid = "";
        dirty = false;
        completion = 0;

    }

    public Plan(Parcel p){
        this();
        parse(p);
    }

    public double assign(String code) throws Exception {
        return assign(code, extqty - cplqty - assignedQty);
    }

    public double assign(String code, double qty) throws Exception {
        if(assigned && !code.equals(assignmentCode))
            throw  new Exception("Already assigned to "+assignmentCode);
        if(qty <= 0)
            throw  new Exception("Invalid assignment qty "+qty);

        assignedQty += qty;
        assignmentCode = code;
        assigned = true;
        //start = System.currentTimeMillis();
        //end = start + duration;
        dirty = true;

        return qty;
    }

    public void deassing(){
        assignedQty = 0;
        assigned = false;
        assignmentCode = "";
        dirty = true;
    }


    protected  void parse(Parcel p){
        id = p.readString();
        assigned = p.readByte() == 0 ? false : true;
        assignmentCode = p.readString();
        assignmentGroup = p.readString();
        completed = p.readByte() == 0 ? false : true;;
        plannerId = p.readString();
        operation = p.readString();
        openum = p.readInt();
        mfgnum = p.readString();
        mfglin = p.readInt();
        wcr = p.readString();
        wst = p.readString();
        workstationNumber = p.readInt();
        start = p.readLong();
        start = p.readLong();
        duration = p.readLong();
        update = p.readLong();
        updateCounter = p.readLong();

        itmref = p.readString();
        itmdes = p.readString();
        itmqty = p.readDouble();
        itmstu = p.readString();
        vcrnumori = p.readString();
        vcrlinori = p.readInt();
        extqty = p.readDouble();
        cplqty = p.readDouble();
        assignedQty = p.readDouble();
        nextmfgnum = p.readString();
        nextopenum = p.readInt();
        nextid = p.readString();
        dirty = p.readByte() == 0 ? false : true;
        completion = p.readDouble();
    }


    @Override
    public boolean equals(Object obj) {
        return id.equals(((Plan)obj).id) ;
    }

    @Override
    public int hashCode() { return id.hashCode(); }

    public static final Parcelable.Creator<Plan>
            CREATOR = new Parcelable.Creator<Plan>() {

        public Plan createFromParcel(Parcel in) {
            return new Plan(in);
        }

        public Plan[] newArray(int size) {
            return new Plan[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(id);
        parcel.writeByte((byte)(assigned ? 1: 0));
        parcel.writeString(assignmentCode);
        parcel.writeString(assignmentGroup);
        parcel.writeByte((byte)(completed ? 1: 0));
        parcel.writeString(plannerId);
        parcel.writeString(operation );
        parcel.writeInt(openum);
        parcel.writeString(mfgnum );
        parcel.writeInt(mfglin);
        parcel.writeString(wcr);
        parcel.writeString(wst);
        parcel.writeInt(workstationNumber);
        parcel.writeLong(start);
        parcel.writeLong(end);
        parcel.writeLong(duration);
        parcel.writeLong(update);
        parcel.writeLong(updateCounter);

        parcel.writeString(itmref);
        parcel.writeString(itmdes);
        parcel.writeDouble(itmqty);
        parcel.writeString(itmstu);
        parcel.writeString(vcrnumori);
        parcel.writeInt(vcrlinori);
        parcel.writeDouble(extqty);
        parcel.writeDouble(cplqty);
        parcel.writeDouble(assignedQty);
        parcel.writeString(nextmfgnum);
        parcel.writeInt(nextopenum);
        parcel.writeString(nextid);
        parcel.writeByte((byte)(dirty ? 1: 0));
        parcel.writeDouble(completion);

    }

    public void copyTo(Plan ex) {

        if(ex == this)
            return; //active objects get set to null

        Parcel p = Parcel.obtain();
        writeToParcel(p, 0);
        p.setDataPosition(0);
        ex.parse(p);
        p.recycle();
    }




    @Dao
    public interface PlanDAO {

        @Insert
        long insert(Plan j);
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        long[] insertAll(Plan... j);

        @Update(onConflict = OnConflictStrategy.REPLACE)
        void updateAll(Plan... j);

        @Query("UPDATE `plan` set `update` = 0")
        void resetUpdateCounters();
        @Query("DELETE FROM `plan` WHERE `update` = 0")
        void deleteObsolete();

        @Delete
        void deleteAll(Plan... j);

        @Query("DELETE FROM `plan` WHERE `id` IN (:id)")
        void deleteAll(String... id);

        @Query("DELETE FROM `plan`")
        void truncate();

        @Query("SELECT * FROM `plan` WHERE \"id\" = :id ")
        Plan get(String id);
        @Query("SELECT * FROM `plan` WHERE \"id\" IN (:id) ")
        List<Plan> get(String... id);
        @Query("SELECT id FROM `plan` ")
        List<String> getAllIds();

        @Query("SELECT * FROM `plan` ORDER BY start ASC ")
        List<Plan> getAllDebug();

        @Query("SELECT * FROM `plan` ORDER BY start ASC ")
        LiveData<List<Plan>> getAll();

        /**
         * Get the qty of articles pending production that will consume
         * the article with code matitmref
         * @param matitmref
         * @return
         */
        @Query("SELECT SUM(extqty - cplqty) AS qty FROM `plan` p " +
                "WHERE p.wcr=:wcr AND p.wst=:wst AND p.completed = 0  " +
                "AND p.mfgnum IN (SELECT mfgnum FROM workmaterial wm WHERE wm.itmref = :matitmref) " +
                "AND p.`end` <= :maxEndTime " +
                "ORDER BY start ASC ")
        double countConsumers(String wcr, String wst, String matitmref, long maxEndTime);
        @Query("SELECT SUM(extqty - cplqty) AS qty FROM `plan` p " +
                "WHERE p.wcr=:wcr AND p.wst=:wst AND p.completed = 0 " +
                "AND p.assignment_group = :assignment_group " +
                "ORDER BY start ASC ")
        double countConsumers(String wcr, String wst, String assignment_group);
/*
        //@Query("SELECT * FROM \"plan\" WHERE \"wcr\"=:wcr AND \"wst\"=:wst AND completed = 0 ORDER BY start ASC LIMIT :limit")
        @Query("SELECT p.*, prev.mincpl  FROM `plan` AS p " +
                "LEFT JOIN (" +
                "SELECT nextmfgnum, nextopenum, MIN(cplqty/extqty) as mincpl " +
                "FROM `plan` " +
                "GROUP BY nextmfgnum, nextopenum) AS prev ON prev.nextmfgnum = p.mfgnum AND prev.nextopenum = p.openum " +
                "WHERE p.wcr=:wcr AND p.wst=:wst AND completed = 0 AND (prev.mincpl is null OR prev.mincpl > 0) " +
                "AND (p.assigned = 0 OR p.assignment_code = :assignmentCode) " +
                "ORDER BY assigned DESC, start ASC LIMIT :limit")
        LiveData<List<UpcomingPlan>> getUpcoming(String wcr, String wst, String assignmentCode, int limit);

        @Query("SELECT * FROM \"plan\" WHERE \"nextmfgnum\"=:mfgnum AND \"nextopenum\"=:openum ORDER BY start ASC")
        List<Plan> getPredecessors(String mfgnum, int openum);
*/

        @Query("SELECT MAX(\"update\") FROM `plan` ")
        Long getMaxUpdate();


    }



}


