package com.example.tac_tech.data

import android.content.ContentValues
import android.content.Context
import android.util.Patterns
import com.example.tac_tech.model.Client

class ClientRepository(context: Context) {

    private val helper = DatabaseHelper(context)

    fun getByCompany(companyId: Long): List<Client> {
        val clients = mutableListOf<Client>()

        helper.readableDatabase.query(
            "clients",
            arrayOf(
                "id", "name", "email", "phone",
                "company_id", "active"
            ),
            "company_id = ? AND active = 1",
            arrayOf(companyId.toString()),
            null,
            null,
            "name COLLATE NOCASE ASC"
        ).use { cursor ->

            while (cursor.moveToNext()) {
                clients.add(
                    Client(
                        id = cursor.getLong(
                            cursor.getColumnIndexOrThrow("id")
                        ),
                        name = cursor.getString(
                            cursor.getColumnIndexOrThrow("name")
                        ),
                        email = cursor.getString(
                            cursor.getColumnIndexOrThrow("email")
                        ) ?: "",
                        phone = cursor.getString(
                            cursor.getColumnIndexOrThrow("phone")
                        ) ?: "",
                        companyId = cursor.getLong(
                            cursor.getColumnIndexOrThrow("company_id")
                        ),
                        active = cursor.getInt(
                            cursor.getColumnIndexOrThrow("active")
                        ) == 1
                    )
                )
            }
        }

        return clients
    }

    fun insert(
        name: String,
        email: String,
        phone: String,
        companyId: Long
    ): Long {
        require(name.isNotBlank()) {
            "El nombre del cliente es obligatorio"
        }

        val trimmedEmail = email.trim()
        if (trimmedEmail.isNotBlank()) {
            require(Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                "El formato del correo electrónico no es válido"
            }
        }

        val values = ContentValues().apply {
            put("name", name.trim())
            put("email", trimmedEmail)
            put("phone", phone.trim())
            put("company_id", companyId)
            put("active", 1)
        }

        return helper.writableDatabase.insertOrThrow(
            "clients",
            null,
            values
        )
    }

    fun update(client: Client): Boolean {
        require(client.name.isNotBlank()) {
            "El nombre del cliente es obligatorio"
        }

        val trimmedEmail = client.email.trim()
        if (trimmedEmail.isNotBlank()) {
            require(Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                "El formato del correo electrónico no es válido"
            }
        }

        val values = ContentValues().apply {
            put("name", client.name.trim())
            put("email", trimmedEmail)
            put("phone", client.phone.trim())
        }

        return helper.writableDatabase.update(
            "clients",
            values,
            "id = ? AND company_id = ? AND active = 1",
            arrayOf(
                client.id.toString(),
                client.companyId.toString()
            )
        ) > 0
    }

    fun deactivate(clientId: Long, companyId: Long): Boolean {
        val values = ContentValues().apply {
            put("active", 0)
        }

        return helper.writableDatabase.update(
            "clients",
            values,
            "id = ? AND company_id = ? AND active = 1",
            arrayOf(clientId.toString(), companyId.toString())
        ) > 0
    }

    fun close() {
        helper.close()
    }
}