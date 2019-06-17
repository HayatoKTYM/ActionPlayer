package jp.vstone.sotasample;

import java.util.HashMap;
import java.util.Map;

import jp.vstone.RobotLib.CRobotPose;
import jp.vstone.RobotLib.CRobotUtil;

class Moter{

	final static private CRobotMem2 mem = new CRobotMem2();
	final static private CSotaMotion2 motion =new CSotaMotion2(mem);
	static final String TAG = "Moter";
	static boolean isInit=false;
	static boolean result;
	static short[] min;
	static short[] max;

	static CRobotPose Pose;

	static public HashMap<Object, String> moter_map=new HashMap<Object,String>();


	public static synchronized void init(){
		if(! isInit){
			moter_map.put(CSotaMotion2.SV_HEAD_Y, "HEAD_Y");
			moter_map.put(CSotaMotion2.SV_HEAD_P, "HEAD_P");
			moter_map.put(CSotaMotion2.SV_HEAD_R, "HEAD_R");
			moter_map.put(CSotaMotion2.SV_BODY_Y, "BODY_Y");
			moter_map.put(CSotaMotion2.SV_L_ELBOW, "L_ELBOW");
			moter_map.put(CSotaMotion2.SV_R_ELBOW, "R_ELBOW");
			moter_map.put(CSotaMotion2.SV_L_SHOULDER, "L_SHOULDER");
			moter_map.put(CSotaMotion2.SV_R_SHOULDER, "R_SHOULDER");
		   	if(mem.Connect()){
			   	//Sota仕様にVSMDを初期化
			   	motion.InitRobot_Sota();
			   	max=motion.max;
			   	min=motion.min;
			   	//サーボモータを現在位置でトルクOnにする
			   	CRobotUtil.Log(TAG, "Servo On");
			   	motion.ServoOn();
			   	motion.EnableCollidionDetect();
			   	isInit=true;
		   	}
		}
	}

	public static synchronized int getReadPose(Object key){
		Pose = motion.getReadPose();
		Map<Byte, Short> cPose= Pose.getPose();
		return (int)cPose.get(key);
	}

	public static synchronized Map<Byte, Short> getReadLED(){
		Pose = motion.getReadPose();
		return Pose.getLed();
	}

	public static synchronized int getTargetPose(Object key){
		Pose = motion.getTargetPose();
		Map<Byte, Short> cPose= Pose.getPose();
		return (int)cPose.get(key);
	}

	public static synchronized Map<Byte, Short> getTargetLED(){
		Pose = motion.getTargetPose();
		return Pose.getLed();
	}

	public static synchronized boolean play(CRobotPose pose,int dur){
		result = motion.play(pose,dur);
		//motion.waitEndinterpAll();
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		return result;
	}

	public static synchronized boolean isButton_Power(){
		return motion.isButton_Power();
	}

	public static synchronized void ServoOff(){
		if(mem.Connect()){
		CRobotUtil.Log(TAG, "Servo Off");
		motion.ServoOff();
		}
	}


}