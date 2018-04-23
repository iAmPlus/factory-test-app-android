package com.iamplus.earin.ui.chat.log.items;

import android.support.v7.widget.RecyclerView;

import com.zopim.android.sdk.model.items.RowItem;

/**
 * Super class every item in the {@link RecyclerView} inherits from.
 *
 * @param <E> specific type of the {@link RowItem} this class should hold.
 */
public abstract class ViewHolderWrapper<E extends RowItem> {

    private final ItemType itemType;
    private final E rowItem;
    private final String messageId;
    private final long id;

    ViewHolderWrapper(final ItemType type, final String messageId, final E rowItem) {
        this.itemType = type;
        this.rowItem = rowItem;
        this.messageId = messageId;
        this.id = messageId.hashCode();
    }

    public String getMessageId() {
        return messageId;
    }

    E getRowItem() {
        return rowItem;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public long getId() {
        return id;
    }

    public abstract void bind(RecyclerView.ViewHolder holder);

    public abstract boolean isUpdated(RowItem rowItem);
}