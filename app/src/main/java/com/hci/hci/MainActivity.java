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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.*;

import java.io.IOException;
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
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.MemoryFile;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechEvent;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.msc.util.FileUtil;
import com.iflytek.cloud.msc.util.log.DebugLog;

import java.util.Vector;


public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = "MainActivity";
    private static int counter = 0;
    private SpeechRecognizer mIat;// 语音听写对象
    private RecognizerDialog mIatDialog;// 语音听写UI

    // 用HashMap存储听写结果
    private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();

    private SharedPreferences mSharedPreferences;//缓存

    private TextView textView;
    private String mEngineType = SpeechConstant.TYPE_CLOUD;// 引擎类型
    private String language = "zh_cn";//识别语言

    private LinearLayout linearLayout;
    private ScrollView tvResult;//识别结果
    private Button btnStart;//开始识别
    private String resultType = "json";//结果内容数据格式

    private List<PackageInfo> clockPackageInfos;//系统时钟软件
    private static final int PERMISSION_REQUEST_CODE = 1001;

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String API_KEY = "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";

    private OkHttpClient client = new OkHttpClient();



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
            if (!isLast){
                printResult(results);//结果数据解析


            }
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

        String content;
        mIatResults.put(sn, text);
        Read("");
        if(compare(text,"打电话")||compare(text,"打个电话")){
            // 声明要拨打的电话号码
            String phoneNumber = "";
            // 创建一个Intent对象
            Intent intent = new Intent(Intent.ACTION_DIAL);
            // 设置URI
            intent.setData(Uri.parse("tel:" + phoneNumber));
            // 启动拨号界面
            startActivity(intent);
        }
        else if(compare(text,"同济")||compare(text,"同济大学")){
            textView = new TextView(this);
            textView.setText(text);
            linearLayout.addView(textView);
            content="同心同德，寄人寄事";
            SpeechUtility.createUtility(MainActivity.this, SpeechConstant.APPID +"=56978643");
            // 初始化合成对象
            mTts = SpeechSynthesizer.createSynthesizer(MainActivity.this, mTtsInitListener);
            if (mTts == null) {
                MainActivity.this.showTip("创建对象失败，请确认 libmsc.so 放置正确，且有调用 createUtility 进行初始化");
                return;
            }
            //设置参数
            hcrsetParam();
            //开始合成播放
            int code = mTts.startSpeaking(content, mTtsListener);
            if (code != ErrorCode.SUCCESS) {
                showTip("语音合成失败,错误码: " + code);
            }
            //Read(content);
        }
        else if(compare(text,"你是谁")||compare(text,"介绍一下你自己")){
            textView = new TextView(this);
            textView.setText(text);
            linearLayout.addView(textView);
            content="我是在用户交互课程中被编写出来的，您的智能语音管家";
            //开始合成播放
            int code = mTts.startSpeaking(content, mTtsListener);
            if (code != ErrorCode.SUCCESS) {
                showTip("语音合成失败,错误码: " + code);
            }
        }
        else if(compare(text,"同济大学怎么样")||compare(text,"同济大学怎样")){
            textView = new TextView(this);
            textView.setText(text);
            linearLayout.addView(textView);
            content="同舟共济，胸怀天下";
            //开始合成播放
            int code = mTts.startSpeaking(content, mTtsListener);
            if (code != ErrorCode.SUCCESS) {
                showTip("语音合成失败,错误码: " + code);
            }
        }
        else if(compare(text,"用户交互技术是什么")||compare(text,"用户交互技术")||compare(text,"用户交互是什么")||compare(text,"什么是用户交互技术")){
            textView = new TextView(this);
            textView.setText(text);
            linearLayout.addView(textView);
            content="用户交互技术是指用于人机交互的各种技术，包括自然语言处理、语音识别、图像识别、手势识别等";
            //开始合成播放
            int code = mTts.startSpeaking(content, mTtsListener);
            if (code != ErrorCode.SUCCESS) {
                showTip("语音合成失败,错误码: " + code);
            }
        }
        else if(compare(text,"怎么学用户交互")||compare(text,"怎么学好用户交互")){
            textView = new TextView(this);
            textView.setText(text);
            linearLayout.addView(textView);
            content="可以在同济大学听沈莹老师讲课，学习知识";
            //开始合成播放
            int code = mTts.startSpeaking(content, mTtsListener);
            if (code != ErrorCode.SUCCESS) {
                showTip("语音合成失败,错误码: " + code);
            }
        }
        else if(compare(text,"打开短信")||compare(text,"短信")){
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setType("vnd.android-dir/mms-sms");
            startActivity(intent);
        }
        else{
            StringBuffer resultBuffer = new StringBuffer();
            for (String key : mIatResults.keySet()) {
                resultBuffer.append(mIatResults.get(key));
            }
            textView = new TextView(this);
            textView.setText(text);
            sendGptRequest(resultBuffer.toString());
            linearLayout.addView(textView);
        }

        // 打开短信界面
        //Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:"));
        //startActivity(intent);

        //
        //Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        //startActivityForResult(intent, 1);
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
            mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
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

    private void sendGptRequest(String prompt) {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        String json = "{"
                + "\"model\":\"gpt-3.5-turbo\","
                + "\"messages\":[{\"role\":\"user\", \"content\":\"" + prompt + "\"}],"
                + "\"max_tokens\":640,"
                + "\"temperature\":0.5"
                + "}";

        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String responseBody = response.body().string();
                                JSONObject jsonObject = new JSONObject(responseBody);
                                JSONObject messageObject = jsonObject.getJSONArray("choices").getJSONObject(0).getJSONObject("message");
                                String content = messageObject.getString("content");

                                //语音朗读
                                Read(content);
                                //

                                TextView gptResultView = new TextView(MainActivity.this);
                                gptResultView.setText(content);
                                linearLayout.addView(gptResultView);
                            } catch (IOException | JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        });
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
    private InitListener mTtsInitListener = new InitListener() {
        @Override
        public void onInit(int code) {
            Log.i(TAG, "InitListener init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败,错误码：" + code);
            } else {
                showTip("初始化成功");
            }
        }
    };
    private void showTip(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
    // 默认发音人
    private String voicer = "xiaoyan";
    // 引擎类型
    private String hcrmEngineType = SpeechConstant.TYPE_CLOUD;
    /**
     * 参数设置
     *
     * @return
     */
    private void hcrsetParam() {
        // 清空参数
        mTts.setParameter(SpeechConstant.PARAMS, null);
        // 根据合成引擎设置相应参数
        if (mEngineType.equals(SpeechConstant.TYPE_CLOUD)) {
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
            //支持实时音频返回，仅在synthesizeToUri条件下支持
            mTts.setParameter(SpeechConstant.TTS_DATA_NOTIFY, "1");
            // 设置在线合成发音人
            mTts.setParameter(SpeechConstant.VOICE_NAME, voicer);
        } else {
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
            mTts.setParameter(SpeechConstant.VOICE_NAME, "");
        }
        // 设置播放合成音频打断音乐播放，默认为true
        mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "false");
        // 设置音频保存路径，保存音频格式支持pcm、wav
        mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "pcm");
        mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, getExternalFilesDir(null) + "/msc/tts.pcm");
    }

    String text = "";
    /**
     * 合成回调监听。
     */
    private SynthesizerListener mTtsListener = new SynthesizerListener() {
        //开始播放
        @Override
        public void onSpeakBegin() {

        }
        //暂停播放
        @Override
        public void onSpeakPaused() {

        }
        //继续播放
        @Override
        public void onSpeakResumed() {

        }
        //合成进度
        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos, String info) {

        }
        //播放进度
        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) {

        }
        //播放完成
        @Override
        public void onCompleted(SpeechError error) {

        }
        //事件
        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {

        }
    };

    private SpeechSynthesizer mTts;

    public void Read(String str){
        //语言合成部分
        SpeechUtility.createUtility(MainActivity.this, SpeechConstant.APPID +"=56978643");
        // 初始化合成对象
        mTts = SpeechSynthesizer.createSynthesizer(MainActivity.this, mTtsInitListener);
        if (mTts == null) {
            MainActivity.this.showTip("创建对象失败，请确认 libmsc.so 放置正确，且有调用 createUtility 进行初始化");
            return;
        }
        //设置参数
        hcrsetParam();
        //开始合成播放
        int code = mTts.startSpeaking(str, mTtsListener);
        if (code != ErrorCode.SUCCESS) {
            showTip("语音合成失败,错误码: " + code);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btn_intro=findViewById(R.id.btn_intro);
        btn_intro.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent i=new Intent(MainActivity.this,IntroActivity.class);
                startActivity(i);

            }
        });

        Button btn_list=findViewById(R.id.btn_list);
        btn_list.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
               Toast.makeText(MainActivity.this,"小组成员：焦骜、黄晨冉、赵一婷",Toast.LENGTH_SHORT).show();
            }
        });

        linearLayout = (LinearLayout) findViewById(R.id.linear_layout);
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