package com.chickenduy.locationApp.backgroundServices.communicationService.model.request.options

class StepsOptions(
    val date: Long) {

    override fun toString(): String {
        return "{\ndate: ${date}\n}"
    }
}