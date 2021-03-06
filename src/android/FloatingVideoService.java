package com.plugin.floatv1.floatingwindow;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.zhongzilian.chestnutapp.MainActivity;
import com.zhongzilian.chestnutapp.R;

import org.apache.cordova.CordovaInterface;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.TimerTask;

import android.widget.SeekBar;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by noah chen on 2022/1/5.
 */

public class FloatingVideoService extends Service  {
  public static boolean isStarted = false;
  public static String videoUrl;
  public static String videoUrl_old;
  public static long times_old;
  public static int times_cur = 0;
  public static LocalDateTime beginPlayer;
  private WindowManager windowManager;
  public static WindowManager.LayoutParams layoutParams;

  public static MediaPlayer mediaPlayer;
  public static View displayView;
  public static Context this_context;
  public static CordovaInterface this_cordova;
  public static View this_view;
  public static RelativeLayout video_display_relativeLayout;
  public static int  landscape = 1;

  public static int layoutParamsWidth = 255;
  public static int layoutParamsHeight = 146;
  public static int  isUpAdd = 1;
  public static int  iCountViewBigger = 2;
  public static int  iCountViewShow = 1;
  private static final int TOUCH_MOVE = 1;//???????????????
  private static final int TOUCH_CLICK = 2;//???????????????
  private static final int TOUCH_DOUBLE_CLICK = 3;//???????????????
  private static final int  TOUCH_ACTION_DOWN = 4;

  private static final long singleClickDurationTime = 200;//??????200ms??????????????????
  private static final long doubleClickDurationTime = 300;//??????300ms????????????????????????

  private long touchDownTime = -1;//??????????????????
  private long lastSingleClickTime = -1;//???????????????????????????

  public static SeekBar seekbar;
  public static boolean isChanging=false;//?????????????????????????????????SeekBar?????????????????????
  public static Timer mTimer;
  public static TimerTask mTimerTask;
  public static  AudioManager am ;
  public  static int maxWidth = 1668;
  public  static int maxHeight = 2768;
  public  static int is_speed = 0; // 1????????????

  @Override
  public void onCreate() {
    super.onCreate();
    isStarted = true;
    windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    layoutParams = new WindowManager.LayoutParams();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
    } else {
      layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
    }
    layoutParams.format = PixelFormat.RGBA_8888;
    layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
    layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

    layoutParams.x = 100;
    layoutParams.y = 100;

    if(landscape==1){
      layoutParams.width = layoutParamsWidth*iCountViewBigger;
      layoutParams.height = layoutParamsHeight*iCountViewBigger;
    }else{
      layoutParams.width = layoutParamsHeight*iCountViewBigger;
      layoutParams.height = layoutParamsWidth*iCountViewBigger;
    }

    mediaPlayer = new MediaPlayer();

    Display defaultDisplay = windowManager.getDefaultDisplay ();
    Point point = new Point();
    defaultDisplay.getSize(point);
    FloatingVideoService.maxWidth = point.x;
    FloatingVideoService.maxHeight = point.y;

    am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

  }



  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @RequiresApi(api = Build.VERSION_CODES.M)
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    showFloatingWindow();
    return super.onStartCommand(intent, flags, startId);
  }

  //??????????????????????????????
  private int customTouchType(MotionEvent event){
    //????????????????????????????????????
    int touchType = -1;
    //??????????????????????????????
    int action = event.getAction();
    //?????????????????????????????????
    if(action == MotionEvent.ACTION_MOVE){
      //???????????????????????????????????????????????????
      long deltaTime = System.currentTimeMillis() - touchDownTime;
      //???????????????????????????????????????200ms????????????????????????????????????
      if(deltaTime > singleClickDurationTime){
        //???????????????????????????
        touchType = TOUCH_MOVE;
      }
      // touchType = TOUCH_MOVE;
    }
    //???????????????????????????
    else if(action == MotionEvent.ACTION_DOWN){
      //?????????????????????
      touchDownTime = System.currentTimeMillis();
      touchType = TOUCH_ACTION_DOWN;
    }
    //???????????????????????????
    else if(action == MotionEvent.ACTION_UP){
      //?????????????????????
      long touchUpTime = System.currentTimeMillis();
      //????????????????????????
      long downUpDurationTime = touchUpTime - touchDownTime;
      //??????????????????????????????200ms???????????????????????????
      Log.println( Log.ERROR ,"","1:"+downUpDurationTime);
      if(downUpDurationTime <= singleClickDurationTime){
        touchType = TOUCH_CLICK;
        //??????????????????????????????????????????????????????
        long twoClickDurationTime = touchUpTime - lastSingleClickTime;
        //????????????????????????????????????300ms???????????????????????????
        if(twoClickDurationTime <=  doubleClickDurationTime){
          //?????????????????????
          touchType = TOUCH_DOUBLE_CLICK;
          //??????????????????????????????????????????????????????????????????????????????
          lastSingleClickTime = -1;
          touchDownTime = -1;
        }
        //????????????????????????????????????300ms?????????????????????????????????????????????
        //?????????????????????????????????300ms??????????????????????????????????????????
        else{
          //??????????????????????????????
          lastSingleClickTime = touchUpTime;
        }
      }
    }
    //???????????????????????????
    return touchType;
  }


  @RequiresApi(api = Build.VERSION_CODES.M)
  public static void hideVideo()
  {
    long cur_times = mediaPlayer.getTimestamp().getAnchorMediaTimeUs();

    videoUrl = "-1";
    times_old = mediaPlayer.getTimestamp().getAnchorMediaTimeUs();
    mediaPlayer.pause();
    mediaPlayer.reset();

    displayView.setVisibility(View.GONE);    // ?????? view
    displayView.destroyDrawingCache();
    displayView.clearAnimation();
    displayView.cancelLongPress();
    displayView.clearFocus();

    isStarted = false;

    FloatingWindowPlugin.callJS(""+cur_times);

    Intent it = new Intent(this_cordova.getActivity().getBaseContext(), FloatingVideoService.class);
    this_cordova.getActivity().getBaseContext().stopService(it);
  }

  public  static   void closeVideo() {
    video_display_relativeLayout.postInvalidate();
    video_display_relativeLayout.post(new Runnable(){
      @RequiresApi(api = Build.VERSION_CODES.M)
      @Override
      public void run() {
        Intent it = new Intent(this_cordova.getActivity().getBaseContext(), FloatingVideoService.class);
        this_cordova.getActivity().getBaseContext().stopService(it);
        video_display_relativeLayout.setVisibility(View.GONE);    // ?????? view
        isStarted = false;
        long cur_times = mediaPlayer.getTimestamp().getAnchorMediaTimeUs();//??????
        FloatingWindowPlugin.callJS(""+cur_times);
        videoUrl = "-1";
        mediaPlayer.pause();
        mediaPlayer.reset();
      }
    });

  }

  public static void showVideo(){
    try {
      //??????OnAudioFocusChangeListener??????(????????????????????????)
      AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
          if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            if (mediaPlayer.isPlaying()) {
              mediaPlayer.pause();
            }
          }
          else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
             if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
              mediaPlayer.start();
            }
            // Resume playback
          }
//          else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
//            if (mediaPlayer.isPlaying()) {
//
//              mediaPlayer.stop();
//            }
//           // am.abandonAudioFocus(afChangeListener);
//          }
        }
      };

      AudioManager mManager = (AudioManager)this_context.getSystemService(Context.AUDIO_SERVICE);
      int result = mManager.requestAudioFocus(afChangeListener,
        AudioManager.STREAM_MUSIC,
        AudioManager.AUDIOFOCUS_GAIN);



      isStarted = true;
      videoUrl_old = videoUrl;
      mediaPlayer.reset();
      mediaPlayer.setDataSource(this_context, Uri.parse(videoUrl));
      mediaPlayer.prepare(); //.prepareAsync(); //
      //mediaPlayer.start();
      if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
        mediaPlayer.start();
      }
      mediaPlayer.seekTo(times_cur); //??????,????????????????????????
      FloatingWindowPlugin.callJS("-1");
      //this_cordova.getActivity().finish();//???????????????,?????????????????????

      seekbar.setMax(mediaPlayer.getDuration());//???????????????
      //----------???????????????????????????---------//
      mTimer = new Timer();
      mTimerTask = new TimerTask() {
        @Override
        public void run() {
          if(isChanging==true) {
            return;
          }
          seekbar.setProgress(mediaPlayer.getCurrentPosition());
        }
      };
      mTimer.schedule(mTimerTask, 0, 10);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }



  @RequiresApi(api = Build.VERSION_CODES.M)
  // @RequiresApi(api = Build.VERSION_CODES.O)
  private void showFloatingWindow() {
    if (Settings.canDrawOverlays(this)) {

      LayoutInflater layoutInflater = LayoutInflater.from(this);
      displayView = layoutInflater.inflate(R.layout.video_display, null);
      video_display_relativeLayout = displayView.findViewById(R.id.video_display_relativeLayout);

      displayView.setVisibility(View.VISIBLE); // ?????? view
      displayView.setOnTouchListener(new FloatingOnTouchListener());
      mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
      SurfaceView surfaceView = displayView.findViewById(R.id.video_display_surfaceview);
      seekbar = (SeekBar) displayView.findViewById(R.id.seekbar);
      seekbar.getThumb().setColorFilter(Color.parseColor("#F5F5F5"), PorterDuff.Mode.SRC_ATOP);
      seekbar.getProgressDrawable().setColorFilter(Color.parseColor("#FFFFFF"),PorterDuff.Mode.SRC_ATOP);

      final SurfaceHolder surfaceHolder = surfaceView.getHolder();
      surfaceHolder.addCallback(new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
          mediaPlayer.setDisplay(surfaceHolder);
          showVideo();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
      });
      mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {

        }
      });

      ImageView closeImageView =   displayView.findViewById(R.id.iv_close_window);
      closeImageView.setOnClickListener(  new  ImageView.OnClickListener() {
        @Override
        public void onClick(View v) {
          // ?????????????????????
          hideVideo();
        }
      });

      // ??????????????????????????????????????????
      ImageView  goMainImageView =   displayView.findViewById(R.id.iv_zoom_main_btn);
      goMainImageView.setOnClickListener(  new  ImageView.OnClickListener() {
        @Override
        public void onClick(View v) {

          /**?????????????????????????????????????????????????????????
           * ?????????????????????????????????????????????????????????*/
          FloatingSystemHelper.setTopApp(this_cordova.getActivity().getBaseContext());
          FloatingWindowPlugin.callJS("-2");
        }
      });

      ImageView  playImageView =   displayView.findViewById(R.id.iv_play_btn);
      playImageView.setOnClickListener(  new  ImageView.OnClickListener() {
        @Override
        public void onClick(View v) {
           // ??????
          mediaPlayer.start();
          showPlayPaushBtn();
        }
      });

      ImageView  pauseImageView =   displayView.findViewById(R.id.iv_pause_btn);
      pauseImageView.setOnClickListener(  new  ImageView.OnClickListener() {
        @Override
        public void onClick(View v) {
          // ??????
          mediaPlayer.pause();
          showPlayPaushBtn();
        }
      });

      //??????
      ImageView  speedImageView =   displayView.findViewById(R.id.iv_speed_btn);
      speedImageView.setOnClickListener(  new  ImageView.OnClickListener() {
        @Override
        public void onClick(View v) {
          // ?????? 15 ???
          long cur_times = mediaPlayer.getTimestamp().getAnchorMediaTimeUs();//??????
          int skt_time =  (int)(cur_times / 1000) + 15 * 1000;
          mediaPlayer.seekTo(skt_time);
        }
      });

      //??????
      ImageView  reverseImageView =   displayView.findViewById(R.id.iv_reverse_btn);
      reverseImageView.setOnClickListener(  new  ImageView.OnClickListener() {
        @Override
        public void onClick(View v) {
          // ?????? 15 ???
          long cur_times = mediaPlayer.getTimestamp().getAnchorMediaTimeUs();//??????
          int skt_time =  (int)(cur_times / 1000) - 15 * 1000;
          mediaPlayer.seekTo(skt_time);
        }
      });


      windowManager.addView(displayView, layoutParams);
    }
  }

  public  void  showPlayPaushBtn(){
    ImageView  playImageView =   displayView.findViewById(R.id.iv_play_btn);
    ImageView  pauseImageView =   displayView.findViewById(R.id.iv_pause_btn);
    ImageView  speedImageView =   displayView.findViewById(R.id.iv_speed_btn);
    ImageView  reverseImageView =   displayView.findViewById(R.id.iv_reverse_btn);
    if(iCountViewShow % 2 == 0) {
      //??????
      if (mediaPlayer.isPlaying()) {
        pauseImageView.setVisibility(View.VISIBLE);
        playImageView.setVisibility(View.GONE);
      } else {
        pauseImageView.setVisibility(View.GONE);
        playImageView.setVisibility(View.VISIBLE);
      }
      if(is_speed==1) {
        speedImageView.setVisibility(View.VISIBLE);
        reverseImageView.setVisibility(View.VISIBLE);
      }
    }
    else {
      //??????
      pauseImageView.setVisibility(View.GONE);
      playImageView.setVisibility(View.GONE);
      speedImageView.setVisibility(View.GONE);
      reverseImageView.setVisibility(View.GONE);
    }
  }



  private class FloatingOnTouchListener implements View.OnTouchListener {
    private int x;
    private int y;

    @Override
    public boolean onTouch(View view, MotionEvent event) {
      //????????????customTouchType?????????????????????????????????????????????
      int touchType = customTouchType(event);
      //???????????????????????????????????????
      if(touchType == TOUCH_ACTION_DOWN) {
        //?????????????????????????????????????????????
        x = (int) event.getRawX();
        y = (int) event.getRawY();
      }
      else if(touchType == TOUCH_MOVE) {
        //?????????????????????????????????????????????
        int nowX = (int) event.getRawX();
        int nowY = (int) event.getRawY();
        int movedX = nowX - x;
        int movedY = nowY - y;
        x = nowX;
        y = nowY;
        layoutParams.x = layoutParams.x + movedX;
        layoutParams.y = layoutParams.y + movedY;
        windowManager.updateViewLayout(view, layoutParams);

      }
      //???????????????????????????????????????
      else if(touchType == TOUCH_DOUBLE_CLICK){
        //changViewHeightWidth();
        //???????????????????????????????????????
        if( iCountViewBigger == 4){
          isUpAdd = 0; //???
        }
        if (iCountViewBigger == 2 ){
          isUpAdd = 1; //???
        }

        if(isUpAdd == 1){
          iCountViewBigger++;
        }else{
          iCountViewBigger--;
        }
        int cur_width =layoutParamsWidth*iCountViewBigger;
        int cur_height = layoutParamsHeight*iCountViewBigger;
        if(cur_width>maxWidth-20){
          cur_width = maxWidth-20;
        }
        if(cur_height>maxHeight-20){
          cur_height = maxHeight-20;
        }
        if(landscape==1){
          layoutParams.width = cur_width;
          layoutParams.height = cur_height;
        }else{
          layoutParams.width = cur_height;
          layoutParams.height = cur_width;
        }
        windowManager.updateViewLayout(view, layoutParams);
        // Log.println( Log.ERROR,"","TOUCH_DOUBLE_CLICK:"+iCountViewBigger+",width:"+layoutParams.width+",height:"+layoutParams.height);
      }
      else if( touchType == TOUCH_CLICK){
        iCountViewShow++;
        ImageView  goMainImageView =   displayView.findViewById(R.id.iv_zoom_main_btn);
        ImageView closeImageView =   displayView.findViewById(R.id.iv_close_window);
        if(iCountViewShow % 2 == 0) {
          //????????????
          closeImageView.setVisibility(View.VISIBLE);
          goMainImageView.setVisibility(View.VISIBLE);
          seekbar.setVisibility(View.VISIBLE);
        }else{
          //????????????
          closeImageView.setVisibility(View.GONE);
          goMainImageView.setVisibility(View.GONE);
          seekbar.setVisibility(View.GONE);
        }
        showPlayPaushBtn();
      }

      return true;

    }
  }
}
