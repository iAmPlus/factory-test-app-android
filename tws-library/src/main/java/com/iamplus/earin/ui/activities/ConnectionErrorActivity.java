package com.iamplus.earin.ui.activities;

import android.os.Bundle;

import com.iamplus.earin.R;

public class ConnectionErrorActivity extends BaseToolbarActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_connection_error);
        super.onCreate(savedInstanceState);

        setToolbarTitle("Error");
        showToolbarLeftIcon(true);
    }
}
