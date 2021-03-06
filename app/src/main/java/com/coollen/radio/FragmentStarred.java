package com.coollen.radio;

import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.coollen.radio.adapters.ItemAdapterStation;
import com.coollen.radio.interfaces.IAdapterRefreshable;
import com.coollen.radio.interfaces.IChanged;

import com.coollen.radio.data.DataRadioStation;

public class FragmentStarred extends Fragment implements IAdapterRefreshable, IChanged {
    private static final String TAG = "FragmentStarred";

    private RecyclerView rvStations;

    private FavouriteManager favouriteManager;
    private HistoryManager historyManager;

    void onStationClick(DataRadioStation theStation) {
        Utils.Play(theStation, getContext());

        historyManager.add(theStation);
    }

    public void RefreshListGui() {
        if (BuildConfig.DEBUG) Log.d(TAG, "refreshing the stations list.");

        ItemAdapterStation adapter = (ItemAdapterStation) rvStations.getAdapter();

        if (BuildConfig.DEBUG) Log.d(TAG, "stations count:" + favouriteManager.listStations.size());

        adapter.updateList(this, favouriteManager.listStations);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        ApplicationMain appMain = (ApplicationMain) getActivity().getApplication();
        historyManager = appMain.getHistoryManager();
        favouriteManager = appMain.getFavouriteManager();
        favouriteManager.setChangedListener(this);

        // Inflate the layout for this fragment
        View view = inflater.inflate(com.coollen.radio.R.layout.fragment_stations, container, false);
        rvStations = (RecyclerView) view.findViewById(com.coollen.radio.R.id.recyclerViewStations);

        ItemAdapterStation adapter = new ItemAdapterStation(getActivity(), com.coollen.radio.R.layout.list_item_station);
        adapter.setStationActionsListener(new ItemAdapterStation.StationActionsListener() {
            @Override
            public void onStationClick(DataRadioStation station) {
                FragmentStarred.this.onStationClick(station);
            }

            @Override
            public void onStationSwiped(final DataRadioStation station) {
                final int removedIdx = favouriteManager.remove(station.ID);

                RefreshListGui();

                Snackbar snackbar = Snackbar
                        .make(rvStations, com.coollen.radio.R.string.notify_station_removed_from_list, Snackbar.LENGTH_LONG);
                snackbar.setAction(com.coollen.radio.R.string.action_station_removed_from_list_undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        favouriteManager.restore(station, removedIdx);
                        RefreshListGui();
                    }
                });
                snackbar.setActionTextColor(Color.GREEN);
                snackbar.setDuration(Snackbar.LENGTH_LONG);
                snackbar.show();
            }
        });

        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        llm.setOrientation(LinearLayoutManager.VERTICAL);

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

    @Override
    public void onChanged() {
        RefreshListGui();
    }
}