package jp.vstone.sotaactionplayer;
/*** Eclipse Class Decompiler plugin, copyright (c) 2012 Chao Chen (cnfree2000@hotmail.com) ***/

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Stack;
import java.util.function.Consumer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import jp.vstone.RobotLib.CRobotUtil;
import jp.vstone.RobotLib.CWavePlayer;

public class CPlayWave2 {
	static final String TAG = "CPlayWave";
	DataLine.Info info = null;
	File audioFile = null;
	SourceDataLine line = null;
	Clip clip = null;
	Mixer mixer = null;
	double volume = 0.0D;
	byte[] data;
	AudioFormat audioformat;
	AudioFormat decodedFormat;
	DataLine.Info linfo;
	int frameSize;
	boolean b_playing = false;
	boolean be_stop = false;
	Thread playthread = null;
	private static boolean MODE_EXTERNAL_PLAYER = true;
	private static boolean MODE_PIPE = false;
	static int aplayfilebuffercnt = 0;
	static final int aplayfilebuffermax = 10;
	ProcessBuilder pb;
	public Stack<Process> process = new Stack<Process>();
	WaveHeader wavhedder = null;

	public CPlayWave2 PlayWave(byte[] wavdata, boolean beWait) {
		if (beWait) {
			return PlayWave_wait(wavdata);
		}
		return PlayWave(wavdata);
	}

	public CPlayWave2 PlayWave(String FilePath, boolean beWait) {
		CRobotUtil.setDebugOut("CPlayWave", Boolean.valueOf(false));
		if (beWait) {
			return PlayWave_wait(FilePath);
		}
		return PlayWave(FilePath);
	}

	public CPlayWave2 PlayWave(String FilePath) {
		//CRobotUtil.Log("CPlayWave", "Play " + FilePath);
		if (CRobotUtil.isWindows()) {
			MODE_EXTERNAL_PLAYER = false;
			MODE_PIPE = false;
		}
		CPlayWave2 cplay = this;
		try {
			cplay.wavhedder = ReadWaveHeader(FilePath);
			if (MODE_PIPE) {
				cplay.playpipe(FilePath);
			} else if (MODE_EXTERNAL_PLAYER) {
				cplay.play_external(FilePath);
			} else {
				cplay.playwaveplayer(AudioSystem.getAudioInputStream(new File(FilePath)));
			}
			return cplay;
		} catch (Exception e) {
		}
		return null;
	}

	public CPlayWave2 PlayWave(byte[] wavdata) {
		if (CRobotUtil.isWindows()) {
			MODE_EXTERNAL_PLAYER = false;
			MODE_PIPE = false;
		}
		if (CRobotUtil.isRpi()) {
			MODE_PIPE = false;
		}
		try {
			if ((MODE_EXTERNAL_PLAYER) || (MODE_PIPE)) {
				return PlayWave(saveTmpFile(wavdata));
			}
			CRobotUtil.Log("CPlayWave", "Play data");
			CPlayWave2 cplay = new CPlayWave2();
			cplay.wavhedder = ReadWaveHeader(new ByteArrayInputStream(wavdata));
			cplay.playwaveplayer(AudioSystem.getAudioInputStream(new ByteArrayInputStream(wavdata)));
			return cplay;
		} catch (Exception e) {
		}
		return null;
	}

	private static String saveTmpFile(byte[] wavdata) {
		String path = "/dev/shm/wavtemp" + aplayfilebuffercnt + ".wav";
		aplayfilebuffercnt = (aplayfilebuffercnt + 1) % 10;
		try {
			FileOutputStream out = new FileOutputStream(path);
			out.write(wavdata);
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		FileOutputStream out;
		return path;
	}

	public CPlayWave2() {
		Runtime.getRuntime().addShutdownHook(new Shutdown());
	}

	private class Shutdown extends Thread {
		private Shutdown() {
		}

		public void run() {
			if(CPlayWave2.this.process != null){
			Iterator<Process> process_ite =CPlayWave2.this.process.iterator();
			Process process_tmp;
			while(process_ite.hasNext()){
			process_tmp=process_ite.next();
			if ((process_tmp != null) && (process_tmp.isAlive())) {
				try {
					process_tmp.waitFor();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			}
			CPlayWave2.this.process.clear();
			}
		}
	}

	private long pipetime = 0L;
	private long pipeendtime = 0L;

	private static CPlayWave2 PlayPipe(String file, CPlayWave2 cplay) {
		cplay.playpipe(file);

		return cplay;
	}

	private void playpipe(String file) {
		this.pipetime = Calendar.getInstance().getTimeInMillis();
		this.pipeendtime = (this.pipetime + getPlayTime() + 200L);
		CRobotUtil.Debug("CPlayWave", "pipetime " + this.pipetime + " pipeendtime " + this.pipeendtime);
		runCmdBackGround("echo -e \"play," + this.pipetime + "," + new File(file).getAbsoluteFile()
				+ "\" > /home/vstone/vstonemagic/wavplay.fifo", "/home/vstone/vstonemagic/");
	}

	private void stoppipe() {
		runCmdBackGround("echo -e \"stop," + this.pipetime + "\" > /home/vstone/vstonemagic/wavplay.fifo",
				"/home/vstone/vstonemagic/");
	}

	private void waitpipe() {
		long now = Calendar.getInstance().getTimeInMillis();
		if (now < this.pipeendtime) {
			CRobotUtil.Debug("CPlayWave", "waitpipe  " + (int) (this.pipeendtime - now));
			CRobotUtil.wait((int) (this.pipeendtime - now));
		}
	}

	static Process runCmdBackGround(String cmd, String dir) {
		Process process = null;
		String[] cmds = { "/bin/sh", "-c", cmd };
		try {
			process = Runtime.getRuntime().exec(cmds, null, new File(dir));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return process;
	}

	static byte[] ReadByte(InputStream fs, int len) {
		byte[] buf = new byte[len + 1];

		Arrays.fill(buf, (byte) 0);
		try {
			int ret = fs.read(buf, 0, len);
			if (ret == len) {
				return buf;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public long getPlayTime() {
		if (this.wavhedder == null) {
			return 0L;
		}
		return this.wavhedder.PlayTimeMs;
	}

	public static long getPlayTime(String FilePath) {
		WaveHeader header = ReadWaveHeader(FilePath);
		return header.PlayTimeMs;
	}

	private static WaveHeader ReadWaveHeader(String sFilePath) {
		if (!new File(sFilePath).exists()) {
			return null;
		}
		try {
			FileInputStream fs = new FileInputStream(sFilePath);
			return ReadWaveHeader(fs);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static WaveHeader ReadWaveHeader(InputStream is) {
		short nRlt = -1;
		WaveHeader wh = new WaveHeader();
		try {
			try {
				wh.riffHeader = new String(ReadByte(is, 4));

				ByteBuffer bytebuf = ByteBuffer.wrap(ReadByte(is, 4));
				bytebuf.order(ByteOrder.LITTLE_ENDIAN);
				wh.waveHeader = new String(ReadByte(is, 4));

				wh.fmtChank = new String(ReadByte(is, 4));
				if ((wh.riffHeader.contains("RIFF")) && (wh.waveHeader.contains("WAVE"))
						&& (wh.fmtChank.contains("fmt"))) {
					bytebuf = ByteBuffer.wrap(ReadByte(is, 4));
					bytebuf.order(ByteOrder.LITTLE_ENDIAN);
					wh.fmtChankSize = bytebuf.getInt();
					bytebuf = ByteBuffer.wrap(ReadByte(is, 2));
					bytebuf.order(ByteOrder.LITTLE_ENDIAN);
					wh.formatID = bytebuf.getShort();
					bytebuf = ByteBuffer.wrap(ReadByte(is, 2));
					bytebuf.order(ByteOrder.LITTLE_ENDIAN);
					wh.channel = bytebuf.getShort();
					bytebuf = ByteBuffer.wrap(ReadByte(is, 4));
					bytebuf.order(ByteOrder.LITTLE_ENDIAN);
					wh.sampleRate = bytebuf.getInt();
					bytebuf = ByteBuffer.wrap(ReadByte(is, 4));
					bytebuf.order(ByteOrder.LITTLE_ENDIAN);
					wh.bytePerSec = bytebuf.getInt();
					bytebuf = ByteBuffer.wrap(ReadByte(is, 2));
					bytebuf.order(ByteOrder.LITTLE_ENDIAN);
					wh.blockSize = bytebuf.getShort();
					bytebuf = ByteBuffer.wrap(ReadByte(is, 2));
					bytebuf.order(ByteOrder.LITTLE_ENDIAN);
					wh.bitPerSample = bytebuf.getShort();

					boolean bMatch = false;
					int n = 0;
					int ret = 0;
					String s = "";
					byte[] buf = new byte['Ȁ'];
					String data = "data";
					for (int i = 0; (ret = is.read(buf)) > 0; i++) {
						ret = indexOf(buf, data.getBytes(), 0);
						if (ret >= 0) {
							wh.dataChank = "data";
							byte[] b = Arrays.copyOfRange(buf, ret + 4, ret + 8);
							bytebuf = ByteBuffer.wrap(b);
							bytebuf.order(ByteOrder.LITTLE_ENDIAN);
							wh.dataChankSize = bytebuf.getInt();
							int n1SecBytes = wh.sampleRate * wh.channel * wh.blockSize;
							wh.PlayTimeMs = ((int) (wh.dataChankSize / n1SecBytes * 1000.0D));

							nRlt = 1;
							break;
						}
					}
					CRobotUtil.Debug("CPlayWave", "wavhedder " + wh.PlayTimeMs);
				}
			} catch (Exception e) {
			}
			is.close();
		} catch (Exception e) {
		}
		return wh;
	}

	private static int indexOf(byte[] org, byte[] dest, int startindex) {
		if ((org == null) || (dest == null)) {
			throw new IllegalArgumentException("`org` or `dest` is null.");
		}
		if ((org.length == 0) || (dest.length == 0) || (org.length < dest.length)) {
			return -1;
		}
		int limitIndex = org.length - dest.length + 1;
		for (int i = startindex; i < limitIndex; i++) {
			for (int j = 0; j < dest.length; j++) {
				if (org[(i + j)] != dest[j]) {
					break;
				}
				if (j == dest.length - 1) {
					return i;
				}
			}
		}
		return -1;
	}

	Calendar starttime = null;
	AudioInputStream decoded_in;
	CWavePlayer decoder;

	private boolean play_external(String file) {
		if (CRobotUtil.isRpi()) {
			//CRobotUtil.Log("CPlayWave", "isRpi");
			this.pb = new ProcessBuilder(new String[] { "nohup", "aplay", file, "2>&1", "&" });
		} else if (CRobotUtil.isEdison()) {
			CRobotUtil.Log("CPlayWave", "isEdison");

			this.pb = new ProcessBuilder(new String[] { "gst-launch-1.0", "filesrc", "location=\"" + file + "\"", "!",
					"wavparse", "!", "volume2", "volume=1.01", "!", "pulsesink" });
		} else if (CRobotUtil.isGrosseTete()) {
			CRobotUtil.Log("CPlayWave", "isGrosseTete");
			this.pb = new ProcessBuilder(new String[] { "nohup", "aplay", "-D", "hw:CODEC", file, ">",
					"/var/log/hogehogeout", "2>&1", "&" });
		}
		try {
			int timeout = 10;
			this.pb.redirectErrorStream(true);
			this.process.add(this.pb.start());
			this.starttime = Calendar.getInstance();
			//CRobotUtil.Debug("CPlayWave", "play " + getPid(this.process.peek()));
			//CRobotUtil.Log("CPlayWave", "play------- " + getPid(this.process.peek()));
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	private boolean externalplayer_wait() {
			if(this.process != null){
			this.process.forEach(new Consumer<Process>(){
				@Override
				public void accept(Process process){
					try {
						if(process != null && process.isAlive())
						process.waitFor();
					} catch (InterruptedException e) {
						// TODO 自動生成された catch ブロック
						e.printStackTrace();
					}
				}
			});

			stop();
			this.process.clear();
			}
			return true;


	}

	private void playwaveplayer(AudioInputStream ais) {
		try {
			this.decoder = new CWavePlayer(ais);
		} catch (IOException | UnsupportedAudioFileException | LineUnavailableException e) {
			e.printStackTrace();
		}
	}

	public CPlayWave2 PlayWave_wait(String FilePath) {
		try {
			CPlayWave2 ret = PlayWave(FilePath);
			if (MODE_PIPE) {
				ret.waitpipe();
			} else {
				if (MODE_EXTERNAL_PLAYER) {
					ret.externalplayer_wait();
					return ret;
				}
				ret.waitend();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public CPlayWave2 PlayWave_wait(byte[] wavdata) {
		try {
			CPlayWave2 ret = PlayWave(wavdata);
			if (MODE_PIPE) {
				ret.waitpipe();
			} else {
				if (MODE_EXTERNAL_PLAYER) {
					ret.externalplayer_wait();
					return ret;
				}
				ret.waitend();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public boolean stop() {
		CRobotUtil.Debug("CPlayWave", "Stop");
		if (MODE_PIPE) {
			stoppipe();
		} else if (MODE_EXTERNAL_PLAYER) {
			if(this.process != null){
			Iterator<Process> process_ite =this.process.iterator();
			Process process_tmp;
			while(process_ite.hasNext()){
			process_tmp=process_ite.next();

			if ((process_tmp != null) && (process_tmp.isAlive())) {
				long timeofprocess = Calendar.getInstance().getTimeInMillis() - this.starttime.getTimeInMillis();
				while (timeofprocess < 100L) {
					CRobotUtil.wait(5);
					timeofprocess = Calendar.getInstance().getTimeInMillis() - this.starttime.getTimeInMillis();
				}
				int PID = getPid(process_tmp);
				//CRobotUtil.Log("CPlayWave", "PID:" + PID);

				String cmd = "kill " + PID;

				File f = new File("/usr/bin/sudo");
				if (f.exists()) {
					cmd = "sudo " + cmd;
				}
				Process processkill = null;
				String[] cmds = { "/bin/sh", "-c", cmd };
				try {
					processkill = Runtime.getRuntime().exec(cmds, null, new File("/home"));
				} catch (IOException e) {
					e.printStackTrace();
				}
				try {
					processkill.waitFor();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				try {
					process_tmp.waitFor();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				}

			}
			this.process.clear();
			}
		} else {
			this.line.stop();
		}
		return false;
	}

	private static int getPid(Process process) {
		try {
			Class<?> cProcessImpl = process.getClass();
			Field fPid = cProcessImpl.getDeclaredField("pid");
			if (!fPid.isAccessible()) {
				fPid.setAccessible(true);
			}
			return fPid.getInt(process);
		} catch (Exception e) {
		}
		return -1;
	}

	public boolean isPlaying() {
		if (this.playthread == null) {
			return false;
		}
		return this.playthread.isAlive();
	}

	public boolean isPlaying2() {
		if (this.process == null) {
			return false;
		}
		if(this.process.isEmpty()){
			return false;
		}
		Iterator<Process> process_ite =this.process.iterator();
		Process process_tmp;
		while(process_ite.hasNext()){
		process_tmp=process_ite.next();
		if(process_tmp.isAlive()){
			return true;
		}
		}
		return false;
	}

	private void waitend() {
		while (isPlaying()) {
			CRobotUtil.wait(100);
		}
	}

	static class WaveHeader {
		public String riffHeader;
		public int fileSize;
		public String waveHeader;
		public String fmtChank;
		public int fmtChankSize;
		public short formatID;
		public short channel;
		public int sampleRate;
		public int bytePerSec;
		public short blockSize;
		public short bitPerSample;
		public String dataChank;
		public int dataChankSize;
		public int PlayTimeMs;

		WaveHeader() {
		}
	}

	static class playThread extends Thread {
		CPlayWave2 parent;
		private byte[] buffer = new byte[32768];

		playThread() {
		}

		public void run() {
			int readPoint = 0;
			int bytesRead = 0;
			boolean notYetEOF = true;
			this.parent.b_playing = true;
			try {
				while (notYetEOF) {
					bytesRead = this.parent.decoded_in.read(this.buffer, readPoint, this.buffer.length - readPoint);
					if (bytesRead == -1) {
						notYetEOF = false;
						break;
					}
					int leftover = bytesRead % this.parent.frameSize;
					this.parent.line.write(this.buffer, readPoint, bytesRead - leftover);
					System.arraycopy(this.buffer, bytesRead, this.buffer, 0, leftover);
					readPoint = leftover;
				}
				this.parent.line.drain();
				this.parent.line.stop();
			} catch (IOException ioe) {
			} finally {
				this.parent.line.close();
			}
			this.parent.b_playing = false;
		}
	}

	static class playThread_Clip extends Thread {
		CPlayWave2 parent;

		playThread_Clip() {
		}

		public void run() {
			super.run();
			try {
				this.parent.b_playing = true;
				this.parent.clip.start();
				Thread.sleep(1L);
				this.parent.clip.drain();
				this.parent.clip.stop();
				this.parent.clip.close();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			this.parent.b_playing = false;
		}
	}
}
