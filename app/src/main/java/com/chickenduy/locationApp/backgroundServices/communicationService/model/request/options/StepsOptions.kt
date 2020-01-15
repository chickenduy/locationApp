package com.chickenduy.locationApp.backgroundServices.communicationService.model.request.options

class StepsOptions(
    val date: Long,
    val lat: Double,
    val lon: Double,
    val radius: Double
) {

    override fun toString(): String {
        return "{\ndate: ${date}\n}"
    }
}