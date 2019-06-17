package jp.vstone.sotasample;


import java.util.ArrayList;

import jp.vstone.RobotLib.CMemDefArrayS16;
import jp.vstone.RobotLib.CMemDefArrayU16;
import jp.vstone.RobotLib.CMemDefArrayU8;
import jp.vstone.RobotLib.CMemDefS16;
import jp.vstone.RobotLib.CMemDefU16;
import jp.vstone.RobotLib.CMemDefU32;
import jp.vstone.RobotLib.CMemDefU8;
import jp.vstone.RobotLib.CRobotSock;

public class CRobotMem2 {
	volatile public CRobotSock sock = null;
	public CMemDefU16 FirmwareRev;
	public CMemDefU8 ModelName;
	public CMemDefU32 MasterCtrlPeriod;
	public CMemDefU16 IxbusPtr;
	public CMemDefS16 AudioVol;
	public CMemDefS16 ServoEN;
	public CMemDefS16 ServoSendEN;
	public CMemDefS16 I2CSendEN;
	public CMemDefU8 ResetREQ;
	public CMemDefU8 ShutdownREQ;
	public CMemDefS16 ServoLockDetectTime;
	public CMemDefS16 ServoLockDetectThreshold;
	public CMemDefS16 ServoLockDetectMaxTorque;
	public CMemDefS16 SwitchStatus0;
	public CMemDefS16 SwitchStatus1;
	public CMemDefS16 SwitchStatus2;
	public CMemDefS16 SwitchStatus3;
	public CMemDefS16 SwitchStatus4;
	public CMemDefU32 TripTime;
	public CMemDefU32 Random;
	public CMemDefU16 AudioOutValue;
	public CMemDefU16 AouioDiff;
	public CMemDefU16 ChargerAdapterVoltage;
	public CMemDefU16 ChargerStatus;
	public CMemDefU16 BatteryVoltage;
	public CMemDefU16 BatteryVoltageAlarmTH;
	public CMemDefU16 BatteryLowVolAlarm;
	public CMemDefS16 MicMode;
	public CMemDefS16 InterigentMicVoiceDetection;
	public CMemDefS16 InterigentMicDetectedDirection;
	public CMemDefU8 ServoBusProtocol;
	public CMemDefU8 ServoBusNum;
	public CMemDefArrayU8 ServoBusIDs;
	public CMemDefU8 ServoBusReadProtocol0;
	public CMemDefU8 ServoBusReadNum0;
	public CMemDefArrayU8 ServoBusReadIDs0;
	public CMemDefU8 ServoBusReadProtocol1;
	public CMemDefU8 ServoBusReadNum1;
	public CMemDefArrayU8 ServoBusReadIDs1;
	public CMemDefU8 ServoBusReadProtocol2;
	public CMemDefU8 ServoBusReadNum2;
	public CMemDefArrayU8 ServoBusReadIDs2;
	public CMemDefU8 ServoBusReadProtocol3;
	public CMemDefU8 ServoBusReadNum3;
	public CMemDefArrayU8 ServoBusReadIDs3;
	public CMemDefU32 ErrorAdr;
	public CMemDefArrayU8 ErrorCode;
	public CMemDefArrayU16 InterpTargetTimeList;
	public CMemDefArrayS16 InterpServoPosTarget;
	public CMemDefArrayS16 InterpServoTorqeTarget;
	public CMemDefArrayS16 InterpLEDTarget;
	public CMemDefArrayS16 InterpServoPosTriggerPointer;
	public CMemDefArrayS16 InterpServoTorqeTriggerPointer;
	public CMemDefArrayS16 InterpLEDTriggerPointer;
	public CMemDefArrayS16 InterpServoPosOutput;
	public CMemDefArrayS16 InterpServoTorqeOutput;
	public CMemDefArrayS16 InterpLEDOutput;
	public CMemDefArrayU16 InterpServoPosRemainingTime;
	public CMemDefArrayU16 InterpServoTorqeRemainingTime;
	public CMemDefArrayU16 InterpLEDRemainingTime;
	public CMemDefArrayS16 ServoReadPos;
	public CMemDefArrayS16 ServoPosLimitLow;
	public CMemDefArrayS16 ServoPosLimitHigh;
	public CMemDefArrayS16 ServoOffset;
	public CMemDefU8 CollidionDetectDisable;
	public CMemDefArrayU16 ServoCollidionStatus;
	public ArrayList<I2CDeviceDefine> I2CDeviceDefines;

	public CRobotSock getSock() {
		return this.sock;
	}

	public static class I2CDeviceDefine {
		CMemDefU8 type;
		CMemDefU8 size;
		CMemDefU8 i2cAddress;
		CMemDefU8 errorstat;
		CMemDefArrayU8 data_u8;
		CMemDefArrayU16 data_u16;

		public I2CDeviceDefine(CRobotSock sock, int _startAddress, int _size) {
			this.type = new CMemDefU8(sock, _startAddress + 0);
			this.size = new CMemDefU8(sock, _startAddress + 1);
			this.i2cAddress = new CMemDefU8(sock, _startAddress + 2);
			this.errorstat = new CMemDefU8(sock, _startAddress + 3);
			if (_size > 4) {
				this.data_u8 = new CMemDefArrayU8(sock, _startAddress + 4, _size - 4);
				this.data_u16 = new CMemDefArrayU16(sock, _startAddress + 4, (_size - 4) / 2);
			}
		}
	}

	public void addI2CDeviceDefine(I2CDeviceDefine define) {
		if (this.I2CDeviceDefines == null) {
			this.I2CDeviceDefines = new ArrayList();
		}
		this.I2CDeviceDefines.add(define);
	}

	public boolean Connect() {
		return this.sock.connect();
	}

	public void Disconnect() {
		this.sock.disconnect();
	}

	public boolean isConected() {
		return this.sock.Conected;
	}

	public CRobotMem2() {
		this.sock = new CRobotSock();

		this.FirmwareRev = new CMemDefU16(this.sock, 16);
		this.ModelName = new CMemDefU8(this.sock, 32);
		this.MasterCtrlPeriod = new CMemDefU32(this.sock, 64);
		this.IxbusPtr = new CMemDefU16(this.sock, 68);
		this.AudioVol = new CMemDefS16(this.sock, 70);
		this.ServoEN = new CMemDefS16(this.sock, 72);
		this.ServoSendEN = new CMemDefS16(this.sock, 74);
		this.I2CSendEN = new CMemDefS16(this.sock, 76);
		this.ResetREQ = new CMemDefU8(this.sock, 80);
		this.ShutdownREQ = new CMemDefU8(this.sock, 81);
		this.SwitchStatus0 = new CMemDefS16(this.sock, 96);
		this.SwitchStatus1 = new CMemDefS16(this.sock, 98);
		this.SwitchStatus2 = new CMemDefS16(this.sock, 100);
		this.SwitchStatus3 = new CMemDefS16(this.sock, 102);
		this.SwitchStatus4 = new CMemDefS16(this.sock, 104);

		this.TripTime = new CMemDefU32(this.sock, 128);
		this.Random = new CMemDefU32(this.sock, 132);
		this.AudioOutValue = new CMemDefU16(this.sock, 136);
		this.AouioDiff = new CMemDefU16(this.sock, 138);

		this.BatteryVoltage = new CMemDefU16(this.sock, 144);
		this.BatteryVoltageAlarmTH = new CMemDefU16(this.sock, 146);
		this.BatteryLowVolAlarm = new CMemDefU16(this.sock, 148);
		this.ChargerAdapterVoltage = new CMemDefU16(this.sock, 112);
		this.ChargerStatus = new CMemDefU16(this.sock, 114);

		this.MicMode = new CMemDefS16(this.sock, 152);
		this.InterigentMicVoiceDetection = new CMemDefS16(this.sock, 154);
		this.InterigentMicDetectedDirection = new CMemDefS16(this.sock, 156);

		this.ServoBusProtocol = new CMemDefU8(this.sock, 160);
		this.ServoBusNum = new CMemDefU8(this.sock, 161);
		this.ServoBusIDs = new CMemDefArrayU8(this.sock, 162, 29);
		this.ServoBusReadProtocol0 = new CMemDefU8(this.sock, 192);
		this.ServoBusReadNum0 = new CMemDefU8(this.sock, 193);
		this.ServoBusReadIDs0 = new CMemDefArrayU8(this.sock, 194, 14);
		this.ServoBusReadProtocol1 = new CMemDefU8(this.sock, 208);
		this.ServoBusReadNum1 = new CMemDefU8(this.sock, 209);
		this.ServoBusReadIDs1 = new CMemDefArrayU8(this.sock, 210, 14);
		this.ServoBusReadProtocol2 = new CMemDefU8(this.sock, 224);
		this.ServoBusReadNum2 = new CMemDefU8(this.sock, 225);
		this.ServoBusReadIDs2 = new CMemDefArrayU8(this.sock, 226, 14);
		this.ServoBusReadProtocol3 = new CMemDefU8(this.sock, 240);
		this.ServoBusReadNum3 = new CMemDefU8(this.sock, 241);
		this.ServoBusReadIDs3 = new CMemDefArrayU8(this.sock, 242, 14);
		this.ErrorAdr = new CMemDefU32(this.sock, 380);
		this.ErrorCode = new CMemDefArrayU8(this.sock, 384, 32);

		this.InterpTargetTimeList = new CMemDefArrayU16(this.sock, 496, 32);

		this.InterpServoPosTarget = new CMemDefArrayS16(this.sock, 2560, 32);
		this.InterpServoTorqeTarget = new CMemDefArrayS16(this.sock, 2624, 32);
		this.InterpLEDTarget = new CMemDefArrayS16(this.sock, 2688, 16);

		this.InterpServoPosTriggerPointer = new CMemDefArrayS16(this.sock, 2816, 32);
		this.InterpServoTorqeTriggerPointer = new CMemDefArrayS16(this.sock, 2880, 32);
		this.InterpLEDTriggerPointer = new CMemDefArrayS16(this.sock, 2944, 16);

		this.InterpServoPosOutput = new CMemDefArrayS16(this.sock, 3072, 32);
		this.InterpServoTorqeOutput = new CMemDefArrayS16(this.sock, 3136, 32);
		this.InterpLEDOutput = new CMemDefArrayS16(this.sock, 3200, 16);
		this.InterpServoPosRemainingTime = new CMemDefArrayU16(this.sock, 3328, 32);
		this.InterpServoTorqeRemainingTime = new CMemDefArrayU16(this.sock, 3392, 32);
		this.InterpLEDRemainingTime = new CMemDefArrayU16(this.sock, 3456, 16);
		this.ServoPosLimitLow = new CMemDefArrayS16(this.sock, 3584, 32);
		this.ServoPosLimitHigh = new CMemDefArrayS16(this.sock, 3648, 32);
		this.ServoReadPos = new CMemDefArrayS16(this.sock, 3712, 32);
		this.ServoOffset = new CMemDefArrayS16(this.sock, 3776, 32);
		this.CollidionDetectDisable = new CMemDefU8(this.sock, 82);
		this.ServoCollidionStatus = new CMemDefArrayU16(this.sock, 3968, 32);
	}
}
