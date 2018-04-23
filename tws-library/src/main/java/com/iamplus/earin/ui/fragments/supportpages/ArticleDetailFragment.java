package com.iamplus.earin.ui.fragments.supportpages;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.iamplus.earin.R;
import com.iamplus.earin.ui.activities.BaseToolbarActivity;
import com.iamplus.earin.ui.fragments.BaseFragment;


public class ArticleDetailFragment extends BaseFragment {

    private static final String ARG_SECTION_NAME = "sectionName";
    private static final String ARG_ARTICLE_NAME = "articleName";
    private static final String ARG_ARTICLE_TITLE = "articleTitle";
    private static final String ARG_ARTICLE_BODY = "articleBody";


    public static ArticleDetailFragment     newInstance(String sectionName, String articleName, String articleTitle, String articleBody) {
        ArticleDetailFragment articleDetailFragment = new ArticleDetailFragment();

        Bundle args = new Bundle();
        args.putString(ARG_SECTION_NAME, sectionName);
        args.putString(ARG_ARTICLE_NAME, articleName);
        args.putString(ARG_ARTICLE_TITLE, articleTitle);
        args.putString(ARG_ARTICLE_BODY, articleBody);
        articleDetailFragment.setArguments(args);

        return articleDetailFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_article_detail, container, false);

        String sectionName = getArguments().getString(ARG_SECTION_NAME);
        String articleTitle = "<h2 style=\"text-align: center; padding: 10px;\">"
                + getArguments().getString(ARG_ARTICLE_TITLE)
                + "</h2>";
        String articleBody = "<div style=\"border-top: 1px solid gray; padding-top: 10px;\">"
                + getArguments().getString(ARG_ARTICLE_BODY)
                + "</div>";

        initToolbar(rootView, sectionName.toUpperCase());
        initToolbarLeftButton(ContextCompat.getDrawable(getActivity(), R.drawable.ic_back), view -> ((BaseToolbarActivity) getActivity()).removeToIndex(0));

        WebView webView = rootView.findViewById(R.id.webView);
        webView.setHorizontalScrollBarEnabled(false);
        String javascriptInject =
                "<script type=\"text/javascript\">\n " +
                    "var imgs = document.getElementsByTagName('img');\n" +
                    "document.getElementsByTagName('body')[0].style.padding= \"15px\"\n" +
                    "for (var i = imgs.length - 1; i >= 0; i--) {\n" +
                        "imgs[i].style.width=\"100%\";\n" +
                        "imgs[i].style.height = \"auto\";\n" +
                    "}\n" +
                "</script>";

        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadDataWithBaseURL(null, articleTitle + articleBody + javascriptInject, "text/html", "utf-8", null);

        return rootView;
    }

}
