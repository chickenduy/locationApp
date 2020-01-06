package com.chickenduy.locationApp.backgroundServices.communicationService.model.options

import com.chickenduy.locationApp.backgroundServices.communicationService.model.entity.Crowd

class RequestOptions(
    var from: String,
    var group: ArrayList<Crowd>) {

    override fun toString(): String {
        return "{\nfrom: ${from},\ngroup: ${group}\n}"
    }
}