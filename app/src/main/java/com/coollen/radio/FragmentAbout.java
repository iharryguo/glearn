package com.coollen.radio;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.coollen.radio.BuildConfig;

public class FragmentAbout extends Fragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(com.coollen.radio.R.layout.layout_about,null);

        TextView aTextVersion = (TextView) view.findViewById(com.coollen.radio.R.id.about_version);
        if (aTextVersion != null) {

            String version = BuildConfig.VERSION_NAME;
            String gitHash = getString(com.coollen.radio.R.string.GIT_HASH);
            String buildDate = getString(com.coollen.radio.R.string.BUILD_DATE);


            if (!gitHash.isEmpty()) {
                version += " (git " + gitHash + ")";
            }

            Resources resources = getResources();
            aTextVersion.setText(resources.getString(com.coollen.radio.R.string.about_version, version+" "+buildDate));

        }

        return view;
    }
}
