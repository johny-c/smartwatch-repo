/*
 Copyright (c) 2011, Sony Ericsson Mobile Communications AB

 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation
 and/or other materials provided with the distribution.

 * Neither the name of the Sony Ericsson Mobile Communications AB nor the names
 of its contributors may be used to endorse or promote products derived from
 this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.praktikum.linuxandc;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Handler;
import android.util.Log;

import com.sonyericsson.extras.liveware.aef.control.Control;
import com.sonyericsson.extras.liveware.extension.util.control.ControlExtension;
import com.sonyericsson.extras.liveware.extension.util.control.ControlTouchEvent;

/**
 * The sample control for SmartWatch handles the control on the accessory.
 * This class exists in one instance for every supported host application that
 * we have registered to
 */
class SWControlExtension extends ControlExtension implements Constants {

    private static final Bitmap.Config BITMAP_CONFIG = Bitmap.Config.RGB_565;
    
    private Options rgbaOptions;

    private static final int ANIMATION_X_POS = 46;

    private static final int ANIMATION_Y_POS = 46;

    private static final int ANIMATION_DELTA_MS = 500;

    private Handler mHandler;

    private boolean mIsShowingAnimation = false;

    private boolean mIsVisible = false;

    private final int width;

    private final int height;
    
    public static final String LOG_TAG = "SWControlExtension";
    

    /**
     * Create sample control.
     *
     * @param hostAppPackageName Package name of host application.
     * @param context The context.
     * @param handler The handler to use
     */
    SWControlExtension(final String hostAppPackageName, final Context context,
            Handler handler) {
        super(context, hostAppPackageName);
        if (handler == null) {
            throw new IllegalArgumentException("handler == null");
        }
        mHandler = handler;
        width = getSupportedControlWidth(context);
        height = getSupportedControlHeight(context);
        rgbaOptions = new BitmapFactory.Options();
        rgbaOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
    }

    /**
     * Get supported control width.
     *
     * @param context The context.
     * @return the width.
     */
    public static int getSupportedControlWidth(Context context) {
        return context.getResources().getDimensionPixelSize(R.dimen.smart_watch_control_width);
    }

    /**
     * Get supported control height.
     *
     * @param context The context.
     * @return the height.
     */
    public static int getSupportedControlHeight(Context context) {
        return context.getResources().getDimensionPixelSize(R.dimen.smart_watch_control_height);
    }

    @Override
    public void onDestroy() {

        Log.d(LOG_TAG, "SWControlExtension onDestroy");
        //stopAnimation();
        mHandler = null;
    };

    @Override
    public void onStart() {
        // Nothing to do. Animation is handled in onResume.
    	
    	Log.d(LOG_TAG, "onStart() is called");
        mContext.registerReceiver(mImageReceiver, 
        		new IntentFilter(IMAGE_BYTES));
        Log.d(LOG_TAG, "ImageReceiver is now registered");
        //Intent i = new Intent(mContext, MainActivity.SWStatusReceiver.class);
        //i.putExtra("connected", true);
        //mContext.sendBroadcast(i);
    }

    @Override
    public void onStop() {
    	Log.d(LOG_TAG, "onStop() is called");
        // Nothing to do. Animation is handled in onPause.
    	if(mImageReceiver != null)
    		mContext.unregisterReceiver(mImageReceiver);
        Log.d(LOG_TAG, "ImageReceiver is now unregistered");
        setScreenState(Control.Intents.SCREEN_STATE_OFF);
    }

    @Override
    public void onResume() {
        mIsVisible = true;
        Log.d(LOG_TAG, "onResume() is called");
        setScreenState(Control.Intents.SCREEN_STATE_ON);
        startRequest();
        // Animation not showing. Show animation.
        mIsShowingAnimation = true;
    }
    
    
    // Handler for received Intents for every new image
    private BroadcastReceiver mImageReceiver = new BroadcastReceiver() {
    	private byte[] imgBytes;
    	private int[] imgColors;
    	private Bitmap bitmap;
    	private static final int intAlpha = 255; // alpha is always 255 (FF) so we don't need to transmit it
    	private int imgLen;
    	
      @Override
      public void onReceive(Context context, Intent intent) {
    	  Log.d("ImageReceiver", "onReceive called");

    	  imgBytes = intent.getByteArrayExtra(BYTE_ARRAY_KEY);
    	  imgLen = imgBytes.length;
    	  int imgInts[] = new int[imgLen];

    	  // Bytes come as signed chars although RGBA struct defines them as unsigned
    	  // so we transform them to integers
    	  for(int i=0; i<imgLen; i++){
    		  imgInts[i] = imgBytes[i] & 0xFF;
    	  }
    	  
    	  // Recreate the image pixels colors
    	  imgColors = new int[width*height];
    	  for (int i = 0; i < imgLen - 3; i += 4) {
    		  imgColors[i / 4] = (intAlpha << 24) | (imgInts[i] << 16) | (imgInts[i + 1] << 8) | imgInts[i + 2];
    		}
    	  bitmap = Bitmap.createBitmap(imgColors, width, height, Bitmap.Config.ARGB_8888);
    	  
    	  
    	  showBitmap(bitmap);
      }
    };
    
    
    

    @Override
    public void onPause() {
        Log.d(LOG_TAG, "onPause() is called");
        stopRequest();
        mIsVisible = false;
        setScreenState(Control.Intents.SCREEN_STATE_OFF);
        if (mIsShowingAnimation) {
            //stopAnimation();
        }
        
    }


    @Override
    public void onTouch(final ControlTouchEvent event) {
        Log.d(LOG_TAG, "onTouch() " + event.getAction());
        if (event.getAction() == Control.Intents.TOUCH_ACTION_RELEASE) {
            if (mIsShowingAnimation) {
                //Log.d(LOG_TAG, "Stopping animation");

                // Stop the animation
                //stopAnimation();
            }
        }
    }



}
