package com.ys.gtcamera;

import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.Toast;
import androidx.fragment.app.FragmentActivity;

public class CameraActivity extends FragmentActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            CameraFragment fragment = (CameraFragment) getSupportFragmentManager().findFragmentById(R.id.camera_action_frag);
            if (fragment.isRecording()) {
                Toast.makeText(this, R.string.camare_recording, Toast.LENGTH_SHORT).show();
            } else {
                System.exit(0);
            }
        }
        return true;
    }

}
