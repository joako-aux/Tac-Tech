package com.example.tac_tech.model

data class User(
    val id: Long,
    val name: String,
    val username: String,
    val role: String,
    val companyId: Long?
)
