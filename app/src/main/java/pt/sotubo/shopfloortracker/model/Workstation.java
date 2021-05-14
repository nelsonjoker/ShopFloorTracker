package pt.sotubo.shopfloortracker.model;

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
 * Created by Nelson on 07/12/2017.
 */

@Entity(tableName = "workstation")
public class Workstation implements Parcelable {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "code")
    private String code;
    @ColumnInfo(name = "wcr")
    private String wcr;
    @ColumnInfo(name = "description")
    private String description;


    public String getCode(){ return this.code; }
    public void setCode(String w) { this.code = w; }
    public String getWcr(){ return wcr; }
    public void setWcr(String n) { wcr = n; }
    public String getDescription(){ return description; }
    public void setDescription(String d) { description = d; }



    public Workstation(){
        code = "";
        wcr = "";
        description = "";
    }

    public Workstation(Parcel p){
        this();
        code = p.readString();
        wcr = p.readString();
        description = p.readString();
    }




    public static final Parcelable.Creator<Workstation>
            CREATOR = new Parcelable.Creator<Workstation>() {

        public Workstation createFromParcel(Parcel in) {
            return new Workstation(in);
        }

        public Workstation[] newArray(int size) {
            return new Workstation[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(code);
        parcel.writeString(wcr);
        parcel.writeString(description);
    }


    @Dao
    public interface WorkstationDAO {

        @Insert
        long insert(Workstation w);

        @Insert
        long[] insertAll(Workstation... w);

        @Update
        void updateAll(Workstation... w);

        @Delete
        void deleteAll(Workstation... w);


        @Query("SELECT * FROM workstation")
        List<Workstation> getAll();
        @Query("SELECT * FROM workstation where wcr = :workcenter")
        List<Workstation> getAll(String workcenter);

    }



}
