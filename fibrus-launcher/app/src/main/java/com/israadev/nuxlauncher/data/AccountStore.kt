package com.israadev.nuxlauncher.data

import android.content.Context
import com.israadev.nuxlauncher.StarlingLauncher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class UserAccount(val id: String, val username: String)

/**
 * Daftar akun milik launcher dan akun yang aktif. Tidak ada layar "pilih akun" di awal: yang aktif di sini yang dipakai
 * (Home menampilkannya, StarlingLauncher menuliskannya ke berkas akun mesin sebelum game dibuka).
 * Baru akun offline; akun Microsoft belum ada.
 */
object AccountStore {
    data class State(val accounts: List<UserAccount> = emptyList(), val selectedId: String? = null) {
        val active: UserAccount? get() = accounts.firstOrNull { it.id == selectedId } ?: accounts.firstOrNull()
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    private var file: File? = null

    @Synchronized
    fun init(context: Context) {
        if (file != null) return
        val f = File(context.filesDir, "fibrus/accounts.json")
        file = f
        _state.value = try {
            val json = JSONObject(f.readText())
            val list = json.optJSONArray("accounts") ?: JSONArray()
            val accounts = (0 until list.length()).mapNotNull {
                val o = list.optJSONObject(it) ?: return@mapNotNull null
                val name = o.optString("username")
                if (StarlingLauncher.validName(name)) UserAccount(o.optString("id").ifBlank { UUID.randomUUID().toString() }, name) else null
            }
            State(accounts, json.optString("selected").takeIf { it.isNotBlank() })
        } catch (_: Exception) {
            State()
        }
    }

    /** Menambah akun offline lalu menjadikannya aktif. Mengembalikan pesan galat, atau null bila berhasil. */
    @Synchronized
    fun addOffline(username: String): String? {
        val name = username.trim()
        if (!StarlingLauncher.validName(name)) return "Name must be 1 to 16 letters, digits or underscores."
        val current = _state.value
        if (current.accounts.any { it.username.equals(name, ignoreCase = true) }) return "That name already exists."
        val account = UserAccount(UUID.randomUUID().toString(), name)
        update(State(current.accounts + account, account.id))
        return null
    }

    @Synchronized
    fun select(id: String) {
        val current = _state.value
        if (current.accounts.any { it.id == id }) update(current.copy(selectedId = id))
    }

    @Synchronized
    fun remove(id: String) {
        val current = _state.value
        val left = current.accounts.filterNot { it.id == id }
        update(State(left, if (current.selectedId == id) left.firstOrNull()?.id else current.selectedId))
    }

    private fun update(next: State) {
        _state.value = next
        val f = file ?: return
        try {
            f.parentFile?.mkdirs()
            val json = JSONObject()
                .put("accounts", JSONArray().also { arr ->
                    next.accounts.forEach { arr.put(JSONObject().put("id", it.id).put("username", it.username)) }
                })
                .put("selected", next.active?.id ?: JSONObject.NULL)
            val temp = File(f.parentFile, "accounts.json.tmp")
            temp.writeText(json.toString())
            f.delete()
            if (!temp.renameTo(f)) temp.delete()
        } catch (_: Exception) {
        }
    }
}
