package com.example.tac_tech.ui.admin

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.example.tac_tech.R
import com.example.tac_tech.data.CompanyRepository
import com.example.tac_tech.data.EmployeeRepository
import com.example.tac_tech.model.Company
import com.example.tac_tech.model.Employee

class SupportTechniciansActivity : Activity() {

    private lateinit var companyRepo: CompanyRepository
    private lateinit var employeeRepo: EmployeeRepository

    private lateinit var spinner: Spinner
    private lateinit var container: LinearLayout
    private lateinit var count: TextView
    private lateinit var empty: TextView
    private lateinit var addButton: Button

    private var companies: List<Company> = emptyList()
    private var companyId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_support_technicians)

        companyRepo = CompanyRepository(this)
        employeeRepo = EmployeeRepository(this)

        spinner = findViewById(R.id.spTechnicianCompany)
        container = findViewById(R.id.technicianContainer)
        count = findViewById(R.id.tvTechnicianCount)
        empty = findViewById(R.id.tvTechnicianEmpty)
        addButton = findViewById(R.id.btnAddTechnician)

        findViewById<Button>(R.id.btnBackTechnicians)?.setOnClickListener { finish() }

        addButton.isEnabled = false
        addButton.setOnClickListener { showForm(null) }

        companies = companyRepo.getAll()

        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            companies.map { it.name }
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                companyId = companies.getOrNull(position)?.id
                addButton.isEnabled = companyId != null
                loadTechnicians()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                companyId = null
                addButton.isEnabled = false
                container.removeAllViews()
            }
        }

        if (companies.isEmpty()) {
            empty.text = "Primero registra una empresa."
            empty.visibility = View.VISIBLE
        }
    }

    private fun loadTechnicians() {
        container.removeAllViews()
        val id = companyId ?: return
        val employees = employeeRepo.getByCompany(id)

        count.text = "Técnicos registrados: ${employees.size}"
        empty.text = "No hay técnicos para esta empresa."
        empty.visibility = if (employees.isEmpty()) View.VISIBLE else View.GONE

        employees.forEach { employee ->
            container.addView(createCard(employee))
        }
    }

    private fun createCard(employee: Employee): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(android.graphics.Color.WHITE)
        }

        val title = TextView(this).apply {
            text = employee.name
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val details = TextView(this).apply {
            text = "Correo: ${employee.email.ifBlank { "No ingresado" }}\n" +
                    "Teléfono: ${employee.phone.ifBlank { "No ingresado" }}"
            setPadding(0, dp(6), 0, dp(6))
        }

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        buttons.addView(Button(this).apply {
            text = "Editar"
            setOnClickListener { showForm(employee) }
        }, LinearLayout.LayoutParams(0, dp(48), 1f))

        buttons.addView(Button(this).apply {
            text = "Desactivar"
            setOnClickListener { confirmDeactivate(employee) }
        }, LinearLayout.LayoutParams(0, dp(48), 1f))

        card.addView(title)
        card.addView(details)
        card.addView(buttons)

        card.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(12)
        }

        return card
    }

    private fun showForm(employee: Employee?) {
        val selectedId = companyId ?: return

        if (employee != null && employee.companyId != selectedId) {
            toast("Selecciona la empresa correcta")
            return
        }

        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }

        val name = EditText(this).apply {
            hint = "Nombre completo *"
            setText(employee?.name.orEmpty())
            setSingleLine(true)
        }

        val email = EditText(this).apply {
            hint = "Correo electrónico"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setText(employee?.email.orEmpty())
        }

        val phone = EditText(this).apply {
            hint = "Teléfono"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            setText(employee?.phone.orEmpty())
        }

        form.addView(name)
        form.addView(email)
        form.addView(phone)

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (employee == null) "Registrar técnico" else "Editar técnico")
            .setView(form)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val technicianName = name.text.toString().trim()

                if (technicianName.isBlank()) {
                    name.error = "El nombre es obligatorio"
                    return@setOnClickListener
                }

                try {
                    if (employee == null) {
                        employeeRepo.insert(
                            technicianName,
                            email.text.toString(),
                            phone.text.toString(),
                            selectedId
                        )
                    } else {
                        employeeRepo.update(
                            employee.copy(
                                name = technicianName,
                                email = email.text.toString(),
                                phone = phone.text.toString()
                            )
                        )
                    }

                    dialog.dismiss()
                    loadTechnicians()
                    toast("Técnico guardado")
                } catch (e: Exception) {
                    toast(e.message ?: "No se pudo guardar")
                }
            }
        }

        dialog.show()
    }

    private fun confirmDeactivate(employee: Employee) {
        AlertDialog.Builder(this)
            .setTitle("Desactivar técnico")
            .setMessage("¿Deseas desactivar a ${employee.name}? Se conservará su registro.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Desactivar") { _, _ ->
                try {
                    val id = companyId
                    if (id == null || id != employee.companyId) {
                        toast("La empresa seleccionada no coincide")
                        return@setPositiveButton
                    }

                    if (employeeRepo.deactivate(employee.id, id)) {
                        loadTechnicians()
                        toast("Técnico desactivado")
                    } else {
                        toast("No se pudo desactivar")
                    }
                } catch (e: Exception) {
                    toast(e.message ?: "Ocurrió un error")
                }
            }
            .show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        employeeRepo.close()
        companyRepo.close()
        super.onDestroy()
    }
}