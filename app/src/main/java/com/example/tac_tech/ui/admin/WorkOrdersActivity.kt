package com.example.tac_tech.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.tac_tech.data.ClientRepository
import com.example.tac_tech.data.CompanyRepository
import com.example.tac_tech.data.EmployeeRepository
import com.example.tac_tech.data.WorkOrderRepository
import com.example.tac_tech.model.Company
import com.example.tac_tech.model.Client
import com.example.tac_tech.model.Employee
import com.example.tac_tech.model.WorkOrder
import com.example.tac_tech.R

class WorkOrdersActivity : ComponentActivity() {

    private lateinit var spCompany: Spinner
    private lateinit var spClient: Spinner
    private lateinit var spTechnician: Spinner
    private lateinit var spPriority: Spinner

    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var btnCreate: Button
    private lateinit var btnBackToMain: Button
    private lateinit var tvOrderCount: TextView
    private lateinit var orderContainer: LinearLayout

    private lateinit var companyRepository: CompanyRepository
    private lateinit var clientRepository: ClientRepository
    private lateinit var employeeRepository: EmployeeRepository
    private lateinit var workOrderRepository: WorkOrderRepository

    private var companies = listOf<Company>()
    private var clients = listOf<Client>()
    private var technicians = listOf<Employee>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_work_orders)

        bindViews()

        companyRepository = CompanyRepository(this)
        clientRepository = ClientRepository(this)
        employeeRepository = EmployeeRepository(this)
        workOrderRepository = WorkOrderRepository(this)

        setupPrioritySpinner()
        loadCompanies()

        spCompany.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (companies.isNotEmpty()) {
                        loadCompanyData(companies[position].id)
                    }
                }

                override fun onNothingSelected(
                    parent: android.widget.AdapterView<*>?
                ) {}
            }

        btnCreate.setOnClickListener {
            createWorkOrder()
        }

        // Navegación corregida para regresar una pantalla atrás de forma segura sin cerrar sesión
        btnBackToMain.setOnClickListener {
            finish()
        }
    }

    private fun bindViews() {
        spCompany = findViewById(R.id.spOrderCompany)
        spClient = findViewById(R.id.spOrderClient)
        spTechnician = findViewById(R.id.spOrderTechnician)
        spPriority = findViewById(R.id.spOrderPriority)

        etTitle = findViewById(R.id.etOrderTitle)
        etDescription = findViewById(R.id.etOrderDescription)
        btnCreate = findViewById(R.id.btnCreateOrder)
        btnBackToMain = findViewById(R.id.btnBackToMain)
        tvOrderCount = findViewById(R.id.tvOrderCount)
        orderContainer = findViewById(R.id.orderContainer)
    }

    private fun setupPrioritySpinner() {
        val priorities = listOf("BAJA", "MEDIA", "ALTA")
        spPriority.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            priorities
        )
    }

    private fun loadCompanies() {
        companies = companyRepository.getAll()

        spCompany.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            companies.map { it.name }
        )

        if (companies.isEmpty()) {
            clients = emptyList()
            technicians = emptyList()
            updateClientSpinner()
            updateTechnicianSpinner()
            showMessage("Primero debes registrar una empresa.")
        } else {
            loadCompanyData(companies.first().id)
        }
    }

    private fun loadCompanyData(companyId: Long) {
        clients = clientRepository.getByCompany(companyId)
        technicians = employeeRepository.getByCompany(companyId)
            .filter { it.active }

        updateClientSpinner()
        updateTechnicianSpinner()
        showWorkOrders(companyId)
    }

    private fun updateClientSpinner() {
        val names = clients.map { it.name }.ifEmpty {
            listOf("No hay clientes registrados")
        }

        spClient.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            names
        )
    }

    private fun updateTechnicianSpinner() {
        val names = listOf("Sin asignar") + technicians.map { it.name }

        spTechnician.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            names
        )
    }

    private fun createWorkOrder() {
        if (companies.isEmpty()) {
            showMessage("Registra una empresa antes de crear órdenes.")
            return
        }

        if (clients.isEmpty()) {
            showMessage("La empresa seleccionada no tiene clientes.")
            return
        }

        val title = etTitle.text.toString().trim()
        val description = etDescription.text.toString().trim()

        if (title.isBlank()) {
            etTitle.error = "Ingresa el título de la orden"
            return
        }

        if (description.isBlank()) {
            etDescription.error = "Ingresa una descripción"
            return
        }

        val company = companies[spCompany.selectedItemPosition]
        val client = clients[spClient.selectedItemPosition]
        val priority = spPriority.selectedItem.toString()

        val technicianPosition = spTechnician.selectedItemPosition
        val technicianId = if (technicianPosition > 0) {
            technicians[technicianPosition - 1].id
        } else {
            null
        }

        val order = WorkOrder(
            id = 0,
            companyId = company.id,
            clientId = client.id,
            employeeId = technicianId,
            title = title,
            description = description,
            priority = priority,
            status = if (technicianId == null) {
                "PENDIENTE"
            } else {
                "ASIGNADA"
            }
        )

        try {
            workOrderRepository.create(order)

            showMessage("Orden de trabajo creada correctamente.")

            etTitle.text.clear()
            etDescription.text.clear()

            showWorkOrders(company.id)
        } catch (e: Exception) {
            showMessage("No se pudo crear la orden: ${e.message}")
        }
    }

    private fun showWorkOrders(companyId: Long) {
        val orders = workOrderRepository.getByCompany(companyId)

        orderContainer.removeAllViews()
        tvOrderCount.text = "Órdenes registradas: ${orders.size}"

        if (orders.isEmpty()) {
            val emptyText = TextView(this)
            emptyText.text = "Todavía no hay órdenes de trabajo."
            emptyText.textSize = 16f
            emptyText.setPadding(8, 16, 8, 16)
            orderContainer.addView(emptyText)
            return
        }

        orders.forEach { order ->
            val item = TextView(this)

            val technicianName = technicians
                .firstOrNull { it.id == order.employeeId }
                ?.name ?: if (order.employeeId == null) {
                "Sin asignar"
            } else {
                "Técnico asignado"
            }

            item.text = """
                Orden #${order.id}: ${order.title}
                Descripción: ${order.description}
                Prioridad: ${order.priority}
                Estado: ${order.status}
                Técnico: $technicianName
            """.trimIndent()

            item.textSize = 15f
            item.setPadding(16, 16, 16, 16)

            val separator = View(this)
            separator.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
            )
            separator.setBackgroundColor(0xFFCCCCCC.toInt())

            orderContainer.addView(item)
            orderContainer.addView(separator)
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()

        companyRepository.close()
        clientRepository.close()
        employeeRepository.close()
        workOrderRepository.close()
    }
}