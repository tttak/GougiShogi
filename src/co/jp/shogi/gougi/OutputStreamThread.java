package co.jp.shogi.gougi;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * 出力用スレッド
 */
public class OutputStreamThread extends Thread {

	/** Logger */
	protected static Logger logger = Logger.getLogger(OutputStreamThread.class.getName());

	/** BufferedWriter */
	private BufferedWriter bw;
	/** 表示名 */
	private String dispName;

	/** コマンドリスト */
	private List<String> commandList = new ArrayList<String>();

	/**
	 * コンストラクタ
	 * 
	 * @param os
	 */
	public OutputStreamThread(OutputStream os) {
		bw = new BufferedWriter(new OutputStreamWriter(os));
	}

	/**
	 * コンストラクタ
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

				// コマンドリストが空ではない場合、ローカル変数にコピーしてからクリア
				// ・スレッドの排他制御
				synchronized (this) {
					if (!commandList.isEmpty()) {
						// デバッグ用
						logger.info("out[" + dispName + "]commandListが空ではない!!commandList.size()=" + commandList.size());
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
							// コマンドを出力
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
	 * コマンドリストが空になるまで待つ
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
	 * クローズ処理
	 */
	public void close() {
		Utils.close(bw);
	}

	// ------------------------------ 単純なGetter&Setter START ------------------------------

	public List<String> getCommandList() {
		return commandList;
	}

	public String getDispName() {
		return dispName;
	}

	public void setDispName(String dispName) {
		this.dispName = dispName;
	}

	// ------------------------------ 単純なGetter&Setter END ------------------------------

}
