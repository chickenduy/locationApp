package com.chickenduy.locationApp.backgroundServices.communicationService

import android.util.Log
import io.textile.pb.Model
import io.textile.pb.QueryOuterClass
import io.textile.textile.BaseTextileEventListener
import io.textile.textile.Textile

class CommunicationReceiver: BaseTextileEventListener() {

    private val TAG = "COMRECEIVER"
    private lateinit var address: String

    override fun nodeStarted() {
        super.nodeStarted()
        address = Textile.instance().account.address()
        Log.d(TAG, "Node has started with the address: $address")
        Log.d(TAG, "Contacts:\n ${Textile.instance().contacts.list()}")

        searchForServer()
        searchForName("andrew")
    }

    override fun nodeFailedToStart(e: Exception) {
        Log.d(TAG, "Node has failed to start: $e")
    }

    override fun notificationReceived(notification: Model.Notification?) {
        super.notificationReceived(notification)
        Log.d(TAG, "notification: from ${notification?.actor} with fields ${notification?.allFields}")
    }

    override fun queryDone(queryId: String?) {
        super.queryDone(queryId)
        Log.d(TAG, "queryDone: $queryId")
    }

    override fun clientThreadQueryResult(queryId: String?, thread: Model.Thread?) {
        super.clientThreadQueryResult(queryId, thread)
        Log.d(TAG, "Found contact with the address: ${thread}")
    }

    override fun contactQueryResult(queryId: String?, contact: Model.Contact?) {
        super.contactQueryResult(queryId, contact)
        Log.d(TAG, "Found contacts")

        Log.d(TAG, "Found contact with the address: ${contact?.address}")
        //Textile.instance().contacts.add(contact)
    }

    private fun searchForServer() {
        Log.d(TAG, "Start searching for P7qcaXrziXHw7dqxvoMMhpcvMZ74zcV4PBNswUYgTJr4GgjP")
        val options = QueryOuterClass.QueryOptions.newBuilder()
            .setWait(10)
            .setLimit(1)
            .build()
        val query = QueryOuterClass.ContactQuery.newBuilder()
            .setAddress("P8FxdgZ1rWxaQ4DrmMBADuYTz4XGpQeThJYxfL2X4WN89hP8")
            .build()
        val searchQuery = Textile.instance().contacts.search(query, options)
    }


    private fun searchForName(name: String) {
        Log.d(TAG, "Start searching for $name")
        val options = QueryOuterClass.QueryOptions.newBuilder()
            .setWait(10)
            .setLimit(1)
            .build()
        val query = QueryOuterClass.ContactQuery.newBuilder()
            .setName(name)
            .build()
        val searchQuery = Textile.instance().contacts.search(query, options)
    }
}