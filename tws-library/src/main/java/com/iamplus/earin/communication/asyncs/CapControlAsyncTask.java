package com.iamplus.earin.communication.asyncs;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Created by Markus Millfjord on 2017-02-08.
 */
public abstract class CapControlAsyncTask<Result> extends AsyncTask<Void, Void, Void>
{
    private static final String TAG = CapControlAsyncTask.class.getSimpleName();

    private ProgressDialog progressDialog;

    private CapControlAsyncTaskCallback<Result> callback;
    private Exception caughtException;
    private Result result;

    public CapControlAsyncTask(CapControlAsyncTaskCallback<Result> callback, Context context)
    {
        //Kick without progress-dialog/message
        this(callback, context, null);
    }

    public CapControlAsyncTask(CapControlAsyncTaskCallback<Result> callback, Context context, String progressMessage)
    {
        this(callback, context, progressMessage, false);
    }

    public CapControlAsyncTask(final CapControlAsyncTaskCallback<Result> callback, Context context, final String progressMessage, boolean isCancelable)
    {
        Log.d(TAG, "Created");

        this.callback = callback;

        //Setup progress dialog...
        if (progressMessage != null)
        {
            this.progressDialog = new ProgressDialog(context); //R.style.CapControlProgressDialog
            this.progressDialog.setCancelable(isCancelable);
            this.progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener()
            {
                @Override
                public void onCancel(DialogInterface dialogInterface)
                {
                    callback.asyncTaskFailed(new Exception("Canceled"));
                }
            });

            this.progressDialog.setMessage(progressMessage);
        }
    }

    @Override
    protected void onPreExecute()
    {
        super.onPreExecute();

        // Showing progress dialog
        if (this.progressDialog != null)
            this.progressDialog.show();
    }

    @Override
    protected Void doInBackground(Void... voids)
    {
        //Do the work...
        try
        {
            this.result = this.performTask();
        }
        catch (Exception x)
        {
            Log.w(TAG, "Opps, task failed", x);
            this.caughtException = x;
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void result)
    {
        super.onPostExecute(result);

        handleReachEnd();
    }

    @Override
    protected void onCancelled()
    {
        super.onCancelled();
        handleReachEnd();
    }

    @Override
    protected void onCancelled(Void aVoid)
    {
        //super.onCancelled(aVoid);
        handleReachEnd();
    }

    private void handleReachEnd()
    {
        // Dismiss the progress dialog
        if (this.progressDialog != null && this.progressDialog.isShowing())
        {
            this.progressDialog.dismiss();
            //progressDialog = null;
        }

        //Ok -- so what happned? let's report back to our callback - if any...
        if (this.callback != null)
        {
            //So, any result, or did we fail?
            if (this.caughtException != null)
            {
                Log.w(TAG, "Found cought exception --> delegating to callback");
                callback.asyncTaskFailed(this.caughtException);
            }
            else
            {
                callback.asyncTaskCompletedSuccessfully(this.result);
            }
        }
    }

    protected abstract Result performTask() throws Exception;

}
