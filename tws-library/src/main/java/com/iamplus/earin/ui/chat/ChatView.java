package com.iamplus.earin.ui.chat;

import android.animation.Animator;
import android.app.Activity;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.iamplus.earin.R;
import com.iamplus.earin.communication.Manager;
import com.iamplus.earin.communication.cap.CapCommunicator;
import com.iamplus.earin.ui.chat.log.ChatLogModel;
import com.iamplus.earin.ui.chat.log.ChatLogMvp;
import com.iamplus.earin.ui.chat.log.ChatLogPresenter;
import com.iamplus.earin.ui.chat.log.ChatLogView;
import com.iamplus.earin.util.bluetooth.BleBroadcastUtil;
import com.iamplus.earin.util.firebase.FirebaseUtil;
import com.zendesk.belvedere.BelvedereIntent;
import com.zendesk.belvedere.BelvedereSource;
import com.zendesk.util.StringUtils;
import com.zopim.android.sdk.api.ZopimChatApi;
import com.zopim.android.sdk.model.Account;
import com.zopim.android.sdk.model.items.RowItem;
import com.zopim.android.sdk.util.BelvedereProvider;

import java.util.List;
import java.util.Map;

class ChatView implements ChatMvp.View {

    private ChatMvp.Presenter mPresenter;

    private final Activity activity;

    private final Snackbar mNoConnectionSnackBar;
    private final Snackbar mTimeoutSnackBar;

    private ChatLogMvp.Presenter mChatLogPresenter;

    private View mRootView;
    private ProgressBar mChatProgressBar;
    private ImageView mSendInputImageView;
    private ImageView mMoreOptionsImageView;
    private EditText mChatInputEditText;
    private ConstraintLayout mMoreOptionsLayout;

    private ConstraintLayout mNoAgentsLayout;
    private EditText mUserEmailEditText;
    private EditText mUserNameEditText;
    private EditText mUserMessageEditText;
    private CheckBox mSendDiagnosticsCheckBox;

    private boolean mMoreOptionsLayoutVisible;

    ChatView(final View rootView, Activity activity) {
        this.mRootView = rootView;
        this.activity = activity;

        mChatProgressBar = rootView.findViewById(R.id.chatProgressBar);

        this.mNoConnectionSnackBar = createSnackBar(R.string.snackbar_connection);
        this.mTimeoutSnackBar = createSnackBar(R.string.snackbar_timeout);
    }

    @Override
    public void initChatUi(final FragmentActivity activity) {
        initToolbar();
        initChatLogRecycler();
        initChatSendButton();
        initChatInput();
        initChatAttachmentButtons();
        initNoAgantsLayout();
    }

    @Override
    public <E extends ChatMvp.Presenter> void setPresenter(final E presenter) {
        this.mPresenter = presenter;
    }

    @Override
    public void updateChatLog(final Map<String, RowItem> chatItemMap) {
        mChatLogPresenter.updateChatLog(chatItemMap);
    }

    @Override
    public void setInputEnabled(final boolean enabled) {
        mChatInputEditText.setEnabled(enabled);
        mSendInputImageView.setEnabled(enabled);
        mMoreOptionsImageView.setEnabled(enabled);
    }

    @Override
    public void showLoading(final boolean loading) {
        mChatProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    @Override
    public void connectionChanged(final boolean connected) {
        setInputEnabled(connected);

        if (connected) {
            mNoConnectionSnackBar.dismiss();
            mRootView.setPadding(0, 0, 0, 0);

            Account account = ZopimChatApi.getDataSource().getAccount();
            if (account != null && Account.Status.OFFLINE == account.getStatus()) {
                mNoAgentsLayout.setVisibility(View.VISIBLE);
                mNoAgentsLayout.setAlpha(0f);
                mNoAgentsLayout.animate().alpha(1f).setDuration(500);
                activity.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
            } else {
                mNoAgentsLayout.setVisibility(View.GONE);
                activity.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }
        } else {
            mNoAgentsLayout.setVisibility(View.GONE);
            mNoConnectionSnackBar.show();
            mRootView.setPadding(0, 0, 0, 120);
        }
    }

    @Override
    public void timeout() {
        setInputEnabled(false);
        mTimeoutSnackBar.show();
        mRootView.setPadding(0, 0, 0, 120);
    }

    private void initToolbar() {
        ViewGroup toolbarLayout = mRootView.findViewById(R.id.toolbar);
        TextView toolbarTitleTextView = toolbarLayout.findViewById(R.id.toolbarTitleTextView);

        toolbarTitleTextView.setVisibility(View.VISIBLE);
        toolbarTitleTextView.setText(activity.getString(R.string.support_chat).toUpperCase());
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(toolbarTitleTextView, 10, 18, 1, TypedValue.COMPLEX_UNIT_SP);

        toolbarLayout.findViewById(R.id.toolbarTitleImage).setVisibility(View.GONE);

        ImageButton mLeftImage = toolbarLayout.findViewById(R.id.leftImageButton);
        mLeftImage.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_back));
        mLeftImage.setVisibility(View.VISIBLE);
        mLeftImage.setOnClickListener(view -> activity.finish());
    }

    private void initNoAgantsLayout() {
        mNoAgentsLayout = mRootView.findViewById(R.id.noAgentsLayout);
        mUserEmailEditText = mRootView.findViewById(R.id.userEmailEditText);
        mUserNameEditText = mRootView.findViewById(R.id.userNameEditText);
        mUserMessageEditText = mRootView.findViewById(R.id.userMessageEditText);
        mSendDiagnosticsCheckBox = mRootView.findViewById(R.id.sendDiagnosticsCheckBox);
        if (!BleBroadcastUtil.getInstance(activity).isConnected()) {
            mSendDiagnosticsCheckBox.setVisibility(View.GONE);
        }
        Button sendMessageButton = mRootView.findViewById(R.id.sendMessageButton);
        sendMessageButton.setOnClickListener(view -> {
            if (mSendDiagnosticsCheckBox.isChecked()) {
                sendLog(null);
            }
            sendOfflineMessage();
        });
    }

    private void sendOfflineMessage() {
        String userName = mUserNameEditText.getText().toString().trim();
        String userEmail = mUserEmailEditText.getText().toString().trim();
        String userMessage = mUserMessageEditText.getText().toString().trim();
        if (userName.isEmpty()) {
            Toast.makeText(activity, activity.getString(R.string.enter_name), Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
            Toast.makeText(activity, activity.getString(R.string.enter_email), Toast.LENGTH_SHORT).show();
            return;
        }
        if (userMessage.isEmpty()) {
            Toast.makeText(activity, activity.getString(R.string.enter_message), Toast.LENGTH_SHORT).show();
            return;
        }

        mPresenter.sendOfflineMessage(userName, userEmail, userMessage);
        ConstraintLayout thankYouLayout = mRootView.findViewById(R.id.thankYouLayout);
        thankYouLayout.setVisibility(View.VISIBLE);
        thankYouLayout.setAlpha(0f);
        thankYouLayout.animate().alpha(1f).setDuration(500);
        ImageButton exitImageButton = mRootView.findViewById(R.id.exitImageButton);
        exitImageButton.setOnClickListener(view -> activity.onBackPressed());
    }


    private void initChatAttachmentButtons() {
        mMoreOptionsImageView = mRootView.findViewById(R.id.moreOptionsImageView);
        TextView optionPhotoTextView = mRootView.findViewById(R.id.optionTakePhotoTextView);
        TextView optionChosePhotoTextView = mRootView.findViewById(R.id.optionChoseFromGalleryTextView);
        TextView optionSendDiagnostics = mRootView.findViewById(R.id.optionSendDiagnostics);
        if (!BleBroadcastUtil.getInstance(activity).isConnected()) {
            optionSendDiagnostics.setVisibility(View.GONE);
            mRootView.findViewById(R.id.optionSendDiagnosticsDivider).setVisibility(View.GONE);
        }
        mMoreOptionsLayout = mRootView.findViewById(R.id.moreOptionsLayout);
        mMoreOptionsLayout.setVisibility(View.GONE);

        mMoreOptionsImageView.setOnClickListener(view -> setAdditionalInputVisible(true));

        ImageButton clearImageButton = mRootView.findViewById(R.id.clearImageButton);
        clearImageButton.setOnClickListener(view -> setAdditionalInputVisible(false));

        optionSendDiagnostics.setOnClickListener(view -> sendLog(new FirebaseUtil.FirebaseLogListener() {
            @Override
            public void onSuccess(String macAddress) {
                mPresenter.sendMessage(activity.getString(R.string.chat_diagnostics_sent, macAddress));
                setAdditionalInputVisible(false);
            }

            @Override
            public void onError(String message) {
                mPresenter.sendMessage(activity.getString(R.string.chat_diagnostics_sending_fail));
                setAdditionalInputVisible(false);
            }
        }));

        optionPhotoTextView.setOnClickListener(view -> {
            final List<BelvedereIntent> belvedereIntents = BelvedereProvider.INSTANCE
                    .getInstance(activity)
                    .getBelvedereIntents();

            for (BelvedereIntent intent : belvedereIntents) {
                if (intent.getSource() == BelvedereSource.Camera) {
                    intent.open(activity);
                }
            }
        });

        optionChosePhotoTextView.setOnClickListener(view -> {
            final List<BelvedereIntent> belvedereIntents = BelvedereProvider.INSTANCE
                    .getInstance(activity)
                    .getBelvedereIntents();

            for (BelvedereIntent intent : belvedereIntents) {
                if (intent.getSource() == BelvedereSource.Gallery) {
                    intent.open(activity);
                }
            }
        });
    }

    private void sendLog(FirebaseUtil.FirebaseLogListener listener) {
        Manager manager = Manager.getSharedManager();
        CapCommunicator communicator = Manager.getSharedManager().getCapCommunicationController().getConnectedCommunicator();
        manager.enqueRequest("lastSessionData", () -> {
            try {
                String data = communicator.getLastSessionData();
                String version = communicator.getVersion();
                FirebaseUtil.getInstance().sendLog(activity, data, version, listener);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    void setAdditionalInputVisible(final boolean visible) {
        if (visible) {
            mMoreOptionsLayout.setVisibility(View.VISIBLE);
            mMoreOptionsLayout.setAlpha(0f);

            mMoreOptionsLayout.animate()
                    .alpha(1.0f)
                    .setDuration(300)
                    .setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animator) {
                        }

                        @Override
                        public void onAnimationEnd(Animator animator) {
                            setInputEnabled(false);
                            mMoreOptionsLayoutVisible = true;
                        }

                        @Override
                        public void onAnimationCancel(Animator animator) {
                        }

                        @Override
                        public void onAnimationRepeat(Animator animator) {
                        }
                    });
        } else {
            mMoreOptionsLayout.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animator) {
                        }

                        @Override
                        public void onAnimationEnd(Animator animator) {
                            mMoreOptionsLayout.setVisibility(View.GONE);
                            setInputEnabled(true);
                            mMoreOptionsLayoutVisible = false;
                        }

                        @Override
                        public void onAnimationCancel(Animator animator) {
                        }

                        @Override
                        public void onAnimationRepeat(Animator animator) {
                        }
                    });
        }
    }

    boolean isMoreOptionsLayoutVisible() {
        return mMoreOptionsLayoutVisible;
    }

    private void initChatInput() {
        mChatInputEditText = mRootView.findViewById(R.id.chatInputEditText);
        mChatInputEditText.setEnabled(false);
        mChatInputEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence charSequence, final int i, final int i1, final int i2) {
            }

            @Override
            public void onTextChanged(final CharSequence charSequence, final int i, final int i1, final int i2) {
                if (StringUtils.isEmpty(mChatInputEditText.getText().toString())) {
                    mSendInputImageView.setVisibility(View.GONE);
                } else {
                    mSendInputImageView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(final Editable editable) {
            }
        });
    }

    private void initChatSendButton() {
        mSendInputImageView = mRootView.findViewById(R.id.sendInputImageView);
        mSendInputImageView.setVisibility(View.GONE);
        mSendInputImageView.setOnClickListener(view -> {
            mPresenter.sendMessage(mChatInputEditText.getText().toString());
            mChatInputEditText.setText("");
        });
    }

    private void initChatLogRecycler() {
        final RecyclerView chatRecyclerView = mRootView.findViewById(R.id.chatRecyclerView);

        final RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(activity);
        chatRecyclerView.setLayoutManager(layoutManager);

        final ChatLogView chatLogAdapter = new ChatLogView(layoutManager); // view
        final ChatLogMvp.Model model = new ChatLogModel(ZopimChatApi.getDataSource()); // model

        mChatLogPresenter = new ChatLogPresenter(chatLogAdapter, model); // presenter
        chatLogAdapter.setPresenter(mChatLogPresenter);

        chatRecyclerView.setAdapter(chatLogAdapter);

        chatRecyclerView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom) {
                chatRecyclerView.postDelayed(chatLogAdapter::scrollToLastMessage, 100);
            }
        });
    }


    private Snackbar createSnackBar(int titleId) {
        final String title = activity.getString(titleId);
        final Snackbar snackbar = Snackbar.make(mChatProgressBar, title, Snackbar.LENGTH_INDEFINITE);
        snackbar.getView().setBackgroundColor(ContextCompat.getColor(activity, R.color.black30));
        return snackbar;
    }
}
