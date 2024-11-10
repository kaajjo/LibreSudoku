package com.kaajjo.libresudoku.app_update

import com.kaajjo.libresudoku.core.update.Version
import org.junit.Test

class VersionTests {
    @Test
    fun testVersionStableComparison() {
        val version1 = Version.Stable(2, 0, 0)
        val version2 = Version.Stable(2, 1, 0)

        assert(version1 < version2)
    }

    @Test
    fun testVersionBetaComparison() {
        val version1 = Version.Beta(2, 0, 0, 1)
        val version2 = Version.Beta(2, 0, 0, 2)

        assert(version1 < version2)
    }

    @Test
    fun testVersionStableBetaComparison() {
        val version1 = Version.Stable(2, 0, 0)
        val version2 = Version.Beta(2, 0, 0, 1)

        assert(version1 > version2)
    }

    @Test
    fun testMajorComparison() {
        val version1 = Version.Stable(2, 0, 0)
        val version2 = Version.Stable(3, 0, 0)

        assert(version1 < version2)
    }

    @Test
    fun testMinorComparison() {
        val version1 = Version.Stable(2, 0, 0)
        val version2 = Version.Stable(2, 1, 0)

        assert(version1 < version2)
    }

    @Test
    fun testPatchComparison() {
        val version1 = Version.Stable(2, 0, 0)
        val version2 = Version.Stable(2, 0, 1)

        assert(version1 < version2)
    }

    @Test
    fun testBuildComparison() {
        val version1 = Version.Beta(2, 0, 0, 1)
        val version2 = Version.Beta(2, 0, 0, 2)

        assert(version1 < version2)
    }

    @Test
    fun testVersionToString() {
        val version = Version.Stable(2, 0, 0)

        assert(version.toVersionString() == "2.0.0")
    }

    @Test
    fun testVersionToStringBeta() {
        val version = Version.Beta(2, 0, 0, 1)

        assert(version.toVersionString() == "2.0.0-beta1")
    }
}