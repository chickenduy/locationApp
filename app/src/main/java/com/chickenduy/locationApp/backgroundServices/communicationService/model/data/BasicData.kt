package com.chickenduy.locationApp.backgroundServices.communicationService.model.data

class BasicData(
    var n: Int,
    val raw: ArrayList<Any>) {

    fun addRaw(value: Any) {
        raw.add(value)
    }
}