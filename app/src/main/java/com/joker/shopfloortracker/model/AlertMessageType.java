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
 * Created by Nelson on 14/03/2018.
 */

@Entity(tableName = "alert_message_type")
public class AlertMessageType implements Parcelable {

    public static final int ALERT_MESSAGE_TYPE_BOM = 100;
    public static final int ALERT_MESSAGE_TYPE_OPERATION = 200;
    public static final int ALERT_MESSAGE_TYPE_APPLICATION= 300;

    @NonNull
    @ColumnInfo(name = "id")
    @PrimaryKey(autoGenerate = false)
    private long id;

    @NonNull
    @ColumnInfo(name = "title")
    private String title;
    @ColumnInfo(name = "description")
    private String description;
    @ColumnInfo(name = "emails")
    private String emails;

    public long getId() { return id; }
    public void setId(long v) { id = v; }

    public String getTitle() { return title; }
    public void setTitle(String v) { title = v; }

    public String getDescription() { return description; }
    public void setDescription(String v) { description = v; }

    public String getEmails() { return emails; }
    public void setEmails(String v) { emails = v; }

    public AlertMessageType(){

        id = -1;
        title = "";
        description = "";
        emails = "";

    }

    public AlertMessageType(Parcel p){
        this();
        id = p.readLong();
        title = p.readString();
        description = p.readString();
        emails = p.readString();
    }


    @Override
    public boolean equals(Object obj) {
        return id == ((AlertMessageType)obj).id ;
    }

    public static final Parcelable.Creator<AlertMessageType>
            CREATOR = new Parcelable.Creator<AlertMessageType>() {

        public AlertMessageType createFromParcel(Parcel in) {
            return new AlertMessageType(in);
        }

        public AlertMessageType[] newArray(int size) {
            return new AlertMessageType[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(id);
        parcel.writeString(title);
        parcel.writeString(description);
        parcel.writeString(emails);
    }



    @Dao
    public interface AlertMessageTypeDAO {

        @Insert
        long insert(AlertMessageType j);
        @Insert
        long[] insertAll(AlertMessageType... j);

        @Update
        void updateAll(AlertMessageType... j);

        @Delete
        void deleteAll(AlertMessageType... j);
        @Query("DELETE FROM \"alert_message_type\"")
        void truncate();

        @Query("SELECT * FROM \"alert_message_type\" where \"id\"=:type_id limit 1")
        AlertMessageType get(long type_id);

        @Query("SELECT * FROM \"alert_message_type\" ")
        List<AlertMessageType> getAll();

    }





}
