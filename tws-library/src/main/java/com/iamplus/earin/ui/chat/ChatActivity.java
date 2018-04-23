package com.iamplus.earin.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.iamplus.earin.R;
import com.zendesk.belvedere.BelvedereCallback;
import com.zendesk.belvedere.BelvedereResult;
import com.zopim.android.sdk.api.ChatApi;
import com.zopim.android.sdk.api.ZopimChatApi;
import com.zopim.android.sdk.util.BelvedereProvider;

import java.util.List;


public class ChatActivity extends AppCompatActivity {

    private ChatMvp.Presenter presenter;
    private ChatView chatView;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        ChatApi chat = ZopimChatApi.resume(this);
        if (chat.hasEnded()) {
            chat = ZopimChatApi.start(this);
        }

        final ChatMvp.Model model = new ChatModel(chat, ZopimChatApi.getDataSource(), getApplicationContext());
        chatView = new ChatView(findViewById(R.id.chat_root_container), this);

        presenter = new ChatPresenter(model, chatView);
        chatView.setPresenter(presenter);
        presenter.install(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.onDestroy();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        BelvedereProvider
                .INSTANCE
                .getInstance(this)
                .getFilesFromActivityOnResult(requestCode, resultCode, data, new BelvedereCallback<List<BelvedereResult>>() {
                    @Override
                    public void success(final List<BelvedereResult> belvedereResults) {
                        presenter.sendFile(belvedereResults);
                        chatView.setAdditionalInputVisible(false);
                    }
                });
    }

    @Override
    public void onBackPressed() {
        if (chatView.isMoreOptionsLayoutVisible()) {
            chatView.setAdditionalInputVisible(false);
        } else {
            super.onBackPressed();
            presenter.chatDismissed();
        }
    }
}
