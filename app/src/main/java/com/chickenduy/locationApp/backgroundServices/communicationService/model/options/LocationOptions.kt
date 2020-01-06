package com.chickenduy.locationApp.backgroundServices.communicationService.model.options

class LocationOptions(
    val timestamp: Long,
    val accuracy: Int){

    override fun toString(): String {
        return "{\nstart: ${timestamp},\naccuracy: ${accuracy}\n}"
    }
}