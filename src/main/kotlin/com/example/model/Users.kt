package com.example.model

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date

object Users : Table() {
    val id = integer("id").autoIncrement()
    val avatarUrl = varchar("avatar_url", 256)
    val name = varchar("name", 32)
    val surname = varchar("surname", 32)
    val city = varchar("city", 32)
    val birthday = date("birthday")
    val phone = varchar("phone", 15).uniqueIndex()
    val email = varchar("email", 128).uniqueIndex()
    val password = varchar("password", 128)
    val organizationId = reference("organization_id", Organizations.id)
    val roleId = reference("role_id", Roles.id)
    override val primaryKey = PrimaryKey(id)
}
