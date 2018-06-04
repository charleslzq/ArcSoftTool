package com.github.charleslzq.face.baidu.sample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.github.charleslzq.face.baidu.BaiduFaceEngine;
import com.github.charleslzq.face.baidu.BaiduFaceEngineServiceBackground;
import com.github.charleslzq.faceengine.support.ServiceConnectionProvider;
import com.github.charleslzq.faceengine.support.ServiceInvoker;
import com.github.charleslzq.faceengine.view.FaceDetectView;
import com.github.charleslzq.faceengine.view.FrameConsumer;
import com.github.charleslzq.faceengine.view.SourceAwarePreviewFrame;

import org.jetbrains.annotations.NotNull;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FaceDetectActivity extends AppCompatActivity {
    @BindView(R.id.faceDetectCamera)
    FaceDetectView faceDetectCamera;

    private ServiceConnectionProvider<BaiduFaceEngine> connection = BaiduFaceEngineServiceBackground.Companion.getBuilder().build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_detect);
        ButterKnife.bind(this);
        faceDetectCamera.onPreview(new FrameConsumer() {
            @Override
            public void accept(@NotNull final SourceAwarePreviewFrame previewFrame) {
                connection.whenConnected(new ServiceInvoker<BaiduFaceEngine>() {
                    @Override
                    public void invoke(BaiduFaceEngine service) {
                        BaiduFaceEngine.User user = service.search(previewFrame);
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
                });
            }
        });
        connection.bind(this, BaiduFaceEngineServiceBackground.class);
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
        unbindService(connection);
        faceDetectCamera.close();
        super.onDestroy();
    }
}
