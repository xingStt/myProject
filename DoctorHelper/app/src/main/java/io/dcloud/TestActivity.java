package io.dcloud;

import io.dcloud.common.DHInterface.AbsMgr;
import io.dcloud.common.DHInterface.IFeature;
import io.dcloud.common.DHInterface.IWebview;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import com.yinhai.doctorHelper.R;

public class TestActivity extends Activity implements IFeature{
	VideoView public_videoView; //定义控件
	public static String url;
    public static int curtime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setWindow();
        setContentView(R.layout.activity_aaa);
        //初始化控件
        public_videoView =(VideoView)findViewById(R.id.videoview);
        //字符串解析成Uri
        Uri uri = Uri.parse(url);     
        //给videoview设置播放资源
        public_videoView.setVideoURI(uri);
        //设置播放器控件
        public_videoView.setMediaController(new MediaController(this));
        //这里用相对布局包裹videoview 实现视频全屏播放 
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
        RelativeLayout.LayoutParams.MATCH_PARENT,
        RelativeLayout.LayoutParams.MATCH_PARENT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        public_videoView.setLayoutParams(layoutParams);
        //播放完成的回调事件
        public_videoView.setOnCompletionListener( new MyPlayerOnCompletionListener());
        System.out.println(curtime);
        public_videoView.seekTo(curtime*1000);
        public_videoView.start();
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
    
    class MyPlayerOnCompletionListener implements MediaPlayer.OnCompletionListener {
    @Override
    public void onCompletion(MediaPlayer mp) {
    	//播放完成后关闭当前活动
    	finish();
    }
  }


	@Override
	public void dispose(String arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public String execute(final IWebview pWebview, final String action, final String[] pArgs) {
		if("startPlay".equals(action))
        {
            Intent intent = new Intent(pWebview.getActivity(), TestActivity.class);
            url = pArgs[1];
            System.out.println(url);
            System.out.println(pArgs[2]);
            double curtimedb = Double.parseDouble(pArgs[2]);
            curtime =(int)curtimedb;
            pWebview.getActivity().startActivityForResult(intent, 2);
        }
		return null;
	}


	@Override
	public void init(AbsMgr arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}
}
