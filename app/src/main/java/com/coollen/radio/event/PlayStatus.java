package com.coollen.radio.event;

/**
 * Created by harryguo on 2018/7/9.
 */

public class PlayStatus {
	public static final int STATUS_INVALID = -1;
	public static final int STATUS_PLAYING = 0;
	public static final int STATUS_STOP = 0;

	public PlayStatus(int status)
	{
		value = status;
	}

	public int value;
}
