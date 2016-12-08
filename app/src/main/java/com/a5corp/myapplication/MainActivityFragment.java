package com.a5corp.myapplication;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        String[] forecastArray = {
                "Today - Sunny 88/63",
                "Tomorrow - Foggy 70/40",
                "Today - Sunny 88/63",
                "Tomorrow - Foggy 70/40",
                "Today - Sunny 88/63",
                "Tomorrow - Foggy 70/40",
                "Today - Sunny 88/63",
                "Tomorrow - Foggy 70/40"
        };

        List<String> weekForecast = new ArrayList<String>(
                Arrays.asList(forecastArray)
        );
        
        return inflater.inflate(R.layout.fragment_main, container, false);
    }
}
