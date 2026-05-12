package com.medownloader.util

/**
 * Reports how many bytes are available on the filesystem backing a given path.
 *
 * Pure interface so business logic (repository, viewmodel) can be unit-tested
 * without the Android framework. The Android implementation lives in `:app`
 * under `AndroidDiskSpaceProbe`.
 */
interface DiskSpaceProbe {
    /**
     * Returns the number of bytes available to the calling app on the filesystem
     * backing [path]. Returns `null` if the filesystem cannot be inspected.
     */
    fun availableBytes(path: String): Long?
}

/**
 * Result of comparing an expected download size to available disk space.
 *
 *  - [Unknown]        size or free space could not be determined. Caller should allow
 *                     the download to proceed (we do not block downloads on uncertainty).
 *  - [Sufficient]     there is enough room with headroom.
 *  - [Insufficient]   there is not enough room; download would fail mid-flight.
 */
sealed class DiskSpaceCheck {
    data object Unknown : DiskSpaceCheck()
    data class Sufficient(val freeBytes: Long, val requiredBytes: Long) : DiskSpaceCheck()
    data class Insufficient(val freeBytes: Long, val requiredBytes: Long) : DiskSpaceCheck()
}

/**
 * Compare [expectedBytes] against [freeBytes] with a safety [headroomBytes].
 * Separated from [DiskSpaceProbe] so the comparison logic is a pure function.
 */
fun evaluateDiskSpace(
    freeBytes: Long?,
    expectedBytes: Long?,
    headroomBytes: Long = DEFAULT_DISK_HEADROOM_BYTES
): DiskSpaceCheck {
    if (freeBytes == null || expectedBytes == null || expectedBytes <= 0L) {
        return DiskSpaceCheck.Unknown
    }
    val required = expectedBytes + headroomBytes
    return if (freeBytes >= required) {
        DiskSpaceCheck.Sufficient(freeBytes, required)
    } else {
        DiskSpaceCheck.Insufficient(freeBytes, required)
    }
}

/** 256 MiB of headroom; leaves room for aria2's `.aria2` control file and other app data. */
const val DEFAULT_DISK_HEADROOM_BYTES: Long = 256L * 1024L * 1024L
