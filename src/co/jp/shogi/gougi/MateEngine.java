package co.jp.shogi.gougi;

/**
 * 詰探索エンジンクラス
 */
public class MateEngine extends UsiEngine {

	/**
	 * checkmateの指し手 （例）「R*6a 5a6a 2a4a 6a6b 8b7a」
	 * ・「checkmate nomate」「checkmate timeout」「checkmateのみ」「checkmate notimplemented」の場合はセットされない
	 */
	private String checkmateMoves;

	/** 直近の「go mate」コマンドの局面 */
	private String latestGoMatePosition;

	/** 探索中か否か */
	private boolean searching;

	/**
	 * 表示用文字列を取得
	 * （例）「[5手詰め]　[６一飛打 ６一(51) ４一(21) ６二(61) ７一(82)]　[NanohaTsumeUSI]」
	 * （例）「[詰み無し]　[NanohaTsumeUSI]」
	 * 
	 * @return
	 */
	public String getMateDisp() {
		StringBuilder sb = new StringBuilder();

		if (Utils.isEmpty(checkmateMoves)) {
			sb.append("[詰み無し]");
		} else {
			sb.append("[");
			sb.append(getMateLength());
			sb.append("手詰め]　[");

			// （例）「R*6a 5a6a 2a4a 6a6b 8b7a」
			String[] sa = checkmateMoves.split(" ", -1);

			for (int i = 0; i < sa.length; i++) {
				sb.append(ShogiUtils.getMoveDispJa(sa[i]));
				if (i < sa.length - 1) {
					sb.append(" ");
				}
			}

			sb.append("]");
		}

		sb.append(" [");
		sb.append(getUsiName());
		sb.append("]");

		return sb.toString();
	}

	/**
	 * 詰手数を返す
	 * @return
	 */
	public int getMateLength() {
		if (Utils.isEmpty(checkmateMoves)) {
			return -1;
		} else {
			// （例）「R*6a 5a6a 2a4a 6a6b 8b7a」→「5」
			String[] sa = checkmateMoves.split(" ", -1);
			return sa.length;
		}
	}

	/**
	 * bestmoveを作成して返す
	 * （例）「R*6a」
	 * 
	 * @return
	 */
	public String createBestmove() {
		return Utils.getSplitResult(checkmateMoves, " ", 0);
	}

	/**
	 * bestmoveコマンドを作成して返す
	 * （例）「bestmove R*6a」
	 * 
	 * @return
	 */
	public String createBestmoveCommand() {
		return "bestmove " + createBestmove();
	}

	/**
	 * 読み筋を作成して返す
	 * （例）「info score mate 5 pv R*6a 5a6a 2a4a 6a6b 8b7a」
	 * 
	 * @return
	 */
	public String createPv() {
		if (Utils.isEmpty(checkmateMoves)) {
			return "";
		} else {
			return "info score mate " + getMateLength() + " pv " + checkmateMoves;
		}
	}

	// ------------------------------ 単純なGetter&Setter START ------------------------------

	public String getCheckmateMoves() {
		return checkmateMoves;
	}

	public void setCheckmateMoves(String checkmateMoves) {
		this.checkmateMoves = checkmateMoves;
	}

	public String getLatestGoMatePosition() {
		return latestGoMatePosition;
	}

	public void setLatestGoMatePosition(String latestGoMatePosition) {
		this.latestGoMatePosition = latestGoMatePosition;
	}

	public boolean isSearching() {
		return searching;
	}

	public void setSearching(boolean searching) {
		this.searching = searching;
	}

	// ------------------------------ 単純なGetter&Setter END ------------------------------

}
