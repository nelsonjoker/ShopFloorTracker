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

@Entity(tableName = "workitem",
        indices = {
                @Index(value = {"mfgnum", "mfglin"}, name = "mfgnum_mfglin_idx", unique = true),
                @Index(value = "mfgnum", name = "itm_mfgnum_idx"),
                @Index(value = "itmref", name = "itm_itmref_idx"),
                @Index(value = {"mfgnum", "itmref"}, name = "mfgnum_itmref_idx"),
        })
public class WorkItem implements Parcelable {


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
    @ColumnInfo(name = "cplqty")
    private double cplqty;
    @ColumnInfo(name = "epxitmref")
    private String epxitmref;
    @ColumnInfo(name = "extqty")
    private double extqty;
    @ColumnInfo(name = "itmsta")
    private int itmsta;
    @ColumnInfo(name = "matitmref")
    private String matitmref;
    @ColumnInfo(name = "mfitrkflg")
    private int mfitrkflg;
    @ColumnInfo(name = "pjt")
    private String pjt;
    @ColumnInfo(name = "soqtext")
    private String soqtext;
    @ColumnInfo(name = "stu")
    private String stu;
    @ColumnInfo(name = "uomextqty")
    private double uomextqty;
    @ColumnInfo(name = "uomstucoe")
    private double uomstucoe;
    @ColumnInfo(name = "vcritmref")
    private String vcritmref;
    @ColumnInfo(name = "vcrlinori")
    private int vcrlinori;
    @ColumnInfo(name = "vcrnumori")
    private String vcrnumori;

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

    public double getCplqty() {
        return cplqty;
    }

    public void setCplqty(double cplqty) {
        this.cplqty = cplqty;
    }

    public String getEpxitmref() {
        return epxitmref;
    }

    public void setEpxitmref(String epxitmref) {
        this.epxitmref = epxitmref;
    }

    public double getExtqty() {
        return extqty;
    }

    public void setExtqty(double extqty) {
        this.extqty = extqty;
    }

    public int getItmsta() {
        return itmsta;
    }

    public void setItmsta(int itmsta) {
        this.itmsta = itmsta;
    }

    public String getMatitmref() {
        return matitmref;
    }

    public void setMatitmref(String matitmref) {
        this.matitmref = matitmref;
    }

    public int getMfitrkflg() {
        return mfitrkflg;
    }

    public void setMfitrkflg(int mfitrkflg) {
        this.mfitrkflg = mfitrkflg;
    }

    public String getPjt() {
        return pjt;
    }

    public void setPjt(String pjt) {
        this.pjt = pjt;
    }

    public String getSoqtext() {
        return soqtext;
    }

    public void setSoqtext(String soqtext) {
        this.soqtext = soqtext;
    }

    public String getStu() {
        return stu;
    }

    public void setStu(String stu) {
        this.stu = stu;
    }

    public double getUomextqty() {
        return uomextqty;
    }

    public void setUomextqty(double uomextqty) {
        this.uomextqty = uomextqty;
    }

    public double getUomstucoe() {
        return uomstucoe;
    }

    public void setUomstucoe(double uomstucoe) {
        this.uomstucoe = uomstucoe;
    }

    public String getVcritmref() {
        return vcritmref;
    }

    public void setVcritmref(String vcritmref) {
        this.vcritmref = vcritmref;
    }

    public int getVcrlinori() {
        return vcrlinori;
    }

    public void setVcrlinori(int vcrlinori) {
        this.vcrlinori = vcrlinori;
    }

    public String getVcrnumori() {
        return vcrnumori;
    }

    public void setVcrnumori(String vcrnumori) {
        this.vcrnumori = vcrnumori;
    }

    public WorkItem(){
        id = 0L;
        mfglin = 0;
        itmref = "";
        itmdes = "";
        mfgnum = "";
        cplqty = 0;
        epxitmref = "";
        extqty = 0;
        itmsta = 0;
        matitmref = "";
        mfitrkflg = 0;
        pjt = "";
        soqtext = "";
        stu = "";
        uomextqty = 0;
        uomstucoe = 0;
        vcritmref = "";
        vcrlinori = 0;
        vcrnumori = "";
    }

    public WorkItem(Parcel p){
        this();
        id = p.readLong();
        mfglin = p.readInt();
        itmref = p.readString();
        itmdes = p.readString();
        mfgnum = p.readString();
        cplqty = p.readDouble();
        epxitmref = p.readString();
        extqty = p.readDouble();
        itmsta = p.readInt();
        matitmref = p.readString();
        mfitrkflg = p.readInt();
        pjt = p.readString();
        soqtext = p.readString();
        stu = p.readString();
        uomextqty = p.readDouble();
        uomstucoe = p.readDouble();
        vcritmref = p.readString();
        vcrlinori = p.readInt();
        vcrnumori = p.readString();
    }





    public static final Parcelable.Creator<WorkItem>
            CREATOR = new Parcelable.Creator<WorkItem>() {

        public WorkItem createFromParcel(Parcel in) {
            return new WorkItem(in);
        }

        public WorkItem[] newArray(int size) {
            return new WorkItem[size];
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
        parcel.writeDouble(cplqty);
        parcel.writeString(epxitmref);
        parcel.writeDouble(extqty);
        parcel.writeInt(itmsta);
        parcel.writeString(matitmref);
        parcel.writeInt(mfitrkflg);
        parcel.writeString(pjt);
        parcel.writeString(soqtext);
        parcel.writeString(stu);
        parcel.writeDouble(uomextqty);
        parcel.writeDouble(uomstucoe);
        parcel.writeString(vcritmref);
        parcel.writeInt(vcrlinori);
        parcel.writeString(vcrnumori);

    }


    @Dao
    public interface WorkItemDAO {

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        long insert(WorkItem w);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        long[] insertAll(WorkItem... w);

        @Update(onConflict = OnConflictStrategy.REPLACE)
        void updateAll(WorkItem... w);

        @Delete
        void deleteAll(WorkItem... w);


        @Query("SELECT * FROM workitem where mfgnum = :mfgnum ")
        List<WorkItem> getAll(String mfgnum);
        @Query("SELECT * FROM workitem where mfgnum IN (:mfgnums) ")
        List<WorkItem> getAll(String[] mfgnums);

    }



}