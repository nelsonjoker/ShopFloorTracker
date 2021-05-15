package com.joker.shopfloortracker.model;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
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
 * Created by Nelson on 12/12/2017.
 */
@Entity(tableName = "workorder")
public class Workorder implements Parcelable {


    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "mfgnum")
    private String mfgnum;
    @ColumnInfo(name = "mfgfcy")
    private String mfgfcy;
    @ColumnInfo(name = "mfgsta")
    private int mfgsta;
    @ColumnInfo(name = "mfgtrkflg")
    private int mfgtrkflg;
    @ColumnInfo(name = "update")
    private long update;

    @NonNull
    public String getMfgnum() {
        return mfgnum;
    }

    public void setMfgnum(@NonNull String mfgnum) {
        this.mfgnum = mfgnum;
    }

    public String getMfgfcy() {
        return mfgfcy;
    }

    public void setMfgfcy(String mfgfcy) {
        this.mfgfcy = mfgfcy;
    }


    public int getMfgsta() {
        return mfgsta;
    }

    public void setMfgsta(int mfgsta) {
        this.mfgsta = mfgsta;
    }

    public int getMfgtrkflg() {
        return mfgtrkflg;
    }

    public void setMfgtrkflg(int mfgtrkflg) {
        this.mfgtrkflg = mfgtrkflg;
    }

    public long getUpdate() {
        return update;
    }

    public void setUpdate(long update) {
        this.update = update;
    }

    @Ignore
    private List<WorkItem> items;
    @Ignore
    private List<WorkMaterial> materials;
    @Ignore
    private List<Operation> operations;


    public List<WorkItem> getItems() {
        return items;
    }

    public void setItems(List<WorkItem> items) {
        this.items = items;
    }

    public List<WorkMaterial> getMaterials() {
        return materials;
    }

    public void setMaterials(List<WorkMaterial> materials) {
        this.materials = materials;
    }

    public List<Operation> getOperations() {
        return operations;
    }

    public void setOperations(List<Operation> operations) {
        this.operations = operations;
    }




    public Workorder(){
        mfgnum = "";
        mfgfcy = "";
        mfgsta = 0;
        mfgtrkflg = 0;
        update = 0;
        items = null;
        materials = null;
        operations = null;

    }

    public Workorder(Parcel p){
        this();
        mfgnum = p.readString();
        mfgfcy = p.readString();
        mfgsta = p.readInt();
        mfgtrkflg = p.readInt();
        update = p.readLong();
    }


    @Override
    public boolean equals(Object obj) {
        return this.mfgnum.equals(((Workorder)obj).getMfgnum());
    }

    public int hashCode() { return mfgnum.hashCode(); }

    public static final Parcelable.Creator<Workorder>
            CREATOR = new Parcelable.Creator<Workorder>() {

        public Workorder createFromParcel(Parcel in) {
            return new Workorder(in);
        }

        public Workorder[] newArray(int size) {
            return new Workorder[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(mfgnum);
        parcel.writeString(mfgfcy);
        parcel.writeInt(mfgsta);
        parcel.writeInt(mfgtrkflg);
        parcel.writeLong(update);
    }


    @Dao
    public interface WorkorderDAO {

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        long insert(Workorder w);
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        long[] insertAll(Workorder... w);

        @Update(onConflict = OnConflictStrategy.REPLACE)
        void updateAll(Workorder... w);

        @Delete
        void deleteAll(Workorder... w);
        @Query("DELETE FROM \"workorder\"")
        void truncate();

        @Query("SELECT * FROM workorder")
        List<Workorder> getAll();
        @Query("SELECT * FROM workorder where mfgnum IN(:mfgnums)")
        List<Workorder> getAll(String[] mfgnums);
        @Query("SELECT wo.mfgnum as mfgnum FROM " +
                "workorder wo " +
                "LEFT JOIN workitem wi ON wi.mfgnum = wo.mfgnum " +
                "LEFT JOIN workmaterial wm ON wm.mfgnum = wi.mfgnum " +
                "LIMIT :limit")
        LiveData<List<String>> getAllChanges(int limit);

        @Query("SELECT * FROM \"workorder\" WHERE \"mfgnum\" = :mfgnum ")
        Workorder get(String mfgnum);


        @Query("SELECT MAX(\"update\") FROM \"workorder\" ")
        Long getMaxUpdate();

    }



}