package com.coollen.radio;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
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

public class FragmentHistory extends Fragment {
    private static final String TAG = "FragmentStarred";

    private RecyclerView rvStations;

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
        }

        RefreshListGui();
        rvStations.smoothScrollToPosition(0);
    }

    protected void RefreshListGui() {
        if (BuildConfig.DEBUG) Log.d(TAG, "refreshing the stations list.");

        ItemAdapterStation adapter = (ItemAdapterStation) rvStations.getAdapter();

        if (BuildConfig.DEBUG) Log.d(TAG, "stations count:" + historyManager.listStations.size());

        adapter.updateList(null, historyManager.listStations);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ApplicationMain appMain = (ApplicationMain) getActivity().getApplication();
        historyManager = appMain.getHistoryManager();
        favouriteManager = appMain.getFavouriteManager();

        ItemAdapterStation adapter = new ItemAdapterStation(getActivity(), com.coollen.radio.R.layout.list_item_station);
        adapter.setStationActionsListener(new ItemAdapterStation.StationActionsListener() {
            @Override
            public void onStationClick(DataRadioStation station) {
                FragmentHistory.this.onStationClick(station);
            }

            @Override
            public void onStationSwiped(final DataRadioStation station) {
                final int removedIdx = historyManager.remove(station.ID);

                RefreshListGui();

                Snackbar snackbar = Snackbar
                        .make(rvStations, com.coollen.radio.R.string.notify_station_removed_from_list, Snackbar.LENGTH_LONG);
                snackbar.setAction(com.coollen.radio.R.string.action_station_removed_from_list_undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        historyManager.restore(station, removedIdx);
                        RefreshListGui();
                    }
                });
                snackbar.setActionTextColor(Color.GREEN);
                snackbar.setDuration(Snackbar.LENGTH_LONG);
                snackbar.show();
            }
        });

        // Inflate the layout for this fragment
        View view = inflater.inflate(com.coollen.radio.R.layout.fragment_stations, container, false);

        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        llm.setOrientation(LinearLayoutManager.VERTICAL);

        rvStations = (RecyclerView) view.findViewById(com.coollen.radio.R.id.recyclerViewStations);
        rvStations.setAdapter(adapter);
        rvStations.setLayoutManager(llm);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rvStations.getContext(),
                llm.getOrientation());
        rvStations.addItemDecoration(dividerItemDecoration);

        adapter.enableItemRemoval(rvStations);

        RefreshListGui();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        rvStations.setAdapter(null);
    }
}