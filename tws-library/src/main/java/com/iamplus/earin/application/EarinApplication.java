package com.iamplus.earin.application;

import android.app.Application;
import android.content.Context;

import com.zendesk.logger.Logger;
import com.zopim.android.sdk.api.ZopimChat;

public class EarinApplication extends Application{

//    private static final String ZOPIM_CHAT_KEY = "MpcNIU7GyIpnUAm0fGq8OsP0qJgNh6yX"; // Mock
    private static final String ZOPIM_CHAT_KEY = "3PQM8O4E6NflsksYjk61aGEp6hcEOaZD";

    private static final long DAY_IN_MILLS = 1000 * 60 * 60 * 24;

    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.setLoggable(true);
        ZopimChat.DefaultConfig defaultConfig = ZopimChat.init(ZOPIM_CHAT_KEY);
        defaultConfig.sessionTimeout(3 * DAY_IN_MILLS);
    }

    public static Context getContext() {
        return mContext;
    }

    public static void setContext(Context context) {
        mContext = context;
    }
}
