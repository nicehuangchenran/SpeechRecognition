package com.hci.hci;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Calendar;

//
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = "MainActivity";

    private SpeechRecognizer mIat;// 语音听写对象
    private RecognizerDialog mIatDialog;// 语音听写UI

    // 用HashMap存储听写结果
    private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();

    private SharedPreferences mSharedPreferences;//缓存

    private String mEngineType = SpeechConstant.TYPE_CLOUD;// 引擎类型
    private String language = "en_us";//识别语言

    private TextView tvResult;//识别结果
    private Button btnStart;//开始识别
    private String resultType = "json";//结果内容数据格式

    private List<PackageInfo> clockPackageInfos;//系统时钟软件
    private static final int PERMISSION_REQUEST_CODE = 1001;


    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.d(TAG, "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showMsg("初始化失败，错误码：" + code + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
            }
        }
    };


    /**
     * 听写UI监听器
     */
    private RecognizerDialogListener mRecognizerDialogListener = new RecognizerDialogListener() {
        public void onResult(RecognizerResult results, boolean isLast) {

            printResult(results);//结果数据解析

        }

        /**
         * 识别回调错误.
         */
        public void onError(SpeechError error) {
            showMsg(error.getPlainDescription(true));
        }

    };

    /**
     * 提示消息
     * @param msg
     */
    private void showMsg(String msg) {
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onClick(View v) {
        if( null == mIat ){
            // 创建单例失败，与 21001 错误为同样原因，参考 http://bbs.xfyun.cn/forum.php?mod=viewthread&tid=9688
            showMsg( "创建对象失败，请确认 libmsc.so 放置正确，且有调用 createUtility 进行初始化" );
            return;
        }

        mIatResults.clear();//清除数据
        setParam(); // 设置参数
        mIatDialog.setListener(mRecognizerDialogListener);//设置监听
        mIatDialog.show();// 显示对话框
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (null != mIat) {
            // 退出时释放连接
            mIat.cancel();
            mIat.destroy();
        }
    }


    /**
     * 数据解析
     *
     * @param results
     */
    private void printResult(RecognizerResult results) {
        String text = JsonParser.parseIatResult(results.getResultString());

        String sn = null;
        // 读取json结果中的sn字段
        try {
            JSONObject resultJson = new JSONObject(results.getResultString());
            sn = resultJson.optString("sn");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mIatResults.put(sn, text);
        if(compare(text,"dial")){
            // 声明要拨打的电话号码
            String phoneNumber = "";
            // 创建一个Intent对象
            Intent intent = new Intent(Intent.ACTION_DIAL);
            // 设置URI
            intent.setData(Uri.parse("tel:" + phoneNumber));
            // 启动拨号界面
            startActivity(intent);
        }

        // 打开短信界面
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:"));
        startActivity(intent);

        //
        //Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        //startActivityForResult(intent, 1);
        
        if(true) {
            //获取当前时间
            Calendar calendar = Calendar.getInstance();

            // 设置闹钟时间为今天的19点
            calendar.set(Calendar.HOUR_OF_DAY, 19);
            calendar.set(Calendar.MINUTE, 0);

            // 如果闹钟时间早于当前时间，则将闹钟时间设为明天的19点
            if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

            // 检查是否已经授予闹钟权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SET_ALARM)
                    != PackageManager.PERMISSION_GRANTED) {
                // 如果没有授予权限，则申请权限
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.SET_ALARM},
                        PERMISSION_REQUEST_CODE);
            } else {
                // 如果已经授予权限，则调用系统自带的闹钟软件设置闹钟
                Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM)
                        .putExtra(AlarmClock.EXTRA_HOUR, calendar.get(Calendar.HOUR_OF_DAY))
                        .putExtra(AlarmClock.EXTRA_MINUTES, calendar.get(Calendar.MINUTE))
                        .putExtra(AlarmClock.EXTRA_SKIP_UI, true);
                startActivity(intent);
            }
        }

        //语音处理
        StringBuffer resultBuffer = new StringBuffer();
        for (String key : mIatResults.keySet()) {
            resultBuffer.append(mIatResults.get(key));
        }
        tvResult.setText(resultBuffer.toString());//听写结果显示

    }

    /* 相似性判断*/
    public boolean compare(String sn, String command) {
        if (sn.endsWith(".")){
            sn = sn.substring(0, sn.length() - 1);
        }
        int n = sn.length(), m = command.length();
        if (Math.abs(n - m) > 1) {
            return false;
        }
        int i = 0, j = 0;
        int count = 0;
        while (i < n && j < m) {
            char c1 = Character.toLowerCase(sn.charAt(i));
            char c2 = Character.toLowerCase(command.charAt(j));
            if (c1 != c2) {
                if (++count > 1) {
                    return false;
                }
                if (n > m) {
                    i++;
                } else if (n < m) {
                    j++;
                } else {
                    i++;
                    j++;
                }
            } else {
                i++;
                j++;
            }
        }
        if (i < n || j < m) {
            count++;
        }
        return true;
    }




    /**
     * 参数设置
     *
     * @return
     */
    public void setParam() {
        // 清空参数
        mIat.setParameter(SpeechConstant.PARAMS, null);
        // 设置听写引擎
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
        // 设置返回结果格式
        mIat.setParameter(SpeechConstant.RESULT_TYPE, resultType);

        if (language.equals("zh_cn")) {
            String lag = mSharedPreferences.getString("iat_language_preference",
                    "mandarin");
            Log.e(TAG, "language:" + language);// 设置语言
            mIat.setParameter(SpeechConstant.LANGUAGE, "en_us");
            // 设置语言区域
            mIat.setParameter(SpeechConstant.ACCENT, lag);
        } else {

            mIat.setParameter(SpeechConstant.LANGUAGE, language);
        }
        Log.e(TAG, "last language:" + mIat.getParameter(SpeechConstant.LANGUAGE));

        //此处用于设置dialog中不显示错误码信息
        //mIat.setParameter("view_tips_plain","false");

        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mIat.setParameter(SpeechConstant.VAD_BOS, mSharedPreferences.getString("iat_vadbos_preference", "4000"));

        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        mIat.setParameter(SpeechConstant.VAD_EOS, mSharedPreferences.getString("iat_vadeos_preference", "1000"));

        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT, mSharedPreferences.getString("iat_punc_preference", "1"));

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        mIat.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/iat.wav");
    }

    /**
     * android 6.0 以上需要动态申请权限
     */
    private void initPermission() {
        String permissions[] = {Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        ArrayList<String> toApplyList = new ArrayList<String>();

        for (String perm : permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
                toApplyList.add(perm);
            }
        }
        String tmpList[] = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()) {
            ActivityCompat.requestPermissions(this, toApplyList.toArray(tmpList), 123);
        }


    }

//    /**
//     * 权限申请回调，可以作进一步处理
//     *
//     * @param requestCode
//     * @param permissions
//     * @param grantResults
//     */
//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        // 此处为android 6.0以上动态授权的回调，用户自行实现。
//
//    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvResult = findViewById(R.id.tv_result);
        btnStart = findViewById(R.id.btn_start);
        btnStart.setOnClickListener(this);//实现点击监听
        initPermission();
        // 使用SpeechRecognizer对象，可根据回调消息自定义界面；
        mIat = SpeechRecognizer.createRecognizer(MainActivity.this, mInitListener);
        // 使用UI听写功能，请根据sdk文件目录下的notice.txt,放置布局文件和图片资源
        mIatDialog = new RecognizerDialog(MainActivity.this, mInitListener);
        mSharedPreferences = getSharedPreferences("ASR",
                Activity.MODE_PRIVATE);

    }


}