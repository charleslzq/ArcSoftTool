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
import com.github.charleslzq.arcsofttools.kotlin.DetectedAge;
import com.github.charleslzq.arcsofttools.kotlin.DetectedGender;
import com.github.charleslzq.arcsofttools.kotlin.Face;
import com.github.charleslzq.arcsofttools.kotlin.Person;
import com.github.charleslzq.arcsofttools.kotlin.WebSocketArcSoftEngineService;
import com.github.charleslzq.faceengine.core.TrackedFace;
import com.github.charleslzq.faceengine.view.CameraPreview;
import com.github.charleslzq.faceengine.view.FaceDetectView;
import com.github.charleslzq.facestore.websocket.WebSocketCompositeFaceStore;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.schedulers.Schedulers;
import kotlin.Pair;

public class FaceDetectActivity extends AppCompatActivity {
    public static final String TAG = "FaceDetectActivity";
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
    private int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_detect);
        ButterKnife.bind(this);
        bindService(new Intent(this, WebSocketArcSoftEngineService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        faceDetectCamera.onPreview(2000, TimeUnit.MILLISECONDS, Schedulers.computation(), new FaceDetectView.FrameConsumer() {
            @Override
            public void accept(@NonNull CameraPreview.PreviewFrame frame) {
                Log.i(TAG, "on frame with size " + frame.getSize().toString() + " and rotation " + frame.getRotation());
                if (faceEngineService != null) {
                    List<TrackedFace> trackedFaces = faceEngineService.trackFace(frame);
                    if (trackedFaces.size() == 1) {
                        Map<TrackedFace, Face> detectedFaces = faceEngineService.detect(frame);
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
                        if (detectedGenders.size() == 1 && detectedGenders.get(0) != null && detectedGenders.get(0).getGender() != null) {
                            messageBuilder.append("detected gender ");
                            messageBuilder.append(detectedGenders.get(0).getGender());
                        } else {
                            messageBuilder.append("fail to detect gender");
                        }
                        messageBuilder.append(", ");
                        messageBuilder.append(++count);

                        Log.i("Check Result", messageBuilder.toString());
                        toast(messageBuilder.toString());
                        if (result != null) {
                            Intent intent = new Intent();
                            intent.putExtra("personName", result);
                            setResult(Activity.RESULT_OK, intent);
//                            finish();
                        }
                    }
                } else {
                    toast("Too much faces!");
                }
            }
        });
    }

    private void toast(final String message) {
        runOnUiThread(new Runnable() {
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
        faceDetectCamera.pause();
        super.onDestroy();
    }
}
