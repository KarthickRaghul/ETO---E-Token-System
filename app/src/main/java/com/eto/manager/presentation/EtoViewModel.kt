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

    // Receptionist actions
    fun approveToken(token: Token) {
        viewModelScope.launch {
            repository.updateTokenStatus(token.id, "SERVING")
            addNotification(
                title = "Token Approved",
                message = "Token ${token.tokenNumber} is approved! You are in the active queue for ${token.doctorName}.",
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
            val activeTokens = tokens.value.filter { it.doctorId == doctorId && it.status == TokenStatus.PENDING }
            val currentServing = tokens.value.find { it.doctorId == doctorId && it.status == TokenStatus.SERVING }
            
            // Mark current serving as completed or skipped
            if (currentServing != null) {
                repository.updateTokenStatus(currentServing.id, "COMPLETED")
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
                delay(20000) // progress queue every 20 seconds
                if (!_isSimulationActive.value) break
                
                // Automatically approve a pending token if serving queue is empty
                val pending = tokens.value.filter { it.status == TokenStatus.PENDING }
                if (pending.isNotEmpty()) {
                    val nextApprove = pending.first()
                    // 50% chance receptionist approves it automatically
                    if (Math.random() > 0.5) {
                        repository.updateTokenStatus(nextApprove.id, "SERVING")
                        addNotification(
                            title = "Queue Update (Simulated)",
                            message = "Token ${nextApprove.tokenNumber} is now being called by ${nextApprove.doctorName}.",
                            type = "SMS"
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
