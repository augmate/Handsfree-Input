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
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
//import android.support.v4.app.NavUtils;
import android.text.Editable;
import android.text.TextWatcher;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;

public class MainActivity extends Activity implements SensorEventListener, TextWatcher {
	
	Singleton 				m_Inst 					= Singleton.getInstance();
	CarouselViewAdapter 	m_carouselAdapter		= null;	 
	private CarouselView coverFlow = null;
	
	private int numDigits = 5;
	private GestureDetector mGestureDetector;
	private SensorManager mSensorManager;
	private boolean gyroActive = false;
	private final int gyroSense = 120;
	private int gyroCount = 0;
	private final int gyroThreshold = 20;
	private float gyroX;
	private float gyroY;
	private final int indexMin = numDigits+1;
	private final int indexMax = 2*numDigits;
	private final int passwordLength = 4;
	private int carouselIndex = indexMin;
	private TextView userInputField;
	private String userInput = "";
	private boolean confirmFlag = false;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
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
		    		new int[]{Color.WHITE, Color.GRAY}));
		    setContentView(panel); 
		    
		    // copy images from assets to sdcard
		    for (int i = 1; i<=numDigits;i++)
		    	AppUtils.AssetFileCopy(this, "/mnt/sdcard/number" +i+".png", "number"+i+".png", false);
		    
		    //Create carousel view documents. Each image is added to the carousel three times to give the illusion of a never ending wheel.
		    ArrayList<CarouselDataItem> Docus = new ArrayList<CarouselDataItem>();
		    for (int i=0;i<numDigits*3;i++) {
		    	CarouselDataItem docu;
		        if (i%numDigits==0) docu = new CarouselDataItem("/mnt/sdcard/number5.png", 0, "First Image "+i);
		       	else if (i%numDigits==1) docu = new CarouselDataItem("/mnt/sdcard/number1.png", 0, "Second Image "+i);
		        else if (i%numDigits==2) docu = new CarouselDataItem("/mnt/sdcard/number2.png", 0, "Third Image "+i);
		        else if (i%numDigits==3) docu = new CarouselDataItem("/mnt/sdcard/number3.png", 0, "Fourth Image "+i);
		        else docu = new CarouselDataItem("/mnt/sdcard/number4.png", 0, "fifth Image "+i);
		        Docus.add(docu);
		    }
		    /*
		    // add the search filter
		    EditText etSearch = new EditText(this);
		    etSearch.setHint("Search your document");
		    etSearch.setSingleLine();
		    etSearch.setTextColor(Color.BLACK);
		    etSearch.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_search, 0, 0, 0);
		    AppUtils.AddView(panel, etSearch, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 
		    		new int[][]{new int[]{RelativeLayout.CENTER_HORIZONTAL}, new int[]{RelativeLayout.ALIGN_PARENT_TOP}}, -1,-1);
		    etSearch.addTextChangedListener((TextWatcher) this);
		    */
		    //Entered numbers
		    
		    userInputField = new TextView(this);
		    userInputField.setTextColor(Color.BLACK);
		    userInputField.setText("Move your head left or right to select a number. Nod to cofirm that number");
		    userInputField.setTextSize(15);
		    AppUtils.AddView(panel, userInputField, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 
		    		new int[][]{new int[]{RelativeLayout.CENTER_HORIZONTAL}, new int[]{RelativeLayout.ALIGN_PARENT_TOP}}, -1,-1);
		
		    // add logo
		    TextView tv = new TextView(this);
		    tv.setTextColor(Color.BLACK);
		    tv.setText("Augmate Login");
		    AppUtils.AddView(panel, tv, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 
		    		new int[][]{new int[]{RelativeLayout.CENTER_HORIZONTAL}, new int[]{RelativeLayout.ALIGN_PARENT_BOTTOM}}, -1,-1);
		    
		    // create the carousel
		    coverFlow = new CarouselView(this);
	        
		    // create adapter and specify device independent items size (scaling)
		    // for more details see: http://www.pocketmagic.net/2013/04/how-to-scale-an-android-ui-on-multiple-screens/
		    m_carouselAdapter =  new CarouselViewAdapter(this,Docus, m_Inst.Scale(400),m_Inst.Scale(300));
	        coverFlow.setAdapter(m_carouselAdapter);
	        coverFlow.setSpacing(-1*m_Inst.Scale(150));
	        coverFlow.setSelection(Integer.MAX_VALUE / 2, true);
	        coverFlow.setAnimationDuration(1000);
	        //coverFlow.setOnItemSelectedListener((OnItemSelectedListener) this);
	
	        AppUtils.AddView(panel, coverFlow, LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT, 
	        		new int[][]{new int[]{RelativeLayout.CENTER_IN_PARENT}},
	        		-1, -1); 
	        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
	        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_UI);
	        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_UI);
    	    mGestureDetector = createGestureDetector(this);
    }

	public void afterTextChanged(Editable arg0) {}

	public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

	public void onTextChanged(CharSequence s, int start, int before, int count) {
		m_carouselAdapter.getFilter().filter(s.toString()); 
	}

	/*public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		 CarouselDataItem docu =  (CarouselDataItem) m_carouselAdapter.getItem((int) arg3);
		 if (docu!=null)
			 Toast.makeText(this, "You've clicked on:"+docu.getDocText(), Toast.LENGTH_SHORT).show();
	}

	public void onNothingSelected(AdapterView<?> arg0) {}*/

	public void onSensorChanged(SensorEvent event) {
		gyroCount++;
		if(event.sensor.getType() == Sensor.TYPE_ORIENTATION){ //X: Up=-179 Down=-1 Y:Left=181 Right=359
			Log.d("xxx", "X:"+event.values[1]+" Y:"+event.values[0] + " INPUT:"+userInput);
			//coverFlow.setSelection(carouselIndex);
			if(!confirmFlag && event.values[1]>-75){
				confirmFlag  = true;
				userInput=userInput.concat(Integer.toString(carouselIndex-numDigits));
				userInputField.setText(userInput);
				Log.d("yyy", userInput);
			}
			else if(confirmFlag && event.values[1]<-75)
				confirmFlag = false;
			Log.d("yyy", userInput);
		}
		
		
		
		
		
		if(!confirmFlag && event.sensor.getType() == Sensor.TYPE_GYROSCOPE)
			//gyroX = event.values[1] + gryoX;
			if (gyroCount > gyroThreshold)
				{
					gyroX = event.values[1] * gyroSense;
					gyroY = event.values[0] * gyroSense;
					//Log.d("yyy", "X:"+gyroX+" Y:"+gyroY);
					if(gyroY < -gyroSense){  //nod
						/*if(userInput.startsWith("Move"))
							userInputField.setText("");
						userInput.concat(Integer.toString(carouselIndex));
						userInputField.setText(userInput);*/
					}
					else if(gyroX> gyroSense){  //right
						carouselIndex++;
						if(carouselIndex>indexMax)
							carouselIndex = indexMin;
					}
					else if(gyroX < -gyroSense){  //left
						carouselIndex--;
						if(carouselIndex<indexMin)
							carouselIndex = indexMax;
					}
					
					coverFlow.setSelection(carouselIndex);
					gyroCount = 0;
				}
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}
	
	private GestureDetector createGestureDetector(Context context) {
	    GestureDetector gestureDetector = new GestureDetector(context);
	        gestureDetector.setBaseListener( new GestureDetector.BaseListener() {
	            public boolean onGesture(Gesture gesture) {
	                return false;
	            }
	        });
	       return gestureDetector;
    }


    
}