package com.example.plugins

import com.example.database.dao.DAO
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.*

fun Application.configureRouting() {
    routing {
        route("api") {
            get("/check-user") {
                val email = call.request.queryParameters["email"] ?: return@get call.badRequest("email")
                val password = call.request.queryParameters["password"] ?: return@get call.badRequest("password")

                val exists = DAO.checkUser(email, password)

                if (exists) {
                    //call.ok("Users exists")
                } else {
                    call.notFound("User does not exist")
                }
            }
        }
    }
}

suspend fun ApplicationCall.badRequest(param: String): Nothing {
    respond(HttpStatusCode.BadRequest, "Missing parameter: $param")
    throw BadRequestException("Missing parameter: $param")
}

suspend fun ApplicationCall.notFound(message: String): Nothing {
    respond(HttpStatusCode.NotFound, message)
    throw NotFoundException()
}
