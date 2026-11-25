package com.israadev.nuxlauncher.core

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Menjalankan unduhan berkas game di luar siklus layar (rotasi/ganti tab tidak mengulang atau menghentikannya). */
object DownloadController {
    sealed interface State {
        data object Idle : State
        data class Running(val progress: GameFiles.Progress) : State
        data class Failed(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    val running: Boolean get() = _state.value is State.Running

    /** [onDone] dipanggil di thread utama setelah semua berkas lengkap. */
    fun start(context: Context, onDone: () -> Unit = {}) {
        if (job?.isActive == true) return
        val app = context.applicationContext
        _state.value = State.Running(GameFiles.Progress(0, 0, 0L, 0L, 0L, preparing = true))
        // Penjaga proses + notifikasi progres. Bila gagal dimulai, unduhan tetap jalan selama aplikasi terbuka.
        try { ContextCompat.startForegroundService(app, Intent(app, DownloadService::class.java)) } catch (_: Exception) { }
        job = scope.launch {
            val result = GameFiles.download(app) { p -> _state.value = State.Running(p) }
            result.fold(
                onSuccess = {
                    _state.value = State.Idle
                    withContext(Dispatchers.Main) { onDone() }
                },
                onFailure = { e ->
                    _state.value = if (e is kotlinx.coroutines.CancellationException) State.Idle
                    else State.Failed(e.message ?: "Download failed.")
                }
            )
        }
    }

    fun cancel() {
        job?.cancel()
        _state.value = State.Idle
    }

    fun dismissError() {
        if (_state.value is State.Failed) _state.value = State.Idle
    }
}
