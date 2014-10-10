package net.pocketmagic.android.carousel;
/*
 * 3D carousel View
 * http://www.pocketmagic.net 
 *
 * Copyright (c) 2013 by Radu Motisan , radu.motisan@gmail.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * For more information on the GPL, please go to:
 * http://www.gnu.org/copyleft/gpl.html
 *
 */ 
import java.util.ArrayList;

import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.speech.tts.TextToSpeech;
//import android.support.v4.app.NavUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class MainActivity extends Activity implements OnItemSelectedListener, SensorEventListener, TextWatcher {
	
	Singleton 				m_Inst 					= Singleton.getInstance();
	CarouselViewAdapter 	m_carouselAdapter		= null;	 
	private CarouselView coverFlow = null;
    private TextToSpeech mSpeech;
	
	private int numDigits = 5;
	private SensorManager mSensorManager;
	private int gyroCount = 0;
	private final int gyroThreshold = 5;
	private TextView userInputField;
	private String userInput = "";
	private boolean confirmFlag = false;
	private CarouselDataItem currentItem;
	private int textColor = Color.WHITE; 
	
    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        mSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
	            public void onInit(int status) {
	                // Do nothing.
	            }
	        });
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,  WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	        
	        //no keyboard unless requested by user
	      	getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN); 
	      		
	        // compute screen size and scaling
	     	Singleton.getInstance().InitGUIFrame(this);
	     	
	     	int padding = m_Inst.Scale(10);
			// create the interface : full screen container
			RelativeLayout panel  = new RelativeLayout(this);
		    panel.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
			panel.setPadding(padding, padding, padding, padding);
		    panel.setBackgroundDrawable(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, 
		    		new int[]{Color.BLACK, Color.BLACK}));
		    setContentView(panel); 
		    
		    // copy images from assets to sdcard
		    for (int i = 1; i<=numDigits;i++)
		    	AppUtils.AssetFileCopy(this, Environment.getExternalStorageDirectory().getPath().concat("/number" +i+".png"), "number"+i+".png", false);
		    
		    //Create carousel view documents. Each image is added to the carousel three times to give the illusion of a never ending wheel.
		    ArrayList<CarouselDataItem> Docus = new ArrayList<CarouselDataItem>();
		    for (int i=1;i<=numDigits;i++) {
		    	CarouselDataItem docu;
		        if (i%numDigits==0) docu = new CarouselDataItem(Environment.getExternalStorageDirectory().getPath().concat("/number5.png"), 0, ""+i);
		       	else if (i%numDigits==1) docu = new CarouselDataItem(Environment.getExternalStorageDirectory().getPath().concat("/number1.png"), 0, ""+i);
		        else if (i%numDigits==2) docu = new CarouselDataItem(Environment.getExternalStorageDirectory().getPath().concat("/number2.png"), 0, ""+i);
		        else if (i%numDigits==3) docu = new CarouselDataItem(Environment.getExternalStorageDirectory().getPath().concat("/number3.png"), 0, ""+i);
		        else docu = new CarouselDataItem(Environment.getExternalStorageDirectory().getPath().concat("/number4.png"), 0, ""+i);
		        Docus.add(docu);
		    }
		    
		    //Entered numbers
		    userInputField = new TextView(this);
		    userInputField.setTextColor(textColor);
		    userInputField.setText("Move your head left or right to select a number. Nod to cofirm that number");
		    userInputField.setTextSize(15);
		    AppUtils.AddView(panel, userInputField, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 
		    		new int[][]{new int[]{RelativeLayout.CENTER_HORIZONTAL}, new int[]{RelativeLayout.ALIGN_PARENT_TOP}}, -1,-1);
		
		    // add logo
		    TextView tv = new TextView(this);
		    tv.setTextColor(textColor);
		    tv.setText("Augmate Login");
		    AppUtils.AddView(panel, tv, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 
		    		new int[][]{new int[]{RelativeLayout.CENTER_HORIZONTAL}, new int[]{RelativeLayout.ALIGN_PARENT_BOTTOM}}, -1,-1);
		    
		    // create the carousel
		    coverFlow = new CarouselView(this);
	        
		    // create adapter and specify device independent items size (scaling)
		    // for more details see: http://www.pocketmagic.net/2013/04/how-to-scale-an-android-ui-on-multiple-screens/
		    int startValue = Integer.MAX_VALUE/2-(Integer.MAX_VALUE/2)%numDigits;
		    m_carouselAdapter =  new CarouselViewAdapter(this,Docus, m_Inst.Scale(400),m_Inst.Scale(300));
	        coverFlow.setAdapter(m_carouselAdapter);
	        coverFlow.setSpacing(-1*m_Inst.Scale(150));
	        coverFlow.setSelection(startValue, true);
	        coverFlow.setAnimationDuration(1000);
	        coverFlow.setOnItemSelectedListener((OnItemSelectedListener) this);
	        currentItem = (CarouselDataItem) m_carouselAdapter.getItem(startValue);
	
	        AppUtils.AddView(panel, coverFlow, LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT, 
	        		new int[][]{new int[]{RelativeLayout.CENTER_IN_PARENT}},
	        		-1, -1); 
	        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
	        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_UI);
	        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_UI);
    	    //mGestureDetector = createGestureDetector(this);
    }
    @Override
    protected void onResume(){
    	mSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            public void onInit(int status) {}});
    	mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),SensorManager.SENSOR_DELAY_UI);
    	super.onResume();
    }
    @Override
    public void onDestroy(){
    	mSpeech.shutdown();
    	mSensorManager.unregisterListener(this);
    	super.onDestroy();
    }
    
    @Override
    protected void onStop(){
        if(mSpeech != null){
        	mSpeech.shutdown();
        	mSensorManager.unregisterListener(this);
        }
        super.onStop();
    }
    
    @Override
    public void onOptionsMenuClosed(Menu menu) {
    	super.onOptionsMenuClosed(menu);
        finish();
    }

	public void afterTextChanged(Editable arg0) {}

	public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

	public void onTextChanged(CharSequence s, int start, int before, int count) {
		m_carouselAdapter.getFilter().filter(s.toString()); 
	}

	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		 currentItem =  (CarouselDataItem) m_carouselAdapter.getItem((int) arg3);
	}

	public void onNothingSelected(AdapterView<?> arg0) {}

	public void onSensorChanged(SensorEvent event) {
		//gyroCount++;
		/*
		if(event.sensor.getType() == Sensor.TYPE_ORIENTATION){ //X: Up=-179 Down=-1 Y:Left=181 Right=359
			Log.d("xxx", "X:"+event.values[1]+" Y:"+event.values[0] + " INPUT:"+userInput);
			//coverFlow.setSelection(carouselIndex);
			if(!confirmFlag && event.values[1]>-75){
				confirmFlag  = true;
				userInput=userInput.concat(Integer.toString(coverFlow.getSelectedItemPosition()-numDigits));
				userInputField.setText(userInput);
				mSpeech.speak(Integer.toString(carouselIndex-numDigits), TextToSpeech.QUEUE_FLUSH, null);
			}
			else if(confirmFlag && event.values[1]<-75)
				confirmFlag = false;
		}
		*/
		if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE && !mSpeech.isSpeaking()){
			processGyro(event.values[1], event.values[0]);
			gyroCount = 0;
		}
	}
	
	@SuppressWarnings("deprecation")
	private void processGyro(double gyroX, double gyroY){
		if(gyroY < -1 && !confirmFlag){  //nod down
			confirmFlag  = true;
			userInput=userInput.concat(currentItem.m_szDocName);
			userInputField.setText(userInput);
			mSpeech.speak(currentItem.m_szDocName, TextToSpeech.QUEUE_FLUSH, null);
			Log.d("yyy", "X:"+gyroX+" Y:"+gyroY+" INPUT:"+userInput);
		}
		else if(gyroY > 1 && !confirmFlag && userInput.length()>0){  //nod up
			confirmFlag  = true;
			userInput=userInput.substring(0, userInput.length()-1);
			userInputField.setText(userInput);
			mSpeech.speak("Delete", TextToSpeech.QUEUE_FLUSH, null);
		}
		else if(gyroX< -1)  //right
			coverFlow.onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, null);		
		else if(gyroX> 1)  //left
			coverFlow.onKeyDown(KeyEvent.KEYCODE_DPAD_LEFT, null);
		
		if(gyroY<1 && gyroY>-1) //Head position is stable on the Y-axis
			confirmFlag = false;
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}   
}