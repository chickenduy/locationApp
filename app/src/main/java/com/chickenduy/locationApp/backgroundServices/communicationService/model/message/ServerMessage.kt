package com.chickenduy.locationApp.backgroundServices.communicationService.model.message

class ServerMessage(
    val password: String,
    val requestHeader: RequestHeader,
    val requestOptions: RequestOptions,
    val requestData: Any,
    val data: Any
    ) {

}