package com.iamplus.systemupdater.task;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.iamplus.systemupdater.Config;
import com.iamplus.systemupdater.Mock;
import com.iamplus.systemupdater.Utils;
import com.iamplus.systemupdater.UpdateInfo;
import com.iamplus.systemupdater.UpdateManager;

public class QueryUpdatesTask extends BaseTask {

    private JSONObject mJson;
    private boolean mIgnore = false;
    private String from_version;

    @Override
    public void onPreExecute() {
        from_version = getParams().getExtras().getString(Config.UPDATER_VERSION);
        switch (getState()) {
            case IDLE:
            case QUERYING_UPDATE:
            case NO_UPDATES:
            case UPDATE_DONE:
            case ERROR:
                mIgnore = false;
                break;
            default:
                mIgnore = true;
                log("Cancelling query update state " + getState());
                cancel(true);
        }
    }

    public boolean doInBackground() {
        if (Config.getBaseUrl(getContext()) == null) {
            log("Query url is null. Will retry ..");
            return false;
        }

        log("Query updates for " + from_version);
        try {
            String url = Config.getQueryUrl(getContext());
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("from_version", from_version);
            String data;
            if (Mock.MOCK) {
                log("MOCKing data **");
                data = Mock.getUpdates(getContext());
            } else {
                data = Utils.makeHttpGetWithParams(url, params);
            }
            mJson = new JSONObject(data);
        } catch (IOException e) {
            log_exception("IOException while queryUpdates ..", e);
            return false;
        } catch (JSONException e) {
            log_exception("JSONException while queryUpdates ..", e);
            return false;
        }
        return true;
    }

    public boolean onPostUi(boolean result) {
        if (!result) {
            log("QueryUpdatesTask failed");
            return mUpdateManager.onQueryFailed();
        }
        mUpdateManager.updateLastCheckDate();
        if (!mJson.has("download_url")) {
            mUpdateManager.onNoUpdatesAvailable();
        } else {
            UpdateInfo ui = new UpdateInfo();
            try {
                ui.loadFromJson(mJson);
                mUpdateManager.onUpdateAvailable(ui);
            } catch (JSONException e) {
                log_exception("JSONException while loading UpdateInfo " + mJson, e);
                mUpdateManager.onNoUpdatesAvailable();
            }
        }
        return true;
    }

    @Override
    public void onCancelled(Boolean result) {
        if (mIgnore) {
            log("Ignoring ..");
            return;
        }
        log("QueryUpdatesTask cancelled");
        mUpdateManager.onQueryCancelled();
    }
}