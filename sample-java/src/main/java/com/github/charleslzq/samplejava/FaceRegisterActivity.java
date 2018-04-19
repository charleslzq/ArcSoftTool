package com.github.charleslzq.samplejava;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.github.charleslzq.faceengine.support.ImageUtils;
import com.github.charleslzq.faceengine.view.CameraPreview;
import com.github.charleslzq.faceengine.view.FaceDetectView;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FaceRegisterActivity extends AppCompatActivity {

    private final AtomicBoolean requireTakePicture = new AtomicBoolean(false);
    @BindView(R.id.faceRegisterCamera)
    FaceDetectView faceDetectView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_register);
        ButterKnife.bind(this);
        faceDetectView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requireTakePicture.compareAndSet(false, true);
            }
        });
        faceDetectView.onPreview(new CameraPreview.FrameConsumer() {
            @Override
            public void accept(@NotNull CameraPreview.PreviewFrame previewFrame) {
                if (requireTakePicture.compareAndSet(true, false)) {
                    Intent intent = new Intent();
                    intent.putExtra("picPath", BitmapFileHelper.save(ImageUtils.toBitmap(previewFrame)));
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                }
            }
        });
    }
}
