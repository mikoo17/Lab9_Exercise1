package com.example.lab9_exercise1;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private SensorManager sensorManager;
    private ListView sensorListView;
    private Button configureButton;
    private List<Sensor> sensors;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorListView = findViewById(R.id.sensor_list_view);
        configureButton = findViewById(R.id.configure_button);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Pobierz listę dostępnych sensorów
        sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);

        // Lista sensorów, które są dostępne na urządzeniu
        List<String> sensorNames = new ArrayList<>();
        for (Sensor sensor : sensors) {
            sensorNames.add(sensor.getName());
        }

        // Użycie ArrayAdapter do wyświetlania listy sensorów w ListView
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_multiple_choice,  // Używamy multiple_choice
                sensorNames);
        sensorListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);  // Umożliwiamy wielokrotny wybór
        sensorListView.setAdapter(adapter);

        // Obsługa kliknięcia przycisku "Konfiguruj sensory"
        configureButton.setOnClickListener(v -> {
            // Zbieramy wybrane sensory
            List<Sensor> selectedSensors = new ArrayList<>();
            for (int i = 0; i < sensorListView.getCount(); i++) {
                if (sensorListView.isItemChecked(i)) {
                    selectedSensors.add(sensors.get(i));
                }
            }

            // Jeśli nie wybrano żadnego sensora, wyświetlamy komunikat
            if (selectedSensors.isEmpty()) {
                Toast.makeText(this, "Proszę wybrać przynajmniej jeden sensor", Toast.LENGTH_SHORT).show();
            } else {
                // Tworzymy tablicę typów wybranych sensorów
                int[] sensorTypes = new int[selectedSensors.size()];
                for (int i = 0; i < selectedSensors.size(); i++) {
                    sensorTypes[i] = selectedSensors.get(i).getType();
                }

                // Przekazujemy wybrane sensory do SensorConfigActivity
                Intent intent = new Intent(MainActivity.this, SensorConfigActivity.class);
                intent.putExtra("sensorTypes", sensorTypes);
                startActivity(intent);
            }
        });
    }
}
