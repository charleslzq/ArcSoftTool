package com.github.charleslzq.samplejava;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.github.charleslzq.arcsofttools.kotlin.DefaultArcSoftEngineService;
import com.github.charleslzq.arcsofttools.kotlin.Face;
import com.github.charleslzq.arcsofttools.kotlin.Person;
import com.github.charleslzq.faceengine.core.FaceEngineService;
import com.github.charleslzq.facestore.ReadWriteFaceStore;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {
    private FaceEngineService<Person, Face, Float, ReadWriteFaceStore<Person, Face>> faceEngineService = null;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            faceEngineService = (FaceEngineService<Person, Face, Float, ReadWriteFaceStore<Person, Face>>) iBinder;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            faceEngineService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        bindService(new Intent(this, DefaultArcSoftEngineService.class), serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (RequestCodes.fromCode(requestCode)) {
            case FACE_CHECK:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String personName = data.getExtras().getString("personName");
                    toast("Found Person " + personName);
                } else {
                    toast("Fail to identify face");
                }
                break;
            case IMAGE_CAMERA:
                if (resultCode == Activity.RESULT_OK && data != null && faceEngineService != null) {
                    registerImage(data);
                }
                break;
        }
    }

    private void registerImage(Intent data) {
        List<Face> faceList = faceEngineService.detect((Bitmap) data.getExtras().get("data"));
        if (!faceList.isEmpty()) {
            String testPersonId = "test";
            faceEngineService.getStore().savePerson(new Person(testPersonId, "test_name"));
            for (Face face : faceList) {
                faceEngineService.getStore().saveFace(testPersonId, face);
            }
        }
    }

    @OnClick(R.id.captureImageButton)
    public void onCaptureImageButtonClicked() {
        startActivityForResult(
                new Intent(MediaStore.ACTION_IMAGE_CAPTURE),
                RequestCodes.IMAGE_CAMERA.getCode()
        );
    }

    @OnClick(R.id.checkFaceButton)
    public void onFaceCheckButtonClicked() {
        startActivityForResult(
                new Intent(this, FaceDetectActivity.class),
                RequestCodes.FACE_CHECK.getCode()
        );
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        unbindService(serviceConnection);
        super.onDestroy();
    }

    enum RequestCodes {
        IMAGE_CAMERA,
        FACE_CHECK;

        public static RequestCodes fromCode(int code) {
            return RequestCodes.values()[code - 1];
        }

        public int getCode() {
            return ordinal() + 1;
        }
    }
}
