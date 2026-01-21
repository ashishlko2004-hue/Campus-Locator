package com.example.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CampusMapView extends View {

    // Listener for building clicks
    public interface OnBuildingClickListener {
        void onBuildingClick(Building building);
    }

    private Paint buildingPaint, pathPaint, textPaint, routePaint, startMarkerPaint, endMarkerPaint;
    private List<Building> buildings = new ArrayList<>();
    private Path routePath = new Path();
    private Point startPoint, endPoint;
    private OnBuildingClickListener buildingClickListener;

    public static Map<String, Point> locations = new HashMap<>();
    public static Map<Point, List<Point>> graph = new HashMap<>();

    public CampusMapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setOnBuildingClickListener(OnBuildingClickListener listener) {
        this.buildingClickListener = listener;
    }

    private void init() {
        // ... (paints initialization remains the same) ...
        buildingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        buildingPaint.setColor(Color.parseColor("#D3D3D3"));
        buildingPaint.setStyle(Paint.Style.FILL);

        pathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pathPaint.setColor(Color.parseColor("#A9A9A9"));
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setStrokeWidth(20f);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(45f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        routePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        routePaint.setColor(Color.parseColor("#00BFFF"));
        routePaint.setStrokeWidth(15f);
        routePaint.setStyle(Paint.Style.STROKE);
        routePaint.setStrokeCap(Paint.Cap.ROUND);
        routePaint.setPathEffect(new DashPathEffect(new float[]{0, 30}, 0));
        routePaint.setShadowLayer(20, 0, 0, Color.CYAN);

        startMarkerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        endMarkerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        buildings.clear();
        locations.clear();
        graph.clear();

        // --- LOCATIONS ---
        String mainGateName = "Main gate";
        Point mainGatePos = new Point(500, 1800);
        locations.put(mainGateName.toLowerCase(), mainGatePos);
        buildings.add(new Building(mainGateName, "Main entrance of the university", new RectF(mainGatePos.x - 250, mainGatePos.y - 60, mainGatePos.x + 250, mainGatePos.y + 60)));

        String atmName = "ATM";
        Point atmPos = new Point(200, 1650);
        locations.put(atmName.toLowerCase(), atmPos);
        buildings.add(new Building(atmName, "Cash withdrawal machine", new RectF(atmPos.x - 100, atmPos.y - 60, atmPos.x + 100, atmPos.y + 60)));

        String parkName = "Central park";
        Point parkPos = new Point(500, 1000);
        locations.put(parkName.toLowerCase(), parkPos);
        buildings.add(new Building(parkName, "A great place to relax", new RectF(parkPos.x - 150, parkPos.y - 150, parkPos.x + 150, parkPos.y + 150)));

        // --- GRAPH ---
        addNode(mainGatePos);
        addNode(atmPos);
        addNode(parkPos);
        addEdge(mainGatePos, atmPos);
        addEdge(mainGatePos, parkPos);
    }

    private void addNode(Point p) { if (!graph.containsKey(p)) graph.put(p, new ArrayList<>()); }
    private void addEdge(Point p1, Point p2) { graph.get(p1).add(p2); graph.get(p2).add(p1); }

    @Override
    protected void onDraw(Canvas canvas) {
        // ... (onDraw remains mostly the same) ...
        super.onDraw(canvas);
        for (Map.Entry<Point, List<Point>> entry : graph.entrySet()){
            for(Point neighbor : entry.getValue()){
                if (entry.getKey().hashCode() < neighbor.hashCode()) {
                    canvas.drawLine(entry.getKey().x, entry.getKey().y, neighbor.x, neighbor.y, pathPaint);
                }
            }
        }
        for (Building building : buildings) {
            canvas.drawRect(building.rect, buildingPaint);
            canvas.drawText(building.name, building.rect.centerX(), building.rect.centerY() + 15, textPaint);
        }
        if (!routePath.isEmpty()) {
            canvas.drawPath(routePath, routePaint);
            if(startPoint != null && endPoint != null){
                startMarkerPaint.setColor(Color.parseColor("#4CAF50"));
                canvas.drawCircle(startPoint.x, startPoint.y, 35f, startMarkerPaint);
                startMarkerPaint.setColor(Color.WHITE);
                canvas.drawCircle(startPoint.x, startPoint.y, 15f, startMarkerPaint);
                endMarkerPaint.setColor(Color.parseColor("#F44336"));
                canvas.drawCircle(endPoint.x, endPoint.y, 35f, endMarkerPaint);
                endMarkerPaint.setColor(Color.WHITE);
                canvas.drawCircle(endPoint.x, endPoint.y, 15f, endMarkerPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();
            for (int i = buildings.size() - 1; i >= 0; i--) {
                Building building = buildings.get(i);
                if (building.rect.contains(x, y)) {
                    if (buildingClickListener != null) {
                        buildingClickListener.onBuildingClick(building); // Notify the listener
                        return true;
                    }
                }
            }
        }
        return super.onTouchEvent(event);
    }

    public void drawRoute(List<Point> path) {
        // ... (drawRoute remains the same) ...
        if (path == null || path.size() < 2) {
            routePath.reset();
            startPoint = null;
            endPoint = null;
            invalidate();
            return;
        }
        this.startPoint = path.get(0);
        this.endPoint = path.get(path.size() - 1);
        routePath.reset();
        routePath.moveTo(path.get(0).x, path.get(0).y);
        for (int i = 1; i < path.size(); i++) {
            routePath.lineTo(path.get(i).x, path.get(i).y);
        }
        invalidate();
    }

    public static class Building {
        String name;
        String description; // Added description field
        RectF rect;

        Building(String name, String description, RectF rect) {
            this.name = name;
            this.description = description;
            this.rect = rect;
        }
    }
}