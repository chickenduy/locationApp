package com.chickenduy.locationApp.backgroundServices.communicationService.model.options

class WalkOptions(
    val start: Long,
    val end: Long) {

    override fun toString(): String {
        return "{\nstart: ${start},\nend: ${end}\n}"
    }
}