package com.example.plugins

import com.example.database.dao.DAO
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.application.*
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
