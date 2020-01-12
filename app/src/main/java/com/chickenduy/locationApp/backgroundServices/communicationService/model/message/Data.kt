package com.chickenduy.locationApp.backgroundServices.communicationService.model.message

class Data(
    val encryptionKey: String,
    val iv: String,
    val requestHeader: RequestHeader,
    val requestOptions: RequestOptions,
    val requestData: Any?,
    val data: String
) {
}