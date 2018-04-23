package com.iamplus.earin.ui.activities;

import android.animation.Animator;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


import com.iamplus.earin.R;
import com.iamplus.earin.ui.fragments.MainMenuFragment;
import com.iamplus.earin.ui.fragments.PowerOffFragment;

import java.util.ArrayList;

import jp.wasabeef.blurry.Blurry;

public abstract class BaseToolbarActivity extends AppCompatActivity implements ViewTreeObserver.OnGlobalLayoutListener {

    protected ViewGroup mContainer;
    private TextView mToolbarTitleTextView;
    private ImageView mToolbarTitleImage;
    private ImageButton mLeftImageButton;
    private ImageButton mRightImageButton;

    protected ArrayList<LinearLayout> mWrapperLinearLayoutArrayList;
    protected ArrayList<Fragment> mFragmentsArrayList;
    private ImageView mBlurImageView;

    private int mContainerWidth;
    private int mContainerHeight;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewGroup toolbarLayout = findViewById(R.id.toolbar);
        mToolbarTitleTextView = toolbarLayout.findViewById(R.id.toolbarTitleTextView);
        mToolbarTitleImage = toolbarLayout.findViewById(R.id.toolbarTitleImage);
        mLeftImageButton = toolbarLayout.findViewById(R.id.leftImageButton);
        mRightImageButton = toolbarLayout.findViewById(R.id.rightImageButton);

        mLeftImageButton.setOnClickListener(mToolbarButtonOnClickListener);
        mRightImageButton.setOnClickListener(mToolbarButtonOnClickListener);

        mContainer = findViewById(R.id.container);
        mContainer.getViewTreeObserver().addOnGlobalLayoutListener(this);
        mWrapperLinearLayoutArrayList = new ArrayList<>();
        mFragmentsArrayList = new ArrayList<>();
    }

    public void setToolbarTitle(String title) {
        mToolbarTitleTextView.setVisibility(View.VISIBLE);
        mToolbarTitleTextView.setText(title);
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(mToolbarTitleTextView, 10, 18, 1, TypedValue.COMPLEX_UNIT_SP);

        mToolbarTitleImage.setVisibility(View.GONE);
    }

    public void showToolbarRightIcon(boolean show) {
        if (show) {
            mRightImageButton.setVisibility(View.VISIBLE);
        } else {
            mRightImageButton.setVisibility(View.INVISIBLE);
        }
    }

    public void showToolbarLeftIcon(boolean show) {
        if (show) {
            mLeftImageButton.setVisibility(View.VISIBLE);
//            mAddonCircleView.setVisibility(View.VISIBLE);
        } else {
            mLeftImageButton.setVisibility(View.INVISIBLE);
        }
    }

    protected View.OnClickListener mToolbarButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view.equals(mLeftImageButton)) {
//                SeekBarInputWrapper.createFirst(-3, -14, 0, 7);
//                SeekBarInputWrapper.createSecond(-1, -14, 0, 2);
//                openFullscreenFragment(new SetTransparencyFragment());
                openFullscreenFragment(new MainMenuFragment());
            } else if (view.equals(mRightImageButton)) {
                openFullscreenFragment(new PowerOffFragment());
            }
        }
    };

    public void openFullscreenFragment(Fragment fragment) {

        LinearLayout wrapperLinearLayout = new LinearLayout(this);
        wrapperLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
        if (mWrapperLinearLayoutArrayList.size() % 2 == 1) {
            wrapperLinearLayout.setId(R.id.fragmentOverlay1);
        } else {
            wrapperLinearLayout.setId(R.id.fragmentOverlay2);
        }
        wrapperLinearLayout.setZ(20);

        wrapperLinearLayout.setLayoutParams(
                new ViewGroup.LayoutParams(mContainerWidth, mContainerHeight));

        getSupportFragmentManager().beginTransaction().add(wrapperLinearLayout.getId(), fragment).commit();

        if (mWrapperLinearLayoutArrayList.size() != 0) {
            int lastPos = mWrapperLinearLayoutArrayList.size() - 1;
            removeViewWithoutAnimation(mContainer, mWrapperLinearLayoutArrayList.get(lastPos));
        } else {
            applyBlur();
        }

        addViewWithAnimation(mContainer, wrapperLinearLayout);

        mWrapperLinearLayoutArrayList.add(wrapperLinearLayout);
        mFragmentsArrayList.add(fragment);
    }

    public void removeLastFragment() {
        if (mWrapperLinearLayoutArrayList.size() == 0) return;
        int lastIndex = mWrapperLinearLayoutArrayList.size() - 1;
        LinearLayout wrapperLinearLayout = mWrapperLinearLayoutArrayList.get(lastIndex);
        removeViewWithAnimation(mContainer, wrapperLinearLayout, mFragmentsArrayList.remove(lastIndex));
        mWrapperLinearLayoutArrayList.remove(lastIndex);
        if (lastIndex > 0) {
            addViewWithAnimation(mContainer, mWrapperLinearLayoutArrayList.get(lastIndex - 1));
        } else {
            removeBlur();
        }
    }

    public void removeToIndex(int index) {
        int lastIndex = mWrapperLinearLayoutArrayList.size() - 1;
        LinearLayout wrapperLinearLayout = mWrapperLinearLayoutArrayList.get(lastIndex);
        removeViewWithAnimation(mContainer, wrapperLinearLayout, mFragmentsArrayList.get(lastIndex));

        addViewWithAnimation(mContainer, mWrapperLinearLayoutArrayList.get(index));
        while (index < mWrapperLinearLayoutArrayList.size() - 1) {
            mWrapperLinearLayoutArrayList.remove(mWrapperLinearLayoutArrayList.size() - 1);
            mFragmentsArrayList.remove(mFragmentsArrayList.size() - 1);
        }
    }

    @Override
    public void onGlobalLayout() {
        mContainerWidth = mContainer.getMeasuredWidth();
        mContainerHeight = mContainer.getMeasuredHeight();
        mContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
    }

    @Override
    public void onBackPressed() {
        if (mWrapperLinearLayoutArrayList.size() > 0) {
            removeLastFragment();
        } else {
            super.onBackPressed();
        }
    }

    protected void applyBlur() {
        mBlurImageView = new ImageView(this);
        mBlurImageView.setLayoutParams(
                new ViewGroup.LayoutParams(mContainerWidth, mContainerHeight));
        mBlurImageView.setZ(10);
        Blurry.with(this).capture(mContainer).into(mBlurImageView);
        addViewWithAnimation(mContainer, mBlurImageView);
    }

    protected void removeBlur() {
        Blurry.delete(mContainer);
        removeViewWithAnimation(mContainer, mBlurImageView, null);
    }

    private void addViewWithAnimation(ViewGroup parent, final View child) {
        child.setAlpha(0f);
        parent.addView(child);

        child.animate()
                .alpha(1.0f)
                .setDuration(500);
    }

    private void removeViewWithoutAnimation(ViewGroup parent, View child) {
        parent.removeView(child);
    }

    private void removeViewWithAnimation(final ViewGroup parent, final View child, final Fragment childFragment) {
        child.animate()
                .alpha(0f)
                .setDuration(500)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animator) {
                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        if (childFragment != null) {
                            getSupportFragmentManager().beginTransaction().remove(childFragment).commit();
                        }
                        parent.removeView(child);
                    }

                    @Override
                    public void onAnimationCancel(Animator animator) {
                    }

                    @Override
                    public void onAnimationRepeat(Animator animator) {
                    }
                });
    }
}