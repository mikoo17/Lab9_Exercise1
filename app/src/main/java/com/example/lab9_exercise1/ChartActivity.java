package com.example.lab9_exercise1;

import android.app.Activity;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class ChartActivity extends Activity implements SensorEventListener {

    private SensorManager sensorManager;
    private List<Sensor> selectedSensors = new ArrayList<>();
    private int[] intervals;
    private float[] thresholds;
    private Timer timer = new Timer();
    private int xIndex = 0;
    private MediaPlayer mediaPlayer;
    private boolean alarmTriggered = false;

    private LinearLayout chartsContainer;

    private Map<Sensor, Long> lastUpdateTimeMap = new HashMap<>();
    private Map<Sensor, List<Entry>> sensorDataMap = new HashMap<>();
    private Map<Sensor, TextView> sensorTextViewMap = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        chartsContainer = findViewById(R.id.chartsContainer);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);


        List<Integer> sensorTypes = getIntent().getIntegerArrayListExtra("sensorTypes");
        intervals = getIntent().getIntArrayExtra("intervals");
        thresholds = getIntent().getFloatArrayExtra("thresholds");

        if (sensorTypes != null && intervals != null && thresholds != null) {
            for (int i = 0; i < sensorTypes.size(); i++) {
                int sensorType = sensorTypes.get(i);
                Sensor sensor = sensorManager.getDefaultSensor(sensorType);
                if (sensor != null) {
                    selectedSensors.add(sensor);

                    sensorDataMap.put(sensor, new ArrayList<>());


                    LineChart lineChart = new LineChart(this);
                    lineChart.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            500
                    ));

                    TextView sensorNameTextView = new TextView(this);
                    sensorNameTextView.setText(sensor.getName());
                    sensorNameTextView.setTextSize(16f);
                    sensorNameTextView.setPadding(16, 8, 16, 0);

                    sensorTextViewMap.put(sensor, sensorNameTextView);


                    chartsContainer.addView(sensorNameTextView);
                    chartsContainer.addView(lineChart);

                    setupChart(lineChart);
                }
            }
        }

        for (Sensor sensor : selectedSensors) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
        }

        mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound); // Replace with your alarm sound resource

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> updateCharts());
            }
        }, 0, 100);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;

        if (selectedSensors.contains(sensor)) {

            long currentTime = System.currentTimeMillis();


            int sensorIndex = selectedSensors.indexOf(sensor);
            int interval = intervals[sensorIndex]; // interwał w milisekundach


            long lastUpdateTime = lastUpdateTimeMap.getOrDefault(sensor, 0L);

            if (currentTime - lastUpdateTime >= interval) {

                lastUpdateTimeMap.put(sensor, currentTime);


                float value = event.values[0];
                // Zapis danych do pliku
                saveDataToFile(sensor.getName(), currentTime, value);
                List<Entry> entriesForSensor = sensorDataMap.get(sensor);
                if (entriesForSensor != null) {
                    entriesForSensor.add(new Entry(xIndex++, value));
                }


                float threshold = thresholds[sensorIndex];
                boolean exceedsThreshold = value > threshold;


                TextView sensorNameTextView = sensorTextViewMap.get(sensor);
                if (sensorNameTextView != null) {
                    sensorNameTextView.setTextColor(exceedsThreshold ? Color.RED : Color.BLACK);
                }

            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void updateCharts() {

        for (int i = 0; i < selectedSensors.size(); i++) {
            Sensor sensor = selectedSensors.get(i);
            LineChart lineChart = (LineChart) chartsContainer.getChildAt(i * 2 + 1); // Access chart
            TextView sensorNameTextView = (TextView) chartsContainer.getChildAt(i * 2); // Access sensor name TextView


            List<Entry> entriesForSensor = sensorDataMap.get(sensor);


            if (entriesForSensor != null && !entriesForSensor.isEmpty()) {
                Entry lastEntry = entriesForSensor.get(entriesForSensor.size() - 1); // Ostatni punkt
                entriesForSensor.add(new Entry(xIndex++, lastEntry.getY())); // Dodaj punkt z tą samą wartością
            }
            LineDataSet dataSet = new LineDataSet(entriesForSensor, sensor.getName() + " Data");
            LineData lineData = new LineData(dataSet);
            lineChart.setData(lineData);
            lineChart.invalidate();
        }
    }

    private void triggerAlarm() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            try {
                mediaPlayer.setOnCompletionListener(mp -> {
                    Log.d("MediaPlayer", "Alarm sound playback completed.");
                    if (alarmTriggered) {
                        Log.d("MediaPlayer", "Restarting alarm sound.");
                        mp.start();
                    }
                });
                Log.d("MediaPlayer", "Starting MediaPlayer...");
                mediaPlayer.start(); // Rozpocznij odtwarzanie
                alarmTriggered = true; // Ustaw flagę, że alarm jest aktywny
            } catch (Exception e) {
                Log.e("MediaPlayer", "Error starting MediaPlayer: " + e.getMessage());
                e.printStackTrace();
            }
        } else if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            Log.d("MediaPlayer", "MediaPlayer is already playing.");
        } else {
            Log.e("MediaPlayer", "MediaPlayer is null. Cannot start.");
        }
    }



    private void stopAlarm() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            try {
                Log.d("MediaPlayer", "Stopping MediaPlayer...");
                mediaPlayer.stop(); // Zatrzymaj odtwarzanie
                mediaPlayer.prepare(); // Przygotuj MediaPlayer do ponownego użycia
                Log.d("MediaPlayer", "MediaPlayer successfully stopped and prepared.");
            } catch (Exception e) {
                Log.e("MediaPlayer", "Error stopping MediaPlayer: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            Log.d("MediaPlayer", "MediaPlayer is not playing or is null.");
        }
        alarmTriggered = false; // Ustaw flagę, że alarm nie jest aktywny
    }



    private void saveDataToFile(String sensorName, long timestamp, float value) {
        String fileName = "sensor_data.txt"; // Nazwa pliku (w wewnętrznej pamięci)
        String dataLine = String.format("%s,%d,%.2f\n", sensorName, timestamp, value);

        try (FileOutputStream fos = openFileOutput(fileName, MODE_APPEND);
             OutputStreamWriter writer = new OutputStreamWriter(fos)) {
            writer.write(dataLine);
            Log.d("SaveData", "Data saved: " + dataLine);
        } catch (Exception e) {
            Log.e("SaveData", "Error saving data: " + e.getMessage());
        }
    }


    private void setupChart(LineChart lineChart) {

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);

        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);

        lineChart.getDescription().setEnabled(false);
        lineChart.setDrawGridBackground(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (Sensor sensor : selectedSensors) {
            sensorManager.unregisterListener(this, sensor);
        }
        timer.cancel();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }
}
