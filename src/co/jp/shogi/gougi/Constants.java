package co.jp.shogi.gougi;

import java.util.ArrayList;
import java.util.List;

/**
 * 定数定義
 */
public class Constants {

	/** usiコマンドに返す「id name」 */
	public static final String USI_ID_NAME = "SimpleGougiShogi_20170202";
	/** usiコマンドに返す「id author」 */
	public static final String USI_ID_AUTHOR = "t";

	/** 設定ファイルのファイル名 */
	public static final String CONFIG_FILENAME = "SimpleGougiShogi.config";

	/** USIエンジンリストの最大件数 */
	public static final int ENGINE_COUNT_MAX = 10;
	/** 「多数決合議（3者）」の場合のUSIエンジンリストの件数 */
	public static final int ENGINE_COUNT_TASUUKETSU_3 = 3;
	/** 「各々の最善手を交換して評価値の合計で判定（2者）」の場合のUSIエンジンリストの件数 */
	public static final int ENGINE_COUNT_BESTMOVE_EXCHANGE_2 = 2;

	/** 【合議タイプ】多数決合議（3者） */
	public static final String GOUGI_TYPE_TASUUKETSU_3 = "多数決合議（3者）";
	/** 【合議タイプ】楽観合議 */
	public static final String GOUGI_TYPE_RAKKAN = "楽観合議";
	/** 【合議タイプ】悲観合議 */
	public static final String GOUGI_TYPE_HIKAN = "悲観合議";
	/** 【合議タイプ】楽観合議と悲観合議を交互 */
	public static final String GOUGI_TYPE_RAKKAN_HIKAN_BYTURNS = "楽観合議と悲観合議を交互";
	/** 【合議タイプ】2手前の評価値からの上昇分の楽観合議 */
	public static final String GOUGI_TYPE_2TEMAE_JOUSHOU_RAKKAN = "2手前の評価値からの上昇分の楽観合議";
	/** 【合議タイプ】2手前の評価値からの上昇分の悲観合議 */
	public static final String GOUGI_TYPE_2TEMAE_JOUSHOU_HIKAN = "2手前の評価値からの上昇分の悲観合議";
	/** 【合議タイプ】各々の最善手を交換して評価値の合計で判定（2者） */
	public static final String GOUGI_TYPE_BESTMOVE_EXCHANGE_2 = "各々の最善手を交換して評価値の合計で判定（2者）";
	/** 【合議タイプ】数手ごとに対局者交代 */
	public static final String GOUGI_TYPE_CHANGE_PLAYER_PLYS = "数手ごとに対局者交代";
	/** 【合議タイプ】2手前の評価値から一定値以上下降したら対局者交代 */
	public static final String GOUGI_TYPE_CHANGE_PLAYER_SCORE_DOWN = "2手前の評価値から一定値以上下降したら対局者交代";
	/** 【合議タイプ】2手前の評価値から一定値以上上昇したら対局者交代 */
	public static final String GOUGI_TYPE_CHANGE_PLAYER_SCORE_UP = "2手前の評価値から一定値以上上昇したら対局者交代";

	/** 合議タイプのリスト */
	public static final List<String> GOUGI_TYPE_LIST;
	static {
		GOUGI_TYPE_LIST = new ArrayList<String>();
		GOUGI_TYPE_LIST.add(GOUGI_TYPE_TASUUKETSU_3);
		GOUGI_TYPE_LIST.add(GOUGI_TYPE_RAKKAN);
		GOUGI_TYPE_LIST.add(GOUGI_TYPE_HIKAN);
		GOUGI_TYPE_LIST.add(GOUGI_TYPE_RAKKAN_HIKAN_BYTURNS);
		GOUGI_TYPE_LIST.add(GOUGI_TYPE_2TEMAE_JOUSHOU_RAKKAN);
		GOUGI_TYPE_LIST.add(GOUGI_TYPE_2TEMAE_JOUSHOU_HIKAN);
		GOUGI_TYPE_LIST.add(GOUGI_TYPE_BESTMOVE_EXCHANGE_2);
		GOUGI_TYPE_LIST.add(GOUGI_TYPE_CHANGE_PLAYER_PLYS);
		GOUGI_TYPE_LIST.add(GOUGI_TYPE_CHANGE_PLAYER_SCORE_DOWN);
		GOUGI_TYPE_LIST.add(GOUGI_TYPE_CHANGE_PLAYER_SCORE_UP);
	}

	/** 評価値未設定 */
	public static final int SCORE_NONE = Integer.MIN_VALUE + 1;
	/** Mateの評価値 */
	public static final int SCORE_MATE = 100000;

}
