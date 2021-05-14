package pt.sotubo.shopfloortracker.model.rest;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by Nelson on 06/12/2017.
 */

public class Api {

    private static String API_USERNAME = "sig";
    private static String API_PASSWORD_TOKEN = "a2292cc4096d84751539965522655cff";
    private static String API_ENDPOINT = "http://api.sotubo.pt/";


    public static final int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();


    private static Api mInstance;
    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;
    private Context mCtx;
    private ThreadPoolExecutor mForLightWeightBackgroundTasks;

    private Api(Context context) {
        mCtx = context;
        mRequestQueue = getRequestQueue();

        mImageLoader = new ImageLoader(mRequestQueue,
                new ImageLoader.ImageCache() {
                    private final LruCache<String, Bitmap>
                            cache = new LruCache<String, Bitmap>(20);

                    @Override
                    public Bitmap getBitmap(String url) {
                        return cache.get(url);
                    }

                    @Override
                    public void putBitmap(String url, Bitmap bitmap) {
                        cache.put(url, bitmap);
                    }
                });

        mForLightWeightBackgroundTasks = new ThreadPoolExecutor(
                NUMBER_OF_CORES,
                NUMBER_OF_CORES * 2,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());

    }

    public String url(String url){

        return url.startsWith("/") ? Api.API_ENDPOINT+url : Api.API_ENDPOINT+"/"+url;
    }

    public static synchronized Api getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new Api(context);
        }
        return mInstance;
    }

    private RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            mRequestQueue = Volley.newRequestQueue(mCtx.getApplicationContext());
        }
        return mRequestQueue;
    }

    private <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

    public ImageLoader getImageLoader() {
        return mImageLoader;
    }


    public void post(String url, Map<String,String> data, ApiResponse response){
        post(url, data, response, true);
    }
    public void post(String url, Map<String,String> data, ApiResponse response, boolean uiThread){

        query(Request.Method.POST, url, data, response, uiThread);

    }


    public void get(String url, ApiResponse response, boolean uiThread){

        query(Request.Method.GET, url, null, response, uiThread);

    }


    protected void query(int method, String url, Map<String,String> data, ApiResponse response, boolean uiThread){

        url = url( url);

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (method, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject r) {
                        boolean res = false;
                        Object jdata = null;
//{"result":1,"jdata":[{"id":13,"description":"Corte","name":"COR","code":"COR"},{"id":14,"description":"Embalagem","name":"EMB","code":"EMB"},{"id":15,"description":"Estofagem","name":"EST","code":"EST"},{"id":16,"description":"Expedição","name":"EXP","code":"EXP"},{"id":17,"description":"Marcenaria","name":"MAR","code":"MAR"},{"id":18,"description":"Pintura","name":"PIN","code":"PIN"},{"id":19,"description":"Quinagem","name":"QUI","code":"QUI"},{"id":20,"description":"Soldadura","name":"SOL","code":"SOL"},{"id":21,"description":"SubContratação","name":"SUB","code":"SUB"},{"id":22,"description":"Tubo","name":"TUB","code":"TUB"},{"id":23,"description":"Ultimacão","name":"ULT","code":"ULT"},{"id":24,"description":"Envernizamento","name":"VER","code":"VER"}],"errors":[],"extra":[],"messages":[]}
                        try {
                            int result = r.getInt("result");
                            if(result > 0) {
                                //jdata = r.getJSONArray("jdata");
                                jdata = r.get("jdata");
                                res = true;
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        if(uiThread)
                            response.onResponse(jdata, null, res);
                        else{
                            final Object fjdata = jdata;
                            final boolean fres = res;
                            mForLightWeightBackgroundTasks.execute(new Runnable() {
                                @Override
                                public void run() {
                                    response.onResponse(fjdata, null, fres);
                                }
                            });
                        }


                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        response.onResponse(null, error, false);
                    }
                }) {

            /**
             * Passing some request headers
             */
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json; charset=utf-8");
                headers.put("authorization", "Basic "+API_USERNAME+":"+API_PASSWORD_TOKEN);
                headers.put("withCredentials", "true");
                return headers;
            }


        };


        addToRequestQueue(jsObjRequest);



    }

}