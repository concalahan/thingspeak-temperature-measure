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

import lab.graph.AsyncTask.JsonAsyncTask;

public class MainActivity extends AppCompatActivity {
    String TAG = "HCMUT";

    private lab.graph.AsyncTask.JsonAsyncTask.JsonListener jsonListener;
    private Context context = this;
    private GraphView graphView;

    private DataPoint dataPoint[] = {
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

        graphView = findViewById(R.id.graph);

        jsonListener = new JsonAsyncTask.JsonListener() {
            @Override
            public void onStreamResponse(List<FeedsModel> feeds) {
                Log.d(TAG, "-----------------------------");
                Log.d(TAG, "field1: " + feeds.get(0).field1);
                Log.d(TAG, "-----------------------------");
                Log.d(TAG, "field2: " + feeds.get(1).field2);

                graphView.removeAllSeries();

                dataPoint[0] = new DataPoint(0, dataPoint[1].getY());
                dataPoint[1] = new DataPoint(1, dataPoint[2].getY());
                dataPoint[2] = new DataPoint(2, dataPoint[3].getY());
                dataPoint[3] = new DataPoint(3, dataPoint[4].getY());
                dataPoint[4] = new DataPoint(4, feeds.get(0).field1);
                dataPoint[4] = new DataPoint(4, feeds.get(1).field2);

                LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(dataPoint);
                graphView.addSeries(series);

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
