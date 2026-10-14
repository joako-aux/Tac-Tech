package com.example.tac_tech.data

import android.content.Context
import com.example.tac_tech.model.User

class UserRepository(context: Context) {

    private val dbHelper = DatabaseHelper(context)

    fun login(username: String, password: String): User? {
        if (username.isBlank() || password.isBlank()) {
            return null
        }

        val db = dbHelper.readableDatabase

        db.query(
            "users",
            arrayOf(
                "id", "name", "username",
                "password_hash", "role", "company_id"
            ),
            "username = ? AND active = 1",
            arrayOf(username.trim()),
            null,
            null,
            null
        ).use { cursor ->

            if (!cursor.moveToFirst()) return null

            val storedHash =
                cursor.getString(cursor.getColumnIndexOrThrow("password_hash"))

            val enteredHash = DatabaseHelper.sha256(password)

            if (storedHash != enteredHash) return null

            val companyIndex =
                cursor.getColumnIndexOrThrow("company_id")

            val companyId =
                if (cursor.isNull(companyIndex)) null
                else cursor.getLong(companyIndex)

            return User(
                id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                username = cursor.getString(
                    cursor.getColumnIndexOrThrow("username")
                ),
                role = cursor.getString(cursor.getColumnIndexOrThrow("role")),
                companyId = companyId
            )
        }
    }

    fun getClientsByCompany(companyId: Long): List<User> {
        val clients = mutableListOf<User>()
        val db = dbHelper.readableDatabase

        db.query(
            "clients",
            arrayOf("id", "name", "email", "company_id"),
            "company_id = ? AND active = 1",
            arrayOf(companyId.toString()),
            null, null, "name ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val emailIndex = cursor.getColumnIndex("email")
                val email = if (emailIndex != -1 && !cursor.isNull(emailIndex)) cursor.getString(emailIndex) else ""

                clients.add(
                    User(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        username = email,
                        role = "CLIENTE",
                        companyId = cursor.getLong(cursor.getColumnIndexOrThrow("company_id"))
                    )
                )
            }
        }
        return clients
    }

    fun getTechniciansByCompany(companyId: Long): List<User> {
        val technicians = mutableListOf<User>()
        val db = dbHelper.readableDatabase

        db.query(
            "employees",
            arrayOf("id", "name", "phone", "company_id"),
            "company_id = ? AND active = 1",
            arrayOf(companyId.toString()),
            null, null, "name ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val phoneIndex = cursor.getColumnIndex("phone")
                val phone = if (phoneIndex != -1 && !cursor.isNull(phoneIndex)) cursor.getString(phoneIndex) else ""

                technicians.add(
                    User(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        username = phone,
                        role = "TECNICO",
                        companyId = cursor.getLong(cursor.getColumnIndexOrThrow("company_id"))
                    )
                )
            }
        }
        return technicians
    }

    fun close() {
        dbHelper.close()
    }
}