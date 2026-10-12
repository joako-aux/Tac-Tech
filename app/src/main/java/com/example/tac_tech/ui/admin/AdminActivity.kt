package com.example.tac_tech.ui.admin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.example.tac_tech.MainActivity

class AdminActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val name = intent.getStringExtra("USER_NAME") ?: "Administrador"

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }

        val textInfo = TextView(this).apply {
            text = "TAC-TECH\n\nMENÚ ADMIN\n\nBienvenido, $name\n\n" +
                    "• Gestión de empresas\n" +
                    "• Designación de clientes\n" +
                    "• Asignación de órdenes\n" +
                    "• Supervisión global\n"
            textSize = 18f
            gravity = Gravity.CENTER
        }

        val clientsButton = Button(this).apply {
            text = "Gestión de clientes"
            setOnClickListener {
                startActivity(Intent(this@AdminActivity, ClientsActivity::class.java))
            }
        }

        val clientAccountsButton = Button(this).apply {
            text = "Cuentas de acceso de clientes"
            setOnClickListener {
                startActivity(Intent(this@AdminActivity, ClientAccountsActivity::class.java))
            }
        }

        val techniciansButton = Button(this).apply {
            text = "Gestión de técnicos de soporte"
            setOnClickListener {
                startActivity(Intent(this@AdminActivity, SupportTechniciansActivity::class.java))
            }
        }

        val supportAccountsButton = Button(this).apply {
            text = "Cuentas de acceso de técnicos"
            setOnClickListener {
                startActivity(Intent(this@AdminActivity, SupportAccountsActivity::class.java))
            }
        }

        val logoutButton = Button(this).apply {
            text = "Cerrar sesión"
            setOnClickListener {
                startActivity(Intent(this@AdminActivity, MainActivity::class.java))
                finish()
            }
        }

        layout.addView(textInfo)

        val buttonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 16 }

        layout.addView(clientsButton, buttonParams)
        layout.addView(clientAccountsButton, buttonParams)
        layout.addView(techniciansButton, buttonParams)
        layout.addView(supportAccountsButton, buttonParams)
        layout.addView(logoutButton, buttonParams)

        setContentView(layout)
    }
}