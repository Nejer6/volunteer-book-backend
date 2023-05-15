package com.example.model

import org.jetbrains.exposed.sql.Table

object Points : Table() {
    val id = integer("id").autoIncrement()
    val userId = reference("user_id", Users.id)
    val eventId = reference("event_id", Events.id)
    val points = integer("points").nullable()
    override val primaryKey = PrimaryKey(id)
}