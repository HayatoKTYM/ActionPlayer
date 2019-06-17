package jp.vstone.sotasample;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import com.google.gson.Gson;

import jp.vstone.RobotLib.CPlayWave;
import jp.vstone.RobotLib.CRobotUtil;
import jp.vstone.network.AppMngRequest;
import jp.vstone.network.TCPIPClient;

public class TextToSpeechSota2 {
	private static final String TAG = "TextToSpeechSota";
	private static String textToSpeechLogUrl = "http://tts2.sota.vstone.co.jp/TTSServerLicense/SaveLog";
	private static final String configfile_tts = "/home/vstone/sotavoice/tts_json.conf";
	private static final String configfile_server = "/home/vstone/sotavoice/tts_server.conf";
	public static final int Params_SpeechRate_Default = 11;
	public static final int Params_Pitch_Default = 13;
	public static final int Params_Intonation_Default = 11;
	public static final int Params_SpeechRate_Max = 25;
	public static final int Params_SpeechRate_Min = 5;
	public static final int Params_Pitch_Max = 20;
	public static final int Params_Pitch_Min = 1;
	public static final int Params_Intonation_Max = 15;
	public static final int Params_Intonation_Min = 1;
	private static final String Type = "FILE_ZIP";
	private static final String TEMP_PATH = "/dev/shm/wavtemptts";

	public TextToSpeechSota2() {
	}

	private static class TTSServerConf {
		String textToSpeechUrl = "http://tts2.sota.vstone.co.jp/TTSServerLicense/GetTTSJson";

		private TTSServerConf() {
		}
	}

	public static class TTSRequest {
		public String TextData = "";
		public String Model = "Sota_A";
		public int SpeechRate = 11;
		public int pitch = 13;
		public int Intonation = 11;
		public String UserAgent = null;
		public String SerialCode = null;

		public TTSRequest() {
		}
	}

	static int ram_file_cnt = 0;
	static final int ram_faile_max = 10;
	static boolean nocache = false;
	static TTSRequest ttsrequest = new TTSRequest();
	static TTSServerConf ttsserver = new TTSServerConf();
	private static final String properties_TTSServer = "TTSServer";

	private static void LoadProperties() {
		if (!new File("/home/vstone/sotavoice/tts_json.conf").exists()) {
			return;
		}
		try {
			byte[] data = Files.readAllBytes(new File("/home/vstone/sotavoice/tts_json.conf").toPath());
			Gson gson = new Gson();
			ttsrequest = (TTSRequest) gson.fromJson(new String(data), TTSRequest.class);
			ttsrequest.UserAgent = getHwCode();
			ttsrequest.SerialCode = CRobotUtil.getSerialCode();
		} catch (IOException e) {
			e.printStackTrace();
		}
		LoadOnPremisesProperties();
	}

	private static void LoadOnPremisesProperties() {
		if (!new File("/home/vstone/sotavoice/tts_server.conf").exists()) {
			return;
		}
		try {
			byte[] data = Files.readAllBytes(new File("/home/vstone/sotavoice/tts_server.conf").toPath());
			Gson gson = new Gson();
			ttsrequest = (TTSRequest) gson.fromJson(new String(data), TTSRequest.class);
			ttsrequest.UserAgent = getHwCode();
			ttsrequest.SerialCode = CRobotUtil.getSerialCode();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static byte[] getTTS(String text) {
		return getTTSData(text);
	}

	public static String getTTSFile(String text) {
		return getTTSFile(text, 11, 13, 11);
	}

	public static String getTTSFile(String text, int speechRate, int pitch, int intonation) {
		CRobotUtil.setDebugOut("TextToSpeechSota", Boolean.valueOf(false));
		if (text == null) {
			return null;
		}
		if (text.length() <= 0) {
			return null;
		}
		if (text.length() > 2048) {
			text = text.substring(0, 2048);
		}
		if (speechRate > 25) {
			speechRate = 25;
		} else if (speechRate < 5) {
			speechRate = 5;
		}
		if (pitch > 20) {
			pitch = 20;
		} else if (pitch < 1) {
			pitch = 1;
		}
		if (intonation > 15) {
			intonation = 15;
		} else if (intonation < 1) {
			intonation = 1;
		}
		String path = null;
		if (!nocache) {
			path = getCache(text, speechRate, pitch, intonation);
		}
		if (path == null) {
			try {
				byte[] voiceData = getData(text, speechRate, pitch, intonation);
				if (voiceData != null) {
					path = writeRam(voiceData);
					writeCache(text, path, speechRate, pitch, intonation);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (path != null) {
			postlog(text);
		}
		return path;
	}

	public static byte[] getTTSData(String text) {
		return getTTSData(text, 13, 13, 11);
	}

	public static byte[] getTTSData(String text, int speechRate, int pitch, int intonation) {
		CRobotUtil.setDebugOut("TextToSpeechSota", Boolean.valueOf(false));
		if (text == null) {
			return null;
		}
		if (text.length() <= 0) {
			return null;
		}
		if (text.length() > 2048) {
			text = text.substring(0, 2048);
		}
		if (speechRate > 25) {
			speechRate = 25;
		} else if (speechRate < 5) {
			speechRate = 5;
		}
		if (pitch > 20) {
			pitch = 20;
		} else if (pitch < 1) {
			pitch = 1;
		}
		if (intonation > 15) {
			intonation = 15;
		} else if (intonation < 1) {
			intonation = 1;
		}
		String path = null;
		if (!nocache) {
			path = getCache(text, speechRate, pitch, intonation);
		}
		byte[] voiceData = null;
		if (path != null) {
			try {
				voiceData = readFileToByte(path);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			voiceData = getData(text, speechRate, pitch, intonation);
			if (voiceData != null) {
				path = writeRam(voiceData);
				writeCache(text, path, speechRate, pitch, intonation);
			}
		}
		if (voiceData != null) {
			postlog(text);
		}
		return voiceData;
	}

	private static byte[] getData(String text, int speechRate, int pitch, int intonation) {
		long start = System.currentTimeMillis();

		CRobotUtil.Log("TextToSpeechSota", "[get:" + text + "][speechRate:" + speechRate + "][pitch:" + pitch + "]"
				+ "][intonation:" + intonation + "]");
		if ("/home/vstone/sotavoice/tts_json.conf" == null) {
			LoadProperties();
		} else {
			CRobotUtil.Debug("TextToSpeechSota", "Loaded");
		}
		if (text == null) {
			return null;
		}
		if (text.length() <= 0) {
			return null;
		}
		byte[] voiceData = null;
		try {
			CRobotUtil.Debug("TextToSpeechSota", "setconfig");
			ttsrequest.TextData = text;
			ttsrequest.SpeechRate = speechRate;
			ttsrequest.pitch = pitch;
			ttsrequest.Intonation = intonation;
			if (ttsrequest.UserAgent == null) {
				ttsrequest.UserAgent = getHwCode();
			}
			if (ttsrequest.SerialCode == null) {
				ttsrequest.SerialCode = CRobotUtil.getSerialCode();
			}
			voiceData = getvoicedata(ttsrequest);
			CRobotUtil.Debug("TextToSpeechSota", "getDecoder");
			if (voiceData == null) {
				CRobotUtil.Err("TextToSpeechSota", "Response is null");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		long end = System.currentTimeMillis();
		CRobotUtil.Debug("TextToSpeechSota", "Type:FILE_ZIP , time : " + (end - start));

		return voiceData;
	}

	private static String getCache(String text, int speechRate, int pitch, int intonation) {
		text = text.replaceAll("\r", "").replace("\n", "");
		text = text + "[speechRate:" + speechRate + "][pitch:" + pitch + "][Intonation:" + intonation + "]";

		text = Base64.getEncoder().encodeToString(text.getBytes());

		AppMngRequest.TTSGetCacheRequest rq = new AppMngRequest.TTSGetCacheRequest();
		rq.text = text;

		String result=null;
		try {
			result = (String) TCPIPClient.getObject("localhost", 6495, 2000, rq

					.getJsonRequest());
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
			return null;
		}

		if (result == null) {
			CRobotUtil.Debug("TextToSpeechSota", "[TTS_GET_CACHE] null");
			return null;
		}
		if (result.endsWith("NG")) {
			return null;
		}
		//CRobotUtil.Log("TextToSpeechSota", "[TTS_USE_CACHE] " + result+text);
		return result;
	}

	private static byte[] readFileToByte(String filePath) throws Exception {
		byte[] b = new byte[1];
		FileInputStream fis = new FileInputStream(filePath);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		while (fis.read(b) > 0) {
			baos.write(b);
		}
		baos.close();
		fis.close();
		b = baos.toByteArray();

		return b;
	}

	private static void writeCache(String text, String ramFilepath, int speechRate, int pitch, int intonation) {
		writeCache wc = new writeCache();
		wc.speechRate = speechRate;
		wc.text = text;
		wc.ramFilepath = ramFilepath;
		wc.pitch = pitch;
		wc.intonation = intonation;
		Thread th = new Thread(wc);
		th.start();
	}

	static class writeCache implements Runnable {
		String text;
		String ramFilepath;
		int speechRate;
		int pitch;
		int intonation;

		writeCache() {
		}

		public void run() {
			this.text = this.text.replaceAll("\r", "").replace("\n", "");
			this.text = (this.text + "[speechRate:" + this.speechRate + "][pitch:" + this.pitch + "][Intonation:"
					+ this.intonation + "]");
			this.text = Base64.getEncoder().encodeToString(this.text.getBytes());

			AppMngRequest.TTSPutCacheRequest rq = new AppMngRequest.TTSPutCacheRequest();
			rq.file = this.ramFilepath;
			rq.text = this.text;
			String result;
			try {
				result = (String) TCPIPClient.getObject("localhost", 6495, 2000, rq

						.getJsonRequest());
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
				CRobotUtil.Err("TextToSpeechSota", "[TTS_PUT_CACHE] error:" + this.text + this.ramFilepath);
				return;
			}

			if (result == null) {
				CRobotUtil.Err("TextToSpeechSota", "[TTS_PUT_CACHE] error:" + this.text + this.ramFilepath);
			} else {
				CRobotUtil.Debug("TextToSpeechSota", "[TTS_PUT_CACHE] OK:" + this.text);
			}
		}
	}

	private static String writeRam(byte[] data) {
		String path = "/dev/shm/wavtemptts" + ram_file_cnt + "B.wav";
		ram_file_cnt = (ram_file_cnt + 1) % 10;
		File file = new File(path);
		file.setWritable(true, true);
		file.setReadable(true, true);
		try {
			FileOutputStream out = new FileOutputStream(file);
			out.write(data);
			out.close();
			file.setWritable(true, true);
			file.setReadable(true, true);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return path;
	}

	private static String url_serial2hw = "http://sota.vstone.co.jp/serialcode/sotaserial2hwcode.php?code=";
	private static String hwcode = null;

	static String getHwCode() {
		if (hwcode == null) {
			if (CRobotUtil.isWindows()) {
				hwcode = getHwCodeHttp(url_serial2hw);
			} else {
				String cmd = "/home/vstone/vstonemagic/sotalcc KCUee3U5WYiTdcvMTVY5rA4V";
				Process process = runCmd(cmd, "/home/vstone/vstonemagic/");
				String[] list = getOutputStrings(process);
				hwcode = list[0];
			}
		}
		return hwcode;
	}

	private static String getHwCodeHttp(String urlstr) {
		String line = null;
		boolean ret = false;
		urlstr = urlstr + CRobotUtil.getSerialCode();

		CRobotUtil.Debug("TextToSpeechSota", "Serial Code:" + CRobotUtil.getSerialCode());
		try {
			URL url = new URL(urlstr);

			HttpURLConnection urlconn = (HttpURLConnection) url.openConnection();
			urlconn.setRequestMethod("GET");
			urlconn.setInstanceFollowRedirects(false);
			urlconn.setRequestProperty("Accept-Language", "ja;q=0.7,en;q=0.3");

			urlconn.connect();

			BufferedReader reader = new BufferedReader(new InputStreamReader(urlconn.getInputStream()));
			for (;;) {
				line = reader.readLine();
				if (line == null) {
					break;
				}
				if (line.indexOf("Error") >= 0) {
					System.out.println("Error");
					line = null;
					break;
				}
				if (line.indexOf("ERROR") >= 0) {
					System.out.println("Error");
					line = null;
					break;
				}
				if (line.indexOf("error") >= 0) {
					System.out.println("Error");
					line = null;
					break;
				}
				System.out.println("line : " + line);
			}
			reader.close();
			urlconn.disconnect();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return line;
	}

	private static Process runCmd(String cmd, String dir) {
		Process process = null;
		String[] cmds = { "/bin/sh", "-c", cmd };
		try {
			process = Runtime.getRuntime().exec(cmds, null, new File(dir));
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			process.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return process;
	}

	private static String[] getOutputStrings(Process process) {
		BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
		ArrayList<String> list = new ArrayList();
		String line = null;
		try {
			while ((line = br.readLine()) != null) {
				list.add(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return (String[]) list.toArray(new String[0]);
	}

	static byte[] getvoicedata(TTSRequest request) {
		byte[] result = null;

		HttpURLConnection connection = null;
		PrintWriter pw = null;
		BufferedInputStream bis = null;
		try {
			Gson gson = new Gson();
			String json = gson.toJson(request);

			URL url = new URL(ttsserver.textToSpeechUrl);
			CRobotUtil.Debug("TextToSpeechSota", url.toString());
			connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(10000);
			connection.setDoInput(true);
			connection.setDoOutput(true);

			connection.setUseCaches(false);

			connection.setRequestMethod("POST");
			connection.setRequestProperty("Connection", "Keep-Alive");

			connection.setRequestProperty("User-Agent", request.UserAgent);
			connection.setRequestProperty("serial-code", request.SerialCode);

			connection.setRequestProperty("result-type", "FILE_ZIP");

			connection.setRequestProperty("Content-Type", "text/xml");
			connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
			connection.setRequestProperty("Host", "localhost");

			CRobotUtil.Debug("TextToSpeechSota", "Send Request");

			pw = new PrintWriter(connection.getOutputStream());
			pw.write(json);
			pw.flush();

			int iResponseCode = connection.getResponseCode();
			CRobotUtil.Debug("TextToSpeechSota", "Get ResponseCode " + iResponseCode);
			if (iResponseCode == 200) {
				bis = new BufferedInputStream(connection.getInputStream());
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				byte[] tmp = new byte['䀀'];
				int len = 0;
				while ((len = bis.read(tmp, 0, tmp.length)) > 0) {
					bos.write(tmp, 0, len);
				}
				bos.flush();
				bos.close();
				byte[] retdata = bos.toByteArray();
				switch ("FILE_ZIP") {
				case "RAW":
					result = retdata;
					break;
				case "BASE64":
					result = Base64.getDecoder().decode(retdata);
					break;
				case "ZIP":
					result = unzip(retdata);
					break;
				case "FILE":
					result = retdata;
					break;
				case "FILE_ZIP":
					result = unzip(retdata);
					break;
				}
			} else {
				if (iResponseCode == 499) {
					CPlayWave.PlayWave_wait("/home/vstone/vstonemagic/menu/voicelicense_error_over.wav");
				} else if (iResponseCode == 493) {
					CPlayWave.PlayWave_wait("/home/vstone/vstonemagic/menu/voicelicense_error_uselimit.wav");
				} else if (iResponseCode == 498) {
					CPlayWave.PlayWave_wait("/home/vstone/vstonemagic/menu/voicelicense_error_noting.wav");
				} else if (iResponseCode == 497) {
					CPlayWave.PlayWave_wait("/home/vstone/vstonemagic/menu/voicelicense_error_code.wav");
				}
				result = null;
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			result = null;
		} catch (IOException e) {
			e.printStackTrace();
			result = null;
		} finally {
			try {
				if (bis != null) {
					bis.close();
				}
				if (pw != null) {
					pw.flush();
					pw.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (connection != null) {
				connection.disconnect();
			}
		}
		return result;
	}

	private static byte[] unzip(byte[] data) {
		Inflater decompresser = new Inflater();
		decompresser.setInput(data);
		ByteArrayOutputStream decompos = new ByteArrayOutputStream();
		byte[] buffer = new byte[32768];
		while (!decompresser.finished()) {
			try {
				int count = decompresser.inflate(buffer);
				decompos.write(buffer, 0, count);
			} catch (DataFormatException e) {
				e.printStackTrace();
			}
		}
		decompresser.end();
		return decompos.toByteArray();
	}

	private static boolean postlog(String text) {
		String json = "{\"datatype\":\"TTS\",\"ttstext\":\"" + text + "\"," + "\"id\":\"Sota"
				+ CRobotUtil.getSerialCode() + "\"}";
		return postjson(textToSpeechLogUrl, json);
	}

	static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
	static final int ERRORCODE_LICENSE_LIMIT = 493;
	static final int ERRORCODE_SERIALCODE_NOTING = 494;
	static final int ERRORCODE_TTS_ERROR = 495;
	static final int ERRORCODE_OVERACCESS = 496;
	static final int ERRORCODE_HWCODE_NOTING = 497;
	static final int ERRORCODE_LICENSE_NOTING = 498;
	static final int ERRORCODE_LICENSE_OVER = 499;

	private static boolean postjson(String _url, String json) {
		CRobotUtil.Debug("TextToSpeechSota", "postjson");
		HttpURLConnection connection = null;
		DataOutputStream os = null;
		boolean result = false;
		try {
			byte[] data = json.getBytes(Charset.forName("UTF-8"));
			URL url = new URL(_url);
			CRobotUtil.Debug("TextToSpeechSota", url.toString());
			connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(10000);
			connection.setDoInput(true);
			connection.setDoOutput(true);

			connection.setUseCaches(false);

			connection.setFixedLengthStreamingMode(data.length);

			connection.setRequestMethod("POST");
			connection.setRequestProperty("Connection", "Keep-Alive");

			connection.setRequestProperty("User-Agent", getHwCode());
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
			connection.setRequestProperty("Log-Date",
					sdf.format(Calendar.getInstance(CRobotUtil.getTimeZone()).getTime()));

			CRobotUtil.Debug("TextToSpeechSota", "Send Request");

			os = new DataOutputStream(connection.getOutputStream());
			os.write(data);
			os.flush();

			int iResponseCode = connection.getResponseCode();
			CRobotUtil.Debug("TextToSpeechSota", "Get ResponseCode " + iResponseCode);
			if (iResponseCode == 200) {
				result = true;
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (os != null) {
					os.flush();
					os.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (connection != null) {
				connection.disconnect();
			}
		}
		return result;
	}
}
