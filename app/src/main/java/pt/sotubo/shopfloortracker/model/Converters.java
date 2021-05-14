package pt.sotubo.shopfloortracker.model;

import android.arch.persistence.room.TypeConverter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by Nelson on 07/12/2017.
 */

public class Converters {
    @TypeConverter
    public static Map<String, Object> mapFromString(String value) {
        Map<String, Object> res = null;
        try {
            JSONObject jobj = new JSONObject(value);
            res = toMap(jobj);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return res;
    }

    @TypeConverter
    public static String stringFromMap(Map<String, Object> m) {
        JSONObject jobj = new JSONObject(m);
        String res = jobj.toString();
        return res;
    }


    private static Map<String, Object> toMap(JSONObject object) throws JSONException {
        Map<String, Object> map = new HashMap<String, Object>();

        Iterator<String> keysItr = object.keys();
        while(keysItr.hasNext()) {
            String key = keysItr.next();
            Object value = object.get(key);

            if(value instanceof JSONArray) {
                value = toList((JSONArray) value);
            }

            else if(value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            map.put(key, value);
        }
        return map;
    }

    private static List<Object> toList(JSONArray array) throws JSONException {
        List<Object> list = new ArrayList<>();
        for(int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if(value instanceof JSONArray) {
                value = toList((JSONArray) value);
            }

            else if(value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            list.add(value);
        }
        return list;
    }
}