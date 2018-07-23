package com.coollen.radio;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.coollen.radio.event.PlayStatus;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.coollen.cache.ACache;
import com.coollen.radio.constant.ConstCommon;
import com.coollen.radio.data.MPDServer;

import com.coollen.radio.BuildConfig;

import com.coollen.radio.data.DataRadioStation;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Utils {
	private static int loadIcons = -1;
	private static ACache sACache;

	public static String getCacheFile(Context ctx, String theURI) {
		StringBuilder chaine = new StringBuilder("");
		try{
			String aFileName = theURI.toLowerCase().replace("http://","");
			aFileName = aFileName.toLowerCase().replace("https://","");
			aFileName = sanitizeName(aFileName);

			File file = new File(ctx.getCacheDir().getAbsolutePath() + "/"+aFileName);
			Date lastModDate = new Date(file.lastModified());

			Date now = new Date();
			long millis = now.getTime() - file.lastModified();
			long secs = millis / 1000;
			long mins = secs/60;
			long hours = mins/60;

			if(BuildConfig.DEBUG) { Log.d("UTIL","File last modified : "+ lastModDate.toString() + " secs="+secs+"  mins="+mins+" hours="+hours); }

			if (hours < 1) {
				FileInputStream aStream = new FileInputStream(file);
				BufferedReader rd = new BufferedReader(new InputStreamReader(aStream));
				String line;
				while ((line = rd.readLine()) != null) {
					chaine.append(line);
				}
				rd.close();
				if(BuildConfig.DEBUG) { Log.d("UTIL", "used cache for:" + theURI); }
				return chaine.toString();
			}
			if(BuildConfig.DEBUG) { Log.d("UTIL", "do not use cache, because too old:" + theURI); }
			return null;
		}
		catch(Exception e){
			Log.e("UTIL","getCacheFile() "+e);
		}
		return null;
	}

	public static void writeFileCache(Context ctx, String theURI, String content){
		try{
			String aFileName = theURI.toLowerCase().replace("http://","");
			aFileName = aFileName.toLowerCase().replace("https://","");
			aFileName = sanitizeName(aFileName);

			File f = new File(ctx.getCacheDir() + "/" + aFileName);
			FileOutputStream aStream = new FileOutputStream(f);
			aStream.write(content.getBytes("utf-8"));
			aStream.close();
		}
		catch(Exception e){
			Log.e("UTIL","writeFileCache() could not write to cache file for:"+theURI);
		}
	}

	public static String downloadFeed(Context ctx, String theURI, boolean forceUpdate, Map<String,String> dictParams) {
		if (!forceUpdate) {
			String cache = getCacheFile(ctx, theURI);
			if (cache != null) {
				return cache;
			}
		}

		StringBuilder chaine = new StringBuilder("");
		try{
			URL url = new URL(theURI);
			HttpURLConnection connection = (HttpURLConnection)url.openConnection();
			connection.setConnectTimeout(4000);
			connection.setReadTimeout(3000);
			connection.setRequestProperty("User-Agent", ctx.getPackageName() + "/" + BuildConfig.VERSION_NAME);
			connection.setDoInput(true);
			if (dictParams != null) {
				connection.setDoOutput(true);
				connection.setRequestProperty("Content-Type", "application/json");
				connection.setRequestProperty("Accept", "application/json");
				connection.setRequestMethod("POST");
			} else {
				connection.setRequestMethod("GET");
			}
			connection.connect();

			if (dictParams != null) {
				JSONObject jsonParams = new JSONObject();
				for (String key: dictParams.keySet()){
					jsonParams.put(key, dictParams.get(key));
				}

				OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
				wr.write(jsonParams.toString());
				wr.flush();
			}

			InputStream inputStream = connection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
			String line;
			while ((line = rd.readLine()) != null) {
				chaine.append(line);
			}

			String s = chaine.toString();
			writeFileCache(ctx,theURI,s);
			if(BuildConfig.DEBUG) { Log.d("UTIL","wrote cache file for:"+theURI); }
			return s;
		} catch (Exception e) {
			Log.e("UTIL","downloadFeed() "+e);
		}

		return null;
	}

	// ASimpleCache模块
	private static ACache getACache(Context ctx)
	{
		if (sACache == null)
		{
			synchronized (Utils.class)
			{
				if (sACache == null)
				{
					ACache tempVar = ACache.get(ctx);
					sACache = tempVar;
				}
			}
		}
		return sACache;
	}

	// 从ACache缓存读电台url
	private static String getStationUrlFromCache(Context ctx, String stationId)
	{
		return getACache(ctx).getAsString("stn_url_" + stationId);
	}

	// 缓存电台url到ACache
	private static void setStationUrlToCache(Context ctx, String stationId, String url)
	{
		getACache(ctx).put("stn_url_" + stationId, url, ACache.TIME_DAY * 20);
	}

	public static String getRealStationUrlFromNet(final Context ctx, final String stationId){
		String result = Utils.downloadFeed(ctx, RadioBrowserServerManager.getWebserviceEndpoint(ctx, "v2/json/url/" + stationId), true, null);
		if (result != null) {
			JSONObject jsonObj;
			try {
				jsonObj = new JSONObject(result);
				result = jsonObj.getString("url");
			} catch (Exception e) {
				Log.e("UTIL", "getRealStationLink() " + e);
				result = null;
			}
		}
		return result;
	}

	public static String getRealStationLink(final Context ctx, final String stationId){
		String result = null;
		if (!TextUtils.isEmpty(stationId))
		{
			result = getStationUrlFromCache(ctx, stationId);
			if (TextUtils.isEmpty(result))
			{
				result = getRealStationUrlFromNet(ctx, stationId);
				if (!TextUtils.isEmpty(result))
					setStationUrlToCache(ctx, stationId, result);
			}
			else // 去网上检查，当前url是否和网上最新的相同。不同则更新缓存
			{
				final String cacheUrl = result;
				new AsyncTask<Void, Void, String>() {
					@Override
					protected String doInBackground(Void... params) {
						String result = getRealStationUrlFromNet(ctx, stationId);
						// 更新缓存
						if (!TextUtils.isEmpty(result) && !result.equalsIgnoreCase(cacheUrl))
							setStationUrlToCache(ctx, stationId, result);
						return result;
					}
				}.execute();
			}
		}
		return result;
	}

	public static DataRadioStation getStationByUuid(Context ctx, String stationUuid){
		Log.w("UTIL","Search by uuid:"+stationUuid);
		String result = Utils.downloadFeed(ctx, RadioBrowserServerManager.getWebserviceEndpoint(ctx, "json/stations/byuuid/" + stationUuid), true, null);
		if (result != null) {
			try {
				DataRadioStation[] list = DataRadioStation.DecodeJson(result);
				if (list != null) {
					if (list.length == 1) {
						return list[0];
					}
					Log.e("UTIL", "stations by uuid did have length:" + list.length);
				}
			} catch (Exception e) {
				Log.e("UTIL", "getStationByUuid() " + e);
			}
		}
		return null;
	}

	public static void Play(final DataRadioStation station, final Context context) {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		boolean play_external = sharedPref.getBoolean("play_external", false);

		Play(station,context,play_external);
		// TODO harryguo test code 检验我改的EventBus的重要功能：调用订阅者并取得返回值
		Object obj = EventBus.getDefault().process(new PlayStatus(PlayStatus.STATUS_PLAYING));
		if (obj != null)
			obj = obj;
		EventBus.getDefault().post(new PlayStatus(PlayStatus.STATUS_PLAYING), "100");
		obj = EventBus.getDefault().process(new PlayStatus(PlayStatus.STATUS_PLAYING), "101");
		if (obj != null)
			obj = obj;
	}

	public static void Play(final DataRadioStation station, final Context context, final boolean external) {
		final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		final boolean warn_no_wifi = sharedPref.getBoolean("warn_no_wifi", true);
		boolean isWifi = Utils.hasWifiConnection(context);
		if (warn_no_wifi && !isWifi) {
			ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
			toneG.startTone(ToneGenerator.TONE_SUP_RADIO_NOTAVAIL, 2000);
			/*Toast.makeText( getBaseContext(), Html.fromHtml( text ), Toast.LENGTH_LONG ).show();
			finish();*/
			Resources res = context.getResources();
			String appName = res.getString(com.coollen.radio.R.string.app_name);
			String title = res.getString(com.coollen.radio.R.string.no_wifi_title);
			String text = String.format(res.getString(com.coollen.radio.R.string.no_wifi_connection),	appName);
			new AlertDialog.Builder(context)
					.setTitle(title)
					.setMessage(text)
					.setNegativeButton(android.R.string.cancel, null) // do not play on cancel
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						@Override public void onClick(DialogInterface dialog, int which) {
							playInternal(station, context, external);
							sharedPref.edit().putBoolean("warn_no_wifi", false).apply();
						}
					})
					.create()
					.show();
		} else {
			playInternal(station, context, external);
			if (!isWifi) {
				Toast toast = Toast.makeText(context, R.string.play_with_gprs, Toast.LENGTH_SHORT);
				toast.show();
			}
		}
	}

	private static void playInternal(final DataRadioStation station, final Context context, final boolean external) {
        context.sendBroadcast(new Intent(ConstCommon.ACTION_SHOW_LOADING));
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				return Utils.getRealStationLink(context.getApplicationContext(), station.ID);
			}

			@Override
			protected void onPostExecute(String result) {
                context.sendBroadcast(new Intent(ConstCommon.ACTION_HIDE_LOADING));

				if (result != null) {
					boolean externalActive = false;
					if (MPDClient.Connected() && MPDClient.Discovered()){
						MPDClient.Play(result, context);
						PlayerServiceUtil.saveInfo(result, station.Name, station.ID, station.IconUrl);
						externalActive = true;
					}
					if (CastHandler.isCastSessionAvailable()){
						if (!externalActive) {
							PlayerServiceUtil.stop(); // stop internal player and not continue playing
						}
						CastHandler.PlayRemote(station.Name, result, station.IconUrl);
						externalActive = true;
					}

					if (!externalActive){
						if (external){
							Intent share = new Intent(Intent.ACTION_VIEW);
							share.setDataAndType(Uri.parse(result), "audio/*");
							context.startActivity(share);
						}else {
							PlayerServiceUtil.play(result, station.Name, station.ID, station.IconUrl);
						}
					}
				} else {
					Toast toast = Toast.makeText(context.getApplicationContext(), context.getResources().getText(com.coollen.radio.R.string.error_station_load), Toast.LENGTH_SHORT);
					toast.show();
				}
				super.onPostExecute(result);
			}
		}.execute();
	}

	public static boolean shouldLoadIcons(final Context context) {
		switch(loadIcons) {
			case -1:
				if(PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext()).getBoolean("load_icons", false)) {
					loadIcons = 1;
					return true;
				} else {
					loadIcons = 0;
					return true;
				}
			case 0:
				return false;
			case 1:
				return true;
		}
		return false;
	}

	public static String getTheme(final Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getString("theme_name", context.getResources().getString(com.coollen.radio.R.string.theme_light));
	}

    public static int getThemeResId(final Context context) {
        String selectedTheme = getTheme(context);
	    if(selectedTheme.equals(context.getResources().getString(com.coollen.radio.R.string.theme_dark)))
            return com.coollen.radio.R.style.MyMaterialTheme_Dark;
	    else
	        return com.coollen.radio.R.style.MyMaterialTheme;
    }

    public static int getTimePickerThemeResId(final Context context) {
        int theme;
        if(getThemeResId(context) == com.coollen.radio.R.style.MyMaterialTheme_Dark)
            theme = com.coollen.radio.R.style.DialogTheme_Dark;
        else
            theme = com.coollen.radio.R.style.DialogTheme;
        return theme;
    }

    public static boolean useCircularIcons(final Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getBoolean("circular_icons", true);
    }

    // Storage Permissions
	public static final int REQUEST_EXTERNAL_STORAGE = 1;
	private static String[] PERMISSIONS_STORAGE = {
			Manifest.permission.WRITE_EXTERNAL_STORAGE
	};

	public static boolean verifyStoragePermissions(Activity activity) {
		// Check if we have write permission
		int permission = ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

		if (permission != PackageManager.PERMISSION_GRANTED) {
			// We don't have permission so prompt the user
			ActivityCompat.requestPermissions(
					activity,
					PERMISSIONS_STORAGE,
					REQUEST_EXTERNAL_STORAGE
			);
			return false;
		}

		return true;
	}

	public static String getReadableBytes(double bytes){
		String[] str = new String[]{"B","KB","MB","GB","TB"};
		for (String aStr : str) {
			if (bytes < 1024) {
				return String.format(Locale.getDefault(), "%1$,.1f %2$s", bytes, aStr);
			}
			bytes = bytes / 1024;
		}
		return String.format(Locale.getDefault(), "%1$,.1f %2$s",bytes*1024,str[str.length-1]);
	}

	public static String sanitizeName(String str) {
		return str.replaceAll("\\W+", "_").replaceAll("^_+", "").replaceAll("_+$", "");
	}

	public static boolean hasWifiConnection(Context context) {
		ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		return mWifi.isConnected();
	}

	public static List<MPDServer> getMPDServers(Context context) {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		String serversFromPrefs = sharedPref.getString("mpd_servers", "");
		Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<MPDServer>>(){}.getType();
		List <MPDServer> serversList = gson.fromJson(serversFromPrefs, type);
		return serversList != null? serversList : new ArrayList<MPDServer>();
	}

    public static void saveMPDServers(List<MPDServer> servers, Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        Gson gson = new Gson();
        String serversJson = gson.toJson(servers);
        editor.putString("mpd_servers", serversJson);
        editor.commit();
    }

    public static boolean bottomNavigationEnabled(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getBoolean("bottom_navigation", false);
    }

    public static String formatStringWithNamedArgs(String format, Map<String, String> args) {
	    StringBuilder builder = new StringBuilder(format);
		for (Map.Entry<String, String> entry : args.entrySet()) {
		    final String key = "${" + entry.getKey() + "}";
		    int startIdx = 0;
		    while (true) {
                final int keyIdx = builder.indexOf(key, startIdx);

                if (keyIdx == -1) {
                    break;
                }

                builder.replace(keyIdx, keyIdx + key.length(), entry.getValue());
                startIdx = keyIdx + entry.getValue().length();
            }
		}

		return builder.toString();
	}
}
