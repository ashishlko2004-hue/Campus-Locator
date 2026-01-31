package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class MapActivity extends AppCompatActivity implements CampusMapView.OnBuildingClickListener {

    private CampusMapView campusMapView;
    private TextView startLocationText, destinationLocationText, distanceText;
    private ImageButton backButton;
    private ExtendedFloatingActionButton btn3dToggle, btnDemoMode;
    private boolean is3D = false;
    private boolean isDemoActive = false;

    // GPS variables
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    // Demo Mode Variables
    private Location demoBaseLocation = null;
    private double currentSimulatedLat = 26.834962; // Defaults to Main Gate
    private double currentSimulatedLng = 80.836733;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        campusMapView = findViewById(R.id.map_view_on_new_page);
        startLocationText = findViewById(R.id.route_info_start);
        destinationLocationText = findViewById(R.id.route_info_destination);
        distanceText = findViewById(R.id.route_info_distance);
        backButton = findViewById(R.id.back_button_map_page);
        btn3dToggle = findViewById(R.id.btn_3d_toggle);
        btnDemoMode = findViewById(R.id.btn_demo_mode);

        campusMapView.setOnBuildingClickListener(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setupLocationUpdates();

        String startName = getIntent().getStringExtra("START_LOCATION");
        String destName = getIntent().getStringExtra("DEST_LOCATION");

        if (startName != null && destName != null) {
            startLocationText.setText("From: " + startName.toUpperCase());
            destinationLocationText.setText("To: " + destName.toUpperCase());

            Point startPoint = CampusMapView.locations.get(startName.toLowerCase());
            Point endPoint = CampusMapView.locations.get(destName.toLowerCase());

            if (startPoint != null && endPoint != null) {
                // Calculate simulated GPS for the selected start location
                calculateSimulatedStart(startPoint.x, startPoint.y);

                List<Point> path = findShortestPath(startPoint, endPoint);
                if (path != null && !path.isEmpty()) {
                    campusMapView.drawRoute(path);
                    double distance = calculateDistance(path);
                    distanceText.setText(String.format("Distance: %.0f units", distance));
                }
            }
        }

        backButton.setOnClickListener(v -> finish());
        
        btn3dToggle.setOnClickListener(v -> {
            is3D = !is3D;
            campusMapView.set3DMode(is3D);
            btn3dToggle.setText(is3D ? "2D View" : "3D View");
        });

        btnDemoMode.setOnClickListener(v -> {
            isDemoActive = !isDemoActive;
            if (isDemoActive) {
                btnDemoMode.setText("Stop Demo");
                demoBaseLocation = null;
                // Instant update at the selected starting location
                campusMapView.updateUserLocation(currentSimulatedLat, currentSimulatedLng);
                Toast.makeText(this, "Demo started from " + startName, Toast.LENGTH_SHORT).show();
            } else {
                btnDemoMode.setText("Demo Mode");
                Toast.makeText(this, "Demo Mode Off", Toast.LENGTH_SHORT).show();
            }
        });

        checkLocationPermission();
    }

    // Helper to map pixels back to GPS for Demo simulation
    private void calculateSimulatedStart(int x, int y) {
        // Calibration values
        double refLat1 = 26.834962, refLng1 = 80.836733; // Main Gate
        float refX1 = 922, refY1 = 692;
        double refLat2 = 26.834028, refLng2 = 80.836861; // Central Park
        float refX2 = 904, refY2 = 515;

        double scaleX = (refX2 - refX1) / (refLng2 - refLng1);
        double scaleY = (refY2 - refY1) / (refLat2 - refLat1);

        currentSimulatedLng = refLng1 + (x - refX1) / scaleX;
        currentSimulatedLat = refLat1 + (y - refY1) / scaleY;
    }

    private void setupLocationUpdates() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        if (isDemoActive) {
                            if (demoBaseLocation == null) {
                                demoBaseLocation = location;
                            }
                            double latDiff = location.getLatitude() - demoBaseLocation.getLatitude();
                            double lngDiff = location.getLongitude() - demoBaseLocation.getLongitude();
                            campusMapView.updateUserLocation(currentSimulatedLat + latDiff, currentSimulatedLng + lngDiff);
                        } else {
                            campusMapView.updateUserLocation(location.getLatitude(), location.getLongitude());
                        }
                    }
                }
            }
        };
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
    }

    @Override
    public void onBuildingClick(CampusMapView.Building building) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(building.name)
                .setMessage(building.description)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private double calculateDistance(List<Point> path) {
        double totalDistance = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            Point p1 = path.get(i);
            Point p2 = path.get(i + 1);
            totalDistance += Math.hypot(p2.x - p1.x, p2.y - p1.y);
        }
        return totalDistance;
    }

    private List<Point> findShortestPath(Point start, Point end) {
        Map<Point, Double> distances = new HashMap<>();
        Map<Point, Point> predecessors = new HashMap<>();
        PriorityQueue<Point> pq = new PriorityQueue<>(Comparator.comparingDouble(distances::get));

        for (Point node : CampusMapView.graph.keySet()) {
            distances.put(node, Double.MAX_VALUE);
        }
        distances.put(start, 0.0);
        pq.add(start);

        while (!pq.isEmpty()) {
            Point current = pq.poll();
            if (current.equals(end)) break;

            if (CampusMapView.graph.get(current) == null) continue;

            for (Point neighbor : CampusMapView.graph.get(current)) {
                double distanceToNeighbor = Math.hypot(current.x - neighbor.x, current.y - neighbor.y);
                double newDist = distances.get(current) + distanceToNeighbor;

                if (newDist < distances.get(neighbor)) {
                    distances.put(neighbor, newDist);
                    predecessors.put(neighbor, current);
                    pq.remove(neighbor);
                    pq.add(neighbor);
                }
            }
        }

        if (!predecessors.containsKey(end)) return null;

        List<Point> path = new ArrayList<>();
        Point current = end;
        while (current != null) {
            path.add(current);
            current = predecessors.get(current);
        }
        Collections.reverse(path);
        return path;
    }
}