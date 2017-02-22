package com.google.android.gms.samples.vision.face.facetracker;

import android.app.Application;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;

/**
 * Created by umshaik on 2/22/17.
 */

public class FaceDetectionApplicationClass extends Application {
    private static FaceServiceClient sFaceServiceClient;

    public static FaceServiceClient getFaceServiceClient() {
        return sFaceServiceClient;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sFaceServiceClient = new FaceServiceRestClient(getString(R.string.subscription_key));
    }
}