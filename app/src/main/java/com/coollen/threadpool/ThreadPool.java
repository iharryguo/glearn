package com.coollen.threadpool;

import android.os.Handler;
import android.os.Looper;

/**
 * Created by harryguo on 2018/7/9.
 */

public class ThreadPool {
	private static volatile ThreadPool sInstance;

	private Object mUIHandlerLock = new Object();
	private Handler mUIHandler;

	private ThreadPool() {
	}

	public static ThreadPool getInstance() {
		if (sInstance == null) {
			synchronized (ThreadPool.class) {
				if (sInstance == null)
					sInstance = new ThreadPool();
			}
		}
		return sInstance;
	}

	public Handler uiHandler() {
		if (mUIHandler == null) {
			synchronized (mUIHandlerLock) {
				if (mUIHandler == null)
					mUIHandler = new Handler(Looper.getMainLooper());
			}
		}
		return mUIHandler;
	}
}
