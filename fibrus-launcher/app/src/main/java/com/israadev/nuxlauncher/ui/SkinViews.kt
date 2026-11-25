package com.israadev.nuxlauncher.ui

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.floor
import kotlin.math.roundToInt

/** Skin bawaan (Steve, 64x64) dari assets. Skin pemain sendiri belum ada. */
private object SkinSource {
    @Volatile private var cache: ImageBitmap? = null
    fun default(context: Context): ImageBitmap = cache ?: BitmapFactory
        .decodeStream(context.applicationContext.assets.open("steve.png"))
        .asImageBitmap().also { cache = it }
}

private fun DrawScope.part(
    img: ImageBitmap, sx: Int, sy: Int, sw: Int, sh: Int,
    dx: Float, dy: Float, dw: Float, dh: Float
) = drawImage(
    image = img,
    srcOffset = IntOffset(sx, sy), srcSize = IntSize(sw, sh),
    dstOffset = IntOffset(dx.roundToInt(), dy.roundToInt()), dstSize = IntSize(dw.roundToInt(), dh.roundToInt()),
    filterQuality = FilterQuality.None
)

/** Kepala (lapisan dalam + topi) dalam lingkaran: ikon akun. */
@Composable
fun AccountAvatar(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val skin = remember { SkinSource.default(context) }
    Canvas(modifier.clip(CircleShape)) {
        part(skin, 8, 8, 8, 8, 0f, 0f, size.width, size.height)
        part(skin, 40, 8, 8, 8, 0f, 0f, size.width, size.height)
    }
}

/** Tubuh penuh tampak depan (16x32 piksel skin), digambar tajam dan ditaruh di tengah bawah ruangnya. */
@Composable
fun SkinFigure(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val skin = remember { SkinSource.default(context) }
    Canvas(modifier) {
        val s = floor(minOf(size.width / 16f, size.height / 32f)).coerceAtLeast(1f)
        val ox = (size.width - 16f * s) / 2f
        val oy = size.height - 32f * s
        fun p(sx: Int, sy: Int, sw: Int, sh: Int, x: Int, y: Int) =
            part(skin, sx, sy, sw, sh, ox + x * s, oy + y * s, sw * s, sh * s)
        // lapisan dalam
        p(4, 20, 4, 12, 4, 20); p(20, 52, 4, 12, 8, 20)      // kaki kanan, kiri
        p(44, 20, 4, 12, 0, 8); p(36, 52, 4, 12, 12, 8)      // lengan kanan, kiri
        p(20, 20, 8, 12, 4, 8); p(8, 8, 8, 8, 4, 0)          // badan, kepala
        // lapisan luar
        p(4, 36, 4, 12, 4, 20); p(4, 52, 4, 12, 8, 20)
        p(44, 36, 4, 12, 0, 8); p(52, 52, 4, 12, 12, 8)
        p(20, 36, 8, 12, 4, 8); p(40, 8, 8, 8, 4, 0)
    }
}
