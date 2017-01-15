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
		try {
			// 直近の評価値（文字列）を取得
			// （例）「502」「-1234」「mate 3」「mate -2」「""」
			String strScore = getLastStrScore();

			if (Utils.isEmpty(strScore)) {
				return Constants.SCORE_NONE;
			}

			if (strScore.startsWith("mate ")) {
				// （例）「3」「-2」
				int mate = Integer.parseInt(Utils.getSplitResult(strScore, " ", 1));

				if (mate > 0) {
					// （例）99997
					return Constants.SCORE_MATE - mate;
				} else if (mate < 0) {
					// （例）-99998
					return -(Constants.SCORE_MATE + mate);
				} else {
					return Constants.SCORE_NONE;
				}
			}

			else {
				// （例）「502」「-1234」
				return Integer.parseInt(strScore);
			}

		} catch (Exception e) {
			return Constants.SCORE_NONE;
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
	 * bestmove、評価値の表示用文字列を取得
	 * （例）「bestmove 7g7f [７六(77)] [評価値 123] [Gikou 20160606]」
	 * 
	 * @return
	 */
	public String getBestmoveScoreDisp() {
		// （例）「bestmove 7g7f」
		String bestmoveExceptPonder = getBestmoveCommandExceptPonder();
		if (bestmoveExceptPonder == null) {
			bestmoveExceptPonder = "";
		}

		// （例）「7g7f」
		String move = Utils.getSplitResult(bestmoveExceptPonder, " ", 1);
		// （例）「7g7f」→「７六(77)」
		// ・本来は「info string」で全角文字はNGかもしれないが．．．
		String moveDispJa = ShogiUtils.getMoveDispJa(move);
		// （例）「bestmove 7g7f [７六(77)] [評価値 123] [Gikou 20160606]」
		return bestmoveExceptPonder + " [" + moveDispJa + "] [評価値 " + getLastStrScore() + "] [" + usiName + "]";
	}

	/**
	 * bestmove、ponder、評価値の表示用文字列を取得
	 * （例）「bestmove 7g7f ponder 3c3d [７六(77) ３四(33)] [評価値 123] [Gikou 20160606]」
	 * 
	 * @return
	 */
	public String getBestmovePonderScoreDisp() {
		StringBuilder sb = new StringBuilder();
		sb.append(bestmoveCommand);
		sb.append(" [");
		sb.append(ShogiUtils.getMoveDispJa(getBestmove()));

		String ponderMove = getPonderMove();
		if (getPonderMove() != null) {
			sb.append(" ");
			sb.append(ShogiUtils.getMoveDispJa(ponderMove));
		}

		sb.append("] [評価値 ");
		sb.append(getLastStrScore());
		sb.append("] [");
		sb.append(usiName);
		sb.append("]");
		return sb.toString();
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

	// ------------------------------ 単純なGetter&Setter END ------------------------------

}
