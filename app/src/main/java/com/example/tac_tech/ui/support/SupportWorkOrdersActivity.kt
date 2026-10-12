package com.example.tac_tech.ui.support

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

class SupportWorkOrdersActivity : ComponentActivity() {
    private lateinit var repository: WorkOrderRepository
    private lateinit var orderContainer: LinearLayout
    private lateinit var orderCount: TextView
    private var technicianId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_support_work_orders)

        orderContainer = findViewById(R.id.supportOrderContainer)
        orderCount = findViewById(R.id.textSupportOrderCount)

        technicianId = intent.getLongExtra("USER_ID", -1L)

        if (technicianId <= 0L) {
            Toast.makeText(this, "No se identificó al técnico. Inicia sesión nuevamente.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        repository = WorkOrderRepository(this)
        loadOrders()
    }

    private fun loadOrders() {
        val orders = repository.getByTechnician(technicianId)
        orderContainer.removeAllViews()
        orderCount.text = "Órdenes asignadas: ${orders.size}"

        if (orders.isEmpty()) {
            val empty = TextView(this).apply {
                text = "No tienes órdenes asignadas actualmente."
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
                    Orden #${order.id}: ${order.title}
                    Descripción: ${order.description}
                    Prioridad: ${order.priority}
                    Estado: ${order.status}
                """.trimIndent()
                textSize = 16f
            }
            card.addView(details)

            if (order.status == "ASIGNADA" || order.status == "EN_PROGRESO") {
                val actionButton = Button(this).apply {
                    text = if (order.status == "ASIGNADA") "Iniciar trabajo" else "Completar orden"
                    setOnClickListener {
                        if (order.status == "ASIGNADA") {
                            updateStatus(order.id, "EN_PROGRESO", null)
                        } else {
                            showCloseOrderDialog(order.id)
                        }
                    }
                }
                card.addView(actionButton)
            }

            val separator = View(this).apply {
                setBackgroundColor(0xFFCCCCCC.toInt())
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            }

            orderContainer.addView(card)
            orderContainer.addView(separator)
        }
    }

    private fun showCloseOrderDialog(orderId: Long) {
        val input = EditText(this).apply {
            hint = "Escribe las observaciones de cierre o reporte técnico"
            setPadding(32, 32, 32, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("Cierre de Orden Técnica")
            .setMessage("Registra las observaciones técnicas o trabajo realizado en terreno:")
            .setView(input)
            .setPositiveButton("Guardar y Completar") { _, _ ->
                val notes = input.text.toString().trim()
                if (notes.isEmpty()) {
                    Toast.makeText(this, "Debes ingresar una observación para completar la orden.", Toast.LENGTH_SHORT).show()
                } else {
                    updateStatus(orderId, "COMPLETADA", notes)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateStatus(orderId: Long, newStatus: String, notes: String?) {
        val updated = repository.updateStatusForTechnician(
            orderId = orderId,
            technicianId = technicianId,
            newStatus = newStatus,
            notes = notes
        )

        if (updated) {
            Toast.makeText(this, "Estado actualizado con éxito.", Toast.LENGTH_SHORT).show()
            loadOrders()
        } else {
            Toast.makeText(this, "No se pudo actualizar la orden.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::repository.isInitialized) {
            repository.close()
        }
    }
}