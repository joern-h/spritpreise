package org.woheller69.spritpreise.ui.viewPager;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import org.woheller69.spritpreise.R;
import org.woheller69.spritpreise.database.CityToWatch;
import org.woheller69.spritpreise.database.CurrentWeatherData;
import org.woheller69.spritpreise.database.Forecast;
import org.woheller69.spritpreise.database.PFASQLiteHelper;
import org.woheller69.spritpreise.database.WeekForecast;
import org.woheller69.spritpreise.services.UpdateDataService;
import org.woheller69.spritpreise.ui.CityFragment;
import org.woheller69.spritpreise.ui.updater.IUpdateableCityUI;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static androidx.core.app.JobIntentService.enqueueWork;
import static org.woheller69.spritpreise.services.UpdateDataService.SKIP_UPDATE_INTERVAL;
import static org.woheller69.spritpreise.ui.RecycleList.CityAdapter.DAY;
import static org.woheller69.spritpreise.ui.RecycleList.CityAdapter.DETAILS;
import static org.woheller69.spritpreise.ui.RecycleList.CityAdapter.OVERVIEW;

/**
 * Created by thomagglaser on 07.08.2017.
 */

public class CityPagerAdapter extends FragmentStatePagerAdapter implements IUpdateableCityUI {

    private Context mContext;

    private PFASQLiteHelper database;
    long lastUpdateTime;

    private List<CityToWatch> cities;
    private List<CurrentWeatherData> currentWeathers;

    private static int[] mDataSetTypes = {OVERVIEW, DETAILS, DAY}; //TODO Make dynamic from Settings

    public CityPagerAdapter(Context context, FragmentManager supportFragmentManager) {
        super(supportFragmentManager,FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        this.mContext = context;
        this.database = PFASQLiteHelper.getInstance(context);
        this.currentWeathers = database.getAllCurrentWeathers();
        this.cities = database.getAllCitiesToWatch();
        try {
            cities = database.getAllCitiesToWatch();
            Collections.sort(cities, new Comparator<CityToWatch>() {
                @Override
                public int compare(CityToWatch o1, CityToWatch o2) {
                    return o1.getRank() - o2.getRank();
                }

            });
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @NonNull
    @Override
    public CityFragment getItem(int position) {
        Bundle args = new Bundle();
        args.putInt("city_id", cities.get(position).getCityId());
        args.putIntArray("dataSetTypes", mDataSetTypes);

        return CityFragment.newInstance(args);
    }

    @Override
    public int getCount() {
        return cities.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (cities.size() == 0) {
            return mContext.getString(R.string.app_name);
        }
        return cities.get(position).getCityName();
    }

    public static void refreshSingleData(Context context, Boolean asap, int cityId) {
        Intent intent = new Intent(context, UpdateDataService.class);
        intent.setAction(UpdateDataService.UPDATE_SINGLE_ACTION);
        intent.putExtra(SKIP_UPDATE_INTERVAL, asap);
        intent.putExtra("cityId",cityId);
        enqueueWork(context, UpdateDataService.class, 0, intent);
    }

    private CurrentWeatherData findWeatherFromID(List<CurrentWeatherData> currentWeathers, int ID) {
        for (CurrentWeatherData weather : currentWeathers) {
            if (weather.getCity_id() == ID) return weather;
        }
        return null;
    }

    @Override
    public void processNewForecasts(List<Forecast> forecasts) {
        notifyDataSetChanged();
    }

    public int getCityIDForPos(int pos) {
            CityToWatch city = cities.get(pos);
                 return city.getCityId();
    }

    public int getPosForCityID(int cityID) {
        for (int i = 0; i < cities.size(); i++) {
            CityToWatch city = cities.get(i);
            if (city.getCityId() == cityID) {
                return i;
            }
        }
        return 0;
    }

    public float getLatForPos(int pos) {
        CityToWatch city = cities.get(pos);
        return city.getLatitude();
    }

    public float getLonForPos(int pos) {
        CityToWatch city = cities.get(pos);
        return city.getLongitude();
    }


}