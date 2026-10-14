package com.example.tac_tech.ui.admin

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.example.tac_tech.R
import com.example.tac_tech.data.ClientRepository
import com.example.tac_tech.data.CompanyRepository
import com.example.tac_tech.model.Client
import com.example.tac_tech.model.Company

class ClientsActivity : Activity() {

    private lateinit var clientRepository: ClientRepository
    private lateinit var companyRepository: CompanyRepository

    private lateinit var btnBackToMain: Button
    private lateinit var companySpinner: Spinner
    private lateinit var clientContainer: LinearLayout
    private lateinit var tvCount: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var addButton: Button

    private var companies: List<Company> = emptyList()
    private var selectedCompanyId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clients)

        clientRepository = ClientRepository(this)
        companyRepository = CompanyRepository(this)

        btnBackToMain = findViewById(R.id.btnBackToMain)
        companySpinner = findViewById(R.id.spCompany)
        clientContainer = findViewById(R.id.clientContainer)
        tvCount = findViewById(R.id.tvClientCount)
        tvEmpty = findViewById(R.id.tvClientEmpty)
        addButton = findViewById(R.id.btnAddClient)

        btnBackToMain.setOnClickListener {
            finish()
        }

        addButton.isEnabled = false

        addButton.setOnClickListener {
            showClientForm(null)
        }

        loadCompanies()
    }

    private fun loadCompanies() {
        companies = companyRepository.getAll()

        if (companies.isEmpty()) {
            selectedCompanyId = null
            addButton.isEnabled = false
            clientContainer.removeAllViews()
            tvCount.text = "Clientes registrados: 0"
            tvEmpty.text = "Primero debes registrar una empresa."
            tvEmpty.visibility = View.VISIBLE
            companySpinner.isEnabled = false
            return
        }

        companySpinner.isEnabled = true

        val names = companies.map { it.name }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            names
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        companySpinner.adapter = adapter

        companySpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedCompanyId = companies[position].id
                    addButton.isEnabled = true
                    loadClients()
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {
                    selectedCompanyId = null
                    addButton.isEnabled = false
                    clientContainer.removeAllViews()
                }
            }
    }

    private fun loadClients() {
        clientContainer.removeAllViews()

        val companyId = selectedCompanyId ?: return
        val clients = clientRepository.getByCompany(companyId)

        tvCount.text = "Clientes registrados: ${clients.size}"
        tvEmpty.text = "No hay clientes para esta empresa."
        tvEmpty.visibility =
            if (clients.isEmpty()) View.VISIBLE else View.GONE

        clients.forEach { client ->
            clientContainer.addView(createClientCard(client))
        }
    }

    private fun createClientCard(client: Client): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setBackgroundColor(android.graphics.Color.WHITE)
        }

        val title = TextView(this).apply {
            text = client.name
            textSize = 18f
            setTextColor(android.graphics.Color.rgb(36, 86, 138))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val details = TextView(this).apply {
            text = "Correo: ${client.email.ifBlank { "No ingresado" }}\n" +
                    "Teléfono: ${client.phone.ifBlank { "No ingresado" }}"
            textSize = 14f
            setPadding(0, dp(6), 0, dp(6))
        }

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val editButton = Button(this).apply {
            text = "Editar"
            setOnClickListener {
                showClientForm(client)
            }
        }

        val deactivateButton = Button(this).apply {
            text = "Desactivar"
            setOnClickListener {
                confirmDeactivate(client)
            }
        }

        actions.addView(
            editButton,
            LinearLayout.LayoutParams(0, dp(48), 1f)
        )

        actions.addView(
            deactivateButton,
            LinearLayout.LayoutParams(0, dp(48), 1f)
        )

        card.addView(title)
        card.addView(details)
        card.addView(actions)

        card.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(12)
        }

        return card
    }

    private fun showClientForm(client: Client?) {
        val companyId = selectedCompanyId ?: run {
            toast("Selecciona una empresa primero")
            return
        }

        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(8), dp(22), 0)
        }

        val nameInput = EditText(this).apply {
            hint = "Nombre completo *"
            setSingleLine(true)
            setText(client?.name.orEmpty())
        }

        val emailInput = EditText(this).apply {
            hint = "Correo electrónico *"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setText(client?.email.orEmpty())
        }

        val phoneInput = EditText(this).apply {
            hint = "Teléfono"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            setText(client?.phone.orEmpty())
        }

        val companyLabel = TextView(this).apply {
            text = "Empresa: " +
                    (companies.find { it.id == companyId }?.name ?: "")
            setPadding(0, dp(12), 0, dp(4))
        }

        form.addView(companyLabel)
        form.addView(nameInput)
        form.addView(emailInput)
        form.addView(phoneInput)

        val dialog = AlertDialog.Builder(this)
            .setTitle(
                if (client == null) "Registrar cliente"
                else "Editar cliente"
            )
            .setView(form)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    val name = nameInput.text.toString().trim()
                    val email = emailInput.text.toString().trim()
                    val phone = phoneInput.text.toString().trim()

                    if (name.isBlank()) {
                        nameInput.error = "Ingresa el nombre"
                        return@setOnClickListener
                    }

                    // Validación del formato del correo electrónico (debe incluir @ y estructura válida)
                    if (email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        emailInput.error = "Ingresa un correo electrónico válido"
                        return@setOnClickListener
                    }

                    if (client != null && client.companyId != companyId) {
                        toast("El cliente no pertenece a esta empresa")
                        return@setOnClickListener
                    }

                    try {
                        if (client == null) {
                            clientRepository.insert(
                                name,
                                email,
                                phone,
                                companyId
                            )
                        } else {
                            clientRepository.update(
                                client.copy(
                                    name = name,
                                    email = email,
                                    phone = phone
                                )
                            )
                        }

                        dialog.dismiss()
                        loadClients()
                        toast("Cliente guardado correctamente")
                    } catch (e: Exception) {
                        toast(e.message ?: "No se pudo guardar el cliente")
                    }
                }
        }

        dialog.show()
    }

    private fun confirmDeactivate(client: Client) {
        AlertDialog.Builder(this)
            .setTitle("Desactivar cliente")
            .setMessage(
                "¿Deseas desactivar a ${client.name}? " +
                        "Su registro histórico se conservará."
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Desactivar") { _, _ ->
                try {
                    val companyId = selectedCompanyId

                    if (companyId == null ||
                        companyId != client.companyId
                    ) {
                        toast("La empresa seleccionada no coincide")
                        return@setPositiveButton
                    }

                    val success = clientRepository.deactivate(
                        client.id,
                        companyId
                    )

                    if (success) {
                        loadClients()
                        toast("Cliente desactivado")
                    } else {
                        toast("No se pudo desactivar el cliente")
                    }
                } catch (e: Exception) {
                    toast(e.message ?: "Ocurrió un error")
                }
            }
            .show()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        clientRepository.close()
        companyRepository.close()
        super.onDestroy()
    }
}