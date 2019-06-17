package jp.vstone.sotaactionplayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import jp.vstone.RobotLib.CMemDefU16;
import jp.vstone.RobotLib.CRoboSetting;
import jp.vstone.RobotLib.CRobotPose;
import jp.vstone.RobotLib.CRobotUtil;
import jp.vstone.RobotLib.CServoSetting;

public abstract class CRobotMotion2 {
	static final String TAG = "CRobotMotion2";
	static final boolean D = false;
	static final double MasterCtrlPeriodDef = 16666.6666667D;
	public CRobotMem2 robomem;
	ArrayList<CServoSetting> ServoSetting = new ArrayList();
	public ArrayList<CRobotMem2.I2CDeviceDefine> LEDDriverDefines;
	public CRobotMem2.I2CDeviceDefine InterigentMicDefine;
	public CRobotMem2.I2CDeviceDefine BatteryChargerDefine;
	public volatile Byte[] allids = { Byte.valueOf((byte)0), Byte.valueOf((byte)1), Byte.valueOf((byte)2), Byte.valueOf((byte)3),
			Byte.valueOf((byte)4), Byte.valueOf((byte)5), Byte.valueOf((byte)6), Byte.valueOf((byte)7), Byte.valueOf((byte)8), Byte.valueOf((byte)9),
			Byte.valueOf((byte)10), Byte.valueOf((byte)11), Byte.valueOf((byte)12), Byte.valueOf((byte)13), Byte.valueOf((byte)14), Byte.valueOf((byte)15),
			Byte.valueOf((byte)16) };
	volatile long HandleDefault = -1L;
	InterpLockerClient2 interpLockerClient;
	private boolean endflag = false;

	public CRobotMotion2(CRobotMem2 mem) {
		CRobotUtil.setDebugOut("CRobotMotion2", Boolean.valueOf(false));
		this.robomem = mem;
		this.interpLockerClient = InterpLockerClient2.createInstance();
	}

	public boolean InitRobot() {
		CRoboSetting robosetting = CRoboSetting.LoadSettingFile();
		if (robosetting == null) {
			return false;
		}
		boolean ret = InitRobot(robosetting);
		return ret;
	}

	public boolean InitRobot(ArrayList<CServoSetting> servosettingarray) {
		return InitRobot(1, servosettingarray);
	}

	public boolean InitRobot(CRoboSetting setting) {
		boolean ret = InitRobot(setting.ServoBusProtocol, setting.ServoSettings);
		initI2C(setting.I2CDeviceSettings);
		return ret;
	}

	public boolean InitRobot(int ServoBusProtocol, ArrayList<CServoSetting> servosettingarray) {
		return InitRobot(ServoBusProtocol, (CServoSetting[]) servosettingarray.toArray(new CServoSetting[0]));
	}

	public boolean InitRobot(int ServoBusProtocol, CServoSetting[] servosettingarray) {
		Runtime.getRuntime().addShutdownHook(new Shutdown());
		if (this.robomem.FirmwareRev.get().intValue() < 20) {
			CRobotUtil.Log("CRobotMotion2", "Error:Firmware is old revision. please update vsmd now");
			return false;
		}
		initServo(ServoBusProtocol, servosettingarray);

		lockServoandLed_forInit();

		CRobotPose pose = new CRobotPose();
		Short[] tpos = new Short[this.allids.length];
		Arrays.fill(tpos, Short.valueOf((byte)(short) 100));
		pose.SetTorque(getDefaultIDs(), tpos);

		CRobotUtil.Debug("CRobotMotion2", "play pose");
		play(pose, 100);

		CRobotUtil.Debug("CRobotMotion2", "wait");
		CRobotUtil.wait(1000);

		CRobotUtil.Debug("CRobotMotion2", "return true");
		return true;
	}

	private void initServo(int ServoBusProtocol, CServoSetting[] servosettingarray) {
		this.robomem.ServoBusNum.set(servosettingarray.length);
		this.allids = new Byte[servosettingarray.length];

		short[] offset = new short[this.robomem.InterpServoPosOutput.length];

		Arrays.fill(this.allids, Byte.valueOf((byte)(byte) 0));

		CRobotUtil.Debug("CRobotMotion2", "FaceDetectTask " + Thread.currentThread().getId());
		this.HandleDefault = Thread.currentThread().getId();
		for (int i = 0; i < servosettingarray.length; i++) {
			CServoSetting svset = servosettingarray[i];
			this.allids[i] = Byte.valueOf((byte)(byte) svset.id);
			offset[svset.id] = ((short) svset.offset);
		}
		this.robomem.MasterCtrlPeriod.set(16666L);
		try {
			CRobotUtil.Log("CRobotMotion2", "MasterCtrlPeriod " + this.robomem.MasterCtrlPeriod.get());
		} catch (Exception e) {
			e.printStackTrace();
		}
		setServoReadandLimit(servosettingarray);

		byte[] preids = new byte[this.allids.length];
		for (int i = 0; i < this.allids.length; i++) {
			preids[i] = this.allids[i].byteValue();
		}
		this.robomem.ServoBusIDs.set(preids);

		this.robomem.ServoBusProtocol.set(ServoBusProtocol);
		this.robomem.ServoSendEN.set(1);
		this.robomem.ServoEN.set(0);

		this.robomem.ServoBusReadProtocol0.set(ServoBusProtocol);
		this.robomem.ServoBusReadProtocol1.set(ServoBusProtocol);
		this.robomem.ServoBusReadProtocol2.set(ServoBusProtocol);
		this.robomem.ServoBusReadProtocol3.set(ServoBusProtocol);

		this.robomem.ServoOffset.set(offset);
	}

	short[] max;
	short[] min;

	public void setServoReadandLimit(CServoSetting[] servosettingarray) {
		max = new short[this.robomem.InterpServoPosOutput.length];
		min = new short[this.robomem.InterpServoPosOutput.length];
		byte[][] rdpos = new byte[4][this.robomem.ServoBusReadIDs0.length];
		int[] rdposcnt = new int[4];

		Arrays.fill(rdpos[0], (byte) 0);
		Arrays.fill(rdpos[1], (byte) 0);
		Arrays.fill(rdpos[2], (byte) 0);
		Arrays.fill(rdpos[3], (byte) 0);
		for (int i = 0; i < servosettingarray.length; i++) {
			CServoSetting svset = servosettingarray[i];
			max[svset.id] = ((short) svset.max);
			min[svset.id] = ((short) svset.min);
			if (svset.readAngleEn) {
				rdpos[svset.readAngleBank][rdposcnt[svset.readAngleBank]] = this.allids[i].byteValue();
				rdposcnt[svset.readAngleBank] += 1;
			}
		}
		this.robomem.ServoPosLimitHigh.set(max);
		this.robomem.ServoPosLimitLow.set(min);

		this.robomem.ServoBusReadNum0.set(rdposcnt[0]);
		this.robomem.ServoBusReadIDs0.set(rdpos[0]);

		this.robomem.ServoBusReadNum1.set(rdposcnt[1]);
		this.robomem.ServoBusReadIDs1.set(rdpos[1]);

		this.robomem.ServoBusReadNum2.set(rdposcnt[2]);
		this.robomem.ServoBusReadIDs2.set(rdpos[2]);

		this.robomem.ServoBusReadNum3.set(rdposcnt[3]);
		this.robomem.ServoBusReadIDs3.set(rdpos[3]);
	}

	private void initI2C(CRoboSetting.I2CDeviceSetting[] i2csettings) {
		this.LEDDriverDefines = new ArrayList();
		int startaddress = 256;
		for (int i = 0; i < i2csettings.length; i++) {
			CRoboSetting.I2CDeviceSetting i2csetting = i2csettings[i];
			switch (i2csetting.type) {
			case 1:
				this.LEDDriverDefines
						.add(new CRobotMem2.I2CDeviceDefine(this.robomem.getSock(), startaddress, i2csetting.size));
				break;
			case 2:
				this.InterigentMicDefine = new CRobotMem2.I2CDeviceDefine(this.robomem.getSock(), startaddress,
						i2csetting.size);
				break;
			case 3:
				this.BatteryChargerDefine = new CRobotMem2.I2CDeviceDefine(this.robomem.getSock(), startaddress,
						i2csetting.size);
				break;
			}
			startaddress += i2csetting.size;
		}
	}

	private void lockServoandLed_forInit() {
		Byte[] ledid = new Byte[16];
		for (byte i = 0; i < 16; i = (byte) (i + 1)) {
			ledid[i] = new Byte(i);
		}
		LockServoHandle(getDefaultIDs());
		LockLEDHandle(ledid);
	}

	class Shutdown extends Thread {
		Shutdown() {
		}

		public void run() {
			CRobotMotion2.this.lockServoandLed_forInit();
			CRobotMotion2.this.ServoOff();
			CRobotMotion2.this.endflag = true;
		}
	}

	public Byte[] getDefaultIDs() {
		return this.allids;
	}

	public String getThreadkey() {
		return getClass().getName();
	}

	public boolean LockServoHandle(Byte[] ids) {
		return LockServoHandle(getThreadkey(), ids);
	}

	public boolean LockLEDHandle(Byte[] ids) {
		return LockLEDHandle(getThreadkey(), ids);
	}

	public boolean LockServoHandle(String LockKey, Byte[] ids) {
		CRobotUtil.Debug("CRobotMotion2", "LockServoHandle:" + LockKey + " :" + CRobotUtil.calledFrom());
		return this.interpLockerClient.LockHandle(LockKey, ids, true);
	}

	public boolean LockLEDHandle(String LockKey, Byte[] ids) {
		CRobotUtil.Debug("CRobotMotion2", "LockLEDHandle:" + LockKey + " :" + CRobotUtil.calledFrom());
		return this.interpLockerClient.LockHandle(LockKey, ids, false);
	}

	public void UnLockServoHandle(Byte[] ids) {
		this.interpLockerClient.UnLockHandle(getThreadkey(), ids, true);
	}

	public void UnLockLEDHandle(Byte[] ids) {
		this.interpLockerClient.UnLockHandle(getThreadkey(), ids, false);
	}

	public void UnLockServoHandle(String LockKey, Byte[] ids) {
		CRobotUtil.Debug("CRobotMotion2", "UnLockServoHandle:" + LockKey + " :" + CRobotUtil.calledFrom());
		this.interpLockerClient.UnLockHandle(LockKey, ids, true);
	}

	public void UnLockLEDHandle(String LockKey, Byte[] ids) {
		CRobotUtil.Debug("CRobotMotion2", "UnLockLEDHandle:" + LockKey + " :" + CRobotUtil.calledFrom());
		this.interpLockerClient.UnLockHandle(LockKey, ids, false);
	}

	private boolean playing = false;

	public boolean play(CRobotPose pose, int msec) {
		return play(pose, msec, getThreadkey());
	}

	public synchronized boolean play(CRobotPose pose, int msec, String LockKey) {
		return play(pose, msec, LockKey, false);
	}

	public synchronized boolean play(CRobotPose pose, int msec, String LockKey, boolean ledposcheck) {
		while ((this.playing) && (!this.endflag)) {
			CRobotUtil.wait(1);
		}
		this.playing = true;
		boolean b_play = false;
		boolean b_servo = false;
		if (SetPose(pose.getPose())) {
			b_play = true;
		}
		if (SetTorque(pose.getTorque())) {
			b_play = true;
		}
		if (SetLed(pose.getLed(), ledposcheck)) {
			b_play = true;
		}
		CRobotUtil.Debug("CRobotMotion2", "b_play " + b_play);
		if (b_play) {
			CRobotUtil.Debug("CRobotMotion2", "msec " + msec);
			if (msec == 0) {
				msec = 1;
			}
			long interp = (long) (msec * 1000.0D / 16666.6666667D);
			if (interp > 65535L) {
				interp = 65535L;
			}
			Short addr = this.interpLockerClient.convLockKeytoTimerAddress(this.robomem, LockKey);
			if (addr == null) {
				this.playing = false;
				return false;
			}
			CRobotUtil.Debug("CRobotMotion2", "Lock pos " + addr);

			CRobotUtil.Debug("CRobotMotion2", "set InterpTargetTimeList pos:" + addr + " value:" + interp);
			new CMemDefU16(this.robomem.sock, addr.shortValue()).set((int) interp);

			CRobotUtil.Debug("CRobotMotion2", "InterpTargetTimeList pos:" + addr);

			this.playing = false;
			return true;
		}
		this.playing = false;
		return false;
	}

	public boolean isEndInterpAll() {
		Short addr = this.interpLockerClient.convLockKeytoTimerAddress(this.robomem, getThreadkey());
		if (addr == null) {
			return false;
		}
		Integer sum = Integer.valueOf((byte)0);
		try {
			Integer[] led = this.robomem.InterpLEDRemainingTime.get();
			Short[] led_pt = this.robomem.InterpLEDTriggerPointer.get();
			if (led == null) {
				return false;
			}
			Integer[] servop = this.robomem.InterpServoPosRemainingTime.get();
			Short[] servop_pt = this.robomem.InterpServoPosTriggerPointer.get();
			if (servop == null) {
				return false;
			}
			Integer[] servot = this.robomem.InterpServoTorqeRemainingTime.get();
			Short[] servot_pt = this.robomem.InterpServoTorqeTriggerPointer.get();
			if (servot == null) {
				return false;
			}
			for (int i = 0; i < led.length; i++) {
				if (led_pt[i].shortValue() == addr.shortValue()) {
					sum = Integer.valueOf((byte)sum.intValue() | led[i].intValue());
				}
			}
			CRobotUtil.Debug("CRobotMotion2", "led sum " + sum);
			for (int i = 0; i < servop.length; i++) {
				if (servop_pt[i].shortValue() == addr.shortValue()) {
					sum = Integer.valueOf((byte)sum.intValue() | servop[i].intValue());
				}
			}
			for (int i = 0; i < servot.length; i++) {
				if (servot_pt[i].shortValue() == addr.shortValue()) {
					sum = Integer.valueOf((byte)sum.intValue() | servot[i].intValue());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		CRobotUtil.Debug("CRobotMotion2", "isEndInterpAll addr:" + addr + " remains:" + sum);
		if (sum.intValue() == 0) {
			return true;
		}
		return false;
	}

	public void waitEndinterpAll() {
		long start = Calendar.getInstance().getTimeInMillis();

		Short addr = this.interpLockerClient.convLockKeytoTimerAddress(this.robomem, getThreadkey());
		try {
			int timeout = 10;
			CMemDefU16 memu16 = new CMemDefU16(this.robomem.sock, addr.shortValue());
			for (;;) {
				Integer ret = memu16.get();
				if (ret == null) {
					return;
				}
				if ((ret.intValue() == 0) || (ret.intValue() == 65535) || (this.endflag)) {
					CRobotUtil.Debug("CRobotMotion2", "timer set end.: " + ret);
					break;
				} else {
					CRobotUtil.Debug("CRobotMotion2", "wait set timer. now : " + ret);
					CRobotUtil.wait(1);
					timeout--;
					if (timeout < 0) {
						break;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		int timeout = 15000;
		while (!this.endflag) {
			if (isEndInterpAll()) {
				long end = Calendar.getInstance().getTimeInMillis();
				CRobotUtil.Debug("CRobotMotion2", "waitEndinterpAll " + (end - start));
				return;
			}
			CRobotUtil.wait(5);

			timeout--;
			if (timeout < 0) {
				break;
			}
		}
	}

	private synchronized boolean SetPose(Map<Byte, Short> map) {
		boolean ret = true;
		if (map == null) {
			return false;
		}
		Byte[] keys = (Byte[]) map.keySet().toArray(new Byte[0]);
		for (int i = 0; i < keys.length; i++) {
			CRobotUtil.Debug("CRobotMotion2",
					"Write pose id :" + keys[i] + " , deg :" + ((Short) map.get(keys[i])).shortValue());
			if (!this.robomem.InterpServoPosTarget.set(((Short) map.get(keys[i])).shortValue(), keys[i].byteValue())) {
				ret = false;
			}
		}
		return ret;
	}

	private synchronized boolean SetTorque(Map<Byte, Short> map) {
		boolean ret = true;
		if (map == null) {
			return false;
		}
		Byte[] keys = (Byte[]) map.keySet().toArray(new Byte[0]);
		for (int i = 0; i < keys.length; i++) {
			if (!this.robomem.InterpServoTorqeTarget.set(((Short) map.get(keys[i])).shortValue(),
					keys[i].byteValue())) {
				ret = false;
			}
		}
		return ret;
	}

	private synchronized boolean SetLed(Map<Byte, Short> map, boolean docheack) {
		boolean ret = true;
		if (map == null) {
			return false;
		}
		Byte[] keys = (Byte[]) map.keySet().toArray(new Byte[0]);
		for (int i = 0; i < keys.length; i++) {
			if (!this.robomem.InterpLEDTarget.set(((Short) map.get(keys[i])).shortValue(), keys[i].byteValue())) {
				ret = false;
			}
		}
		if (docheack) {
			for (int i = 0; i < keys.length; i++) {
				Short readvalue = this.robomem.InterpLEDTarget.get(Integer.valueOf((byte)keys[i].byteValue()));
				if ((readvalue.shortValue() != ((Short) map.get(keys[i])).shortValue())
						&& (!this.robomem.InterpLEDTarget.set(((Short) map.get(keys[i])).shortValue(),
								keys[i].byteValue()))) {
					ret = false;
				}
			}
		}
		return ret;
	}

	public Short[] getReadpos() {
		Short[] pos = new Short[this.allids.length];
		Short[] allpos = this.robomem.ServoReadPos.get();
		if (allpos == null) {
			return null;
		}
		for (int i = 0; i < this.allids.length; i++) {
			pos[i] = allpos[this.allids[i].byteValue()];
		}
		return pos;
	}

	public CRobotPose getReadPose() {
		CRobotPose pose = new CRobotPose();
		Short[] pos = getReadpos();
		if (pos == null) {
			return null;
		}
		pose.SetPose(this.allids, pos);
		pose.SetLed(this.ledid, this.robomem.InterpLEDOutput.get());
		return pose;
	}

	private Short[] getTagetpos() {
		Short[] pos = new Short[this.allids.length];
		Short[] allpos = this.robomem.InterpServoPosTarget.get();
		if (allpos == null) {
			return null;
		}
		for (int i = 0; i < this.allids.length; i++) {
			pos[i] = allpos[this.allids[i].byteValue()];
		}
		return pos;
	}

	private Short[] getTagetTorque() {
		Short[] pos = new Short[this.allids.length];
		Short[] allpos = this.robomem.InterpServoTorqeTarget.get();
		if (allpos == null) {
			return null;
		}
		for (int i = 0; i < this.allids.length; i++) {
			pos[i] = allpos[this.allids[i].byteValue()];
		}
		return pos;
	}

	private Byte[] ledid = { Byte.valueOf((byte)0), Byte.valueOf((byte)1), Byte.valueOf((byte)2), Byte.valueOf((byte)3), Byte.valueOf((byte)4),
			Byte.valueOf((byte)5), Byte.valueOf((byte)6), Byte.valueOf((byte)7), Byte.valueOf((byte)8), Byte.valueOf((byte)9), Byte.valueOf((byte)10),
			Byte.valueOf((byte)11), Byte.valueOf((byte)12), Byte.valueOf((byte)13), Byte.valueOf((byte)14), Byte.valueOf((byte)15) };

	public CRobotPose getTargetPose() {
		CRobotPose pose = new CRobotPose();
		Short[] pos = getTagetpos();
		if (pos == null) {
			return null;
		}
		pose.SetPose(this.allids, pos);

		pos = getTagetTorque();
		if (pos == null) {
			return null;
		}
		pose.SetTorque(this.allids, pos);

		pose.SetLed(this.ledid, this.robomem.InterpLEDTarget.get());
		return pose;
	}

	public void ServoOn() {
		Short[] pos = getReadpos();
		CRobotPose pose = new CRobotPose();
		if (pos != null) {
			for (int i = 0; i < pos.length; i++) {
				if (pos[i].shortValue() == Short.MIN_VALUE) {
					pos[i] = Short.valueOf((byte)0);
				}
			}
		} else {
			pos = new Short[getDefaultIDs().length];
			for (int i = 0; i < pos.length; i++) {
				pos[i] = Short.valueOf((byte)0);
			}
		}
		pose.SetPose(getDefaultIDs(), pos);
		play(pose, 100);
		CRobotUtil.wait(110);
		this.robomem.ServoEN.set(1);
	}

	public void ServoOff() {
		this.robomem.ServoEN.set(0);
	}

	public void EnableCollidionDetect() {
		this.robomem.CollidionDetectDisable.set(0);
	}

	public void DisableCollidionDetect() {
		this.robomem.CollidionDetectDisable.set(1);
	}

	public abstract HashMap<Byte, Short> getSpeechRecogLEDColor();

	public boolean isButton_VolUp() {
		try {
			if (this.robomem.SwitchStatus1.get().shortValue() == 1) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean isButton_VolDown() {
		try {
			if (this.robomem.SwitchStatus0.get().shortValue() == 1) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean isButton_Power() {
		try {
			if (this.robomem.SwitchStatus4.get().shortValue() == 1) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public int getBatteryVoltage() {
		Integer v = this.robomem.BatteryVoltage.get();
		if (v == null) {
			return 0;
		}
		return (int) (4.0283203125D * v.intValue());
	}

	public int getChargerAdapterVoltage() {
		Integer v = this.robomem.ChargerAdapterVoltage.get();
		if (v == null) {
			return 0;
		}
		return (int) (4.0283203125D * v.intValue());
	}

	public boolean isCharging() {
		Integer v = this.robomem.ChargerStatus.get();
		if (v == null) {
			return false;
		}
		if ((v.intValue() < 400) && (getChargerAdapterVoltage() > 10000)) {
			return true;
		}
		return false;
	}
}
