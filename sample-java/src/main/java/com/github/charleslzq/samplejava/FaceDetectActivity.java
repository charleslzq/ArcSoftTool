package com.github.charleslzq.samplejava;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.github.charleslzq.arcsofttools.kotlin.ArcSoftFaceEngineService;
import com.github.charleslzq.arcsofttools.kotlin.DefaultArcSoftEngineService;
import com.github.charleslzq.arcsofttools.kotlin.DetectedAge;
import com.github.charleslzq.arcsofttools.kotlin.DetectedGender;
import com.github.charleslzq.arcsofttools.kotlin.Face;
import com.github.charleslzq.arcsofttools.kotlin.Person;
import com.github.charleslzq.faceengine.support.FaceDetectView;
import com.github.charleslzq.facestore.ReadWriteFaceStore;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.fotoapparat.preview.Frame;
import kotlin.Pair;

public class FaceDetectActivity extends AppCompatActivity {
    public static final String TAG = "FaceDetectActivity";
    @BindView(R.id.faceDetectCamera)
    FaceDetectView faceDetectCamera;
    private ArcSoftFaceEngineService<ReadWriteFaceStore<Person, Face>> faceEngineService = null;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            faceEngineService = (ArcSoftFaceEngineService<ReadWriteFaceStore<Person, Face>>) iBinder;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            faceEngineService = null;
        }
    };
    private int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_detect);
        ButterKnife.bind(this);
        bindService(new Intent(this, DefaultArcSoftEngineService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        faceDetectCamera.onPreviewFrame(new FaceDetectView.FrameConsumer() {
            @Override
            public void accept(@NonNull Frame frame) {
                Log.i(TAG, "on frame with size " + frame.getSize().toString() + " and rotation " + frame.getRotation());
                if (faceEngineService != null) {
                    List<Face> detectedFaces = faceEngineService.detect(frame);
                    List<DetectedAge> detectedAges = faceEngineService.detectAge(frame);
                    List<DetectedGender> detectedGenders = faceEngineService.detectGender(frame);
                    StringBuilder messageBuilder = new StringBuilder();
                    String result = null;
                    if (detectedFaces.size() == 1) {
                        Pair<Person, Float> max = new Pair<>(new Person("", ""), 0f);
                        for (Face face : detectedFaces) {
                            Pair<Person, Float> matchResult = faceEngineService.search(face);
                            if (matchResult != null && matchResult.getSecond() > max.getSecond()) {
                                max = matchResult;
                            }
                        }
                        if (max.getSecond() > 0) {
                            result = max.getFirst().getName();
                            messageBuilder.append("Match Result ");
                            messageBuilder.append(result);
                        } else {
                            messageBuilder.append("No Match Result");
                        }
                    } else {
                        messageBuilder.append("No or too much (");
                        messageBuilder.append(detectedFaces.size());
                        messageBuilder.append(") Face(s) Detected");
                    }
                    messageBuilder.append(", ");
                    if (detectedAges.size() == 1 && detectedAges.get(0).getAge() > 0) {
                        messageBuilder.append("detected age ");
                        messageBuilder.append(detectedAges.get(0).getAge());
                    } else {
                        messageBuilder.append("fail to detect age");
                    }
                    messageBuilder.append(", ");
                    if (detectedGenders.size() == 1 && detectedGenders.get(0) != null && detectedGenders.get(0).getGender() != null) {
                        messageBuilder.append("detected gender ");
                        messageBuilder.append(detectedGenders.get(0).getGender());
                    } else {
                        messageBuilder.append("fail to detect gender");
                    }
                    messageBuilder.append(", ");
                    messageBuilder.append(++count);

                    toast(messageBuilder.toString());
                    if (result != null) {
                        Intent intent = new Intent();
                        intent.putExtra("personName", result);
                        setResult(Activity.RESULT_OK, intent);
                        finish();
                    }
                }
            }
        });
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        faceDetectCamera.start();
    }

    @Override
    protected void onPause() {
        faceDetectCamera.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        unbindService(serviceConnection);
        faceDetectCamera.stop();
        super.onDestroy();
    }
}
