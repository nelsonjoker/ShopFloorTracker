package pt.sotubo.shopfloortracker.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Nelson on 05/12/2017.
 */

@Entity(tableName = "job")
public class Job implements Parcelable{

    @NonNull
    @ColumnInfo(name = "id")
    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    @ColumnInfo(name = "assignment_code")
    private String assignmentCode;

    @ColumnInfo(name = "operator")
    private String operator;
    @ColumnInfo(name = "wcr")
    private String wcr;
    @ColumnInfo(name = "wst")
    private String wst;
    @ColumnInfo(name = "index")
    private int index;
    @ColumnInfo(name = "name")
    private String name;
    @ColumnInfo(name = "visible")
    private boolean visible;
    @ColumnInfo(name = "parameters")
    private Map<String, Object> filterParameters;

    public long getId() { return id; }
    public void setId(long v) { id = v; }
    public String getAssignmentCode() { return assignmentCode; }
    public void setAssignmentCode(String v) { assignmentCode = v; }
    public String getOperator() { return operator; }
    public void setOperator(String o) { operator = o; }
    public String getWcr() { return wcr; }
    public void setWcr(String w) { wcr = w; }
    public String getWst() { return wst; }
    public void setWst(String o) { wst = o; }
    public int getIndex() { return index; }
    public void setIndex(int i) { index = i; }
    public String getName() { return name; }
    public void setName(String n) { name = n; }
    public boolean getVisible() { return visible; }
    public void setVisible(boolean v) { visible = v; }
    public Map<String, Object> getFilterParameters() { return filterParameters; }
    public void setFilterParameters(Map<String, Object> v) { filterParameters = v; }

    public Job(){

        operator = "";
        wcr = "";
        wst = "";
        index = 0;
        name = "";
        assignmentCode = "";
        visible = true;
        filterParameters = new HashMap<>();
    }

    public Job(Parcel p){
        this();
        id = p.readLong();
        operator = p.readString();
        wcr = p.readString();
        wst = p.readString();
        index = p.readInt();
        name = p.readString();
        assignmentCode = p.readString();
        visible = p.readByte() != 0;
        p.readMap(filterParameters, HashMap.class.getClassLoader());

    }


    @Override
    public boolean equals(Object obj) {
        return id == ((Job)obj).id;
    }

    @Override
    public int hashCode() { return (int)(this.id^(this.id>>>32)); }

    public static final Parcelable.Creator<Job>
            CREATOR = new Parcelable.Creator<Job>() {

        public Job createFromParcel(Parcel in) {
            return new Job(in);
        }

        public Job[] newArray(int size) {
            return new Job[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(id);
        parcel.writeString(operator);
        parcel.writeString(wcr);
        parcel.writeString(wst);
        parcel.writeInt(index);
        parcel.writeString(name);
        parcel.writeString(assignmentCode);
        parcel.writeByte((byte)(visible ? 1 : 0));
        parcel.writeMap(filterParameters);
    }

    public void copyTo(Job ex) {
        ex.id = id;
        ex.operator = operator;
        ex.wcr = wcr;
        ex.wst = wst;
        ex.index = index;
        ex.name = name;
        ex.assignmentCode = assignmentCode;
        ex.visible = visible;
        ex.filterParameters.clear();
        ex.filterParameters.putAll(this.filterParameters);
    }


    @Dao
    public interface JobDAO {

        @Insert
        long insert(Job j);
        @Insert
        long[] insertAll(Job... j);

        @Update
        void updateAll(Job... j);

        @Delete
        void deleteAll(Job... j);
        @Query("DELETE FROM \"job\"")
        void truncate();


        @Query("SELECT * FROM job where visible = 1")
        List<Job> getAll();

    }
}
