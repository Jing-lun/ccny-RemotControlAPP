package com.fei435;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.fei435.Constant.CommandArray;

import static com.fei435.Constant.COMM_SERVO;
import static com.fei435.Constant.COMM_SUCTION_OFF;
import static com.fei435.Constant.COMM_SUCTION_ON;
import static com.fei435.Constant.COMM_LED_ON;
import static com.fei435.Constant.COMM_LED_OFF;
import static com.fei435.Constant.COMM_LIGHT_ON;
import static com.fei435.Constant.COMM_LIGHT_OFF;
import static com.fei435.Constant.COMM_FRAME_LIGHT_ON;
import static com.fei435.Constant.COMM_FRAME_LIGHT_OFF;

public class Main extends Activity implements
        com.fei435.SeekBar.OnSeekBarChangeListener,android.widget.SeekBar.OnSeekBarChangeListener  //分别是横向和纵向的SeekBar
{
    private final int MIN_GEAR_STEP = 5;
    private final int MAX_GEAR_VALUE = 180;
    private final int INIT_GEAR_VALUE = 50;

    private final int MIN_GEAR_STEP_1 = 1;
    private final int MAX_GEAR_VALUE_1 = 100;
    private final int INIT_GEAR_VALUE_1 = 50;

    private final int MIN_GEAR_STEP_2 = 1;
    private final int MAX_GEAR_VALUE_2 = 100;
    private final int INIT_GEAR_VALUE_2 = 50;

    public static int flagsuction = 0;
    public static int flagled = 0;

    private ImageButton ForWard;  //按钮的类，代表一个按钮
    private ImageButton BackWard;
    private ImageButton TurnLeft;
    private ImageButton TurnRight;

    //插入我的按钮
    private ImageButton Servo;
    private ImageButton Suction;
    private ImageButton Led;
    private ImageButton Light;
    private ImageButton FrameLight;

    private boolean bAnimationEnabled = true;
    private TextView mLogText;
    private boolean bGravityDetectOn = false;

    private ImageView imageView = null;
    private ImageView SoundView = null;
    private Bitmap bmp = null;
    private Bitmap bmp_sound = null;
    private Button VideoStream;
    private Button SoundStream;

    private Drawable ForWardon;
    private Drawable ForWardoff;
    private Drawable BackWardon;
    private Drawable BackWardoff;
    private Drawable TurnLefton;
    private Drawable TurnLeftoff;
    private Drawable TurnRighton;
    private Drawable TurnRightoff;
    private Drawable Ledon;
    private Drawable Ledoff;
    private Drawable Lighton;
    private Drawable Lightoff;
    private Drawable FrameLighton;
    private Drawable FrameLightoff;

    //插入我的按钮
    private Drawable Servoon;
    private Drawable Servooff;
    private Drawable Suctionon;
    private Drawable Suctionoff;

    private com.fei435.SeekBar mSeekBar1;
    private com.fei435.SeekBar mSeekBar2;            //自己实现的纵向seekbar,这是进度条
    private android.widget.SeekBar mSpeedSeekBar1;
    private android.widget.SeekBar mSpeedSeekBar2;   //系统自带的横向seekbar
    private int  mSeekBarValue1 = -1;
    private int  mSeekBarValue2 = -1;
    private int  mSpeedSeekBarValue1 = -1;
    private int  mSpeedSeekBarValue2 = -1;
    private EditText editTextSpeed1;
    private EditText editTextSpeed2;

    private CheckBox speedChangeCheckBox;

    private ImageButton buttonCus1;
    private boolean bCaptureOn = false;

    private boolean mQuitFlag = false;
    private boolean bHeartBreakFlag = false;//only for a test by feifei435
    private int mHeartBreakCounter = 0;     //小车心跳包计数
    private int mLastCounter = 0;

    private Vibrator mVibrator= null;
    private SensorManager mSensorMgr = null;
    private Sensor sensor = null;
    private int lastCommand = 0x0;

    private WiFiCarController mWiFiCarControler = null;//网络连接线程的类
    private Context mContext;
    MjpegView backgroundView = null;

    Handler mHandler = new Handler() {
        public void handleMessage(Message msg)
        {
            //Log.i("Handle", "handle internal Message, id=" + msg.what);

            imageView.setImageBitmap(bmp);
            SoundView.setImageBitmap(bmp_sound);

            switch (msg.what) {
                case Constant.MSG_ID_ERR_RECEIVE:
                    Log.i("socket", "MSG_ID_ERR_RECEIVE");
                    break;
                case Constant.MSG_ID_CON_READ:
                    byte[] command = (byte[])msg.obj;
                    //Log.i("mLogText","handle response from router: " + command.toString() );
                    handleCallback(command);
                    break;
                case Constant.MSG_ID_ERR_INIT_READ:
                    mLogText.setText("Connecting to WiFiCar failed!");
                    Log.i("mLogText","Connecting to WiFiCar failed!");
                    break;
                case Constant.MSG_ID_CON_SUCCESS:
                    mLogText.setText("Connecting to WiFiCar succeed!");
                    Log.i("mLogText","Connecting to WiFiCar succeed!");
                    //连接成功，延时2秒发送速度设置指令
                    Message msgChangeSpeed1 = new Message();
                    msgChangeSpeed1.what = Constant.MSG_ID_SET_SPEED;
                    msgChangeSpeed1.obj = Constant.COMM_SPEED_VALUE_1;

                    Message msgChangeSpeed2 = new Message();
                    msgChangeSpeed2.what = Constant.MSG_ID_SET_SPEED;
                    msgChangeSpeed2.obj = Constant.COMM_SPEED_VALUE_2;

                    mHandler.sendMessageDelayed(msgChangeSpeed1, 2000);
                    mHandler.sendMessageDelayed(msgChangeSpeed2, 2500);

                    break;
                case Constant.MSG_ID_SET_SPEED:
                    mWiFiCarControler.sendCommand((byte[])msg.obj);
                    break;
                case Constant.MSG_ID_SET_UI_INFO:      //别的类给mLogText显示消息
                    String str = (String)msg.obj;
                    mLogText.setText(str);

                    Message msgHB1 = new Message();
                    msgHB1.what = Constant.MSG_ID_HEART_BREAK_RECEIVE;//启动心跳包检测循环
                    //mHandler.sendMessage(msgHB1);

                    Message msgHB2 = new Message();
                    msgHB2.what = Constant.MSG_ID_HEART_BREAK_SEND;//启动心跳包循环发送
                    //mHandler.sendMessage(msgHB2);

                    break;
                case Constant.MSG_ID_ERR_CONN:
                    //mLogText.setText("连接控制地址失败!");
                    //Log.i("mLogText","连接控制地址失败!");
                    mLogText.setText("Establish connection failed!");
                    Log.i("mLogText","Establish connection failed!");
                    break;
                case Constant.MSG_ID_CLEAR_QUIT_FLAG:
                    mQuitFlag = false;
                    break;
                case Constant.MSG_ID_START_CHECK:
                    //mLogText.setText("开始进行自检，请稍等。。。。!!");
                    //Log.i("mLogText","开始进行自检，请稍等。。。。!!");
                    mLogText.setText("Checking Please wait!!");
                    Log.i("mLogText","Checking Please wait!!");
                    //TODO:bReaddyToSendCmd应该放在哪里？
                    //bReaddyToSendCmd = true;
                    mWiFiCarControler.selfcheck();
                    break;
                case Constant.MSG_ID_HEART_BREAK_RECEIVE:
                    if (mHeartBreakCounter == 0) {
                        bHeartBreakFlag = false;

                    } else if (mHeartBreakCounter > 0) {
                        bHeartBreakFlag = true;
                    } else {
                        //mLogText.setText("心跳包出现异常，已经忽略...");
                        //Log.i("heart","心跳包出现异常，已经忽略...");
                        mLogText.setText("Heartbeat error");
                        Log.i("heart","Heartbeat error");
                    }
                    Log.i("heart", "handle MSG_ID_HEART_BREAK_RECEIVE :flag=" + bHeartBreakFlag);

                    if (mLastCounter == 0 && mHeartBreakCounter > 0) {
//                        startIconAnimation();
                    }
                    mLastCounter = mHeartBreakCounter;
                    mHeartBreakCounter = 0;
                    Message msgHB = new Message();
                    msgHB.what = Constant.MSG_ID_HEART_BREAK_RECEIVE;//启动心跳包检测循环
                    mHandler.sendMessageDelayed(msgHB, Constant.HEART_BREAK_CHECK_INTERVAL);
                    break;
                case Constant.MSG_ID_HEART_BREAK_SEND:
                    Message msgSB = new Message();
                    msgSB.what = Constant.MSG_ID_HEART_BREAK_SEND;//循环向路由器发送心跳包
                    Log.i("heart", "handle MSG_ID_HEART_BREAK_SEND");

                    mWiFiCarControler.sendCommand(Constant.COMM_HEART_BREAK);
                    mHandler.sendMessageDelayed (msgSB, Constant.HEART_BREAK_SEND_INTERVAL);
                    break;
                default :
                    break;
            }
            super.handleMessage(msg);
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {   //每个android App一启动都会调用的函数
        super.onCreate(savedInstanceState);
        Log.i("SurfaceStatus","onCreate");
        mContext = this;
        //TODO:以后可以把mHandler、mLogText都加入mContext中
        mWiFiCarControler = new WiFiCarController(mHandler, mLogText, mContext);
        mVibrator = (Vibrator)this.getSystemService(Service.VIBRATOR_SERVICE);
        mSensorMgr = (SensorManager)this.getSystemService(SENSOR_SERVICE);    //初始化感应器
        sensor = mSensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);    //实例化一个重力感应sensor

        initSettings(); //初始化建立网络连接

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);//隐去标题（应用的名字必须要写在setContentView之前，否则会有异常）
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.main); //用于显示布局

        imageView = (ImageView)findViewById(R.id.btnImageView);
        SoundView = (ImageView)findViewById(R.id.btnSoundView);
        VideoStream = (Button)findViewById(R.id.btnVideoStream);
        SoundStream = (Button)findViewById(R.id.btnSoundStream);

        //获取按钮
        ForWard= (ImageButton)findViewById(R.id.btnForward);
        TurnLeft= (ImageButton)findViewById(R.id.btnLeft);
        TurnRight=(ImageButton)findViewById(R.id.btnRight);
        BackWard= (ImageButton)findViewById(R.id.btnBack);
        speedChangeCheckBox = (CheckBox)findViewById(R.id.speedChangeCheckbox);

        //我的按钮
        Servo = (ImageButton)findViewById(R.id.btnServo);
        Suction = (ImageButton)findViewById(R.id.btnStop);
        Led = (ImageButton)findViewById(R.id.btnLed);
        Light = (ImageButton)findViewById(R.id.btnLight);
        FrameLight = (ImageButton)findViewById(R.id.btnFrameLight);

        buttonCus1= (ImageButton)findViewById(R.id.ButtonCus1);
        buttonCus1.setOnClickListener(buttonCus1ClickListener);

        ForWardon = getResources().getDrawable(R.drawable.sym_forward_1);
        ForWardoff = getResources().getDrawable(R.drawable.sym_forward);

        TurnLefton = getResources().getDrawable(R.drawable.sym_left_1);
        TurnLeftoff = getResources().getDrawable(R.drawable.sym_left);

        TurnRighton = getResources().getDrawable(R.drawable.sym_right_1);
        TurnRightoff = getResources().getDrawable(R.drawable.sym_right);

        BackWardon = getResources().getDrawable(R.drawable.sym_backward_1);
        BackWardoff = getResources().getDrawable(R.drawable.sym_backward);

        Servoon = getResources().getDrawable(R.drawable.sym_stop_1);
        Servooff = getResources().getDrawable(R.drawable.sym_stop);

        Suctionon = getResources().getDrawable(R.drawable.sym_stop_1);
        Suctionoff = getResources().getDrawable(R.drawable.sym_stop);

        Ledon = getResources().getDrawable(R.drawable.sym_stop_1);
        Ledoff = getResources().getDrawable(R.drawable.sym_stop);

        Lighton = getResources().getDrawable(R.drawable.sym_stop_1);
        Lightoff = getResources().getDrawable(R.drawable.sym_stop);

        FrameLighton = getResources().getDrawable(R.drawable.sym_stop_1);
        FrameLightoff = getResources().getDrawable(R.drawable.sym_stop);

        //显示视频及按钮的view,即MjpegView，backgroundView是MjpegView的实例
        backgroundView = (MjpegView)findViewById(R.id.mySurfaceView1);
        backgroundView.setHandler(mHandler);

        mLogText = (TextView)findViewById(R.id.logTextView);
        if (null != mLogText) {
            mLogText.setBackgroundColor(Color.argb(0, 0, 0, 0));//0~255透明度值  255不透明
            mLogText.setTextColor(Color.argb(255, 255, 255, 255));
            mLogText.setTextSize(10);
            mLogText.setText("");
        }

        //启动线程

        //线程启动完成

        mSpeedSeekBar1 = (android.widget.SeekBar)findViewById(R.id.seekBarSpeed1);
        mSpeedSeekBar1.setMax(MAX_GEAR_VALUE_1);
        mSpeedSeekBar1.setProgress(INIT_GEAR_VALUE_1);
        mSpeedSeekBar1.setOnSeekBarChangeListener(this);
        editTextSpeed1 = (EditText)findViewById(R.id.editTextSpeed1);
        editTextSpeed1.setText(INIT_GEAR_VALUE_1+"");

        mSpeedSeekBar2 = (android.widget.SeekBar)findViewById(R.id.seekBarSpeed2);
        mSpeedSeekBar2.setMax(MAX_GEAR_VALUE_2);
        mSpeedSeekBar2.setProgress(INIT_GEAR_VALUE_2);
        mSpeedSeekBar2.setOnSeekBarChangeListener(this);
        editTextSpeed2 = (EditText)findViewById(R.id.editTextSpeed2);
        editTextSpeed2.setText(INIT_GEAR_VALUE_2+"");

        VideoStream.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Thread t = new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        Socket socket = null;
                            try {
                                socket = new Socket("192.168.0.115", 8000);
                                while (true) {
                                DataInputStream dataInput = new DataInputStream(
                                        socket.getInputStream());
                                int size = dataInput.readInt();
                                byte[] data = new byte[size];
                                // dataInput.readFully(data);
                                int len = 0;
                                while (len < size) {
                                    len += dataInput.read(data, len, size - len);
                                }

                                ByteArrayOutputStream outPut = new ByteArrayOutputStream();
                                bmp = BitmapFactory.decodeByteArray(data, 0,
                                        data.length);
                                bmp.compress(CompressFormat.PNG, 100, outPut);
                                //imageView.setImageBitmap(bmp);
                                mHandler.obtainMessage().sendToTarget();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                    }
                };
                t.start();
            }
        });

        SoundStream.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Thread t = new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        Socket socket = null;
                        try {
                            socket = new Socket("192.168.0.115", 8080);
                            while (true) {
                                DataInputStream dataInput = new DataInputStream(
                                        socket.getInputStream());
                                int size = dataInput.readInt();
                                byte[] data = new byte[size];
                                // dataInput.readFully(data);
                                int len = 0;
                                while (len < size) {
                                    len += dataInput.read(data, len, size - len);
                                }

                                ByteArrayOutputStream outPut = new ByteArrayOutputStream();
                                bmp_sound = BitmapFactory.decodeByteArray(data, 0,
                                        data.length);
                                bmp_sound.compress(CompressFormat.PNG, 100, outPut);
                                //SoundView.setImageBitmap(bmp);
                                mHandler.obtainMessage().sendToTarget();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                };
                t.start();
            }
        });

//        SoundStream.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Thread t = new Thread() {
//                    @Override
//                    public void run() {
//                        super.run();
//                        boolean run = true;
//                        try
//                        {
//                            DatagramSocket udpSocket = new DatagramSocket(8080, InetAddress.getLocalHost());
//                            while (run) {
//                                byte[] message = new byte[8000];
//                                DatagramPacket packet = new DatagramPacket(message, message.length);
//                                udpSocket.receive(packet);
//                            }
//                        }
//                        catch (IOException e)
//                        {
//                            run = false;
//                        }
//                    }
//                };
//                t.start();
//            }
//        });

        ForWard.setOnTouchListener( new View.OnTouchListener(){
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch(action)
                {
                    case MotionEvent.ACTION_DOWN:
                        mVibrator.vibrate(100);
                        mWiFiCarControler.sendCommand(Constant.COMM_FORWARD);   //发送前进命令
                        ForWard.setImageDrawable(ForWardon);
                        ForWard.invalidateDrawable(ForWardon);
                        break;
                    case MotionEvent.ACTION_UP:
                        mWiFiCarControler.sendCommand(Constant.COMM_STOP);
                        ForWard.setImageDrawable(ForWardoff);
                        ForWard.invalidateDrawable(ForWardoff);
                        break;
                }

                return false;
            }
        });

        BackWard.setOnTouchListener(new View.OnTouchListener()
        {
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch(action)
                {
                    case MotionEvent.ACTION_DOWN:
                        mVibrator.vibrate(100);
                        mWiFiCarControler.sendCommand(Constant.COMM_BACKWARD);  //发送后退命令
                        BackWard.setImageDrawable(BackWardon);
                        BackWard.invalidateDrawable(BackWardon);
                        break;
                    case MotionEvent.ACTION_UP:
                        mWiFiCarControler.sendCommand(Constant.COMM_STOP);
                        BackWard.setImageDrawable(BackWardoff);
                        BackWard.invalidateDrawable(BackWardoff);
                        break;
                }
                return false;
            }

        });

        TurnRight.setOnTouchListener(new View.OnTouchListener()
        {
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch(action)
                {
                    case MotionEvent.ACTION_DOWN:
                        mVibrator.vibrate(100);
                        mWiFiCarControler.sendCommand(Constant.COMM_RIGHT);
                        TurnRight.setImageDrawable(TurnRighton);
                        TurnRight.invalidateDrawable(TurnRighton);
                        break;
                    case MotionEvent.ACTION_UP:
                        mWiFiCarControler.sendCommand(Constant.COMM_STOP);
                        TurnRight.setImageDrawable(TurnRightoff);
                        TurnRight.invalidateDrawable(TurnRightoff);
                        break;
                }
                return false;
            }
        });

        TurnLeft.setOnTouchListener(new View.OnTouchListener()
        {
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch(action)
                {
                    case MotionEvent.ACTION_DOWN:
                        mVibrator.vibrate(100);
                        mWiFiCarControler.sendCommand(Constant.COMM_LEFT);
                        TurnLeft.setImageDrawable(TurnLefton);
                        TurnLeft.invalidateDrawable(TurnLefton);
                        break;
                    case MotionEvent.ACTION_UP:
                        mWiFiCarControler.sendCommand(Constant.COMM_STOP);
                        TurnLeft.setImageDrawable(TurnLeftoff);
                        TurnLeft.invalidateDrawable(TurnLeftoff);
                        break;
                }
                return false;
            }
        });

        Servo.setOnTouchListener( new View.OnTouchListener(){
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch(action)
                {
                    case MotionEvent.ACTION_DOWN:
                        mVibrator.vibrate(100);
                        Constant.COMM_SERVO[2] = (byte)(Constant.COMM_SERVO[2] + Constant.COMM_SERVO[3]);
                        mWiFiCarControler.sendCommand(COMM_SERVO);   //Send Sever Position
                        Servo.setImageDrawable(Servoon);
                        Servo.invalidateDrawable(Servoon);
                        break;
                    case MotionEvent.ACTION_UP:
                        Servo.setImageDrawable(Servooff);
                        Servo.invalidateDrawable(Servooff);
                        break;
                }
                return false;
            }
        });

        // Suction Start
        Suction.setOnTouchListener( new View.OnTouchListener(){
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch(action)
                {
                    case MotionEvent.ACTION_DOWN:
                        if(flagsuction == 0)
                        {
                            flagsuction = 1;
                            mVibrator.vibrate(100);
                            mWiFiCarControler.sendCommand(COMM_SUCTION_ON);   //Send Sever Position
                            Suction.setImageDrawable(Suctionon);
                            Suction.invalidateDrawable(Suctionon);
                            break;
                        }
                        else
                        {
                            flagsuction = 0;
                            mVibrator.vibrate(100);
                            mWiFiCarControler.sendCommand(COMM_SUCTION_OFF);   //Send Sever Position
                            Suction.setImageDrawable(Suctionoff);
                            Suction.invalidateDrawable(Suctionoff);
                            break;
                        }
                }
                return false;
            }
        });

        // Solinoid Start
        Led.setOnTouchListener( new View.OnTouchListener(){
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch(action)
                {
                    case MotionEvent.ACTION_DOWN:
                        if(flagled == 0)
                        {
                            flagled = 1;
                            mVibrator.vibrate(100);
                            mWiFiCarControler.sendCommand(COMM_LED_ON);   //Send Sever Position
                            Led.setImageDrawable(Ledon);
                            Led.invalidateDrawable(Ledon);
                            break;
                        }
                        else
                        {
                            flagled = 0;
                            mVibrator.vibrate(100);
                            mWiFiCarControler.sendCommand(COMM_LED_OFF);   //Send Sever Position
                            Led.setImageDrawable(Ledoff);
                            Led.invalidateDrawable(Ledoff);
                            break;
                        }
                }
                return false;
            }
        });

        // Light Start
        Light.setOnTouchListener( new View.OnTouchListener(){
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch(action)
                {
                    case MotionEvent.ACTION_DOWN:
                        if(flagled == 0)
                        {
                            flagled = 1;
                            mVibrator.vibrate(100);
                            mWiFiCarControler.sendCommand(COMM_LIGHT_ON);   //light on
                            Light.setImageDrawable(Lighton);
                            Light.invalidateDrawable(Lighton);
                            break;
                        }
                        else
                        {
                            flagled = 0;
                            mVibrator.vibrate(100);
                            mWiFiCarControler.sendCommand(COMM_LIGHT_OFF);   //light off
                            Light.setImageDrawable(Lightoff);
                            Light.invalidateDrawable(Lightoff);
                            break;
                        }
                }
                return false;
            }
        });

        // Frame Light Start
        FrameLight.setOnTouchListener( new View.OnTouchListener(){
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch(action)
                {
                    case MotionEvent.ACTION_DOWN:
                        if(flagled == 0)
                        {
                            flagled = 1;
                            mVibrator.vibrate(100);
                            mWiFiCarControler.sendCommand(COMM_FRAME_LIGHT_ON);   //light on
                            FrameLight.setImageDrawable(FrameLighton);
                            FrameLight.invalidateDrawable(FrameLighton);
                            break;
                        }
                        else
                        {
                            flagled = 0;
                            mVibrator.vibrate(100);
                            mWiFiCarControler.sendCommand(COMM_FRAME_LIGHT_OFF);   //light off
                            FrameLight.setImageDrawable(FrameLightoff);
                            FrameLight.invalidateDrawable(FrameLightoff);
                            break;
                        }
                }
                return false;
            }
        });

        //***********************
        //暂时不清楚这个方法的意思
        //***********************
        SensorEventListener lsn = new SensorEventListener(){
            public void onSensorChanged (SensorEvent e){
                if(bGravityDetectOn){
                    float x = e.values[SensorManager.DATA_X];
                    float y = e.values[SensorManager.DATA_Y];
                    float z = e.values[SensorManager.DATA_Z];

                    if (x < 2)
                    {
                        //不要一直重复发送命令造成单片机负担
                        if(lastCommand != Constant.COMM_FORWARD[2]){
                            mWiFiCarControler.sendCommand(Constant.COMM_FORWARD);   //发送前进命令//前进
                        }
                        lastCommand = Constant.COMM_FORWARD[2];
                    }
                    else if (x > 7)
                    {
                        if(lastCommand != Constant.COMM_FORWARD[2]){
                            mWiFiCarControler.sendCommand(Constant.COMM_BACKWARD);  //发送后退命令//后退
                        }
                        lastCommand = Constant.COMM_BACKWARD[2];
                    }
                    else if (y < -1)
                    {
                        if(lastCommand != Constant.COMM_LEFT[2]){
                            mWiFiCarControler.sendCommand(Constant.COMM_LEFT);  //发送后退命令//左
                        }
                        lastCommand = Constant.COMM_LEFT[2];
                    }
                    else if (y > 1)
                    {
                        if(lastCommand != Constant.COMM_RIGHT[2]){
                            mWiFiCarControler.sendCommand(Constant.COMM_RIGHT);  //发送后退命令//右
                        }
                        lastCommand = Constant.COMM_RIGHT[2];
                    }
                    else
                    {
                        if(lastCommand != Constant.COMM_STOP[2]){
                            mWiFiCarControler.sendCommand(Constant.COMM_STOP);  //发送后退命令//停
                        }
                        lastCommand = Constant.COMM_STOP[2];
                    }
                }
            }
            public void onAccuracyChanged (Sensor s, int accuracy){
            }
        };
        mSensorMgr.registerListener (lsn, sensor, SensorManager.SENSOR_DELAY_GAME);

//        gravityDetectToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//                                                           public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                                                               if (isChecked) {
//                                                                   bGravityDetectOn = true;
//                                                               }else {
//                                                                   bGravityDetectOn = false;
//                                                               }
//                                                           }
//                                                       }
//        );

        speedChangeCheckBox.setOnCheckedChangeListener (new CompoundButton.OnCheckedChangeListener()
        {
            public void onCheckedChanged (CompoundButton buttonView, boolean isChecked)
            {
                if (isChecked){
                    //editText1.setText(buttonView.getText() + "选中");
                    mSpeedSeekBar1.setVisibility(View.VISIBLE);
                    mSpeedSeekBar2.setVisibility(View.VISIBLE);
                    editTextSpeed1.setVisibility(View.VISIBLE);
                    editTextSpeed2.setVisibility(View.VISIBLE);
                }
                else{
                    //editText1.setText(buttonView.getText() + "取消选中");
                    mSpeedSeekBar1.setVisibility(View.INVISIBLE);
                    mSpeedSeekBar2.setVisibility(View.INVISIBLE);
                    editTextSpeed1.setVisibility(View.INVISIBLE);
                    editTextSpeed2.setVisibility(View.INVISIBLE);
                }
            }
        });

        editTextSpeed1.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                String str = editTextSpeed1.getText().toString();
                int value = Integer.parseInt(str);
                mSpeedSeekBar1.setProgress(value);

                Message msg = new Message();
                msg.what = Constant.MSG_ID_SET_SPEED;
                //拆分并转换一个int为两个byte 十六进制
                //Constant.COMM_SPEED_VALUE_1[2] = (byte)(value >> 8);
                Constant.COMM_SPEED_VALUE_1[2] = 0x01;
                Constant.COMM_SPEED_VALUE_1[3] = (byte)(value);
                Log.i("speed", "set speed(十进制):"+value);
                msg.obj = Constant.COMM_SPEED_VALUE_1;
                mHandler.sendMessage(msg);
                return false;
            }
        });
        editTextSpeed2.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                String str = editTextSpeed2.getText().toString();
                int value = Integer.parseInt(str);
                mSpeedSeekBar2.setProgress(value);

                Message msg = new Message();
                msg.what = Constant.MSG_ID_SET_SPEED;
                //拆分并转换一个int为两个byte 十六进制
                //Constant.COMM_SPEED_VALUE_2[2] = (byte)(value >> 8);
                Constant.COMM_SPEED_VALUE_2[2] = 0x02;
                Constant.COMM_SPEED_VALUE_2[3] = (byte)(value);
                Log.i("speed", "set speed(十进制):"+value);
                msg.obj = Constant.COMM_SPEED_VALUE_2;
                mHandler.sendMessage(msg);
                return false;
            }
        });
        //connect


//        WebView mWebView=(WebView) findViewById(R.id.webview);
//
//        //allow zoom in and out controls
//        mWebView.getSettings().setJavaScriptEnabled(true);
//
//        //zoom out to best fit the screen
//        mWebView.getSettings().setLoadWithOverviewMode(true);
//        mWebView.getSettings().setUseWideViewPort(true);
//
//        //load the stream link
//         mWebView.loadUrl("http://192.168.0.106:8080/stream_viewer?topic=/camera/color/image_rect_color");
//
//        //set the view to be explicitly on the webview widget
//        mWebView.setWebViewClient(new InsideWebViewClient());
    }


//    private OnClickListener buttonLenClickListener = new OnClickListener() {
//        public void onClick(View arg0) {
//            mVibrator.vibrate(100);
//            if (bCaptureOn) {
//                bCaptureOn = false;
//                //sendCommand(COMM_LEN_OFF);
//                Log.i("ScreenCapture", "button turn off capture clicked");
//                backgroundView.toggleVideoCapture();
//                buttonLen.setImageDrawable(buttonLenoff);
//                buttonLen.invalidateDrawable(buttonLenon);
//            } else  {
//                bCaptureOn = true;
//                //sendCommand(COMM_LEN_ON);
//                Log.i("ScreenCapture", "button turn on capture clicked");
//                backgroundView.toggleVideoCapture();
//                buttonLen.setImageDrawable(buttonLenon);
//                buttonLen.invalidateDrawable(buttonLenon);
//            }
//        }
//    };

    //照相
    private OnClickListener buttonTakePicClickListener = new OnClickListener() {
        public void onClick(View arg0) {
            mVibrator.vibrate(100);
            if (null != backgroundView) {
                backgroundView.saveBitmap();
            }
        }
    };



    private class InsideWebViewClient extends WebViewClient {
        // Force links to be opened inside WebView and not in Default Browser
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;

        }
    }


    private OnClickListener buttonCus1ClickListener = new OnClickListener() {
        public void onClick(View arg0) {
            mVibrator.vibrate(100);
            Log.i("settingclick","buttonCus1ClickListener");

            mWiFiCarControler.disconnFromRouter();

            Intent setIntent = new Intent();
            setIntent.setClass(mContext, WifiCarSettings.class);
            startActivity(setIntent);
        }
    };

/*    private OnLongClickListener buttonCus1ClickListener2 = new OnLongClickListener() {
        public boolean onLongClick(View arg0) {
        	Log.i("settingclick","buttonCus1ClickListener2");
            mThreadFlag = false;
            try {
                if (null != mThreadClient)
                    mThreadClient.join(); // wait for second to finish
            } catch (InterruptedException e) {
                Log.i("mLogText","关闭路由器监听进程失败。。。" +  e.getMessage());
            }
            return false;
        }
    };*/

    private void inspectParam(){
        Log.i("inspect", "CAMERA_VIDEO_URL"+Constant.CAMERA_VIDEO_URL);
        Log.i("inspect", "ROUTER_CONTROL_URL"+Constant.ROUTER_CONTROL_URL);
        Log.i("inspect", "ROUTER_CONTROL_PORT"+Constant.ROUTER_CONTROL_PORT);
        Log.i("inspect", "m4test"+Constant.m4test);
    }

    private void initSettings () {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        //DEFAULT_VALUE_CAMERA_URL,DEFAULT_VALUE_CAMERA_URL_TEST都是获取失败后默认的地址
        Constant.CAMERA_VIDEO_URL = settings.getString(Constant.PREF_KEY_CAMERA_URL, Constant.DEFAULT_VALUE_CAMERA_URL);
        Constant.CAMERA_VIDEO_URL_TEST = settings.getString(Constant.PREF_KEY_CAMERA_URL_TEST, Constant.DEFAULT_VALUE_CAMERA_URL_TEST);
//		 if (!settings.contains(Constant.PREF_KEY_CAMERA_URL)) {
//			settings.edit().putString(Constant.PREF_KEY_CAMERA_URL, Constant.DEFAULT_VALUE_CAMERA_URL);
//		 }
//		 if(!settings.contains(Constant.PREF_KEY_CAMERA_URL_TEST)){
//			 settings.edit().putString(Constant.PREF_KEY_CAMERA_URL_TEST, Constant.DEFAULT_VALUE_CAMERA_URL_TEST);
//		 }

        //DEFAULT_VALUE_ROUTER_URL是获取sharedPreference失败后的默认地址
        String RouterUrl = settings.getString(Constant.PREF_KEY_ROUTER_URL, Constant.DEFAULT_VALUE_ROUTER_URL);
        int index = RouterUrl.indexOf(":");
        String routerIP = "";
        String routerPort = "";
        int port = 0;
        if (index > 0) {
            routerIP = RouterUrl.substring(0, index);
            routerPort = RouterUrl.substring(index+1, RouterUrl.length() );
            port = Integer.parseInt(routerPort);
        }
        Constant.ROUTER_CONTROL_URL = routerIP;
        Constant.ROUTER_CONTROL_PORT = port;
//		 if (!settings.contains(Constant.PREF_KEY_ROUTER_URL)) {
//			 settings.edit().putString(Constant.PREF_KEY_ROUTER_URL, Constant.DEFAULT_VALUE_ROUTER_URL);
//		 }

        RouterUrl = settings.getString(Constant.PREF_KEY_ROUTER_URL_TEST, Constant.DEFAULT_VALUE_ROUTER_URL_TEST);
        index = RouterUrl.indexOf(":");
        if (index > 0) {
            routerIP = RouterUrl.substring(0, index);
            routerPort = RouterUrl.substring(index+1, RouterUrl.length() );
            port = Integer.parseInt(routerPort);
        }
        Constant.ROUTER_CONTROL_URL_TEST = routerIP;
        Constant.ROUTER_CONTROL_PORT_TEST = port;
//		 if(!settings.contains(Constant.PREF_KEY_ROUTER_URL_TEST)){
//			 settings.edit().putString(Constant.PREF_KEY_ROUTER_URL_TEST, Constant.DEFAULT_VALUE_ROUTER_URL_TEST);
//		 }

        Constant.m4test =  settings.getBoolean(Constant.PREF_KEY_TEST_MODE_ENABLED, false);
//		 if(!settings.contains(Constant.PREF_KEY_TEST_MODE_ENABLED)){
//			 settings.edit().putBoolean(Constant.PREF_KEY_TEST_MODE_ENABLED, false);
//		 }

        initLenControl(Constant.PREF_KEY_LEN_ON, Constant.DEFAULT_VALUE_LEN_ON);
        initLenControl(Constant.PREF_KEY_LEN_OFF, Constant.DEFAULT_VALUE_LEN_OFF);

        //inspectParam();
    }

    void initLenControl (String prefKey, String defaultValue) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        String comm = settings.getString(prefKey, defaultValue);
        CommandArray cmd = new CommandArray(comm);
        if (cmd.isValid() ) {
            if (Constant.PREF_KEY_LEN_ON.equalsIgnoreCase(prefKey)) {
                Constant.COMM_LEN_ON[1] = cmd.mCmd1;
                Constant.COMM_LEN_ON[2] = cmd.mCmd2;
                Constant.COMM_LEN_ON[3] = cmd.mCmd3;
            } else if (Constant.PREF_KEY_LEN_OFF.equalsIgnoreCase(prefKey)) {
                Constant.COMM_LEN_OFF[1] = cmd.mCmd1;
                Constant.COMM_LEN_OFF[2] = cmd.mCmd2;
                Constant.COMM_LEN_OFF[3] = cmd.mCmd3;
            } else {
                Log.i("Main", "unknow prefKey:" + prefKey);
            }
        } else {
            Log.i("Main", "error format of command:" + comm);
        }
    }

    private void handleCallback(byte[] command) {
        if (null == command || command.length != Constant.COMMAND_LENGTH) {
            return;
        }

        byte cmd1 = command[1];
        byte cmd2 = command[2];
        //byte cmd3 = command[3];

        if (command[0] != Constant.COMMAND_PERFIX || command[Constant.COMMAND_LENGTH-1] !=  Constant.COMMAND_PERFIX) {
            return;
        }

        if (cmd1 != 0xEE) {
            Log.i("Socket", "unknow command from router, ignore it! cmd1=" + cmd1);
            return;
        }

        switch (cmd2) {
            case (byte)0xE1:
                //Log.i("heart","收到小车心跳包 ！");
                Log.i("heart","Received Heartbeat");
                handleHeartBreak();
                break;
//        case (byte)0xE2:
//            handleHeartBreak();
//            break;
            default:
                break;
        }
    }


    private boolean isIconAnimationEnabled () {
        //return bAnimationEnabled && bHeartBreakFlag;
        return bAnimationEnabled;
    }
    private boolean mIconAnimationState = false;

    /** Icon animation handler for flashing warning alerts. */
//    private final Handler mAnimationHandler = new Handler() {
//        @Override
//        public void handleMessage(Message msg) {
//            if (mIconAnimationState) {
//                mAnimIndicator.setAlpha(255);
//                if (isIconAnimationEnabled()) {
//                    mAnimationHandler.sendEmptyMessageDelayed(0, WARNING_ICON_ON_DURATION_MSEC);
//                }
//            } else {
//                mAnimIndicator.setAlpha(0);
//                if (isIconAnimationEnabled()) {
//                    mAnimationHandler.sendEmptyMessageDelayed(0, WARNING_ICON_OFF_DURATION_MSEC);
//                }
//            }
//            mIconAnimationState = !mIconAnimationState;
//            mAnimIndicator.invalidateDrawable(mWarningIcon);
//        }
//    };
//
//    private void startIconAnimation() {
//        Log.i("Animation", "startIconAnimation handler : " + mAnimationHandler);
//        if (mAnimIndicator != null) {
//            mAnimIndicator.setImageDrawable(mWarningIcon);
//        }
//        if (isIconAnimationEnabled())
//            mAnimationHandler.sendEmptyMessageDelayed(0, WARNING_ICON_ON_DURATION_MSEC);
//    }

    private void handleHeartBreak() {
        Log.i("heart", "handleHeartBreak");
        mHeartBreakCounter++;
        bHeartBreakFlag = true;
    }

    private void stopIconAnimation() {
//        mAnimationHandler.removeMessages(0);
    }

    public void onProgressChanged(com.fei435.SeekBar seekBar, int progress, boolean fromUserh) {

        if(seekBar == mSeekBar1){
            if (Math.abs(progress - mSeekBarValue1) > MIN_GEAR_STEP) {
                Log.i("mLogText","change angle: " + progress);
                mSeekBarValue1 = progress;
                Constant.COMM_GEAR_CONTROL_1[3] = (byte)progress;
                mWiFiCarControler.sendCommand(Constant.COMM_GEAR_CONTROL_1);
            }
        }else if(seekBar == mSeekBar2){
            if (Math.abs(progress - mSeekBarValue2) > MIN_GEAR_STEP) {
                Log.i("mLogText","change angle: " + progress);
                mSeekBarValue2 = progress;
                Constant.COMM_GEAR_CONTROL_2[3] = (byte)progress;
                mWiFiCarControler.sendCommand(Constant.COMM_GEAR_CONTROL_2);
            }
        }
    }
    public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
        if(seekBar == mSpeedSeekBar1){
            if (Math.abs(progress - mSpeedSeekBarValue1) > MIN_GEAR_STEP_1) {
                Log.i("mLogText","change speed: " + progress);
                editTextSpeed1.setText(progress+"");
                mSpeedSeekBarValue1 = progress;

                Message msg = new Message();
                msg.what = Constant.MSG_ID_SET_SPEED;
                //拆分并转换一个int为两个byte 十六进制
                //Constant.COMM_SPEED_VALUE_1[2] = (byte)(progress >> 8);
                Constant.COMM_SPEED_VALUE_1[2] = 0x01;
                Constant.COMM_SPEED_VALUE_1[3] = (byte)(progress);
                //Log.i("speed", "set speed(十进制):"+progress);
                Log.i("speed", "set speed:"+progress);
                msg.obj = Constant.COMM_SPEED_VALUE_1;
                mHandler.sendMessage(msg);
            }
        }else if(seekBar == mSpeedSeekBar2){
            if (Math.abs(progress - mSpeedSeekBarValue2) > MIN_GEAR_STEP_2) {
                Log.i("mLogText","change speed: " + progress);
                editTextSpeed2.setText(progress+"");
                mSpeedSeekBarValue2 = progress;

                Message msg = new Message();
                msg.what = Constant.MSG_ID_SET_SPEED;
                //拆分并转换一个int为两个byte 十六进制
                //Constant.COMM_SPEED_VALUE_2[2] = (byte)(progress >> 8);
                Constant.COMM_SPEED_VALUE_2[2] = 0x02;
                Constant.COMM_SPEED_VALUE_2[3] = (byte)(progress);
                //Log.i("speed", "set speed(十进制):"+progress);
                Log.i("speed", "set speed:"+progress);
                msg.obj = Constant.COMM_SPEED_VALUE_2;
                mHandler.sendMessage(msg);
            }
        }
    }

    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    public void onDestroy() {
        super.onDestroy();
        Log.i("SurfaceStatus","onDestroy");
        mWiFiCarControler.disconnFromRouter();

        stopIconAnimation();
    }
    protected void onResume() {
        super.onResume();
        Log.i("SurfaceStatus","onResume");

        if(Constant.CURRENT_WIFI_STATE == Constant.WIFI_STATE_CONNECTED){
            String cameraUrl = null;
            if (Constant.m4test) {
                cameraUrl = Constant.CAMERA_VIDEO_URL_TEST;
            } else {
                cameraUrl = Constant.CAMERA_VIDEO_URL;
            }
            if (null != cameraUrl && cameraUrl.length() > 4) {
                backgroundView.setSource(cameraUrl);//初始化Camera ,设置视频流读取地址
                backgroundView.resumePlayback();//启动线程，播放视频
            }
        }

        mWiFiCarControler.connectToRouter(); //连接小车
    }

    protected void onPause() {
        super.onPause();
        Log.i("SurfaceStatus", "onPause");
        mWiFiCarControler.disconnFromRouter();
    }

    @Override
    public void onBackPressed() {
        if (mQuitFlag) {
            finish();
        } else {
            mQuitFlag = true;
            Toast.makeText(mContext, "请再次按返回键退出应用", Toast.LENGTH_LONG).show();
            Message msg = new Message();
            msg.what = Constant.MSG_ID_CLEAR_QUIT_FLAG;
            mHandler.sendMessageDelayed(msg, Constant.QUIT_BUTTON_PRESS_INTERVAL);
        }
    }

    public void onStartTrackingTouch(android.widget.SeekBar seekBar) {
        // TODO Auto-generated method stub

    }

    public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
        // TODO Auto-generated method stub

    }

    //*******************
    //读取手柄方法
    //********************

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = false;
        int deviceId = event.getDeviceId();
        if (deviceId != -1) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        //mVibrator.vibrate(100);
                        mWiFiCarControler.sendCommand(Constant.COMM_LEFT);    //发送左转命令
                        TurnLeft.setImageDrawable(TurnLefton);
                        TurnLeft.invalidateDrawable(TurnLefton);
                        handled = true;
                        break;
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                        //mVibrator.vibrate(100);
                        mWiFiCarControler.sendCommand(Constant.COMM_RIGHT);    //发送右转命令
                        TurnRight.setImageDrawable(TurnRighton);
                        TurnRight.invalidateDrawable(TurnRighton);
                        handled = true;
                        break;
                    case KeyEvent.KEYCODE_DPAD_UP:
                        //mVibrator.vibrate(100);
                        mWiFiCarControler.sendCommand(Constant.COMM_FORWARD);   //发送前进命令
                        ForWard.setImageDrawable(ForWardon);
                        ForWard.invalidateDrawable(ForWardon);
                        handled = true;
                        break;
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                        //mVibrator.vibrate(100);
                        mWiFiCarControler.sendCommand(Constant.COMM_BACKWARD);  //发送后退命令
                        BackWard.setImageDrawable(BackWardon);
                        BackWard.invalidateDrawable(BackWardon);
                        handled = true;
                        break;
                    default:
                        handled = true;
                        break;
                }
        }

        return handled;

        //return super.onKeyDown(keyCode, event);
    }
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        boolean handled = false;
        int deviceId = event.getDeviceId();
        if (deviceId != -1) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    //mVibrator.vibrate(100);//震动，准备关掉这个函数
                    mWiFiCarControler.sendCommand(Constant.COMM_STOP);    //发送停止命令
                    TurnLeft.setImageDrawable(TurnLeftoff);
                    TurnLeft.invalidateDrawable(TurnLeftoff);
                    handled = true;
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    //mVibrator.vibrate(100);
                    mWiFiCarControler.sendCommand(Constant.COMM_STOP);    //发送停止命令
                    TurnRight.setImageDrawable(TurnRightoff);
                    TurnRight.invalidateDrawable(TurnRightoff);
                    handled = true;
                    break;
                case KeyEvent.KEYCODE_DPAD_UP:
                    //mVibrator.vibrate(100);
                    mWiFiCarControler.sendCommand(Constant.COMM_STOP);   //发送停止命令
                    ForWard.setImageDrawable(ForWardoff);
                    ForWard.invalidateDrawable(ForWardoff);
                    handled = true;
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    //mVibrator.vibrate(100);
                    mWiFiCarControler.sendCommand(Constant.COMM_STOP);  //发送停止命令
                    BackWard.setImageDrawable(BackWardoff);
                    BackWard.invalidateDrawable(BackWardoff);
                    handled = true;
                    break;
                default:
                    handled = true;
                    break;
            }
        }

        return handled;
    }

//    @Override
//    public boolean onGenericMotionEvent(MotionEvent event) {
//        int eventSource = event.getSource();
//        if ((((eventSource & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) ||
//                ((eventSource & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK))
//                && event.getAction() == MotionEvent.ACTION_MOVE) {
//            int id = event.getDeviceId();
//            if (-1 != id)
//            {
//                handleGenericMotionEvent(event);
//            }
//        }
//        return false;
//    }
//
//    private boolean handleGenericMotionEvent(MotionEvent event)
//    {
//        // Process all historical movement samples in the batch.
//        final int historySize = event.getHistorySize();
//        for (int i = 0; i < historySize; i++)
//        {
//            processJoystickInput(event, i);
//        }
//
//        // Process the current movement sample in the batch.
//        processJoystickInput(event, -1);
//        return true;
//    }
//
//    private void processJoystickInput(MotionEvent event, int historyPos)
//    {
//        // Get joystick position.
//        // Many game pads with two joysticks report the position of the
//        // second
//        // joystick
//        // using the Z and RZ axes so we also handle those.
//        // In a real game, we would allow the user to configure the axes
//        // manually.
//        if (null == m_inputDevice)
//        {
//            m_inputDevice = event.getDevice();
//        }
//        float x = getCenteredAxis(event, m_inputDevice, MotionEvent.AXIS_X, historyPos);
////        float x = event.getAxisValue(MotionEvent.AXIS_X);
//        Log.e(TAG, "JoyStick Value: x " + x);
//        if (x>=0 && x<=0.1)
//        {
//            mWiFiCarControler.sendCommand(Constant.COMM_STOP);    //发送右转命令
//            TurnRight.setImageDrawable(TurnRightoff);
//            TurnRight.invalidateDrawable(TurnRightoff);
//        }
//        if(x>0.1 && x<=1.0)
//        {
//            mWiFiCarControler.sendCommand(Constant.COMM_RIGHT);    //发送右转命令
//            TurnRight.setImageDrawable(TurnRighton);
//            TurnRight.invalidateDrawable(TurnRighton);
//        }
//
//        float y = getCenteredAxis(event, m_inputDevice, MotionEvent.AXIS_Y, historyPos);
//        if (y == 0)
//        {
//            y = getCenteredAxis(event, m_inputDevice, MotionEvent.AXIS_HAT_Y, historyPos);
//        }
//        if (y == 0)
//        {
//            y = getCenteredAxis(event, m_inputDevice, MotionEvent.AXIS_RZ, historyPos);
//        }
//    }
//
//    private float getCenteredAxis(MotionEvent event, InputDevice device, int axis, int historyPos)
//    {
//        final InputDevice.MotionRange range = device.getMotionRange(axis, event.getSource());
//        if (range != null)
//        {
//            final float flat = range.getFlat();
//            final float value = historyPos < 0 ? event.getAxisValue(axis)
//                    : event.getHistoricalAxisValue(axis, historyPos);
//
//            // Ignore axis values that are within the 'flat' region of the
//            // joystick axis center.
//            // A joystick at rest does not always report an absolute position of
//            // (0,0).
//            if (Math.abs(value) > flat)
//            {
//                return value;
//            }
//        }
//        return 0;
//    }
    //*******************
    //读取手柄方法完
    //********************
}