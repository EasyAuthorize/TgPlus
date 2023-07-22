package com.easy.tgPlus;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Button;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;
import android.widget.TextView;
import android.content.SharedPreferences;

public class MainActivity extends Activity{ 

	private View.OnClickListener myOnclick = new View.OnClickListener(){

		@Override
		public void onClick(View v){
			int vid = v.getId();

			if (vid == R.id.Btn_start){
				//
				Toast.makeText(MainActivity.this,"isActivate2:"+isActivate2(), Toast.LENGTH_SHORT).show();
			}else if (vid == R.id.Btn_stop){
				Toast.makeText(MainActivity.this,"stop", Toast.LENGTH_SHORT).show();
			}else if (vid == R.id.Btn_test){
                Toast.makeText(MainActivity.this, "测试" + getApplicationContext().getPackageResourcePath() + 10 / 0, Toast.LENGTH_SHORT).show();
			}else{
				Toast.makeText(MainActivity.this, "此ID(" + vid + ")事件未注册", Toast.LENGTH_SHORT).show();
			}
		}

	};

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        LinearLayout rootlayout = new LinearLayout(this);

		TextView info = new TextView(MainActivity.this);
		info.setText(isActivate() ?"模块已激活": "模块未激活");
		rootlayout.addView(info);

		Button btn = new Button(MainActivity.this);
		btn.setAllCaps(false);
		btn.setText("Start");
		btn.setId(R.id.Btn_start);
		btn.setOnClickListener(myOnclick);
		rootlayout.addView(btn);

		btn = new Button(MainActivity.this);
		btn.setText("stop");
		btn.setId(R.id.Btn_stop);
		btn.setOnClickListener(myOnclick);
		rootlayout.addView(btn);

		btn = new Button(MainActivity.this);
		btn.setText("test");
		btn.setId(R.id.Btn_test);
		btn.setOnClickListener(myOnclick);
		rootlayout.addView(btn);

		Button untest = new Button(MainActivity.this);
		untest.setText("未注册的按钮");
		untest.setId(3);
		untest.setOnClickListener(myOnclick);
		rootlayout.addView(untest);

		rootlayout.setGravity(Gravity.CENTER);
        rootlayout.setOrientation(LinearLayout.VERTICAL);
        rootlayout.setBackgroundColor(0xFF545454);

		setContentView(rootlayout);
    }

	private static final boolean isActivate(){
		return false;
	}
	
	private static final boolean isActivate2(){
		return true;
	}

} 
