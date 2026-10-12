package com.example.tac_tech.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import com.example.tac_tech.model.Company

class CompanyRepository(context: Context) {

    private val helper = DatabaseHelper(context)

    fun getAll(): List<Company> {
        val companies = mutableListOf<Company>()

        helper.readableDatabase.query(
            "companies",
            arrayOf("id", "name", "tax_id", "address"),
            null,
            null,
            null,
            null,
            "name COLLATE NOCASE ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                companies.add(
                    Company(
                        id = cursor.getLong(
                            cursor.getColumnIndexOrThrow("id")
                        ),
                        name = cursor.getString(
                            cursor.getColumnIndexOrThrow("name")
                        ),
                        taxId = cursor.getString(
                            cursor.getColumnIndexOrThrow("tax_id")
                        ) ?: "",
                        address = cursor.getString(
                            cursor.getColumnIndexOrThrow("address")
                        ) ?: ""
                    )
                )
            }
        }

        return companies
    }

    fun insert(
        name: String,
        taxId: String,
        address: String
    ): Long {
        require(name.isNotBlank()) {
            "El nombre de la empresa es obligatorio"
        }

        val values = ContentValues().apply {
            put("name", name.trim())
            put("tax_id", taxId.trim())
            put("address", address.trim())
        }

        return helper.writableDatabase.insertOrThrow(
            "companies",
            null,
            values
        )
    }

    fun update(company: Company): Boolean {
        require(company.name.isNotBlank()) {
            "El nombre de la empresa es obligatorio"
        }

        val values = ContentValues().apply {
            put("name", company.name.trim())
            put("tax_id", company.taxId.trim())
            put("address", company.address.trim())
        }

        return helper.writableDatabase.update(
            "companies",
            values,
            "id = ?",
            arrayOf(company.id.toString())
        ) > 0
    }

    fun delete(id: Long): Boolean {
        return try {
            helper.writableDatabase.delete(
                "companies",
                "id = ?",
                arrayOf(id.toString())
            ) > 0
        } catch (e: SQLiteConstraintException) {
            throw IllegalStateException(
                "No se puede eliminar esta empresa porque tiene " +
                        "registros asociados."
            )
        }
    }

    fun close() {
        helper.close()
    }
}
