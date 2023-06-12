package com.example.database

import com.example.model.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate

object DatabaseFactory {
    fun init() {
        val driverClassName = "org.h2.Driver"
        val jdbcURL = "jdbc:h2:file:./build/db"
        val database = Database.connect(jdbcURL, driverClassName)
        transaction {
            SchemaUtils.drop(Directions, Users, Events, Organizations, Requests, RequestStatuses, Points, Roles)
            SchemaUtils.create(Directions, Users, Events, Organizations, Requests, RequestStatuses, Points, Roles)

            val userRoleId = Roles.insert {
                it[role] = "user"
            } get Roles.id

            val adminRoleId = Roles.insert {
                it[role] = "admin"
            } get Roles.id

            val directionTitles =
                listOf("Спортивное", "Патриотическое", "Духовно-нравственное", "Социальное", "Культурное")
            val directionsID = Directions.batchInsert(directionTitles) { title ->
                this[Directions.title] = title
            }

            val requestStatuses = listOf(
                "Not submitted",
                "Under review",
                "Accepted",
                "Declined",
                "Created",
                "Occupied"
            )
            val requestStatusesId = RequestStatuses.batchInsert(requestStatuses) {
                this[RequestStatuses.status] = it
            }

            val organizationId = Organizations.insert {
                it[name] = "Valonter ООО"
            } get Organizations.id

            val adminId = Users.insert {
                it[roleId] = adminRoleId
                it[avatarUrl] = "https://cs6.pikabu.ru/avatars/483/v483457.jpg?2118451620"
                it[name] = "Никита"
                it[surname] = "Дёмин"
                it[city] = "Пушкино"
                it[birthday] = LocalDate.of(2002, 5, 4)
                it[phone] = "8 962 947 89 60"
                it[email] = "nejer6@gmail.com"
                it[password] = "qwerty"
                it[this.organizationId] = organizationId
            } get Users.id

            val userId = Users.insert {
                it[roleId] = userRoleId
                it[avatarUrl] = "https://cs6.pikabu.ru/avatars/663/v663703-279838185.jpg"
                it[name] = "Татьяна"
                it[surname] = "Кудрявцева"
                it[city] = "Москва"
                it[birthday] = LocalDate.of(2000, 2, 21)
                it[phone] = "8 123 456 78 90"
                it[email] = "email.email.com"
                it[password] = "qwerty"
                it[this.organizationId] = organizationId
            } get Users.id

            val notSubmittedEventId = Events.insert {
                it[title] = "Культура для всех"
                it[date] = LocalDate.of(2023, 6, 25)
                it[directionId] = directionTitles.indexOf("Культурное") + 1
                it[address] = "г. Пушкино, ул. Колотушкина, д. 2"
                it[organizer] = "Лорд Волдеморт"
                it[description] = "Наше спортивное волонтерское мероприятие = это отличная возможность" +
                        " принять участие в организации и проведении спортивных мероприятий в нашем " +
                        "городе. \nМероприятие состоится 25.05.23 на стадионе Динамо"
                it[maxParticipant] = null
            } get Events.id

            Requests.insert {
                it[eventId] = notSubmittedEventId
                it[this.userId] = adminId
                it[statusId] = requestStatuses.indexOf("Created") + 1
            }

            val previousEventId = Events.insert {
                it[title] = "Спорт для всех"
                it[date] = LocalDate.of(2023, 4, 25)
                it[directionId] = directionTitles.indexOf("Спортивное") + 1
                it[address] = "г. Пушкино, ул. Колотушкина, д. 2"
                it[organizer] = "Лорд Волдеморт"
                it[description] = "Наше спортивное волонтерское мероприятие = это отличная возможность" +
                        " принять участие в организации и проведении спортивных мероприятий в нашем " +
                        "городе. \nМероприятие состоится 25.04.23 на стадионе Динамо"
            } get Events.id

            Requests.insert {
                it[eventId] = previousEventId
                it[this.userId] = adminId
                it[statusId] = requestStatuses.indexOf("Created") + 1
            }

            var requestId = Requests.insert {
                it[this.userId] = userId
                it[this.eventId] = previousEventId
                it[statusId] = requestStatuses.indexOf("Accepted") + 1
            }

            Points.insert {
                it[this.userId] = userId
                it[eventId] = previousEventId
                it[points] = 80
            }

            val event3 = Events.insert {
                it[title] = "Социум для всех"
                it[date] = LocalDate.of(2023, 6, 25)
                it[directionId] = directionTitles.indexOf("Социальное") + 1
                it[address] = "г. Пушкино, ул. Колотушкина, д. 2"
                it[organizer] = "Лорд Волдеморт"
                it[description] = "Наше спортивное волонтерское мероприятие = это отличная возможность" +
                        " принять участие в организации и проведении спортивных мероприятий в нашем " +
                        "городе. \nМероприятие состоится 25.06.23 на стадионе Динамо"
                it[maxParticipant] = 100
            } get Events.id

            Requests.insert {
                it[this.userId] = adminId
                it[eventId] = event3
                it[statusId] = requestStatuses.indexOf("Created") + 1
            }

            Requests.insert {
                it[this.userId] = userId
                it[this.eventId] = event3
                it[statusId] = requestStatuses.indexOf("Accepted") + 1
            }
        }
    }
}