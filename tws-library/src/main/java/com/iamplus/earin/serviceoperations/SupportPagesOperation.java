package com.iamplus.earin.serviceoperations;

import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.iamplus.earin.application.EarinApplication;
import com.iamplus.earin.ui.fragments.supportpages.Article;
import com.iamplus.earin.ui.fragments.supportpages.Section;

import org.json.JSONObject;

import java.util.ArrayList;

public class SupportPagesOperation extends BaseServiceOperation {

    private static final String TAG = SupportPagesOperation.class.getSimpleName();

    private static final String GET_SECTIONS_URL = "https://epickal.zendesk.com/api/v2/help_center/en-us/categories/115000449889/sections.json";
    private static final String GET_ARTICLES_URL = "https://epickal.zendesk.com/api/v2/help_center/en-us/sections/{sectionID}/articles.json";

    public static void getSections(final GetSectionsHandler getSectionsHandler) {

        RequestQueue queue = Volley.newRequestQueue(EarinApplication.getContext());

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (GET_SECTIONS_URL, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        ArrayList<Section> sectionArrayList = Section.fromJson(response);
                        if (sectionArrayList.size() == 0) {
                            getSectionsHandler.onError(204);
                        } else {
                            getSectionsHandler.onSuccess(sectionArrayList);
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        getSectionsHandler.onError(error.networkResponse.statusCode);
                        error.printStackTrace();

                    }
                });
        queue.add(jsonObjectRequest);
    }

    public static void getArticles(String sectionId, final GetArticlesHandler getArticlesHandler) {
        String url = GET_ARTICLES_URL.replace("{sectionID}", sectionId);
        Log.w(TAG, "Get articles url: " + url);
        RequestQueue queue = Volley.newRequestQueue(EarinApplication.getContext());

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        ArrayList<Article> articlesList = Article.fromJson(response);
                        if (articlesList.size() == 0) {
                            getArticlesHandler.onError(204);
                        } else {
                            getArticlesHandler.onSuccess(articlesList);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("SupportPagesOperation", "Error getting data: " + error.networkResponse.statusCode);
                        getArticlesHandler.onError(error.networkResponse.statusCode);
                    }
                });
        queue.add(jsonObjectRequest);
    }

    public interface GetSectionsHandler extends BaseOperationHandler {
        void onSuccess(ArrayList<Section> sections);
    }

    public interface GetArticlesHandler extends BaseOperationHandler {
        void onSuccess(ArrayList<Article> articles);
    }
}
