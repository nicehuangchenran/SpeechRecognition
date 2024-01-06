package com.hci.hci;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Application;
import android.os.Bundle;

import com.iflytek.cloud.SpeechUtility;

public class SpeechRecognition extends Application {

    @Override
    public void onCreate() {
        SpeechUtility.createUtility(SpeechRecognition.this, "appid=Your appid here");
        super.onCreate();
    }
}