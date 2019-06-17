package jp.vstone.sotasample;

import java.awt.Color;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.monea.api.LocalModuleProxy;
import org.monea.api.ModuleContext;
import org.monea.api.ModuleException;
import org.monea.api.ProcessingRequest;
import org.monea.api.ProcessingRequest.Param;
import org.monea.api.ProcessingRequestQueue;

import jp.vstone.RobotLib.CRobotPose;
import jp.vstone.RobotLib.CRobotUtil;
import jp.vstone.camera.CameraCapture;


public class SotaActionPlayer {

	static final String TAG = "ActionPlayer";
	public static HashMap<String, Action> Basic_motion_map = new HashMap<String,Action >();

	static String[] status=new String[24];


	public static void main(String[] args) throws ModuleException, IOException, InterruptedException {
	System.setProperty("REGISTRY_SERVER_PORT","25002");
	Moter.init();
	CRobotUtil.Log(TAG, "Servo Init");

	CRobotPose pose = new CRobotPose();
	//LEDを点灯（左目：赤、右目：赤、口：Max、電源ボタン：赤）
	pose.setLED_Sota(Color.RED, Color.RED, 255, Color.GREEN);

	//遷移時間1000msecで動作開始。
	CRobotUtil.Log(TAG, "play:" + Moter.play(pose,300));


	ImageServer imageServer=new ImageServer();
	imageServer.setDaemon(true);
	imageServer.start();

	CRobotUtil.Log(TAG,"Monea接続待ち");
	MONEAConnector moneaConnector = MONEAConnector.getInstance("xml/S_AP.xml");
	ModuleContext context = moneaConnector.getModuleContext();
	while(context == null)
		context = moneaConnector.timedGetModuleContext(-1);
	CRobotUtil.Log(TAG,"Monea接続完了");
	Thread.sleep(1000);
	LocalModuleProxy localModule = context.getLocalModule();
	while(localModule == null)
    	localModule = context.getLocalModule();
	CRobotUtil.Log(TAG,"ローカルモジュール取得完了");


	ProcessingRequestQueue queue1 = localModule.getProcessingRequestEventQueue("play");
	ProcessingRequestQueue queue2 = localModule.getProcessingRequestEventQueue("cancel");
	final Action_Watcher action_watcher=new Action_Watcher(queue1,queue2);
	action_watcher.setDaemon(true);
	action_watcher.start();



	Basic_motion_map.put("head_y", Servo_Controller.getInstance(CSotaMotion2.SV_HEAD_Y));
	Basic_motion_map.put("head_p", Servo_Controller.getInstance(CSotaMotion2.SV_HEAD_P));
	Basic_motion_map.put("head_r", Servo_Controller.getInstance(CSotaMotion2.SV_HEAD_R));
	Basic_motion_map.put("body_y", Servo_Controller.getInstance(CSotaMotion2.SV_BODY_Y));
	Basic_motion_map.put("l_elb", Servo_Controller.getInstance(CSotaMotion2.SV_L_ELBOW));
	Basic_motion_map.put("l_sho", Servo_Controller.getInstance(CSotaMotion2.SV_L_SHOULDER));
	Basic_motion_map.put("r_elb", Servo_Controller.getInstance(CSotaMotion2.SV_R_ELBOW));
	Basic_motion_map.put("r_sho", Servo_Controller.getInstance(CSotaMotion2.SV_R_SHOULDER));
	Basic_motion_map.put("speak", Speaking_Controller.getInstance());
	Basic_motion_map.put("led", LED_Controller.getInstance());


    //シャットダウンフックを登録します。
    Runtime.getRuntime().addShutdownHook(new Thread(){
    		public void run(){
    		terminate();
    	}}
     );

    Map<Byte, Short> c_led;

    //メインスレッドをmonea公開用に使用
	while(true){
		if(Moter.isButton_Power()){
			break;
		}

		status[0]="head_p";
		status[1]=String.valueOf(-(int)Moter.getReadPose(CSotaMotion2.SV_HEAD_P)/20);
		status[2]="head_y";
		status[3]=String.valueOf((int)Moter.getReadPose(CSotaMotion2.SV_HEAD_Y)/20);
		status[4]="head_r";
		status[5]=String.valueOf((int)Moter.getReadPose(CSotaMotion2.SV_HEAD_R)/20);
		status[6]="body_y";
		status[7]=String.valueOf((int)Moter.getReadPose(CSotaMotion2.SV_BODY_Y)/20);
		status[8]="l_sho";
		status[9]=String.valueOf((int)Moter.getReadPose(CSotaMotion2.SV_L_SHOULDER)/20);
		status[10]="l_elb";
		status[11]=String.valueOf((int)Moter.getReadPose(CSotaMotion2.SV_L_ELBOW)/20);
		status[12]="r_sho";
		status[13]=String.valueOf(-(int)Moter.getReadPose(CSotaMotion2.SV_R_SHOULDER)/20);
		status[14]="r_elb";
		status[15]=String.valueOf(-(int)Moter.getReadPose(CSotaMotion2.SV_R_ELBOW)/20);

		c_led = Moter.getReadLED();
		status[16]="led_x";
		status[17]=String.valueOf((int)c_led.get((byte)0));
		status[18]="led_y";
		status[19]=String.valueOf((int)c_led.get((byte)1));
		status[20]="led_z";
		status[21]=String.valueOf((int)c_led.get((byte)2));

		status[22]="speak";
		if(Speaking_Controller.isSpeaking()){
			status[23]="1";
		}else{
			if(status[23] !=null && status[23].trim().equals("1")){
				localModule.setAsString("speaked_contents", Speaking_Controller.speaking_text);
			}
			status[23]="0";
		}


		localModule.setAsStringArray("status", status);

		localModule.commit();
		Thread.sleep(50);
	}

	System.exit(0);

	}

	public static void terminate(){
		CRobotUtil.Log(TAG, "終了処理");
		Moter.ServoOff();
	}

}


class ImageServer extends Thread{

public static final int PORT=33333;
static final String TAG = "ImageServer";

public void run() {
   	final CameraCapture cap = new CameraCapture(CameraCapture.CAP_IMAGE_SIZE_QVGA,CameraCapture.CAP_FORMAT_3BYTE_BGR);
   	try {
		cap.openDevice("/dev/video0");
	} catch (IOException e1) {
		// TODO 自動生成された catch ブロック
		e1.printStackTrace();
	}

    ServerSocketChannel serverChannel = null;

    //シャットダウンフックを登録します。
    Runtime.getRuntime().addShutdownHook(new Thread(){
    		public void run(){
    		CRobotUtil.Log(TAG, "終了処理");
    		cap.close();

    	}}
     );


    try {
        serverChannel = ServerSocketChannel.open();
        serverChannel.socket().bind(new InetSocketAddress(PORT));
        CRobotUtil.Log(TAG,"Start Accept");
        while (true) {
        	SocketChannel channel = serverChannel.accept();
            new Thread(new ChannelEchoThread(channel,cap)).start();
        }
    } catch (IOException e) {
        e.printStackTrace();
    } finally {
        if (serverChannel != null && serverChannel.isOpen()) {
            try {
            	CRobotUtil.Log(TAG,"Disconect");
                serverChannel.close();
            } catch (IOException e) {}
        }
    	}


	}

	}

class ChannelEchoThread implements Runnable {

	   private static final int BUF_SIZE = 4096;
	   SocketChannel channel = null;
	   static final String TAG = "ImageServer";
	   CameraCapture cap;

	   public ChannelEchoThread(SocketChannel channel,CameraCapture cap) {
	       this.channel = channel;
	       this.cap=cap;
	   }

	   public void run() {
	       ByteBuffer buf = ByteBuffer.allocate(BUF_SIZE);
	       Charset charset = Charset.forName("UTF-8");

	       try {
	            CRobotUtil.Log(TAG,channel.socket().getRemoteSocketAddress() + ":Accept");
	            while(true){
	            	buf.clear();
	            if (channel.read(buf) < 0) {
	                return;
	            }
	            buf.flip();
	            String input = charset.decode(buf).toString();
	            CRobotUtil.Log(TAG,channel.socket().getRemoteSocketAddress() + ":" + input);

	            String[] order = input.split(",");
	            if(order[0].equals("image")){
	        	   	cap.snap();
	        	   	cap.snap();
	        	   	cap.snap();
	        	   	byte[] data = cap.getImageRawData();
	        	   	ByteBuffer buffer = ByteBuffer.wrap(data);
	            	channel.write(charset.encode(CharBuffer.wrap(Integer.toString(data.length)+"END\n")));
	            	Thread.sleep(10);
	            	while(buffer.hasRemaining()){
	            		channel.write(buffer);
	            	}

	            	CRobotUtil.Log(TAG,"sended");

		            }else if(order[0].equals("end")){
		            	break;
	            }else{
		            	channel.write(charset.encode("Sota can't understand your message\n"));
		            }
	            }

	       } catch (IOException | InterruptedException e) {
	           e.printStackTrace();
	           return;
	       } finally {
	           if (channel != null && channel.isOpen()) {
	               try {
	                   channel.close();
	               } catch (IOException e) {}
	           }
	       }
	   }}


class Action_Watcher extends Thread{
    static final String TAG = "Action_watcher";
	   ProcessingRequestQueue play_queue=null;
	   ProcessingRequestQueue cancel_queue=null;


	   public Action_Watcher(ProcessingRequestQueue play_queue,ProcessingRequestQueue cancel_queue) throws ModuleException, IOException, InterruptedException {
	       this.play_queue = play_queue;
	       this.cancel_queue = cancel_queue;
	   }

	   public void run() {
		    CRobotPose pose;
			//すべての軸を動作
			pose = new CRobotPose();
			pose.SetPose(new Byte[] {1   ,2   ,3   ,4   ,5   ,6   ,7   ,8}	//id
			,  new Short[]{0   ,-900,0   ,900 ,0   ,0   ,0   ,0}				//target pos
					);
			//LEDを点灯（左目：赤、右目：赤、口：Max、電源ボタン：赤）
			pose.setLED_Sota(Color.GRAY, Color.GRAY, 255, Color.GREEN);

			//遷移時間1000msecで動作開始。
			CRobotUtil.Log(TAG, "play:" + Moter.play(pose,600));
		   try {

		   ProcessingRequest item=null;
		   List<Param> params=null;
		   Param param =null;

		   while(true){

					if(! play_queue.isEmpty() || !cancel_queue.isEmpty()){
						RequestParams requestParams=new RequestParams("root");
						if(!play_queue.isEmpty()){
							item = play_queue.pop();
						}else{
							item = cancel_queue.pop();
						}
						try {
							params = item.params();
						} catch (IOException e) {
							// TODO 自動生成された catch ブロック
							e.printStackTrace();
							continue;
						}
						CRobotUtil.Log(TAG, "Queue:"+item.getProcessingName());
						for(int i=0;i<params.size();i++){
							param = params.get(i);
							if(param.name().equals("x")){
								requestParams.points[0]=(double)param.toFloat64();
							}else if(param.name().equals("y")){
								requestParams.points[1]=(double)param.toFloat64();
							}else if(param.name().equals("z")){
								requestParams.points[2]=(double)param.toFloat64();
							}else if (param.name().equals("content")){
								requestParams.content=param.toString();
							}else if(param.name().equals("duration")){
								requestParams.duration=(double)param.toFloat64();
							}else if(param.name().equals("actionName")){
								requestParams.actionName=param.toString();
							}
						}
						if (item.getProcessingName().equals("play")){
							SotaActionPlayer.Basic_motion_map.get(requestParams.actionName).play(requestParams);

						}else if(item.getProcessingName().equals("cancel")){
							SotaActionPlayer.Basic_motion_map.get(requestParams.actionName).cancel();
						}

					}

					Thread.sleep(10);

			}
	   		}catch (ModuleException |IOException| InterruptedException  e) {
				e.printStackTrace();
			}

	   }

}



class Servo_Controller extends Action{
	   String TAG="";
	   CRobotPose pose;
	   private static HashMap<Byte, Servo_Controller> instanceMap =new HashMap<Byte, Servo_Controller>();

		Byte key;

		public Servo_Controller(Byte key) {
				this.key=key;
				TAG="Servo controller:"+Moter.moter_map.get(key);
		}

		//シングルトン化
		public static Servo_Controller getInstance(Byte key) {
			Servo_Controller instance = instanceMap.get(key);
			if (instance != null)
				return instance;
			instance = new Servo_Controller(key);
			instanceMap.put(key, instance);
			return instance;
		}


		@Override
		public void play(RequestParams requestParams){
			//一部の軸を指定して動作
			//CSotaMotionの定数を利用してID指定する場合
			pose = new CRobotPose();
			int angle=0;
			if(key==CSotaMotion2.SV_HEAD_P || key==CSotaMotion2.SV_R_SHOULDER || key==CSotaMotion2.SV_R_ELBOW){
				angle=-(int)(requestParams.points[0]*20);
			}else{
				angle=(int)(requestParams.points[0]*20);
			}
			angle=Math.max(angle, Moter.min[key]);
			angle=Math.min(angle, Moter.max[key]);
			pose.SetPose(new Byte[] {key}	//id
						,  new Short[]{(short)angle}	//target pos
			);
			CRobotUtil.Log(TAG, "angle:"+String.valueOf(requestParams.points[0]));
			Moter.play(pose,(int)requestParams.duration);
		}

		@Override
		public void cancel(){

		}

	}

class Speaking_Controller extends Action{
	   String TAG="Speaking controller";
	   private static Speaking_Controller instance ;

	   final static CPlayWave2 cPlayWave2=new CPlayWave2();
	   public static String speaking_text="";

		public Speaking_Controller() {
		}

		//シングルトン化
		public static Speaking_Controller getInstance() {
			if (instance != null)
				return instance;
			instance = new Speaking_Controller();
			return instance;
		}

		public static boolean isSpeaking(){
			return cPlayWave2.isPlaying2();
		}

		@Override
		public void play(RequestParams requestParams){
			if(requestParams.content.length()>0){
			byte[] data =  TextToSpeechSota2.getTTS(requestParams.content);
			if(data == null){
				return ;
			}
			CRobotUtil.Log(TAG, "contents:"+requestParams.content);
				cPlayWave2.PlayWave(data);
				speaking_text=requestParams.content;
			}

		}

		@Override
		public void cancel() {
			speaking_text="途中終了:"+speaking_text;
			cPlayWave2.stop();
		}

	}


class LED_Controller extends Action{
	   String TAG = "LED controller";
	   CRobotPose pose;
	   private static LED_Controller instance ;
		Color Eye_Color=new Color(0,0,255);

		public LED_Controller() {

		}

		//シングルトン化
		public static LED_Controller getInstance() {
			if (instance != null)
				return instance;
			instance = new LED_Controller();
			return instance;
		}


		@Override
		public void play(RequestParams requestParams){
			Eye_Color=new Color((int)requestParams.points[0],(int)requestParams.points[1],(int)requestParams.points[2]);
			pose = new CRobotPose();
			pose.setLED_Sota(Eye_Color, Eye_Color, 255, Color.GREEN);
			Moter.play(pose,(int)requestParams.duration);
		}

		@Override
		public void cancel(){

		}

	}

	class Action{
		public void play(RequestParams requestParams) {

		}
		public void cancel() {

		}
	}



