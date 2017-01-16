package co.jp.shogi.gougi;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 合議ロジック
 */
public class GougiLogic {

	/** Logger */
	protected static Logger logger = Logger.getLogger(GougiLogic.class.getName());

	/** 合議タイプ */
	private String gougiType;

	/** USIエンジンリスト */
	private List<UsiEngine> usiEngineList;

	/**
	 * コンストラクタ
	 * 
	 * @param gougiType
	 * @param usiEngineList
	 */
	public GougiLogic(String gougiType, List<UsiEngine> usiEngineList) {
		super();
		this.gougiType = gougiType;
		this.usiEngineList = usiEngineList;
	}

	/**
	 * 合議を実行
	 * 
	 * @return
	 */
	public UsiEngine execute() {
		// 多数決合議（3者）の場合
		if (Constants.GOUGI_TYPE_TASUUKETSU_3.equals(gougiType)) {
			return gougi_tasuuketsu_3();
		}

		// 楽観合議の場合
		else if (Constants.GOUGI_TYPE_RAKKAN.equals(gougiType)) {
			return gougi_rakkan();
		}

		// 悲観合議の場合
		else if (Constants.GOUGI_TYPE_HIKAN.equals(gougiType)) {
			return gougi_hikan();
		}

		// 2手前の評価値からの上昇分の楽観合議の場合
		else if (Constants.GOUGI_TYPE_2TEMAE_JOUSHOU_RAKKAN.equals(gougiType)) {
			return gougi_2temae_joushou_rakkan();
		}

		// 2手前の評価値からの上昇分の悲観合議の場合
		else if (Constants.GOUGI_TYPE_2TEMAE_JOUSHOU_HIKAN.equals(gougiType)) {
			return gougi_2temae_joushou_hikan();
		}

		// その他の場合
		else {
			return null;
		}
	}

	/**
	 * 多数決合議（3者）を実行
	 * ・多数決で決定
	 * ・三者三様の場合はエンジン1の指し手を採用する
	 * ※このメソッドは戻り値がエンジンだが、多数決で決定された場合は、その指し手の中でエンジン番号が最小のエンジンを返す
	 * 
	 * @return
	 */
	private UsiEngine gougi_tasuuketsu_3() {
		// bestmoveが返ってきていないエンジンが存在する場合
		if (ShogiUtils.containsEmptyBestmove(usiEngineList)) {
			return null;
		}

		// bestmoveごとの件数Map（ponder部分を除く）を作成
		Map<String, Integer> bestmoveCountMap = ShogiUtils.createBestmoveCountMap(usiEngineList);

		// 三者三様の場合
		if (bestmoveCountMap.size() >= Constants.TASUUKETSU_3_ENGINE_COUNT) {
			// エンジン1の指し手を採用する
			return usiEngineList.get(0);
		}

		// その他の場合（「3対0」または「2対1」の場合）
		else {
			for (String bestmoveCommand : bestmoveCountMap.keySet()) {
				// 「3対0」または「2対1」しかありえないので、2票以上で確定
				if (bestmoveCountMap.get(bestmoveCommand) >= 2) {
					// bestmove（ponder部分を除く）が一致するエンジンのうち、エンジン番号が最小のエンジンを返す
					return ShogiUtils.getMinEngine(usiEngineList, bestmoveCommand);
				}
			}
		}

		return null;
	}

	/**
	 * 楽観合議を実行
	 * 
	 * @return
	 */
	private UsiEngine gougi_rakkan() {
		// bestmoveが返ってきていないエンジンが存在する場合
		if (ShogiUtils.containsEmptyBestmove(usiEngineList)) {
			return null;
		}

		// 戻り値用
		UsiEngine result = null;
		// 評価値の最大値
		int max_score = Integer.MIN_VALUE;

		// 評価値が最大のエンジンを求める
		for (UsiEngine engine : usiEngineList) {
			int score = engine.getLastScore();
			if (score != Constants.SCORE_NONE && score > max_score) {
				result = engine;
				max_score = score;
			}
		}

		// この段階で合議結果が決まらない場合、エンジン1をセット
		// ・すべてのエンジンが「SCORE_NONE」の場合など
		if (result == null) {
			result = usiEngineList.get(0);
		}

		return result;
	}

	/**
	 * 悲観合議を実行
	 * 
	 * @return
	 */
	private UsiEngine gougi_hikan() {
		// bestmoveが返ってきていないエンジンが存在する場合
		if (ShogiUtils.containsEmptyBestmove(usiEngineList)) {
			return null;
		}

		// 戻り値用
		UsiEngine result = null;
		// 評価値の最小値
		int min_score = Integer.MAX_VALUE;

		// 評価値が最大のエンジンを求める
		for (UsiEngine engine : usiEngineList) {
			int score = engine.getLastScore();
			if (score != Constants.SCORE_NONE && score < min_score) {
				result = engine;
				min_score = score;
			}
		}

		// この段階で合議結果が決まらない場合、エンジン1をセット
		// ・すべてのエンジンが「SCORE_NONE」の場合など
		if (result == null) {
			result = usiEngineList.get(0);
		}

		return result;
	}

	/**
	 * 2手前の評価値からの上昇分の楽観合議を実行
	 * 
	 * @return
	 */
	private UsiEngine gougi_2temae_joushou_rakkan() {
		// bestmoveが返ってきていないエンジンが存在する場合
		if (ShogiUtils.containsEmptyBestmove(usiEngineList)) {
			return null;
		}

		// 戻り値用
		UsiEngine result = null;
		// 「2手前の評価値からの上昇分」の最大値
		int max_score = Integer.MIN_VALUE;

		// 「2手前の評価値からの上昇分」が最大のエンジンを求める
		for (UsiEngine engine : usiEngineList) {
			int score = engine.getScore2TemaeJoushou();
			if (score != Constants.SCORE_NONE && score > max_score) {
				result = engine;
				max_score = score;
			}
		}

		// この段階で合議結果が決まらない場合、エンジン1をセット
		// ・すべてのエンジンが「SCORE_NONE」の場合など
		if (result == null) {
			result = usiEngineList.get(0);
		}

		return result;
	}

	/**
	 * 2手前の評価値からの上昇分の悲観合議を実行
	 * 
	 * @return
	 */
	private UsiEngine gougi_2temae_joushou_hikan() {
		// bestmoveが返ってきていないエンジンが存在する場合
		if (ShogiUtils.containsEmptyBestmove(usiEngineList)) {
			return null;
		}

		// 戻り値用
		UsiEngine result = null;
		// 「2手前の評価値からの上昇分」の最小値
		int min_score = Integer.MAX_VALUE;

		// 「2手前の評価値からの上昇分」が最大のエンジンを求める
		for (UsiEngine engine : usiEngineList) {
			int score = engine.getScore2TemaeJoushou();
			if (score != Constants.SCORE_NONE && score < min_score) {
				result = engine;
				min_score = score;
			}
		}

		// この段階で合議結果が決まらない場合、エンジン1をセット
		// ・すべてのエンジンが「SCORE_NONE」の場合など
		if (result == null) {
			result = usiEngineList.get(0);
		}

		return result;
	}

	// ------------------------------ 単純なGetter&Setter START ------------------------------

	public String getGougiType() {
		return gougiType;
	}

	public void setGougiType(String gougiType) {
		this.gougiType = gougiType;
	}

	public List<UsiEngine> getUsiEngineList() {
		return usiEngineList;
	}

	public void setUsiEngineList(List<UsiEngine> usiEngineList) {
		this.usiEngineList = usiEngineList;
	}

	// ------------------------------ 単純なGetter&Setter END ------------------------------

}
