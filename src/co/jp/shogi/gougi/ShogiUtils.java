package co.jp.shogi.gougi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 将棋関連のユーティリティクラス
 */
public class ShogiUtils {

	/**
	 * 全エンジンのbestmoveコマンドをクリアする
	 * 
	 * @param usiEngineList
	 */
	public static void clearBestmove(List<UsiEngine> usiEngineList) {
		for (UsiEngine engine : usiEngineList) {
			engine.setBestmoveCommand(null);
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
	 * 合議を実行し、合議結果のbestmoveコマンド（ponder部分を除く）を返す
	 * ・多数決合議
	 * ・三者三様の場合はエンジン1の指し手を採用する
	 * 
	 * @param usiEngineList
	 * @return
	 */
	public static String getGougiBestmoveCommandExceptPonder(List<UsiEngine> usiEngineList) {
		// bestmoveごとの件数Map（ponder部分を除く）を作成
		Map<String, Integer> bestmoveCountMap = createBestmoveCountMap(usiEngineList);

		// 三者三様の場合
		if (bestmoveCountMap.size() >= 3) {
			// エンジン1の指し手を採用する
			return getEngine(usiEngineList, 1).getBestmoveCommandExceptPonder();
		}

		// その他の場合（「3対0」または「2対1」の場合）
		else {
			for (String bestmoveCommand : bestmoveCountMap.keySet()) {
				// 「3対0」または「2対1」しかありえないので、2票以上で確定
				if (bestmoveCountMap.get(bestmoveCommand) >= 2) {
					return bestmoveCommand;
				}
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

}
