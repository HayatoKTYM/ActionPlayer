package jp.vstone.sotaactionplayer;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.monea.api.ModuleContext;
import org.monea.api.ModuleContextFactory;
import org.monea.api.ModuleException;
import org.monea.api.ModuleLogger;
import org.monea.api.ModuleLoggerFactory;

/**
 *
 * MONEA���g���āC�͂��߂� ModuleContext ���擾���ɍs���ƁC������ �u���b�N�����̂ŁC�����������邽�߂̃��[�e�B���e�B�[�N���X�D
 *
 * @author fujie
 *
 */
public class MONEAConnector {
	/** ���W���[��XML�̃p�X���̃V���O���g�� */
	private static HashMap<String, MONEAConnector> instanceMap = new HashMap<String, MONEAConnector>();

	private MONEAConnectingThread thread = null;

	private ModuleContext moduleContext = null;

	public static MONEAConnector getInstance(String moduleXMLPath) {
		MONEAConnector instance = instanceMap.get(moduleXMLPath);
		if (instance != null)
			return instance;
		instance = new MONEAConnector(moduleXMLPath);
		instanceMap.put(moduleXMLPath, instance);
		return instance;
	}

	private MONEAConnector(String moduleXMLPath) {
		this.thread = new MONEAConnectingThread(this, moduleXMLPath);
		this.thread.start();
	}

	synchronized void setModuleContext(ModuleContext context) {
		this.moduleContext = context;
		this.notifyAll();
	}

	public ModuleContext getModuleContext() {
		return this.moduleContext;
	}

	public synchronized ModuleContext timedGetModuleContext(long timeout) {
		if (this.moduleContext != null)
			return this.moduleContext;
		try {
			if (timeout < 0) {
				this.wait();
			} else {
				this.wait(timeout);
			}
		} catch (InterruptedException e) {
		}
		return this.moduleContext;
	}
}

class MONEAConnectingThread extends Thread {

	private MONEAConnector moneaConnector = null;
	private String moduleXMLPath = null;

	MONEAConnectingThread(MONEAConnector moneaConnector, String moduleXMLPath) {
		this.moneaConnector = moneaConnector;
		this.moduleXMLPath = moduleXMLPath;
	}

	@Override
	public void run() {
		try {
			while (true) {
				ModuleContext context = ModuleContextFactory.newContext(
						new File(this.moduleXMLPath), new MyLoggerFactory());
				if (context != null) {
					this.moneaConnector.setModuleContext(context);
					break;
				}
				Thread.sleep(50);
			}

		} catch (ModuleException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}
}

class MyLoggerFactory implements ModuleLoggerFactory {

	@Override
	public ModuleLogger createLogger(String arg0) {
		return new MyLogger();
	}
}

class MyLogger implements ModuleLogger {
	private Logger logger;


	MyLogger() {
		logger = Config.getLogger();
	}

	public void error(Object msg) {
		//logger.log(Level.SEVERE, msg.toString());
	}

	public void error(Object msg, Throwable failure) {
		//logger.log(Level.SEVERE, msg.toString(), failure);
	}

	public void warn(Object msg) {
		//logger.log(Level.WARNING, msg.toString());
	}

	public void warn(Object msg, Throwable failure) {
		//logger.log(Level.WARNING, msg.toString(), failure);
	}

	public void info(Object msg) {
		//System.err.println("*****" + msg);
		// logger.log(Level.INFO, msg.toString());
	}

	public void info(Object msg, Throwable failure) {
		System.err.println("*****" + msg);
		//failure.printStackTrace();
		// logger.log(Level.INFO, msg.toString(), failure);
	}

	public void debug(Object msg) {
		//System.err.println("*****" + msg);
		// logger.log(Level.INFO, msg.toString());
		// logger.log(Level.FINE, msg.toString());
	}

	public void debug(Object msg, Throwable failure) {
		System.err.println("*****" + msg);
		//failure.printStackTrace();
		// logger.log(Level.INFO, msg.toString(), failure);
	}

	public boolean isErrorEnabled() {
		return logger.isLoggable(Level.SEVERE);
	}

	public boolean isWarnEnabled() {
		return logger.isLoggable(Level.WARNING);
	}

	public boolean isInfoEnabled() {
		return false;
		// return true;
		// return logger.isLoggable(Level.INFO);
	}

	public boolean isDebugEnabled() {
		return false;
		// return true;
		// return logger.isLoggable(Level.FINE);
	}

}
