package com.chickenduy.locationApp.backgroundServices.communicationService.model.message

import com.chickenduy.locationApp.backgroundServices.communicationService.model.request.RequestHeader
import com.chickenduy.locationApp.backgroundServices.communicationService.model.request.RequestOptions

class ServerMessage(
    val password: String,
    val requestHeader: RequestHeader,
    val requestOptions: RequestOptions,
    val requestData: Any,
    val data: Any
    ) {

}