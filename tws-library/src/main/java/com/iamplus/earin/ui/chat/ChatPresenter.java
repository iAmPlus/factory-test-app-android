package com.iamplus.earin.ui.chat;

import android.support.v4.app.FragmentActivity;

import com.zendesk.belvedere.BelvedereResult;
import com.zendesk.util.CollectionUtils;
import com.zopim.android.sdk.model.Connection;
import com.zopim.android.sdk.model.items.RowItem;

import java.util.List;
import java.util.Map;

/**
 * Bridge code between {@link ChatPresenter} and {@link ChatView}.
 */
class ChatPresenter implements ChatMvp.Presenter {

    private final ChatMvp.Model model;
    private final ChatMvp.View view;

    // keep a strong reference
    @SuppressWarnings("FieldCanBeLocal")
    private ChatMvp.Model.ChatListener chatListener;

    ChatPresenter(final ChatMvp.Model model, final ChatMvp.View view) {
        this.model = model;
        this.view = view;
    }

    @Override
    public void install(FragmentActivity fragmentActivity) {
        chatListener = new MyChatListener();
        model.registerChatListener(chatListener);

        view.initChatUi(fragmentActivity);
        view.setInputEnabled(false);
        view.showLoading(true);
    }

    @Override
    public void sendMessage(final String message) {
        model.sendMessage(message);
    }

    @Override
    public void sendOfflineMessage(String userName, String email, String message) {
        model.sendOfflineMessage(userName, email, message);
    }

    @Override
    public void sendFile(final List<BelvedereResult> belvedereResult) {
        if (CollectionUtils.isNotEmpty(belvedereResult)) {
            model.sendAttachment(belvedereResult.get(0).getFile());
        }
    }

    @Override
    public void onDestroy() {
        model.unregisterChatListener();
    }

    @Override
    public void chatDismissed() {
        model.unregisterChatListener();
        model.clearChatIfEnded();
    }


    private class MyChatListener implements ChatMvp.Model.ChatListener {

        @Override
        public void onUpdateChatLog(final Map<String, RowItem> chatItems) {
            view.updateChatLog(chatItems);
        }

        @Override
        public void onUpdateConnection(final Connection connection) {
            switch (connection.getStatus()) {
                case NO_CONNECTION: {
                    view.connectionChanged(false);
                    break;
                }
                case CONNECTED: {
                    view.connectionChanged(true);
                    view.showLoading(false);
                    break;
                }
            }
        }

        @Override
        public void onTimeout() {
            view.timeout();
            model.unregisterChatListener();
        }
    }
}
