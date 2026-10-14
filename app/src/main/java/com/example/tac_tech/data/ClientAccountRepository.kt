package com.example.tac_tech.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteConstraintException

class ClientAccountRepository(context: Context) {

    private val helper = DatabaseHelper(context)

    fun createAccount(
        clientId: Long,
        username: String,
        password: String
    ): Long {
        require(username.isNotBlank()) {
            "El nombre de usuario es obligatorio"
        }

        require(username.length >= 4) {
            "El usuario debe tener al menos 4 caracteres"
        }

        require(password.length >= 8) {
            "La contraseña debe tener al menos 8 caracteres"
        }

        val db = helper.writableDatabase

        db.beginTransaction()

        try {
            // Verificar que el cliente existe y está activo.
            val client = db.query(
                "clients",
                arrayOf("id", "name", "company_id"),
                "id = ? AND active = 1",
                arrayOf(clientId.toString()),
                null,
                null,
                null
            ).use { cursor ->
                if (!cursor.moveToFirst()) {
                    throw IllegalArgumentException(
                        "El cliente no existe o está desactivado"
                    )
                }

                Triple(
                    cursor.getLong(0),
                    cursor.getString(1),
                    cursor.getLong(2)
                )
            }

            // Verificar que el cliente no tenga otra cuenta.
            db.query(
                "users",
                arrayOf("id"),
                "client_id = ?",
                arrayOf(clientId.toString()),
                null,
                null,
                null
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    throw IllegalStateException(
                        "Este cliente ya tiene una cuenta"
                    )
                }
            }

            val values = ContentValues().apply {
                put("name", client.second)
                put("username", username.trim())
                put(
                    "password_hash",
                    DatabaseHelper.sha256(password)
                )
                put("role", "CLIENTE")
                put("company_id", client.third)
                put("client_id", client.first)
                put("active", 1)
            }

            val id = db.insertOrThrow("users", null, values)

            db.setTransactionSuccessful()
            return id

        } catch (e: SQLiteConstraintException) {
            throw IllegalStateException(
                "El usuario ya existe o la cuenta no cumple las " +
                        "restricciones de la base de datos."
            )
        } finally {
            db.endTransaction()
        }
    }

    fun close() {
        helper.close()
    }
}
