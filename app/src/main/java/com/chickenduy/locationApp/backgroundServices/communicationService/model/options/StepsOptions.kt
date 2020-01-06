package com.chickenduy.locationApp.backgroundServices.communicationService.model.options

class StepsOptions(
    val date: Long) {

    override fun toString(): String {
        return "{\ndate: ${date}\n}"
    }
}