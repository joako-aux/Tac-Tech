package com.example.tac_tech

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.example.tac_tech.data.UserRepository
import com.example.tac_tech.ui.admin.AdminActivity
import com.example.tac_tech.ui.support.SupportActivity
import com.example.tac_tech.ui.client.ClientActivity

class MainActivity : Activity() {

    private lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        userRepository = UserRepository(this)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvMessage = findViewById<TextView>(R.id.tvMessage)

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString()

            val user = userRepository.login(username, password)

            if (user == null) {
                tvMessage.text = "Usuario o contraseña incorrectos"
                return@setOnClickListener
            }

            val destination = when (user.role) {
                "ADMIN" -> AdminActivity::class.java
                "SOPORTE" -> SupportActivity::class.java
                "CLIENTE" -> ClientActivity::class.java
                else -> null
            }

            if (destination == null) {
                tvMessage.text = "El rol no es válido"
                return@setOnClickListener
            }

            val intent = Intent(this, destination).apply {
                putExtra("USER_ID", user.id)
                putExtra("USER_NAME", user.name)
                putExtra("COMPANY_ID", user.companyId ?: -1L)
                putExtra("USER_ROLE", user.role)
            }

            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        userRepository.close()
        super.onDestroy()
    }
}



