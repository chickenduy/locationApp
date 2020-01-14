package com.chickenduy.locationApp.backgroundServices.communicationService.model.request

class RequestHeader(
    val id: String,
    val start: Long,
    val end: Long,
    val type: String) {

    override fun toString(): String {
        return "{\nid: ${this.id},\nstart: ${start},\nend: ${end},\ntype: ${type}\n}"
    }
}