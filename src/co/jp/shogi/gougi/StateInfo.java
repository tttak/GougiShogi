package co.jp.shogi.gougi;

import java.util.logging.Logger;

/**
 * 現在の状態を保持するクラス
 */
public class StateInfo {

	/** Logger */
	protected static Logger logger = Logger.getLogger(StateInfo.class.getName());

	/** 対局中か否か */
	private boolean duringGame;
	/** 自分の手番か否か */
	private boolean myTurn;
	/** ponder実施中か否か */
	private boolean pondering = false;

	/** 直近の「position」コマンドの局面（「go（ponderではない）」か「go ponder」かは問わない） */
	// （例）「position startpos moves 7g7f」
	// （例）「position startpos」
	private String latestPosition;

	/** 直近の「go（ponderではない）」コマンドの局面 */
	private String latestGoNotPonderPosition;
	/** 直近の「go ponder」コマンドの局面 */
	private String latestGoPonderPosition;

	// ----- 時間計測用

	/** 標準入力（GUI）からの直近の「go」コマンドまたは「go ponder」コマンド */
	private String latestSysinGoCommand;
	/** 標準入力（GUI）からの直近の「go」コマンドまたは「go ponder」コマンド時のタイムスタンプ */
	private long nanoTimeAtlatestSysinGoCommand;

	// ----- 合議タイプ「各々の最善手を交換して評価値の合計で判定（2者）」の場合に使用
	/** 最善手の交換前か否か */
	private boolean before_exchange_flg = true;

	// ----- 合議タイプ「数手ごとに対局者交代」の場合に使用
	/** 対局者を交代する手数 */
	private int changePlayerPlys = 1;

	// ----- 合議タイプ「2手前の評価値から一定値以上下降したら対局者交代」「2手前の評価値から一定値以上上昇したら対局者交代」の場合に使用
	/** 対局者を交代する評価値の差分 */
	private int changePlayerScoreDiff = 100;

	// ----- 合議タイプ「詰探索エンジンとの合議」の場合に使用
	/** 詰探索エンジンのタイムアウト（ミリ秒） */
	private int mateTimeout = 10000;

	// ---------- Singleton化 START ----------

	private StateInfo() {
	}

	private static class SingletonHolder {
		private static final StateInfo instance = new StateInfo();
	}

	public static StateInfo getInstance() {
		return SingletonHolder.instance;
	}

	// ---------- Singleton化 END ----------

	/**
	 * クリア処理
	 */
	public void clear() {
		myTurn = false;
		latestGoNotPonderPosition = null;
		latestGoPonderPosition = null;
		latestPosition = null;
	}

	/**
	 * 標準入力（GUI）からの直近の「go」コマンドまたは「go ponder」コマンドをセット
	 * ・タイムスタンプもセットする
	 * 
	 * @param latestSysinGoCommand
	 */
	public void setLatestSysinGoCommand(String latestSysinGoCommand) {
		this.latestSysinGoCommand = latestSysinGoCommand;
		this.nanoTimeAtlatestSysinGoCommand = System.nanoTime();
	}

	/**
	 * 自分が先手か否かを返す
	 * ・直近の「position」コマンドのmovesの後の指し手の数で判定する
	 * （例）「position startpos」　→　先手
	 * （例）「position startpos moves 7g7f」　→　後手
	 * （例）「position startpos moves 7g7f 3c3d」　→　先手
	 * 
	 * @return
	 */
	public boolean isSente() {
		// 直近の「position」コマンドが存在しない場合、判定不能だがtrueを返しておく
		if (Utils.isEmpty(latestPosition)) {
			return true;
		}

		// 直近の「position」コマンドが「position startpos」で始まらない場合、判定不能だがtrueを返しておく
		if (!latestPosition.startsWith("position startpos")) {
			return true;
		}

		// 先手
		if (latestPosition.equals("position startpos")) {
			return true;
		}

		// 判定不能だがtrueを返しておく
		if (!latestPosition.startsWith("position startpos moves ")) {
			return true;
		}

		// latestPosition全体のスペースで区切った件数で判定する
		String[] sa = latestPosition.split(" ", -1);
		if (sa.length % 2 == 0) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * 「go」コマンドまたは「go ponder」コマンドの時間オプションを返す
	 * ・直近の「go」コマンドまたは「go ponder」コマンドのbtimeまたはwtime部分を書き換えて返す
	 * ・直近の「go」コマンドまたは「go ponder」コマンド受信時からの経過時間をbtimeまたはwtimeから引く（引いた結果が0より小さい場合は0）
	 * 
	 * ・自分が先手で、直近のコマンドからの経過時間が1秒の場合
	 * （例）「btime 60000 wtime 50000 byoyomi 10000」
	 * →「btime 59000 wtime 50000 byoyomi 10000」
	 * （例）「btime 40000 wtime 50000 binc 10000 winc 10000」
	 * →「btime 39000 wtime 50000 binc 10000 winc 10000」
	 * （例）「btime 0 wtime 0 byoyomi 2000」
	 * →「btime 0 wtime 0 byoyomi 2000」
	 * 
	 * @return
	 */
	public String getGoTimeOption() {
		// デフォルト値
		final String DEFAULT_VALUE = "btime 0 wtime 0 byoyomi 10000";

		try {
			if (Utils.isEmpty(latestSysinGoCommand) || !latestSysinGoCommand.startsWith("go ")) {
				return DEFAULT_VALUE;
			}

			String[] sa = latestSysinGoCommand.split(" ", -1);
			int timeIndex = Integer.MIN_VALUE;

			for (int i = 0; i < sa.length; i++) {
				if (isSente() && "btime".equals(sa[i]) || !isSente() && "wtime".equals(sa[i])) {
					timeIndex = i;
					break;
				}
			}

			int time_before = Integer.parseInt(sa[timeIndex + 1]);
			int time_diff = (int) ((System.nanoTime() - nanoTimeAtlatestSysinGoCommand) / 1000000);
			int time_after = time_before - time_diff;

			logger.info("time_before=" + time_before);
			logger.info("time_diff=" + time_diff);
			logger.info("time_after=" + time_after);

			if (time_after < 0) {
				time_after = 0;
			}

			StringBuilder sb = new StringBuilder();

			for (int i = 0; i < sa.length; i++) {
				if ("go".equals(sa[i]) || "ponder".equals(sa[i])) {
					continue;
				}

				if (i == timeIndex + 1) {
					sb.append(time_after);
				} else {
					sb.append(sa[i]);
				}

				sb.append(" ");
			}

			return sb.toString().trim();
		} catch (Exception e) {
			return DEFAULT_VALUE;
		}
	}

	/**
	 * 「go」コマンドの時間オプションを返す
	 * ・直近の「go」コマンドで、自分の持ち時間部分を引数value倍する
	 * 
	 * ・自分が先手で、valueが0.4の場合
	 * （例）「go btime 60000 wtime 50000 byoyomi 10000」
	 * →「btime 24000 wtime 50000 byoyomi 4000」
	 * （例）「go ponder btime 40000 wtime 50000 binc 10000 winc 10000」
	 * →「btime 16000 wtime 50000 binc 4000 winc 10000」
	 * （例）「go btime 0 wtime 0 byoyomi 2000」
	 * →「btime 0 wtime 0 byoyomi 800」
	 * 
	 * @return
	 */
	public String getGoTimeOption(double value) {
		// デフォルト値
		final String DEFAULT_VALUE = "btime 0 wtime 0 byoyomi 10000";

		try {
			if (Utils.isEmpty(latestSysinGoCommand) || !latestSysinGoCommand.startsWith("go ")) {
				return DEFAULT_VALUE;
			}

			String[] sa = latestSysinGoCommand.split(" ", -1);
			StringBuilder sb = new StringBuilder();

			boolean nextFlg = false;

			for (int i = 0; i < sa.length; i++) {
				if ("go".equals(sa[i]) || "ponder".equals(sa[i])) {
					continue;
				}

				if (nextFlg) {
					nextFlg = false;
					sb.append((int) (Integer.parseInt(sa[i]) * value));
				} else {
					sb.append(sa[i]);
					if (isSente() && ("btime".equals(sa[i]) || "binc".equals(sa[i]))) {
						nextFlg = true;
					} else if (!isSente() && ("wtime".equals(sa[i]) || "winc".equals(sa[i]))) {
						nextFlg = true;
					} else if ("byoyomi".equals(sa[i])) {
						nextFlg = true;
					} else {
						nextFlg = false;
					}
				}

				sb.append(" ");
			}

			return sb.toString().trim();
		} catch (Exception e) {
			return DEFAULT_VALUE;
		}
	}

	// ------------------------------ 単純なGetter&Setter START ------------------------------

	public boolean isMyTurn() {
		return myTurn;
	}

	public void setMyTurn(boolean myTurn) {
		this.myTurn = myTurn;
	}

	public String getLatestGoNotPonderPosition() {
		return latestGoNotPonderPosition;
	}

	public void setLatestGoNotPonderPosition(String latestGoNotPonderPosition) {
		this.latestGoNotPonderPosition = latestGoNotPonderPosition;
	}

	public String getLatestGoPonderPosition() {
		return latestGoPonderPosition;
	}

	public void setLatestGoPonderPosition(String latestGoPonderPosition) {
		this.latestGoPonderPosition = latestGoPonderPosition;
	}

	public String getLatestPosition() {
		return latestPosition;
	}

	public void setLatestPosition(String latestPosition) {
		this.latestPosition = latestPosition;
	}

	public boolean isPondering() {
		return pondering;
	}

	public void setPondering(boolean pondering) {
		this.pondering = pondering;
	}

	public String getLatestSysinGoCommand() {
		return latestSysinGoCommand;
	}

	public long getNanoTimeAtlatestSysinGoCommand() {
		return nanoTimeAtlatestSysinGoCommand;
	}

	public boolean isDuringGame() {
		return duringGame;
	}

	public void setDuringGame(boolean duringGame) {
		this.duringGame = duringGame;
	}

	public boolean isBefore_exchange_flg() {
		return before_exchange_flg;
	}

	public void setBefore_exchange_flg(boolean before_exchange_flg) {
		this.before_exchange_flg = before_exchange_flg;
	}

	public int getChangePlayerPlys() {
		return changePlayerPlys;
	}

	public void setChangePlayerPlys(int changePlayerPlys) {
		this.changePlayerPlys = changePlayerPlys;
	}

	public int getChangePlayerScoreDiff() {
		return changePlayerScoreDiff;
	}

	public void setChangePlayerScoreDiff(int changePlayerScoreDiff) {
		this.changePlayerScoreDiff = changePlayerScoreDiff;
	}

	public int getMateTimeout() {
		return mateTimeout;
	}

	public void setMateTimeout(int mateTimeout) {
		this.mateTimeout = mateTimeout;
	}

	// ------------------------------ 単純なGetter&Setter END ------------------------------

}
