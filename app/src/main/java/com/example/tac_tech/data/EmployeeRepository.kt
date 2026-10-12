package com.example.tac_tech.data

import android.content.ContentValues
import android.content.Context
import com.example.tac_tech.model.Employee

class EmployeeRepository(context: Context) {

    private val helper = DatabaseHelper(context)

    fun getByCompany(companyId: Long): List<Employee> {
        val employees = mutableListOf<Employee>()

        helper.readableDatabase.query(
            "employees",
            arrayOf("id", "name", "email", "phone", "company_id", "active"),
            "company_id = ? AND active = 1",
            arrayOf(companyId.toString()),
            null, null,
            "name COLLATE NOCASE ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                employees.add(
                    Employee(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        email = cursor.getString(cursor.getColumnIndexOrThrow("email")) ?: "",
                        phone = cursor.getString(cursor.getColumnIndexOrThrow("phone")) ?: "",
                        companyId = cursor.getLong(cursor.getColumnIndexOrThrow("company_id")),
                        active = cursor.getInt(cursor.getColumnIndexOrThrow("active")) == 1
                    )
                )
            }
        }
        return employees
    }

    fun insert(name: String, email: String, phone: String, companyId: Long): Long {
        require(name.isNotBlank()) { "El nombre del técnico es obligatorio" }

        val values = ContentValues().apply {
            put("name", name.trim())
            put("email", email.trim())
            put("phone", phone.trim())
            put("company_id", companyId)
            put("active", 1)
        }

        return helper.writableDatabase.insertOrThrow("employees", null, values)
    }

    fun update(employee: Employee): Boolean {
        require(employee.name.isNotBlank()) { "El nombre es obligatorio" }

        val values = ContentValues().apply {
            put("name", employee.name.trim())
            put("email", employee.email.trim())
            put("phone", employee.phone.trim())
        }

        return helper.writableDatabase.update(
            "employees",
            values,
            "id = ? AND company_id = ? AND active = 1",
            arrayOf(employee.id.toString(), employee.companyId.toString())
        ) > 0
    }

    fun deactivate(employeeId: Long, companyId: Long): Boolean {
        val values = ContentValues().apply { put("active", 0) }

        return helper.writableDatabase.update(
            "employees",
            values,
            "id = ? AND company_id = ? AND active = 1",
            arrayOf(employeeId.toString(), companyId.toString())
        ) > 0
    }

    fun close() {
        helper.close()
    }
}