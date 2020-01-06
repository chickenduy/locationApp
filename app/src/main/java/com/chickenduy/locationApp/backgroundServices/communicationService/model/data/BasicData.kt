package com.chickenduy.locationApp.backgroundServices.communicationService.model.data

class BasicData(
    val n: Int,
    val raw: ArrayList<Int>) {

    fun addRaw(value: Int) {
        raw.add(value)
    }
}