package co.jp.shogi.gougi;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * USIエンジンクラス
 */
public class UsiEngine {

	/** 実行ファイル （例）「C:/将棋/Gikou/gikou.exe」 */
	private File exeFile;
	/** USI名 （例）「Gikou 20160606」　 */
	private String usiName;
	/** エンジン番号（1,2,3,...,エンジン件数） */
	private int engineNumber;

	/** プロセス */
	private Process process;
	/** inputスレッド */
	private InputStreamThread inputThread;
	/** outputスレッド */
	private OutputStreamThread outputThread;

	/** USIオプションのSet */
	private Set<String> optionSet = new HashSet<String>();

	/** bestmoveコマンド （例）「bestmove 3i4h ponder 3c8h+」 */
	private String bestmoveCommand;
	/** 直近の読み筋 （例）「info depth 3 seldepth 4 multipv 1 score cp 502 nodes 774 nps 154800 time 5 pv 2g3g 4h3g 2c3d S*3f」 */
	private String latestPv;

	/** 【前回の値】bestmoveコマンド （例）「bestmove 3i4h ponder 3c8h+」 */
	private String prev_bestmoveCommand;
	/** 【前回の値】直近の読み筋 （例）「info depth 3 seldepth 4 multipv 1 score cp 502 nodes 774 nps 154800 time 5 pv 2g3g 4h3g 2c3d S*3f」 */
	private String prev_latestPv;

	// ----- 合議タイプ「各々の最善手を交換して評価値の合計で判定（2者）」の場合に使用
	/** 【最善手の交換前の値】bestmoveコマンド （例）「bestmove 3i4h ponder 3c8h+」 */
	private String before_exchange_bestmoveCommand;
	/** 【最善手の交換前の値】直近の読み筋 （例）「info depth 3 seldepth 4 multipv 1 score cp 502 nodes 774 nps 154800 time 5 pv 2g3g 4h3g 2c3d S*3f」 */
	private String before_exchange_latestPv;
	// -----

	/** ponder設定のon/off */
	private boolean ponderOnOff = false;
	/** ponder実施中か否か */
	private boolean pondering = false;
	/** ponder実施中の局面 */
	private String ponderingPosition;

	/** 事前ponder実施中か否か */
	private boolean prePondering = false;

	/**
	 * プロセスの終了
	 */
	public void destroy() {
		if (inputThread != null) {
			inputThread.close();
		}
		if (outputThread != null) {
			outputThread.close();
		}

		Utils.destroy(process);
	}

	/**
	 * このエンジンの表示用文字列を返す
	 * （例）「Engine1」
	 * 
	 * @return
	 */
	public String getEngineDisp() {
		return "Engine" + getEngineNumber();
	}

	/**
	 * このエンジンのUSIオプションのプレフィックスを返す
	 * （例）「E1_」
	 * 
	 * @return
	 */
	public String getOptionNamePrefix() {
		return "E" + getEngineNumber() + "_";
	}

	/**
	 * bestmoveコマンド（ponder部分を除く）を返す
	 * （例）「bestmove 3i4h」
	 * 
	 * @return
	 */
	public String getBestmoveCommandExceptPonder() {
		if (bestmoveCommand == null) {
			return null;
		}

		int index = bestmoveCommand.indexOf("ponder");
		if (index >= 0) {
			// （例）「bestmove 3i4h ponder 3c8h+」→「bestmove 3i4h」
			return bestmoveCommand.substring(0, index).trim();
		} else {
			return bestmoveCommand.trim();
		}
	}

	/**
	 * bestmove、直近の読み筋のクリア
	 */
	public void clearBestmoveLatestPv() {
		this.bestmoveCommand = null;
		this.latestPv = null;
	}

	/**
	 * 【最善手の交換前の値】bestmove、直近の読み筋のクリア
	 */
	public void clear_before_exchange_BestmoveLatestPv() {
		this.before_exchange_bestmoveCommand = null;
		this.before_exchange_latestPv = null;
	}

	/**
	 * 直近の評価値（文字列）を取得
	 * 
	 * @return
	 */
	public String getLatestStrScore() {
		return ShogiUtils.getStrScoreFromInfoPv(latestPv);
	}

	/**
	 * 直近の評価値（数値）を取得
	 * 
	 * @return
	 */
	public int getLatestScore() {
		// 直近の評価値（文字列）を取得
		// （例）「502」「-1234」「mate 3」「mate -2」「""」
		String strScore = getLatestStrScore();

		return ShogiUtils.getScoreFromStrScore(strScore);
	}

	/**
	 * 【前回の値】直近の評価値（文字列）を取得
	 * 
	 * @return
	 */
	public String getPrevLatestStrScore() {
		return ShogiUtils.getStrScoreFromInfoPv(prev_latestPv);
	}

	/**
	 * 【前回の値】直近の評価値（数値）を取得
	 * 
	 * @return
	 */
	public int getPrevLatestScore() {
		// 【前回の値】直近の評価値（文字列）を取得
		// （例）「502」「-1234」「mate 3」「mate -2」「""」
		String strScore = getPrevLatestStrScore();

		return ShogiUtils.getScoreFromStrScore(strScore);
	}

	/**
	 * 【最善手の交換前の値】直近の評価値（文字列）を取得
	 * 
	 * @return
	 */
	public String get_before_exchange_LatestStrScore() {
		return ShogiUtils.getStrScoreFromInfoPv(before_exchange_latestPv);
	}

	/**
	 * 【最善手の交換前の値】直近の評価値（数値）を取得
	 * 
	 * @return
	 */
	public int get_before_exchange_LatestScore() {
		// 【最善手の交換前の値】直近の評価値（文字列）を取得
		// （例）「502」「-1234」「mate 3」「mate -2」「""」
		String strScore = get_before_exchange_LatestStrScore();

		return ShogiUtils.getScoreFromStrScore(strScore);
	}

	/**
	 * 2手前の評価値からの上昇分を取得
	 * 
	 * @return
	 */
	public int getScore2TemaeJoushou() {
		int current_score = getLatestScore();
		int prev_score = getPrevLatestScore();

		if (current_score == Constants.SCORE_NONE || prev_score == Constants.SCORE_NONE) {
			return Constants.SCORE_NONE;
		} else {
			return current_score - prev_score;
		}
	}

	/**
	 * bestmoveの指し手を取得
	 * （例）「bestmove 3i4h ponder 3c8h+」→「3i4h」
	 * 
	 * @return
	 */
	public String getBestmove() {
		return getBestmove(bestmoveCommand);
	}

	/**
	 * 【最善手の交換前の値】bestmoveの指し手を取得
	 * （例）「bestmove 3i4h ponder 3c8h+」→「3i4h」
	 * 
	 * @return
	 */
	public String get_before_exchange_Bestmove() {
		return getBestmove(before_exchange_bestmoveCommand);
	}

	/**
	 * bestmoveの指し手を取得
	 * （例）「bestmove 3i4h ponder 3c8h+」→「3i4h」
	 * 
	 * @return
	 */
	private static String getBestmove(String bestmoveCommand) {
		try {
			if (bestmoveCommand == null) {
				return null;
			}

			if (!bestmoveCommand.startsWith("bestmove ")) {
				return null;
			}

			// （例）「bestmove 3i4h ponder 3c8h+」→「3i4h」
			return Utils.getSplitResult(bestmoveCommand, " ", 1).trim();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * ponderの指し手を取得
	 * （例）「bestmove 3i4h ponder 3c8h+」→「3c8h+」
	 * 
	 * @return
	 */
	public String getPonderMove() {
		try {
			if (bestmoveCommand == null) {
				return null;
			}

			int index = bestmoveCommand.indexOf("ponder ");
			if (index < 0) {
				return null;
			}

			// （例）「bestmove 3i4h ponder 3c8h+」→「3c8h+」
			return bestmoveCommand.substring(index + "ponder ".length()).trim();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * bestmove、ponder、評価値の表示用文字列を取得
	 * （例）引数ponderがtrueの場合　「bestmove 7g7f ponder 3c3d [７六(77) ３四(33)] [評価値 123 （前回100 差分23）] [Gikou 20160606]」
	 * （例）引数ponderがfalseの場合「bestmove 7g7f [７六(77)] [評価値 123 （前回100 差分23）] [Gikou 20160606]」
	 * ・引数prev_diffがtrueの場合、（前回100 差分23）の部分を省略する
	 * ・本来は「info string」で全角文字はNGかもしれないが．．．
	 * 
	 * @param ponder
	 * @param prev_diff
	 * @return
	 */
	public String getBestmoveScoreDisp(boolean ponder, boolean prev_diff) {
		StringBuilder sb = new StringBuilder();

		// （例）「bestmove 7g7f」
		String bestmoveExceptPonder = getBestmoveCommandExceptPonder();
		if (bestmoveExceptPonder == null) {
			bestmoveExceptPonder = "";
		}

		if (ponder) {
			// （例）「bestmove 7g7f ponder 3c3d」
			sb.append(bestmoveCommand);
		} else {
			// （例）「bestmove 7g7f」
			sb.append(bestmoveExceptPonder);
		}

		sb.append(" [");
		// （例）「７六(77)」
		sb.append(ShogiUtils.getMoveDispJa(getBestmove()));

		// （例）「3c3d」
		String ponderMove = getPonderMove();
		if (ponder) {
			if (ponderMove != null) {
				sb.append(" ");
				// （例）「３四(33)」
				sb.append(ShogiUtils.getMoveDispJa(ponderMove));
			}
		}

		sb.append("] [評価値 ");
		sb.append(getLatestStrScore());

		if (prev_diff) {
			sb.append(" （前回");
			sb.append(getPrevLatestStrScore());
			sb.append(" 差分");

			// 2手前の評価値からの上昇分
			int score2TemaeJousho = getScore2TemaeJoushou();
			if (score2TemaeJousho == Constants.SCORE_NONE) {
				sb.append(" ");
			} else {
				sb.append(score2TemaeJousho);
			}

			sb.append("）");
		}

		sb.append("] [");
		sb.append(usiName);
		sb.append("]");

		return sb.toString();
	}

	/**
	 * 【前回の値】を保存
	 */
	public void savePrevValue() {
		// 【前回の値】bestmoveコマンド
		prev_bestmoveCommand = bestmoveCommand;
		// 【前回の値】直近の読み筋
		prev_latestPv = latestPv;
	}

	/**
	 * 【最善手の交換前の値】を保存
	 */
	public void save_before_exchange_Value() {
		// 【最善手の交換前の値】bestmoveコマンド
		before_exchange_bestmoveCommand = bestmoveCommand;
		// 【最善手の交換前の値】直近の読み筋
		before_exchange_latestPv = latestPv;
	}

	/**
	 * クリア処理
	 */
	public void clear() {
		bestmoveCommand = null;
		latestPv = null;
		prev_bestmoveCommand = null;
		prev_latestPv = null;
		before_exchange_bestmoveCommand = null;
		before_exchange_latestPv = null;
	}

	// ------------------------------ 単純なGetter&Setter START ------------------------------

	public File getExeFile() {
		return exeFile;
	}

	public void setExeFile(File exeFile) {
		this.exeFile = exeFile;
	}

	public String getUsiName() {
		return usiName;
	}

	public void setUsiName(String usiName) {
		this.usiName = usiName;
	}

	public int getEngineNumber() {
		return engineNumber;
	}

	public void setEngineNumber(int engineNumber) {
		this.engineNumber = engineNumber;
	}

	public Process getProcess() {
		return process;
	}

	public void setProcess(Process process) {
		this.process = process;
	}

	public InputStreamThread getInputThread() {
		return inputThread;
	}

	public void setInputThread(InputStreamThread inputThread) {
		this.inputThread = inputThread;
	}

	public OutputStreamThread getOutputThread() {
		return outputThread;
	}

	public void setOutputThread(OutputStreamThread outputThread) {
		this.outputThread = outputThread;
	}

	public String getBestmoveCommand() {
		return bestmoveCommand;
	}

	public void setBestmoveCommand(String bestmoveCommand) {
		this.bestmoveCommand = bestmoveCommand;
	}

	public Set<String> getOptionSet() {
		return optionSet;
	}

	public void setOptionSet(Set<String> optionSet) {
		this.optionSet = optionSet;
	}

	public String getLatestPv() {
		return latestPv;
	}

	public void setLatestPv(String latestPv) {
		this.latestPv = latestPv;
	}

	public String getPrev_bestmoveCommand() {
		return prev_bestmoveCommand;
	}

	public void setPrev_bestmoveCommand(String prev_bestmoveCommand) {
		this.prev_bestmoveCommand = prev_bestmoveCommand;
	}

	public String getPrev_latestPv() {
		return prev_latestPv;
	}

	public void setPrev_latestPv(String prev_latestPv) {
		this.prev_latestPv = prev_latestPv;
	}

	public boolean isPonderOnOff() {
		return ponderOnOff;
	}

	public void setPonderOnOff(boolean ponderOnOff) {
		this.ponderOnOff = ponderOnOff;
	}

	public boolean isPondering() {
		return pondering;
	}

	public void setPondering(boolean pondering) {
		this.pondering = pondering;
	}

	public String getPonderingPosition() {
		return ponderingPosition;
	}

	public void setPonderingPosition(String ponderingPosition) {
		this.ponderingPosition = ponderingPosition;
	}

	public boolean isPrePondering() {
		return prePondering;
	}

	public void setPrePondering(boolean prePondering) {
		this.prePondering = prePondering;
	}

	public String getBefore_exchange_bestmoveCommand() {
		return before_exchange_bestmoveCommand;
	}

	public void setBefore_exchange_bestmoveCommand(String before_exchange_bestmoveCommand) {
		this.before_exchange_bestmoveCommand = before_exchange_bestmoveCommand;
	}

	public String getBefore_exchange_latestPv() {
		return before_exchange_latestPv;
	}

	public void setBefore_exchange_latestPv(String before_exchange_latestPv) {
		this.before_exchange_latestPv = before_exchange_latestPv;
	}

	// ------------------------------ 単純なGetter&Setter END ------------------------------

}
