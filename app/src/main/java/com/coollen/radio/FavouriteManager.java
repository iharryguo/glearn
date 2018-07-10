package com.coollen.radio;

import android.content.Context;

import com.coollen.radio.data.DataRadioStation;

public class FavouriteManager extends StationSaveManager{
    @Override
    protected String getSaveId(){
        return "favourites";
    }

    public FavouriteManager(Context ctx) {
        super(ctx);
    }

    @Override
    public void add(DataRadioStation station){
        DataRadioStation stationFromId = getById(station.ID);
        if (!has(station.ID)) {
            listStations.add(station);
            Save();
        }
    }


}
