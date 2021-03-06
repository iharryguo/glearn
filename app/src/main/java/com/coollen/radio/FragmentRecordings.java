package com.coollen.radio;

import android.content.Intent;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.ListViewCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.coollen.radio.data.DataRecording;
import com.coollen.radio.interfaces.IFragmentRefreshable;
import com.coollen.radio.recording.RecordingsManager;

import com.coollen.radio.BuildConfig;

import com.coollen.radio.adapters.ItemAdapterRecordings;

import java.io.File;

public class FragmentRecordings extends Fragment implements IFragmentRefreshable {
    private ItemAdapterRecordings itemAdapterRecordings;
    private ListViewCompat lv;
    final String TAG = "FragREC";

    void ClickOnItem(DataRecording theData) {
        String path = RecordingsManager.getRecordDir() + "/" + theData.Name;
        if(BuildConfig.DEBUG) { Log.d(TAG,"play :"+path); }
        Intent i = new Intent(path);
        i.setAction(android.content.Intent.ACTION_VIEW);
        File file = new File(path);
        Uri uri;
        // 判断版本大于等于7.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // "net.csdn.blog.ruancoder.fileprovider"即是在清单文件中配置的authorities
            uri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".fileprovider", file);
            // 给目标应用一个临时授权
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            uri = Uri.fromFile(file);
        }
        i.setDataAndType(uri, "audio/*");
        startActivity(i);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(com.coollen.radio.R.layout.layout_statistics,null);

        if (itemAdapterRecordings == null) {
            itemAdapterRecordings = new ItemAdapterRecordings(getActivity(), com.coollen.radio.R.layout.list_item_recording);
        }

        lv = (ListViewCompat)view.findViewById(com.coollen.radio.R.id.listViewStatistics);
        lv.setAdapter(itemAdapterRecordings);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object anObject = parent.getItemAtPosition(position);
                if (anObject instanceof DataRecording) {
                    ClickOnItem((DataRecording) anObject);
                }
            }
        });

        final int[] attrs = new int[] {android.R.attr.listDivider};
        final TypedArray a = getContext().obtainStyledAttributes(attrs);
        lv.setDivider(a.getDrawable(0));
        a.recycle();

        RefreshListGui();

        return view;
    }

    protected void RefreshListGui(){
        if(BuildConfig.DEBUG) { Log.d(TAG, "RefreshListGUI()"); }

        if (!Utils.verifyStoragePermissions(getActivity())){
            Log.e(TAG,"could not get permissions");
        }

        if (lv != null) {
            if(BuildConfig.DEBUG) { Log.d(TAG,"LV != null"); }
            ItemAdapterRecordings arrayAdapter = (ItemAdapterRecordings) lv.getAdapter();
            arrayAdapter.clear();
            DataRecording[] recordings = RecordingsManager.getRecordings();
            if(BuildConfig.DEBUG) { Log.d(TAG,"Station count:"+recordings.length); }
            for (DataRecording aRecording : recordings) {
                if (!aRecording.Name.equals(PlayerServiceUtil.getCurrentRecordFileName())) {
                    arrayAdapter.add(aRecording);
                }
            }

            lv.invalidate();
        }else{
            Log.e(TAG,"LV == null");
        }
    }

    @Override
    public void Refresh() {
        RefreshListGui();
    }
}
