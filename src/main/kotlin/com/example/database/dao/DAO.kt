package com.example.database.dao

import com.example.dto.EventDTO
import com.example.dto.EventDetailDTO
import com.example.dto.UserProfileDTO
import com.example.model.*
import com.example.utils.localDateToString
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDate

object DAO {
    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun checkUser(email: String, password: String): Boolean = dbQuery {
        val user = Users
            .select { Users.email.eq(email) and Users.password.eq(password) }
            .singleOrNull()
        return@dbQuery user != null
    }

    suspend fun insertRequest(email: String, eventId: Int) = dbQuery {
        Requests.insert {
            it[userId] = Users
                .select { Users.email eq email }
                .map { it[Users.id] }
                .first()
            it[this.eventId] = eventId
            it[statusId] = RequestStatuses
                .select { RequestStatuses.status eq "Under review" }
                .map { it[RequestStatuses.id] }
                .first() //todo
        }
    }

    suspend fun getEventsByEmail(email: String): List<EventDTO> = dbQuery {
        return@dbQuery Events
            .leftJoin(Requests)
            .leftJoin(RequestStatuses)
            .innerJoin(Directions)
            .leftJoin(Users)
            .select { (Users.email.eq(email) or Users.email.isNull()) and
                    Events.date.greaterEq(LocalDate.now()) and
                    (RequestStatuses.status.neq("Accepted") or RequestStatuses.status.isNull()) }
            .map {
                EventDTO(
                    id = it[Events.id],
                    title = it[Events.title],
                    date = localDateToString(it[Events.date]),
                    direction = it[Directions.title],
                    points = null
                )
            }
    }

    suspend fun getUserIdByEmailAndPassword(email: String, password: String): Int? = dbQuery {
        val id = Users
            .select { Users.email.eq(email) and Users.password.eq(password) }
            .map {
                it[Users.id]
            }.singleOrNull()
        return@dbQuery id
    }

    suspend fun getEventDetailByIdAndUserEmail(eventId: Int, email: String): EventDetailDTO? = dbQuery {
        return@dbQuery Events
            .innerJoin(Directions)
            .leftJoin(Requests)
            .leftJoin(RequestStatuses)
            .leftJoin(Users)
            .select { (Users.email.eq(email) or Users.email.isNull()) and Events.id.eq(eventId) }
            .map {
                EventDetailDTO(
                    id = it[Events.id],
                    title = it[Events.title],
                    date = localDateToString(it[Events.date]),
                    direction = it[Directions.title],
                    address = it[Events.address],
                    organizer = it[Events.organizer],
                    description = it[Events.description],
                    state = it.getOrNull(RequestStatuses.status) ?: "Not submitted"
                )
            }.singleOrNull()
    }

    suspend fun getUserProfileByEmail(email: String): UserProfileDTO = dbQuery {
        val user = Users
            .innerJoin(Organizations)
            .select { Users.email eq email }
            .map { _user ->
                UserProfileDTO(
                    id = _user[Users.id],
                    avatarUrl = _user[Users.avatarUrl],
                    birthday = localDateToString(_user[Users.birthday]),
                    city = _user[Users.city],
                    email = _user[Users.email],
                    name = _user[Users.name],
                    phone = _user[Users.phone],
                    surname = _user[Users.surname],
                    organization = _user[Organizations.name],
                    currentEvents = Events
                        .innerJoin(Requests)
                        .innerJoin(RequestStatuses)
                        .innerJoin(Directions)
                        .select { (Requests.userId eq _user[Users.id]) and (RequestStatuses.status eq "Accepted") and (Events.date greaterEq LocalDate.now()) }
                        .map {
                            EventDTO(
                                id = it[Events.id],
                                title = it[Events.title],
                                date = localDateToString(it[Events.date]),
                                points = null,
                                direction = it[Directions.title]
                            )
                        },
                    previousEvents = Events
                        .innerJoin(Requests)
                        .innerJoin(RequestStatuses)
                        .innerJoin(Directions)
                        .innerJoin(Points)
                        .select { (Requests.userId eq _user[Users.id]) and (RequestStatuses.status eq "Accepted") and (Events.date less LocalDate.now()) }
                        .map {
                            EventDTO(
                                id = it[Events.id],
                                title = it[Events.title],
                                date = localDateToString(it[Events.date]),
                                points = it[Points.points],
                                direction = it[Directions.title]
                            )
                        },
                    points = Points
                        .slice(Points.points.sum())
                        .select { Points.userId eq _user[Users.id] }
                        .map {
                            it[Points.points.sum()]
                        }.singleOrNull() ?: 0
                )
            }.singleOrNull()

        return@dbQuery user!!
    }
}