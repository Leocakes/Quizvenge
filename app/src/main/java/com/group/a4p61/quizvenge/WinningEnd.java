package com.group.a4p61.quizvenge;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class WinningEnd extends android.app.Fragment {

    public WinningEnd() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        TextView v = container.findViewById(R.id.winning_text);
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_winning_end, container, false);
    }

    public void testtext(String text){
        TextView txtview = (TextView) getView().findViewById(R.id.winning_text);
        txtview.setText(text);
    }

    public void close_end(View view) {
        System.exit(0);
    }

}
