package com.iamplus.earin.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.iamplus.earin.R;
import com.iamplus.earin.util.SeekBarInputWrapper;

public class SetTransparencyFragment extends BaseFragment implements ViewTreeObserver.OnGlobalLayoutListener {

    public static final int MODE_OFF = 0;
    public static final int MODE_ON = 1;
    public static final int MODE_AUTO = 2;

    private static final String BUNDLE_EXTRA_TITLE = "title";
    private static final String BUNDLE_EXTRA_MODE = "mode";

    private View mRootView;
    private SeekBar mFirstSeekBar;
    private TextView mFirstSeekBarTextView;
    private SeekBar mSecondSeekBar;
    private TextView mSecondSeekBarTextView;
    private SeekBarInputWrapper mFirstSeekBarInputWrapper;
    private SeekBarInputWrapper mSecondSeekBarInputWrapper;

    private ViewGroup mBarsContainer;
    private ViewGroup mBottomTextContainer;
    private TextView mOnTextView;
    private TextView mOffTextView;
    private TextView mAutoTextView;
    private TextView mSeekBar1MaxTextView;
    private TextView mSeekBar1MinTextView;
    private TextView mSeekBar2MaxTextView;
    private TextView mSeekBar2MinTextView;

    private View.OnClickListener buttonOnClickListener = view -> {
        int i = view.getId();
        if (i == R.id.onTextView) {
            changeMode(MODE_ON, true);

        } else if (i == R.id.offTextView) {
            changeMode(MODE_OFF, true);

        } else if (i == R.id.autoTextView) {
            changeMode(MODE_AUTO, true);

        }
    };

    private void changeMode(int mode, boolean sendToDelegate) {
        if (sendToDelegate) {
            SeekBarInputWrapper.changeMode(mode);
        }

        switch (mode) {
            case MODE_ON:
                mOnTextView.setTextColor(getResources().getColor(R.color.black));
                mOffTextView.setTextColor(getResources().getColor(R.color.darkGrey));
                mAutoTextView.setTextColor(getResources().getColor(R.color.darkGrey));

                mFirstSeekBar.setAlpha(1f);
                mSeekBar1MaxTextView.setAlpha(1f);
                mSeekBar1MinTextView.setAlpha(1f);
                mFirstSeekBar.setEnabled(true);

                mSecondSeekBar.setAlpha(1f);
                mSeekBar2MaxTextView.setAlpha(1f);
                mSeekBar2MinTextView.setAlpha(1f);
                mSecondSeekBar.setEnabled(true);
                break;
            case MODE_OFF:
                mOnTextView.setTextColor(getResources().getColor(R.color.darkGrey));
                mOffTextView.setTextColor(getResources().getColor(R.color.black));
                mAutoTextView.setTextColor(getResources().getColor(R.color.darkGrey));

                mFirstSeekBar.setAlpha(0.4f);
                mSeekBar1MaxTextView.setAlpha(0.4f);
                mSeekBar1MinTextView.setAlpha(0.4f);
                mFirstSeekBar.setEnabled(false);

                mSecondSeekBar.setAlpha(0.4f);
                mSeekBar2MaxTextView.setAlpha(0.4f);
                mSeekBar2MinTextView.setAlpha(0.4f);
                mSecondSeekBar.setEnabled(false);
                break;
            case MODE_AUTO:
                mOnTextView.setTextColor(getResources().getColor(R.color.darkGrey));
                mOffTextView.setTextColor(getResources().getColor(R.color.darkGrey));
                mAutoTextView.setTextColor(getResources().getColor(R.color.black));

                mFirstSeekBar.setAlpha(1f);
                mSeekBar1MaxTextView.setAlpha(1f);
                mSeekBar1MinTextView.setAlpha(1f);
                mFirstSeekBar.setEnabled(true);

                mSecondSeekBar.setAlpha(1f);
                mSeekBar2MaxTextView.setAlpha(1f);
                mSeekBar2MinTextView.setAlpha(1f);
                mSecondSeekBar.setEnabled(true);
                break;
        }
    }

    public static SetTransparencyFragment getInstance(String title, int currentMode) {
        SetTransparencyFragment dualSeekBarFragment = new SetTransparencyFragment();

        Bundle args = new Bundle();
        args.putString(BUNDLE_EXTRA_TITLE, title);
        args.putInt(BUNDLE_EXTRA_MODE, currentMode);
        dualSeekBarFragment.setArguments(args);

        return dualSeekBarFragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_set_transparency, container, false);

        initToolbar(mRootView, getArguments().getString(BUNDLE_EXTRA_TITLE, "").toUpperCase());
        mFirstSeekBar = mRootView.findViewById(R.id.seekBar1);
        mSecondSeekBar = mRootView.findViewById(R.id.seekBar2);
        mFirstSeekBarTextView = mRootView.findViewById(R.id.seekBar1TextView);
        mSecondSeekBarTextView = mRootView.findViewById(R.id.seekBar2TextView);

        mFirstSeekBarInputWrapper = SeekBarInputWrapper.getFirstInstance();
        mSecondSeekBarInputWrapper = SeekBarInputWrapper.getSecondInstance();

        mOnTextView = mRootView.findViewById(R.id.onTextView);
        mOffTextView = mRootView.findViewById(R.id.offTextView);
        mAutoTextView = mRootView.findViewById(R.id.autoTextView);

        mSeekBar1MaxTextView = mRootView.findViewById(R.id.seekBar1MaxTextView);
        mSeekBar1MinTextView = mRootView.findViewById(R.id.seekBar1MinTextView);
        mSeekBar2MaxTextView = mRootView.findViewById(R.id.seekBar2MaxTextView);
        mSeekBar2MinTextView = mRootView.findViewById(R.id.seekBar2MinTextView);

        mOnTextView.setOnClickListener(buttonOnClickListener);
        mOffTextView.setOnClickListener(buttonOnClickListener);
        mAutoTextView.setOnClickListener(buttonOnClickListener);

        mBarsContainer = mRootView.findViewById(R.id.barsContainer);
        mBottomTextContainer = mRootView.findViewById(R.id.bottomTextContainer);
        int mode = getArguments().getInt(BUNDLE_EXTRA_MODE, 1);
        changeMode(mode, false);

        mFirstSeekBar.setMax(mFirstSeekBarInputWrapper.getMaxDisplayValue() - mFirstSeekBarInputWrapper.getMinDisplayValue());
        mSecondSeekBar.setMax(mSecondSeekBarInputWrapper.getMaxDisplayValue() - mSecondSeekBarInputWrapper.getMinDisplayValue());

        mFirstSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar arg0) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {
            }

            @Override
            public void onProgressChanged(SeekBar mSeekBar, int progress, boolean arg2) {
                mFirstSeekBarInputWrapper.setCurrentDisplayValue(progress + mFirstSeekBarInputWrapper.getMinDisplayValue());
//                handleSeekBarChange(1);
                SeekBarInputWrapper.save();
            }
        });

        mSecondSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar arg0) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {
            }

            @Override
            public void onProgressChanged(SeekBar mSeekBar, int progress, boolean arg2) {
                mSecondSeekBarInputWrapper.setCurrentDisplayValue(progress + mSecondSeekBarInputWrapper.getMinDisplayValue());
//                handleSeekBarChange(2);
                SeekBarInputWrapper.save();
            }
        });

        ImageButton clearImageButton = mRootView.findViewById(R.id.clearImageButton);
        clearImageButton.setOnClickListener(view -> mActivity.removeLastFragment());


        ImageView helpImageView = mRootView.findViewById(R.id.helpImageView);
        helpImageView.setOnClickListener(view -> mActivity.openFullscreenFragment(new TransparencyHelpFragment()));


        mRootView.getViewTreeObserver().addOnGlobalLayoutListener(this);
        return mRootView;
    }

    @Override
    public void onStop() {
        super.onStop();
        SeekBarInputWrapper.exit();
    }

//    private void handleSeekBarChange(int pos) {
//        if (pos == 1) {
//            Rect thumbBounds = mFirstSeekBar.getThumb().getBounds();
//            mFirstSeekBarTextView.setX(thumbBounds.left + getContext().getResources().getDimension(R.dimen.seek_bar_margin));
//            mFirstSeekBarTextView.setText(String.valueOf(mFirstSeekBarInputWrapper.getCurrentDisplayValue()));
//        } else {
//            Rect thumbBounds = mSecondSeekBar.getThumb().getBounds();
//            mSecondSeekBarTextView.setX(thumbBounds.left + getContext().getResources().getDimension(R.dimen.seek_bar_margin));
//            mSecondSeekBarTextView.setText(String.valueOf(mSecondSeekBarInputWrapper.getCurrentDisplayValue()));
//        }
//    }

    @Override
    public void onGlobalLayout() {
        mRootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
//        handleSeekBarChange(1);
//        handleSeekBarChange(2);
        int bottomDifference = mBarsContainer.getBottom() - mBottomTextContainer.getTop();
        if (bottomDifference > 0) {
            mBarsContainer.setPadding(bottomDifference, 0, 0, 0);
        }

        mFirstSeekBar.setProgress(mFirstSeekBarInputWrapper.getCurrentDisplayValue() - mFirstSeekBarInputWrapper.getMinDisplayValue());
        mSecondSeekBar.setProgress(mSecondSeekBarInputWrapper.getCurrentDisplayValue() - mSecondSeekBarInputWrapper.getMinDisplayValue());
    }
}
