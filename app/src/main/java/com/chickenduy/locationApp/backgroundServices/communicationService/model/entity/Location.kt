package com.chickenduy.locationApp.backgroundServices.communicationService.model.entity

class Location(
    val blCorner: Coordinates,
    val trCorner: Coordinates) {

    override fun toString(): String {
        return "blCorner: (${blCorner.long}|${blCorner.lat}), trCorner: (${trCorner.long}|${trCorner.lat})"
    }
}