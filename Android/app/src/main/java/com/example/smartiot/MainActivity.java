package com.example.smartiot;

import android.content.Context;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    String TAG = "HCMUT";

    private lab.graph.AsyncTask.JsonAsyncTask.JsonListener jsonListener;
    private Context context = this;
    private GraphView graphView1;
    private GraphView graphView2;

    private DataPoint dataPoint1[] = {
            new DataPoint(0, 1),
            new DataPoint(1, 2),
            new DataPoint(2, 3),
            new DataPoint(3, 4),
            new DataPoint(4, 5)
    };

    private DataPoint dataPoint2[] = {
            new DataPoint(0, 1),
            new DataPoint(1, 2),
            new DataPoint(2, 3),
            new DataPoint(3, 4),
            new DataPoint(4, 5)
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        graphView1 = findViewById(R.id.graph1);
        graphView2 = findViewById(R.id.graph2);

        jsonListener = new lab.graph.AsyncTask.JsonAsyncTask.JsonListener() {
            @Override
            public void onStreamResponse(List<FeedsModel> feeds) {
                Log.d(TAG, "-----------------------------");
                Log.d(TAG, "field1: " + feeds.get(0).field1);
                Log.d(TAG, "field2: " + feeds.get(0).field2);

                // Display data temperature
                graphView1.removeAllSeries();
                dataPoint1[0] = new DataPoint(0, dataPoint1[1].getY());
                dataPoint1[1] = new DataPoint(1, dataPoint1[2].getY());
                dataPoint1[2] = new DataPoint(2, dataPoint1[3].getY());
                dataPoint1[3] = new DataPoint(3, dataPoint1[4].getY());
                dataPoint1[4] = new DataPoint(4, feeds.get(0).field1);

                LineGraphSeries<DataPoint> series1 = new LineGraphSeries<DataPoint>(dataPoint1);
                graphView1.addSeries(series1);

                // Display data light level
                graphView2.removeAllSeries();
                dataPoint2[0] = new DataPoint(0, dataPoint2[1].getY());
                dataPoint2[1] = new DataPoint(1, dataPoint2[2].getY());
                dataPoint2[2] = new DataPoint(2, dataPoint2[3].getY());
                dataPoint2[3] = new DataPoint(3, dataPoint2[4].getY());
                dataPoint2[4] = new DataPoint(4, feeds.get(0).field2);

                LineGraphSeries<DataPoint> series2 = new LineGraphSeries<DataPoint>(dataPoint2);
                graphView2.addSeries(series2);

            }

            @Override
            public void onStreamError(List<FeedsModel> feeds) {
                Log.d(TAG, "onStreamError: " + feeds);
            }
        };

        callAsynchronousTask();
    }

    public void callAsynchronousTask() {
        final Handler handler = new Handler();
        Timer timer = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            new lab.graph.AsyncTask.JsonAsyncTask(context, jsonListener).execute("https://api.thingspeak.com/channels/728948/feeds.json?results=2");
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            Log.d(TAG, "run exception: " + e);
                        }
                    }
                });
            }
        };
        timer.schedule(doAsynchronousTask, 0, 5000); //execute in every 5 second
    }
}
