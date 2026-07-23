# ETO (Electronic Token Online) - Project Plan

ETO replaces traditional paper tokens in hospitals with an automated, real-time digital queue system. This document details the project scope, role workflows, design specifications, and implementation phases.

---

## 🎯 Project Goals

- **Reduce Patient Wait Times**: Enable remote search, booking, and real-time live queue tracking.
- **Increase Clinic Efficiency**: Automate walk-in registrations, token approvals, call progression, and billing.
- **Improve Medical Documentation**: Provide doctors with simple consulting rooms for prescriptions and diagnosis.
- **Deliver High-Fidelity UI/UX**: Enhance native mobile feel through physics-based gesture modifiers, shiny accents, and smooth Canvas drawing animations.

---

## 👥 User Roles & Flow Matrix

| Role | Core Objective | Key Activities |
| :--- | :--- | :--- |
| **Patient** | Register & track consultations | Search doctors, request tokens, watch live status progress, receive alerts |
| **Receptionist** | Front-desk management | Approve/reject token requests, register walk-ins, move active queues, record payments |
| **Doctor** | Consultation execution | Access queue, edit symptoms, prescribe medications, finalize consults |
| **Admin** | System configuration & health | View hourly throughput graphs, toggle ticker simulators, clear database |

---

## 🗓️ Implementation Roadmap

### Phase 1: Setup & Architecture Foundation `[COMPLETED]`
- Initialize multi-module Gradle layout matching Android guidelines.
- Establish clean architectural boundaries: Data layer (Room SQLite), Domain layer (Models and Interfaces), and Presentation layer (Jetpack Compose, MVVM).

### Phase 2: Room SQLite Database Persistence `[COMPLETED]`
- Define schemas for Doctors, Departments, and Tokens.
- Write Room Entity objects, DAOs (Data Access Objects), and database instances.
- Implement Repository pattern to wrap local database operations with Flow publishers.

### Phase 3: Domain Business Logic & Simulation `[COMPLETED]`
- Write viewmodels to hold UI state using Kotlin StateFlow.
- Implement a background simulation loop utilizing Kotlin Coroutines. The simulator automatically spawns walk-ins, moves queues forward, and triggers notification alerts.

### Phase 4: Custom Animation Framework (ReactBits Ports) `[COMPLETED]`
- Port advanced physics-based motion to Jetpack Compose:
  - `bounceClick()`: Spring-driven touch feedback.
  - `magnetEffect()`: Physics-based pointer-drag tracking.
  - `shimmer()`: Shader-like skeleton loader.
- Create aesthetic components: `ShinyText` (lustrous animated gradient mask) and `SpotlightCard` (interactive touch-coordinates radial lighting).

### Phase 5: High-Fidelity Portal Layouts `[COMPLETED]`
- **Patient Dashboard**: Custom Canvas progress ring with glowing pulsing halos.
- **Receptionist Panel**: Walk-in registration forms, billing grids, and action buttons.
- **Doctor Consulting Desk**: Diagnosis input forms, drug entry lines, and waiting queues.
- **Admin Dashboard**: Custom Canvas hourly spline line graph, touch hover guides, and database settings.

---

## 🎨 Visual Identity & Color Palette

The color system is optimized for dark mode to provide a modern, sleek appearance:

> [!TIP]
> **Primary Theme Palette**
> - **Midnight Black (`#01082D`)**: Scaffold window background.
> - **Navy Deep (`#041D56`)**: Core cards and surfaces.
> - **Steel Blue (`#266CA9`)**: Borders, selected tags, and outlines.
> - **Ice Cyan (`#ADE1FB`)**: Highlight alerts, progress sweeps, and gradients.
