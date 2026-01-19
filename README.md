# ⌚ Stress-Level Management via Smartwatch Monitoring

A complete end-to-end IoT solution for real-time stress detection. This system utilizes a hybrid Deep Learning model (CNN+GRU) deployed on a Wear OS smartwatch to analyze sensor data, syncs results via Firebase, and visualizes live stress events on a React web dashboard.

**Academic Project** • Machine Learning: Supervised Tehniques • Faculty of Electrical Engineering (ETF) Sarajevo

![Python](https://img.shields.io/badge/Python-3.11-blue?logo=python&logoColor=white)
![PyTorch](https://img.shields.io/badge/PyTorch-2.0-ee4c2c?logo=pytorch&logoColor=white)
![Android](https://img.shields.io/badge/Android-Wear_OS-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF?logo=kotlin&logoColor=white)
![React](https://img.shields.io/badge/React-18-61DAFB?logo=react&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Realtime_DB-FFCA28?logo=firebase&logoColor=black)
![License](https://img.shields.io/badge/License-MIT-yellow)

## 📋 Table of Contents

- [Overview](#-overview)
- [System Architecture](#%EF%B8%8F-system-architecture)
- [Demos & Live Dashboards](#-demos--live-dashboards)
- [Machine Learning Pipeline](#-machine-learning-pipeline)
- [Prototypes](#-prototypes)
- [Repository Structure](#-repository-structure)
- [Installation & Usage](#-installation--usage)
- [Results](#-results)
- [Contributors](#-contributors)

## 🎯 Overview

Stress is a prevalent health issue affecting productivity and well-being. While many wearables track heart rate, few offer **real-time, medical-grade stress classification**.

This project implements a **Multimodal Stress Monitoring System** that:

1. **Analyzes** physiological data (HRV, EDA, BVP, Temp) using Deep Learning.
2. **Performs Inference** directly on the wearable device (Edge AI).
3. **Streams** data to the cloud for remote monitoring.

The system was developed through rigorous phases: from EDA and baseline LSTM models to a refined **Hybrid CNN + GRU** architecture optimized for mobile deployment.

## 🎥 Demos & Live Dashboards

### 🔴 Live Prototypes

The wearable dashboard is currently deployed and listening for live events from wearable devices.
**[View Live Dashboard](https://stressmonitor-ed5d0.web.app/)**

The survey-based webapp is currently deployed and ready for use.
**[Visit Web App](https://stressguard-survey-a-7po2.bolt.host/)**

### 📹 Prototypes Video Showcase

[![Wearable Prototype](https://img.youtube.com/vi/G_Cdquk34x8/hqdefault.jpg)](https://www.youtube.com/watch?v=G_Cdquk34x8)
[![Survey-Based Prototype](https://img.youtube.com/vi/1iY4-zavq-E/hqdefault.jpg)](https://www.youtube.com/watch?v=1iY4-zavq-E)

*1st:  A showcase of the Wearable app performing inference and the React Dashboard updating in real-time.*

*2st:  A showcase of the Survey-based webapp performing inference in real-time.*

## ⚙️ System Architecture

The solution consists of three main components connected via the cloud:

1. **Wearable Device (Android):** Simulates sensor data collection and runs the `.pt` (PyTorch Lite) model for on-device inference.
2. **Backend (Firebase):** Acts as the real-time bridge, storing sensor readings and stress labels with timestamp validation.
3. **Frontend (React Dashboard):** A live interface for visualizing stress trends, heart rate variability, and historical logs.

![Architecture Diagram](Diagrams/architecture_diagram.png)

## 🧠 Machine Learning Pipeline

We experimented with 9 different architectures before finalizing the edge model.

![ML Pipeline Diagram](Diagrams/ml_pipeline_diagram.png)

### 1. Data Processing

- **Input:** 30-second windows of physiological signals (BVP, EDA, Temp).
- **Preprocessing:** Normalization, Sliding Window Segmentation.
- **Feature Selection:** Reduced input size to **5 critical features** (`MEAN_RR`, `SDRR`, `RMSSD`, `HR`, `pNN50`) to ensure high performance on limited smartwatch hardware.

### 2. Model Evolution

- **Baseline:** LSTM (Long Short-Term Memory).
- **Intermediate:** Bi-LSTM (inspired by xLSTM) and ResNet.
- **Top Performers:** CNN (Convolutional Neural Networks) and GRU (Gated Recurrent Units).

### 3. Final Architecture: Hybrid CNN + GRU

For the wearable application, we combined the strengths of both top models:

- **CNN Layers:** Extract spatial features (patterns in physiological spikes) from sensor signals.
- **GRU Layers:** Capture temporal dependencies over the 30-step window. It is faster and more "mobile-friendly" than a standard LSTM.
- **Performance:** The hybrid model showed realistic generalization (avoiding pure CNN overfitting) and was exported as `stress_model.pt` for Android.

![Final Model Diagram](Diagrams/model_diagram.png)

## 📱 Prototypes

### ⌚ Wearable App (Kotlin)

Built in **Android Studio**. It reads sensor data (simulated for testing stability), processes it through the PyTorch model, and displays:

- Real-time BPM.
- Stress Label (Relaxed, Stressed, Interrupted).
- Uploads data to Firebase (optimized with debounce to prevent API spam).

### 💻 Web Dashboard (React.js)

A Dark-mode UI hosted on **Firebase**.

- **Live Connection:** Listens to Firebase Realtime Database.
- **Visuals:** Dynamic cards with Sparkline charts for heart rate.
- **Alerts:** Visual feedback for "Stressed" (Red) or "Interrupted" (Yellow) states.

## 📂 Repository Structure

```text
├── Code
│   ├── Comparison              # Jupyter notebooks comparing model performance
│   ├── EDA                     # Exploratory Data Analysis notebooks
│   ├── SurveyAPI               # Server starter for survey-based webapp
│   ├── Traning                 # Training scripts for baseline and improved models
│   └── Traning-waearable       # Final Hybrid model training & .pt android export
├── Dataset
│   ├── full.csv                # Complete processed dataset
│   ├── train.csv               # Splited training dataset
│   ├── test.csv                # Splited testing dataset
│   ├── train-wearable.csv      # Optimized 5-feature train dataset
│   └── test-wearable.csv       # Optimized 5-feature test dataset
├── Documentation               # PDF Reports and Demo Video
├── Models
│   ├── base                    # 9 trained models
│   └── wearable                # stress_model.pt for Android
├── Presentations               # Midterm and Final slides
├── Prototypes
│   ├── StressMonitorDashbord   # Web Dashbord (React.js) Source
│   └── StressMonitorWearable   # Wearable App (Kotlin) Source
└── README.md
```

## 🚀 Installation & Usage

### Python Analysis (ML Models)

```bash
cd Code
# Make sure to have a virtual environment with required packages
jupyter notebook
```

### Android Wear App

1. Open Android Studio.
2. Select Open and navigate to `Prototypes/StressMonitorWearable`.
3. Read the README inside for additional setup.
4. Sync Gradle files.
5. Start an Emulator (Wear OS Square/Round) or connect a physical Galaxy Watch.
6. Run the app (Green Play Button).

### Web Dashboard

```bash
cd Prototypes/StressMonitorDashbord
npm install
npm start
```

## 📊 Results

- **Comparison:** The CNN model initially showed the highest accuracy on the static dataset.
- **Generalization:** The Hybrid CNN+GRU proved most robust for stream data, balancing precision with computational efficiency.
- **Latency:** The full pipeline (Watch -> Cloud -> Web) achieves sub-second latency for live updates.

### Final Model Performance

The Hybrid CNN+GRU model achieved a **97% accuracy** on the final test set, demonstrating robust classification across all stress categories without significant overfitting.

![Final Performance Metrics](Diagrams/performance.png)

#### Final Test Report

| Class | Precision | Recall | F1-Score | Support |
| :--- | :---: | :---: | :---: | :---: |
| **no stress** | 0.99 | 0.96 | 0.98 | 22142 |
| **interruption** | 0.96 | 0.98 | 0.97 | 11772 |
| **time pressure** | 0.95 | 0.99 | 0.97 | 7089 |
| | | | | |
| **accuracy** | | | **0.97** | **41003** |
| **macro avg** | 0.97 | 0.98 | 0.97 | 41003 |
| **weighted avg** | 0.97 | 0.97 | 0.97 | 41003 |

## 👥 Contributors

- **Emin Hadžiabdić** [@ehadziabdic]
- **Armin Memišević** [@arminn2206]
- **Muhammed Pašić** [@MuhaxD]

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.
