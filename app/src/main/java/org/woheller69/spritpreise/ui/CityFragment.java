package org.woheller69.spritpreise.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;

import android.view.View;
import android.view.ViewGroup;

import org.woheller69.spritpreise.R;
import org.woheller69.spritpreise.activities.CityGasPricesActivity;
import org.woheller69.spritpreise.database.Station;
import org.woheller69.spritpreise.ui.RecycleList.CityAdapter;
import org.woheller69.spritpreise.ui.RecycleList.OnSwipeDownListener;
import org.woheller69.spritpreise.ui.updater.IUpdateableCityUI;
import org.woheller69.spritpreise.ui.updater.ViewUpdater;
import org.woheller69.spritpreise.ui.viewPager.CityPagerAdapter;

import java.util.List;

public class CityFragment extends Fragment implements IUpdateableCityUI {
    private static final int MINGRIDWIDTH = 500;
    private int mCityId = -1;
    private int[] mDataSetTypes = new int[]{};

    private CityAdapter mAdapter;

    private RecyclerView recyclerView;

    public static CityFragment newInstance(Bundle args)
    {
        CityFragment cityFragment = new CityFragment();
        cityFragment.setArguments(args);
        return cityFragment;
    }

    public void setAdapter(CityAdapter adapter) {
        mAdapter = adapter;

        if (recyclerView != null) {
            recyclerView.setAdapter(mAdapter);
            recyclerView.setFocusable(false);
        }
    }

    public void loadData() {

                mAdapter = new CityAdapter(mCityId, mDataSetTypes, getContext());
                setAdapter(mAdapter);
            }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        ViewUpdater.addSubscriber(this);
    }

    @Override
    public void onDetach() {
        ViewUpdater.removeSubscriber(this);

        super.onDetach();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.city_fragment, container, false);

        recyclerView = v.findViewById(R.id.CityRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()){
            public boolean canScrollVertically() {    //Make parent recyclerview not scrollable (not needed in this app) and scroll stations instead
                return false;
            }
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener(){
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!recyclerView.canScrollVertically(-1)){
                    recyclerView.setOnTouchListener(new OnSwipeDownListener(getContext()) {
                        public void onSwipeDown() {
                                CityPagerAdapter.refreshSingleData(getContext(),true,mCityId);
                                CityGasPricesActivity.startRefreshAnimation();
                        }
                    });
                }else recyclerView.setOnTouchListener(null);
            }
        });

        Bundle args = getArguments();
        mCityId = args.getInt("city_id");
        mDataSetTypes = args.getIntArray("dataSetTypes");

        loadData();

        return v;
    }

    @Override
    public void processUpdateStations(List<Station> stations) {
        if (stations != null && stations.size() > 0 && stations.get(0).getCity_id() == mCityId) {
            if (mAdapter != null) {
                mAdapter.updateStationsData(stations);
            }
        }
    }

}
