package com.example.developerhaoz.glideutils;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {

    WeakReference mWeakReference = new WeakReference<Activity>(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final ImageView mIvPhoto = (ImageView) findViewById(R.id.main_iv_photo);
        Button mBtnLoadImage = (Button) findViewById(R.id.main_btn_load_image);
        CommonImageLoader.getInstance().addGlideRequests(this);

        mBtnLoadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String imageUrl = "http://ww3.sinaimg.cn/large/7a8aed7bgw1eswencfur6j20hq0qodhs.jpg";
                Activity activity = (Activity) mWeakReference.get();
                CommonImageLoader.getInstance().displayImage(activity.hashCode(),imageUrl, mIvPhoto);
            }
        });


    }
}
