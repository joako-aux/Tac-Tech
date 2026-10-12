package com.example.tac_tech.ui.admin

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.*
import com.example.tac_tech.R
import com.example.tac_tech.data.CompanyRepository
import com.example.tac_tech.data.EmployeeRepository
import com.example.tac_tech.data.SupportAccountRepository
import com.example.tac_tech.model.Company
import com.example.tac_tech.model.Employee

class SupportAccountsActivity : Activity() {

    private lateinit var companyRepo: CompanyRepository
    private lateinit var employeeRepo: EmployeeRepository
    private lateinit var accountRepo: SupportAccountRepository

    private lateinit var companySpinner: Spinner
    private lateinit var employeeSpinner: Spinner
    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var createButton: Button
    private lateinit var message: TextView

    private var companies: List<Company> = emptyList()
    private var employees: List<Employee> = emptyList()
    private var selectedCompanyId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_support_accounts)

        companyRepo = CompanyRepository(this)
        employeeRepo = EmployeeRepository(this)
        accountRepo = SupportAccountRepository(this)

        companySpinner = findViewById(R.id.spSupportCompany)
        employeeSpinner = findViewById(R.id.spSupportEmployee)
        username = findViewById(R.id.etSupportUsername)
        password = findViewById(R.id.etSupportPassword)
        createButton = findViewById(R.id.btnCreateSupportAccount)
        message = findViewById(R.id.tvSupportAccountMessage)

        findViewById<Button>(R.id.btnBackAccounts)?.setOnClickListener { finish() }

        createButton.isEnabled = false

        companySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCompanyId = companies.getOrNull(position)?.id
                loadEmployees()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedCompanyId = null
                employees = emptyList()
                createButton.isEnabled = false
            }
        }

        createButton.setOnClickListener { createAccount() }

        loadCompanies()
    }

    private fun loadCompanies() {
        companies = companyRepo.getAll()

        companySpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            companies.map { it.name }
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        if (companies.isEmpty()) {
            message.text = "Primero registra una empresa."
        }
    }

    private fun loadEmployees() {
        val companyId = selectedCompanyId

        employees = if (companyId == null) {
            emptyList()
        } else {
            employeeRepo.getByCompany(companyId)
        }

        employeeSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            employees.map { it.name }
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        createButton.isEnabled = employees.isNotEmpty()

        message.text = if (employees.isEmpty()) {
            "No hay técnicos activos para esta empresa."
        } else {
            "Selecciona el técnico al que asignarás una cuenta."
        }
    }

    private fun createAccount() {
        val companyId = selectedCompanyId
        val position = employeeSpinner.selectedItemPosition

        if (companyId == null || position !in employees.indices) {
            message.text = "Selecciona una empresa y un técnico."
            return
        }

        val employee = employees[position]

        if (employee.companyId != companyId) {
            message.text = "El técnico no pertenece a esta empresa."
            return
        }

        val user = username.text.toString().trim()
        val pass = password.text.toString()

        if (user.length < 4) {
            username.error = "Mínimo 4 caracteres"
            return
        }

        if (pass.length < 8) {
            password.error = "Mínimo 8 caracteres"
            return
        }

        createButton.isEnabled = false

        try {
            accountRepo.createAccount(
                employee.id,
                user,
                pass
            )

            username.text.clear()
            password.text.clear()

            message.text = "Cuenta creada para ${employee.name}. Rol: SOPORTE."

            Toast.makeText(this, "Cuenta de soporte creada", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            message.text = e.message ?: "No se pudo crear la cuenta."
        } finally {
            createButton.isEnabled = employees.isNotEmpty()
        }
    }

    override fun onDestroy() {
        companyRepo.close()
        employeeRepo.close()
        accountRepo.close()
        super.onDestroy()
    }
}