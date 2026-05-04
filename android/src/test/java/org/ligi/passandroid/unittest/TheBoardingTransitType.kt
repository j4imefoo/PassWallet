package org.ligi.passandroid.unittest

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.pass.BoardingTransitType
import org.ligi.passandroid.reader.AppleStylePassReader
import java.io.File
import java.nio.file.Files

class TheBoardingTransitType {

    @Test
    fun mapsAppleTransitTypes() {
        assertThat(BoardingTransitType.fromPassKitName("PKTransitTypeAir")).isEqualTo(BoardingTransitType.AIR)
        assertThat(BoardingTransitType.fromPassKitName("PKTransitTypeBoat")).isEqualTo(BoardingTransitType.BOAT)
        assertThat(BoardingTransitType.fromPassKitName("PKTransitTypeBus")).isEqualTo(BoardingTransitType.BUS)
        assertThat(BoardingTransitType.fromPassKitName("PKTransitTypeTrain")).isEqualTo(BoardingTransitType.TRAIN)
        assertThat(BoardingTransitType.fromPassKitName("PKTransitTypeGeneric")).isEqualTo(BoardingTransitType.GENERIC)
    }

    @Test
    fun defaultsUnknownTransitTypesToGeneric() {
        assertThat(BoardingTransitType.fromPassKitName(null)).isEqualTo(BoardingTransitType.GENERIC)
        assertThat(BoardingTransitType.fromPassKitName("wat")).isEqualTo(BoardingTransitType.GENERIC)
    }

    @Test
    fun readsTransitTypeFromPassJson() {
        val passDir = Files.createTempDirectory("passwallet-transit-test").toFile()
        try {
            File(passDir, "pass.json").writeText("{\"boardingPass\":{\"transitType\":\"PKTransitTypeTrain\"}}")

            assertThat(AppleStylePassReader.readBoardingTransitType(passDir)).isEqualTo(BoardingTransitType.TRAIN)
        } finally {
            passDir.deleteRecursively()
        }
    }
}
