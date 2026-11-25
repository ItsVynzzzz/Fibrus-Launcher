package com.israadev.nuxlauncher.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

/**
 * Pengunduh berkas milik Fibrus (tanpa pustaka luar): menulis ke berkas sementara, memeriksa ukuran dan SHA-1, baru
 * memindahkannya ke tujuan. Unduhan yang terputus tidak pernah meninggalkan berkas setengah jadi di tujuan.
 */
object Http {
    private const val USER_AGENT = "Fibrus-Launcher/0.1"
    private const val BUFFER = 64 * 1024

    /**
     * [onBytes] dipanggil dengan jumlah byte yang sudah terbaca PADA PERCOBAAN INI (mulai dari nol lagi tiap percobaan
     * ulang), jadi pemanggil menghitung selisihnya sendiri. Berhenti bila coroutine dibatalkan.
     */
    suspend fun download(
        url: String,
        target: File,
        expectedSha1: String? = null,
        expectedSize: Long = -1L,
        retries: Int = 3,
        onBytes: (Long) -> Unit = {}
    ) {
        require(url.startsWith("https://")) { "Hanya https yang diizinkan: $url" }
        target.parentFile?.mkdirs()
        var last: Exception? = null
        for (attempt in 1..retries.coerceAtLeast(1)) {
            coroutineContext.ensureActive()
            val part = File(target.parentFile, "${target.name}.part_${System.nanoTime()}")
            try {
                fetch(url, part, expectedSha1, expectedSize, onBytes)
                target.delete()
                if (!part.renameTo(target)) {
                    part.copyTo(target, overwrite = true)
                    part.delete()
                }
                return
            } catch (e: kotlinx.coroutines.CancellationException) {
                part.delete()
                throw e
            } catch (e: Exception) {
                part.delete()
                last = e
                if (attempt < retries) delay(600L * attempt)
            }
        }
        throw IOException("${target.name}: ${last?.message ?: "gagal"}", last)
    }

    private suspend fun fetch(url: String, part: File, sha1: String?, size: Long, onBytes: (Long) -> Unit) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
        }
        try {
            val code = conn.responseCode
            if (code != 200) throw IOException("HTTP $code")
            val digest = MessageDigest.getInstance("SHA-1")
            var total = 0L
            conn.inputStream.use { input ->
                part.outputStream().use { out ->
                    val buf = ByteArray(BUFFER)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        digest.update(buf, 0, n)
                        total += n
                        onBytes(total)
                        coroutineContext.ensureActive()
                    }
                }
            }
            if (size >= 0L && total != size) throw IOException("ukuran $total, seharusnya $size")
            if (sha1 != null) {
                val got = digest.digest().joinToString("") { "%02x".format(it) }
                if (!got.equals(sha1, ignoreCase = true)) throw IOException("SHA-1 tidak cocok")
            }
        } finally {
            conn.disconnect()
        }
    }
}
