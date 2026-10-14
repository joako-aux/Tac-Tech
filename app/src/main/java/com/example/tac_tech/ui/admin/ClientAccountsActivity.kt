package com.example.tac_tech.ui.admin

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.*
import com.example.tac_tech.R
import com.example.tac_tech.data.ClientAccountRepository
import com.example.tac_tech.data.ClientRepository
import com.example.tac_tech.data.CompanyRepository
import com.example.tac_tech.model.Client
import com.example.tac_tech.model.Company

class ClientAccountsActivity : Activity() {

    private lateinit var companyRepository: CompanyRepository
    private lateinit var clientRepository: ClientRepository
    private lateinit var accountRepository: ClientAccountRepository

    private lateinit var btnBackToMain: Button
    private lateinit var companySpinner: Spinner
    private lateinit var clientSpinner: Spinner
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var createButton: Button
    private lateinit var message: TextView

    private var companies: List<Company> = emptyList()
    private var clients: List<Client> = emptyList()
    private var selectedCompanyId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_accounts)

        companyRepository = CompanyRepository(this)
        clientRepository = ClientRepository(this)
        accountRepository = ClientAccountRepository(this)

        btnBackToMain = findViewById(R.id.btnBackToMain)
        companySpinner = findViewById(R.id.spAccountCompany)
        clientSpinner = findViewById(R.id.spAccountClient)
        usernameInput = findViewById(R.id.etAccountUsername)
        passwordInput = findViewById(R.id.etAccountPassword)
        createButton = findViewById(R.id.btnCreateAccount)
        message = findViewById(R.id.tvAccountMessage)

        // Acción para volver al menú anterior de forma limpia
        btnBackToMain.setOnClickListener {
            finish()
        }

        createButton.isEnabled = false

        companySpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedCompanyId = companies.getOrNull(position)?.id
                    loadClients()
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {
                    selectedCompanyId = null
                    clients = emptyList()
                    createButton.isEnabled = false
                }
            }

        createButton.setOnClickListener {
            createAccount()
        }

        loadCompanies()
    }

    private fun loadCompanies() {
        companies = companyRepository.getAll()

        companySpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            companies.map { it.name }
        ).also {
            it.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )
        }

        if (companies.isEmpty()) {
            message.text = "Primero debes registrar una empresa."
            createButton.isEnabled = false
        }
    }

    private fun loadClients() {
        val companyId = selectedCompanyId

        clients = if (companyId == null) {
            emptyList()
        } else {
            clientRepository.getByCompany(companyId)
        }

        clientSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            clients.map { it.name }
        ).also {
            it.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )
        }

        createButton.isEnabled = clients.isNotEmpty()

        message.text = if (clients.isEmpty()) {
            "Esta empresa no tiene clientes activos. Registra uno primero."
        } else {
            "Selecciona el cliente al que asignarás una cuenta."
        }
    }

    private fun createAccount() {
        val companyId = selectedCompanyId
        val position = clientSpinner.selectedItemPosition

        if (companyId == null ||
            position !in clients.indices
        ) {
            message.text = "Selecciona una empresa y un cliente."
            return
        }

        val client = clients[position]

        if (client.companyId != companyId) {
            message.text = "El cliente no pertenece a la empresa seleccionada."
            return
        }

        val username = usernameInput.text.toString().trim()
        val password = passwordInput.text.toString()

        if (username.length < 4) {
            usernameInput.error = "Usa al menos 4 caracteres"
            return
        }

        if (password.length < 8) {
            passwordInput.error = "Usa al menos 8 caracteres"
            return
        }

        createButton.isEnabled = false

        try {
            accountRepository.createAccount(
                client.id,
                username,
                password
            )

            usernameInput.text.clear()
            passwordInput.text.clear()

            message.text =
                "Cuenta creada correctamente para ${client.name}."

            Toast.makeText(
                this,
                "Cuenta creada",
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {
            message.text = e.message ?: "No se pudo crear la cuenta."
        } finally {
            createButton.isEnabled = clients.isNotEmpty()
        }
    }

    override fun onDestroy() {
        companyRepository.close()
        clientRepository.close()
        accountRepository.close()
        super.onDestroy()
    }
}