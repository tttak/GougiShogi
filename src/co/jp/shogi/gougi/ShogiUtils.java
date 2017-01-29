package co.jp.shogi.gougi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 将棋関連のユーティリティクラス
 */
public class ShogiUtils {

	/**
	 * 全エンジンのbestmove、直近の読み筋をクリアする
	 * 
	 * @param usiEngineList
	 */
	public static void clearBestmoveLatestPv(List<UsiEngine> usiEngineList) {
		for (UsiEngine engine : usiEngineList) {
			engine.clearBestmoveLatestPv();
		}
	}

	/**
	 * 全エンジンの【最善手の交換前の値】bestmove、直近の読み筋をクリアする
	 * 
	 * @param usiEngineList
	 */
	public static void clear_before_exchange_BestmoveLatestPv(List<UsiEngine> usiEngineList) {
		for (UsiEngine engine : usiEngineList) {
			engine.clearBestmoveLatestPv();
		}
	}

	/**
	 * bestmoveコマンドが空のエンジンが存在するか否かを返す
	 * 
	 * @param usiEngineList
	 * @return
	 */
	public static boolean containsEmptyBestmove(List<UsiEngine> usiEngineList) {
		for (UsiEngine engine : usiEngineList) {
			if (Utils.isEmpty(engine.getBestmoveCommand())) {
				return true;
			}
		}

		return false;
	}

	/**
	 * bestmoveごとの件数Map（ponder部分を除く）を作成する
	 * 
	 * @param usiEngineList
	 * @return
	 */
	public static Map<String, Integer> createBestmoveCountMap(List<UsiEngine> usiEngineList) {
		Map<String, Integer> map = new HashMap<String, Integer>();

		for (UsiEngine engine : usiEngineList) {
			String command = engine.getBestmoveCommandExceptPonder();
			Utils.addToCountMap(map, command, 1);
		}

		return map;
	}

	/**
	 * エンジン番号が引数engineNumberのエンジンを返す
	 * 
	 * @param usiEngineList
	 * @param engineNumber
	 * @return
	 */
	public static UsiEngine getEngine(List<UsiEngine> usiEngineList, int engineNumber) {
		for (UsiEngine engine : usiEngineList) {
			if (engine.getEngineNumber() == engineNumber) {
				return engine;
			}
		}

		return null;
	}

	/**
	 * bestmove（ponder部分を除く）が引数と一致するエンジンのうち、エンジン番号が最小のエンジンを返す
	 * 
	 * @param usiEngineList
	 * @param bestmoveExceptPonder
	 * @return
	 */
	public static UsiEngine getMinEngine(List<UsiEngine> usiEngineList, String bestmoveExceptPonder) {
		if (Utils.isEmpty(bestmoveExceptPonder)) {
			return null;
		}

		// エンジン番号順に並んでいるはずなので、これでよい
		for (UsiEngine engine : usiEngineList) {
			// bestmove（ponder部分を除く）で比較
			if (bestmoveExceptPonder.equals(engine.getBestmoveCommandExceptPonder())) {
				return engine;
			}
		}

		return null;
	}

	/**
	 * コマンドからオプション名を取得
	 * （例）「option name Threads type spin default 4 min 1 max 256」
	 * → 「Threads」
	 * （例）「setoption name E2_Threads value 1」
	 * → 「E2_Threads」
	 * 
	 * @param command
	 * @return
	 */
	public static String getOptionName(String command) {
		try {
			// 「option name ～」「setoption name ～」以外の場合、nullを返す
			if (!(command.startsWith("option name ") || command.startsWith("setoption name "))) {
				return null;
			}

			String[] sa = command.split(" ", -1);
			return sa[2].trim();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * moveを日本語に変換して返す
	 * （例）「7g7f」→「７六(77)」
	 * （例）「2d2c+」→「２三成(24)」
	 * （例）「P*8g」→「８七歩打」
	 * 
	 * @param move
	 * @return
	 */
	public static String getMoveDispJa(String move) {
		try {
			if ("resign".equals(move)) {
				return "投了";
			}

			String s1 = move.substring(0, 1);
			String s2 = move.substring(1, 2);
			String s3 = move.substring(2, 3);
			String s4 = move.substring(3, 4);
			String s5 = null;

			if (move.length() > 4) {
				s5 = move.substring(4, 5);
			}

			// 駒打ちの場合
			if ("*".equals(s2)) {
				StringBuilder sb = new StringBuilder();
				sb.append(Utils.getZenkaku(Utils.getIntValue(s3, 0)));
				sb.append(Utils.getKanji(Utils.getIntFromAlphabet(s4.charAt(0))));
				sb.append(getPieceTypeDisp(s1));
				sb.append("打");

				return sb.toString();
			}

			// 駒打ちではない場合（駒の移動手の場合）
			else {
				StringBuilder sb = new StringBuilder();
				sb.append(Utils.getZenkaku(Utils.getIntValue(s3, 0)));
				sb.append(Utils.getKanji(Utils.getIntFromAlphabet(s4.charAt(0))));

				if ("+".equals(s5)) {
					sb.append("成");
				}

				sb.append("(");
				sb.append(Utils.getIntValue(s1, 0));
				sb.append(Utils.getIntFromAlphabet(s2.charAt(0)));
				sb.append(")");

				return sb.toString();
			}

		} catch (Exception e) {
			e.printStackTrace();
			return move;
		}
	}

	/**
	 * 駒の日本語名を返す
	 * （例）「K」→「玉」
	 * 
	 * @param pieceType
	 * @return
	 */
	public static String getPieceTypeDisp(String pieceType) {
		if ("K".equals(pieceType)) {
			return "玉";
		} else if ("R".equals(pieceType)) {
			return "飛";
		} else if ("B".equals(pieceType)) {
			return "角";
		} else if ("G".equals(pieceType)) {
			return "金";
		} else if ("S".equals(pieceType)) {
			return "銀";
		} else if ("N".equals(pieceType)) {
			return "桂";
		} else if ("L".equals(pieceType)) {
			return "香";
		} else if ("P".equals(pieceType)) {
			return "歩";
		} else {
			return "";
		}
	}

	/**
	 * 読み筋から評価値（文字列）を取得
	 * （例）「info depth 3 seldepth 4 multipv 1 score cp 502 nodes 774 nps 154800 time 5 pv 2g3g 4h3g 2c3d S*3f」
	 * → 「502」
	 * （例）「info depth 5 seldepth 4 time 2 nodes 1399 nps 699500 hashfull 657 score mate 3 multipv 1 pv 4i2i+ 3h3g G*4g」
	 * → 「mate 3」
	 * （例）「info depth 5 seldepth 3 time 1 nodes 14 nps 14000 hashfull 663 score mate -2 multipv 1 pv 5g5f G*4e」
	 * → 「mate -2」
	 * 
	 * @param infoPv
	 * @return
	 */
	public static String getStrScoreFromInfoPv(String infoPv) {
		try {
			if (Utils.isEmpty(infoPv)) {
				return "";
			}

			if (infoPv.indexOf("score cp ") >= 0) {
				// （例）「info depth 3 seldepth 4 multipv 1 score cp 502 nodes 774 nps 154800 time 5 pv 2g3g 4h3g 2c3d S*3f」
				// → 「502 nodes 774 nps 154800 time 5 pv 2g3g 4h3g 2c3d S*3f」
				String str = infoPv.substring(infoPv.indexOf("score cp ") + "score cp ".length()).trim();
				// （例）「502」
				return Utils.getSplitResult(str, " ", 0);
			}

			else if (infoPv.indexOf("score mate ") >= 0) {
				// （例）「info depth 5 seldepth 4 time 2 nodes 1399 nps 699500 hashfull 657 score mate 3 multipv 1 pv 4i2i+ 3h3g G*4g」
				// → 「3 multipv 1 pv 4i2i+ 3h3g G*4g」
				String str = infoPv.substring(infoPv.indexOf("score mate ") + "score mate ".length()).trim();
				// （例）「mate 3」
				return "mate " + Utils.getSplitResult(str, " ", 0);
			}

			else {
				return "";
			}
		} catch (Exception e) {
			return "";
		}
	}

	/**
	 * 評価値（文字列）から評価値（数値）を取得
	 * （例）「502」→「502」
	 * （例）「-1234」→「-1234」
	 * （例）「mate 3」→「99997」
	 * （例）「mate -2」→「-99998」
	 * （例）「mate」→「Constants.SCORE_NONE」
	 * （例）「""」→「Constants.SCORE_NONE」
	 * 
	 * @param strScore
	 * @return
	 */
	public static int getScoreFromStrScore(String strScore) {
		try {
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
	 * 「両エンジンのbestmoveの指し手とponderの指し手がともに存在し、かつ、等しい」か否かを返す
	 * 
	 * @param engine1
	 * @param engine2
	 * @return
	 */
	public static boolean equalsBestmoveAndPondermove(UsiEngine engine1, UsiEngine engine2) {
		if (Utils.isEmpty(engine1.getBestmove())) {
			return false;
		}
		if (Utils.isEmpty(engine2.getBestmove())) {
			return false;
		}
		if (Utils.isEmpty(engine1.getPonderMove())) {
			return false;
		}
		if (Utils.isEmpty(engine2.getPonderMove())) {
			return false;
		}

		return engine1.getBestmove().equals(engine2.getBestmove()) && engine1.getPonderMove().equals(engine2.getPonderMove());
	}

	/**
	 * 局面に指し手を追加
	 * （例）「position startpos moves 7g7f」 + 「3c3d」 → 「position startpos moves 7g7f 3c3d」
	 * （例）「position startpos」 + 「7g7f」 → 「position startpos moves 7g7f」
	 * 
	 * @param position
	 * @param move
	 * @return
	 */
	public static String appendMove(String position, String move) {
		if (position.contains(" moves ")) {
			return position + " " + move;
		} else {
			return position + " moves " + move;
		}
	}

	/**
	 * 全エンジンについて、「事前ponder実施中か否か」のフラグをセットする
	 * 
	 * @param usiEngineList
	 * @param prePondering
	 */
	public static void setPrePondering(List<UsiEngine> usiEngineList, boolean prePondering) {
		for (UsiEngine engine : usiEngineList) {
			engine.setPrePondering(prePondering);
		}
	}

	/**
	 * 「go」コマンドの時間オプションの先後を入れ替える
	 * 
	 * （例）「btime 60000 wtime 50000 byoyomi 10000」
	 * →「wtime 60000 btime 50000 byoyomi 10000」
	 * （例）「btime 40000 wtime 50000 binc 10000 winc 8000」
	 * →「wtime 40000 btime 50000 winc 10000 binc 8000」
	 * 
	 * @param option
	 * @return
	 */
	public static String reverseGoTimeOption(String option) {
		try {
			String s = option;

			s = s.replaceAll("btime", "temp");
			s = s.replaceAll("wtime", "btime");
			s = s.replaceAll("temp", "wtime");

			s = s.replaceAll("binc", "temp");
			s = s.replaceAll("winc", "binc");
			s = s.replaceAll("temp", "winc");

			return s;
		} catch (Exception e) {
			return option;
		}
	}

	/**
	 * 全エンジンにコマンド送信
	 * 
	 * @param usiEngineList
	 * @param command
	 */
	public static void sendCommandToAllEngines(List<UsiEngine> usiEngineList, String command) {
		// 各エンジンへのコマンドリストに追加
		for (UsiEngine engine : usiEngineList) {
			// ・スレッドの排他制御
			synchronized (engine.getOutputThread()) {
				engine.getOutputThread().getCommandList().add(command);
			}
		}
	}

}
