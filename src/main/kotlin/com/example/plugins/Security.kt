package com.example.plugins

import com.example.database.dao.DAO
import com.example.dto.EventCreateDTO
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

fun Application.configureSecurity() {

    authentication {
        basic(name = "myauth1") {
            realm = "Ktor Server"
            validate { credentials ->
                val userId = DAO.getUserIdByEmailAndPassword(credentials.name, credentials.password)
                if (userId != null) {
                    UserIdPrincipal(credentials.name)
                } else {
                    null
                }
            }
        }

//        form(name = "myauth2") {
//            userParamName = "user"
//            passwordParamName = "password"
//            challenge {
//                /**/
//            }
//        }
    }
    routing {
        authenticate("myauth1") {
            route("/api/protected") {
                get("/profile") {
                    val principal = call.principal<UserIdPrincipal>()!!
                    val email = principal.name
                    val userProfile = DAO.getUserProfileByEmail(email)
                    call.respond(userProfile)
                }

                route("/admin") {
                    route("/events") {
                        get {
                            val principal = call.principal<UserIdPrincipal>()!!
                            val email = principal.name
                            val events = DAO.getEventsByAdminEmail(email)
                            call.respond(events)
                        }

                        post("create") {
                            val principal = call.principal<UserIdPrincipal>()!!
                            val email = principal.name
                            val isAdmin = DAO.isAdmin(email)
                            if (isAdmin) {
                                val event = call.receive<EventCreateDTO>()
                                DAO.addEvent(event, email)
                                call.respond(HttpStatusCode.Created, "Event created successfully")
                            } else {
                                call.respond(HttpStatusCode.Forbidden, "Events can only be created by admins")
                            }
                        }

                        route("{id}") {
                            route("edit") {
                                get {
                                    val principal = call.principal<UserIdPrincipal>()!!
                                    val email = principal.name
                                    val id = call.parameters["id"]?.toInt() ?: return@get call.badRequest("id")
                                    val isAdmin = DAO.isAdmin(email)
                                    if (isAdmin) {
                                        val eventEditDTO =
                                            DAO.getEventEdite(id)
                                                ?: return@get call.notFound("Event with id $id not fount")
                                        call.respond(eventEditDTO)
                                    } else {
                                        call.respond(HttpStatusCode.Forbidden, "Events can only be created by admins")
                                    }
                                }

                                route("participants") {
                                    route("{userId}") {
                                        put("/points/{points}") {
                                            val principal = call.principal<UserIdPrincipal>()!!
                                            val email = principal.name
                                            val eventId =
                                                call.parameters["id"]?.toInt() ?: return@put call.badRequest("id")
                                            val userId = call.parameters["userId"]?.toInt()
                                                ?: return@put call.badRequest("userId")
                                            val points: Int? = call.parameters["points"]?.toIntOrNull()
                                            val isAdmin = DAO.isAdmin(email)
                                            if (isAdmin) {
                                                DAO.updatePoints(eventId, userId, points)
                                                call.respond(HttpStatusCode.OK)
                                            } else {
                                                call.respond(
                                                    HttpStatusCode.Forbidden,
                                                    "Events can only be created by admins"
                                                )
                                            }
                                        }

                                        delete {
                                            val principal = call.principal<UserIdPrincipal>()!!
                                            val email = principal.name
                                            val eventId =
                                                call.parameters["id"]?.toInt() ?: return@delete call.badRequest("id")
                                            val userId = call.parameters["userId"]?.toInt()
                                                ?: return@delete call.badRequest("userId")

                                            val isAdmin = DAO.isAdmin(email)
                                            if (isAdmin) {
                                                DAO.updateRequest(userId, eventId, "Declined")
                                                DAO.deletePoints(eventId, userId)
                                                call.respond(HttpStatusCode.OK)
                                            } else {
                                                call.respond(
                                                    HttpStatusCode.Forbidden,
                                                    "Events can only be created by admins"
                                                )
                                            }
                                        }
                                    }
                                }

                                route("requests") {
                                    route("{userId}") {
                                        put("accept") {
                                            val principal = call.principal<UserIdPrincipal>()!!
                                            val email = principal.name
                                            val eventId =
                                                call.parameters["id"]?.toInt() ?: return@put call.badRequest("id")
                                            val userId = call.parameters["userId"]?.toInt()
                                                ?: return@put call.badRequest("userId")
                                            val isAdmin = DAO.isAdmin(email)
                                            if (isAdmin) {
                                                DAO.updateRequest(userId, eventId, "Accepted")
                                                call.respond(HttpStatusCode.OK)
                                            } else {
                                                call.respond(
                                                    HttpStatusCode.Forbidden,
                                                    "Events can only be created by admins"
                                                )
                                            }
                                        }

                                        put("decline") {
                                            val principal = call.principal<UserIdPrincipal>()!!
                                            val email = principal.name
                                            val eventId =
                                                call.parameters["id"]?.toInt() ?: return@put call.badRequest("id")
                                            val userId = call.parameters["userId"]?.toInt()
                                                ?: return@put call.badRequest("userId")
                                            val isAdmin = DAO.isAdmin(email)
                                            if (isAdmin) {
                                                DAO.updateRequest(userId, eventId, "Declined")
                                                call.respond(HttpStatusCode.OK)
                                            } else {
                                                call.respond(
                                                    HttpStatusCode.Forbidden,
                                                    "Events can only be created by admins"
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                route("/events") {
                    get {
                        val principal = call.principal<UserIdPrincipal>()!!
                        val email = principal.name
                        val events = DAO.getEventsByEmail(email)
                        call.respond(events)
                    }



                    route("/{id}") {
                        get {
                            val principal = call.principal<UserIdPrincipal>()!!
                            val email = principal.name
                            val id = call.parameters["id"]?.toInt() ?: return@get call.badRequest("id")
                            val eventDTO = DAO.getEventDetailByIdAndUserEmail(id, email)
                                ?: return@get call.notFound("Event with id $id not fount")
                            call.respond(eventDTO)
                        }



                        post("send-request") {
                            val principal = call.principal<UserIdPrincipal>()!!
                            val email = principal.name
                            val id = call.parameters["id"]?.toInt() ?: return@post call.badRequest("id")
                            DAO.insertRequest(email, id)
                            call.respond(HttpStatusCode.OK, "The request has been sent.")
                        }
                    }
                }
            }
        }
//        authenticate("myauth2") {
//            get("/protected/route/form") {
//                val principal = call.principal<UserIdPrincipal>()!!
//                call.respondText("Hello ${principal.name}")
//            }
//        }
    }
}
