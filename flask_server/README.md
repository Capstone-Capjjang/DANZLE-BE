# Flask Server Folder Documentation

This documentation explains the purpose and logic of each file in the `flask_server/` directory.

### 🧱 Flask Server Architecture

flask_server/</br>
├── flask_pose_eval.py</br>
├── pose.py</br>
├── requirements.txt</br>
├── Dockerfile</br>
└── ref_poses/</br>
└── SuperShy_ref_pose_filtered_1sec_normalized.json</br>

---

## 📄 File Overview

### 1. `flask_pose_eval.py` ⚙️

- **Purpose**: Main Flask server that exposes HTTP endpoints for real-time pose evaluation and resource management.
- **Key Endpoints**:
    - `POST /analyze`
        - Accepts an image frame, song title, session ID, and frame index.
        - Extracts user pose via MediaPipe and compares it to reference pose.
        - Calculates pose similarity using the Basic Directional Pose (BDP) method. If two frames are available for a session, the score is extended with motion similarity through the Double Frame method.
        - Returns JSON with score, feedback category, and frame index.
    - `POST /save`
        - Clears memory and resets pose queues.
        - Triggers garbage collection and closes MediaPipe Pose instance.
    - `GET /health`
        - Health check endpoint returning `{status: 'UP'}`.

---

### 2. `pose.py` 🧠

- **Purpose**: Contains the core pose extraction and comparison logic.
- **Main Components**:
    - `extract_pose_keypoints(frame)`: Extracts and normalizes pose keypoints from a frame.
    - `compare_pose_bdp(user, ref)`: Compares a user’s pose with reference using directional vectors and angular differences.
    - `compare_pose_bdp_double_frame(...)`: Enhances BDP with motion vectors across two frames (user and reference), providing temporal continuity analysis.
    - `get_pose_detector()` / `close_pose_detector()`: Singleton pattern for efficient MediaPipe model loading and unloading.

---

### 3. `Dockerfile` 🐳

- **Purpose**: Container configuration to run the Flask server in a minimal Python environment.
- **Key Steps**:
    - Uses `python:3.10-slim` base image.
    - Installs OpenCV dependencies (e.g., libgl1, libglib2.0).
    - Installs Python dependencies via `requirements.txt`.
    - Sets performance and memory tuning environment variables.
    - Exposes port 5000.

---

### 4. `ref_poses/SuperShy_ref_pose_filtered_1sec_normalized.json` 🧍‍♂️📊

- **Purpose**: Stores pre-processed, normalized keypoint data for the expert dancer (reference pose).
- **Structure**: JSON map from frame index to normalized keypoints.
- **Usage**: Loaded and cached by `flask_pose_eval.py` upon first use to reduce repeated file I/O.

---

## 🗒️ Notes

- The server supports session-aware frame comparison with frame queues (`user_pose_queue`, `ref_pose_queue`).
- Pose score feedback labels: `Perfect`, `Good`, `Normal`, `Bad`, `Worst`.
- Logging is enabled for debugging and performance diagnostics.

---

## 📦 Requirements

- Python 3.10+
- OpenCV, MediaPipe, Flask, NumPy
- JSON-formatted expert keypoint dataset under `ref_poses/`
