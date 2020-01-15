package com.chickenduy.locationApp.backgroundServices.communicationService.model.request.options

class WalkOptions(
    val start: Long,
    val end: Long,
    val lat: Double,
    val long: Double,
    val radius: Double
) {

    override fun toString(): String {
        return "{\nstart: ${start},\nend: ${end}\n}"
    }
}