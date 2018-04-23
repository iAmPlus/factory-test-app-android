package com.iamplus.earin.ui.fragments.supportpages;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;


import com.iamplus.earin.R;
import com.iamplus.earin.serviceoperations.SupportPagesOperation;
import com.iamplus.earin.ui.activities.BaseToolbarActivity;
import com.iamplus.earin.ui.fragments.BaseFragment;

import java.util.ArrayList;


public class ArticlesFragment extends BaseFragment implements SectionAdapter.OnItemClickListener {

    private static final String ARG_SECTION_ID = "sectionId";
    private static final String ARG_SECTION_NAME = "sectionName";

    private RecyclerView mArticlesRecyclerView;
    private ProgressBar mFetchingDataProgressBar;

    private String mSectionName;

    public static ArticlesFragment newInstance(String sectionId, String sectionName) {
        ArticlesFragment articlesFragment = new ArticlesFragment();

        Bundle args = new Bundle();
        args.putString(ARG_SECTION_ID, sectionId);
        args.putString(ARG_SECTION_NAME, sectionName);
        articlesFragment.setArguments(args);

        return articlesFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_support_list, container, false);

        String sectionId = getArguments().getString(ARG_SECTION_ID);
        mSectionName = getArguments().getString(ARG_SECTION_NAME, "");

        initToolbar(rootView, mSectionName.toUpperCase());
        initToolbarLeftButton(ContextCompat.getDrawable(getActivity(), R.drawable.ic_back), view -> ((BaseToolbarActivity) getActivity()).removeToIndex(0));

        mFetchingDataProgressBar = rootView.findViewById(R.id.fetchingDataProgressBar);

        mArticlesRecyclerView = rootView.findViewById(R.id.recyclerView);
        mArticlesRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mArticlesRecyclerView.setItemAnimator(new DefaultItemAnimator());
        SupportPagesOperation.getArticles(sectionId, new SupportPagesOperation.GetArticlesHandler() {
            @Override
            public void onSuccess(ArrayList<Article> articles) {
                Log.w("ArticlesFragment", "size: " + articles.size());
                ArticleAdapter articleAdapter = new ArticleAdapter(articles, ArticlesFragment.this);
                mArticlesRecyclerView.setAdapter(articleAdapter);
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
        Article article = (Article) item;

        ((BaseToolbarActivity) getActivity()).openFullscreenFragment(
                ArticleDetailFragment.newInstance(
                        mSectionName,
                        article.getName(),
                        article.getTitle(),
                        article.getBody()));
    }

}
