package com.example.model

import org.jetbrains.exposed.sql.Table

object RequestStatuses : Table() {
    val id = integer("id").autoIncrement()
    val status = varchar("status", 32)
    override val primaryKey = PrimaryKey(id)
}