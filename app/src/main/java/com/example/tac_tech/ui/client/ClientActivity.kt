package com.example.tac_tech.ui.client

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class ClientActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userId = intent.getLongExtra("USER_ID", -1L)
        val name = intent.getStringExtra("USER_NAME") ?: "Cliente"

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }

        val tvWelcome = TextView(this).apply {
            text = "TAC-TECH\n\nMENÚ CLIENTE\n\nBienvenido, $name\n"
            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }

        val btnWorkOrders = Button(this).apply {
            text = "Consultar Estado de Solicitudes"
            setOnClickListener {
                val intent = Intent(this@ClientActivity, ClientWorkOrdersActivity::class.java).apply {
                    putExtra("USER_ID", userId)
                }
                startActivity(intent)
            }
        }

        container.addView(tvWelcome)
        container.addView(btnWorkOrders)

        setContentView(container)
    }
}


