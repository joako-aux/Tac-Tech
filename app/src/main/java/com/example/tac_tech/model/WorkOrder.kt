package com.example.tac_tech.model


data class WorkOrder(
    val id: Long = 0,
    val companyId: Long,
    val clientId: Long,
    val employeeId: Long? = null,
    val title: String,
    val description: String,
    val priority: String = "MEDIA",
    val status: String = "PENDIENTE",
    val createdAt: String = ""
)
