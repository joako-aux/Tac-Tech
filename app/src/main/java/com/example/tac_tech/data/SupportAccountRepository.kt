package com.example.tac_tech.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteConstraintException

class SupportAccountRepository(context: Context) {

    private val helper = DatabaseHelper(context)

    fun createAccount(employeeId: Long, username: String, password: String): Long {
        require(username.trim().length >= 4) { "El usuario debe tener al menos 4 caracteres" }
        require(password.length >= 8) { "La contraseña debe tener al menos 8 caracteres" }

        val db = helper.writableDatabase
        db.beginTransaction()

        try {
            val employee = db.query(
                "employees",
                arrayOf("id", "name", "company_id"),
                "id = ? AND active = 1",
                arrayOf(employeeId.toString()),
                null, null, null
            ).use { cursor ->
                if (!cursor.moveToFirst()) {
                    throw IllegalArgumentException("El técnico no existe o está desactivado")
                }
                Triple(cursor.getLong(0), cursor.getString(1), cursor.getLong(2))
            }

            db.query(
                "users",
                arrayOf("id"),
                "employee_id = ?",
                arrayOf(employeeId.toString()),
                null, null, null
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    throw IllegalStateException("Este técnico ya tiene una cuenta")
                }
            }

            val values = ContentValues().apply {
                put("name", employee.second)
                put("username", username.trim())
                put("password_hash", DatabaseHelper.sha256(password))
                put("role", "SOPORTE")
                put("company_id", employee.third)
                putNull("client_id")
                put("employee_id", employee.first)
                put("active", 1)
            }

            val id = db.insertOrThrow("users", null, values)
            db.setTransactionSuccessful()
            return id

        } catch (e: SQLiteConstraintException) {
            throw IllegalStateException("El usuario ya existe o la cuenta incumple restricciones")
        } finally {
            db.endTransaction()
        }
    }

    fun close() {
        helper.close()
    }
}