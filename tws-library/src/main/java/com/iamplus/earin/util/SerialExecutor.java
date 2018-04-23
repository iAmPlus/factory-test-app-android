package com.iamplus.earin.util;

import android.support.annotation.NonNull;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SerialExecutor implements Executor {
    private final Queue<Runnable> mTasks = new ArrayDeque<>();
    private final Executor mExecutor;
    private Runnable mActiveRunnable;
    private static SerialExecutor mInstance;

    public synchronized static SerialExecutor getInstance() {
        if (mInstance == null) {
            mInstance = new SerialExecutor();
        }

        return mInstance;
    }

    private SerialExecutor() {
        this.mExecutor = Executors.newFixedThreadPool(1);
    }

    public synchronized void execute(@NonNull final Runnable r) {
        mTasks.add(() -> {
            try {
                r.run();
            } finally {
                scheduleNext();
            }
        });
        if (mActiveRunnable == null) {
            scheduleNext();
        }
    }

    private synchronized void scheduleNext() {
        if ((mActiveRunnable = mTasks.poll()) != null) {
            mExecutor.execute(mActiveRunnable);
        }
    }
}
