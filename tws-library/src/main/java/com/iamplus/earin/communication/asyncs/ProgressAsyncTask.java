package com.iamplus.earin.communication.asyncs;

import android.content.Context;

/**
 * Created by Markus Millfjord on 2017-02-08.
 */
public class ProgressAsyncTask extends CapControlAsyncTask<Void>
{
    private static final String TAG = ProgressAsyncTask.class.getSimpleName();

    private int timeoutSecs;

    public ProgressAsyncTask(String progressMessage, final CapControlAsyncTaskCallback<Void> callback, Context context)
    {
        super(callback, context, progressMessage, false);
        this.timeoutSecs = -1;
    }

    public ProgressAsyncTask(boolean cancelable, int timeoutSecs, String progressMessage, final CapControlAsyncTaskCallback<Void> callback, Context context)
    {
        super(callback, context, progressMessage, cancelable);
        this.timeoutSecs = timeoutSecs;
    }

    @Override
    protected Void performTask() throws Exception
    {
        if (this.timeoutSecs >  0)
        {
            final long start = System.currentTimeMillis();
            final long timeoutMillis = (timeoutSecs * 1000);

            while (System.currentTimeMillis() < (start + timeoutMillis))
            {
                if (isCancelled())
                    break;

                try
                {
                    Thread.sleep(1000);
                }
                catch(Exception ex) {}
            }
        }
        else
        {
            while (true)
            {
                if (isCancelled())
                    break;

                try
                {
                    Thread.sleep(1000);
                }
                catch(Exception ex) {}
            }
        }

        return null;
    }

}