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

	/** ponderの有無 */
	private boolean ponderFlg = false;

	/** bestmoveコマンド （例）「bestmove 3i4h ponder 3c8h+」 */
	private String bestmoveCommand;
	/** 直近の読み筋 （例）「info depth 3 seldepth 4 multipv 1 score cp 502 nodes 774 nps 154800 time 5 pv 2g3g 4h3g 2c3d S*3f」 */
	private String lastPv;

	/** 【前回の値】bestmoveコマンド （例）「bestmove 3i4h ponder 3c8h+」 */
	private String prev_bestmoveCommand;
	/** 【前回の値】直近の読み筋 （例）「info depth 3 seldepth 4 multipv 1 score cp 502 nodes 774 nps 154800 time 5 pv 2g3g 4h3g 2c3d S*3f」 */
	private String prev_lastPv;

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
	public void clearBestmoveLastPv() {
		this.bestmoveCommand = null;
		this.lastPv = null;
	}

	/**
	 * 直近の評価値（文字列）を取得
	 * 
	 * @return
	 */
	public String getLastStrScore() {
		return ShogiUtils.getStrScoreFromInfoPv(lastPv);
	}

	/**
	 * 直近の評価値（数値）を取得
	 * 
	 * @return
	 */
	public int getLastScore() {
		// 直近の評価値（文字列）を取得
		// （例）「502」「-1234」「mate 3」「mate -2」「""」
		String strScore = getLastStrScore();

		return ShogiUtils.getScoreFromStrScore(strScore);
	}

	/**
	 * 【前回の値】直近の評価値（文字列）を取得
	 * 
	 * @return
	 */
	public String getPrevLastStrScore() {
		return ShogiUtils.getStrScoreFromInfoPv(prev_lastPv);
	}

	/**
	 * 【前回の値】直近の評価値（数値）を取得
	 * 
	 * @return
	 */
	public int getPrevLastScore() {
		// 【前回の値】直近の評価値（文字列）を取得
		// （例）「502」「-1234」「mate 3」「mate -2」「""」
		String strScore = getPrevLastStrScore();

		return ShogiUtils.getScoreFromStrScore(strScore);
	}

	/**
	 * 2手前の評価値からの上昇分を取得
	 * 
	 * @return
	 */
	public int getScore2TemaeJoushou() {
		int current_score = getLastScore();
		int prev_score = getPrevLastScore();

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
	 * ・本来は「info string」で全角文字はNGかもしれないが．．．
	 * 
	 * @param ponder
	 * @return
	 */
	public String getBestmoveScoreDisp(boolean ponder) {
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
		sb.append(getLastStrScore());
		sb.append(" （前回");
		sb.append(getPrevLastStrScore());
		sb.append(" 差分");

		// 2手前の評価値からの上昇分
		int score2TemaeJousho = getScore2TemaeJoushou();
		if (score2TemaeJousho == Constants.SCORE_NONE) {
			sb.append(" ");
		} else {
			sb.append(score2TemaeJousho);
		}

		sb.append("）] [");
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
		prev_lastPv = lastPv;
	}

	/**
	 * クリア処理
	 */
	public void clear() {
		bestmoveCommand = null;
		lastPv = null;
		prev_bestmoveCommand = null;
		prev_lastPv = null;
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

	public String getLastPv() {
		return lastPv;
	}

	public void setLastPv(String lastPv) {
		this.lastPv = lastPv;
	}

	public boolean isPonderFlg() {
		return ponderFlg;
	}

	public void setPonderFlg(boolean ponderFlg) {
		this.ponderFlg = ponderFlg;
	}

	public String getPrev_bestmoveCommand() {
		return prev_bestmoveCommand;
	}

	public void setPrev_bestmoveCommand(String prev_bestmoveCommand) {
		this.prev_bestmoveCommand = prev_bestmoveCommand;
	}

	public String getPrev_lastPv() {
		return prev_lastPv;
	}

	public void setPrev_lastPv(String prev_lastPv) {
		this.prev_lastPv = prev_lastPv;
	}

	// ------------------------------ 単純なGetter&Setter END ------------------------------

}
