package com.looker.droidify.index

import com.looker.droidify.database.Database
import com.looker.droidify.model.Repository
import com.looker.droidify.utility.common.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * The repository list is not editable on the device. It is fetched from [LIST_URL]
 * (a file in the project's GitHub fork) and the local database is made to match it.
 *
 * If the fetch or parse fails, the database is left untouched, so the last good list stays.
 */
object RemoteRepositories {

    private const val LIST_URL =
        "https://raw.githubusercontent.com/jpsutton/droidify-client/main/repos.json"

    private lateinit var client: OkHttpClient
    private val mutex = Mutex()

    fun init(client: OkHttpClient) {
        this.client = client
    }

    /** Returns true if the local repository table changed. */
    suspend fun refresh(): Boolean = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val wanted = parse(fetch())
                Database.RepositoryAdapter.syncWith(wanted)
            } catch (e: Exception) {
                log(e)
                false
            }
        }
    }

    private fun fetch(): String {
        val request = Request.Builder().url(LIST_URL).header("Cache-Control", "no-cache").build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Repo list fetch failed: HTTP ${response.code}" }
            return response.body.string()
        }
    }

    internal fun parse(json: String): List<Repository> {
        val array = JSONObject(json).getJSONArray("repositories")
        val repos = (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            val address = item.getString("address").trim()
            val fingerprint = item.getString("fingerprint").trim()
            require(address.startsWith("https://")) { "Repo address must be https: $address" }
            require(fingerprint.isNotEmpty()) { "Repo fingerprint missing: $address" }
            Repository.defaultRepository(
                address = address,
                name = item.optString("name", address),
                description = item.optString("description", ""),
                enabled = item.optBoolean("enabled", true),
                fingerprint = fingerprint,
            )
        }
        // An empty or broken list must never wipe every repository.
        require(repos.isNotEmpty()) { "Repo list is empty" }
        return repos
    }
}
