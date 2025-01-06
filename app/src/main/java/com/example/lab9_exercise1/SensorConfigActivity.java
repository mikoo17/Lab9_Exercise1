package com.example.lab9_exercise1;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class SensorConfigActivity extends Activity {
    private LinearLayout sensorConfigsContainer;
    private Button startButton;

    private int[] sensorTypes; // Tablica typów sensorów
    private List<EditText> intervalEditTexts = new ArrayList<>();
    private List<EditText> thresholdEditTexts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_config);

        sensorConfigsContainer = findViewById(R.id.sensor_configs_container);
        startButton = findViewById(R.id.start_button);

        // Odbierz dane przekazane przez MainActivity
        sensorTypes = getIntent().getIntArrayExtra("sensorTypes");

        // Tworzymy pola konfiguracji dla każdego sensora
        for (int i = 0; i < sensorTypes.length; i++) {
            Sensor sensor = ((SensorManager) getSystemService(SENSOR_SERVICE)).getDefaultSensor(sensorTypes[i]);
            if (sensor != null) {
                // Tworzymy widżety dla sensora
                TextView sensorNameTextView = new TextView(this);
                sensorNameTextView.setText(sensor.getName());
                sensorConfigsContainer.addView(sensorNameTextView);

                // Interwał
                EditText intervalEditText = new EditText(this);
                intervalEditText.setHint("Interwał dla " + sensor.getName() + " (ms)");
                intervalEditText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                sensorConfigsContainer.addView(intervalEditText);
                intervalEditTexts.add(intervalEditText);

                // Próg alarmu
                EditText thresholdEditText = new EditText(this);
                thresholdEditText.setHint("Próg dla " + sensor.getName());
                thresholdEditText.setInputType(android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
                sensorConfigsContainer.addView(thresholdEditText);
                thresholdEditTexts.add(thresholdEditText);

                // Add text watcher to each field to monitor user input
                intervalEditText.addTextChangedListener(createTextWatcher());
                thresholdEditText.addTextChangedListener(createTextWatcher());
            }
        }

        // Aktywacja przycisku "Rozpocznij" tylko po ustawieniu wszystkich wartości
        startButton.setOnClickListener(v -> {
            // Sprawdzamy, czy wszystkie pola są wypełnione
            boolean allFieldsValid = true;
            List<Integer> intervals = new ArrayList<>();
            List<Float> thresholds = new ArrayList<>();
            for (int i = 0; i < sensorTypes.length; i++) {
                try {
                    int interval = Integer.parseInt(intervalEditTexts.get(i).getText().toString());
                    float threshold = Float.parseFloat(thresholdEditTexts.get(i).getText().toString());
                    intervals.add(interval);
                    thresholds.add(threshold);
                } catch (NumberFormatException e) {
                    allFieldsValid = false;
                    break;
                }
            }

            // Jeśli wszystkie pola są poprawne, przechodzimy do ChartActivity
            if (allFieldsValid) {
                // Convert the intervals and thresholds lists to arrays
                int[] intervalsArray = new int[intervals.size()];
                for (int i = 0; i < intervals.size(); i++) {
                    intervalsArray[i] = intervals.get(i);
                }

                float[] thresholdsArray = new float[thresholds.size()];
                for (int i = 0; i < thresholds.size(); i++) {
                    thresholdsArray[i] = thresholds.get(i);
                }

                // Przekazanie danych do ChartActivity
                Intent chartIntent = new Intent(SensorConfigActivity.this, ChartActivity.class);

                // Convert sensorTypes to an ArrayList and pass it
                ArrayList<Integer> sensorTypesList = new ArrayList<>();
                for (int sensorType : sensorTypes) {
                    sensorTypesList.add(sensorType);
                }

                chartIntent.putIntegerArrayListExtra("sensorTypes", sensorTypesList); // Pass as ArrayList
                chartIntent.putExtra("intervals", intervalsArray); // Pass the intervals array
                chartIntent.putExtra("thresholds", thresholdsArray); // Pass the thresholds array
                startActivity(chartIntent);
            } else {
                Toast.makeText(SensorConfigActivity.this, "Proszę wprowadzić prawidłowe dane", Toast.LENGTH_SHORT).show();
            }
        });

        // Initially, the button should be disabled
        startButton.setEnabled(false);
    }

    // Creates a TextWatcher to monitor changes in the EditText fields
    private TextWatcher createTextWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                checkIfAllFieldsValid();
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        };
    }

    // Checks if all the input fields are valid and enables the button if they are
    private void checkIfAllFieldsValid() {
        boolean allFieldsValid = true;
        for (int i = 0; i < sensorTypes.length; i++) {
            String interval = intervalEditTexts.get(i).getText().toString();
            String threshold = thresholdEditTexts.get(i).getText().toString();

            // Check if both fields are filled
            if (interval.isEmpty() || threshold.isEmpty()) {
                allFieldsValid = false;
                break;
            }

            try {
                Integer.parseInt(interval);  // Validate interval as integer
                Float.parseFloat(threshold);  // Validate threshold as float
            } catch (NumberFormatException e) {
                allFieldsValid = false;
                break;
            }
        }

        // Enable or disable the start button based on the validation
        startButton.setEnabled(allFieldsValid);
    }
}
