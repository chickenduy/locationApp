package com.chickenduy.locationApp.backgroundServices.communicationService.model.request.options

class LocationOptions(
    val date: Long,
    val accuracy: Int,
    val anonymity: Int,
    val lat: Double,
    val lon: Double,
    val radius: Double
) {

    override fun toString(): String {
        return "{\nstart: ${date},\naccuracy: ${accuracy}\n}"
    }
}