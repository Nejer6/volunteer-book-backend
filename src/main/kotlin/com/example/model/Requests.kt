package com.example.model

import org.jetbrains.exposed.sql.Table

object Requests : Table() {
    val id = integer("id").autoIncrement()
    val userId = reference("user_id", Users.id)
    val eventId = reference("event_id", Events.id)
    val statusId = reference("status_id", RequestStatuses.id)
    override val primaryKey = PrimaryKey(id)
}