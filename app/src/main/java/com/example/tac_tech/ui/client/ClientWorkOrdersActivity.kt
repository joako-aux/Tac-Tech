package com.example.tac_tech.ui.client

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.tac_tech.R
import com.example.tac_tech.data.WorkOrderRepository

class ClientWorkOrdersActivity : ComponentActivity() {
    private lateinit var repository: WorkOrderRepository
    private lateinit var orderContainer: LinearLayout
    private lateinit var orderCount: TextView
    private var clientId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_work_orders)

        orderContainer = findViewById(R.id.clientOrderContainer)
        orderCount = findViewById(R.id.textClientOrderCount)

        clientId = intent.getLongExtra("USER_ID", -1L)

        if (clientId <= 0L) {
            Toast.makeText(this, "No se identificó al cliente. Inicia sesión nuevamente.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        repository = WorkOrderRepository(this)
        loadClientOrders()
    }

    private fun loadClientOrders() {
        val orders = repository.getByClient(clientId)
        orderContainer.removeAllViews()
        orderCount.text = "Total de solicitudes: ${orders.size}"

        if (orders.isEmpty()) {
            val empty = TextView(this).apply {
                text = "No tienes solicitudes registradas actualmente."
                textSize = 16f
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
                textSize = 16f
            }
            card.addView(details)

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
        super.onDestroy()
        if (::repository.isInitialized) {
            repository.close()
        }
    }
}