package jp.vstone.sotaactionplayer;


import java.util.HashMap;

import jp.vstone.RobotLib.CRobotPose;

public class CSotaMotion2 extends CRobotMotion2 {
	public static final byte SV_BODY_Y = 1;
	public static final byte SV_L_SHOULDER = 2;
	public static final byte SV_L_ELBOW = 3;
	public static final byte SV_R_SHOULDER = 4;
	public static final byte SV_R_ELBOW = 5;
	public static final byte SV_HEAD_Y = 6;
	public static final byte SV_HEAD_P = 7;
	public static final byte SV_HEAD_R = 8;

	public CSotaMotion2(CRobotMem2 mem) {
		super(mem);
	}

	public boolean InitRobot_Sota() {
		return super.InitRobot();
	}

	public static CRobotPose getInitPose() {
		CRobotPose pose = new CRobotPose();
		pose.SetPose(
				new Byte[] { Byte.valueOf((byte) 1), Byte.valueOf((byte) 2), Byte.valueOf((byte) 3), Byte.valueOf((byte) 4), Byte.valueOf((byte) 5),
						Byte.valueOf((byte) 6), Byte.valueOf((byte) 7), Byte.valueOf((byte) 8) },
				new Short[] { Short.valueOf((short) 0), Short.valueOf((short) 64636), Short.valueOf((short) 0), Short.valueOf((short) 900),
						Short.valueOf((short)0), Short.valueOf((short) 0), Short.valueOf((short) 0), Short.valueOf((short) 0) });

		return pose;
	}



	public HashMap<Byte, Short> getSpeechRecogLEDColor() {
		HashMap<Byte, Short> map = new HashMap();
		map.put(Byte.valueOf((byte) 8), Short.valueOf((short) 60));
		map.put(Byte.valueOf((byte) 9), Short.valueOf((short) 255));
		map.put(Byte.valueOf((byte) 10), Short.valueOf((short) 200));

		map.put(Byte.valueOf((byte) 11), Short.valueOf((short) 60));
		map.put(Byte.valueOf((byte) 12), Short.valueOf((short) 255));
		map.put(Byte.valueOf((byte) 13), Short.valueOf((short) 200));

		return map;
	}
}
