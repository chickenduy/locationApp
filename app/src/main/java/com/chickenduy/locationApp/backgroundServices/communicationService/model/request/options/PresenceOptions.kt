package com.chickenduy.locationApp.backgroundServices.communicationService.model.request.options

class PresenceOptions(
    val start: Long,
    val end: Long,
    val lat: Double,
    val lon: Double,
    val radius: Double
) {

    override fun toString(): String {
        return "{\nstart: ${start},\nend: ${end},\nlong: ${lon},\nlat: ${lat},\nradius: ${radius}\n}"
    }
}