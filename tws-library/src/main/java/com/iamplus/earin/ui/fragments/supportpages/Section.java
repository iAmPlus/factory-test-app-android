package com.iamplus.earin.ui.fragments.supportpages;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Section {

    String name;
    String id;

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public Section(String name, String id) {
        this.name = name != null ? name : "NULLL";
        this.id = id;
    }

    public static ArrayList<Section> fromJson(JSONObject jsonObject) {
        ArrayList<Section> sectionArrayList = new ArrayList<>();
        try {
            JSONArray sectionsJsonArray = jsonObject.getJSONArray("sections");
            for (int i=0; i<sectionsJsonArray.length(); i++) {
                JSONObject sectionJsonObject = sectionsJsonArray.getJSONObject(i);
                sectionArrayList.add(new Section(sectionJsonObject.getString("name"),
                        sectionJsonObject.getString("id")));

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return sectionArrayList;
    }
}
