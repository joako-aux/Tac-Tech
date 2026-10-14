package com.example.tac_tech.ui.admin

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.tac_tech.R
import com.example.tac_tech.data.CompanyRepository
import com.example.tac_tech.data.UserRepository
import com.example.tac_tech.data.WorkOrderRepository
import com.example.tac_tech.model.Company
import com.example.tac_tech.model.User
import com.example.tac_tech.model.WorkOrder

class AdminWorkOrdersActivity : ComponentActivity() {

    private lateinit var spOrderCompany: Spinner
    private lateinit var spOrderClient: Spinner
    private lateinit var btnBackToMain: Button
    private lateinit var tvOrderCount: TextView
    private lateinit var orderContainer: LinearLayout

    private lateinit var companyRepository: CompanyRepository
    private lateinit var userRepository: UserRepository
    private lateinit var workOrderRepository: WorkOrderRepository

    private var companiesList: List<Company> = emptyList()
    private var clientsList: List<User> = emptyList()
    private var currentOrdersList: List<WorkOrder> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_work_orders)

        companyRepository = CompanyRepository(this)
        userRepository = UserRepository(this)
        workOrderRepository = WorkOrderRepository(this)

        spOrderCompany = findViewById(R.id.spOrderCompany)
        spOrderClient = findViewById(R.id.spOrderClient)
        btnBackToMain = findViewById(R.id.btnBackToMain)
        tvOrderCount = findViewById(R.id.tvOrderCount)
        orderContainer = findViewById(R.id.orderContainer)

        setupSpinners()
        loadOrdersSummary()

        btnBackToMain.setOnClickListener {
            finish()
        }
    }

    private fun setupSpinners() {
        companiesList = companyRepository.getAll()
        val companyNames = mutableListOf("Todas las empresas (Ver todo)")
        companyNames.addAll(companiesList.map { it.name })

        val companyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, companyNames)
        companyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spOrderCompany.adapter = companyAdapter

        spOrderCompany.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    clientsList = emptyList()
                    updateClientSpinnerForGlobal()
                    loadOrdersSummary()
                } else if (companiesList.isNotEmpty()) {
                    val selectedCompany = companiesList[position - 1]
                    updateClients(selectedCompany.id)
                    loadOrdersSummaryForCompany(selectedCompany.id)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spOrderClient.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val compPos = spOrderCompany.selectedItemPosition
                if (compPos > 0 && clientsList.isNotEmpty() && position > 0 && (position - 1) < clientsList.size) {
                    val selectedClient = clientsList[position - 1]
                    loadOrdersSummaryForClient(selectedClient.id)
                } else if (position == 0 && compPos > 0) {
                    val selectedCompany = companiesList[compPos - 1]
                    loadOrdersSummaryForCompany(selectedCompany.id)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateClientSpinnerForGlobal() {
        val clientNames = listOf("Todos los clientes")
        val clientAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, clientNames)
        clientAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spOrderClient.adapter = clientAdapter
    }

    private fun updateClients(companyId: Long) {
        clientsList = userRepository.getClientsByCompany(companyId)
        val clientNames = mutableListOf("Todos los clientes de la empresa")
        clientNames.addAll(clientsList.map { it.name })
        val clientAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, clientNames)
        clientAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spOrderClient.adapter = clientAdapter
    }

    private fun loadOrdersSummary() {
        currentOrdersList = workOrderRepository.getAllOrders()
        renderOrdersList(currentOrdersList)
    }

    private fun loadOrdersSummaryForCompany(companyId: Long) {
        currentOrdersList = workOrderRepository.getByCompany(companyId)
        renderOrdersList(currentOrdersList)
    }

    private fun loadOrdersSummaryForClient(clientId: Long) {
        val allOrders = workOrderRepository.getAllOrders()
        currentOrdersList = allOrders.filter { it.clientId == clientId }
        renderOrdersList(currentOrdersList)
    }

    private fun renderOrdersList(orders: List<WorkOrder>) {
        tvOrderCount.text = "Solicitudes registradas: ${orders.size}"
        orderContainer.removeAllViews()

        if (orders.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "No hay solicitudes de soporte registradas."
                setPadding(12, 16, 12, 16)
            }
            orderContainer.addView(emptyTv)
            return
        }

        for (order in orders) {
            // Buscamos el nombre del técnico de soporte asignado si existe un employeeId
            val technicianName = if (order.employeeId != null) {
                val techs = userRepository.getTechniciansByCompany(order.companyId)
                techs.firstOrNull { it.id == order.employeeId }?.name ?: "Técnico ID #${order.employeeId}"
            } else {
                "Sin asignar"
            }

            val cardLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 16, 16, 16)
                setBackgroundColor(android.graphics.Color.WHITE)
                isClickable = true
                isFocusable = true
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 12)
                }
                setOnClickListener {
                    showAssignTechnicianDialog(order)
                }
            }

            val tv = TextView(this).apply {
                text = "Solicitud #${order.id} - [${order.priority}] ${order.title}\n" +
                        "Descripción: ${order.description}\n" +
                        "Estado: ${order.status} | Técnico: $technicianName\n\n" +
                        "(Toca aquí para asignar/cambiar técnico)"
                setPadding(0, 4, 0, 4)
                textSize = 14f
            }
            cardLayout.addView(tv)
            orderContainer.addView(cardLayout)
        }
    }

    private fun showAssignTechnicianDialog(order: WorkOrder) {
        val techniciansList = userRepository.getTechniciansByCompany(order.companyId)

        if (techniciansList.isEmpty()) {
            Toast.makeText(this, "No hay técnicos registrados para la empresa de esta solicitud.", Toast.LENGTH_LONG).show()
            return
        }

        val techNames = techniciansList.map { it.name }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Asignar Técnico a Solicitud #${order.id}")
            .setItems(techNames) { _, which ->
                val selectedTech = techniciansList[which]

                val updated = workOrderRepository.assignTechnician(
                    orderId = order.id,
                    companyId = order.companyId,
                    employeeId = selectedTech.id
                )

                if (updated) {
                    Toast.makeText(this, "¡Técnico ${selectedTech.name} asignado correctamente!", Toast.LENGTH_SHORT).show()
                    val compPos = spOrderCompany.selectedItemPosition
                    if (compPos > 0) {
                        loadOrdersSummaryForCompany(companiesList[compPos - 1].id)
                    } else {
                        loadOrdersSummary()
                    }
                } else {
                    Toast.makeText(this, "No se pudo asignar el técnico.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::companyRepository.isInitialized) companyRepository.close()
        if (::userRepository.isInitialized) userRepository.close()
        if (::workOrderRepository.isInitialized) workOrderRepository.close()
    }
}