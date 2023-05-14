package com.example.model

import org.jetbrains.exposed.sql.Table

object Organizations : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 128)
    override val primaryKey = PrimaryKey(id)
}