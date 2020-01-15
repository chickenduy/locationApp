package com.chickenduy.locationApp.backgroundServices.communicationService.model.message

class PushyMessage(
    val to: String,
    val time_to_live: Int,
    val data: PushyData
)