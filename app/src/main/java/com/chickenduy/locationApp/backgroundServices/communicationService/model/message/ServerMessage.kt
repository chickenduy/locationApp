package com.chickenduy.locationApp.backgroundServices.communicationService.model.message

import com.chickenduy.locationApp.backgroundServices.communicationService.model.request.RequestHeader
import com.chickenduy.locationApp.backgroundServices.communicationService.model.request.RequestOptions

class ServerMessage<T>(
    val password: String,
    val requestHeader: RequestHeader,
    val requestOptions: RequestOptions,
    val requestData: T,
    val data: Any
)