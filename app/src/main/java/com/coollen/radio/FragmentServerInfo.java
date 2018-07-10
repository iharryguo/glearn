package com.coollen.radio;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.ListViewCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.coollen.radio.constant.ConstCommon;
import com.coollen.radio.data.DataStatistics;
import com.coollen.radio.interfaces.IFragmentRefreshable;
import com.coollen.radio.adapters.ItemAdapterStatistics;

public class FragmentServerInfo extends Fragment implements IFragmentRefreshable {
    private ItemAdapterStatistics itemAdapterStatistics;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(com.coollen.radio.R.layout.layout_statistics,null);

        if (itemAdapterStatistics == null) {
            itemAdapterStatistics = new ItemAdapterStatistics(getActivity(), com.coollen.radio.R.layout.list_item_statistic);
        }

        ListViewCompat lv = (ListViewCompat)view.findViewById(com.coollen.radio.R.id.listViewStatistics);
        lv.setAdapter(itemAdapterStatistics);

        Download(false);

        return view;
    }

    void Download(final boolean forceUpdate){
        getContext().sendBroadcast(new Intent(ConstCommon.ACTION_SHOW_LOADING));
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String endpoint = RadioBrowserServerManager.getWebserviceEndpoint(getActivity(),"json/stats");
                if (endpoint != null) {
                    return Utils.downloadFeed(getActivity(), endpoint, forceUpdate, null);
                }
                return null;
            }

            @Override
            protected void onPostExecute(String result) {
                if(getContext() != null)
                    getContext().sendBroadcast(new Intent(ConstCommon.ACTION_HIDE_LOADING));
                if (result != null) {
                    itemAdapterStatistics.clear();
                    DataStatistics[] items = DataStatistics.DecodeJson(result);
                    for(DataStatistics item: items) {
                        itemAdapterStatistics.add(item);
                    }
                }else{
                    try {
                        Toast toast = Toast.makeText(getContext(), getResources().getText(com.coollen.radio.R.string.error_list_update), Toast.LENGTH_SHORT);
                        toast.show();
                    }
                    catch(Exception e){
                        Log.e("ERR",e.toString());
                    }
                }
                super.onPostExecute(result);
            }
        }.execute();
    }

    @Override
    public void Refresh() {
        Download(true);
    }
}
