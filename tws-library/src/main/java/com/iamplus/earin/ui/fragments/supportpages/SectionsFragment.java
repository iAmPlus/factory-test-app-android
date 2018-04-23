package com.iamplus.earin.ui.fragments.supportpages;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.iamplus.earin.R;
import com.iamplus.earin.serviceoperations.SupportPagesOperation;
import com.iamplus.earin.ui.activities.BaseToolbarActivity;
import com.iamplus.earin.ui.fragments.BaseFragment;
import com.iamplus.earin.ui.fragments.NoConnectionFragment;
import com.iamplus.earin.util.NetworkUtil;

import java.util.ArrayList;

public class SectionsFragment extends BaseFragment implements SectionAdapter.OnItemClickListener {

    private RecyclerView mSectionsRecyclerView;
    private ProgressBar mFetchingDataProgressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_support_list, container, false);

        initToolbar(rootView, getString(R.string.support_pages).toUpperCase());
        initToolbarLeftButton(ContextCompat.getDrawable(getActivity(), R.drawable.ic_back), view -> ((BaseToolbarActivity) getActivity()).removeToIndex(0));

        mFetchingDataProgressBar = rootView.findViewById(R.id.fetchingDataProgressBar);

        mSectionsRecyclerView = rootView.findViewById(R.id.recyclerView);
        mSectionsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mSectionsRecyclerView.setItemAnimator(new DefaultItemAnimator());

        SupportPagesOperation.getSections(new SupportPagesOperation.GetSectionsHandler() {
            @Override
            public void onSuccess(ArrayList<Section> sections) {
                SectionAdapter sectionsAdapter = new SectionAdapter(sections, SectionsFragment.this);
                mSectionsRecyclerView.setAdapter(sectionsAdapter);
                mFetchingDataProgressBar.setVisibility(View.GONE);
            }

            @Override
            public void onError(int statusCode) {
                Toast.makeText(getActivity(), "ERROR getting sections!", Toast.LENGTH_LONG).show();
            }
        });
        return rootView;
    }

    @Override
    public void onItemClick(Object item) {
        if (NetworkUtil.isInternetAvailable(getActivity())) {
            ((BaseToolbarActivity) getActivity()).openFullscreenFragment(
                    ArticlesFragment.newInstance(
                            ((Section) item).getId(), ((Section) item).getName()));
        } else {
            ((BaseToolbarActivity) getActivity()).openFullscreenFragment(NoConnectionFragment.newInstance(getString(R.string.online_store), getString(R.string.no_connection_store)));
        }
    }
}
