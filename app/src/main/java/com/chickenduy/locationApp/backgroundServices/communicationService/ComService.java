package com.chickenduy.locationApp.backgroundServices.communicationService;

import android.content.Context;
import android.util.Log;

import java.io.File;
import io.textile.ipfslite.BuildConfig;
import io.textile.ipfslite.Peer;

public class ComService {

    private String TAG = "COMSERVICE";
    public Peer litePeer;

    public ComService(Context ctx) {
        initIPFS(ctx);
    }

    private void initIPFS(Context ctx) {
        try {
            final File filesDir = ctx.getFilesDir();
            final String path = new File(filesDir, "ipfslite").getAbsolutePath();
            litePeer = new Peer(path, BuildConfig.DEBUG);
            litePeer.start();
            Log.d(TAG, litePeer.started().toString());
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

}
