package org.ligi.passandroid.ui.rendering

import org.ligi.passandroid.R
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.pass.BoardingTransitType
import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.reader.AppleStylePassReader

object BoardingTransitIcon {

    fun ensureTransitType(pass: Pass, passStore: PassStore): BoardingTransitType {
        if (!pass.isBoardingPass()) return BoardingTransitType.GENERIC
        if (pass.boardingTransitType != BoardingTransitType.GENERIC) return pass.boardingTransitType

        val parsedTransitType = AppleStylePassReader.readBoardingTransitType(passStore.getPathForID(pass.id))
        if (parsedTransitType != BoardingTransitType.GENERIC) {
            pass.boardingTransitType = parsedTransitType
        }
        return pass.boardingTransitType
    }

    fun drawableFor(transitType: BoardingTransitType): Int {
        return when (transitType) {
            BoardingTransitType.AIR -> R.drawable.ic_transit_air_24
            BoardingTransitType.BOAT -> R.drawable.ic_transit_boat_24
            BoardingTransitType.BUS -> R.drawable.ic_transit_bus_24
            BoardingTransitType.TRAIN -> R.drawable.ic_transit_train_24
            BoardingTransitType.GENERIC -> R.drawable.ic_transit_generic_24
        }
    }

    private fun Pass.isBoardingPass(): Boolean {
        return type == PassType.PKBOARDING || type == PassType.BOARDING
    }
}
