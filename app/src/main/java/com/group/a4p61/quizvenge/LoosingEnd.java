package com.group.a4p61.quizvenge;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 */
public class LoosingEnd extends android.app.Fragment {


    public LoosingEnd() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_loosing_end, container, false);
    }

    public void testtext(String text){
        TextView txtview = (TextView) getView().findViewById(R.id.loosingtext);
        txtview.setText(text);
    }
    public void close_end(View view) {
        System.exit(0);
    }



}
