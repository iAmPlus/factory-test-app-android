package com.iamplus.systemupdater.ui;

import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.iamplus.systemupdater.R;
import com.iamplus.systemupdater.UpdateInfo;
import com.iamplus.systemupdater.Utils;

public class DownloadUi extends BaseUi {

    private ProgressBar mProgressBar;
    private TextView mDownloadSize;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.download, container, false);

        mProgressBar = (ProgressBar) v.findViewById(R.id.download_progress);
        mProgressBar.setIndeterminate(false);
        mDownloadSize = (TextView) v.findViewById(R.id.download_size);

        setProgress(getUpdateManager().getDownloadPercentage(),
                getUpdateManager().getDownloadSize());
        return v;
    }

    public void setProgress(int progress, int downloadSize) {
        if (progress == -1) {
            mProgressBar.setIndeterminate(true);
            mDownloadSize.setVisibility(View.GONE);
        } else {
            mProgressBar.setIndeterminate(false);
            mProgressBar.setProgress(progress);
            mDownloadSize.setVisibility(View.VISIBLE);
            setSize(downloadSize);
        }
    }

    private void setSize(int size) {
        UpdateInfo ui = getUpdateManager().getUpdateInfo();
        int totalSize = ui != null ? ui.size : 0;
        String msg = Utils.readableFileSize(size) + " / " + Utils.readableFileSize(totalSize);
        mDownloadSize.setText(msg);
    }
}
