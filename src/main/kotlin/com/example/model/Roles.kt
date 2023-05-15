package com.example.model

import org.jetbrains.exposed.sql.Table

object Roles : Table() {
    val id = integer("id").autoIncrement()
    val role = varchar("role", 16)
    override val primaryKey = PrimaryKey(id)
}
