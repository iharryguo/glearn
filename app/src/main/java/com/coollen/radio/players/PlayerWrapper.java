package com.coollen.radio.players;

import android.content.Context;
import android.support.annotation.NonNull;

import com.coollen.radio.data.ShoutcastInfo;
import com.coollen.radio.data.StreamLiveInfo;
import com.coollen.radio.recording.Recordable;

import okhttp3.OkHttpClient;

public interface PlayerWrapper extends Recordable {
    interface PlayListener {
        void onStateChanged(RadioPlayer.PlayState state);

        void onPlayerError(final int messageId);

        void onDataSourceShoutcastInfo(ShoutcastInfo shoutcastInfo, boolean isHls);

        void onDataSourceStreamLiveInfo(StreamLiveInfo liveInfo);
    }

    void playRemote(@NonNull OkHttpClient httpClient, @NonNull String streamUrl, @NonNull Context context, boolean isAlarm);

    void pause();

    void stop();

    boolean isPlaying();

    long getBufferedMs();

    int getAudioSessionId();

    long getTotalTransferredBytes();

    long getCurrentPlaybackTransferredBytes();

    void setVolume(float newVolume);

    void setStateListener(PlayListener listener);
}
