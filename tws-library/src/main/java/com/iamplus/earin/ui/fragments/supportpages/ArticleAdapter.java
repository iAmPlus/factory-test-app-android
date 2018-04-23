package com.iamplus.earin.ui.fragments.supportpages;

import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;

class ArticleAdapter extends GenericAdapter {

    private ArrayList<Article> mItemsArrayList;
    private SectionAdapter.OnItemClickListener mOnItemClickListener;

    ArticleAdapter(ArrayList<Article> items, SectionAdapter.OnItemClickListener onItemClickListener) {
        this.mItemsArrayList = items;
        this.mOnItemClickListener = onItemClickListener;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final Article item = mItemsArrayList.get(position);
        GenericViewHolder itemHolder = (GenericViewHolder) holder;
        itemHolder.name.setText(item.getName());

        itemHolder.container.setOnClickListener(v -> {
            if (mOnItemClickListener != null) {
                mOnItemClickListener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mItemsArrayList.size();
    }
}
