package com.ya.composertwovideo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String    TAG = "MainActivity : ";
    private              Button    mButton;
    private              VideoView mVideoView;
    String composerVideoPath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mButton = (Button) findViewById(R.id.btn_compose);
        mVideoView = (VideoView) findViewById(R.id.main_video);
        mButton.setOnClickListener(this);
        configViewPath();
        initVideoView();
    }

    private void configViewPath() {
        Log.d(TAG, "configViewPath: ");
        File videoFile = new File(getFilesDir(), "/compose_video/");
        if (!videoFile.exists()) {
            boolean result = videoFile.mkdirs();
            Log.d(TAG, "    mkdirs status : " + result);
        }

        Log.d(TAG, "    videoFile status : " + videoFile.exists());
        composerVideoPath = new File(getFilesDir(), "/compose_video/compose_out.mp4").getAbsolutePath();
    }

    private void initVideoView() {
        mVideoView.setMediaController(new MediaController(this));
    }

    private void startCompose() {
        Log.d(TAG, "startCompose: ");
        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<String> videoList = new ArrayList<>();
                videoList.add(getFilesDir() + "/test_1.mp4");
                videoList.add(getFilesDir() + "/test_2.mp4");
                Log.i(TAG, "    videoList " + videoList);

                VideoComposer composer = new VideoComposer(videoList, composerVideoPath);
                final boolean result   = composer.startCompose();
                mButton.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "compose result is : " + result, Toast.LENGTH_LONG).show();
                        if (result) {
                            playVideo();
                        }
                    }
                });
                Log.i(TAG, "compose result: " + result);
            }
        }).start();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_compose) {
            startCompose();
        }
    }

    private void playVideo() {
        if (mVideoView.isPlaying()) {
            return;
        }
        if (new File(composerVideoPath).exists()) {
            mVideoView.setVideoPath(composerVideoPath);
            mVideoView.start();
        }
    }
}