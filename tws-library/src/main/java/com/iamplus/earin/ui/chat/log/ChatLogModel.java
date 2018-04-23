package com.iamplus.earin.ui.chat.log;


import android.support.v7.widget.RecyclerView;

import com.iamplus.earin.R;
import com.iamplus.earin.application.EarinApplication;
import com.iamplus.earin.ui.chat.log.items.AgentMessageWrapper;
import com.iamplus.earin.ui.chat.log.items.ItemFactory;
import com.iamplus.earin.ui.chat.log.items.ViewHolderWrapper;
import com.zopim.android.sdk.data.DataSource;
import com.zopim.android.sdk.model.items.AgentMessage;
import com.zopim.android.sdk.model.items.RowItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Model class for representing, updating and holding the internal state of the {@link RecyclerView}.
 */
public class ChatLogModel implements ChatLogMvp.Model {

    private final List<ViewHolderWrapper> listItems;
    private final DataSource dataSource;

    public ChatLogModel(DataSource dataSource) {
        this.listItems = new ArrayList<>();
        this.listItems.add(getIntroMessage());
        this.dataSource = dataSource;
    }

    private AgentMessageWrapper getIntroMessage() {
        AgentMessage agentMessage = new AgentMessage();
        agentMessage.setMessage(EarinApplication.getContext().getString(R.string.support_chat_intro_message));
        return new AgentMessageWrapper("0", agentMessage);
    }

    @Override
    public ViewHolderWrapper getViewHolderWrapperForPos(final int position) {
        return listItems.get(position);
    }

    @Override
    public int getItemCount() {
        return listItems.size();
    }

    @Override
    public ChatLogUpdateResult updateChatLog(final Map<String, RowItem> chatItemMap) {
        final List<ViewHolderWrapper> viewHolderWrappers = new ArrayList<>(listItems);
        final List<ViewHolderWrapper> updateHolderWrapper = new ArrayList<>();

        final List<Integer> updatedViewIndices = new ArrayList<>();
        final List<Integer> insertedIndices = new ArrayList<>();

        boolean unableToDoIncrementalUpdate = false;

        if (ItemFactory.countUsedMessages(chatItemMap.values()) < viewHolderWrappers.size()) {
            viewHolderWrappers.clear();
            unableToDoIncrementalUpdate = true;
        }

        for (Map.Entry<String, RowItem> entry : chatItemMap.entrySet()) {
            final ViewHolderWrapper holderById = findHolderById(viewHolderWrappers, entry.getKey());

            if (holderById != null && holderById.isUpdated(entry.getValue())) {
                // Update an existing item
                final ViewHolderWrapper wrapper = ItemFactory.get(entry.getKey(), entry.getValue(), dataSource.getAgents());

                if (wrapper != null) {
                    updateHolderWrapper.add(wrapper);
                    updatedViewIndices.add(updateHolderWrapper.size() - 1);
                } else {
                    // Not able to update item, remove from list
                    // we have to update the whole list
                    unableToDoIncrementalUpdate = true;
                }

            } else if (holderById != null) {
                // Carry over an existing item
                updateHolderWrapper.add(holderById);

            } else {
                // New item, create and insert
                final ViewHolderWrapper wrapper = ItemFactory.get(entry.getKey(), entry.getValue(), dataSource.getAgents());
                if (wrapper != null) {
                    updateHolderWrapper.add(wrapper);
                    insertedIndices.add(updateHolderWrapper.size() - 1);
                }
            }
        }

        listItems.clear();
        listItems.add(getIntroMessage());
        listItems.addAll(updateHolderWrapper);

        if (unableToDoIncrementalUpdate) {
            return ChatLogUpdateResult.create();
        } else {
            return ChatLogUpdateResult.create(updatedViewIndices, insertedIndices);
        }
    }

    private ViewHolderWrapper findHolderById(List<ViewHolderWrapper> list, String id) {
        for (ViewHolderWrapper wrapper : list) {
            if (wrapper.getMessageId().equals(id)) {
                return wrapper;
            }
        }
        return null;
    }

}
