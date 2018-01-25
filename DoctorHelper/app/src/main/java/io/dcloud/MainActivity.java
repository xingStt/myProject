package io.dcloud;

import io.dcloud.common.DHInterface.AbsMgr;
import io.dcloud.common.DHInterface.IFeature;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.common.util.JSUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.yinhai.doctorHelper.R;

import org.json.JSONArray;


public class MainActivity extends Activity implements IFeature,SurfaceHolder.Callback,MediaRecorder.OnInfoListener {
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Button btnStartStop;
    private boolean isRecording = false;//标记是否已经在录制
    private MediaRecorder mRecorder;//音视频录制类
    private Camera mCamera = null;//相机
    private Camera.Size mSize = null;//相机的尺寸
    private TextView textView;
    private int text = 0;
    private int mCameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK;//默认后置摄像头
    private static final SparseIntArray orientations = new SparseIntArray();//手机旋转对应的调整角度
    private android.os.Handler handler = new android.os.Handler();
    public static IWebview _pWebview;
    public static String _callBackId;
    public static String path;
    public static String posterPath;
    private static long curtime;
    public final static int REQUEST_READ_PHONE_STATE = 1;
    static {
        orientations.append(Surface.ROTATION_0, 90);
        orientations.append(Surface.ROTATION_90, 0);
        orientations.append(Surface.ROTATION_180, 270);
        orientations.append(Surface.ROTATION_270, 180);
    }

    String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO};
    List<String> mPermissionList = new ArrayList<>();

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            text++;
            String d =  timeToStr(text);
            textView.setText(d+"");
            handler.postDelayed(this,1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setWindow();
        setContentView(R.layout.activity_media_recorder);
        initViews();
        //注册home键、任务键点击事件的服务
        registerReceiver(mHomeKeyEventReceiver, new IntentFilter(
                Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        //注册短按电源键的服务(还有问题)
        final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mBatInfoReceiver, filter);

        for(int i=0;i<permissions.length;i++){
            if (ContextCompat.checkSelfPermission(this, permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                mPermissionList.add(permissions[i]);
            }
        }

        if (mPermissionList.isEmpty()) {//未授予的权限为空，表示都授予了
            initCamera();
        } else {//请求权限方法
            String[] permissions = mPermissionList.toArray(new String[mPermissionList.size()]);//将List转为数组
            ActivityCompat.requestPermissions(MainActivity.this, permissions, 1);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        int ii=0;
        switch (requestCode) {
            case 1:
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        ii++;
                    }
                }
                if(ii>0){
                    finish();
                   return;
                }else{
                    initCamera();
//                    mSurfaceHolder = mSurfaceView.getHolder();
                    if (mCamera == null) {
                        return;
                    }
                    try {
                        //设置显示
                        mCamera.setPreviewDisplay(mSurfaceView.getHolder());
                        mCamera.startPreview();
                    } catch (Exception e) {
                        e.printStackTrace();
                        releaseCamera();
                        finish();
                    }
                }
                break;
            default:
                break;
        }
    }

    private void setWindow() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);// 去掉标题栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);// 设置全屏
        // 设置竖屏显示
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        // 选择支持半透明模式,在有surfaceview的activity中使用。
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
    }

    private void initViews() {
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceview);
        btnStartStop = (Button) findViewById(R.id.btnStartStop);
        textView = (TextView)findViewById(R.id.text);
        btnStartStop.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!isRecording) {
                    startRecord();
                } else {
                    stopRecord();
                }
            }
        });
        SurfaceHolder holder = mSurfaceView.getHolder();// 取得holder
        holder.setFormat(PixelFormat.TRANSPARENT);
        holder.setKeepScreenOn(true);
        holder.addCallback(this); // holder加入回调接口
    }

    /**
     * 初始化相机
     */
    private void initCamera() {
        if (Camera.getNumberOfCameras() == 2) {
            mCamera = Camera.open(mCameraFacing);
        } else {
            mCamera = Camera.open();
        }

        CameraSizeComparator sizeComparator = new CameraSizeComparator();
        Camera.Parameters parameters = mCamera.getParameters();

        if (mSize == null) {
            List<Camera.Size> vSizeList = parameters.getSupportedPreviewSizes();
            Collections.sort(vSizeList, sizeComparator);

            for (int num = 0; num < vSizeList.size(); num++) {
                Camera.Size size = vSizeList.get(num);

                if (size.width >= 800 && size.height >= 480) {
                    this.mSize = size;
                    break;
                }
            }
            mSize = vSizeList.get(0);

            List<String> focusModesList = parameters.getSupportedFocusModes();

            //增加对聚焦模式的判断
            if (focusModesList.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            } else if (focusModesList.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
            mCamera.setParameters(parameters);
        }
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int orientation = orientations.get(rotation);
        mCamera.setDisplayOrientation(orientation);
        mCamera.startPreview();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //initCamera();
    }

    @Override
    public void onPause() {
        releaseCamera();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mHomeKeyEventReceiver);
        unregisterReceiver(mBatInfoReceiver);
    }



    /*
     * 重写onKeyUp的点击后退事件,点击后退的时候释放摄像头,不然下一次打开摄像头会出错
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (mCamera != null&&!isRecording) {
                    System.out.println("mCamera != null&&!isRecording");
                    mCamera.release();
                    mCamera = null;
                    finish();
                }
                if(isRecording){
                    stopRecord();
                }
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }


    /**
     * 点击home键和任务键的时候释放摄像头
     */
    private BroadcastReceiver mHomeKeyEventReceiver = new BroadcastReceiver() {
        String SYSTEM_REASON = "reason";
        String SYSTEM_HOME_KEY = "homekey";
        String SYSTEM_RECENT_APPS = "recentapps";
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                String reason = intent.getStringExtra(SYSTEM_REASON);
                if (TextUtils.equals(reason, SYSTEM_HOME_KEY)) {
                    releaseCameraAndRecord();
                }else if(TextUtils.equals(reason, SYSTEM_RECENT_APPS)){
                    releaseCameraAndRecord();
                }
            }
        }
    };
    private final BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            if(Intent.ACTION_SCREEN_OFF.equals(action)) {
                releaseCameraAndRecord();
            }
        }
    };

    public void releaseCameraAndRecord(){
        if (mCamera != null&&!isRecording) {
            System.out.println("mCamera != null&&!isRecording");
            mCamera.release();
            mCamera = null;
        }
        if(isRecording){
            stopRecord();
        }
    }

    /**
     * 开始录制
     */
    private void startRecord() {
        handler.postDelayed(runnable,1000);
        if (mRecorder == null) {
            mRecorder = new MediaRecorder(); // 创建MediaRecorder
        }
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.unlock();
            mRecorder.setCamera(mCamera);
            mRecorder.setOrientationHint(90);
        }
        mRecorder.setOnInfoListener(this);
        try {
            // 设置音频采集方式
            mRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            //设置视频的采集方式
            mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

            //设置文件的输出格式
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);//aac_adif， aac_adts， output_format_rtp_avp， output_format_mpeg2ts ，webm
            //设置audio的编码格式
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            //设置video的编码格式
            mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            //设置录制的视频编码比特率
            mRecorder.setVideoEncodingBitRate(1024 * 1024*10);
            //设置录制的视频帧率,注意文档的说明:
            mRecorder.setVideoFrameRate(4);
            //设置要捕获的视频的宽度和高度
            mSurfaceHolder.setFixedSize(1280 , 720);
            mRecorder.setVideoSize(1280, 720);
            mRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());


//            CamcorderProfile profile;
//            if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_720P)) {
//                profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
//            } else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_480P)) {
//                profile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
//            } else {
//                profile = CamcorderProfile.get(CamcorderProfile.QUALITY_LOW);
//            }
//            mRecorder.setProfile(profile);
            //设置录制视频的大小（毫秒）
            mRecorder.setMaxFileSize(50000000);
            //设置记录会话的最大持续时间（毫秒）
            mRecorder.setMaxDuration(300 * 1000);
            path = getExternalFilesDir(null).getAbsolutePath();
            int index = path.lastIndexOf("/");
            path = path.substring(0, index)+"/apps/doctorHelper/doc/video";
            if (path != null) {
                File dir = new File(path);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                curtime= System.currentTimeMillis();
                path = dir + "/" + curtime + ".mp4";
                //设置输出文件的路径
                mRecorder.setOutputFile(path);
                //准备录制
                mRecorder.prepare();
                //开始录制
                mRecorder.start();
                isRecording = true;
                btnStartStop.setText("停止");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * 停止录制
     */
    private void stopRecord() {
        try {
            handler.removeCallbacks(runnable);
            //停止录制
            mRecorder.stop();
            //重置
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
            btnStartStop.setText("开始");
            if (mCamera != null) {
                mCamera.release();
                mCamera = null;
            }

            MediaMetadataRetriever media = new MediaMetadataRetriever();
            media.setDataSource(path);
            Bitmap bitmap = media.getFrameAtTime();
            String poster =  saveBitmapToSDCard(bitmap);
            String pathS = path+"@"+poster;
            //关闭当前activity
            finish();
            //返回视频路径
            JSUtil.execCallback(_pWebview, _callBackId, pathS, JSUtil.OK, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        isRecording = false;
    }

    public String saveBitmapToSDCard(Bitmap bitmap) {
        posterPath = getExternalFilesDir(null).getAbsolutePath();
        int index = posterPath.lastIndexOf("/");
        posterPath = posterPath.substring(0, index)+"/apps/doctorHelper/doc/video/"+curtime+ "$poster.bmp";
        File f = new File(posterPath);
        try {
            f.createNewFile();
        } catch (IOException e) {
            System.out.println("在保存图片时出错：" + e.toString());
        }
        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(f);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
        } catch (Exception e) {
            return "create_bitmap_error";
        }
        try {
            fOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return posterPath;
    }

    /**
     * 释放MediaRecorder
     */
    private void releaseMediaRecorder() {
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }
    }

    /**
     * 释放相机资源
     */
    private void releaseCamera() {
        try {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
                mCamera.unlock();
                mCamera.release();
            }
        } catch (RuntimeException e) {
        } finally {
            mCamera = null;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        // 将holder，这个holder为开始在onCreate里面取得的holder，将它赋给mSurfaceHolder
        mSurfaceHolder = holder;
        if (mCamera == null) {
            return;
        }
        try {
            //设置显示
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
            releaseCamera();
            finish();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // 将holder，这个holder为开始在onCreate里面取得的holder，将它赋给mSurfaceHolder
        mSurfaceHolder = holder;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // surfaceDestroyed的时候同时对象设置为null
        if (isRecording && mCamera != null) {
            mCamera.lock();
        }
        mSurfaceView = null;
        mSurfaceHolder = null;
        releaseMediaRecorder();
        releaseCamera();
    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        if(what==MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED){
            stopRecord();
        }
        if(what==MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED){
            stopRecord();
        }
    }

    private class CameraSizeComparator implements Comparator<Camera.Size> {
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            if (lhs.width == rhs.width) {
                return 0;
            } else if (lhs.width > rhs.width) {
                return 1;
            } else {
                return -1;
            }
        }
    }

    public String timeToStr(int time){
        if(time==0) {
            return "00:00";
        }
        int m = (time % 3600) / 60;
        int s = time % 60;
        return(ultZeroize(m) + ":" + ultZeroize(s));
    };
    public String ultZeroize(int v){
        String z = "";
        String d = String.valueOf(v);
        for(int i = 0; i < 2 - d.length(); i++) {
            z += "0";
        }
        return z + d;
    }

    @Override
    public void dispose(String arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public String execute(final IWebview pWebview, final String action, final String[] pArgs) {
        if("startRecorder".equals(action))
        {
            Intent intent = new Intent(pWebview.getActivity(), MainActivity.class);
            _pWebview = pWebview;
            _callBackId = pArgs[0];
            System.out.println(_pWebview+"||||"+_callBackId);
            pWebview.getActivity().startActivityForResult(intent, 1);
        }
        return null;
    }

    @Override
    public void init(AbsMgr arg0, String arg1) {
        // TODO Auto-generated method stub

    }
}