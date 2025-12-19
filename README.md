# MotorMonitor Android App
MotorMonitor is a native Android application designed for real-time monitoring and control of a DC motor. The app receives data via Bluetooth and visualizes critical metrics through dynamic charts, providing an elegant and professional dashboard for electromedical or industrial systems.

## 🚀 Main Features
Real-time Monitoring: Visualizes 4 key motor parameters:

**Voltage (V):** Displayed in Volts.

**Current (A):** Displayed in Amperes.

**Speed (RPM):** Displayed in Revolutions Per Minute.

**Duty Cycle (%):** Displayed as a percentage (0-100).

**Interactive Charts:** Powered by MPAndroidChart with smooth scrolling, auto-scaling, and gradient styling.

**Remote Control:** Dedicated ON and OFF buttons to actuate a relay via Bluetooth.

**Responsive Design:** Horizontal layout with a smooth scrollable interface, optimized for dashboard viewing.

**Premium UI:** Dark-themed design using Material Design Cards for a modern look.
## 🛠️ Tech Stack
Language: Kotlin

Framework: Android Jetpack (Views)

Library: MPAndroidChart

Protocol: Bluetooth SPP (Serial Port Profile)
## 📡 Communication Protocol
From Hardware (Arduino) to App
The hardware must send a *comma-separated* string followed by a newline character (\n) every ~500ms.

Format: voltage,current,rpm,dutycycle\n

Example: 12.34,1.56,2400,85\n

From App to Hardware
The application sends plain text commands with a newline character:

START Button: Sends "ON\n"

STOP Button: Sends "OFF\n"

## 📱 Hardware Requirements
Microcontroller: Arduino Uno, Nano, ESP32, or similar.

Bluetooth Module: HC-05 or HC-06.

Sensors: Voltage divider, ACS712 (Current), and Tachometer/Encoder (RPM).

Actuator: Relay module for motor control.
