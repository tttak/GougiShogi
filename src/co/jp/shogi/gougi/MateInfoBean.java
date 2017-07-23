package co.jp.shogi.gougi;

/**
 * 合議タイプ「詰探索エンジンとの合議（読み筋の局面も詰探索） 」のmateinfoコマンドの送信状況を保持するクラス
 */
public class MateInfoBean {

	/** mateinfoコマンド */
	private String command;

	/** mateinfoコマンドの送信回数 */
	private int count = 0;

	/** mateinfoコマンドの前回送信時刻（System.currentTimeMillis()を使用） */
	private long prevTime;

	/**
	 * コンストラクタ
	 * @param command
	 */
	public MateInfoBean(String command) {
		this.command = command;
	}

	// ------------------------------ 単純なGetter&Setter START ------------------------------

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public long getPrevTime() {
		return prevTime;
	}

	public void setPrevTime(long prevTime) {
		this.prevTime = prevTime;
	}

	// ------------------------------ 単純なGetter&Setter END ------------------------------

}
