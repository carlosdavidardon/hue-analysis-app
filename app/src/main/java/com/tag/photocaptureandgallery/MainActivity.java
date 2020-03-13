package com.tag.photocaptureandgallery;

import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.takeimage.R;

public class MainActivity extends Activity {

	Resources res;
	private int REQUEST_CAMERA = 0, SELECT_FILE = 1;
	private int MAX_IMAGE_SIZE = 300;
	private Button btnSelect;
	private ImageView ivImage;
	private String userChoosenTask;
	private TextView txtPrediction;
	private TextView txtYellow;
	private TextView txtGreen;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		res = getResources();
		setContentView(R.layout.activity_main);
		btnSelect = (Button) findViewById(R.id.btnSelectPhoto);
		btnSelect.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				selectImage();
			}
		});
		ivImage = (ImageView) findViewById(R.id.ivImage);
		txtPrediction = (TextView) findViewById(R.id.txtPrediction);
		txtYellow = (TextView) findViewById(R.id.txtYellow);
		txtGreen = (TextView) findViewById(R.id.txtGreen);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		switch (requestCode) {
			case Utility.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					if(userChoosenTask.equals(res.getString(R.string.option_take)))
						cameraIntent();
					else if(userChoosenTask.equals(res.getString(R.string.option_select)))
						galleryIntent();
				} else {
					//code for deny
				}
				break;
		}
	}

	private void calculateColors(Bitmap thumbnail){
		int yellow=0;
		int green=0;
		for(int y = 0; y<thumbnail.getHeight(); y++){
			for(int x=0; x<thumbnail.getWidth(); x++){
				int c = thumbnail.getPixel(x, y);
				float hsvColor[] = {0,0,0};
				Color.colorToHSV(c, hsvColor);
				if ( (Color.red(c)>150 && Color.green(c)>150 && Color.blue(c)>150) ||
                        (Color.blue(c)!=0 && ((Color.red(c)/Color.blue(c))<1.3 && (Color.green(c)/Color.blue(c))<1.3)) ) {
					//white
                }else if(hsvColor[0]>25 && hsvColor[0]<85){//yellow
					yellow++;
				}else if(hsvColor[0]>=85 && hsvColor[0]<170){//green
					green++;
				}
			}
		}
		float percentageY = yellow*1.0f/(yellow+green);
		float percentageG = green*1.0f/(yellow+green);
		if(percentageY<0.25){//low
			txtPrediction.setText(res.getString(R.string.label_low));
		}else if(percentageY>=0.25 && percentageY<0.75){//medium
			txtPrediction.setText(res.getString(R.string.label_medium));
		}else{//high: >=0.75
			txtPrediction.setText(res.getString(R.string.label_high));
		}
		String yellowPer = String.format("%.2f", percentageY*100);
		String greenPer = String.format("%.2f", percentageG*100);
		txtYellow.setText(res.getString(R.string.color_yellow)+": "+yellowPer+"%");
		txtGreen.setText(res.getString(R.string.color_green)+": "+greenPer+"%");
	}

	private void selectImage() {
		final CharSequence[] items = { res.getString(R.string.option_take), res.getString(R.string.option_select),
				res.getString(R.string.option_cancel) };

		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder.setTitle(res.getString(R.string.option_title));
		builder.setItems(items, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int item) {
				boolean result=Utility.checkPermission(MainActivity.this);

				if (items[item].equals(res.getString(R.string.option_take))) {
					userChoosenTask =res.getString(R.string.option_take);
					if(result)
						cameraIntent();

				} else if (items[item].equals(res.getString(R.string.option_select))) {
					userChoosenTask =res.getString(R.string.option_select);
					if(result)
						galleryIntent();

				} else if (items[item].equals(res.getString(R.string.option_cancel))) {
					dialog.dismiss();
				}
			}
		});
		builder.show();
	}

	private void galleryIntent()
	{
		Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);//
		startActivityForResult(Intent.createChooser(intent, res.getString(R.string.library_title)),SELECT_FILE);
	}

	private void cameraIntent()
	{
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		startActivityForResult(intent, REQUEST_CAMERA);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == Activity.RESULT_OK) {
			if (requestCode == SELECT_FILE)
				onSelectFromGalleryResult(data);
			else if (requestCode == REQUEST_CAMERA)
				onCaptureImageResult(data);
		}
	}

	public static Bitmap scaleDown(Bitmap realImage, float maxImageSize,
								   boolean filter) {
		float ratio = Math.min(
				(float) maxImageSize / realImage.getWidth(),
				(float) maxImageSize / realImage.getHeight());
		int width = Math.round((float) ratio * realImage.getWidth());
		int height = Math.round((float) ratio * realImage.getHeight());

		Bitmap newBitmap = Bitmap.createScaledBitmap(realImage, width,
				height, filter);
		return newBitmap;
	}

	private void onCaptureImageResult(Intent data) {
		Bitmap thumbnail = (Bitmap) data.getExtras().get("data");

		Bitmap scaledBitmap;
		if(thumbnail.getWidth()> MAX_IMAGE_SIZE || thumbnail.getHeight()>MAX_IMAGE_SIZE){
			scaledBitmap = scaleDown(thumbnail, MAX_IMAGE_SIZE, true);
		}else{
			scaledBitmap = thumbnail;
		}
		calculateColors(scaledBitmap);

		ivImage.setImageBitmap(thumbnail);
	}


	@SuppressWarnings("deprecation")
	private void onSelectFromGalleryResult(Intent data) {

		Bitmap bm=null;
		if (data != null) {
			try {
				bm = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), data.getData());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		Bitmap scaledBitmap;
		if(bm.getWidth()> MAX_IMAGE_SIZE || bm.getHeight()>MAX_IMAGE_SIZE){
			scaledBitmap = scaleDown(bm, MAX_IMAGE_SIZE, true);
		}else{
			scaledBitmap = bm;
		}
		calculateColors(scaledBitmap);
		ivImage.setImageBitmap(scaledBitmap);
	}

	public void onClickAbout(View v) {
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder.setTitle(res.getString(R.string.about_title));
		builder.setMessage(res.getString(R.string.about_msg));
		builder.setPositiveButton(res.getString(R.string.about_positive),new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				String url = "http://cunori.edu.gt/mosaico-dorado/";
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(url));
				startActivity(i);
			}
		});
		builder.setNegativeButton(res.getString(R.string.about_negative),new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				return;
			}
		});
		builder.show();
	}

}