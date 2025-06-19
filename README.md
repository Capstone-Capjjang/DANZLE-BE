# Danzle-BE

## 💡 Project Overview

This repository contains the **entire backend structure** of **DANZLE**, a K-pop dance learning and accuracy evaluation service. The system integrates **Spring Boot** and **Flask** servers using **Docker**, and evaluates the user's dance moves in **real time by comparing them with expert poses**.

<br>

## 📁 Directory Structure

DANZLE-BE/
├── docker-compose.yml            # Manages overall backend execution</br>
├── .env_spring                               # Environment variables for Spring server</br>
├── .env_flask                                  # Environment variables for Flask server</br>
│</br>
├── spring_server/                         # Spring Boot API server</br>
│   ├── Dockerfile                           # For multi-stage build</br>
│   ├── build.gradle                        # Gradle project settings</br>
│   ├── settings.gradle</br>
│   └── src/                                       # Java source code</br>
│       ├── main/java/...</br>
│       └── main/resources/...</br>
│</br>
├── flask_server/                            # Flask-based pose evaluation server</br>
│   ├── Dockerfile</br>
│   ├── flask_pose_eval.py           # Real-time pose evaluation API</br>
│   ├── pose.py                               # BDP-based evaluation logic</br>
│   ├── requirements.txt</br>
│   └── ref_poses/                          # Reference pose JSON data</br>
│       └── <song>_ref_pose_filtered_1sec_normalized.json</br>

</br>

## ⚙️ Tech Stacks

| Component | Technologies |
| --- | --- |
| Backend API | Spring Boot, Java 21, Gradle |
| Pose Evaluation | Python 3.10, Flask, MediaPipe, OpenCV |
| Server Infra | Docker, Docker Compose |
| Communication | RESTful API (Spring ↔ Flask) |
| Pose Algorithm | BDP (Basic Directional Pose), Cosine Similarity |

<br>

## 📍Features

- Captures user video every second as frames
- Flask server extracts and evaluates poses using MediaPipe
- Calculates similarity against expert poses (BDP-based)
- Spring server receives results from Flask and returns them to the user
- Flask provides `/analyze` and `/health` API
- Results are shared via a Docker volume (`/srv/shared`)

<br>

## 🔗 pring ↔ Flask Integration

Spring Boot internally uses `WebClient` or `RestTemplate` to send a POST request to:

`http://flask-app:5000/analyze`, delivering user pose images.

The Flask server analyzes the image and returns the evaluation result as JSON. Both containers communicate directly via a Docker network (`capston_app-network`).

Analysis results are stored in `/app/output`, which is mounted to `/srv/shared` so that Spring can directly access them.

</br>

## 🚀 How to Run (for developers)

### Requirements

- Docker 24.x or higher
- docker-compose v2 or higher
- Linux-compatible server (ex. Ubuntu, EC2)

### Steps to Run

1. Clone the repository:

```bash
git clone https://github.com/Capstone-Capjjang/DANZLE-BE.git
cd DANZLE-BE
```

1. Create environment variable files:

```bash
vim .env_spring
vim .env_flask
```

1. Build and run the backend:

```bash
docker-compose up --build -d
```

<br>

## 🔐 Environment Variables

| File | Key Environment Variables |
| --- | --- |
| `.env_spring` | DB URL, JWT secret, S3 keys, etc. |
| `.env_flask` | AWS S3 credentials, FLASK_ENV, evaluation settings |

> `.env_*` files contain sensitive information and are provided with **only variable names**, not actual values, in Git.
> 

</br>

## 🐳 docker-compose.yml Overview

- `spring-app`: Spring Boot app, exposes port 8080
- `flask-app`: Flask pose evaluation server, exposes port 5000
- `redis`: Used for temporary token storage (e.g., password reset), port 6379

</br>

## 📝 Additional Notes

- The `Dockerfile` in `spring_server/` uses a multi-stage build to generate and run the `.jar` file via Gradle.
- The `.jar` file is not included in the GitHub repository and is generated during build.
- The backend uses Docker Compose to ensure a consistent build and deployment environment for both development and production.
