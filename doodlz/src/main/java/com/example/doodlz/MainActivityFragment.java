package com.example.doodlz;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

/**
 * A placeholder fragment containing the DoodleView
 */
public class MainActivityFragment extends Fragment {
    private DoodleView doodleView; // handles touch events and draws

    // accelerometer information, calculate changes in the device’s acceleration to
    // determine when a shake event occurs
    private float acceleration;
    private float currentAcceleration;
    private float lastAcceleration;

    // use this to prevent multiple dialogs from being displayed simultaneously
    // for example, if the Choose Color dialog is displayed and the user accidentally
    // shakes the device, the dialog for erasing the image should not be displayed.
    private boolean dialogOnScreen = false;

    // value used to determine whether user shook the device to erase
    // used to ensure that small device movements (which happen frequently) are not
    // interpreted as shakes -- we picked this constant via trial and error by shaking
    // the app on several different types of devices.
    private static final int ACCELERATION_THRESHOLD = 100000;

    // used to identify the request for using external storage, which the save image feature needs
    private static final int SAVE_IMAGE_PERMISSION_REQUEST_CODE = 1;


    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_main, container, false);

        setHasOptionsMenu(true); // this fragment has menu items to display

        doodleView = (DoodleView) view.findViewById(R.id.doodleView);

        // initialize acceleration values that help calculate acceleration changes
        // to determine whether the user shook the device
        acceleration = 0.00f;
        currentAcceleration = SensorManager.GRAVITY_EARTH;
        lastAcceleration = SensorManager.GRAVITY_EARTH;

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        disableAccelerometerListening(); // stop listening for shake
    }

    @Override
    public void onResume() {
        super.onResume();
        enableAccelerometerListening(); // listen for shake event
    }

    // enable listening for accelerometer events
    private void enableAccelerometerListening() {
        SensorManager sensorManager = (SensorManager)
                getActivity().getSystemService(Context.SENSOR_SERVICE);
        // register to listen for accelerometer events
        sensorManager.registerListener(sensorEventListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    // disable listening for accelerometer events
    private void disableAccelerometerListening() {
        SensorManager sensorManager = (SensorManager)
                getActivity().getSystemService(Context.SENSOR_SERVICE);
        // stop listening for accelerometer events
        sensorManager.unregisterListener(sensorEventListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
    }

    // event handler for accelerometer events
    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            // ensure that other dialogs are not displayed
            if (!dialogOnScreen) {
                // get x, y, and z values for the SensorEvent
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                // save previous acceleration value
                lastAcceleration = currentAcceleration;

                // calculate the current acceleration
                currentAcceleration = x * x + y * y + z * z;

                // calculate the change in acceleration
                acceleration = currentAcceleration * (currentAcceleration - lastAcceleration);

                // if the acceleration is above a certain threshold
                if (acceleration > ACCELERATION_THRESHOLD)
                    confirmErase();
            }

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    // confirm whether image should be erased
    private void confirmErase() {
        EraseImageDialogFragment fragment = new EraseImageDialogFragment();
        fragment.show(getFragmentManager(), "erase dialog");
    }

    public DoodleView getDoodleView() {
        return doodleView;
    }

    // indicates whether a dialog is displayed
    public void setDialogOnScreen(boolean visible) {
        dialogOnScreen = visible;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.doodle_fragment_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.color:
                ColorDialogFragment colorDialog = new ColorDialogFragment();
                colorDialog.show(getFragmentManager(), "color dialog");
                return true; // consume the menu event
            case R.id.line_width:
                LineWidthDialogFragment widthDialog = new LineWidthDialogFragment();
                widthDialog.show(getFragmentManager(), "line width dialog");
                return true;
            case R.id.delete_drawing:
                confirmErase(); // confirm before erasing image
                return true;
            case R.id.save:
                saveImage(); // check permission and save current image
                return true;
            case R.id.print:
                doodleView.printImage(); // print the current images
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // requests the permission needed for saving the image if necessary or
    // saves the image if the app already has permission
    private void saveImage() {
        // checks if the app does not have permission needed to save the image
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (getContext().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED) {
                // shows an explanation of why permission is needed
                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setMessage(R.string.permission_explanation);
                    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermissions(new String[]{
                                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    SAVE_IMAGE_PERMISSION_REQUEST_CODE);
                        }
                    });
                    builder.create().show();
                } else {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            SAVE_IMAGE_PERMISSION_REQUEST_CODE);
                }
            } else { // if app already has permission to write to external storage
                doodleView.saveImage(); // save the image
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case SAVE_IMAGE_PERMISSION_REQUEST_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    doodleView.saveImage(); // save the image
                }
                return;
        }
    }
}
