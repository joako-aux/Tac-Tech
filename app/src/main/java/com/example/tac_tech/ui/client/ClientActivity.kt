package com.example.tac_tech.ui.client

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.tac_tech.MainActivity
import com.example.tac_tech.data.WorkOrderRepository

class ClientActivity : ComponentActivity() {
    private lateinit var repository: WorkOrderRepository
    private lateinit var orderContainer: LinearLayout
    private var userId: Long = -1L
    private var clientId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userId = intent.getLongExtra("USER_ID", -1L)
        val name = intent.getStringExtra("USER_NAME") ?: "Cliente"

        if (userId <= 0L) {
            Toast.makeText(this, "Sesión inválida.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        repository = WorkOrderRepository(this)
        clientId = repository.getClientIdByUserId(userId)

        // Contenedor principal
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val tvWelcome = TextView(this).apply {
            text = "TAC-TECH - Menú Cliente\nBienvenido, $name"
            textSize = 18f
            setPadding(0, 0, 0, 8)
        }
        mainLayout.addView(tvWelcome)

        val btnLogoutOrHome = Button(this).apply {
            text = "Cerrar Sesión"
            setOnClickListener {
                val intent = Intent(this@ClientActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                finish()
            }
        }
        mainLayout.addView(btnLogoutOrHome)

        val btnManageOrders = Button(this).apply {
            text = "Crear o Editar Solicitud a Soporte"
            setOnClickListener {
                val intent = Intent(this@ClientActivity, ClientWorkOrdersActivity::class.java).apply {
                    putExtra("USER_ID", userId)
                }
                startActivity(intent)
            }
        }
        mainLayout.addView(btnManageOrders)

        val tvSectionTitle = TextView(this).apply {
            text = "\nMis Solicitudes Realizadas"
            textSize = 16f
            setPadding(0, 16, 0, 8)
        }
        mainLayout.addView(tvSectionTitle)

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        orderContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        scrollView.addView(orderContainer)
        mainLayout.addView(scrollView)

        setContentView(mainLayout)
    }

    override fun onResume() {
        super.onResume()
        if (::repository.isInitialized) {
            clientId = repository.getClientIdByUserId(userId)
            loadClientOrders()
        }
    }

    private fun loadClientOrders() {
        val orders = repository.getByClient(clientId)
        orderContainer.removeAllViews()

        if (orders.isEmpty()) {
            val empty = TextView(this).apply {
                text = "No tienes solicitudes registradas actualmente."
                textSize = 14f
                setPadding(8, 16, 8, 16)
            }
            orderContainer.addView(empty)
            return
        }

        orders.forEach { order ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 16, 16, 16)
            }

            val details = TextView(this).apply {
                text = """
                    Solicitud #${order.id}: ${order.title}
                    Descripción: ${order.description}
                    Prioridad: ${order.priority}
                    Estado: ${order.status}
                """.trimIndent()
                textSize = 14f
            }
            card.addView(details)

            if (order.status == "PENDIENTE") {
                val editButton = Button(this).apply {
                    text = "Editar Solicitud"
                    setOnClickListener {
                        val intent = Intent(this@ClientActivity, ClientWorkOrdersActivity::class.java).apply {
                            putExtra("USER_ID", userId)
                            putExtra("ORDER_ID", order.id)
                        }
                        startActivity(intent)
                    }
                }
                card.addView(editButton)
            }

            val alreadyRated = order.description.contains("[Calificación Cliente]")
            if (order.status == "COMPLETADA" && !alreadyRated) {
                val rateButton = Button(this).apply {
                    text = "Calificar servicio"
                    setOnClickListener {
                        showRatingDialog(order.id)
                    }
                }
                card.addView(rateButton)
            }

            val separator = View(this).apply {
                setBackgroundColor(0xFFCCCCCC.toInt())
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            }

            orderContainer.addView(card)
            orderContainer.addView(separator)
        }
    }

    private fun showRatingDialog(orderId: Long) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val etRating = EditText(this).apply {
            hint = "Puntuación de 1 a 5 (Ejemplo: 5)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        val etFeedback = EditText(this).apply {
            hint = "Comentario u observación del servicio (opcional)"
        }

        layout.addView(etRating)
        layout.addView(etFeedback)

        AlertDialog.Builder(this)
            .setTitle("Calificar Servicio")
            .setMessage("Evalúa la atención entregada por el técnico:")
            .setView(layout)
            .setPositiveButton("Enviar Calificación") { _, _ ->
                val ratingStr = etRating.text.toString().trim()
                val rating = ratingStr.toIntOrNull()
                val feedback = etFeedback.text.toString().trim()

                if (rating == null || rating !in 1..5) {
                    Toast.makeText(this, "Por favor ingresa una nota válida entre 1 y 5.", Toast.LENGTH_SHORT).show()
                } else {
                    val updated = repository.rateWorkOrder(orderId, clientId, rating, feedback)
                    if (updated) {
                        Toast.makeText(this, "¡Gracias por tu calificación!", Toast.LENGTH_SHORT).show()
                        loadClientOrders()
                    } else {
                        Toast.makeText(this, "No se pudo registrar la calificación.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroy() {
        if (::repository.isInitialized) {
            repository.close()
        }
        super.onDestroy()
    }
}