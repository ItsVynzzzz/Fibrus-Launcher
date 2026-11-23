package com.israadev.nuxlauncher.core

import android.app.ActivityManager
import android.content.Context
import com.israadev.nuxlauncher.StarlingLauncher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Unduhan berkas resmi Minecraft untuk mesin Starling (client, server, library, aset; sekitar 500 MiB), dikerjakan
 * launcher sendiri dengan [Http]. Ini menggantikan layar penyiapan milik mesin: begitu penanda `starling-files.json`
 * ada, mesin menganggap berkasnya siap dan tidak menanyakan akun atau mengunduh lagi.
 *
 * Tata letak dan aturannya SAMA dengan yang dibaca mesin (accounts::download_files di sumber Starling): berkas di
 * files/starling/minecraft/{versions,libraries,assets}, SHA-1 tiap berkas dicocokkan dengan daftar Mojang, library
 * dihitung sebagai Linux, penanda ditulis paling akhir. Jangan ubah tanpa mengubah sisi mesin.
 *
 * Patokan (versi dan hash) ada di assets/starling-pins.json dan harus sama dengan sumber mesin yang dibangun.
 */
object GameFiles {
    private const val PINS_ASSET = "starling-pins.json"
    private const val FILES_DONE = "starling-files.json"
    private const val META_URL = "https://piston-meta.mojang.com"
    private const val RESOURCES_URL = "https://resources.download.minecraft.net"
    private const val MAX_FAILED_FILES = 3
    private const val SPACE_MARGIN_BYTES = 256L * 1024L * 1024L
    private const val REPORT_INTERVAL_MS = 300L
    private const val EMPTY_SHA1 = "da39a3ee5e6b4b0d3255bfef95601890afd80709"

    class Pins(val version: String, val manifests: List<String>, val clientSha1: String, val serverSha1: String)

    class Progress(
        val filesDone: Int,
        val filesTotal: Int,
        val bytesDone: Long,
        val bytesTotal: Long,
        val bytesPerSecond: Long,
        val preparing: Boolean
    ) {
        val fraction: Float get() = if (bytesTotal > 0L) (bytesDone.toFloat() / bytesTotal).coerceIn(0f, 1f) else 0f
    }

    private class Item(val url: String, val file: File, val sha1: String, val size: Long)

    @Volatile private var cachedPins: Pins? = null

    /** Patokan dari assets, atau null bila tidak ada/rusak/bukan untuk versi yang dikerjakan Starling. */
    fun pins(context: Context): Pins? {
        cachedPins?.let { return it }
        return try {
            val json = JSONObject(context.applicationContext.assets.open(PINS_ASSET).use { it.readBytes().toString(Charsets.UTF_8) })
            val manifests = ArrayList<String>()
            manifests += json.optString("manifest_sha1")
            json.optJSONArray("accepted_manifest_sha1")?.let { for (i in 0 until it.length()) manifests += it.optString(i) }
            val pins = Pins(json.optString("version"), manifests, json.optString("client_sha1"), json.optString("server_sha1"))
            val ok = pins.version == StarlingLauncher.MC_VERSION && pins.manifests.all(::isSha1) &&
                isSha1(pins.clientSha1) && isSha1(pins.serverSha1)
            if (ok) pins.also { cachedPins = it } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun isSha1(s: String) = s.length == 40 && s.all { it in '0'..'9' || it in 'a'..'f' }

    fun root(context: Context): File = File(StarlingLauncher.home(context), "minecraft")

    /** true bila penanda "semua berkas ada" sudah ditulis (oleh launcher ini atau oleh mesin). */
    fun installed(context: Context): Boolean = File(root(context), FILES_DONE).isFile

    /** Ukuran folder berkas game (byte). Jalankan di thread latar. */
    fun sizeOnDisk(context: Context): Long =
        try { root(context).walkTopDown().filter { it.isFile }.sumOf { it.length() } } catch (_: Throwable) { 0L }

    /** Menghapus penanda saja: unduhan berikutnya memeriksa ulang semua berkas dan hanya mengunduh yang kurang/rusak. */
    fun invalidate(context: Context) {
        File(root(context), FILES_DONE).delete()
    }

    fun deleteAll(context: Context) {
        root(context).deleteRecursively()
    }

    suspend fun download(context: Context, onProgress: (Progress) -> Unit): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val pins = pins(context) ?: throw IOException("Patokan berkas Starling tidak tersedia di aplikasi ini.")
            runDownload(context.applicationContext, pins, onProgress)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun runDownload(context: Context, pins: Pins, onProgress: (Progress) -> Unit) {
        val root = root(context)
        if (!root.isDirectory && !root.mkdirs()) throw IOException("Folder data tidak bisa dibuat.")
        cleanStaleParts(root)
        File(root, FILES_DONE).delete() // unduhan yang terputus tidak boleh tampak selesai
        onProgress(Progress(0, 0, 0L, 0L, 0L, preparing = true))
        val v = pins.version

        // 1. Berkas versi (hash dipatok)
        val manifestFile = File(root, "versions/$v/$v.json")
        val manifestBytes = readIfHashIn(manifestFile, pins.manifests) ?: run {
            Http.download("$META_URL/v1/packages/${pins.manifests[0]}/$v.json", manifestFile, expectedSha1 = pins.manifests[0], retries = 4)
            manifestFile.readBytes()
        }
        val json = try { JSONObject(manifestBytes.toString(Charsets.UTF_8)) } catch (_: Exception) {
            throw IOException("Berkas versi $v dari Mojang tidak valid.")
        }

        // 2. Daftar berkas: client, server, library, indeks aset, objek aset
        val items = LinkedHashMap<String, Item>()
        fun add(i: Item) { items.putIfAbsent(i.file.absolutePath, i) }
        val downloads = json.optJSONObject("downloads") ?: throw IOException("Berkas versi $v tidak mendaftar unduhan.")
        fun jar(key: String, file: File, pinned: String): Item {
            val d = downloads.optJSONObject(key) ?: throw IOException("Berkas versi $v tidak mendaftar unduhan $key.")
            val url = d.optString("url"); val sha1 = d.optString("sha1")
            if (url.isBlank() || sha1.isBlank()) throw IOException("Berkas versi $v tidak mendaftar unduhan $key.")
            if (sha1 != pinned) throw IOException("Jar $key di berkas versi berbeda dari yang dipatok Starling.")
            return Item(url, file, sha1, d.optLong("size"))
        }
        val clientJar = File(root, "versions/$v/$v.jar")
        val serverJar = File(root, "versions/$v/$v-server.jar")
        add(jar("client", clientJar, pins.clientSha1))
        add(jar("server", serverJar, pins.serverSha1))

        val librariesDir = File(root, "libraries")
        json.optJSONArray("libraries")?.let { libs ->
            for (i in 0 until libs.length()) {
                val lib = libs.optJSONObject(i) ?: continue
                val a = lib.optJSONObject("downloads")?.optJSONObject("artifact") ?: continue
                val path = a.optString("path"); val url = a.optString("url"); val sha1 = a.optString("sha1")
                if (path.isBlank() || url.isBlank() || sha1.isBlank() || !libraryApplies(lib)) continue
                val target = resolveInside(librariesDir, path) ?: continue
                add(Item(url, target, sha1, a.optLong("size")))
            }
        }

        val idx = json.optJSONObject("assetIndex") ?: throw IOException("Berkas versi $v tidak mendaftar indeks aset.")
        val indexId = idx.optString("id"); val indexUrl = idx.optString("url"); val indexSha1 = idx.optString("sha1")
        if (indexId.isBlank() || indexUrl.isBlank() || indexSha1.isBlank()) throw IOException("Indeks aset tidak lengkap.")
        val indexFile = resolveInside(File(root, "assets/indexes"), "$indexId.json") ?: throw IOException("Nama indeks aset tidak valid.")
        val indexBytes = readIfHashIn(indexFile, listOf(indexSha1)) ?: run {
            Http.download(indexUrl, indexFile, expectedSha1 = indexSha1, retries = 4)
            indexFile.readBytes()
        }
        val objects = try { JSONObject(indexBytes.toString(Charsets.UTF_8)).optJSONObject("objects") } catch (_: Exception) { null }
            ?: throw IOException("Indeks aset dari Mojang tidak valid.")
        val objectsDir = File(root, "assets/objects")
        val names = objects.keys()
        while (names.hasNext()) {
            val e = objects.optJSONObject(names.next()) ?: continue
            val hash = e.optString("hash")
            if (!isSha1(hash)) continue
            val p = hash.substring(0, 2)
            add(Item("$RESOURCES_URL/$p/$hash", File(objectsDir, "$p/$hash"), hash, e.optLong("size")))
        }

        // 3. Ruang kosong
        val pending = items.values.filter { !(it.file.isFile && it.file.length() == it.size) }
        val pendingBytes = pending.sumOf { it.size.coerceAtLeast(0L) }
        val totalBytes = items.values.sumOf { it.size.coerceAtLeast(0L) }
        val free = root.usableSpace
        if (pendingBytes + SPACE_MARGIN_BYTES > free) {
            throw IOException("Not enough storage: about ${(pendingBytes + SPACE_MARGIN_BYTES - free) / (1024L * 1024L) + 1L} MB more needed.")
        }

        // 4. Unduh: yang besar dulu, beberapa sekaligus
        val bytes = AtomicLong(totalBytes - pendingBytes)
        val files = AtomicInteger(items.size - pending.size)
        val failed = AtomicInteger(0)
        val firstError = AtomicReference<String?>(null)
        val queue = ConcurrentLinkedQueue(pending.sortedByDescending { it.size })
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mem = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val workers = if (mem.totalMem / (1024L * 1024L) < 2500L) 8 else 16

        fun report(speed: Long) = onProgress(Progress(files.get(), items.size, bytes.get().coerceAtMost(totalBytes), totalBytes, speed, false))
        report(0L)

        coroutineScope {
            val reporter = launch {
                var lastBytes = bytes.get(); var lastTime = System.nanoTime(); var speed = 0.0
                while (true) {
                    delay(REPORT_INTERVAL_MS)
                    val now = System.nanoTime(); val cur = bytes.get()
                    val sec = (now - lastTime) / 1_000_000_000.0
                    if (sec > 0.0) {
                        val inst = ((cur - lastBytes) / sec).coerceAtLeast(0.0)
                        speed = if (speed == 0.0) inst else speed * 0.7 + inst * 0.3
                    }
                    lastBytes = cur; lastTime = now
                    report(speed.toLong())
                }
            }
            val jobs = List(workers) {
                async {
                    while (failed.get() < MAX_FAILED_FILES) {
                        ensureActive()
                        val item = queue.poll() ?: break
                        item.file.parentFile?.mkdirs()
                        if (item.size == 0L && item.sha1 == EMPTY_SHA1) {
                            item.file.writeBytes(ByteArray(0)); files.incrementAndGet(); continue
                        }
                        var counted = 0L
                        try {
                            Http.download(item.url, item.file, expectedSha1 = item.sha1, expectedSize = item.size, retries = 3) { read ->
                                bytes.addAndGet(read - counted); counted = read // percobaan ulang mulai dari nol: selisih negatif, hitungan tetap benar
                            }
                            files.incrementAndGet()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            bytes.addAndGet(-counted)
                            firstError.compareAndSet(null, "${item.file.name}: ${e.message}")
                            failed.incrementAndGet()
                        }
                    }
                }
            }
            try { jobs.awaitAll() } finally { reporter.cancel() }
        }
        currentCoroutineContext().ensureActive()
        if (failed.get() > 0) {
            throw IOException("Some files failed to download. Check your connection and try again; finished files are kept. (${firstError.get() ?: "unknown"})")
        }
        items.values.firstOrNull { !it.file.isFile }?.let { throw IOException("${it.file.name} is missing after download.") }

        // 5. Penanda ditulis paling akhir (isi dan nama sama dengan yang ditulis mesin)
        val done = JSONObject().put("version", v).put("client", clientJar.absolutePath).put("server", serverJar.absolutePath)
        val temp = File(root, "$FILES_DONE.tmp")
        temp.writeText(done.toString())
        val marker = File(root, FILES_DONE)
        marker.delete()
        if (!temp.renameTo(marker)) { temp.delete(); throw IOException("$FILES_DONE tidak bisa disimpan.") }
        report(0L)
    }

    private fun readIfHashIn(file: File, hashes: List<String>): ByteArray? {
        if (!file.isFile) return null
        val data = try { file.readBytes() } catch (_: Exception) { return null }
        val digest = MessageDigest.getInstance("SHA-1").digest(data).joinToString("") { "%02x".format(it) }
        return if (digest in hashes) data else null
    }

    /** Aturan library Mojang untuk HP: dihitung sebagai Linux, hanya artefak biasa (tanpa klasifier native). */
    private fun libraryApplies(lib: JSONObject): Boolean {
        val rules = lib.optJSONArray("rules") ?: return true
        var allowed = false
        for (i in 0 until rules.length()) {
            val r = rules.optJSONObject(i) ?: continue
            val os = r.optJSONObject("os")?.optString("name")?.takeIf { it.isNotEmpty() }
            if (os == null || os == "linux") allowed = r.optString("action") == "allow"
        }
        return allowed
    }

    /** [relative] di dalam [base]; null bila mencoba keluar dari folder itu (../ dan sejenisnya). */
    private fun resolveInside(base: File, relative: String): File? {
        if (relative.startsWith("/") || relative.contains('\\')) return null
        val target = File(base, relative).canonicalFile
        val b = base.canonicalFile
        return if (target.path.startsWith(b.path + File.separator)) target else null
    }

    private fun cleanStaleParts(root: File) {
        val cutoff = System.currentTimeMillis() - 10L * 60L * 1000L
        try {
            root.walkTopDown().filter { it.isFile && it.name.contains(".part_") && it.lastModified() < cutoff }.forEach { it.delete() }
        } catch (_: Throwable) {
        }
    }
}
