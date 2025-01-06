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
    private int[] intervals; // Array of intervals for each sensor
    private float[] thresholds; // Array of thresholds for each sensor
    private Timer timer = new Timer();
    private int xIndex = 0;
    private MediaPlayer mediaPlayer;
    private boolean alarmTriggered = false;

    private LinearLayout chartsContainer;  // To hold dynamically created charts

    // Map to store the data for each sensor
    private Map<Sensor, List<Entry>> sensorDataMap = new HashMap<>();
    private Map<Sensor, TextView> sensorTextViewMap = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        chartsContainer = findViewById(R.id.chartsContainer); // Get the container

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Retrieve selected sensors, intervals, and thresholds from Intent
        List<Integer> sensorTypes = getIntent().getIntegerArrayListExtra("sensorTypes");
        intervals = getIntent().getIntArrayExtra("intervals"); // Retrieve array of intervals
        thresholds = getIntent().getFloatArrayExtra("thresholds"); // Retrieve array of thresholds

        if (sensorTypes != null && intervals != null && thresholds != null) {
            for (int i = 0; i < sensorTypes.size(); i++) {
                int sensorType = sensorTypes.get(i);
                Sensor sensor = sensorManager.getDefaultSensor(sensorType);
                if (sensor != null) {
                    selectedSensors.add(sensor);
                    // Create a new list of entries for this sensor
                    sensorDataMap.put(sensor, new ArrayList<>());

                    // Dynamically create LineChart views for each selected sensor
                    LineChart lineChart = new LineChart(this);
                    lineChart.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            500 // Set chart height dynamically
                    ));

                    // Create a TextView to show the sensor name
                    TextView sensorNameTextView = new TextView(this);
                    sensorNameTextView.setText(sensor.getName());
                    sensorNameTextView.setTextSize(16f); // Adjust the text size if necessary
                    sensorNameTextView.setPadding(16, 8, 16, 0); // Add padding for better readability

                    sensorTextViewMap.put(sensor, sensorNameTextView);

                    // Add the TextView and LineChart to the container
                    chartsContainer.addView(sensorNameTextView);
                    chartsContainer.addView(lineChart);

                    // Initialize chart settings for each sensor
                    setupChart(lineChart);
                }
            }
        }

        // Register listeners for the sensors
        for (Sensor sensor : selectedSensors) {
            sensorManager.registerListener(this, sensor, 300_000);
        }

        // Initialize MediaPlayer (if required)
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound); // Replace with your alarm sound resource

        // Timer to update charts periodically
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> updateCharts());
            }
        }, 0, 2000); // Default 1000ms, will be overridden later by specific sensor intervals
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (selectedSensors.contains(event.sensor)) {
            float value = event.values[0]; // Pobierz wartość czujnika comm

            // Dodaj nowy punkt danych do wykresu
            List<Entry> entriesForSensor = sensorDataMap.get(event.sensor);
            if (entriesForSensor != null) {
                entriesForSensor.add(new Entry(xIndex++, value));
            }

            // Sprawdź, czy jakikolwiek sensor przekracza próg
            // Tworzymy listę z wartościami true/false dla każdego sensora
            List<Boolean> sensorThresholdStatus = new ArrayList<>();

            // Sprawdź, czy jakikolwiek sensor przekracza próg
            for (int i = 0; i < selectedSensors.size(); i++) {
                Sensor sensor = selectedSensors.get(i);
                if (sensor.equals(event.sensor)) {
                    float threshold = thresholds[i];
                    boolean exceedsThreshold = value > threshold;
                    sensorThresholdStatus.add(exceedsThreshold); // Dodaj wynik dla bieżącego sensora

                    // Zaktualizuj kolor nazwy sensora
                    TextView sensorNameTextView = sensorTextViewMap.get(sensor);
                    if (sensorNameTextView != null) {
                        if (exceedsThreshold) {
                            sensorNameTextView.setTextColor(Color.RED);
                        } else {
                            sensorNameTextView.setTextColor(Color.BLACK);
                        }
                    }
                }
            }

            // Sprawdź, czy jakikolwiek sensor przekracza próg
            boolean anySensorExceedsThreshold = sensorThresholdStatus.contains(true); // Sprawdzamy, czy któryś sensor przekroczył próg

            // Zarządzaj alarmem na podstawie stanu sensora
            if (anySensorExceedsThreshold) {
                triggerAlarm(); // Uruchom alarm
            } else {
                stopAlarm(); // Zatrzymaj alarm
            }

            // Zapisz dane do pliku
            saveDataToFile(xIndex - 1, value);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle accuracy change (if necessary)
    }

    private void updateCharts() {
        // Update each chart with data from the corresponding sensor
        for (int i = 0; i < selectedSensors.size(); i++) {
            Sensor sensor = selectedSensors.get(i);
            LineChart lineChart = (LineChart) chartsContainer.getChildAt(i * 2 + 1); // Access chart
            TextView sensorNameTextView = (TextView) chartsContainer.getChildAt(i * 2); // Access sensor name TextView

            // Get the data for the current sensor
            List<Entry> entriesForSensor = sensorDataMap.get(sensor);
            LineDataSet dataSet = new LineDataSet(entriesForSensor, sensor.getName() + " Data"); // Use sensor name for label
            LineData lineData = new LineData(dataSet);
            lineChart.setData(lineData);
            lineChart.invalidate(); // Refresh the chart
        }
    }

    private void triggerAlarm() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            try {
                mediaPlayer.setOnCompletionListener(mp -> {
                    Log.d("MediaPlayer", "Alarm sound playback completed.");
                    if (alarmTriggered) {
                        Log.d("MediaPlayer", "Restarting alarm sound.");
                        mp.start(); // Odtwórz ponownie
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



    private void saveDataToFile(int x, float value) {
        try (FileOutputStream fos = openFileOutput("sensor_data_" + System.currentTimeMillis() + ".txt", MODE_APPEND);
             OutputStreamWriter writer = new OutputStreamWriter(fos)) {
            writer.write(String.format("Time: %d, Value: %.2f\n", x, value));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupChart(LineChart lineChart) {
        // Set up the chart properties
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setGranularity(1f); // Only show integer values
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f); // Set min value for Y-axis

        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false); // Disable right axis

        lineChart.getDescription().setEnabled(false); // Disable description
        lineChart.setDrawGridBackground(false); // Disable grid background
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
