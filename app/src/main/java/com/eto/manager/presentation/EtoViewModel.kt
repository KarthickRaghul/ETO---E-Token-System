package com.eto.manager.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eto.manager.data.local.AppDatabase
import com.eto.manager.data.repository.EtoRepositoryImpl
import com.eto.manager.domain.model.Department
import com.eto.manager.domain.model.Doctor
import com.eto.manager.domain.model.PaymentStatus
import com.eto.manager.domain.model.Token
import com.eto.manager.domain.model.TokenStatus
import com.eto.manager.domain.repository.EtoRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotificationItem(
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "SMS" // SMS, PUSH, IN_APP
)

enum class UserRole {
    PATIENT,
    RECEPTIONIST,
    DOCTOR,
    ADMIN
}

class EtoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: EtoRepository

    // UI Roles and Navigation States
    private val _currentRole = MutableStateFlow(UserRole.PATIENT)
    val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    // Patient Context
    val patientName = MutableStateFlow("John Doe")
    val patientPhone = MutableStateFlow("9876543210")
    private val _selectedDoctorId = MutableStateFlow<String?>(null)
    val selectedDoctorId = _selectedDoctorId.asStateFlow()
    val symptomsInput = MutableStateFlow("")

    // Doctor Context
    private val _selectedDoctorForView = MutableStateFlow<String>("d1") // Default to Dr. Sarah
    val selectedDoctorForView = _selectedDoctorForView.asStateFlow()

    // Simulated Phone Alerts
    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications = _notifications.asStateFlow()

    // Simulation controls
    private val _isSimulationActive = MutableStateFlow(true)
    val isSimulationActive = _isSimulationActive.asStateFlow()
    private var simulationJob: Job? = null

    // Room DB streams
    val doctors: StateFlow<List<Doctor>>
    val departments: StateFlow<List<Department>>
    val tokens: StateFlow<List<Token>>

    init {
        val db = AppDatabase.getDatabase(application)
        repository = EtoRepositoryImpl(
            db.doctorDao(),
            db.departmentDao(),
            db.tokenDao()
        )

        // Seed initial data immediately
        viewModelScope.launch {
            repository.seedInitialData()
        }

        doctors = repository.getDoctors().stateIn(
            viewModelScope, SharingStarted.Lazily, emptyList()
        )
        departments = repository.getDepartments().stateIn(
            viewModelScope, SharingStarted.Lazily, emptyList()
        )
        tokens = repository.getTokens().stateIn(
            viewModelScope, SharingStarted.Lazily, emptyList()
        )

        // Start background queue progression loop
        startSimulation()
    }

    fun setRole(role: UserRole) {
        _currentRole.value = role
    }

    fun selectDoctor(doctorId: String?) {
        _selectedDoctorId.value = doctorId
    }

    fun selectDoctorForView(doctorId: String) {
        _selectedDoctorForView.value = doctorId
    }

    // Patient actions
    fun requestToken() {
        val name = patientName.value
        val phone = patientPhone.value
        val docId = selectedDoctorId.value ?: return
        val symptoms = symptomsInput.value

        viewModelScope.launch {
            val doc = doctors.value.find { it.id == docId } ?: return@launch
            repository.requestToken(name, phone, docId, symptoms, isWalkIn = false)
            symptomsInput.value = ""
            _selectedDoctorId.value = null

            addNotification(
                title = "Token Request Sent",
                message = "Your request for ${doc.name} (${doc.departmentName}) is sent. Waiting for approval.",
                type = "PUSH"
            )
        }
    }

    private val mockNames = listOf("Alice Smith", "Bob Jones", "Charlie Brown", "Diana Prince", "Ethan Hunt", "Fiona Gallagher", "George Clark")
    private val mockPhones = listOf("9876540001", "9876540002", "9876540003", "9876540004", "9876540005", "9876540006")
    private val mockSymptoms = listOf("Persistent cough and fever", "Sharp chest pain on exertion", "Red itchy rash on arms", "Mild migraine and sensitivity to light", "Sore throat and body aches")
    private val mockDiagnoses = mapOf(
        "Cardiologist" to listOf("Mild Hypertension", "Arrhythmia", "Angina pectoris"),
        "Pediatrician" to listOf("Acute Bronchitis", "Seasonal Allergies", "Viral Fever"),
        "Dermatologist" to listOf("Contact Dermatitis", "Eczema flare-up", "Urticaria"),
        "Physician" to listOf("Common Cold", "Influenza Type A", "Acute Pharyngitis")
    )
    private val mockPrescriptions = mapOf(
        "Cardiologist" to listOf("Amlodipine 5mg daily", "Metoprolol 25mg daily", "Atorvastatin 10mg daily"),
        "Pediatrician" to listOf("Amoxicillin suspension", "Cetirizine syrup", "Paracetamol drops"),
        "Dermatologist" to listOf("Hydrocortisone cream 1%", "Cetirizine 10mg", "Desonide lotion"),
        "Physician" to listOf("Paracetamol 500mg as needed", "Ibuprofen 400mg", "Saline nasal spray")
    )

    // Receptionist actions
    fun approveToken(token: Token) {
        viewModelScope.launch {
            repository.updateTokenStatus(token.id, "APPROVED")
            addNotification(
                title = "Token Approved",
                message = "Token ${token.tokenNumber} is approved! You are now in the active waiting queue for ${token.doctorName}.",
                type = "SMS"
            )
        }
    }

    fun rejectToken(token: Token) {
        viewModelScope.launch {
            repository.updateTokenStatus(token.id, "SKIPPED")
            addNotification(
                title = "Token Cancelled",
                message = "Your token request ${token.tokenNumber} was rejected by receptionist.",
                type = "SMS"
            )
        }
    }

    fun callNextPatient(doctorId: String) {
        viewModelScope.launch {
            val activeTokens = tokens.value.filter { it.doctorId == doctorId && it.status == TokenStatus.APPROVED }
            val currentServing = tokens.value.find { it.doctorId == doctorId && it.status == TokenStatus.SERVING }
            
            // Mark current serving as skipped (not completed, doctor finalizes completed)
            if (currentServing != null) {
                repository.updateTokenStatus(currentServing.id, "SKIPPED")
            }
            
            // Call next
            val next = activeTokens.minByOrNull { it.id }
            if (next != null) {
                repository.updateTokenStatus(next.id, "SERVING")
                addNotification(
                    title = "Your Turn is Near",
                    message = "Token ${next.tokenNumber} (${next.patientName}): Please report to ${next.doctorName}'s consultation room immediately.",
                    type = "SMS"
                )
            }
        }
    }

    fun skipPatient(token: Token) {
        viewModelScope.launch {
            repository.updateTokenStatus(token.id, "SKIPPED")
            addNotification(
                title = "Token Skipped",
                message = "Token ${token.tokenNumber} was marked as skipped. Please contact receptionist to recall.",
                type = "SMS"
            )
        }
    }

    fun registerWalkIn(name: String, phone: String, doctorId: String, symptoms: String) {
        viewModelScope.launch {
            val doc = doctors.value.find { it.id == doctorId } ?: return@launch
            repository.requestToken(name, phone, doctorId, symptoms, isWalkIn = true)
            addNotification(
                title = "Walk-in Registered",
                message = "Walk-in patient $name registered for ${doc.name}.",
                type = "IN_APP"
            )
        }
    }

    // Doctor actions
    fun completeConsultation(token: Token, diagnosis: String, prescription: String, fee: Double) {
        viewModelScope.launch {
            repository.recordConsultation(token.id, diagnosis, prescription, fee)
            addNotification(
                title = "Consultation Complete",
                message = "Your consultation with ${token.doctorName} is complete. Prescription: $prescription. Bill: ₹$fee. Please pay at reception.",
                type = "SMS"
            )
        }
    }

    // Billing actions
    fun recordPayment(token: Token) {
        viewModelScope.launch {
            repository.recordPayment(token.id)
            addNotification(
                title = "Payment Confirmed",
                message = "Payment of ₹${token.billAmount} for token ${token.tokenNumber} confirmed. Receipt sent.",
                type = "SMS"
            )
        }
    }

    // Simulation Logic
    fun toggleSimulation() {
        _isSimulationActive.value = !_isSimulationActive.value
        if (_isSimulationActive.value) {
            startSimulation()
        } else {
            simulationJob?.cancel()
        }
    }

    private fun startSimulation() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            while (true) {
                delay(12000) // progress queue every 12 seconds
                if (!_isSimulationActive.value) break
                
                val r = Math.random()

                // 1. Spawning new patient tokens (30% chance)
                if (r < 0.3) {
                    val availableDocs = doctors.value.filter { it.isAvailable }
                    if (availableDocs.isNotEmpty()) {
                        val doc = availableDocs.random()
                        val patientName = mockNames.random()
                        val patientPhone = mockPhones.random()
                        val symptoms = mockSymptoms.random()
                        
                        val isWalkIn = Math.random() > 0.5
                        repository.requestToken(patientName, patientPhone, doc.id, symptoms, isWalkIn)
                        
                        addNotification(
                            title = if (isWalkIn) "Walk-in Registered (Simulated)" else "Online Appointment Requested (Simulated)",
                            message = "Patient $patientName registered for ${doc.name}.",
                            type = if (isWalkIn) "IN_APP" else "PUSH"
                        )
                    }
                }
                // 2. Approving pending online requests (25% chance)
                else if (r < 0.55) {
                    val pendingTokens = tokens.value.filter { it.status == TokenStatus.PENDING }
                    val oldestPending = pendingTokens.minByOrNull { it.id }
                    if (oldestPending != null) {
                        repository.updateTokenStatus(oldestPending.id, "APPROVED")
                        addNotification(
                            title = "Appointment Approved (Simulated)",
                            message = "Token ${oldestPending.tokenNumber} for ${oldestPending.patientName} approved by front desk.",
                            type = "SMS"
                        )
                    }
                }
                // 3. Simulating doctor consultation completion (20% chance)
                else if (r < 0.75) {
                    val servingTokens = tokens.value.filter { it.status == TokenStatus.SERVING }
                    if (servingTokens.isNotEmpty()) {
                        val tokenToComplete = servingTokens.random()
                        val doc = doctors.value.find { it.id == tokenToComplete.doctorId }
                        val specialty = doc?.specialty ?: "Physician"
                        val diagnosisList = mockDiagnoses[specialty] ?: mockDiagnoses["Physician"]!!
                        val prescriptionList = mockPrescriptions[specialty] ?: mockPrescriptions["Physician"]!!
                        
                        val diagnosis = diagnosisList.random()
                        val prescription = prescriptionList.random()
                        val fee = 300.0 + (System.currentTimeMillis() % 5) * 100
                        
                        repository.recordConsultation(tokenToComplete.id, diagnosis, prescription, fee)
                        
                        addNotification(
                            title = "Consultation Complete (Simulated)",
                            message = "Dr. ${tokenToComplete.doctorName} completed consultation for ${tokenToComplete.patientName}. Bill: ₹${fee.toInt()}.",
                            type = "SMS"
                        )
                    }
                }
                // 4. Auto calling next patient if doctor is idle and has waiting list (15% chance)
                else if (r < 0.9) {
                    val activeDocs = doctors.value.filter { it.isAvailable }
                    for (doc in activeDocs) {
                        val docQueue = tokens.value.filter { it.doctorId == doc.id }
                        val serving = docQueue.find { it.status == TokenStatus.SERVING }
                        val approved = docQueue.filter { it.status == TokenStatus.APPROVED }.sortedBy { it.id }
                        
                        if (serving == null && approved.isNotEmpty()) {
                            val next = approved.first()
                            repository.updateTokenStatus(next.id, "SERVING")
                            addNotification(
                                title = "Calling Patient (Simulated)",
                                message = "Token ${next.tokenNumber} (${next.patientName}): Please proceed to ${next.doctorName}'s room.",
                                type = "SMS"
                            )
                            break
                        }
                    }
                }
                // 5. Simulating invoice payment (10% chance)
                else {
                    val pendingPayments = tokens.value.filter { it.status == TokenStatus.COMPLETED && it.paymentStatus == PaymentStatus.PENDING }
                    val payment = pendingPayments.minByOrNull { it.id }
                    if (payment != null) {
                        repository.recordPayment(payment.id)
                        addNotification(
                            title = "Bill Paid (Simulated)",
                            message = "Payment of ₹${payment.billAmount.toInt()} received for ${payment.patientName} (${payment.tokenNumber}).",
                            type = "IN_APP"
                        )
                    }
                }
            }
        }
    }

    private fun addNotification(title: String, message: String, type: String) {
        val newItem = NotificationItem(title, message, type = type)
        _notifications.value = listOf(newItem) + _notifications.value
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAll()
            _notifications.value = emptyList()
        }
    }
}
