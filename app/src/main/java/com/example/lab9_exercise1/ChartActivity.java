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
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
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
            float value = event.values[0]; // Get the sensor value

            // Get the list of entries for the current sensor
            List<Entry> entriesForSensor = sensorDataMap.get(event.sensor);

            // Add new data point for the current sensor
            if (entriesForSensor != null) {
                entriesForSensor.add(new Entry(xIndex++, value));
            }

            // Track if any sensor exceeds its threshold
            boolean anySensorExceedsThreshold = false;

            // Iterate over all selected sensors to check if any exceeds the threshold
            for (int i = 0; i < selectedSensors.size(); i++) {
                Sensor sensor = selectedSensors.get(i);
                float threshold = thresholds[i];


                if (event.sensor == sensor && value > threshold) {
                    anySensorExceedsThreshold = true; // Mark if any sensor exceeds threshold


                    // Change the color of the TextView for the corresponding sensor
                    TextView sensorNameTextView = (TextView) chartsContainer.getChildAt(i * 2);

                    sensorNameTextView.setTextColor(Color.RED); // Change color to red
                } else {

                    // Restore the color to black for sensors not exceeding the threshold
                    TextView sensorNameTextView = (TextView) chartsContainer.getChildAt(i * 2);
                    sensorNameTextView.setTextColor(Color.BLACK); // Restore color to default (black)
                }
            }

            // Trigger the alarm if any sensor exceeds its threshold
            if (anySensorExceedsThreshold && !alarmTriggered) {

                triggerAlarm();
                alarmTriggered = true; // Make sure the alarm is only triggered once
            } else if (!anySensorExceedsThreshold && alarmTriggered) {

                stopAlarm();
                alarmTriggered = false; // Update the alarm status
            }

            // Save data to file
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
            mediaPlayer.start(); // Play alarm sound
        }
    }

    private void stopAlarm() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop(); // Stop the alarm sound
            try {
                mediaPlayer.prepare(); // Re-prepare the MediaPlayer for the next use
            } catch (Exception e) {
                e.printStackTrace();
            }
            alarmTriggered = false;
        }
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
