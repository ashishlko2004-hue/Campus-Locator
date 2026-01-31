# 🎓 Campus Locator App

A smart navigation system designed for university campuses. This app helps students and visitors find the shortest path between buildings using advanced algorithms and live GPS tracking.

## 🚀 Key Features

- **📍 Shortest Path Navigation:** Uses **Dijkstra's Algorithm** to find the most efficient route between any two locations.
- **🗺️ Interactive Custom Map:** A high-resolution custom map overlay with **Pinch-to-Zoom** and **Pan** support.
- **📡 Live GPS Tracking:** Real-time user location tracking displayed as a **Blue Dot** on the custom campus map.
- **🕹️ Demo Mode:** A simulation feature to demonstrate live tracking movement even when not physically present on campus.
- **📐 3D Perspective View:** Toggle between 2D and 3D views for a modern mapping experience.
- **🔐 Secure Access:** Integrated with **Firebase Authentication** for user login and registration.

## 🛠️ Tech Stack

- **Android Studio** (Java/XML)
- **Firebase Auth** (User Management)
- **Google Play Services Location** (GPS Integration)
- **Custom Graph Data Structure** (Mapping of campus nodes and edges)

## 📌 Implementation Details

- **Pathfinding:** The app converts campus locations into a mathematical graph. When a destination is selected, Dijkstra's algorithm calculates the shortest weight path through nodes.
- **Coordinate Mapping:** Uses **Linear Interpolation** to map real-world Latitude/Longitude coordinates onto specific pixel coordinates (X, Y) of the campus image.

## 📥 Installation

1. Clone this repository.
2. Add your own `google-services.json` file inside the `app/` directory (for Firebase features).
3. Build and Run on a real Android device for the best GPS experience.

---
**Developed for College Project Submission** 🚀
