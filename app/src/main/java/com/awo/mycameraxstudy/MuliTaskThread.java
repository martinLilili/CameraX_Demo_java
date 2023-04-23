package com.awo.mycameraxstudy;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

public class MuliTaskThread extends HandlerThread {

    static String THREADNAME = "camerax_MuliTaskThread";
    private static MuliTaskThread mInstance;
    private static Handler mHandler;

    private static Handler mainHandler;

    public MuliTaskThread() {
        super(THREADNAME, android.os.Process.THREAD_PRIORITY_DEFAULT);

    }


    public static void prepareThread() {
        if (mInstance == null) {
            mInstance = new MuliTaskThread();
            // 创建HandlerThread后一定要记得start()
            mInstance.start();
            // 获取HandlerThread的Looper,创建Handler，通过Looper初始化
        }
        if (mHandler == null) {
            mHandler = new Handler(mInstance.getLooper());
        }
    }

    /**
     * 如果需要在后台线程做一件事情，那么直接调用post方法，使用非常方便
     */
    public static void postToMultiTaskThread(final Runnable runnable) {
        prepareThread();
        if (isInMuliTaskThread()) {
            runnable.run();
        } else {
            mHandler.post(runnable);
        }

    }

    public static void postToMainThread(final Runnable runnable) {
        if (mainHandler == null) {
            mainHandler = new Handler(Looper.getMainLooper());
        }
        mainHandler.post(runnable);

    }

    public static boolean isInMuliTaskThread () {
        return Thread.currentThread().getName().equals(THREADNAME);
    }

    /**
     * 退出HandlerThread
     */
    public static void destroyThread() {
        if (mInstance != null) {
            mInstance.quit();
            mInstance = null;
            mHandler = null;
        }
    }
}
