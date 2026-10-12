package com.example.tac_tech.ui.admin


import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.example.tac_tech.R
import com.example.tac_tech.data.CompanyRepository
import com.example.tac_tech.model.Company

class CompaniesActivity : Activity() {

    private lateinit var repository: CompanyRepository
    private lateinit var container: LinearLayout
    private lateinit var tvCount: TextView
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_companies)

        repository = CompanyRepository(this)

        container = findViewById(R.id.companyContainer)
        tvCount = findViewById(R.id.tvCompanyCount)
        tvEmpty = findViewById(R.id.tvEmpty)

        findViewById<Button>(R.id.btnAddCompany).setOnClickListener {
            showCompanyForm(null)
        }

        loadCompanies()
    }

    private fun loadCompanies() {
        container.removeAllViews()

        val companies = repository.getAll()

        tvCount.text = "Empresas registradas: ${companies.size}"
        tvEmpty.visibility =
            if (companies.isEmpty()) android.view.View.VISIBLE
            else android.view.View.GONE

        companies.forEach { company ->
            container.addView(createCompanyCard(company))
        }
    }

    private fun createCompanyCard(company: Company): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setBackgroundColor(Color.WHITE)
        }

        val title = TextView(this).apply {
            text = company.name
            textSize = 19f
            setTextColor(Color.rgb(36, 86, 138))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val details = TextView(this).apply {
            text = "ID: ${company.id}\n" +
                    "Identificación tributaria: " +
                    company.taxId.ifBlank { "No ingresada" } +
                    "\nDirección: " +
                    company.address.ifBlank { "No ingresada" }
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, dp(8), 0, dp(8))
        }

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val editButton = Button(this).apply {
            text = "Editar"
            setOnClickListener { showCompanyForm(company) }
        }

        val deleteButton = Button(this).apply {
            text = "Eliminar"
            setOnClickListener { confirmDelete(company) }
        }

        actions.addView(
            editButton,
            LinearLayout.LayoutParams(0, dp(48), 1f)
        )
        actions.addView(
            deleteButton,
            LinearLayout.LayoutParams(0, dp(48), 1f)
        )

        card.addView(title)
        card.addView(details)
        card.addView(actions)

        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(12)
        }

        card.layoutParams = params

        return card
    }

    private fun showCompanyForm(company: Company?) {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(8), dp(22), 0)
        }

        val nameInput = EditText(this).apply {
            hint = "Nombre de la empresa *"
            setSingleLine(true)
            setText(company?.name.orEmpty())
        }

        val taxIdInput = EditText(this).apply {
            hint = "RUT o identificación tributaria"
            setSingleLine(true)
            setText(company?.taxId.orEmpty())
        }

        val addressInput = EditText(this).apply {
            hint = "Dirección"
            setText(company?.address.orEmpty())
        }

        form.addView(nameInput)
        form.addView(taxIdInput)
        form.addView(addressInput)

        val dialog = AlertDialog.Builder(this)
            .setTitle(
                if (company == null) "Registrar empresa"
                else "Editar empresa"
            )
            .setView(form)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    val name = nameInput.text.toString().trim()

                    if (name.isBlank()) {
                        nameInput.error = "Ingresa el nombre"
                        return@setOnClickListener
                    }

                    try {
                        if (company == null) {
                            repository.insert(
                                name,
                                taxIdInput.text.toString(),
                                addressInput.text.toString()
                            )
                        } else {
                            repository.update(
                                company.copy(
                                    name = name,
                                    taxId = taxIdInput.text.toString(),
                                    address = addressInput.text.toString()
                                )
                            )
                        }

                        dialog.dismiss()
                        loadCompanies()
                    } catch (e: Exception) {
                        nameInput.error =
                            e.message ?: "No se pudo guardar"
                    }
                }
        }

        dialog.show()
    }

    private fun confirmDelete(company: Company) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar empresa")
            .setMessage(
                "¿Quieres eliminar ${company.name}? " +
                        "Esta operación puede estar bloqueada si " +
                        "existen registros asociados."
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                try {
                    val deleted = repository.delete(company.id)

                    if (deleted) {
                        loadCompanies()
                        message("Empresa eliminada")
                    } else {
                        message("La empresa ya no existe")
                    }
                } catch (e: Exception) {
                    message(
                        e.message ?: "No se pudo eliminar la empresa"
                    )
                }
            }
            .show()
    }

    private fun message(text: String) {
        AlertDialog.Builder(this)
            .setMessage(text)
            .setPositiveButton("Aceptar", null)
            .show()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        repository.close()
        super.onDestroy()
    }
}
