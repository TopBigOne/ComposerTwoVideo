package com.ya.composertwovideo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private              Button mButton;
    private static final String TAG = "MainActivity : ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mButton = (Button) findViewById(R.id.button);
        mButton.setOnClickListener(this);
    }


    private void startCompose() {
        Log.d(TAG, "startCompose: ");
        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<String> videoList = new ArrayList<>();

                //待合成的2个视频文件
                videoList.add(getFilesDir() + "/test_1.mp4");
                videoList.add(getFilesDir() + "/test_2.mp4");
                Log.i(TAG, "    videoList " + videoList);

                VideoComposer composer = new VideoComposer(videoList, getFilesDir()+"/compose_out.mp4");
                final boolean result   = composer.joinVideo();

                mButton.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "合成结果 " + result, Toast.LENGTH_LONG);
                    }
                });
                Log.i(TAG, "compose result: " + result);
            }
        }).start();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.button) {
            startCompose();
        }

    }
}