package com.github.zly2006.plugins

import com.github.zly2006.Yggdrasill
import com.github.zly2006.yggdrasill
import com.google.gson.Gson
import com.google.gson.JsonElement
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.locations.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import java.io.File
import java.util.Objects
import java.util.UUID

fun Application.configureRouting() {
    install(Locations) {
    }

    suspend fun ApplicationCall.err(error: String, errorMessage: String) {
        respondText(contentType = ContentType.parse("application/json")) {
            Gson().toJson(object {
                val error = error
                val errorMessage = errorMessage
            })
        }
    }
    suspend fun ApplicationCall.respondJson(json: Any) {
        respondText(contentType = ContentType.parse("application/json")) {
            Gson().toJson(json)
        }
    }

    routing {
        post("/") {
            call.respondText(contentType = ContentType.parse("application/json")) {
                "{}"
            }
        }
        get("/") {
            call.respondFile(File("index.html"))
        }
        post("/authenticate") {
            class Req {
                inner class Agent {
                    var name = ""
                    var version = 0
                }
                var agent = Agent()
                var username = ""
                var password = ""
                var clientToken = ""
                var requestUser = false
            }
            class Res {
                inner class User{
                    var username = ""
                    var id = ""
                }
                var user: User? = null
                var clientToken = ""
                var accessToken = ""
                var availableProfiles = mutableListOf<Yggdrasill.User.Profile>()
                var selectedProfile = Yggdrasill.User.Profile()
            }
            val req = try {
                Gson().fromJson(call.receiveText(), Req::class.java)
            } catch (e: Exception) {
                call.err("Method Not Allowed","")
                return@post
            }
            val cid = if (req.clientToken != "") req.clientToken else UUID.randomUUID().toString()
            val u = yggdrasill.users[req.username]
            if (u == null ||
                    u.password != req.password) {
                call.err("ForbiddenOperationException","ForbiddenOperationException")
                return@post
            }
            val res = Res()
            u.accessToken = yggdrasill.newToken()
            u.client = cid
            res.clientToken = cid
            res.accessToken = u.accessToken
            res.availableProfiles = u.availableProfiles
            res.selectedProfile = u.selectedProfile
            if (req.requestUser) {
                res.user = res.User()
                res.user!!.id = u.id
                res.user!!.username = u.username
            }
            call.respondJson(res)
        }
        post("/refresh") {
            class Req {
                var accessToken = ""
                var clientToken = ""
                var selectedProfile: Yggdrasill.User.Profile? = null
                var requestUser = false
            }
            class Res {
                inner class User{
                    var username = ""
                    var id = ""
                }
                var user: User? = null
                var clientToken = ""
                var accessToken = ""
                var selectedProfile: Yggdrasill.User.Profile? = null
            }
            val req = try {
                Gson().fromJson(call.receiveText(), Req::class.java)
            } catch (e: Exception) {
                call.err("Method Not Allowed","")
                return@post
            }
            val u = yggdrasill.users.entries.first { it.value.accessToken == req.accessToken }
            u.value.accessToken = yggdrasill.newToken()
            val res = Res()
            if (req.requestUser) {
                res.user = res.User()
                res.user!!.id = u.value.id
                res.user!!.username = u.value.username
            }
            res.clientToken = u.value.client
            res.accessToken = u.value.accessToken
            if (req.selectedProfile != null) {
                u.value.selectedProfile = u.value.availableProfiles.first { it.id == req.selectedProfile!!.id }
            }
            res.selectedProfile = u.value.selectedProfile
            call.respondJson(res)
        }
        post("/validate") {
            class Req {
                var accessToken = ""
                var clientToken = ""
            }
            val req = try {
                Gson().fromJson(call.receiveText(), Req::class.java)
            } catch (e: Exception) {
                call.err("Method Not Allowed","")
                return@post
            }
            if (yggdrasill.users.values.firstOrNull { it.accessToken == req.accessToken } != null) {
                call.respond(HttpStatusCode.NoContent, "")
            } else {
                call.respond(HttpStatusCode.Forbidden,"{\"error\":\"\",\"errorMessage\":\"\"}")
            }
        }
        post("/invalidate") {
            class Req {
                var accessToken = ""
                var clientToken = ""
            }
            val req = try {
                Gson().fromJson(call.receiveText(), Req::class.java)
            } catch (e: Exception) {
                call.err("Method Not Allowed","")
                return@post
            }
            val u = yggdrasill.users.values.firstOrNull { it.accessToken == req.accessToken }
            if (u != null) {
                u.accessToken = ""
            } else call.err("", "")
        }
        get<MyLocation> {
            call.respondText("Location: name=${it.name}, arg1=${it.arg1}, arg2=${it.arg2}")
        }
        // Register nested routes
        get<Type.Edit> {
            call.respondText("Inside $it")
        }
        get<Type.List> {
            call.respondText("Inside $it")
        }
    }
}

@Location("/location/{name}")
class MyLocation(val name: String, val arg1: Int = 42, val arg2: String = "default")
@Location("/type/{name}")
data class Type(val name: String) {
    @Location("/edit")
    data class Edit(val type: Type)

    @Location("/list/{page}")
    data class List(val type: Type, val page: Int)
}
