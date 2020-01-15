package com.chickenduy.locationApp.backgroundServices.communicationService.model.request

import com.chickenduy.locationApp.backgroundServices.communicationService.model.entity.Crowd

class RequestOptions(
    val groupNumber: Int,
    val numberOfGroups: Int,
    var from: String,
    var group: ArrayList<Crowd>
) {

    override fun toString(): String {
        return "{\nfrom: ${from},\ngroup: ${group}\n}"
    }
}