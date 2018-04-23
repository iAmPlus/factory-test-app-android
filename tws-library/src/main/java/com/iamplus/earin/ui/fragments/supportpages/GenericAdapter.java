package com.iamplus.earin.ui.fragments.supportpages;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.iamplus.earin.R;

public abstract class GenericAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_support_list, parent, false);
        return new GenericViewHolder(itemView);
    }


    public class GenericViewHolder extends RecyclerView.ViewHolder {

        TextView name;
        LinearLayout container;

        GenericViewHolder(View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.container);
            name = itemView.findViewById(R.id.itemName);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Object item);
    }
}
