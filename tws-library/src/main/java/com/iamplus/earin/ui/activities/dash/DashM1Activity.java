package com.iamplus.earin.ui.activities.dash;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import com.iamplus.earin.R;
import com.iamplus.earin.ui.activities.BaseToolbarActivity;

public class DashM1Activity extends BaseToolbarActivity {

    private LinearLayout mBalanceLinearLayout;
    private LinearLayout mGainLinearLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_dash_m1);
        super.onCreate(savedInstanceState);

        mBalanceLinearLayout = (LinearLayout) findViewById(R.id.balanceLinearLayout);
        mGainLinearLayout = (LinearLayout) findViewById(R.id.gainLinearLayout);

        mBalanceLinearLayout.setOnClickListener(mPropertyOnClickListener);
        mGainLinearLayout.setOnClickListener(mPropertyOnClickListener);

        showToolbarLeftIcon(true);
    }

    private View.OnClickListener mPropertyOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view.equals(mBalanceLinearLayout)) {
//                openFullscreenFragment(SetBalanceFragment.getInstance(getString(R.string.balance), SetBalanceFragment.ORIENTATION_HORIZONTAL, 5, 10, 3));
            } else if (view.equals(mGainLinearLayout)) {
//                openFullscreenFragment(SetBalanceFragment.getInstance(getString(R.string.gain), SetBalanceFragment.ORIENTATION_HORIZONTAL, 5, 10, 3));
            }
        }
    };
}
