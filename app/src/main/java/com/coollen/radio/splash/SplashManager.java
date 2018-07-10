package com.coollen.radio.splash;

import android.content.Context;
import android.view.View;

import com.coollen.radio.R;

/**
 * Created by harryguo on 2018/7/9.
 */

public class SplashManager {
	private static volatile SplashManager sInstance;

	private SplashManager() {
	}

	public static SplashManager getInstance() {
		if (sInstance == null) {
			synchronized (SplashManager.class) {
				if (sInstance == null)
					sInstance = new SplashManager();
			}
		}
		return sInstance;
	}

	public View getSplashView(Context context) {
		return View.inflate(context, R.layout.layout_splash, null);
	}
}
