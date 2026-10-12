package com.example.tac_tech.model

data class Client(
    val id: Long,
    val name: String,
    val email: String,
    val phone: String,
    val companyId: Long,
    val active: Boolean
)
