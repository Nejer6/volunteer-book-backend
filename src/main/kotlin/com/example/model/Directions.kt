package com.example.model

import org.jetbrains.exposed.sql.Table


object Directions : Table() {
    val id = integer("id").autoIncrement()
    val title = varchar("title", 127).uniqueIndex()
    override val primaryKey = PrimaryKey(id)
}
