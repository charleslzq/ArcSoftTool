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
import android.widget.Toast;

import com.github.charleslzq.arcsofttools.kotlin.ArcSoftFaceEngineService;
import com.github.charleslzq.arcsofttools.kotlin.DetectedAge;
import com.github.charleslzq.arcsofttools.kotlin.DetectedGender;
import com.github.charleslzq.arcsofttools.kotlin.Face;
import com.github.charleslzq.arcsofttools.kotlin.Person;
import com.github.charleslzq.arcsofttools.kotlin.WebSocketArcSoftEngineService;
import com.github.charleslzq.faceengine.core.TrackedFace;
import com.github.charleslzq.faceengine.support.FutureSupport;
import com.github.charleslzq.faceengine.view.CameraPreview;
import com.github.charleslzq.faceengine.view.CameraPreviewOperator;
import com.github.charleslzq.faceengine.view.FaceDetectView;
import com.github.charleslzq.facestore.websocket.WebSocketCompositeFaceStore;
import com.orhanobut.logger.Logger;

import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import kotlin.Pair;

public class FaceDetectActivity extends AppCompatActivity {
    @BindView(R.id.faceDetectCamera)
    FaceDetectView faceDetectCamera;
    private ArcSoftFaceEngineService<WebSocketCompositeFaceStore<Person, Face>> faceEngineService = null;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            faceEngineService = (ArcSoftFaceEngineService<WebSocketCompositeFaceStore<Person, Face>>) iBinder;
            faceEngineService.getStore().refresh();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            faceEngineService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_detect);
        ButterKnife.bind(this);
        StringBuilder sb = new StringBuilder();
        for (CameraPreviewOperator cameraPreviewOperator : faceDetectCamera.getCameras()) {
            sb.append(cameraPreviewOperator.getId());
            sb.append(" ");
        }
        Logger.i("All Cameras: " + sb.toString());
        if (faceDetectCamera.getSelectedCamera() != null) {
            Logger.i("Use Camera: " + faceDetectCamera.getSelectedCamera().getId());
        }
        faceDetectCamera.onPreview(new FaceDetectView.FrameConsumer() {
            @Override
            public void accept(@NonNull CameraPreview.PreviewFrame frame) {
                Logger.i("on frame with size " + frame.getSize().toString() + " and rotation " + frame.getRotation() + ", " + frame.getSequence());
                if (faceEngineService != null) {
                    Map<TrackedFace, Face> detectedFaces = faceEngineService.detect(frame);
                    Logger.i("Detected Faces: " + detectedFaces.size());
                    faceDetectCamera.updateTrackFaces(detectedFaces.keySet());
                    List<DetectedAge> detectedAges = faceEngineService.detectAge(frame);
                    List<DetectedGender> detectedGenders = faceEngineService.detectGender(frame);
                    StringBuilder messageBuilder = new StringBuilder();
                    String result = null;
                    if (detectedFaces.size() == 1) {
                        Pair<Person, Float> max = new Pair<>(new Person("", ""), 0f);
                        for (Face face : detectedFaces.values()) {
                            Pair<Person, Float> matchResult = faceEngineService.search(face);
                            if (matchResult != null && matchResult.getSecond() > max.getSecond()) {
                                max = matchResult;
                            }
                        }
                        if (max.getSecond() > 0.5f) {
                            result = max.getFirst().getName();
                            messageBuilder.append("Match Result ");
                            messageBuilder.append(result);
                            messageBuilder.append(" score ");
                            messageBuilder.append(max.getSecond());
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
                    if (detectedGenders.size() == 1 && detectedGenders.get(0) != null) {
                        messageBuilder.append("detected gender ");
                        messageBuilder.append(detectedGenders.get(0).getGender());
                    } else {
                        messageBuilder.append("fail to detect gender");
                    }
                    messageBuilder.append(", ");
                    messageBuilder.append(frame.getSequence());

                    Logger.i("Check Result: " + messageBuilder.toString());
                    toast(messageBuilder.toString());
                    if (result != null) {
                        Intent intent = new Intent();
                        intent.putExtra("personName", result);
                        setResult(Activity.RESULT_OK, intent);
//                            finish();
                    }
                }
                Logger.i("Handle Completed for frame " + frame.getSequence());
            }
        });
        bindService(new Intent(this, WebSocketArcSoftEngineService.class), serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void toast(final String message) {
        faceDetectCamera.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(FaceDetectActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        faceDetectCamera.start();
    }

    @Override
    protected void onPause() {
        faceDetectCamera.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        unbindService(serviceConnection);
        faceDetectCamera.close();
        FutureSupport.getFaceEngineTaskExecutor().cancelTasks();
        super.onDestroy();
    }
}
