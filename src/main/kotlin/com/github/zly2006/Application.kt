package com.github.zly2006

import io.ktor.server.application.*
import com.github.zly2006.plugins.*
import com.google.gson.Gson
import io.ktor.util.*
import java.io.File
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random
import kotlin.random.nextULong
import kotlin.system.exitProcess

class Yggdrasill {
    var secret = ""
    class User {
        class Profile(
            var id: String = "",
            var name: String = ""
        )

        var username = ""
        var password = ""
        var accessToken = ""
        var client = ""
        var id = ""
        var availableProfiles = mutableListOf<Profile>()
        var selectedProfile = Profile()
    }
    var users = mutableMapOf<String, User>()
    fun newToken(): String {
        val gson = Gson()
        val header = gson.toJson(object {
            val typ = "JWT"
            val alg = "H256"
        }).encodeBase64()
        val body = gson.toJson(object {
            val ran = Random.nextULong()
            val iss = "yggdrasill"
            val sub = ""
            val iat = Calendar.getInstance().timeInMillis
            val exp = Calendar.getInstance().timeInMillis + 1000 * 3600 * 72 // 3 days
        }).encodeBase64()
        val sign = Mac.getInstance("HmacSHA256").let {
            it.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
            it.doFinal("$header.$body".toByteArray()).encodeBase64()
        }
        return "$header.$body.$sign"
    }
    fun verifyToken(token: String): Boolean {
        return false
    }
}

var yggdrasill = Yggdrasill()

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    if (!File("index.html").exists()) {
        File("index.html").writeBytes(
            javaClass.classLoader.getResource("index.html").readBytes()
        )
    }
    yggdrasill = Gson().fromJson(File("data.json").readText(), Yggdrasill::class.java)
    configureSecurity()
    configureRouting()
    while (true) {
        val cmd = readln().split(' ')
        when (cmd[0]) {
            "" -> {}
            "stop", "end" -> exitProcess(0)
            "adduser" -> {
                if (cmd.size < 3) {
                    println("adduser <name> <password> [uuid]")
                    continue
                }
                val id = if (cmd.size >= 4) cmd[3] else UUID.randomUUID().toString()
                val name = cmd[1]
                val password = cmd[2]
                if (yggdrasill.users[name] != null) {
                    println("user already exists.")
                    continue
                }
                yggdrasill.users[name] = Yggdrasill.User()
                yggdrasill.users[name] as Yggdrasill.User
                yggdrasill.users[name]!!.id = id
                yggdrasill.users[name]!!.password = password
                yggdrasill.users[name]!!.username = name
                yggdrasill.users[name]!!.availableProfiles.add(Yggdrasill.User.Profile(UUID.randomUUID().toString(), name))
                yggdrasill.users[name]!!.selectedProfile = yggdrasill.users[name]!!.availableProfiles[0]
            }
        }
    }
}
