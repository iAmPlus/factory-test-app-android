package com.iamplus.earin.ui.fragments;

import android.graphics.Rect;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.iamplus.earin.R;
import com.iamplus.earin.util.SeekBarInputWrapper;

public class SetBalanceFragment extends BaseFragment implements ViewTreeObserver.OnGlobalLayoutListener {

    public static final int ORIENTATION_HORIZONTAL = 0;
    public static final int ORIENTATION_VERTICAL = 1;

    private static final String BUNDLE_EXTRA_TITLE = "title";
    private static final String BUNDLE_EXTRA_ORIENTATION = "orientation";

    private View mRootView;
    private SeekBar mSeekBar;
    private TextView mSeekBarTextView;

    private SeekBarInputWrapper mSeekBarInputWrapper;

    public static SetBalanceFragment getInstance(String title, int orientation) {

        SetBalanceFragment setBalanceFragment = new SetBalanceFragment();

        Bundle args = new Bundle();
        args.putString(BUNDLE_EXTRA_TITLE, title);
        args.putInt(BUNDLE_EXTRA_ORIENTATION, orientation);
        setBalanceFragment.setArguments(args);

        return setBalanceFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_set_balance, container, false);

        initToolbar(mRootView, getArguments().getString(BUNDLE_EXTRA_TITLE, "").toUpperCase());
        mSeekBar = mRootView.findViewById(R.id.seekBar);
        mSeekBarTextView = mRootView.findViewById(R.id.seekBarTextView);

        mSeekBarInputWrapper = SeekBarInputWrapper.getFirstInstance();

        mSeekBar.setMax(mSeekBarInputWrapper.getMaxDisplayValue() - mSeekBarInputWrapper.getMinDisplayValue());

        if (getArguments().getInt(BUNDLE_EXTRA_ORIENTATION) == ORIENTATION_VERTICAL) {
            ConstraintLayout seekBarContainer = mRootView.findViewById(R.id.seekBarContainer);
            seekBarContainer.setRotation(-90);
            mSeekBarTextView.setRotation(90);
        }

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar arg0) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {
            }

            @Override
            public void onProgressChanged(SeekBar mSeekBar, int progress, boolean arg2) {
                mSeekBarInputWrapper.setCurrentDisplayValue(progress + mSeekBarInputWrapper.getMinDisplayValue());
                SeekBarInputWrapper.save();
                handleSeekBarChange();
            }
        });

        ImageButton clearImageButton = mRootView.findViewById(R.id.clearImageButton);
        clearImageButton.setOnClickListener(view -> mActivity.removeLastFragment());

        mRootView.getViewTreeObserver().addOnGlobalLayoutListener(this);

        return mRootView;
    }

    @Override
    public void onStop() {
        super.onStop();
        SeekBarInputWrapper.exit();
    }

    private void handleSeekBarChange() {
        Rect thumbBounds = mSeekBar.getThumb().getBounds();
        mSeekBarTextView.setX(thumbBounds.left);
        mSeekBarTextView.setText(String.valueOf(String.valueOf(mSeekBarInputWrapper.getCurrentDisplayValue())));
    }

    @Override
    public void onGlobalLayout() {
        mRootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        handleSeekBarChange();
        mSeekBar.setProgress(mSeekBarInputWrapper.getCurrentDisplayValue() - mSeekBarInputWrapper.getMinDisplayValue());
    }
}