package com.coollen.radio;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.coollen.radio.adapters.ItemAdapterStation;
import com.coollen.radio.data.DataRadioStation;

import java.util.ArrayList;

public class FragmentStations extends FragmentBase {
    private static final String TAG = "FragmentStations";

    private RecyclerView rvStations;
    private SwipeRefreshLayout swipeRefreshLayout;

    private SharedPreferences sharedPref;
    private HistoryManager historyManager;
    private FavouriteManager favouriteManager;

    void onStationClick(DataRadioStation theStation) {
        Context context = getContext();
        Utils.Play(theStation, context);

        historyManager.add(theStation);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        final Boolean autoFavorite = sharedPref.getBoolean("auto_favorite", false);
        if (autoFavorite && !favouriteManager.has(theStation.ID)) {
            favouriteManager.add(theStation);
            Toast toast = Toast.makeText(context, context.getString(com.coollen.radio.R.string.notify_autostarred), Toast.LENGTH_SHORT);
            toast.show();
            RefreshListGui();
        }
    }

    @Override
    protected void RefreshListGui() {
        if (rvStations == null) {
            return;
        }

        if (BuildConfig.DEBUG) Log.d(TAG, "refreshing the stations list.");

        Context ctx = getContext();
        if (sharedPref == null) {
            sharedPref = PreferenceManager.getDefaultSharedPreferences(ctx);
        }

        boolean show_broken = sharedPref.getBoolean("show_broken", false);

        ArrayList<DataRadioStation> filteredStationsList = new ArrayList<>();
        DataRadioStation[] radioStations = DataRadioStation.DecodeJson(getUrlResult());

        if (BuildConfig.DEBUG) Log.d(TAG, "station count:" + radioStations.length);

        for (DataRadioStation station : radioStations) {
            if (show_broken || station.Working) {
                filteredStationsList.add(station);
            }
        }

        ItemAdapterStation adapter = (ItemAdapterStation) rvStations.getAdapter();
        if (adapter != null) {
            adapter.updateList(null, filteredStationsList);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(com.coollen.radio.R.layout.fragment_stations_remote, container, false);
        rvStations = (RecyclerView) view.findViewById(com.coollen.radio.R.id.recyclerViewStations);

        ApplicationMain appMain = (ApplicationMain) getActivity().getApplication();
        historyManager = appMain.getHistoryManager();
        favouriteManager = appMain.getFavouriteManager();

        ItemAdapterStation adapter = new ItemAdapterStation(getActivity(), com.coollen.radio.R.layout.list_item_station);
        adapter.setStationActionsListener(new ItemAdapterStation.StationActionsListener() {
            @Override
            public void onStationClick(DataRadioStation station) {
                FragmentStations.this.onStationClick(station);
            }

            @Override
            public void onStationSwiped(DataRadioStation station) {
            }
        });

        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        llm.setOrientation(LinearLayoutManager.VERTICAL);

        rvStations.setLayoutManager(llm);
        rvStations.setAdapter(adapter);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rvStations.getContext(),
                llm.getOrientation());
        rvStations.addItemDecoration(dividerItemDecoration);

        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(com.coollen.radio.R.id.swiperefresh);
        swipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        if (BuildConfig.DEBUG) Log.d(TAG, "swipe to refresh.");
                        DownloadUrl(true, false);
                    }
                }
        );

        RefreshListGui();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        rvStations.setAdapter(null);
    }

    @Override
    protected void DownloadFinished() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }
}