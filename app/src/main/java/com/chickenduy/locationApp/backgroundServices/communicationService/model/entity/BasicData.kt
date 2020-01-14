package com.chickenduy.locationApp.backgroundServices.communicationService.model.entity

class BasicData(
    var n: Int,
    val raw: ArrayList<Any>) {

    fun addRaw(value: Any) {
        raw.add(value)
    }
}