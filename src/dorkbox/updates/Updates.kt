package dorkbox.updates

import dorkbox.propertyLoader.Property
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.fixedRateTimer


object Updates {
    data class UpdateInfo(val uuid: String, val data: String) {
        override fun toString(): String {
            return "$uuid:$data"
        }
    }

    /**
     * Enables the use of the update system, which verifies this class + UUID + data information with the update server.
     */
    @Property
    @Volatile
    var ENABLE = true

    private var SERVER = "updates.dorkbox.com"

    private val instanceUuid = UUID.randomUUID().toString().replace("-", "")

    // create a map of all UUIDs. and only run an update once all have been collected.
    // This runs max 1x every 4 hours (5 minutes after startup) - and only a SINGLE request is made for all checked UUIDs.
    private val updates = mutableMapOf<Class<*>, MutableList<UpdateInfo>>()

    init {
        fixedRateTimer("update-system", true, TimeUnit.MINUTES.toMillis(5), TimeUnit.HOURS.toMillis(4)) {
            if (ENABLE) {
                runAtTimedInterval()
            }
        }
    }

    private fun runAtTimedInterval() {
        // convert the hashmap into something serialized. This is super, super simple and not optimized. Again, no dependencies!
        val buffer = StringBuilder()
        buffer.append(instanceUuid)

        synchronized(updates) {
            val entries = updates.entries
            if (entries.size > 0) {
                buffer.append(':')
            }

            entries.forEachIndexed { i, (key, value) ->
                buffer.append(key.name) // classNameOnly
                buffer.append('[')
                value.forEachIndexed { j, it ->
                    buffer.append(it)
                    if (j < value.size - 1) {
                        buffer.append(',')
                    }
                }
                buffer.append(']')

                if (i < entries.size - 1) {
                    buffer.append(':')
                }
            }
        }

        val sendData = buffer.toString().toByteArray()

        // default is http! This way we don't have to deal with crypto, and since there is nothing PRIVATE about the connection,
        // this is fine - at least for now. We DO want to support redirects, in case OLD code is running in the wild.
        var location = "http://$SERVER/"

        var base: URL
        var next: URL
        val visited = mutableMapOf<String, Int>()
        var visitedCount: Int


        while (true) {
            visitedCount = (visited[location] ?: 0) + 1
            visited[location] = visitedCount

            if (visitedCount > 3)  {
//                println("Stuck in a loop")
                return
            }

            try {
                base = URL(location)
                with(base.openConnection() as HttpURLConnection) {
                    requestMethod = "POST"
                    doOutput = true
                    useCaches = false
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Dorkbox-Updates")
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    setRequestProperty("charset", "utf-8")
                    setRequestProperty("Content-Length", sendData.size.toString())

                    // must write data to the server (the server might not even read this, especially if there is no connection)
                    DataOutputStream(outputStream).use {
                        it.write(sendData)
                    }

//                    println("URL : $url")
//                    println("Response Code : $responseCode")

                    when (responseCode) {
                        HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_MOVED_TEMP -> {
                            location = getHeaderField("Location")
                            location = URLDecoder.decode(location, "UTF-8")

//                            println("Response redirect to: $location")

                            next = URL(base, location) // Deal with relative URLs
                            location = next.toExternalForm()
                            return@with
                        }
                        HttpURLConnection.HTTP_OK -> {
//                            InputStreamReader(inputStream).useLines {
//                                it.forEach {
//                                    println("Response : $it")
//                                }
//                            }

                            // done
                            return
                        }
                        else -> {
//                            println("Response error: $location")
                            // done
                            return
                        }
                    }
                }
            } catch (e: Exception) {
//                e.printStackTrace()
            }
        }
    }

    
    
    /**
     * Verifies this class + UUID + version information with the update server.
     *
     *
     * Only the class + UUID + version information is tracked, saved, and used by dorkbox, llc. Nothing else.
     *
     * Specifically, IP address/machine info/time/http headers/user info/names/etc is not used, saved, or tracked.
     *
     * In accordance with the GDPR, there is absolutely NO DATA specific to the machine, connection, or user that is saved, read or used.
     *
     * While this is trivial to override/modify/replace to do absolutely nothing, the purpose of this is to manage the update info
     * and usage across all projects and systems owned by dorkbox, llc.
     *
     * @param class the class that is to be registered
     * @param uuid the uuid assigned to the class
     * @param data any extra data specific to the class
     */
    fun add(`class`: Class<*>, uuid: String, data: String) {
        val updateInfo = UpdateInfo(uuid, data)

        synchronized(updates) {
            var mutableList = updates[`class`]

            if (mutableList == null) {
                mutableList = mutableListOf()
                updates[`class`] = mutableList
            }

            if (!mutableList.contains(updateInfo)) {
                mutableList.add(updateInfo)
            }
        }
    }
}
