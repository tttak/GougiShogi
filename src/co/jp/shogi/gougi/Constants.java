package co.jp.shogi.gougi;

import java.util.ArrayList;
import java.util.List;

/**
 * 定数定義
 */
public class Constants {

	/** usiコマンドに返す「id name」 */
	public static final String USI_ID_NAME = "SimpleGougiShogi_20170117";
	/** usiコマンドに返す「id author」 */
	public static final String USI_ID_AUTHOR = "t";

	/** 設定ファイルのファイル名 */
	public static final String CONFIG_FILENAME = "SimpleGougiShogi.config";

	/** USIエンジンリストの最大件数 */
	public static final int MAX_ENGINE_COUNT = 10;
	/** 多数決合議（3者）の場合のUSIエンジンリストの件数 */
	public static final int TASUUKETSU_3_ENGINE_COUNT = 3;

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
	}

	/** 評価値未設定 */
	public static final int SCORE_NONE = Integer.MIN_VALUE + 1;
	/** Mateの評価値 */
	public static final int SCORE_MATE = 100000;

}
