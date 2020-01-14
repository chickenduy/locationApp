package com.chickenduy.locationApp.backgroundServices.communicationService.model.request.options

class PresenceOptions(
    val start: Long,
    val end: Long,
    val long: Double,
    val lat: Double,
    val radius: Double) {

    override fun toString(): String {
        return "{\nstart: ${start},\nend: ${end},\nlong: ${long},\nlat: ${lat},\nradius: ${radius}\n}"
    }
}