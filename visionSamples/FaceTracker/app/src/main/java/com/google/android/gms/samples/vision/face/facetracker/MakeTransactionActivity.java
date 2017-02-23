package com.google.android.gms.samples.vision.face.facetracker;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.samples.vision.face.facetracker.ui.camera.CameraSourcePreview;
import com.google.android.gms.samples.vision.face.facetracker.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.contract.VerifyResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static com.google.android.gms.samples.vision.face.facetracker.R.id.preview;

public class MakeTransactionActivity extends AppCompatActivity {

    private static final String TAG = MakeTransactionActivity.class.getName();
    private static final int RC_HANDLE_GMS = 9001;
    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;
    private Uri mPhotoPath1;
    private Uri mPhotoPath2;
    private EditText mCreditCardCVVEditText;
    private CameraSource mCameraSource = null;
    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;
    private File externalStoragePublicDirectory;
    private File photo;
    private com.microsoft.projectoxford.face.contract.Face[] mFace2;
    private com.microsoft.projectoxford.face.contract.Face[] mFace1;
    private int mCount = 0;
    private EditText mCreditCardEditText;
    private ProgressDialog progressDialog;
    private ImageView mphoto1;
    private ImageView mphoto2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_make_transaction);
        setUpIds();
        getExtraDataFromIntent();
        checkPermission();

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");


    }

    private void startCameraSource() {

        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    private void checkPermission() {
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
        } else {
            requestCameraPermission();
        }
    }

    private void setUpIds() {
        mCreditCardEditText = (EditText) findViewById(R.id.creditcard_number);
        mCreditCardCVVEditText = (EditText) findViewById(R.id.creditcardcvv_number);
        mPreview = (CameraSourcePreview) findViewById(preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);
        mphoto1 = (ImageView) findViewById(R.id.photo1);
        mphoto2 = (ImageView) findViewById(R.id.photo2);
        mCreditCardEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count == 2) {
                    startCameraSource();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void getExtraDataFromIntent() {
        Intent intent = getIntent();
        mPhotoPath1 = Uri.parse(intent.getStringExtra(Intent.EXTRA_TEXT));
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                // this will run in the main thread
                Bitmap bitmap1 = HelperClass.loadSizeLimitedBitmapFromUri(mPhotoPath1, getContentResolver());
                if (bitmap1 != null) detect(bitmap1, 1);
                mphoto1.setImageBitmap(bitmap1);
            }
        });
    }

    private void createCameraSource() {

        Context context = getApplicationContext();
        FaceDetector detector = new FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();
        GraphicFaceTrackerFactory graphics = new GraphicFaceTrackerFactory();
        MultiProcessor<Face> googleProcess = new MultiProcessor.Builder<>(graphics).build();

        detector.setProcessor(googleProcess);
        // new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory())
        //        .build());

        if (!detector.isOperational()) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Log.w(TAG, "Face detector dependencies are not yet available.");
        }

        mCameraSource = new CameraSource.Builder(context, detector)
                .setRequestedPreviewSize(1200, 1200)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedFps(30.0f)
                .build();
    }

    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    public void submitButton(View view) {
        progressDialog.show();
        if (mCreditCardEditText.getText().toString().length() < 3) {
            mCreditCardEditText.setError("Cannot be empty");
            return;
        }

        if (TextUtils.isEmpty(mCreditCardCVVEditText.getText().toString())) {
            mCreditCardCVVEditText.setError("Cannot be null");
            return;
        }
        if (mCreditCardCVVEditText.getText().toString().length() >= 2) {

        }

    }

    private void detect(final Bitmap bitmap, final int index) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {  // Put the image into an input stream for detection.
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
                ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

                //Start a background task to detect faces in the image.
                new DetectionTask(index).execute(inputStream);

            }
        });
        //Set the status to show that detection starts.
        //setInfo("Detecting...");
    }

    private void setUiAfterDetection(com.microsoft.projectoxford.face.contract.Face[] result, int mIndex) {
        if (mIndex == 1) {
            mFace1 = result;
        } else if (mIndex == 2) {
            mFace2 = result;
            startVerification();
        }
    }

    private void startVerification() {
        new VerificationTask(mFace1[0].faceId, mFace2[0].faceId).execute();
    }

    // Background task of face detection.
    private class DetectionTask extends AsyncTask<InputStream, String, com.microsoft.projectoxford.face.contract.Face[]> {

        private final int mIndex;

        DetectionTask(int index) {
            mIndex = index;
        }

        @Override
        protected com.microsoft.projectoxford.face.contract.Face[] doInBackground(InputStream... params) {
            // Get an instance of face service client to detect faces in image.
            FaceServiceClient faceServiceClient = FaceDetectionApplicationClass.getFaceServiceClient();
            try {
                publishProgress("Detecting...");

                // Start detection.
                return faceServiceClient.detect(
                        params[0],  /* Input stream of image to detect */
                        true,       /* Whether to return face ID */
                        false,       /* Whether to return face landmarks */
                        /* Which face attributes to analyze, currently we support:
                           age,gender,headPose,smile,facialHair */
                        null);
            } catch (Exception e) {
                publishProgress(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            //progressDialog.show();
            //addLog("Request: Detecting in image" + mIndex);
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            //progressDialog.setMessage(progress[0]);
            //setInfo(progress[0]);
        }

        @Override
        protected void onPostExecute(com.microsoft.projectoxford.face.contract.Face[] result) {
            // Show the result on screen when detection is done.
            if (result != null) {
                setUiAfterDetection(result, mIndex);
            }
        }

    }

    private class VerificationTask extends AsyncTask<Void, String, VerifyResult> {
        // The IDs of two face to verify.
        private UUID mFaceId0;
        private UUID mFaceId1;

        VerificationTask(UUID faceId0, UUID faceId1) {
            mFaceId0 = faceId0;
            mFaceId1 = faceId1;
        }

        @Override
        protected VerifyResult doInBackground(Void... params) {
            // Get an instance of face service client to detect faces in image.
            FaceServiceClient faceServiceClient = FaceDetectionApplicationClass.getFaceServiceClient();
            try {
                publishProgress("Verifying...");

                // Start verification.
                return faceServiceClient.verify(
                        mFaceId0,      /* The first face ID to verify */
                        mFaceId1);     /* The second face ID to verify */
            } catch (Exception e) {
                publishProgress(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            // progressDialog.show();
            //addLog("Request: Verifying face " + mFaceId0 + " and face " + mFaceId1);
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            //progressDialog.setMessage(progress[0]);
            //setInfo(progress[0]);
        }

        @Override
        protected void onPostExecute(VerifyResult result) {
            progressDialog.dismiss();
            if (result != null) {
                Log.i(TAG, "Response: Success. Face " + mFaceId0 + " and face "
                        + mFaceId1 + (result.isIdentical ? " " : " don't ")
                        + "belong to the same person");

                TextView tv = (TextView) findViewById(R.id.text_output);
                tv.setText("Face1 " + " and Face2 "
                        + (result.isIdentical ? " " : " don't ")
                        + "belong to the same person");
                tv.append("\n" + "face matching Confidence is around  " + result.confidence);
            }

            // Show the result on screen when verification is done.
            // setUiAfterVerification(result);
        }
    }

    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        @Override
        public Tracker<Face> create(Face face) {
            return new GraphicFaceTracker(mGraphicOverlay);
        }
    }

    /**
     * Face tracker for each detected individual. This maintains a face graphic within the app's
     * associated face overlay.
     */
    private class GraphicFaceTracker extends Tracker<Face> {
        private GraphicOverlay mOverlay;
        private FaceGraphic mFaceGraphic;

        GraphicFaceTracker(GraphicOverlay overlay) {
            mOverlay = overlay;
            mFaceGraphic = new FaceGraphic(overlay);
        }

        /**
         * Start tracking the detected face instance within the face overlay.
         */
        @Override
        public void onNewItem(int faceId, Face item) {
            mFaceGraphic.setId(faceId);
        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
            mOverlay.add(mFaceGraphic);
            mFaceGraphic.updateFace(face);
            try {
                mCameraSource.takePicture(null, new CameraSource.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] bytes) {
                        new SaveImageTask().execute(bytes);
                        mPreview.stop();
                        mOverlay.postInvalidate();
                        mFaceGraphic.postInvalidate();
                        mGraphicOverlay.postInvalidate();
                        mCameraSource.release();

                    }

                });
            } catch (Exception e) {
                e.printStackTrace();
            }


        }

        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            mOverlay.remove(mFaceGraphic);
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        @Override
        public void onDone() {
            mOverlay.remove(mFaceGraphic);
        }
    }


    private class SaveImageTask extends AsyncTask<byte[], Void, Void> {

        @Override
        protected Void doInBackground(byte[]... data) {
            FileOutputStream outStream = null;

            // Write to SD Card
            try {
                externalStoragePublicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + "/FaceIdentificationPath");
                if (!externalStoragePublicDirectory.exists())
                    externalStoragePublicDirectory.mkdir();
                photo = new File(externalStoragePublicDirectory,
                        "JPEG_" + System.currentTimeMillis() + ".jpg");

                FileOutputStream fos = new FileOutputStream(photo.getPath());

                fos.write(data[0]);
                fos.close();
            } catch (java.io.IOException e) {
                Log.e("PictureDemo", "Exception in photoCallback", e);
            } finally {

            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Toast.makeText(MakeTransactionActivity.this, "Photo Preview Available @ " + photo.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            mPhotoPath2 = Uri.fromFile(photo);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Bitmap bitmap2 = HelperClass.loadSizeLimitedBitmapFromUri(mPhotoPath2, getContentResolver());
                    if (bitmap2 != null) detect(bitmap2, 2);
                    mphoto2.setImageBitmap(bitmap2);
                }
            });
        }
    }
}
