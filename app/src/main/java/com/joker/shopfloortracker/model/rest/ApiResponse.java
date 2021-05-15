package com.joker.shopfloortracker.model.rest;

import com.android.volley.VolleyError;

/**
 * Created by Nelson on 06/12/2017.
 */

public abstract class ApiResponse{

    public abstract void onResponse(Object response, VolleyError err, boolean result);
}
