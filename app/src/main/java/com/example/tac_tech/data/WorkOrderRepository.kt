package com.example.tac_tech.data

import android.content.ContentValues
import android.content.Context
import com.example.tac_tech.model.WorkOrder

class WorkOrderRepository(context: Context) {

    private val helper = DatabaseHelper(context)

    fun create(order: WorkOrder): Long {
        require(order.title.isNotBlank()) {
            "El título es obligatorio"
        }

        require(order.description.isNotBlank()) {
            "La descripción es obligatoria"
        }

        require(order.priority in listOf("BAJA", "MEDIA", "ALTA")) {
            "Prioridad inválida"
        }

        val db = helper.writableDatabase

        db.beginTransaction()

        try {
            // Comprobar que el cliente pertenece a la empresa.
            val validClient = db.query(
                "clients",
                arrayOf("id"),
                "id = ? AND company_id = ? AND active = 1",
                arrayOf(
                    order.clientId.toString(),
                    order.companyId.toString()
                ),
                null,
                null,
                null
            ).use { it.moveToFirst() }

            require(validClient) {
                "El cliente no pertenece a la empresa seleccionada"
            }

            // Validar al técnico, si se ha seleccionado uno.
            if (order.employeeId != null) {
                require(
                    isValidTechnician(
                        db,
                        order.employeeId,
                        order.companyId
                    )
                ) {
                    "El técnico no pertenece a esta empresa o está inactivo"
                }
            }

            val status = if (order.employeeId == null) {
                "PENDIENTE"
            } else {
                "ASIGNADA"
            }

            val values = ContentValues().apply {
                put("company_id", order.companyId)
                put("client_id", order.clientId)

                if (order.employeeId == null) {
                    putNull("employee_id")
                } else {
                    put("employee_id", order.employeeId)
                }

                put("title", order.title.trim())
                put("description", order.description.trim())
                put("priority", order.priority)
                put("status", status)
            }

            val id = db.insertOrThrow(
                "work_orders",
                null,
                values
            )

            db.setTransactionSuccessful()
            return id
        } finally {
            db.endTransaction()
        }
    }

    fun getAllOrders(): List<WorkOrder> {
        return queryOrders(null, null)
    }

    fun assignTechnician(
        orderId: Long,
        companyId: Long,
        employeeId: Long
    ): Boolean {
        val db = helper.writableDatabase

        db.beginTransaction()

        try {
            require(
                isValidTechnician(db, employeeId, companyId)
            ) {
                "El técnico no pertenece a esta empresa o está inactivo"
            }

            val values = ContentValues().apply {
                put("employee_id", employeeId)
                put("status", "ASIGNADA")
            }

            val updated = db.update(
                "work_orders",
                values,
                """
                    id = ? AND company_id = ?
                    AND status = 'PENDIENTE'
                """.trimIndent(),
                arrayOf(orderId.toString(), companyId.toString())
            ) > 0

            db.setTransactionSuccessful()
            return updated
        } finally {
            db.endTransaction()
        }
    }

    fun getByCompany(companyId: Long): List<WorkOrder> {
        return queryOrders(
            "company_id = ?",
            arrayOf(companyId.toString())
        )
    }

    fun getByTechnician(employeeId: Long): List<WorkOrder> {
        return queryOrders(
            "employee_id = ?",
            arrayOf(employeeId.toString())
        )
    }

    private fun queryOrders(
        selection: String?,
        args: Array<String>?
    ): List<WorkOrder> {
        val orders = mutableListOf<WorkOrder>()

        helper.readableDatabase.query(
            "work_orders",
            arrayOf(
                "id", "company_id", "client_id",
                "employee_id", "title", "description",
                "priority", "status", "created_at"
            ),
            selection,
            args,
            null,
            null,
            "id DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val employeeIndex =
                    cursor.getColumnIndexOrThrow("employee_id")

                orders.add(
                    WorkOrder(
                        id = cursor.getLong(0),
                        companyId = cursor.getLong(1),
                        clientId = cursor.getLong(2),
                        employeeId = if (
                            cursor.isNull(employeeIndex)
                        ) null else cursor.getLong(employeeIndex),
                        title = cursor.getString(4),
                        description = cursor.getString(5),
                        priority = cursor.getString(6),
                        status = cursor.getString(7),
                        createdAt = cursor.getString(8)
                    )
                )
            }
        }

        return orders
    }

    private fun isValidTechnician(
        db: android.database.sqlite.SQLiteDatabase,
        employeeId: Long,
        companyId: Long
    ): Boolean {
        return db.query(
            "employees",
            arrayOf("id"),
            "id = ? AND company_id = ? AND active = 1",
            arrayOf(employeeId.toString(), companyId.toString()),
            null,
            null,
            null
        ).use { it.moveToFirst() }
    }

    fun updateOrderDetails(
        orderId: Long,
        title: String,
        description: String
    ): Boolean {
        val db = helper.writableDatabase
        val values = ContentValues().apply {
            put("title", title.trim())
            put("description", description.trim())
        }

        return db.update(
            "work_orders",
            values,
            "id = ?",
            arrayOf(orderId.toString())
        ) > 0
    }

    // Mapeo adaptable para encontrar la ID real del cliente en la tabla clients
    fun getClientIdByUserId(userId: Long): Long {
        val db = helper.readableDatabase

        // 1. Obtener el nombre del usuario logueado desde la tabla users
        var userName: String? = null
        try {
            db.query(
                "users",
                arrayOf("name"),
                "id = ?",
                arrayOf(userId.toString()),
                null, null, null
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    userName = cursor.getString(0)
                }
            }
        } catch (_: Exception) { }

        // 2. Si tenemos el nombre del usuario, buscar la id correspondiente en clients por nombre
        if (!userName.isNullOrBlank()) {
            try {
                db.query(
                    "clients",
                    arrayOf("id"),
                    "name = ?",
                    arrayOf(userName),
                    null, null, null
                ).use { cursor ->
                    if (cursor.moveToFirst()) {
                        return cursor.getLong(0)
                    }
                }
            } catch (_: Exception) { }
        }

        // 3. Fallback: verificar si el userId existe directamente como id en clients
        try {
            db.query(
                "clients",
                arrayOf("id"),
                "id = ?",
                arrayOf(userId.toString()),
                null, null, null
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getLong(0)
                }
            }
        } catch (_: Exception) { }

        // 4. Si todo lo anterior falla, obtener el id del primer cliente en la tabla
        try {
            db.query(
                "clients",
                arrayOf("id"),
                null, null, null, null, "id ASC", "1"
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getLong(0)
                }
            }
        } catch (_: Exception) { }

        return userId
    }

    // Mapeo adaptable para encontrar la ID del técnico en la tabla employees
    fun getEmployeeIdByUserId(userId: Long): Long {
        val db = helper.readableDatabase

        var userName: String? = null
        try {
            db.query(
                "users",
                arrayOf("name"),
                "id = ?",
                arrayOf(userId.toString()),
                null, null, null
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    userName = cursor.getString(0)
                }
            }
        } catch (_: Exception) { }

        if (!userName.isNullOrBlank()) {
            try {
                db.query(
                    "employees",
                    arrayOf("id"),
                    "name = ?",
                    arrayOf(userName),
                    null, null, null
                ).use { cursor ->
                    if (cursor.moveToFirst()) {
                        return cursor.getLong(0)
                    }
                }
            } catch (_: Exception) { }
        }

        try {
            db.query(
                "employees",
                arrayOf("id"),
                "id = ?",
                arrayOf(userId.toString()),
                null, null, null
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getLong(0)
                }
            }
        } catch (_: Exception) { }

        return userId
    }

    fun close() {
        helper.close()
    }

    fun updateStatusForTechnician(
        orderId: Long,
        technicianId: Long,
        newStatus: String,
        notes: String? = null
    ): Boolean {
        val db = helper.writableDatabase
        val values = ContentValues().apply {
            put("status", newStatus)
            if (!notes.isNullOrBlank()) {
                val currentOrder = getByTechnician(technicianId).firstOrNull { it.id == orderId }
                val updatedDescription = if (currentOrder != null) {
                    "${currentOrder.description}\n[Cierre Técnico]: ${notes.trim()}"
                } else {
                    "[Cierre Técnico]: ${notes.trim()}"
                }
                put("description", updatedDescription)
            }
        }

        val updated = db.update(
            "work_orders",
            values,
            "id = ? AND employee_id = ?",
            arrayOf(orderId.toString(), technicianId.toString())
        ) > 0

        return updated
    }

    fun rateWorkOrder(orderId: Long, clientId: Long, rating: Int, feedback: String?): Boolean {
        val db = helper.writableDatabase
        val currentOrder = getByClient(clientId).firstOrNull { it.id == orderId } ?: return false

        val ratingText = if (!feedback.isNullOrBlank()) {
            "\n[Calificación Cliente]: $rating/5 - ${feedback.trim()}"
        } else {
            "\n[Calificación Cliente]: $rating/5"
        }

        val values = ContentValues().apply {
            put("description", "${currentOrder.description}$ratingText")
        }

        return db.update(
            "work_orders",
            values,
            "id = ? AND client_id = ? AND status = 'COMPLETADA'",
            arrayOf(orderId.toString(), clientId.toString())
        ) > 0
    }

    fun getByClient(clientId: Long): List<WorkOrder> {
        return queryOrders(
            "client_id = ?",
            arrayOf(clientId.toString())
        )
    }
}