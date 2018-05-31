package com.github.charleslzq.face.baidu.sample;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;

import com.github.charleslzq.face.baidu.BaiduFaceEngine;
import com.github.charleslzq.face.baidu.BaiduFaceEngineService;
import com.github.charleslzq.face.baidu.BaiduFaceEngineServiceBackground;
import com.github.charleslzq.faceengine.view.CameraPreview;
import com.github.charleslzq.faceengine.view.FaceDetectView;

import org.jetbrains.annotations.NotNull;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FaceDetectActivity extends AppCompatActivity {
    @BindView(R.id.faceDetectCamera)
    FaceDetectView faceDetectCamera;
    private BaiduFaceEngineService faceEngineService = null;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            faceEngineService = (BaiduFaceEngineService) iBinder;
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
        faceDetectCamera.onPreview(new CameraPreview.FrameConsumer() {
            @Override
            public void accept(@NotNull CameraPreview.PreviewFrame previewFrame) {
                if (faceEngineService != null) {
                    BaiduFaceEngine.User user = faceEngineService.search(previewFrame);
                    if (user != null) {
                        Intent intent = new Intent();
                        intent.putExtra("groupId", user.getGroupId());
                        intent.putExtra("userId", user.getUserId());
                        if (user.getUserInfo() != null) {
                            intent.putExtra("userInfo", user.getUserInfo());
                        }
                        setResult(Activity.RESULT_OK, intent);
                        finish();
                    }
                }
            }
        });
        bindService(new Intent(this, BaiduFaceEngineServiceBackground.class), serviceConnection, Context.BIND_AUTO_CREATE);
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
        super.onDestroy();
    }
}
