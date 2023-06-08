package com.hci.hci;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Application;
import android.os.Bundle;

import com.iflytek.cloud.SpeechUtility;

public class SpeechRecognition extends Application {

    @Override
    public void onCreate() {
        SpeechUtility.createUtility(SpeechRecognition.this, "appid=56521e36");
        super.onCreate();
    }
}