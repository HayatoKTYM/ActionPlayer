package jp.vstone.sotaactionplayer;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.BiConsumer;

import org.monea.api.LocalModuleProxy;
import org.monea.api.ModuleContext;
import org.monea.api.ModuleException;
import org.monea.api.ProcessingRequest;
import org.monea.api.ProcessingRequest.Param;
import org.monea.api.ProcessingRequestQueue;

import jp.vstone.RobotLib.CRobotPose;
import jp.vstone.RobotLib.CRobotUtil;


public class SotaActionPlayer {

	static final String TAG = "ActionPlayer";
	public static HashMap<String, Action> Basic_motion_map = new HashMap<String,Action >();

	public static HashMap<String, Integer> Current_angle_map = new HashMap<String,Integer >();


	public static void main(String[] args) throws ModuleException, IOException, InterruptedException {
	//System.setProperty("REGISTRY_SERVER_PORT","25001");
	Moter.init();

	Moter.max[CSotaMotion2.SV_BODY_Y]=600;
	Moter.min[CSotaMotion2.SV_BODY_Y]=-600;

	CRobotUtil.Log(TAG, "Servo Init");

	CRobotPose pose = new CRobotPose();
	//LEDを点灯（左目：赤、右目：赤、口：Max、電源ボタン：赤）
	pose.setLED_Sota(Color.RED, Color.RED, 255, Color.GREEN);

	CRobotUtil.Log(TAG, "play:" + Moter.play(pose,300));


	//ImageServer imageServer=new ImageServer();
	//imageServer.setDaemon(true);
	//imageServer.start();

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

	new Status_Check(localModule);

    //シャットダウンフックを登録します。
    Runtime.getRuntime().addShutdownHook(new Thread(){
    		public void run(){
    		terminate();
    	}}
     );

    Map<Byte, Short> c_led;
    boolean flag=false;

	Current_angle_map.put("head_p", (int)Moter.getReadPose(CSotaMotion2.SV_HEAD_P));
	Current_angle_map.put("head_y", (int)Moter.getReadPose(CSotaMotion2.SV_HEAD_Y) / 2);
	Current_angle_map.put("head_r", (int)Moter.getReadPose(CSotaMotion2.SV_HEAD_R));
	Current_angle_map.put("body_y", (int)Moter.getReadPose(CSotaMotion2.SV_BODY_Y) / 2);
	Current_angle_map.put("position_y", 0);
	Current_angle_map.put("position_x", 0);
	Current_angle_map.put("l_sho", (int)Moter.getReadPose(CSotaMotion2.SV_L_SHOULDER));
	Current_angle_map.put("l_elb", (int)Moter.getReadPose(CSotaMotion2.SV_L_ELBOW));
	Current_angle_map.put("r_sho", (int)Moter.getReadPose(CSotaMotion2.SV_R_SHOULDER));
	Current_angle_map.put("r_elb", (int)Moter.getReadPose(CSotaMotion2.SV_R_ELBOW));
	c_led = Moter.getReadLED();
	Current_angle_map.put("led_b", (int)c_led.get((byte)0));
	Current_angle_map.put("led_g", (int)c_led.get((byte)1));
	Current_angle_map.put("led_r", (int)c_led.get((byte)2));

	String[] name_set = Current_angle_map.keySet().toArray(new String[0]);
	String[] angles_set = new String[]{"","","","","","","","","","","","","","","","","","","","","",""};

    //メインスレッドをmonea公開用に使用
	while(true){
		if(Moter.isButton_Power()){
			break;
		}

		Current_angle_map.put("head_p", (int)Moter.getReadPose(CSotaMotion2.SV_HEAD_P));
		Current_angle_map.put("head_y", (int)Moter.getReadPose(CSotaMotion2.SV_HEAD_Y) / 2 );
		Current_angle_map.put("head_r", (int)Moter.getReadPose(CSotaMotion2.SV_HEAD_R));
		Current_angle_map.put("body_y", (int)Moter.getReadPose(CSotaMotion2.SV_BODY_Y) / 2 );
		Current_angle_map.put("position_y", 0);
		Current_angle_map.put("position_x", 0);
		Current_angle_map.put("l_sho", (int)Moter.getReadPose(CSotaMotion2.SV_L_SHOULDER));
		Current_angle_map.put("l_elb", (int)Moter.getReadPose(CSotaMotion2.SV_L_ELBOW));
		Current_angle_map.put("r_sho", (int)Moter.getReadPose(CSotaMotion2.SV_R_SHOULDER));
		Current_angle_map.put("r_elb", (int)Moter.getReadPose(CSotaMotion2.SV_R_ELBOW));
		c_led = Moter.getReadLED();
		Current_angle_map.put("led_b", (int)c_led.get((byte)0));
		Current_angle_map.put("led_g", (int)c_led.get((byte)1));
		Current_angle_map.put("led_r", (int)c_led.get((byte)2));




		if(Speaking_Controller.isSpeaking()){
			flag=true;
		}else{
			if(flag){
				localModule.setAsString("speaked_contents", Speaking_Controller.speaking_text);
				flag=false;
			}
		}

		for(int i=0;i<11;i++){
			String name = name_set[i].trim();
			angles_set[i*2]=name;
			if(name.equals("head_p") || name.equals("r_sho") || name.equals("r_elb") )
			{
				angles_set[i*2+1]=String.valueOf(-Current_angle_map.get(name)/10);
			}else{
				angles_set[i*2+1]=String.valueOf(Current_angle_map.get(name)/10);
			}
		}
		if(angles_set!=null){
			localModule.setAsStringArray("angle", angles_set);
		}
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


class Status_Check extends Thread{

static final String TAG = "Status_Check";
public static HashMap<String, String> statuses = new HashMap<String,String >();
Queue<String[]> ready_queue = new ArrayDeque<String[]>();
static LocalModuleProxy localModule;

	public Status_Check(LocalModuleProxy localModule) {
	   this.localModule=localModule;
       this.setDaemon(true);
       this.start();
	}

public void run() {
	CRobotUtil.Log(TAG, "thread start");
	while(true){
		ready_queue.clear();
		SotaActionPlayer.Basic_motion_map.forEach(new BiConsumer<String, Action>(){
		@Override
	    public void accept(String actionName, Action action) {
	        if(statuses.containsKey(actionName) && statuses.get(actionName).trim().contains("play")){
	        	if(actionName.contains("led")){
	        		if(Math.abs(action.Eye_Color.getBlue()-SotaActionPlayer.Current_angle_map.get("led_b"))+Math.abs(action.Eye_Color.getGreen()-SotaActionPlayer.Current_angle_map.get("led_g"))+Math.abs(action.Eye_Color.getRed()-SotaActionPlayer.Current_angle_map.get("led_r"))<1){
	        			CRobotUtil.Log(actionName, "完了");
	        			setStatus(actionName, "ready",action.epock);

	        		}
	        	}else if(actionName.contains("speak")){
	        		if(! Speaking_Controller.isSpeaking()){
	        			CRobotUtil.Log(actionName, "完了");
	        			setStatus(actionName, "ready",action.epock);
	        			updata();
	        		}
	        	}else{
	        		if(actionName.contains("head_y") || actionName.contains("body_y")){
						if(Math.abs(action.angle-SotaActionPlayer.Current_angle_map.get(actionName)*2)<30){
							CRobotUtil.Log(actionName, "完了");
							setStatus(actionName, "ready",action.epock);
			        		updata();
			        	}
	        		}else{
						if(Math.abs(action.angle-SotaActionPlayer.Current_angle_map.get(actionName))<30){
							CRobotUtil.Log(actionName, "完了");
							setStatus(actionName, "ready",action.epock);
			        		updata();
			        	}
	        		}
	        	}

	        }
	    }});
		try {
			updata();
			Thread.sleep(50);
		} catch (InterruptedException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}


		}
	}

	static void updata(){
		try {
			localModule.setAsStringArray("status", statuses.toString().replace("{", "").replace("}", "").split("=|,"));
			localModule.commit();
		} catch (ModuleException | IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}

	synchronized static void setStatus(String actionName, String status, int epock){
		CRobotUtil.Log(TAG,actionName+":"+status+":"+String.valueOf(epock));
		statuses.put(actionName, status+":"+String.valueOf(epock));
	}
}

/**
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
**/

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
							}else if(param.name().equals("epock")){
								requestParams.epock=param.toInteger32();
							}
						}
						CRobotUtil.Log(TAG, "Queue:"+item.getProcessingName()+" actioName: "+requestParams.actionName);

						if (item.getProcessingName().equals("play")){
							if(SotaActionPlayer.Basic_motion_map.containsKey(requestParams.actionName)){
							  SotaActionPlayer.Basic_motion_map.get(requestParams.actionName).play(requestParams);
							}else{
								Status_Check.setStatus(requestParams.actionName,"ready",requestParams.epock);
								Status_Check.updata();
							}

						}else if(item.getProcessingName().equals("cancel")){
							if(SotaActionPlayer.Basic_motion_map.containsKey(requestParams.actionName)){
							  SotaActionPlayer.Basic_motion_map.get(requestParams.actionName).cancel();
							}
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
			epock=requestParams.epock;
			Status_Check.setStatus(requestParams.actionName,"play",epock);
			//一部の軸を指定して動作
			//CSotaMotionの定数を利用してID指定する場合
			pose = new CRobotPose();
			if(key==CSotaMotion2.SV_HEAD_P || key==CSotaMotion2.SV_R_SHOULDER || key==CSotaMotion2.SV_R_ELBOW){
				angle=-(int)(requestParams.points[0]*10);
			}else{
				angle=(int)(requestParams.points[0]*10);
			}
			if(requestParams.actionName.equals("head_y") || requestParams.actionName.equals("body_y")){
				angle=angle*2;
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
			//現在角度で固定
			pose = new CRobotPose();

			pose.SetPose(new Byte[] {key}	//id
			,  new Short[]{(short)Moter.getReadPose(key)}	//target pos
);
			Moter.play(pose,100);
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
			String path =  TextToSpeechSota2.getTTSFile(requestParams.content, (int)((1400-requestParams.duration)/100), 13, 11);
			if(path == null){
				return ;
			}
			CRobotUtil.Log(TAG, "contents:"+requestParams.content);
			if(isSpeaking()){
				cancel();
			}
				cPlayWave2.PlayWave(path);
				speaking_text=requestParams.content;
			}
			epock=requestParams.epock;
			Status_Check.setStatus(requestParams.actionName,"play",epock);

		}

		@Override
		public void cancel() {
			speaking_text="interrupt:"+speaking_text;
			cPlayWave2.stop();
		}

	}


class LED_Controller extends Action{
	   String TAG = "LED controller";
	   CRobotPose pose;
	   private static LED_Controller instance ;


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
			epock=requestParams.epock;
			Status_Check.setStatus(requestParams.actionName,"play",epock);
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
		int angle=0;
		int epock=0;
		Color Eye_Color=new Color(0,0,255);
		public void play(RequestParams requestParams) {

		}

		public void cancel() {

		}
	}



