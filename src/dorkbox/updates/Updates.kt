package dorkbox.updates

import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.util.*
import java.util.concurrent.TimeUnit


object Updates {
    data class UpdateInfo(val uuid: String, val data: String) {
        override fun toString(): String {
            return "$uuid:$data"
        }
    }

    /**
     * Gets the version number.
     */
    const val version = "1.1"

    /**
     * Enables the use of the update system, which verifies this class + UUID + data information with the update server.
     *
     * If not manually set, it will compare the System Property 'dorkbox.Updates.ENABLED' to the value 'true'. Any other value
     * will disable the update loop
     */
    @Volatile
    var ENABLE = try { System.getProperty("${Updates::class.qualifiedName}.ENABLE", "true") == "true" } catch (e: Exception) { true }

    /**
     * Enables output for debugging
     */
    @Volatile
    internal var DEBUG = false

    private var SERVER = "updates.dorkbox.com"

    private val instanceUuid = UUID.randomUUID().toString().replace("-", "")

    // create a map of all UUIDs. and only run an update once all have been collected.
    // This runs max 1x every 4 hours (5 minutes after startup) - and only a SINGLE request is made for all checked UUIDs.
    private val updates = mutableMapOf<Class<*>, MutableList<UpdateInfo>>()

    private fun runAtTimedInterval() {
        // convert the hashmap into something serialized. This is super, super simple and not optimized. Again, no dependencies!
        val buffer = StringBuilder()

        // this is the INSTANCE UUID of the currently running process
        buffer.append(instanceUuid)
        buffer.append(':')
        buffer.append(version)

        synchronized(updates) {
            val entries = updates.entries
            if (entries.size > 0) {
                buffer.append(':')
            }

            entries.forEachIndexed { i, (key, value) ->
                buffer.append(key.name) // registered class name only, ie: 'dorkbox.TestA'
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

        val toString = buffer.toString()
        if (DEBUG) {
            println("Update string: $toString")
        }
        val sendData = toString.toByteArray()

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
                if (DEBUG) {
                    println("Stuck in a loop for $location")
                }
                return
            }

            try {
                base = URL(location)
                with(base.openConnection() as HttpURLConnection) {
                    requestMethod = "POST"
                    doOutput = true
                    useCaches = false
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Dorkbox-Updater")
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    setRequestProperty("charset", "utf-8")
                    setRequestProperty("Content-Length", sendData.size.toString())

                    // must write data to the server (the server might not even read this, especially if there is no connection)
                    DataOutputStream(outputStream).use {
                        it.write(sendData)
                    }

                    if (DEBUG) {
                        println("Requesting URL : $url")
                        println("Response Code : $responseCode")
                    }

                    when (responseCode) {
                        HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_MOVED_TEMP -> {
                            location = getHeaderField("Location")
                            location = URLDecoder.decode(location, "UTF-8")

                            if (DEBUG) {
                                println("Response to '$url' redirected to  '$location'")
                            }

                            next = URL(base, location) // Deal with relative URLs
                            location = next.toExternalForm()
                            return@with
                        }
                        HttpURLConnection.HTTP_OK -> {
                            if (DEBUG) {
                                InputStreamReader(inputStream).useLines { line ->
                                    line.forEach {
                                        println("Response : $it")
                                    }
                                }
                            }

                            // done
                            return
                        }
                        else -> {
                            if (DEBUG) {
                                println("Response error: $location")
                            }

                            // done
                            return
                        }
                    }
                }
            } catch (e: Exception) {
                if (DEBUG) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun initThread() {
        val t = Thread {
            if (DEBUG) {
                Thread.sleep(TimeUnit.SECONDS.toMillis(5))
            } else {
                Thread.sleep(TimeUnit.MINUTES.toMillis(5))
            }

            // give time on startup to configure the ENABLE flag
            if (!ENABLE) {
                return@Thread
            }

            while (true) {
                try {
                    runAtTimedInterval()
                    Thread.sleep(TimeUnit.HOURS.toMillis(4))
                } catch (e: Exception) {
                    if (DEBUG) {
                        e.printStackTrace()
                    }
                }
            }
        }

        t.isDaemon = true
        t.name = "Maintenance Updates"
        t.priority = Thread.MIN_PRIORITY // lowest priority is fine for us

        t.start()
    }
    
    /**
     * Verifies this class + UUID + version information with the update server.
     *
     * What is used?
     *   + class
     *   + UUID
     *   + version
     *   This information is tracked, saved, and used by dorkbox, llc. Nothing else. This data is not shared or used by any entity
     *   other than dorkbox, llc - and it is used and processed on-premises.
     *
     * What is NOT used?
     *  + IP address
     *  + machine info
     *  + cookies
     *  + time
     *  + http headers
     *  + user info
     *  + names
     *  + etc
     *  This (non-exhaustive list) is not used, saved, processed, or tracked by anyone. This information thrown out on ingress.
     *
     * In accordance with the GDPR, there is absolutely NO DATA specific to the machine, connection, or user that is saved, read or used.
     *
     * While this is trivial to override/modify/replace to do absolutely nothing, the purpose of this is to manage the update info
     * and usage across all projects and systems using dorkbox technologies.
     *
     * @param class the class that is to be registered
     * @param uuid the uuid assigned to the class
     * @param data any extra data specific to the class
     */
    fun add(`class`: Class<*>, uuid: String, data: String) {
        if (DEBUG) {
            println("Adding ${`class`.name} $uuid, $data")
        }

        val updateInfo = UpdateInfo(uuid, data)
        synchronized(updates) {
            if (updates.isEmpty()) {
                initThread();
            }

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
