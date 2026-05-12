package com.medownloader.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiskSpaceTest {

    @Test
    fun `null free bytes is unknown`() {
        val result = evaluateDiskSpace(freeBytes = null, expectedBytes = 1_000L)
        assertEquals(DiskSpaceCheck.Unknown, result)
    }

    @Test
    fun `null expected bytes is unknown`() {
        val result = evaluateDiskSpace(freeBytes = 1_000L, expectedBytes = null)
        assertEquals(DiskSpaceCheck.Unknown, result)
    }

    @Test
    fun `zero expected bytes is unknown not insufficient`() {
        // size=0 happens when the server didn't return Content-Length; never block.
        val result = evaluateDiskSpace(freeBytes = 0L, expectedBytes = 0L)
        assertEquals(DiskSpaceCheck.Unknown, result)
    }

    @Test
    fun `free bytes greater than expected plus headroom is sufficient`() {
        val headroom = 10L
        val result = evaluateDiskSpace(freeBytes = 200L, expectedBytes = 100L, headroomBytes = headroom)
        assertTrue(result is DiskSpaceCheck.Sufficient)
        val s = result as DiskSpaceCheck.Sufficient
        assertEquals(200L, s.freeBytes)
        assertEquals(110L, s.requiredBytes)
    }

    @Test
    fun `free bytes exactly equals required is sufficient`() {
        val result = evaluateDiskSpace(freeBytes = 110L, expectedBytes = 100L, headroomBytes = 10L)
        assertTrue(result is DiskSpaceCheck.Sufficient)
    }

    @Test
    fun `free bytes less than required is insufficient`() {
        val result = evaluateDiskSpace(freeBytes = 109L, expectedBytes = 100L, headroomBytes = 10L)
        assertTrue(result is DiskSpaceCheck.Insufficient)
        val i = result as DiskSpaceCheck.Insufficient
        assertEquals(109L, i.freeBytes)
        assertEquals(110L, i.requiredBytes)
    }

    @Test
    fun `realistic 10 GB download on 6 GB free is insufficient`() {
        val gb: Long = 1024L * 1024L * 1024L
        val result = evaluateDiskSpace(
            freeBytes = 6L * gb,
            expectedBytes = 10L * gb,
            headroomBytes = 256L * 1024L * 1024L
        )
        assertTrue(result is DiskSpaceCheck.Insufficient)
    }

    @Test
    fun `realistic 100 MB download on 6 GB free is sufficient`() {
        val mb: Long = 1024L * 1024L
        val gb: Long = 1024L * mb
        val result = evaluateDiskSpace(
            freeBytes = 6L * gb,
            expectedBytes = 100L * mb,
            headroomBytes = 256L * mb
        )
        assertTrue(result is DiskSpaceCheck.Sufficient)
    }
}
