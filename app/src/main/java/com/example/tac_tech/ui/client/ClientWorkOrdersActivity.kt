package com.example.tac_tech.ui.client

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.tac_tech.MainActivity
import com.example.tac_tech.R
import com.example.tac_tech.data.WorkOrderRepository
import com.example.tac_tech.model.WorkOrder

class ClientWorkOrdersActivity : ComponentActivity() {
    private lateinit var repository: WorkOrderRepository
    private lateinit var formTitle: TextView
    private lateinit var etDeviceDescription: EditText
    private lateinit var etAddress: EditText
    private lateinit var etIssueDescription: EditText
    private lateinit var btnSubmitOrder: Button
    private lateinit var btnBackToMain: Button

    private var rawUserId: Long = -1L
    private var clientId: Long = -1L
    private var orderIdToEdit: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_work_orders)

        formTitle = findViewById(R.id.formTitle)
        etDeviceDescription = findViewById(R.id.etDeviceDescription)
        etAddress = findViewById(R.id.etAddress)
        etIssueDescription = findViewById(R.id.etIssueDescription)
        btnSubmitOrder = findViewById(R.id.btnSubmitOrder)
        btnBackToMain = findViewById(R.id.btnBackToMain)

        rawUserId = intent.getLongExtra("USER_ID", -1L)
        orderIdToEdit = intent.getLongExtra("ORDER_ID", -1L)

        if (rawUserId <= 0L) {
            Toast.makeText(this, "No se identificó al cliente. Inicia sesión nuevamente.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        repository = WorkOrderRepository(this)
        clientId = repository.getClientIdByUserId(rawUserId)

        if (orderIdToEdit != -1L) {
            formTitle.text = "Editar Solicitud a Soporte"
            btnSubmitOrder.text = "Actualizar Solicitud"
            loadOrderDataForEditing(orderIdToEdit)
        } else {
            formTitle.text = "Nueva Solicitud de Soporte"
            btnSubmitOrder.text = "Crear Solicitud a Soporte"
        }

        btnSubmitOrder.setOnClickListener {
            saveOrUpdateOrder()
        }

        // Botón único para regresar limpiamente
        btnBackToMain.setOnClickListener {
            returnToClientMenu()
        }
    }

    private fun loadOrderDataForEditing(orderId: Long) {
        val orders = repository.getByClient(clientId)
        val order = orders.firstOrNull { it.id == orderId }

        if (order != null) {
            etIssueDescription.setText(order.description)
        }
    }

    private fun saveOrUpdateOrder() {
        val device = etDeviceDescription.text.toString().trim()
        val address = etAddress.text.toString().trim()
        val issue = etIssueDescription.text.toString().trim()

        if (device.isEmpty() || address.isEmpty() || issue.isEmpty()) {
            Toast.makeText(this, "Por favor completa todos los campos del formulario.", Toast.LENGTH_SHORT).show()
            return
        }

        val companyId = intent.getLongExtra("COMPANY_ID", 1L)
        val fullDescription = "Equipo: $device | Dirección: $address | Detalle: $issue"

        val realClientId = repository.getClientIdByUserId(rawUserId)

        if (orderIdToEdit == -1L) {
            val newOrder = WorkOrder(
                id = 0L,
                companyId = companyId,
                clientId = realClientId,
                employeeId = null,
                title = "Solicitud: $device",
                description = fullDescription,
                priority = "MEDIA",
                status = "PENDIENTE"
            )

            val newId = repository.create(newOrder)
            if (newId > 0) {
                Toast.makeText(this, "¡Solicitud creada exitosamente!", Toast.LENGTH_SHORT).show()
                returnToClientMenu()
            } else {
                Toast.makeText(this, "Error al crear la solicitud.", Toast.LENGTH_SHORT).show()
            }
        } else {
            val updated = repository.updateOrderDetails(orderIdToEdit, "Solicitud: $device", fullDescription)
            if (updated) {
                Toast.makeText(this, "Solicitud actualizada correctamente.", Toast.LENGTH_SHORT).show()
                returnToClientMenu()
            } else {
                Toast.makeText(this, "Error al actualizar la solicitud.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun returnToClientMenu() {
        val intent = Intent(this, ClientActivity::class.java).apply {
            putExtra("USER_ID", rawUserId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::repository.isInitialized) {
            repository.close()
        }
    }
}