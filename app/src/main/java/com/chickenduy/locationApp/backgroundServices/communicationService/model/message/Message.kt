package com.chickenduy.locationApp.backgroundServices.communicationService.model.message


class Message<T, S>(
    var to: String,
    val time_to_live: Int,
    val data: Data<T, S>
) {
}