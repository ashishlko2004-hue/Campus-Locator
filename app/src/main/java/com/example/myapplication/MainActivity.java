package com.example.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private Spinner startLocationSpinner;
    private Spinner destinationSpinner;
    private Button findRouteButton;
    private MaterialCardView logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startLocationSpinner = findViewById(R.id.start_location_spinner);
        destinationSpinner = findViewById(R.id.destination_spinner);
        findRouteButton = findViewById(R.id.find_route_button);
        logoutButton = findViewById(R.id.logout_button);

        ArrayList<String> locationNames = new ArrayList<>(Arrays.asList(
                "main gate", "central park", "green park", "helipad", "junior ground", "academic block a1", "academic block a2", "administrative building", "atm", "auditorium", "iet building", "library", "cafeteria", "ground"
        ));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, locationNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        startLocationSpinner.setAdapter(adapter);
        destinationSpinner.setAdapter(adapter);

        AdapterView.OnItemSelectedListener colorChangeListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                try {
                    ((TextView) parent.getChildAt(0)).setTextColor(Color.BLACK);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };

        startLocationSpinner.setOnItemSelectedListener(colorChangeListener);
        destinationSpinner.setOnItemSelectedListener(colorChangeListener);

        findRouteButton.setOnClickListener(v -> {
            String startName = startLocationSpinner.getSelectedItem().toString();
            String destName = destinationSpinner.getSelectedItem().toString();

            if (startName.isEmpty() || destName.isEmpty() || startName.equals(destName)) {
                Toast.makeText(MainActivity.this, "Please select valid and different start and destination", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(MainActivity.this, MapActivity.class);
            intent.putExtra("START_LOCATION", startName);
            intent.putExtra("DEST_LOCATION", destName);
            startActivity(intent);
        });

        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}