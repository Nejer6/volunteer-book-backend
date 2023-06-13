package com.example.database.dao

import com.example.dto.*
import com.example.model.*
import com.example.utils.localDateToString
import com.example.utils.toLocalDate
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDate

object DAO {
    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun getUserProfileById(userId: Int): UserProfileDTO = dbQuery {
        val email = Users
            .select { Users.id.eq(userId) }
            .map {
                it[Users.email]
            }
            .first()
        getUserProfileByEmail(email)
    }

    suspend fun checkUser(email: String, password: String): Boolean = dbQuery {
        val user = Users
            .select { Users.email.eq(email) and Users.password.eq(password) }
            .singleOrNull()
        return@dbQuery user != null
    }

    suspend fun deletePoints(eventId: Int, userId: Int) = dbQuery {
        Points.deleteWhere { Points.userId.eq(userId) and Points.eventId.eq(eventId) }
    }

    suspend fun updatePoints(eventId: Int, userId: Int, points: Int?) = dbQuery {
        Points.update( { Points.eventId.eq(eventId) and Points.userId.eq(userId) } ) {
            it[this.points] = points
        }
    }

    suspend fun updateRequest(userId: Int, eventId: Int, newStatus: String) = dbQuery {
        Requests.update({ Requests.eventId.eq(eventId) and Requests.userId.eq(userId) }) {
            it[statusId] = RequestStatuses
                .select { RequestStatuses.status.eq(newStatus) }
                .map { it[RequestStatuses.id] }
                .first()
        }

        if (newStatus == "Accepted") {
            Points.insert {
                it[this.eventId] = eventId
                it[this.userId] = userId
                it[points] = null
            }
        }
    }

    suspend fun getEventEdite(eventId: Int): EventEditDTO? = dbQuery {
        Events
            .innerJoin(Directions)
            .select { Events.id.eq(eventId) }
            .map { event ->
                EventEditDTO(
                    id = event[Events.id],
                    title = event[Events.title],
                    date = localDateToString(event[Events.date]),
                    direction = event[Directions.title],
                    address = event[Events.address],
                    organizer = event[Events.organizer],
                    description = event[Events.description],
                    requests = Users
                        .innerJoin(Requests)
                        .innerJoin(RequestStatuses)
                        .select { Requests.eventId.eq(eventId) and RequestStatuses.status.eq("Under review") }
                        .map {
                            UserDTO(
                                id = it[Users.id],
                                name = it[Users.name],
                                surname = it[Users.surname]
                            )
                        },
                    maxParticipant = event[Events.maxParticipant],
                    participants = Users
                        .innerJoin(Requests)
                        .innerJoin(RequestStatuses)
                        .innerJoin(Points)
                        .select {
                            Requests.eventId.eq(eventId) and RequestStatuses.status.eq("Accepted") and Points.eventId.eq(
                                Requests.eventId
                            )
                        }
                        .map {
                            ParticipantDTO(
                                id = it[Users.id],
                                name = it[Users.name],
                                surname = it[Users.surname],
                                points = it[Points.points]
                            )
                        }
                )
            }.singleOrNull()
    }

    suspend fun isAdmin(email: String): Boolean = dbQuery {
        val admin = Users
            .innerJoin(Roles)
            .select { Users.email.eq(email) and Roles.role.eq("admin") }
            .singleOrNull()

        admin != null
    }

    suspend fun addEvent(eventCreateDTO: EventCreateDTO, email: String) = dbQuery {
        val eventId = Events.insert {
            it[title] = eventCreateDTO.title
            it[date] = eventCreateDTO.date.toLocalDate()
            it[directionId] = Directions
                .select { Directions.title.eq(eventCreateDTO.direction) }
                .map { it[Directions.id] }
                .first()
            it[address] = eventCreateDTO.address
            it[organizer] = eventCreateDTO.organizer
            it[description] = eventCreateDTO.description
            it[maxParticipant] = eventCreateDTO.maxParticipant
        } get Events.id

        Requests.insert {
            it[userId] = Users.select { Users.email.eq(email) }.map { it[Users.id] }.first()
            it[this.eventId] = eventId
            it[statusId] = RequestStatuses
                .select { RequestStatuses.status.eq("Created") }
                .map { it[RequestStatuses.id] }
                .first()
        }
    }

    suspend fun getEventsByAdminEmail(email: String) = dbQuery {
        return@dbQuery Events
            .innerJoin(Requests)
            .innerJoin(RequestStatuses)
            .innerJoin(Users)
            .innerJoin(Directions)
            .select { Users.email.eq(email) and RequestStatuses.status.eq("Created") }
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
            .crossJoin(Users)
            .join(
                Requests,
                JoinType.LEFT,
                Events.id,
                Requests.eventId,
                additionalConstraint = { Users.id.eq(Requests.userId) }
            )
            .leftJoin(RequestStatuses)
            .innerJoin(Directions)
            .select {
                Events.date.greaterEq(LocalDate.now()).and(
                    Requests.id.isNull()
                        .or(RequestStatuses.status.neq("Accepted").and(RequestStatuses.status.neq("Created")))
                ).and(Users.email.eq(email))
            }
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
            .crossJoin(Users)
            .join(
                Requests,
                JoinType.LEFT,
                Events.id,
                Requests.eventId,
                additionalConstraint = { Requests.userId eq Users.id }
            )
            .leftJoin(RequestStatuses)
            .select {
                Events.id.eq(eventId).and(
                    Users.email.eq(email)
                )
            }
            .map {
                EventDetailDTO(
                    id = it[Events.id],
                    title = it[Events.title],
                    date = localDateToString(it[Events.date]),
                    direction = it[Directions.title],
                    address = it[Events.address],
                    organizer = it[Events.organizer],
                    description = it[Events.description],
                    state = it.getOrNull(RequestStatuses.status) ?: if ((it[Events.maxParticipant]
                            ?: Int.MAX_VALUE) > getNumberOfParticipants(eventId)
                    )  "Not submitted" else "Occupied",
                    maxParticipant = it[Events.maxParticipant]
                )
            }.singleOrNull()
    }

    private suspend fun getNumberOfParticipants(eventId: Int) : Int = dbQuery {
        return@dbQuery Events
            .innerJoin(Requests)
            .innerJoin(RequestStatuses)
            .select { (RequestStatuses.status eq "Accepted") and (Events.id eq eventId)}
            .count()
            .toInt()
    }

    suspend fun getUserProfileByEmail(email: String): UserProfileDTO = dbQuery {
        val user = Users
            .innerJoin(Organizations)
            .innerJoin(Roles)
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
                        .innerJoin(Events)
                        .slice(Points.points.sum())
                        .select {( Points.userId eq _user[Users.id]) and (Events.date less LocalDate.now()) }
                        .map {
                            it[Points.points.sum()]
                        }.singleOrNull() ?: 0,
                    role = _user[Roles.role]
                )
            }.singleOrNull()

        return@dbQuery user!!
    }
}