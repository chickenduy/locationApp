package com.chickenduy.locationApp.backgroundServices.communicationService.model.message

class Data<T, S>(
    val encryptionKey: String,
    val iv: String,
    val requestHeader: RequestHeader,
    val requestOptions: RequestOptions,
    val requestData: T,
    val data: S
) {
}