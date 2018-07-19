package com.coollen.radio;

import android.content.*;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;

import com.coollen.radio.data.DataRadioStation;
import com.coollen.radio.data.StreamLiveInfo;

public class FragmentPlayer extends Fragment {
	TextView aTextViewName;
	ImageButton buttonPause;
	private BroadcastReceiver updateUIReciver;
	private TextView textViewLiveInfo;
	private TextView textViewExtraInfo;
	private TextView textViewRecordingInfo;
	private TextView textViewTransferredbytes;
	private ImageButton buttonRecord;
	private ImageButton buttonRecordings;
	private ImageView imageViewIcon;
	private Thread t;
	private RelativeLayout layoutPlaying;
	private RelativeLayout layoutRecording;
	private LinearLayout layoutRadioName;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View view = inflater.inflate(com.coollen.radio.R.layout.layout_player_status, container, false);

        IntentFilter filter = new IntentFilter();

        filter.addAction(PlayerService.PLAYER_SERVICE_TIMER_UPDATE);
        filter.addAction(PlayerService.PLAYER_SERVICE_STATUS_UPDATE);
        filter.addAction(PlayerService.PLAYER_SERVICE_META_UPDATE);

        updateUIReciver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                UpdateOutput();
            }
        };
        getContext().registerReceiver(updateUIReciver,filter);

		return view;
	}

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

		InitControls();
		SetInfoFromHistory(!PlayerServiceUtil.isPlaying() && !MPDClient.isPlaying);
		UpdateOutput();
		setupIcon();

		t = new Thread() {
			@Override
			public void run() {
				try {
					while (!isInterrupted()) {
						Thread.sleep(1000);
						getActivity().runOnUiThread(new Runnable() {
							@Override
							public void run() {
								UpdateOutput();
							}
						});
					}
				} catch (InterruptedException e) {
				}
			}
		};
		t.start();
	}

	private void InitControls() {
		aTextViewName = (TextView) getActivity().findViewById(com.coollen.radio.R.id.detail_station_name_value);
		textViewLiveInfo = (TextView) getActivity().findViewById(com.coollen.radio.R.id.textViewLiveInfo);
		textViewExtraInfo = (TextView) getActivity().findViewById(com.coollen.radio.R.id.textViewExtraStreamInfo);
		textViewRecordingInfo = (TextView) getActivity().findViewById(com.coollen.radio.R.id.textViewRecordingInfo);
		textViewTransferredbytes = (TextView) getActivity().findViewById(com.coollen.radio.R.id.textViewTransferredBytes);
		layoutPlaying = (RelativeLayout) getActivity().findViewById(com.coollen.radio.R.id.RelativeLayout1);
        layoutRecording = (RelativeLayout) getActivity().findViewById(com.coollen.radio.R.id.RelativeLayout2);
		imageViewIcon = (ImageView) getActivity().findViewById(com.coollen.radio.R.id.playerRadioImage);
		layoutRadioName = (LinearLayout) getActivity().findViewById(com.coollen.radio.R.id.linear_radio_name);

		buttonPause = (ImageButton) getActivity().findViewById(com.coollen.radio.R.id.buttonPause);
		View.OnClickListener clickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (PlayerServiceUtil.isPlaying() || MPDClient.isPlaying) {
					buttonPause.setImageResource(com.coollen.radio.R.drawable.ic_play_circle);
					buttonPause.setContentDescription(getResources().getString(com.coollen.radio.R.string.detail_play));
					if (PlayerServiceUtil.isRecording()) {
						buttonRecord.setImageResource(com.coollen.radio.R.drawable.ic_start_recording);
						buttonRecord.setContentDescription(getResources().getString(com.coollen.radio.R.string.image_button_record));
						String recordingInfo = getResources().getString(com.coollen.radio.R.string.player_info_recorded_to, PlayerServiceUtil.getCurrentRecordFileName());
						textViewRecordingInfo.setText(recordingInfo);
						PlayerServiceUtil.stopRecording();
						layoutPlaying.setVisibility(View.GONE);
						layoutRecording.setVisibility(View.VISIBLE);
					}
					if(PlayerServiceUtil.isPlaying())
						PlayerServiceUtil.pause();
						// Don't stop MPD playback when a user is listening in the app
					else if(MPDClient.isPlaying)
						MPDClient.Stop(getContext());
				} else {
					buttonPause.setImageResource(com.coollen.radio.R.drawable.ic_pause_circle);
					buttonPause.setContentDescription(getResources().getString(com.coollen.radio.R.string.detail_pause));
					SetInfoFromHistory(true);
				}
			}
		};
		buttonPause.setOnClickListener(clickListener);
		textViewTransferredbytes.setOnClickListener(clickListener);


		buttonRecord = (ImageButton) getActivity().findViewById(com.coollen.radio.R.id.buttonRecord);
		if (buttonRecord != null){
			buttonRecord.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (PlayerServiceUtil.isRecording()) {
						buttonRecord.setImageResource(com.coollen.radio.R.drawable.ic_start_recording);
						buttonRecord.setContentDescription(getResources().getString(com.coollen.radio.R.string.image_button_record));
						String recordingInfo = getResources().getString(com.coollen.radio.R.string.player_info_recorded_to,PlayerServiceUtil.getCurrentRecordFileName());
						textViewRecordingInfo.setText(recordingInfo);
						PlayerServiceUtil.stopRecording();
					} else if(PlayerServiceUtil.isPlaying()) {
						if (Utils.verifyStoragePermissions(getActivity())) {
							buttonRecord.setImageResource(com.coollen.radio.R.drawable.ic_stop_recording);
							buttonRecord.setContentDescription(getResources().getString(com.coollen.radio.R.string.detail_stop));
							PlayerServiceUtil.startRecording();
							if (PlayerServiceUtil.getCurrentRecordFileName() != null && PlayerServiceUtil.isRecording()){
								String recordingInfo = getResources().getString(com.coollen.radio.R.string.player_info_recording_to,PlayerServiceUtil.getCurrentRecordFileName());
								textViewRecordingInfo.setText(recordingInfo);
							}
						}
					}
				}
			});
		}

        buttonRecordings = (ImageButton) getActivity().findViewById(com.coollen.radio.R.id.buttonRecordings);
        buttonRecordings.setVisibility(Utils.bottomNavigationEnabled(getContext())? View.VISIBLE : View.GONE);
		if (buttonRecordings != null){
            buttonRecordings.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toolbar toolbar = ((ActivityMain)getActivity()).getToolbar();
                    // Don't add fragment twice
                    if(toolbar.getTitle().toString().equals(getString(com.coollen.radio.R.string.nav_item_recordings))) {
                        getActivity().onBackPressed();
                        return;
                    }

                    toolbar.setTitle(getString(com.coollen.radio.R.string.nav_item_recordings));
                    FragmentRecordings f = new FragmentRecordings();
                    FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                    fragmentTransaction.replace(com.coollen.radio.R.id.containerView, f).addToBackStack(String.valueOf(ActivityMain.FRAGMENT_FROM_BACKSTACK)).commit();
                }
            });
        }

		View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentVisibility = layoutPlaying.getVisibility();
                layoutPlaying.setVisibility(currentVisibility == View.GONE ? View.VISIBLE : View.GONE);
	            layoutRecording.setVisibility(currentVisibility == View.GONE ? View.GONE : View.VISIBLE);
            }
        };

		layoutRadioName.setOnClickListener(onClickListener);
        layoutRecording.setOnClickListener(onClickListener);
		layoutRecording.setVisibility(View.GONE);
	}

	private void SetInfoFromHistory(boolean startPlaying) {
        ApplicationMain appMain = (ApplicationMain) getActivity().getApplication();
        HistoryManager historyManager = appMain.getHistoryManager();
        DataRadioStation[] history = historyManager.getList();

        if(history.length > 0) {
            DataRadioStation lastStation = history[0];
            if(startPlaying)
                Utils.Play(lastStation, getContext());
            else {
                aTextViewName.setText(lastStation.Name);

                if (!Utils.shouldLoadIcons(getContext()))
                    imageViewIcon.setVisibility(View.GONE);
                else
                    PlayerServiceUtil.getStationIcon(imageViewIcon, lastStation.IconUrl);
            }
        }
    }

    private void setupIcon() {
        boolean useCircularIcons = PreferenceManager.getDefaultSharedPreferences(getContext().getApplicationContext()).getBoolean("circular_icons", false);
        if(useCircularIcons) {
            imageViewIcon.setBackgroundColor(getContext().getResources().getColor(android.R.color.black));
            ImageView transparentCircle = (ImageView) getView().findViewById(com.coollen.radio.R.id.transparentCircle);
            transparentCircle.setVisibility(View.VISIBLE);
        }
    }

	@Override
	public void onDestroy() {
		if (t != null) {
			t.interrupt();
		}
		super.onDestroy();
		if (updateUIReciver != null) {
			getContext().unregisterReceiver(updateUIReciver);
			updateUIReciver = null;
		}
	}

	private void UpdateOutput() {
	if(getView() == null || PlayerServiceUtil.getStationName() == null) return;

		buttonPause = (ImageButton) getActivity().findViewById(com.coollen.radio.R.id.buttonPause);
		if (PlayerServiceUtil.isPlaying() || MPDClient.isPlaying) {
			buttonPause.setImageResource(com.coollen.radio.R.drawable.ic_pause_circle);
			buttonPause.setContentDescription(getResources().getString(com.coollen.radio.R.string.detail_pause));
		} else {
			buttonPause.setImageResource(com.coollen.radio.R.drawable.ic_play_circle);
			buttonPause.setContentDescription(getResources().getString(com.coollen.radio.R.string.detail_play));
		}

		buttonRecord = (ImageButton) getActivity().findViewById(com.coollen.radio.R.id.buttonRecord);
		TypedValue tv = new TypedValue();
		getContext().getTheme().resolveAttribute(com.coollen.radio.R.attr.colorAccentMy, tv, true);

		if (PlayerServiceUtil.isRecording()) {
			buttonRecord.setImageResource(com.coollen.radio.R.drawable.ic_stop_recording);
			buttonRecord.setContentDescription(getResources().getString(com.coollen.radio.R.string.detail_stop));
			aTextViewName.setTextColor(getResources().getColor(com.coollen.radio.R.color.startRecordingColor));
		} else if (android.os.Environment.getExternalStorageDirectory().canWrite()) {
			buttonRecord.setImageResource(com.coollen.radio.R.drawable.ic_start_recording);
			buttonRecord.setContentDescription(getResources().getString(com.coollen.radio.R.string.image_button_record));
			aTextViewName.setTextColor(tv.data);
		} else {
			buttonRecord.setImageResource(com.coollen.radio.R.drawable.ic_fiber_manual_record_black_50dp);
			buttonRecord.setContentDescription(getResources().getString(com.coollen.radio.R.string.image_button_record_request_permission));
			aTextViewName.setTextColor(tv.data);
		}

		if(BuildConfig.DEBUG) { Log.d("ARR","UpdateOutput()"); }

		if (aTextViewName != null) {
			String stationName = PlayerServiceUtil.getStationName();
			String streamName = PlayerServiceUtil.getStreamName();
			if (!TextUtils.isEmpty(streamName)) {
				aTextViewName.setText(streamName);
			}else{
				aTextViewName.setText(stationName);
			}
		}

        StreamLiveInfo liveInfo = PlayerServiceUtil.getMetadataLive();
        String streamTitle = liveInfo.getTitle();
        if (!TextUtils.isEmpty(streamTitle)) {
            textViewLiveInfo.setVisibility(View.VISIBLE);
            textViewLiveInfo.setText(streamTitle);
            aTextViewName.setGravity(Gravity.BOTTOM);
        } else {
            textViewLiveInfo.setVisibility(View.GONE);
            aTextViewName.setGravity(Gravity.CENTER_VERTICAL);
        }

		if (PlayerServiceUtil.getCurrentRecordFileName() != null && PlayerServiceUtil.isRecording()){
			String recordingInfo = getResources().getString(com.coollen.radio.R.string.player_info_recording_to,PlayerServiceUtil.getCurrentRecordFileName());
			textViewRecordingInfo.setText(recordingInfo);
		}

		String strExtra = "";
		if (PlayerServiceUtil.getIsHls()){
			strExtra += "HLS-Stream\n";
		}
		if (PlayerServiceUtil.getMetadataGenre() != null) {
			strExtra += PlayerServiceUtil.getMetadataGenre() + "\n";
		}
		if (PlayerServiceUtil.getMetadataHomepage() != null) {
			strExtra += PlayerServiceUtil.getMetadataHomepage();
		}
		textViewExtraInfo.setText(strExtra);

		String byteInfo = Utils.getReadableBytes(PlayerServiceUtil.getTransferredBytes());
		if (PlayerServiceUtil.getMetadataBitrate() > 0) {
			byteInfo += " (" + PlayerServiceUtil.getMetadataBitrate() + " kbps)";
		}

		if (PlayerServiceUtil.isPlaying() || PlayerServiceUtil.isRecording()) {
			textViewTransferredbytes.setText(byteInfo);
		}

			if (!Utils.shouldLoadIcons(getContext())) {
                imageViewIcon.setVisibility(View.GONE);
			} else {
                PlayerServiceUtil.getStationIcon(imageViewIcon, null);
			}

	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {

		buttonRecord = (ImageButton) getActivity().findViewById(com.coollen.radio.R.id.buttonRecord);
		switch (requestCode) {
			case Utils.REQUEST_EXTERNAL_STORAGE: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					PlayerServiceUtil.startRecording();
					buttonRecord.setImageResource(com.coollen.radio.R.drawable.ic_fiber_manual_record_red_50dp);
					buttonRecord.setContentDescription(getResources().getString(com.coollen.radio.R.string.detail_stop));
				} else {
					buttonRecord.setImageResource(com.coollen.radio.R.drawable.ic_fiber_manual_record_black_50dp);
			buttonRecord.setContentDescription(getResources().getString(com.coollen.radio.R.string.image_button_record_request_permission));
					Toast toast = Toast.makeText(getActivity(), getResources().getString(com.coollen.radio.R.string.error_record_needs_write), Toast.LENGTH_SHORT);
					toast.show();
				}
				return;
			}
		}
	}
}
