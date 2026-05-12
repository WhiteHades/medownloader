package com.medownloader.util

import android.os.Environment
import android.os.StatFs
import java.io.File

/**
 * Android-backed [DiskSpaceProbe] using [StatFs].
 *
 * The pure interface and evaluator live in the `:core` module so they can be
 * unit-tested without the Android framework.
 */
object AndroidDiskSpaceProbe : DiskSpaceProbe {
    override fun availableBytes(path: String): Long? {
        val target = File(path).takeIf { it.exists() }
            ?: File(path).parentFile?.takeIf { it.exists() }
            ?: Environment.getExternalStorageDirectory()
        return try {
            val stat = StatFs(target.absolutePath)
            stat.availableBytes
        } catch (_: Exception) {
            null
        }
    }
}
