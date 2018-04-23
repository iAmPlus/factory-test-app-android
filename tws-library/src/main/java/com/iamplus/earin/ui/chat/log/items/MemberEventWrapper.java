package com.iamplus.earin.ui.chat.log.items;

import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import com.iamplus.earin.R;
import com.zopim.android.sdk.model.items.ChatMemberEvent;
import com.zopim.android.sdk.model.items.RowItem;


public class MemberEventWrapper extends ViewHolderWrapper<ChatMemberEvent> {

    MemberEventWrapper(final String messageId, final ChatMemberEvent rowItem) {
        super(ItemType.MEMBER_EVENT, messageId, rowItem);
    }

    @Override
    public void bind(final RecyclerView.ViewHolder holder) {
        final TextView textView = holder.itemView.findViewById(R.id.chat_log_event_message_textview);
        textView.setText("Agent: " + getRowItem().getMessage());
    }

    @Override
    public boolean isUpdated(final RowItem rowItem) {
        return false;
    }
}
