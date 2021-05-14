package pt.sotubo.shopfloortracker.model;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Entity;
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

@Entity(tableName = "itmmaster")
public class Article implements Parcelable {




    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "itmref")
    private String itmref;
    @ColumnInfo(name = "fcy")
    private String fcy;
    @ColumnInfo(name = "itmdes")
    private String itmdes;
    @ColumnInfo(name = "itmsta")
    private int itmsta;
    @ColumnInfo(name = "phyall")
    private double phyall;
    @ColumnInfo(name = "physto")
    private double physto;
    @ColumnInfo(name = "stu")
    private String stu;
    @ColumnInfo(name = "reocod")
    private int reocod;
    @ColumnInfo(name = "tclcod")
    private String tclcod;
    @ColumnInfo(name = "tsicod0")
    private String tsicod0;
    @ColumnInfo(name = "update")
    private long update;

    @NonNull
    public String getItmref() {
        return itmref;
    }

    public void setItmref(@NonNull String itmref) {
        this.itmref = itmref;
    }

    public String getFcy() {
        return fcy;
    }

    public void setFcy(String fcy) {
        this.fcy = fcy;
    }

    public String getItmdes() {
        return itmdes;
    }

    public void setItmdes(String itmdes) {
        this.itmdes = itmdes;
    }

    public int getItmsta() {
        return itmsta;
    }

    public void setItmsta(int itmsta) {
        this.itmsta = itmsta;
    }

    public double getPhyall() {
        return phyall;
    }

    public void setPhyall(double phyall) {
        this.phyall = phyall;
    }

    public double getPhysto() { return physto; }

    public void setPhysto(double physto) {
        this.physto = physto;
    }

    public String getStu() { return stu; }

    public void setStu(String stu) { this.stu = stu; }

    public int getReocod() {
        return reocod;
    }

    public void setReocod(int reocod) {
        this.reocod = reocod;
    }

    public String getTclcod() {
        return tclcod;
    }

    public void setTclcod(String tclcod) {
        this.tclcod = tclcod;
    }

    public String getTsicod0() {
        return tsicod0;
    }

    public void setTsicod0(String v) {
        this.tsicod0 = v;
    }


    public long getUpdate() {
        return update;
    }

    public void setUpdate(long update) {
        this.update = update;
    }


    public Article(){

        itmref = "";
        fcy = "";
        itmdes = "";
        itmsta = 1;
        phyall = 0;
        physto = 0;
        stu = "UN";
        reocod = 0;
        tclcod = "";
        tsicod0 = "";
        update = 0;
    }

    public Article(Parcel p){
        this();
        itmref = p.readString();
        fcy = p.readString();
        itmdes = p.readString();
        itmsta = p.readInt();
        phyall = p.readDouble();
        physto = p.readDouble();
        stu = p.readString();
        reocod = p.readInt();
        tclcod = p.readString();
        tsicod0 = p.readString();
        update = p.readLong();
    }


    @Override
    public boolean equals(Object obj) {
        return itmref.equals(((Article)obj).itmref) ;
    }

    @Override
    public int hashCode() { return itmref.hashCode(); }

    public static final Parcelable.Creator<Article>
            CREATOR = new Parcelable.Creator<Article>() {

        public Article createFromParcel(Parcel in) {
            return new Article(in);
        }

        public Article[] newArray(int size) {
            return new Article[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(itmref);
        parcel.writeString(fcy);
        parcel.writeString(itmdes );
        parcel.writeInt(itmsta);
        parcel.writeDouble(phyall );
        parcel.writeDouble(physto);
        parcel.writeString(stu);
        parcel.writeInt(reocod);
        parcel.writeString(tclcod);
        parcel.writeString(tsicod0);
        parcel.writeLong(update);

    }



    @Dao
    public interface ArticleDAO {

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        long insert(Article j);
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        long[] insertAll(Article... j);

        @Update(onConflict = OnConflictStrategy.REPLACE)
        void updateAll(Article... j);

        @Delete
        void deleteAll(Article... j);
        @Query("DELETE FROM `itmmaster`")
        void truncate();

        @Query("SELECT * FROM `itmmaster` where `itmref`=:itmref limit 1")
        Article get(String itmref);
        @Query("SELECT * FROM `itmmaster` WHERE `itmref` IN (:itmref) ")
        List<Article> get(String... itmref);

        @Query("SELECT * FROM `itmmaster` ")
        List<Article> getAll();

        @Query("SELECT itmref from `itmmaster` LIMIT :limit")
        LiveData<List<String>> getAllChanges(int limit);

        @Query("SELECT MAX(\"update\") FROM `itmmaster` ")
        Long getMaxUpdate();

    }



}
