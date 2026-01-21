package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import android.graphics.Point;

public class MapActivity extends AppCompatActivity implements CampusMapView.OnBuildingClickListener {

    private CampusMapView campusMapView;
    private TextView startLocationText, destinationLocationText, distanceText;
    private ImageButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        campusMapView = findViewById(R.id.map_view_on_new_page);
        startLocationText = findViewById(R.id.route_info_start);
        destinationLocationText = findViewById(R.id.route_info_destination);
        distanceText = findViewById(R.id.route_info_distance);
        backButton = findViewById(R.id.back_button_map_page);

        campusMapView.setOnBuildingClickListener(this);

        String startName = getIntent().getStringExtra("START_LOCATION");
        String destName = getIntent().getStringExtra("DEST_LOCATION");

        if (startName != null && destName != null) {
            startLocationText.setText(startName.toUpperCase());
            destinationLocationText.setText(destName.toUpperCase());

            Point startPoint = CampusMapView.locations.get(startName.toLowerCase());
            Point endPoint = CampusMapView.locations.get(destName.toLowerCase());

            if (startPoint != null && endPoint != null) {
                List<Point> path = findShortestPath(startPoint, endPoint);
                if (path != null && !path.isEmpty()) {
                    campusMapView.drawRoute(path);
                    
                    // Calculate and display the distance
                    double distance = calculateDistance(path);
                    distanceText.setText(String.format("Approx. Distance: %.0f units", distance));

                } else {
                    Toast.makeText(this, "Route not possible", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "One of the locations was not found.", Toast.LENGTH_SHORT).show();
            }
        }

        backButton.setOnClickListener(v -> finish());
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