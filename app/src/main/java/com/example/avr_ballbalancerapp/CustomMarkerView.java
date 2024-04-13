package com.example.avr_ballbalancerapp;

import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

public class CustomMarkerView extends MarkerView {

    private TextView tvContent;

    public CustomMarkerView(Context context, int layoutResource){
        super(context, layoutResource);
        tvContent = (TextView) findViewById(R.id.tvContent);
    }
    
    @Override
    public MPPointF getOffset() {
        return super.getOffset();
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        super.refreshContent(e, highlight);

        if (highlight.getDataSetIndex() + 1 == 1) //CV - Control Value (Servo PWM output)
        {
            tvContent.setText(String.valueOf("PV [mm]: " + e.getY()));
        }
        else if (highlight.getDataSetIndex() + 1 == 2)
        {
            tvContent.setText(String.valueOf("CV [PWM]: " + e.getY() ));
        }
        else
        {

        }


    }
}
