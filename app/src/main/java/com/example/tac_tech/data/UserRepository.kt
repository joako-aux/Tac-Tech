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

    fun close() {
        dbHelper.close()
    }
}



