package com.coollen.radio.splash;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;

import com.coollen.radio.R;

/**
 * Created by harryguo on 2018/7/9.
 */

public class SplashManager {
	private static volatile SplashManager sInstance;
	private int[] mSplashImageIds;

	private SplashManager() {
		mSplashImageIds = new int[]{R.drawable.splash, R.drawable.splash1, R.drawable.splash2};
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
		View splashView = View.inflate(context, R.layout.layout_splash, null);
		// ImageView splashImg = splashView.findViewById(R.id.splashImageView);
		splashView.setBackgroundResource(mSplashImageIds[(int) (Math.random() * 100 % mSplashImageIds.length)]);
		return splashView;
	}
}
