package pt.sotubo.shopfloortracker.model;

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
 * Created by Nelson on 12/12/2017.
 */

@Entity(tableName = "workmaterial",
        indices = {
                @Index(value = {"mfgnum", "mfglin", "bomseq", "itmref"}, name = "mfm0_idx", unique = true),
                @Index(value = "mfgnum", name = "mat_mfgnum_idx"),
                @Index(value = "itmref", name = "mat_itmref_idx"),
                @Index(value = {"mfgnum", "bomope"}, name = "mfgnum_bomope_idx"),
        })
public class WorkMaterial implements Parcelable {


    @PrimaryKey(autoGenerate = true)
    @NonNull
    @ColumnInfo(name = "id")
    private long id;

    @ColumnInfo(name = "mfglin")
    private int mfglin;

    @NonNull
    @ColumnInfo(name = "itmref")
    private String itmref;
    @ColumnInfo(name = "itmdes")
    private String itmdes;
    @NonNull
    @ColumnInfo(name = "mfgnum")
    private String mfgnum;

    @ColumnInfo(name = "bomope")
    private int bomope;
    @ColumnInfo(name = "bomseq")
    private int bomseq;
    @ColumnInfo(name = "mfmtrkflg")
    private int mfmtrkflg;

    @ColumnInfo(name = "retqty")
    private double retqty;
    @ColumnInfo(name = "allqty")
    private double allqty;
    @ColumnInfo(name = "avaqty")
    private double avaqty;
    @ColumnInfo(name = "shtqty")
    private double shtqty;
    @ColumnInfo(name = "useqty")
    private double useqty;
    @ColumnInfo(name = "stu")
    private String stu;


    @NonNull
    public Long getId() {
        return id;
    }

    public void setId(@NonNull Long id) {
        this.id = id;
    }

    public int getMfglin() {
        return mfglin;
    }

    public void setMfglin(int i) {
        this.mfglin = i;
    }

    @NonNull
    public String getItmref() {
        return itmref;
    }

    public void setItmref(@NonNull String itmref) {
        this.itmref = itmref;
    }

    public String getItmdes() {
        return itmdes;
    }

    public void setItmdes(String itmdes) {
        this.itmdes = itmdes;
    }

    @NonNull
    public String getMfgnum() {
        return mfgnum;
    }

    public void setMfgnum(@NonNull String mfgnum) {
        this.mfgnum = mfgnum;
    }

    public int getBomope() {
        return bomope;
    }

    public void setBomope(int bomope) {
        this.bomope = bomope;
    }

    public int getBomseq() {
        return bomseq;
    }

    public void setBomseq(int bomseq) {
        this.bomseq = bomseq;
    }

    public int getMfmtrkflg() {
        return mfmtrkflg;
    }

    public void setMfmtrkflg(int mfmtrkflg) {
        this.mfmtrkflg = mfmtrkflg;
    }

    public double getRetqty() {
        return retqty;
    }

    public void setRetqty(double retqty) {
        this.retqty = retqty;
    }

    public double getAllqty() {
        return allqty;
    }

    public void setAllqty(double allqty) {
        this.allqty = allqty;
    }

    public double getAvaqty() {
        return avaqty;
    }

    public void setAvaqty(double avaqty) {
        this.avaqty = avaqty;
    }

    public double getShtqty() {
        return shtqty;
    }

    public void setShtqty(double shtqty) {
        this.shtqty = shtqty;
    }

    public double getUseqty() {
        return useqty;
    }

    public void setUseqty(double useqty) {
        this.useqty = useqty;
    }

    public String getStu() {
        return stu;
    }

    public void setStu(String stu) {
        this.stu = stu;
    }



    public WorkMaterial(){
        id = 0L;
        mfglin = 0;
        itmref = "";
        itmdes = "";
        mfgnum = "";
        bomope = 0;
        bomseq = 0;
        mfmtrkflg = 0;
        allqty = 0;
        avaqty = 0;
        shtqty = 0;
        useqty = 0;
        stu = "";
    }

    public WorkMaterial(Parcel p){
        this();
        id = p.readLong();
        mfglin = p.readInt();
        itmref = p.readString();
        itmdes = p.readString();
        mfgnum = p.readString();
        bomope = p.readInt();
        bomseq = p.readInt();
        mfmtrkflg = p.readInt();
        allqty = p.readDouble();
        avaqty = p.readDouble();
        shtqty = p.readDouble();
        useqty = p.readDouble();
        stu = p.readString();
    }

    public static final Parcelable.Creator<WorkMaterial>
            CREATOR = new Parcelable.Creator<WorkMaterial>() {

        public WorkMaterial createFromParcel(Parcel in) {
            return new WorkMaterial(in);
        }

        public WorkMaterial[] newArray(int size) {
            return new WorkMaterial[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(id);
        parcel.writeInt(mfglin);
        parcel.writeString(itmref);
        parcel.writeString(itmdes);
        parcel.writeString(mfgnum);
        parcel.writeInt(bomope);
        parcel.writeInt(bomseq);
        parcel.writeInt(mfmtrkflg);
        parcel.writeDouble(allqty);
        parcel.writeDouble(avaqty);
        parcel.writeDouble(shtqty);
        parcel.writeDouble(useqty);
        parcel.writeString(stu);

    }


    @Dao
    public interface WorkMaterialDAO {

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        long insert(WorkMaterial w);
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        long[] insertAll(WorkMaterial... w);

        @Update(onConflict = OnConflictStrategy.REPLACE)
        void updateAll(WorkMaterial... w);

        @Delete
        void deleteAll(WorkMaterial... w);


        @Query("SELECT * FROM workmaterial")
        List<WorkMaterial> getAll();
        //@Query("SELECT * FROM workmaterial where mfgnum = :mfgnum ")
        //List<WorkMaterial> getAll(String mfgnum);
        @Query("SELECT * FROM workmaterial where mfgnum IN (:mfgnums) ")
        List<WorkMaterial> getAll(String... mfgnums);


    }



}