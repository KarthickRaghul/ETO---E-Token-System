# ETO Data Flow Diagrams

This document contains flow diagrams mapping the primary workflows within the Electronic Token Online (ETO) system.

---

## 1. Patient Booking Flow
This maps how a Patient searches for a hospital, selects a doctor, and requests an appointment.

```mermaid
sequenceDiagram
    autonumber
    actor Patient
    participant App as Android Client (Patient)
    participant API as Backend REST API
    participant DB as PostgreSQL DB

    Patient->>App: Search Hospital / Specialization
    App->>API: GET /api/hospitals?search=...
    API->>DB: Query hospitals by name/specialization/symptom
    DB-->>API: Returns matching hospitals
    API-->>App: Display list of hospitals
    
    Patient->>App: Select Hospital & Department
    App->>API: GET /api/hospitals/:id/departments
    API-->>App: Display departments

    Patient->>App: Select Doctor & Preferred Time
    App->>API: GET /api/doctors?department_id=...
    API-->>App: Display doctor details & schedule availability
    
    Patient->>App: Submit Booking Request
    App->>API: POST /api/appointments
    API->>DB: INSERT INTO appointments & appointment_requests (status = 'PENDING')
    DB-->>API: Row Created
    API-->>App: Confirm Submission (Request Sent)
```

---

## 2. Receptionist Approval Flow
This maps how a receptionist reviews appointment requests and issues tokens.

```mermaid
sequenceDiagram
    autonumber
    actor Receptionist
    participant App as Android Client (Receptionist)
    participant API as Backend REST API
    participant DB as PostgreSQL DB
    participant WS as Socket.IO Hub

    Receptionist->>App: Open Portal -> View Requests
    App->>API: GET /api/appointment-requests
    API->>DB: Query pending requests
    DB-->>API: Returns pending requests
    API-->>App: Render requests list
    
    Receptionist->>App: Click 'Approve'
    App->>API: PATCH /api/appointment-requests/:id/approve
    
    Note over API, DB: Database Transaction BEGIN
    API->>DB: UPDATE appointment_requests SET status = 'APPROVED'
    API->>DB: UPDATE appointments SET status = 'APPROVED'
    API->>DB: Generate safe token_number & INSERT INTO tokens
    API->>DB: INSERT INTO queue_entries (status = 'WAITING')
    API->>DB: INSERT INTO queue_events (event_type = 'TOKEN_APPROVED')
    API->>DB: INSERT INTO notifications for Patient
    Note over API, DB: Database Transaction COMMIT
    
    DB-->>API: Transaction Complete
    API->>WS: Emit 'queue_update'
    API-->>App: Success Response
```

---

## 3. Queue Progression Flow
This maps the progression of a token from waiting to serving or being skipped.

```mermaid
stateDiagram-v2
    [*] --> PENDING : Token requested by patient
    PENDING --> APPROVED : Receptionist approves request
    APPROVED --> WAITING : Enters active queue (queue_entries)
    
    state WAITING {
        [*] --> InPosition
    }
    
    WAITING --> CALLED : Doctor/Receptionist calls patient (Call Next)
    CALLED --> SERVING : Doctor starts consultation
    CALLED --> SKIPPED : Patient did not arrive (No Show)
    
    SKIPPED --> WAITING : Doctor recalls skipped patient
    
    SERVING --> COMPLETED : Consultation completes & bill generated
    WAITING --> CANCELLED : Patient cancels token
    APPROVED --> CANCELLED : Patient cancels token
```

---

## 4. Doctor Consultation Flow
This maps the doctor's interaction during a clinical consultation, resulting in medical records and prescriptions.

```mermaid
sequenceDiagram
    autonumber
    actor Doctor
    participant App as Android Client (Doctor)
    participant API as Backend REST API
    participant DB as PostgreSQL DB
    participant WS as Socket.IO Hub

    Doctor->>App: Click "Start Consultation" on Called Patient
    App->>API: PATCH /api/tokens/:id/status (SERVING)
    API->>DB: UPDATE tokens & queue_entries status = 'SERVING'
    API->>DB: INSERT INTO consultations (status = 'IN_PROGRESS')
    API->>WS: Emit 'queue_update'
    API-->>App: Consultation Started
    
    Doctor->>App: Record Diagnosis, Notes & Prescriptions
    Doctor->>App: Click "Complete Consultation"
    App->>API: PATCH /api/consultations/:id/complete
    
    Note over API, DB: Database Transaction BEGIN
    API->>DB: UPDATE consultations SET status = 'COMPLETED'
    API->>DB: UPDATE tokens SET status = 'COMPLETED'
    API->>DB: UPDATE queue_entries SET status = 'COMPLETED'
    API->>DB: INSERT INTO medical_records
    API->>DB: INSERT INTO prescriptions & prescription_items
    API->>DB: INSERT INTO bills & bill_items (fee, etc.)
    API->>DB: INSERT INTO queue_events (event_type = 'TOKEN_COMPLETED')
    Note over API, DB: Database Transaction COMMIT
    
    DB-->>API: Success
    API->>WS: Emit 'queue_update'
    API-->>App: Consultation Completed & Bill Created
```

---

## 5. Billing Flow
This maps how a bill is generated and paid.

```mermaid
sequenceDiagram
    autonumber
    actor Patient
    actor Receptionist
    participant App as Android Client (Receptionist/Patient)
    participant API as Backend REST API
    participant DB as PostgreSQL DB
    participant WS as Socket.IO Hub

    Note over DB: Bill created on Consultation completion
    Patient->>Receptionist: Arrives at billing desk
    Receptionist->>App: Open Pending Bills
    App->>API: GET /api/bills?status=PENDING
    API->>DB: Query pending bills
    DB-->>API: Returns bills
    API-->>App: Render bills list
    
    Patient->>Receptionist: Pay cash / card / UPI
    Receptionist->>App: Record Payment
    App->>API: POST /api/payments
    
    Note over API, DB: Database Transaction BEGIN
    API->>DB: INSERT INTO payments (status = 'SUCCESS')
    API->>DB: UPDATE bills SET status = 'PAID'
    API->>DB: INSERT INTO audit_logs (action = 'PAYMENT_RECORDED')
    Note over API, DB: Database Transaction COMMIT
    
    DB-->>API: Payment processed
    API->>WS: Emit 'queue_update' (payment/bill changes)
    API-->>App: Receipt generated
```

---

## 6. Notifications & Real-Time Sync Flow
This maps how a database update triggers WebSocket updates and schedules a REST API sync on the Android App.

```mermaid
sequenceDiagram
    autonumber
    actor Staff as Receptionist / Doctor
    participant DB as PostgreSQL DB
    participant API as Backend REST API
    participant WS as Socket.IO Server
    participant Android as Android App (Patient/Staff)
    participant Room as Room SQLite Cache

    Staff->>API: Performs Action (e.g., Approve Token)
    API->>DB: Commit changes & Insert notification record
    DB-->>API: Success
    API->>WS: Emit event 'queue_update'
    WS-->>Android: WebSocket event received (queue_update)
    
    Note over Android: SocketManager triggers local Sync flow
    Android->>API: GET /api/tokens (Auth API call)
    API->>DB: Query fresh normalized token list
    DB-->>API: Return rows
    API-->>Android: Return list of tokens
    
    Android->>Room: Transaction: clear local tokens + insert fresh tokens
    Note over Android: Flow/StateFlow publishes changes to Compose UI
    Android-->>Staff: Compose UI redraws with fresh cache data
```
