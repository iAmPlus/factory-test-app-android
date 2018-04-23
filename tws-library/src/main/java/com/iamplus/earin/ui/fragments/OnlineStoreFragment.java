package com.iamplus.earin.ui.fragments;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.iamplus.earin.R;

public class OnlineStoreFragment extends BaseFragment {

    private RelativeLayout mContentContainer;
    private ProgressBar mLoadProgressBar;

    private WebView mWebView;
    private boolean mLoaded;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.frament_online_store, container, false);

        initToolbar(rootView, getString(R.string.online_store).toUpperCase());
        initToolbarLeftButton(ContextCompat.getDrawable(getActivity(), R.drawable.ic_back), view -> mActivity.removeLastFragment());

        mContentContainer = rootView.findViewById(R.id.contentContainer);
        mLoadProgressBar = rootView.findViewById(R.id.loadProgressBar);
        mWebView = new WebView(mActivity);

        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                mWebView.loadUrl(request.getUrl().toString());
                return true;
            }

            @Override
            public void onPageCommitVisible(WebView view, String url) {
                super.onPageCommitVisible(view, url);
                if (!mLoaded) {
                    mContentContainer.addView(mWebView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    mLoadProgressBar.setVisibility(View.GONE);
                    mLoaded = true;
                }
            }
        });

        mWebView.loadUrl("https://store.earin.com/");

        return rootView;
    }
}
