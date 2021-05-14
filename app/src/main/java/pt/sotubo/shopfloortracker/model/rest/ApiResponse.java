package pt.sotubo.shopfloortracker.model.rest;

import com.android.volley.VolleyError;

import org.json.JSONArray;

/**
 * Created by Nelson on 06/12/2017.
 */

public abstract class ApiResponse{

    public abstract void onResponse(Object response, VolleyError err, boolean result);
}
