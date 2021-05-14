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

@Entity(tableName = "operation",
        indices = {
                @Index(value = {"mfgnum", "openum", "opesplnum"}, name = "mfo0_idx", unique = true)
        })
public class Operation implements Parcelable {


    @PrimaryKey(autoGenerate = true)
    @NonNull
    @ColumnInfo(name = "id")
    private long id;

    @NonNull
    @ColumnInfo(name = "mfgnum")
    private String mfgnum;

    @ColumnInfo(name = "wcr")
    private String wcr;
    @ColumnInfo(name = "labwcr")
    private String labwcr;
    @ColumnInfo(name = "extlab")
    private String extlab;
    @ColumnInfo(name = "extlabnbr")
    private int extlabnbr;
    @ColumnInfo(name = "extwst")
    private String extwst;
    @ColumnInfo(name = "extwstnbr")
    private int extwstnbr;
    @ColumnInfo(name = "opestr")
    private long opestr;



    @ColumnInfo(name = "opeend")
    private long opeend;

    @ColumnInfo(name = "mfotrkflg")
    private int mfotrkflg;
    @ColumnInfo(name = "nexopenum")
    private int nexopenum;
    @ColumnInfo(name = "openum")
    private int openum;
    @ColumnInfo(name = "opesplnum")
    private int opesplnum;
    @ColumnInfo(name = "opesta")
    private int opesta;
    @ColumnInfo(name = "opeuom")
    private String opeuom;


    @ColumnInfo(name = "cplqty")
    private double cplqty;
    @ColumnInfo(name = "extqty")
    private double extqty;

    @ColumnInfo(name = "extopetim")
    private double extopetim;
    @ColumnInfo(name = "extsettim")
    private double extsettim;
    @ColumnInfo(name = "extunttim")
    private double extunttim;
    @ColumnInfo(name = "cplopetim")
    private double cplopetim;
    @ColumnInfo(name = "cplsettim")
    private double cplsettim;


    @ColumnInfo(name = "timuomcod")
    private int timuomcod;

    @NonNull
    public Long getId() {
        return id;
    }

    public void setId(@NonNull Long id) {
        this.id = id;
    }

    @NonNull
    public String getMfgnum() {
        return mfgnum;
    }

    public void setMfgnum(@NonNull String mfgnum) {
        this.mfgnum = mfgnum;
    }

    public String getWcr() {
        return wcr;
    }

    public void setWcr(String wcr) {
        this.wcr = wcr;
    }

    public String getLabwcr() {
        return labwcr;
    }

    public void setLabwcr(String labwcr) {
        this.labwcr = labwcr;
    }

    public String getExtlab() {
        return extlab;
    }

    public void setExtlab(String extlab) {
        this.extlab = extlab;
    }

    public int getExtlabnbr() {
        return extlabnbr;
    }

    public void setExtlabnbr(int extlabnbr) {
        this.extlabnbr = extlabnbr;
    }

    public String getExtwst() {
        return extwst;
    }

    public void setExtwst(String extwst) {
        this.extwst = extwst;
    }

    public int getExtwstnbr() {
        return extwstnbr;
    }

    public void setExtwstnbr(int extwstnbr) {
        this.extwstnbr = extwstnbr;
    }

    public long getOpestr() {
        return opestr;
    }

    public void setOpestr(long opestr) {
        this.opestr = opestr;
    }

    public long getOpeend() {
        return opeend;
    }

    public void setOpeend(long opeend) {
        this.opeend = opeend;
    }

    public int getMfotrkflg() {
        return mfotrkflg;
    }

    public void setMfotrkflg(int mfotrkflg) {
        this.mfotrkflg = mfotrkflg;
    }

    public int getNexopenum() {
        return nexopenum;
    }

    public void setNexopenum(int nexopenum) {
        this.nexopenum = nexopenum;
    }

    public int getOpenum() {
        return openum;
    }

    public void setOpenum(int openum) {
        this.openum = openum;
    }

    public int getOpesplnum() {
        return opesplnum;
    }

    public void setOpesplnum(int opeslpnum) {
        this.opesplnum = opesplnum;
    }


    public int getOpesta() {
        return opesta;
    }

    public void setOpesta(int opesta) {
        this.opesta = opesta;
    }

    public String getOpeuom() {
        return opeuom;
    }

    public void setOpeuom(String opeuom) {
        this.opeuom = opeuom;
    }

    public double getCplqty() {
        return cplqty;
    }

    public void setCplqty(double cplqty) {
        this.cplqty = cplqty;
    }

    public double getExtqty() {
        return extqty;
    }

    public void setExtqty(double extqty) {
        this.extqty = extqty;
    }

    public double getExtopetim() {
        return extopetim;
    }

    public void setExtopetim(double extopetim) {
        this.extopetim = extopetim;
    }

    public double getExtsettim() {
        return extsettim;
    }

    public void setExtsettim(double extsettim) {
        this.extsettim = extsettim;
    }

    public double getExtunttim() {
        return extunttim;
    }

    public void setExtunttim(double extunttim) {
        this.extunttim = extunttim;
    }

    public double getCplopetim() {
        return cplopetim;
    }

    public void setCplopetim(double t) {
        this.cplopetim = t;
    }
    public double getCplsettim() {
        return cplsettim;
    }

    public void setCplsettim(double t) {
        this.cplsettim = t;
    }

    public int getTimuomcod() {
        return timuomcod;
    }

    public void setTimuomcod(int timuomcod) {
        this.timuomcod = timuomcod;
    }


    public Operation(){
        id = 0L;
        mfgnum = "";
        wcr = "";
        labwcr = "";
        extlab = "";
        extlabnbr = 0;
        extwst = "";
        extwstnbr = 0;
        opestr = System.currentTimeMillis();
        opeend = System.currentTimeMillis();
        mfotrkflg = 0;
        nexopenum = 0;
        openum = 0;
        opesplnum = 0;
        opesta = 0;
        opeuom = "";
        cplqty = 0;
        extqty = 0;
        extopetim = 0;
        extsettim = 0;
        extunttim = 0;
        cplopetim = 0;
        cplsettim = 0;
        timuomcod = 2;
    }

    public Operation(Parcel p){
        this();

        id = p.readLong();
        mfgnum = p.readString();
        wcr = p.readString();
        labwcr = p.readString();
        extlab = p.readString();
        extlabnbr = p.readInt();
        extwst = p.readString();
        extwstnbr = p.readInt();
        opestr = p.readLong();
        opeend = p.readLong();
        mfotrkflg = p.readInt();
        nexopenum = p.readInt();
        openum = p.readInt();
        opesplnum = p.readInt();
        opesta = p.readInt();
        opeuom = p.readString();
        cplqty = p.readDouble();
        extqty = p.readDouble();
        extopetim = p.readDouble();
        extsettim = p.readDouble();
        extunttim = p.readDouble();
        cplopetim = p.readDouble();
        cplsettim = p.readDouble();
        timuomcod = p.readInt();


    }

    public static final Parcelable.Creator<Operation>
            CREATOR = new Parcelable.Creator<Operation>() {

        public Operation createFromParcel(Parcel in) {
            return new Operation(in);
        }

        public Operation[] newArray(int size) {
            return new Operation[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(id);
        parcel.writeString(mfgnum);
        parcel.writeString(wcr);
        parcel.writeString(labwcr);
        parcel.writeString(extlab);
        parcel.writeInt(extlabnbr);
        parcel.writeString(extwst);
        parcel.writeInt(extwstnbr);
        parcel.writeLong(opestr);
        parcel.writeLong(opeend);
        parcel.writeDouble(mfotrkflg);
        parcel.writeInt(nexopenum);
        parcel.writeInt(openum);
        parcel.writeInt(opesplnum);
        parcel.writeInt(opesta);
        parcel.writeString(opeuom);
        parcel.writeDouble(cplqty);
        parcel.writeDouble(extqty);
        parcel.writeDouble(extopetim);
        parcel.writeDouble(extsettim);
        parcel.writeDouble(extunttim);
        parcel.writeDouble(cplopetim);
        parcel.writeDouble(cplsettim);
        parcel.writeInt(timuomcod);

    }


    @Dao
    public interface OperationDAO {

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        long insert(Operation o);
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        long[] insertAll(Operation... o);

        @Update(onConflict = OnConflictStrategy.REPLACE)
        void updateAll(Operation... o);

        @Delete
        void deleteAll(Operation... o);


        @Query("SELECT * FROM operation")
        List<Operation> getAll();

        @Query("SELECT * FROM operation where mfgnum = :mfgnum ")
        List<Operation> getAll(String mfgnum);
        @Query("SELECT * FROM operation where mfgnum IN (:mfgnums) ")
        List<Operation> getAll(String[] mfgnums);


    }



}