package com.iamplus.earin.communication.asyncs;

/**
 * Created by Markus Millfjord on 2017-02-08.
 */
public interface CapControlAsyncTaskCallback<Result>
{
    public void asyncTaskCompletedSuccessfully(Result result);
    public void asyncTaskFailed(Exception exception);
}
