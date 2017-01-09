package co.jp.shogi.gougi;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * �o�͗p�X���b�h
 */
public class OutputStreamThread extends Thread {

	/** Logger */
	protected static Logger logger = Logger.getLogger(OutputStreamThread.class.getName());

	/** BufferedWriter */
	private BufferedWriter bw;
	/** �\���� */
	private String dispName;

	/** �R�}���h���X�g */
	private List<String> commandList = new ArrayList<String>();

	/**
	 * �R���X�g���N�^
	 * 
	 * @param os
	 */
	public OutputStreamThread(OutputStream os) {
		bw = new BufferedWriter(new OutputStreamWriter(os));
	}

	/**
	 * �R���X�g���N�^
	 * 
	 * @param os
	 * @param dispName
	 */
	public OutputStreamThread(OutputStream os, String dispName) {
		this(os);
		this.dispName = dispName;
	}

	/* (non-Javadoc)
	 * 
	 * @see java.lang.Thread#run() */
	@Override
	public void run() {
		try {
			while (true) {
				List<String> strlist = null;

				// �R�}���h���X�g����ł͂Ȃ��ꍇ�A���[�J���ϐ��ɃR�s�[���Ă���N���A
				// �E�X���b�h�̔r������
				synchronized (this) {
					if (!commandList.isEmpty()) {
						// �f�o�b�O�p
						logger.info("out[" + dispName + "]commandList����ł͂Ȃ�!!commandList.size()=" + commandList.size());
						for (String cmd : commandList) {
							logger.info("out[" + dispName + "]" + cmd);
						}

						strlist = new ArrayList<String>(commandList);
						commandList.clear();
					}
				}

				if (strlist != null) {
					for (String command : strlist) {
						try {
							// �R�}���h���o��
							bw.write(command);
							bw.newLine();
							bw.flush();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}

				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} finally {
			Utils.close(bw);
		}
	}

	/**
	 * �R�}���h���X�g����ɂȂ�܂ő҂�
	 */
	public void waitUntilEmpty() {
		try {
			while (!commandList.isEmpty()) {
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
		Utils.close(bw);
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
