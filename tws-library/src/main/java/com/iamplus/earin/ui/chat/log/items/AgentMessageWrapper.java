package com.iamplus.earin.ui.chat.log.items;

import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import com.iamplus.earin.R;
import com.zopim.android.sdk.model.items.AgentMessage;
import com.zopim.android.sdk.model.items.RowItem;

/**
 * Class used to display a {@link AgentMessage} as an item in a {@link RecyclerView}.
 * <p>
 * Responsible for binding values to inflated views and to determine if the item needs to be updated.
 */
public class AgentMessageWrapper extends ViewHolderWrapper<AgentMessage> {

    public AgentMessageWrapper(final String messageId, final AgentMessage rowItem) {
        super(ItemType.AGENT_MESSAGE, messageId, rowItem);
    }

    @Override
    public void bind(final RecyclerView.ViewHolder holder) {
        BinderHelper.displayTimeStamp(holder.itemView, getRowItem());

        final TextView textView = holder.itemView.findViewById(R.id.chat_log_message_textview);
        textView.setText(getRowItem().getMessage());
    }

    @Override
    public boolean isUpdated(final RowItem rowItem) {
        return false;
    }
}