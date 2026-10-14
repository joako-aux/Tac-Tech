package com.example.tac_tech.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.security.MessageDigest

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE companies (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                tax_id TEXT,
                address TEXT,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE clients (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                email TEXT,
                phone TEXT,
                company_id INTEGER NOT NULL,
                active INTEGER NOT NULL DEFAULT 1,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(company_id) REFERENCES companies(id)
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE employees (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                email TEXT,
                phone TEXT,
                company_id INTEGER NOT NULL,
                active INTEGER NOT NULL DEFAULT 1,
                user_id INTEGER,
                FOREIGN KEY(company_id) REFERENCES companies(id),
                FOREIGN KEY(user_id) REFERENCES users(id)
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE work_orders (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                company_id INTEGER NOT NULL,
                client_id INTEGER NOT NULL,
                employee_id INTEGER,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                priority TEXT NOT NULL DEFAULT 'MEDIA'
                    CHECK(priority IN ('BAJA','MEDIA','ALTA')),
                status TEXT NOT NULL DEFAULT 'PENDIENTE'
                    CHECK(status IN (
                        'PENDIENTE',
                        'ASIGNADA',
                        'EN_PROGRESO',
                        'COMPLETADA',
                        'CANCELADA'
                    )),
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(company_id) REFERENCES companies(id),
                FOREIGN KEY(client_id) REFERENCES clients(id),
                FOREIGN KEY(employee_id) REFERENCES employees(id)
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                username TEXT NOT NULL UNIQUE,
                password_hash TEXT NOT NULL,
                role TEXT NOT NULL
                    CHECK(role IN ('ADMIN','SOPORTE','CLIENTE')),
                company_id INTEGER,
                client_id INTEGER,
                employee_id INTEGER,
                active INTEGER NOT NULL DEFAULT 1,
                FOREIGN KEY(company_id) REFERENCES companies(id),
                FOREIGN KEY(client_id) REFERENCES clients(id),
                FOREIGN KEY(employee_id) REFERENCES employees(id)
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                description TEXT,
                company_id INTEGER NOT NULL,
                client_user_id INTEGER,
                technician_user_id INTEGER,
                status TEXT NOT NULL DEFAULT 'PENDIENTE',
                created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                updated_at TEXT DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(company_id) REFERENCES companies(id),
                FOREIGN KEY(client_user_id) REFERENCES users(id),
                FOREIGN KEY(technician_user_id) REFERENCES users(id)
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE visits (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                task_id INTEGER NOT NULL,
                technician_user_id INTEGER NOT NULL,
                latitude REAL,
                longitude REAL,
                photo_path TEXT,
                visited_at TEXT DEFAULT CURRENT_TIMESTAMP,
                notes TEXT,
                FOREIGN KEY(task_id) REFERENCES tasks(id),
                FOREIGN KEY(technician_user_id) REFERENCES users(id)
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE sync_queue (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                entity_type TEXT NOT NULL,
                entity_id INTEGER NOT NULL,
                operation TEXT NOT NULL,
                payload TEXT,
                status TEXT NOT NULL DEFAULT 'PENDIENTE',
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """.trimIndent())

        db.execSQL("""
            CREATE UNIQUE INDEX IF NOT EXISTS idx_users_client_id
            ON users(client_id)
            WHERE client_id IS NOT NULL
        """.trimIndent())

        db.execSQL("""
            CREATE UNIQUE INDEX IF NOT EXISTS idx_users_employee_id
            ON users(employee_id)
            WHERE employee_id IS NOT NULL
        """.trimIndent())

        val company = ContentValues().apply {
            put("name", "Empresa Demo")
            put("tax_id", "")
        }
        val companyId = db.insertOrThrow("companies", null, company)

        val admin = ContentValues().apply {
            put("name", "Administrador")
            put("username", "admin")
            put("password_hash", sha256("Admin123!"))
            put("role", "ADMIN")
            put("company_id", companyId)
            put("active", 1)
        }
        db.insertOrThrow("users", null, admin)
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        if (oldVersion < 2) {
            db.execSQL("""
                CREATE TABLE clients (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    email TEXT,
                    phone TEXT,
                    company_id INTEGER NOT NULL,
                    active INTEGER NOT NULL DEFAULT 1,
                    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(company_id) REFERENCES companies(id)
                )
            """.trimIndent())
        }

        if (oldVersion < 3) {
            db.execSQL(
                "ALTER TABLE users ADD COLUMN client_id INTEGER " +
                        "REFERENCES clients(id)"
            )
        }

        if (oldVersion < 4) {
            db.execSQL(
                "ALTER TABLE users ADD COLUMN employee_id INTEGER " +
                        "REFERENCES employees(id)"
            )

            try {
                db.execSQL("ALTER TABLE employees ADD COLUMN phone TEXT")
            } catch (_: Exception) {}

            try {
                db.execSQL("ALTER TABLE employees ADD COLUMN active INTEGER NOT NULL DEFAULT 1")
            } catch (_: Exception) {}
        }

        if (oldVersion < 5) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS work_orders (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    company_id INTEGER NOT NULL,
                    client_id INTEGER NOT NULL,
                    employee_id INTEGER,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    priority TEXT NOT NULL DEFAULT 'MEDIA'
                        CHECK(priority IN ('BAJA','MEDIA','ALTA')),
                    status TEXT NOT NULL DEFAULT 'PENDIENTE'
                        CHECK(status IN (
                            'PENDIENTE','ASIGNADA','EN_PROGRESO',
                            'COMPLETADA','CANCELADA'
                        )),
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(company_id) REFERENCES companies(id),
                    FOREIGN KEY(client_id) REFERENCES clients(id),
                    FOREIGN KEY(employee_id) REFERENCES employees(id)
                )
            """.trimIndent())
        }

        db.execSQL("""
            CREATE UNIQUE INDEX IF NOT EXISTS idx_users_client_id
            ON users(client_id)
            WHERE client_id IS NOT NULL
        """.trimIndent())

        db.execSQL("""
            CREATE UNIQUE INDEX IF NOT EXISTS idx_users_employee_id
            ON users(employee_id)
            WHERE employee_id IS NOT NULL
        """.trimIndent())
    }

    companion object {
        private const val DATABASE_NAME = "tactech.db"
        private const val DATABASE_VERSION = 5

        fun sha256(value: String): String {
            val bytes = MessageDigest.getInstance("SHA-256")
                .digest(value.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}