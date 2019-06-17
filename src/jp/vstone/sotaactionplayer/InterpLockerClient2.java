package jp.vstone.sotaactionplayer;


import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;

import jp.vstone.network.AppMngRequest;
import jp.vstone.network.TCPIPClient;

public class InterpLockerClient2 {
	static InterpLockerClient2 instance = null;

	public static InterpLockerClient2 createInstance() {
		if (instance == null) {
			instance = new InterpLockerClient2();
		}
		return instance;
	}

	private InterpLockerClient2() {
	}

	public boolean LockHandle(String LockKey, Byte[] ids, boolean isServo) {

		AppMngRequest.InterpLockRequest rq = new AppMngRequest.InterpLockRequest();
		rq.key = LockKey;
		rq.ids = ids;
		rq.isServo = isServo;
		String result;
		try {

			result = (String) TCPIPClient.getObject("localhost", 6495, 2000, rq

					.getJsonRequest());
		} catch (ClassNotFoundException e) {

			e.printStackTrace();
			return false;
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		if (result == null) {
			return false;
		}
		if (result.equals("OK")) {
			convLockKeytoTimerAddress(null, LockKey);
			return true;
		}
		return false;
	}

	public boolean UnLockHandle(String LockKey, Byte[] ids, boolean isServo) {
		AppMngRequest.InterpUnLockRequest rq = new AppMngRequest.InterpUnLockRequest();
		rq.key = LockKey;
		rq.ids = ids;
		rq.isServo = isServo;
		String result;
		try {
			result = (String) TCPIPClient.getObject("localhost", 6495, 2000, rq

					.getJsonRequest());
		} catch (ClassNotFoundException e) {

			e.printStackTrace();
			return false;
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		removeTimer(LockKey);
		if (result == null) {
			return false;
		}
		if (result.equals("OK")) {
			return true;
		}
		return false;
	}

	HashMap<String, Short> TimerList = new HashMap();

	private void putTimer(String LockKey, Short timer) {
		this.TimerList.put(LockKey, timer);
	}

	private Short getTimer(String LockKey) {
		return (Short) this.TimerList.get(LockKey);
	}

	private void removeTimer(String LockKey) {
		this.TimerList.remove(LockKey);
	}

	public Short convLockKeytoTimerAddress(CRobotMem2 mem, String LockKey) {
		Short result = getTimer(LockKey);
		if (result != null) {
			return result;
		}
		AppMngRequest.InterpConvKey2Addr rq = new AppMngRequest.InterpConvKey2Addr();
		rq.key = LockKey;
		try {
			result = (Short) TCPIPClient.getObject("localhost", 6495, 2000, rq

					.getJsonRequest());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return result;
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return result;
		} catch (IOException e) {
			e.printStackTrace();
			return result;
		}
		if ((result == null) && (mem != null)) {
			result = Short.valueOf((short) mem.InterpTargetTimeList.address);
		}
		putTimer(LockKey, result);
		return result;
	}
}
