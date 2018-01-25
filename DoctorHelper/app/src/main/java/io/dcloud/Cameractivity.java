package io.dcloud;



import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.yinhai.doctorHelper.R;

import io.dcloud.common.DHInterface.AbsMgr;
import io.dcloud.common.DHInterface.IFeature;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.util.JSUtil;


public class Cameractivity extends Activity implements IFeature {
	public static final int TAKE_PHOTO = 1;
	private ImageView picture;
	private Uri imageUri;
	private String sourpath;
	public static IWebview _pWebview;
	public static String _callBackId;

	ImageView imageView;
	Button saveBtn;
	double pictureRelativeLeft, pictureRelativeTop, pictureRelativeRight,
			pictureRelativeButtom;
	double imageViewLeft, imageViewTop, imageViewRight, imageViewButtom;
	double pictureRealLeft, pictureRealTop, pictureRealRight,
			pictureRealButtom;
	Bitmap bitmap;
	double proportionWidth, proportionHeight;
	double bitmapWidth, bitmapHeight;
	Canvas canvas;
	Path path;
	double preX, preY;
	Paint paint;
	boolean hasOut=false;
	private static long curtime;

	
	@Override 
    public void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState);
				sourpath = getExternalCacheDir()+"output_img.jpg";
				File outputImage = new File(sourpath);
				try {
					if(outputImage.exists()){
						outputImage.delete();
					}
					outputImage.createNewFile();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				imageUri = Uri.fromFile(outputImage);
				Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
				intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
				startActivityForResult(intent, TAKE_PHOTO);
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case TAKE_PHOTO:
			if(resultCode==RESULT_OK){
					initScraw();
			}else{
				finish();
			}
			break;
		default:
			break;
		}
	}

	public void initScraw(){
		System.out.println("进入scraw");
		setContentView(R.layout.activity_scrawl);
		setActionBar();
		imageView = (ImageView) findViewById(R.id.scrawlImageView);
		BitmapFactory.Options bfoOptions = new BitmapFactory.Options();
		bfoOptions.inScaled = false;
		File outputImage = new File(sourpath);
		imageUri = Uri.fromFile(outputImage);
		try {
			bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri)).copy(Bitmap.Config.ARGB_8888, true);
			imageView.setImageBitmap(bitmap);
			bitmapWidth = bitmap.getWidth();
			bitmapHeight = bitmap.getHeight();
			canvas = new Canvas();
			System.out.println(bitmap);
			canvas.setBitmap(bitmap);
			setPiont();
			path = new Path();
			System.out.println("bitmap:   " + bitmapWidth + "     " + bitmapHeight);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	void setActionBar(){
		ActionBar actionBar=getActionBar();
		actionBar.setTitle(" ");
		actionBar.setDisplayShowHomeEnabled(false);
		actionBar.setDisplayHomeAsUpEnabled(false);
	}
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		if (hasFocus == true) {
			Matrix matrix = imageView.getImageMatrix();
			Rect rect = imageView.getDrawable().getBounds();
			float[] values = new float[9];
			matrix.getValues(values);
			pictureRelativeLeft = values[2];
			pictureRelativeTop = values[5];
			pictureRelativeRight = pictureRelativeLeft + rect.width()
					* values[0];
			pictureRelativeButtom = pictureRelativeTop + rect.height()
					* values[0];

			int[] location = new int[2]; // 获取组件在手机屏幕中的绝对像素位置
			imageView.getLocationOnScreen(location);
			imageViewLeft = location[0];
			imageViewTop = location[1];
			System.out.println("imageView:" + imageViewLeft + "     "
					+ imageViewTop);
			imageViewRight = imageView.getRight();
			imageViewButtom = imageView.getBottom();
			setPictureRealPosition();
			proportionWidth = bitmapWidth
					/ (pictureRealRight - pictureRealLeft);
			proportionHeight = bitmapHeight
					/ (pictureRealButtom - pictureRealTop);
		}
	}

	void setPiont() {
		// 设置画笔的颜色
		paint = new Paint(Paint.DITHER_FLAG);
		paint.setColor(Color.RED);
		// 设置画笔风格
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(4);
		// 反锯齿
		paint.setAntiAlias(true);
		paint.setDither(true);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		double x = event.getX();
		double y = event.getY();
		if (x >= pictureRealLeft && x <= pictureRealRight
				&& y >= pictureRealTop && y <= pictureRealButtom) {
			switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					x = (x - pictureRealLeft) * proportionWidth;
					y = (y - pictureRealTop) * proportionHeight;
					path.moveTo((float) x, (float) y);
					preX = x;
					preY = y;
					break;
				case MotionEvent.ACTION_MOVE:
					System.out.println(x + "     " + y);
					x = (x - pictureRealLeft) * proportionWidth;
					y = (y - pictureRealTop) * proportionHeight;
					if(hasOut==true){
						path.reset();
						path.moveTo((float) x, (float) y);
						preX=x;
						preY=y;
						System.out.println("reset");
						hasOut=false;
					}
					path.quadTo((float) preX, (float) preY, (float) x, (float) y);
					preX = x;
					preY = y;
					break;
				case MotionEvent.ACTION_UP:
					System.out.println(x + "     " + y);
					x = (x - pictureRealLeft) * proportionWidth;
					y = (y - pictureRealTop) * proportionHeight;
					canvas.drawPath(path, paint);
					path.reset();
					break;
			}
		} else {
			path.reset();
			hasOut=true;
		}
		// 将cacheBitmap绘制到该View组件上
		//canvas.drawBitmap(bitmap, 0, 0, paint); // ②
		// 沿着path绘制
		canvas.drawPath(path, paint);
		imageView.setImageBitmap(bitmap);
		// invalidate();
		return false;
	}

	void setPictureRealPosition() {
		pictureRealLeft = imageViewLeft + pictureRelativeLeft;
		pictureRealTop = imageViewTop + pictureRelativeTop;
		pictureRealRight = imageViewLeft + pictureRelativeRight;
		pictureRealButtom = imageViewTop + pictureRelativeButtom;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		setResult(2);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.scrawl, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		int id = item.getItemId();
		if (id == R.id.scrawlSure) {
			String pos =  saveBitmapToSDCard(bitmap);
			//返回视频路径
			finish();
			JSUtil.execCallback(_pWebview,_callBackId, pos, JSUtil.OK, false);
			return true;
		}
		if(id==android.R.id.home){
			setResult(2);
			finish();
			return true;
		}
		return false;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_BACK:
				finish();
				return true;
			default:
				return super.onKeyUp(keyCode, event);
		}
	}


	public static String photoPath;
	public String saveBitmapToSDCard(Bitmap bitmap) {
		photoPath = getExternalFilesDir(null).getAbsolutePath();
		int index = photoPath.lastIndexOf("/");
		curtime = System.currentTimeMillis();
		photoPath =photoPath.substring(0, index)+"/apps/doctorHelper/doc/camera/"+curtime+ ".jpg";
		File f = new File(photoPath);
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
			bitmap.compress(Bitmap.CompressFormat.JPEG, 50, fOut);
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
		return photoPath;
	}

	@Override
	public String execute(final IWebview pWebview, final String action, final String[] pArgs) {
		if("startCamera".equals(action))
		{
			Intent intent = new Intent(pWebview.getActivity(), Cameractivity.class);
			_pWebview = pWebview;
			_callBackId = pArgs[0];
			pWebview.getActivity().startActivityForResult(intent, 1);
		}
		return null;
	}

	@Override
	public void init(AbsMgr absMgr, String s) {

	}

	@Override
	public void dispose(String s) {

	}
}