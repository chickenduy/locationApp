package com.chickenduy.locationApp.backgroundServices.communicationService.model.entity

class Crowd(
    val id: String,
    val publicKey: String
) {

    override fun toString(): String {
        return "\n{\nid: ${this.id},\npublicKey: ${this.publicKey}\n}]"
    }
}