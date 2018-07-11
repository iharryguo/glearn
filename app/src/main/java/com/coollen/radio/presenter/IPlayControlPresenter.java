package com.coollen.radio.presenter;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import com.coollen.radio.data.DataRadioStation;
import com.rustamg.filedialogs.FileDialog;

import java.io.File;

/**
 * Created by harryguo on 2018/7/9.
 */

public interface IPlayControlPresenter {
	void play(DataRadioStation station, Context context);
}
