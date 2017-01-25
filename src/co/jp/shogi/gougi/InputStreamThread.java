package co.jp.shogi.gougi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * ���͗p�X���b�h
 */
public class InputStreamThread extends Thread {

	/** Logger */
	protected static Logger logger = Logger.getLogger(InputStreamThread.class.getName());

	/** BufferedReader */
	private BufferedReader br;
	/** �\���� */
	private String dispName;

	/** �R�}���h���X�g */
	private List<String> commandList = new ArrayList<String>();

	/**
	 * �R���X�g���N�^
	 * 
	 * @param is
	 */
	public InputStreamThread(InputStream is) {
		br = new BufferedReader(new InputStreamReader(is));
	}

	/**
	 * �R���X�g���N�^
	 * 
	 * @param is
	 * @param dispName
	 */
	public InputStreamThread(InputStream is, String dispName) {
		this(is);
		this.dispName = dispName;
	}

	/* (non-Javadoc)
	 * 
	 * @see java.lang.Thread#run() */
	@Override
	public void run() {
		try {
			String line;
			while ((line = br.readLine()) != null) {
				logger.info("in[" + dispName + "]�R�}���h��M!!" + line);

				// ��M�����R�}���h���R�}���h���X�g�ɒǉ�
				// �E�X���b�h�̔r������
				synchronized (this) {
					commandList.add(line);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			Utils.close(br);
		}
	}

	/**
	 * ����̃R�}���h����M����܂ő҂�
	 * �Etimeout�̓~���b�Ŏw��
	 * 
	 * @param command
	 * @param timeout
	 */
	public void waitUntilCommand(String command, int timeout) {
		try {
			long time_start = System.currentTimeMillis();
			while (true) {
				synchronized (this) {
					if (commandList.contains(command)) {
						break;
					}
				}
				if (System.currentTimeMillis() > time_start + timeout) {
					logger.info("[waitUntilCommand]�^�C���A�E�g�����F" + command + "," + timeout);
					break;
				}
				Thread.sleep(10);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * ����̕�����Ŏn�܂�R�}���h����M����܂ő҂�
	 * �Etimeout�̓~���b�Ŏw��
	 * 
	 * @param prefix
	 * @param timeout
	 */
	public void waitUntilCommandStartsWith(String prefix, int timeout) {
		try {
			long time_start = System.currentTimeMillis();
			while (true) {
				synchronized (this) {
					if (Utils.containsStartsWith(commandList, prefix)) {
						break;
					}
				}
				if (System.currentTimeMillis() > time_start + timeout) {
					logger.info("[waitUntilCommandStartsWith]�^�C���A�E�g�����F" + prefix + "," + timeout);
					break;
				}
				Thread.sleep(10);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * �N���[�Y����
	 */
	public void close() {
		Utils.close(br);
	}

	// ------------------------------ �P����Getter&Setter START ------------------------------

	public List<String> getCommandList() {
		return commandList;
	}

	public String getDispName() {
		return dispName;
	}

	public void setDispName(String dispName) {
		this.dispName = dispName;
	}

	// ------------------------------ �P����Getter&Setter END ------------------------------

}
