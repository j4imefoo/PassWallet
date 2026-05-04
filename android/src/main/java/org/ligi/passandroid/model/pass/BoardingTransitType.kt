package org.ligi.passandroid.model.pass

private const val AIR_KEY = "PKTransitTypeAir"
private const val BOAT_KEY = "PKTransitTypeBoat"
private const val BUS_KEY = "PKTransitTypeBus"
private const val TRAIN_KEY = "PKTransitTypeTrain"
private const val GENERIC_KEY = "PKTransitTypeGeneric"

enum class BoardingTransitType(val passKitName: String) {
    AIR(AIR_KEY),
    BOAT(BOAT_KEY),
    BUS(BUS_KEY),
    TRAIN(TRAIN_KEY),
    GENERIC(GENERIC_KEY);

    companion object {
        fun fromPassKitName(name: String?): BoardingTransitType {
            return values().firstOrNull { it.passKitName == name } ?: GENERIC
        }
    }
}
