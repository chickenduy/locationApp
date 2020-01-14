package com.chickenduy.locationApp.backgroundServices.communicationService.model.request.options

class LocationOptions(
    val date: Long,
    val accuracy: Int,
    val anonymity: Int){

    override fun toString(): String {
        return "{\nstart: ${date},\naccuracy: ${accuracy}\n}"
    }
}