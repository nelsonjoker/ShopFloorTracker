package pt.sotubo.shopfloortracker.model;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * Created by Nelson on 13/04/2018.
 */

public class UpcomingPlan extends Plan {

    /**
     * Remaining qty available to be executed
     */
    @ColumnInfo(name = "remaining")
    private double remainingQty;
    public double getRemainingQty() { return remainingQty; }
    public  void setRemainingQty(double r) { remainingQty = r; }

    @ColumnInfo(name = "pjt")
    private String pjt;
    public String getPjt() { return pjt; }
    public  void setPjt(String r) { pjt = r; }



    public  UpcomingPlan(){
        super();
        remainingQty = 0;
        pjt = "";
    }

    public UpcomingPlan(Parcel p){
        super(p);
    }


    @Override
    public double assign(String code) throws Exception {
        return assign(code, getRemainingQty() - getAssignedQty());
    }


    @Override
    protected void parse(Parcel p) {
        super.parse(p);
        remainingQty = p.readDouble();
        pjt = p.readString();
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        super.writeToParcel(parcel, flags);
        parcel.writeDouble(remainingQty);
        parcel.writeString(pjt);
    }

    public static final Parcelable.Creator<UpcomingPlan>
            CREATOR = new Parcelable.Creator<UpcomingPlan>() {

        public UpcomingPlan createFromParcel(Parcel in) {
            return new UpcomingPlan(in);
        }

        public UpcomingPlan[] newArray(int size) {
            return new UpcomingPlan[size];
        }
    };


    @Dao
    public interface UpcomingPlanDAO extends Plan.PlanDAO{


        @Query("select " +
                "p.*, wi.pjt as pjt, " +
                "ROUND( p.extqty - p.cplqty, 2*(p.itmstu!=\"UN\")) as remaining " +
                "from " +
                "`plan` p " +
                "left join `workitem` wi on wi.mfgnum=p.mfgnum and wi.itmref = p.itmref "+  //TODO: missing plan.mfglin we are
                "WHERE p.id IN (:id) " )
        List<UpcomingPlan> getUpcoming(String... id);

        @Query("select " +
                "p.*, wi.pjt as pjt, " +
                "ROUND( p.extqty - p.cplqty, 2*(p.itmstu!=\"UN\")) as remaining " +
                "from " +
                "`plan` p " +
                "left join `workitem` wi on wi.mfgnum=p.mfgnum and wi.itmref = p.itmref "+  //TODO: missing plan.mfglin we are

                "WHERE p.wcr=:wcr AND p.wst=:wst AND p.completed = 0 " +
                "AND ROUND( p.extqty - p.cplqty, 2*(p.itmstu!=\"UN\")) > 0 " +
                "AND (p.assigned = 0 OR p.assignment_code = :assignmentCode) " +
                "AND ( :itmref='' OR p.itmref LIKE :itmref ) " +
                "ORDER BY p.itmref DESC, p.start ASC LIMIT :limit")
        LiveData<List<UpcomingPlan>> getUpcoming(String wcr, String wst, String assignmentCode, int limit, String itmref);

        @Query("select " +
                "p.*, wi.pjt as pjt, " +
                "ROUND( p.extqty - p.cplqty, 2*(p.itmstu!=\"UN\")) as remaining " +
                "from " +
                "`plan` p " +
                "left join `workitem` wi on wi.mfgnum=p.mfgnum and wi.itmref = p.itmref "+  //TODO: missing plan.mfglin we are
                "left join ( " +
                "    select " +
                "    p.id, " +
                "    wm.itmref in (:materials) as match_materials" +
                "    from " +
                "    `plan` p " +
                "    left join workmaterial wm on wm.mfgnum = p.mfgnum and wm.bomope = p.openum " +
                "    group by p.id " +
                ") rmi on rmi.id = p.id " +
                "WHERE p.wcr=:wcr AND p.wst=:wst AND p.completed = 0 " +
                "AND ROUND( p.extqty - p.cplqty, 2*(p.itmstu!=\"UN\")) > 0 " +
                "AND (( p.assigned = 0 AND rmi.match_materials = 1 ) OR p.assignment_code = :assignmentCode) " +
                "AND ( :itmref='' OR p.itmref LIKE :itmref ) " +
                "ORDER BY p.itmref DESC, p.start ASC LIMIT :limit")
        LiveData<List<UpcomingPlan>> getUpcoming(String wcr, String wst, String assignmentCode, int limit,  String itmref, String... materials);


        @Query("select " +
                "p.*, wi.pjt as pjt, " +
                "ROUND( p.extqty - p.cplqty, 2*(p.itmstu!=\"UN\")) as remaining " +
                "from " +
                "`plan` p " +
                "left join `workitem` wi on wi.mfgnum=p.mfgnum and wi.itmref = p.itmref "+  //TODO: missing plan.mfglin we are

                "WHERE p.wcr=:wcr AND p.wst=:wst AND p.completed = 0 " +
                "AND ROUND( p.extqty - p.cplqty, 2*(p.itmstu!=\"UN\")) > 0 " +
                "AND (p.assigned = 0 OR p.assignment_code = :assignmentCode) " +
                "AND ( :itmref='' OR p.itmref LIKE :itmref ) " +
                "ORDER BY p.itmref DESC, p.start ASC LIMIT :limit")
        List<UpcomingPlan> getUpcoming_(String wcr, String wst, String assignmentCode, int limit, String itmref);

        @Query("select " +
                "p.*, wi.pjt as pjt, " +
                "ROUND( p.extqty - p.cplqty, 2*(p.itmstu!=\"UN\")) as remaining " +
                "from " +
                "`plan` p " +
                "left join `workitem` wi on wi.mfgnum=p.mfgnum and wi.itmref = p.itmref "+  //TODO: missing plan.mfglin we are
                "left join ( " +
                "    select " +
                "    p.id, " +
                "    wm.itmref in (:materials) as match_materials" +
                "    from " +
                "    `plan` p " +
                "    left join workmaterial wm on wm.mfgnum = p.mfgnum and wm.bomope = p.openum " +
                "    group by p.id " +
                ") rmi on rmi.id = p.id " +
                "WHERE p.wcr=:wcr AND p.wst=:wst AND p.completed = 0 " +
                "AND ROUND( p.extqty - p.cplqty, 2*(p.itmstu!=\"UN\")) > 0 " +
                "AND (( p.assigned = 0 AND rmi.match_materials = 1 ) OR p.assignment_code = :assignmentCode) " +
                "AND ( :itmref='' OR p.itmref LIKE :itmref ) " +
                "ORDER BY p.itmref DESC, p.start ASC LIMIT :limit")
        List<UpcomingPlan> getUpcoming_(String wcr, String wst, String assignmentCode, int limit,  String itmref, String... materials);

        @Query("select " +
                "p.*, wi.pjt as pjt, " +
                "ROUND(MIN(ifnull(rmi.min_rmi, p.extqty), ifnull(oi.min_oi*p.extqty, p.extqty), p.extqty) - p.cplqty, 2*(p.itmstu!=\"UN\")) as remaining " +
                "from " +
                "`plan` p " +
                "left join `workitem` wi on wi.mfgnum=p.mfgnum and wi.itmref = p.itmref "+  //TODO: missing plan.mfglin we are
                "left join( " +
                "    select " +
                "    p.id, " +
                "    MIN((p.extqty/wm.retqty)*(wm.retqty - prev.sext + prev.scpl)) as min_rmi " +
                "    from " +
                "    `plan` p " +
                "    left join workmaterial wm on wm.mfgnum = p.mfgnum and wm.bomope = p.openum " +
                "    left join ( " +
                "        select nextmfgnum, nextopenum, itmref, sum(extqty) as sext, sum(cplqty) scpl " +
                "        from `plan` " +
                "        group by nextmfgnum, nextopenum, itmref " +
                "    ) prev on prev.nextmfgnum = wm.mfgnum and prev.nextopenum = wm.bomope and prev.itmref = wm.itmref " +
                "    group by p.id " +
                ") rmi on rmi.id = p.id " +
                "left join( " +
                "    select " +
                "    nextmfgnum, " +
                "    nextopenum, " +
                "    itmref, " +
                "    min(cplqty / extqty) as min_oi " +
                "    from " +
                "    `plan` " +
                "    group by nextmfgnum, nextopenum, itmref " +
                ")as oi on oi.nextmfgnum = p.mfgnum and oi.nextopenum = p.openum and oi.itmref = p.itmref " +
                "WHERE p.wcr=:wcr AND p.wst=:wst AND p.completed = 0 " +
                "AND ROUND(MIN(ifnull(rmi.min_rmi, p.extqty), ifnull(oi.min_oi*p.extqty, p.extqty), p.extqty) - p.cplqty , 2*(p.itmstu!=\"UN\")) > 0 " +
                "AND (p.assigned = 0 OR p.assignment_code = :assignmentCode) " +
                "ORDER BY p.assigned DESC, p.start ASC LIMIT :limit")
        List<UpcomingPlan> getUpcomingDebug(String wcr, String wst, String assignmentCode, int limit);


    }

}
