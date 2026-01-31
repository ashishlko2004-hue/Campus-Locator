package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CampusMapView extends View {

    public interface OnBuildingClickListener {
        void onBuildingClick(Building building);
    }

    private Paint routePaint, startMarkerPaint, endMarkerPaint, textPaint, routeGlowPaint, userLocationPaint;
    private List<Building> buildings = new ArrayList<>();
    private Path routePath = new Path();
    private Point startPoint, endPoint;
    private OnBuildingClickListener buildingClickListener;
    private Bitmap mapBitmap;

    private Matrix imageToViewMatrix = new Matrix();
    private Matrix transformMatrix = new Matrix();
    private Matrix fullTransformMatrix = new Matrix();
    private Matrix invertMatrix = new Matrix();
    
    private float imgWidth, imgHeight;
    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;
    private float scaleFactor = 1.0f;
    private boolean is3DMode = false;
    private Context context;

    // --- GPS Calibration Data ---
    private double refLat1 = 26.834962, refLng1 = 80.836733; // Main Gate
    private float refX1 = 920, refY1 = 722; 
    
    private double refLat2 = 26.834028, refLng2 = 80.836861; // Central Park
    private float refX2 = 904, refY2 = 515;

    private Point userPixelLocation = null;

    public static Map<String, Point> locations = new HashMap<>();
    public static Map<Point, List<Point>> graph = new HashMap<>();

    public CampusMapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init(context);
    }

    private void init(Context context) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false; 
        mapBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.campus_map_bg, options);
        
        if (mapBitmap != null) {
            imgWidth = mapBitmap.getWidth();
            imgHeight = mapBitmap.getHeight();
        }

        routePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        routePaint.setColor(Color.parseColor("#00BFFF")); 
        routePaint.setStrokeWidth(5f); 
        routePaint.setStyle(Paint.Style.STROKE);
        routePaint.setStrokeCap(Paint.Cap.ROUND);
        routePaint.setPathEffect(new DashPathEffect(new float[]{15, 10}, 0));

        routeGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        routeGlowPaint.setColor(Color.parseColor("#4400BFFF"));
        routeGlowPaint.setStrokeWidth(10f);
        routeGlowPaint.setStyle(Paint.Style.STROKE);
        routeGlowPaint.setStrokeCap(Paint.Cap.ROUND);

        startMarkerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        endMarkerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        userLocationPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        userLocationPaint.setColor(Color.BLUE);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLACK); 
        textPaint.setTextSize(18f);
        textPaint.setFakeBoldText(true);
        textPaint.setShadowLayer(8, 0, 0, Color.WHITE); 

        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());

        setupLocations();
    }

    public void updateUserLocation(double lat, double lng) {
        float x = (float) (refX1 + (lng - refLng1) * (refX2 - refX1) / (refLng2 - refLng1));
        float y = (float) (refY1 + (lat - refLat1) * (refY2 - refY1) / (refLat2 - refLat1));
        userPixelLocation = new Point((int)x, (int)y);
        invalidate();
    }

    private void drawMarkerPin(Canvas canvas, float x, float y, int color) {
        Paint pinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pinPaint.setColor(color);
        Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(Color.parseColor("#44000000"));
        canvas.drawCircle(x, y, 4f, shadowPaint);
        canvas.drawCircle(x, y - 18, 10f, pinPaint);
        Path path = new Path();
        path.moveTo(x - 10, y - 18);
        path.lineTo(x + 10, y - 18);
        path.lineTo(x, y);
        path.close();
        canvas.drawPath(path, pinPaint);
        Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(Color.WHITE);
        canvas.drawCircle(x, y - 18, 4f, dotPaint);
    }

    public void setOnBuildingClickListener(OnBuildingClickListener listener) {
        this.buildingClickListener = listener;
    }

    public void set3DMode(boolean enabled) {
        this.is3DMode = enabled;
        invalidate();
    }

    private void setupLocations() {
        buildings.clear();
        locations.clear();
        graph.clear();

        // --- Destinations ---
        addNamedLocation("Main gate", 920, 722, -25, 30, true); 
        addNamedLocation("ATM", 868, 684, -15, -15, true);
        addNamedLocation("Central park", 904, 515, 0, 0, false); 
        addNamedLocation("Green park", 998, 634, 0, 20, true);
        addNamedLocation("Ground", 1153, 529, 0, 20, true); 
        addNamedLocation("Helipad", 1159, 386, 0, 20, true); 
        addNamedLocation("Junior ground", 1039, 330, -20, 25, true); 
        addNamedLocation("Auditorium", 775, 646, -45, 25, true);
        addNamedLocation("Academic block a1", 846, 345, 0, 5, true); 
        addNamedLocation("Academic block a2", 970, 380, 0, 5, true); // Updated Coords
        addNamedLocation("Administrative building", 906, 323, -25, 5, true); 

        Point pMainGate = locations.get("main gate");
        Point pATM = locations.get("atm");
        Point pPark = locations.get("central park");
        Point pGreenPark = locations.get("green park");
        Point pGround = locations.get("ground");
        Point pHelipad = locations.get("helipad");
        Point pJuniorGround = locations.get("junior ground");
        Point pAuditorium = locations.get("auditorium");
        Point pAcademicA1 = locations.get("academic block a1");
        Point pAcademicA2 = locations.get("academic block a2");
        Point pAdminBuilding = locations.get("administrative building");

        // --- Path Nodes ---
        Point pPathATM = new Point(911, 679);
        Point pStraight1 = new Point(904, 562); 
        Point pStraightRight = new Point(992, 562); 
        Point pGroundNode1 = new Point(1023, 572);
        Point pGroundNode2 = new Point(1091, 566);
        Point pGroundNode3 = new Point(1082, 521); 
        
        Point pHelipadNode1 = new Point(1078, 461);
        Point pHelipadNode2 = new Point(1131, 466);
        Point pHelipadNode3 = new Point(1120, 366);

        Point pAudNode1 = new Point(828, 522);
        Point pAudNode2 = new Point(825, 627);

        Point pAcadABNode1 = new Point(847, 396); 
        Point pAcadABNode2 = new Point(890, 385); 
        Point pAcadStart = new Point(789, 530);
        
        // NEW Path Nodes for A2
        Point pA2NewNode1 = new Point(924, 578);
        Point pA2NewNode2 = new Point(994, 570);

        addNode(pMainGate);
        addNode(pPathATM);
        addNode(pATM);
        addNode(pStraight1);
        addNode(pPark);
        addNode(pStraightRight);
        addNode(pGreenPark);
        addNode(pGroundNode1);
        addNode(pGroundNode2);
        addNode(pGroundNode3);
        addNode(pGround);
        addNode(pHelipadNode1);
        addNode(pHelipadNode2);
        addNode(pHelipadNode3);
        addNode(pHelipad);
        addNode(pJuniorGround);
        addNode(pAudNode1);
        addNode(pAudNode2);
        addNode(pAuditorium);
        addNode(pAcademicA1);
        addNode(pAcadABNode1);
        addNode(pAcadABNode2);
        addNode(pAcademicA2);
        addNode(pAdminBuilding);
        addNode(pAcadStart);
        addNode(pA2NewNode1);
        addNode(pA2NewNode2);

        // --- Connections ---
        addEdge(pMainGate, pPathATM);
        addEdge(pPathATM, pATM);
        
        addEdge(pMainGate, pA2NewNode1); // Connecting Gate to new A2 node
        addEdge(pA2NewNode1, pStraight1);
        addEdge(pStraight1, pPark);
        
        addEdge(pStraight1, pStraightRight);
        addEdge(pStraightRight, pGreenPark);
        
        addEdge(pStraightRight, pGroundNode1);
        addEdge(pGroundNode1, pGroundNode2);
        addEdge(pGroundNode2, pGroundNode3);
        addEdge(pGroundNode3, pGround);
        
        addEdge(pGroundNode3, pHelipadNode1);
        addEdge(pHelipadNode1, pHelipadNode2);
        addEdge(pHelipadNode2, pHelipadNode3);
        addEdge(pHelipadNode3, pHelipad);

        addEdge(pGround, pHelipad);
        addEdge(pHelipadNode3, pJuniorGround); 
        addEdge(pHelipadNode1, pJuniorGround); 

        addEdge(pStraight1, pAudNode1);
        addEdge(pAudNode1, pAudNode2);
        addEdge(pAudNode2, pAuditorium);

        addEdge(pATM, pAuditorium);

        addEdge(pStraight1, pAcadStart);
        addEdge(pAcadStart, pAcadABNode1);
        addEdge(pAcadABNode1, pAcademicA1);
        addEdge(pAcadABNode1, pAcadABNode2);
        addEdge(pAcadABNode2, pAdminBuilding);
        
        // NEW Route to A2
        addEdge(pA2NewNode1, pA2NewNode2);
        addEdge(pA2NewNode2, pAcademicA2);
    }

    private void addNamedLocation(String name, int x, int y, float labelOffsetX, float labelOffsetY, boolean showLabel) {
        Point p = new Point(x, y);
        locations.put(name.toLowerCase(), p);
        buildings.add(new Building(name, "", new RectF(x - 20, y - 20, x + 20, y + 20), labelOffsetX, labelOffsetY, showLabel));
    }

    private void addNode(Point p) { if (!graph.containsKey(p)) graph.put(p, new ArrayList<>()); }
    private void addEdge(Point p1, Point p2) { graph.get(p1).add(p2); graph.get(p2).add(p1); }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mapBitmap == null) return;

        RectF src = new RectF(0, 0, imgWidth, imgHeight);
        RectF dst = new RectF(0, 0, getWidth(), getHeight());
        imageToViewMatrix.setRectToRect(src, dst, Matrix.ScaleToFit.CENTER);

        fullTransformMatrix.set(imageToViewMatrix);
        fullTransformMatrix.postConcat(transformMatrix);

        if (is3DMode) {
            Matrix tiltMatrix = new Matrix();
            tiltMatrix.setSkew(-0.1f, 0, getWidth()/2f, getHeight()/2f);
            fullTransformMatrix.postConcat(tiltMatrix);
        }

        fullTransformMatrix.invert(invertMatrix);

        canvas.save();
        canvas.concat(fullTransformMatrix);

        // Draw Map
        canvas.drawBitmap(mapBitmap, 0, 0, null);

        // Draw Route
        if (!routePath.isEmpty()) {
            canvas.drawPath(routePath, routeGlowPaint);
            canvas.drawPath(routePath, routePaint);
            
            if(startPoint != null && endPoint != null){
                drawMarkerPin(canvas, startPoint.x, startPoint.y, Color.parseColor("#4CAF50")); // Green Start
                drawMarkerPin(canvas, endPoint.x, endPoint.y, Color.parseColor("#F44336")); // Red Dest
            }
        }

        // DRAW USER BLUE DOT
        if (userPixelLocation != null) {
            userLocationPaint.setAlpha(50);
            canvas.drawCircle(userPixelLocation.x, userPixelLocation.y, 20f, userLocationPaint);
            userLocationPaint.setAlpha(255);
            canvas.drawCircle(userPixelLocation.x, userPixelLocation.y, 10f, userLocationPaint);
            userLocationPaint.setColor(Color.WHITE);
            canvas.drawCircle(userPixelLocation.x, userPixelLocation.y, 4f, userLocationPaint);
            userLocationPaint.setColor(Color.BLUE);
        }

        // Draw Labels
        for (Building building : buildings) {
            if (building.showLabel) {
                float originalTextSize = textPaint.getTextSize();
                String displayText = building.name;
                
                if (building.name.equalsIgnoreCase("Junior ground")) {
                    textPaint.setTextSize(12f);
                }
                
                // Shortcut labels
                if (building.name.equalsIgnoreCase("Academic block a1")) {
                    displayText = "A1";
                }
                if (building.name.equalsIgnoreCase("Academic block a2")) {
                    displayText = "A2";
                }
                if (building.name.equalsIgnoreCase("administrative building")) {
                    displayText = "AB";
                }
                
                canvas.drawText(displayText, building.rect.centerX() + building.labelOffsetX,
                              building.rect.centerY() + building.labelOffsetY, textPaint);
                textPaint.setTextSize(originalTextSize);
            }
        }

        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(1.0f, Math.min(scaleFactor, 5.0f));
            transformMatrix.postScale(detector.getScaleFactor(), detector.getScaleFactor(), 
                                    detector.getFocusX(), detector.getFocusY());
            invalidate();
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            transformMatrix.postTranslate(-distanceX, -distanceY);
            invalidate();
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            float[] pts = {e.getX(), e.getY()};
            invertMatrix.mapPoints(pts);
            float imgX = pts[0];
            float imgY = pts[1];

            Toast.makeText(context, "X=" + (int)imgX + ", Y=" + (int)imgY, Toast.LENGTH_SHORT).show();

            for (Building building : buildings) {
                if (Math.abs(building.rect.centerX() - imgX) < 40 && Math.abs(building.rect.centerY() - imgY) < 40) {
                    if (buildingClickListener != null) {
                        buildingClickListener.onBuildingClick(building);
                        return true;
                    }
                }
            }
            return super.onSingleTapUp(e);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (scaleFactor > 1.0f) {
                transformMatrix.reset();
                scaleFactor = 1.0f;
            } else {
                transformMatrix.postScale(2.0f, 2.0f, e.getX(), e.getY());
                scaleFactor = 2.0f;
            }
            invalidate();
            return true;
        }
    }

    public void drawRoute(List<Point> path) {
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
        String description;
        RectF rect;
        float labelOffsetX;
        float labelOffsetY;
        boolean showLabel;
        Building(String name, String description, RectF rect, float labelOffsetX, float labelOffsetY, boolean showLabel) {
            this.name = name;
            this.description = description;
            this.rect = rect;
            this.labelOffsetX = labelOffsetX;
            this.labelOffsetY = labelOffsetY;
            this.showLabel = showLabel;
        }
    }
}
