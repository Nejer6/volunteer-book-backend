package com.example.model

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date

object Events : Table() {
    val id = integer("id").autoIncrement()
    val title = varchar("title", 255)
    val date = date("date")
    val directionId = reference("direction_id", Directions.id)
    val address = varchar("address", 512)
    val organizer = varchar("organizer", 255)
    val description = text("description")
    override val primaryKey = PrimaryKey(id)
}
