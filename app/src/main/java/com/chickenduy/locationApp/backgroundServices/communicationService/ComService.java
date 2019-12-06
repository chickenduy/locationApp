package com.chickenduy.locationApp.backgroundServices.communicationService;

import android.content.Context;
import android.util.Log;

import com.chickenduy.locationApp.MyApp;

import java.io.File;

import io.textile.ipfslite.BuildConfig;
import io.textile.ipfslite.Peer;

public class ComService {

    private String TAG = "COMSERVICE";
    private Peer litePeer;

    public ComService() {
        try {
            Context ctx = MyApp.Companion.getInstance();
            final File filesDir = ctx.getFilesDir();
            final String path = new File(filesDir, "ipfs-lite").getAbsolutePath();
            litePeer = new Peer(path, BuildConfig.DEBUG);
            litePeer.start();
            Log.d(TAG, litePeer.started().toString());
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

}
