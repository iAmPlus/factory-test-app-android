package com.iamplus.earin.ui.fragments;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.widget.TextViewCompat;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.iamplus.earin.R;
import com.iamplus.earin.ui.activities.BaseToolbarActivity;

public class BaseFragment extends Fragment {

    protected BaseToolbarActivity mActivity;
    private ViewGroup mToolbarLayout;

    //Called on API 23 and above
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof BaseToolbarActivity) {
            mActivity = (BaseToolbarActivity) context;
        } else {
            throw new RuntimeException("NO PARENT ACTIVITY!");
        }
    }

    //Called on API 22 and below
    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mActivity = (BaseToolbarActivity) activity;
            if (mActivity == null) {
                throw new RuntimeException("NO PARENT ACTIVITY!");
            }
        }
    }


    protected void initToolbar(View rootView, String name) {
        mToolbarLayout = rootView.findViewById(R.id.toolbar);
        TextView toolbarTitleTextView = mToolbarLayout.findViewById(R.id.toolbarTitleTextView);

        toolbarTitleTextView.setVisibility(View.VISIBLE);
        toolbarTitleTextView.setText(name);
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(toolbarTitleTextView, 10, 18, 1, TypedValue.COMPLEX_UNIT_SP);
        mToolbarLayout.findViewById(R.id.toolbarTitleImage).setVisibility(View.GONE);
    }
    protected void initToolbarLeftButton(Drawable drawable, View.OnClickListener onClickListener) {
        ImageButton leftImage = mToolbarLayout.findViewById(R.id.leftImageButton);
        leftImage.setImageDrawable(drawable);
        leftImage.setVisibility(View.VISIBLE);
        leftImage.setOnClickListener(onClickListener);
    }

}
