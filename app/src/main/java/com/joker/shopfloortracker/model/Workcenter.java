package com.joker.shopfloortracker.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.List;

/**
 * Created by Nelson on 05/12/2017.
 */

@Entity(tableName = "workcenter")
public class Workcenter implements Parcelable {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "code")
    private String code;
    @ColumnInfo(name = "name")
    private String name;
    @ColumnInfo(name = "description")
    private String description;


    public String getCode(){ return this.code; }
    public void setCode(String w) { this.code = w; }
    public String getName(){ return name; }
    public void setName(String n) { name = n; }
    public String getDescription(){ return description; }
    public void setDescription(String d) { description = d; }



    public Workcenter(){
        code = "";
        name = "";
        description = "";
    }

    public Workcenter(Parcel p){
        this();
        code = p.readString();
        name = p.readString();
        description = p.readString();
    }




    public static final Parcelable.Creator<Workcenter>
            CREATOR = new Parcelable.Creator<Workcenter>() {

        public Workcenter createFromParcel(Parcel in) {
            return new Workcenter(in);
        }

        public Workcenter[] newArray(int size) {
            return new Workcenter[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(code);
        parcel.writeString(name);
        parcel.writeString(description);
    }


    @Dao
    public interface WorkcenterDAO {

        // Adds a workcenter to the database
        @Insert
        long insert(Workcenter workcenter);
        @Insert
        long[] insertAll(Workcenter... workcenters);

        @Update
        void updateAll(Workcenter... w);

        // Removes a workcenter from the database
        @Delete
        void deleteAll(Workcenter... w);


        // Gets all workcenters in the database
        @Query("SELECT * FROM workcenter")
        List<Workcenter> getAll();

    }



}
