package com.samsung.android.scan3d.http

import android.content.SharedPreferences
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import java.io.OutputStream

class HttpService {
    lateinit var engine: NettyApplicationEngine
    var channel = Channel<ByteArray>(2)
    /*fun producer(): suspend OutputStream.() -> Unit = {
        val o = this
        channel = Channel()
        channel.consumeEach {
            o.write("--FRAME\r\nContent-Type: image/jpeg\r\n\r\n".toByteArray())
            o.write(it)
            o.flush()
        }
    }
    public fun main() {
        engine = embeddedServer(Netty, port = 8080) {
            routing {
                get("/cam") {
                    call.respondText("Ok")
                }
                get("/cam.mjpeg") {
                    call.respondOutputStream(
                        ContentType.parse("multipart/x-mixed-replace;boundary=FRAME"),
                        HttpStatusCode.OK, producer()
                    )
                }
            }
        }
        engine.start(wait = false)
    }*/
    private fun producer(clientChannel: Channel<ByteArray>): suspend OutputStream.() -> Unit = {
        val o = this
        clientChannel.consumeEach {
            o.write("--FRAME\r\nContent-Type: image/jpeg\r\n\r\n".toByteArray())
            o.write(it)
            o.flush()
        }
    }

    public fun main() {
        engine = embeddedServer(Netty, port = 8080) {
            routing {
                get("/cam") {
                    call.respondText("Ok")
                }
                get("/cam.mjpeg") {
                    val clientChannel = Channel<ByteArray>(2)
                    launch {
                        channel.consumeEach { frame ->
                            clientChannel.send(frame)
                        }
                    }
                    call.respondOutputStream(
                        ContentType.parse("multipart/x-mixed-replace;boundary=FRAME"),
                        HttpStatusCode.OK, producer(clientChannel)
                    )
                }
            }
        }
        engine.start(wait = false)
    }

}