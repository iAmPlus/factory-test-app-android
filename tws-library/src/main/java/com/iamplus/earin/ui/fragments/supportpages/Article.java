package com.iamplus.earin.ui.fragments.supportpages;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Article {

    String name;
    String id;
    String title;
    String body;


    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getBody() {
        return body;
    }

    public String getTitle() {
        return title;
    }

    public Article(String name, String id, String title, String body) {
        this.name = name;
        this.id = id;
        this.title = title;
        this.body = body;
    }

    public static ArrayList<Article> fromJson(JSONObject jsonObject) {
        ArrayList<Article> articleArrayList = new ArrayList<>();
        try {
            JSONArray articlesJsonArray = jsonObject.getJSONArray("articles");
            for (int i=0; i<articlesJsonArray.length(); i++) {
                JSONObject sectionJsonObject = articlesJsonArray.getJSONObject(i);
                articleArrayList.add(new Article(
                        sectionJsonObject.getString("name"),
                        sectionJsonObject.getString("id"),
                        sectionJsonObject.getString("title"),
                        sectionJsonObject.getString("body")
                ));
                Log.w("Article", "added");
            }
        } catch (JSONException e) {
            Log.w("Article", "not added");
            e.printStackTrace();
        }

        Log.w("Article", "size: " + articleArrayList.size());
        return articleArrayList;
    }
}
