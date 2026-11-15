# 🚀 OrbitalVitals — Autonomous Deep Space Biomedical Monitor & Decision Support

![OrbitalVitals Logo](logo-OrbitalVitals.jpg)

> **"OrbitalVitals is the real-life 'Omnitrix' for astronauts: a tactical wearable and autonomous system that turns NASA Open Science data into a wrist-worn doctor capable of saving lives when Earth is 20 minutes of silence away."**

---

## 🌌 Overview

During deep-space missions (Moon, Mars transit, and planetary EVAs), communication delays with Earth reach up to **20 minutes each way (40 minutes round-trip)** or suffer complete radio blackouts. Astronauts cannot wait for Houston or interpret complex clinical spreadsheets during a medical crisis inside a pressurized spacesuit.

**OrbitalVitals** is an **autonomous, 100% offline-first biomedical monitoring and clinical decision-support engine** designed for spacecraft avionics and tactical wrist wearables:

- **Ultra-Light Space-Grade Footprint:** Runs in **under 24 MB of RAM** (`-Xms8m -Xmx24m`) in pure **Java 25** (Virtual Threads, Stream Gatherers, Records, and lock-free Ring Buffers) with **zero external dependencies**.
- **Predictive Statistical Triage:** Uses embedded **EWMA** (Exponentially Weighted Moving Average) + **Z-Score** deviation against each astronaut's personal pre-flight baseline.
- **4 Physiological Systems Monitored:** Cardiovascular, Musculoskeletal, Neuro-Ocular (**SANS**), and Immune/Radiation.
- **Actionable Countermeasures & Store-and-Forward Outbox:** Explains *why* an alert triggered (citing the exact **NASA OSDR** study) and *what to do right now*, while queuing alert packets locally to synchronize with Earth once communication resumes.
- **Multilingual Out-of-the-Box:** Instant real-time switching between **English (`EN`)**, **Portuguese (`PT`)**, and **Spanish (`ES`)**.

---

## 🧬 Scientific Validation — Official NASA Data (OSDR & RadLab)

All mathematical models and clinical thresholds are calibrated using real datasets from the **NASA Open Science Data Repository (OSDR)** and **NASA RadLab API**:

| Physiological System | Monitored Biomarkers & Metrics | Official NASA OSDR Datasets & APIs |
| :--- | :--- | :--- |
| **1. Cardiovascular** | Heart Rate (`BPM`), `HRV (RMSSD ms)`, Systolic/Diastolic BP, `SpO2 (%)`, **Aortic Peak Blood Velocity (`cm/s`)** | **`OSD-435` (`LSDS-22`)** Aortic Doppler Echocardiography & **`OSD-575` (`LSDS-8`)** *SpaceX Inspiration4* Cardiovascular Panel |
| **2. Musculoskeletal** | Monthly Bone Loss (`%/mo`), Cumulative **DEXA Loss (`%`)**, **Vertebral Compression Strength (`%`)** | **`OSD-108`**, **`OSD-310` (`LSDS-30`)**, **`OSD-471`** (Vertebral Biomechanics) & **`OSD-942`** (64-Day Human Bed-Rest DEXA) |
| **3. Neuro-Ocular (SANS)** | Intraocular Pressure (`IOP mmHg`), Optic Disc Edema (`mm`), **Optic Nerve Tilt (`°`)** | **`OSD-815`, `OSD-816`, `OSD-817`, `OSD-818` (`LSDS-136`)** Human *Ex-Vivo* Spaceflight-Associated Neuro-Ocular Syndrome Models |
| **4. Immune & Radiation** | External vs. Shielded Dose (`mGy/d`), Cumulative Dose (`mGy`), **Neutrophil-to-Lymphocyte Ratio (`NLR`)**, **C-Reactive Protein (`CRP mg/L`)** | **NASA RadLab API** (78% Hull vs. 32% EVA Suit Shielding), **`OSD-569` (`LSDS-7`)** & **`OSD-575` (`LSDS-8`)** *SpaceX Inspiration4* Hematology & Cytokines |

For complete mathematical formulas and dataset traceability, see [Validacao-Cientifica-NASA-OSDR.md](Validacao-Cientifica-NASA-OSDR.md).

---

## ⚡ Quick Start (Run in 5 Seconds)

### Prerequisites
- **JDK 25** (uses modern standard library features with zero external dependencies).

### Linux / macOS / Git Bash
```bash
./run.sh
```

### Windows (PowerShell)
```powershell
mkdir -Force build
javac -encoding UTF-8 -d build (Get-ChildItem -Recurse src -Filter *.java | ForEach-Object { $_.FullName })
java -Xms8m -Xmx24m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -cp build main
```

Then open the interactive tactical dashboard in your browser:
👉 **`http://localhost:8080`**

*(Optional CLI single-run diagnostic mode: `java -cp build main en --once`)*
