package jp.vstone.sotasample;
/*
 *	Copyright (c) 2010 Perceptual Computing Group, Waseda University.
 *	All Rights Reserved.
 *	(www.pcl.cs.waseda.ac.jp)
 */


import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;

/*+
 * �����ς�����Ƃ��ɕύX���K�v�Ȓ萔�����W�߂��N���X.
 *
 * �D�݂̖��ł����Cstatic�����o�ϐ��͒�`�����C�����o�֐��Œ��ڕԂ�l�𐶐����邱�Ƃɂ��܂��D
 * @author fujie
 *
 */
public class Config {
	/** SQLPath�iSCHEMA2�j */
	private static final String SQL_PATH = "10.1.7.7";

	/** SQLPath�i���[�J���j */
//	private static final String SQL_PATH = "localhost";

	/* --- ��� --- */
	/**
	 * �t�@�C���̃t���p�X����Ԃ��D �t�@�C���͂��̃N���X�Ɠ����t�H���_�ɂ��邱�Ƃ�z�肵�Ă���D
	 *
	 * @param filename
	 * @return
	 */
	public static String getFullPath(String filename) {
		URL url = Config.class.getResource(filename);
		String encPath = url.getPath();
		String decPath = null;
		try {
			decPath = URLDecoder.decode(encPath, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return decPath;
	}

	public static String getWorkspacePath() {
		return "C:\\pleiades-e3.5-cpp-jre_20100226\\eclipse\\workspece\\SchemaActionManager\\";
	}

	/* --- MONEA�֌W --- */
	/**
	 * ��ǂŎg��moduleXML�̃t���p�X��Ԃ�
	 *
	 * @param gameType
	 * @return
	 */
	public static String getNandokuModuleXMLPath() {
		return getFullPath("ActionManager.xml");
	}


	/**
	 * MC�p�����F�����remote����Ԃ�
	 *
	 * @param gameType
	 * @return
	 */
	public static String getRemoteNameOfSkoodMC() {
		return "speechRecognizer";
	}

	/**
	 * A�p�����F�����remote����Ԃ�
	 *
	 * @param gameType
	 * @return
	 */
	public static String getRemoteNameOfSkoodA() {
		return "speechRecognizer_A";
	}

	/**
	 * B�p�����F�����remote����Ԃ�
	 *
	 * @param gameType
	 * @return
	 */
	public static String getRemoteNameOfSkoodB() {
		return "speechRecognizer_B";
	}

	/**
	 * C�p�����F�����remote����Ԃ�
	 *
	 * @param gameType
	 * @return
	 */
	public static String getRemoteNameOfSkoodC() {
		return "speechRecognizer_C";
	}

	/**
	 * A�p���������ʊ��remote����Ԃ�
	 *
	 * @param gameType
	 * @return
	 */
	public static String getRemoteNameOfAcousticFeatureA() {
		return "FeatureMakerforA";
	}

	/**
	 * B�p�����F�����remote����Ԃ�
	 *
	 * @param gameType
	 * @return
	 */
	public static String getRemoteNameOfAcousticFeatureB() {
		return "FeatureMakerforB";
	}

	/**
	 * C�p�����F�����remote����Ԃ�
	 *
	 * @param gameType
	 * @return
	 */
	public static String getRemoteNameOfAcousticFeatureC() {
		return "FeatureMakerforC";
	}



	/**
	 * �Ď�����SKOOD�̃^�O
	 *
	 * @param gameType
	 * @return
	 */
	public static String getSkoodTagToWatch() {
		return "NGRAM";
	}

	/**
	 * �����������W���[����remote��
	 */
	public static String getRemoteNameOfSpeechSynthesizer() {
		return "synthesizer";
	}

	/**
	 * ���{�b�g���䃂�W���[����remote��
	 */
	public static String getRemoteNameOfRobotController() {
		return "action";
	}

	/* --- �g�ђ[��������Ď����邽�߂�URL --- */
	/**
	 * �g�ђ[������̊Ď��ׂ̈�URL
	 */
	public static String getURLForSender() {
		/* �{�ԗp */
		return "http://" + SQL_PATH + "/nandoku/senderToMain/send.php";
		// return "/Users/matsuyama/Sites/www/nandoku/senderToMain/send.php";
	}

	/**
	 * �Ď��̂P��ڂɎg��URL
	 *
	 * @return
	 */
	public static String getURLForSenderFirstTime() {
		/* �{�ԗp */
		return "http://" + SQL_PATH + "/nandoku/senderToMain/sendNoWait.php";
		// return
		// "/Users/matsuyama/Sites/www/nandoku/senderToMain/sendNoWait.php";
	}

	public static String getRecoPath() {
		File file = new File("model");
		return file.getAbsolutePath() + "/";
	}

	/* --- ??? --- */
	// TODO �v�m�F
	/**
	 * ���X�N���v�g�̃p�X���擾�i�g���ĂȂ��l�q�j
	 *
	 * @param gameType
	 *            ????
	 */
	@Deprecated
	public static String getScriptPath(int gameType) {
		// TODO ���ɂ���Ă����̐ݒ��ύX����K�v����D
		if (gameType == 1)
			return "X:/www/nations/";
		else
			return "/Users/matsuyama/Sites/www/nandoku";
	}



	/* --- SQL --- */
	/**
	 * ���DB��SQL�p�X���擾
	 *
	 * @return
	 */
	public static String getSQLPath() {
		/* �{�ԗp */
		return "jdbc:mysql://"
				+ SQL_PATH
				+ "/nandoku_dial?user=root&password=schema&useUnicode=true&characterEncoding=SJIS";
	}

	/* ---- (�W��)���O�֌W ---- */
	/** ���K�[�̎擾 */
	public static Logger getLogger() {
		return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	}

	/** ���ړI�Ƀ��O���o�� */
	public static void log(Level level, String msg, Object o) {
		if (o != null)
			Config.getLogger().log(level,
					o.getClass().getSimpleName() + " " + msg);
		else
			Config.getLogger().log(level, msg);
	}

	/** ���ړI�Ƀ��O���o�� */
	public static void log(Level level, String msg) {
		Config.log(level, msg, null);
	}

	/** ���ړI�ɃG���[���O���o�� */
	public static void logError(String msg) {
		Config.getLogger().severe(msg);
	}

	/** ���ړI�Ɍx�����O���o�� */
	public static void logWarn(String msg) {
		Config.getLogger().warning(msg);
	}

	/** ���ړI�ɏ�񃍃O���o�� */
	public static void logInfo(String msg) {
		Config.getLogger().info(msg);
	}

	/** ���ړI�Ƀf�o�b�O���O���o�� */
	public static void logDebug(String msg) {
		Config.getLogger().fine(msg);
	}

	/** ���ړI�ɃG���[���O���o�� */
	public static void logError(String msg, Object o) {
		if (o == null)
			Config.getLogger().severe(msg);
		else
			Config.getLogger().severe(o.getClass().getSimpleName() + " " + msg);
	}

	/** ���ړI�Ɍx�����O���o�� */
	public static void logWarn(String msg, Object o) {
		if (o == null)
			Config.getLogger().warning(msg);
		else
			Config.getLogger()
					.warning(o.getClass().getSimpleName() + " " + msg);
	}

	/** ���ړI�ɏ�񃍃O���o�� */
	public static void logInfo(String msg, Object o) {
		if (o == null)
			Config.getLogger().info(msg);
		else
			Config.getLogger().info(o.getClass().getSimpleName() + " " + msg);
	}

	/** ���ړI�Ƀf�o�b�O���O���o�� */
	public static void logDebug(String msg, Object o) {
		if (o == null)
			Config.getLogger().fine(msg);
		else
			Config.getLogger().fine(o.getClass().getSimpleName() + " " + msg);
	}

	/** ���ړI�Ƀ��O���x����ݒ� */
	public static void setLogLevel(Level level) {
		Config.getLogger().setLevel(level);
	}

	public static String getModuleNandokuXmlPath() {
		File file = new File("src\\ActionManager.xml");
		return file.getAbsolutePath();
	}


}
