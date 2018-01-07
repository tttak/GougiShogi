package co.jp.shogi.gougi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * 設定ファイル関連クラス
 */
public class GougiConfig {

	/** Logger */
	protected static Logger logger = Logger.getLogger(GougiConfig.class.getName());

	/** 合議タイプ */
	private String gougiType;
	/** USIエンジンリスト */
	private List<UsiEngine> usiEngineList;
	/** 詰探索エンジン */
	private MateEngine mateEngine;

	/** 合議タイプ「詰探索エンジンとの合議（読み筋の局面も詰探索）」の場合に、読み筋局面を探索する詰探索エンジンの数 */
	private int pvMateEngineCount = Constants.PV_MATE_ENGINE_CNT_DEFAULT;

	// ---------- Singleton化 START ----------

	private GougiConfig() {
	}

	private static class SingletonHolder {
		private static final GougiConfig instance = new GougiConfig();
	}

	public static GougiConfig getInstance() {
		return SingletonHolder.instance;
	}

	// ---------- Singleton化 END ----------

	/**
	 * 設定ファイル読込み
	 * 
	 * @throws IOException
	 */
	public void readConfigFile() throws IOException {
		List<UsiEngine> list = new ArrayList<UsiEngine>();
		BufferedReader br = null;

		// 初期化
		gougiType = null;
		usiEngineList = null;
		mateEngine = null;

		try {
			br = new BufferedReader(new FileReader(new File(getConfigFilePath())));

			String line;

			// 1行ずつ読み込む
			while ((line = br.readLine()) != null) {
				logger.info(line);
				line = line.trim();

				// 合議タイプの設定
				if (line.startsWith("Gougi.Type=")) {
					// 複数行で設定されていた場合、先勝ちとする
					if (gougiType == null) {
						gougiType = line.substring("Gougi.Type=".length()).trim();
					}
				}

				// エンジンの設定
				for (int i = 1; i <= Constants.ENGINE_COUNT_MAX; i++) {
					String str = "Engine" + i + ".Path=";

					// （例）「Engine1.Path=」で始まる行の場合
					if (line.startsWith(str)) {
						// 同じエンジン番号が複数行にあった場合、先勝ちとする
						if (ShogiUtils.getEngine(list, i) == null) {
							UsiEngine engine = new UsiEngine();
							engine.setEngineNumber(i);
							engine.setExeFile(new File(line.substring(str.length()).trim()));
							list.add(engine);
						}
					}
				}

				// 詰探索エンジンの設定
				String str = "MateEngine.Path=";
				if (line.startsWith(str)) {
					// 詰探索エンジンの設定が複数行存在する場合、先勝ちとする
					if (mateEngine == null) {
						mateEngine = new MateEngine();
						mateEngine.setEngineNumber(0);
						mateEngine.setExeFile(new File(line.substring(str.length()).trim()));
					}
				}

				// 合議タイプ「詰探索エンジンとの合議（読み筋の局面も詰探索）」の場合に、読み筋局面を探索する詰探索エンジンの数
				str = "MateEngine.forPV.Count=";
				if (line.startsWith(str)) {
					pvMateEngineCount = Utils.getIntValue(line.substring(str.length()).trim(), Constants.PV_MATE_ENGINE_CNT_DEFAULT);
				}
			}

			// ソート結果をUSIエンジンリストにセット
			usiEngineList = sort(list);
		} finally {
			Utils.close(br);
		}
	}

	/**
	 * エラーメッセージ取得（設定ファイルの内容のチェック）
	 * 
	 * @return
	 */
	public String getErrorMessage() {
		// デバッグログ
		logger.info("gougiType=" + gougiType);

		if (usiEngineList == null) {
			logger.info("usiEngineList == null");
		} else {
			logger.info("usiEngineList.size()=" + usiEngineList.size());
		}

		// エンジンが存在しない場合
		if (usiEngineList == null || usiEngineList.isEmpty()) {
			// チェックNG
			return "エンジンの設定が見つかりません。";
		}

		// 合議タイプが正しくない場合
		if (!Constants.GOUGI_TYPE_LIST.contains(gougiType)) {
			// チェックNG
			return "合議タイプが正しくありません。";
		}

		// 合議タイプが「多数決合議（3者）」の場合
		if (Constants.GOUGI_TYPE_TASUUKETSU_3.equals(gougiType)) {
			// エンジンが3個ではない場合
			if (usiEngineList.size() != Constants.ENGINE_COUNT_TASUUKETSU_3) {
				// チェックNG
				return "合議タイプが「多数決合議（3者）」の場合、エンジンは3種類設定してください。";
			}
		}

		// 合議タイプが「各々の最善手を交換して評価値の合計で判定（2者）」の場合
		if (Constants.GOUGI_TYPE_BESTMOVE_EXCHANGE_2.equals(gougiType)) {
			// エンジンが2個ではない場合
			if (usiEngineList.size() != Constants.ENGINE_COUNT_BESTMOVE_EXCHANGE_2) {
				// チェックNG
				return "合議タイプが「各々の最善手を交換して評価値の合計で判定（2者）」の場合、エンジンは2種類設定してください。";
			}
		}

		// 合議タイプが「序盤・中盤・終盤で対局者交代」の場合
		if (Constants.GOUGI_TYPE_CHANGE_PLAYER_JOBAN_CHUUBAN_SHUUBAN.equals(gougiType)) {
			// エンジンが3個ではない場合
			if (usiEngineList.size() != Constants.ENGINE_COUNT_CHANGE_PLAYER_JOBAN_CHUUBAN_SHUUBAN) {
				// チェックNG
				return "合議タイプが「序盤・中盤・終盤で対局者交代」の場合、エンジンは3種類設定してください。";
			}
		}

		// 合議タイプが「詰探索エンジンとの合議」の場合
		if (Constants.GOUGI_TYPE_MATE.equals(gougiType)) {
			// エンジンが1個ではない場合
			if (usiEngineList.size() != Constants.ENGINE_COUNT_MATE) {
				// チェックNG
				return "合議タイプが「詰探索エンジンとの合議」の場合、エンジン（通常）は1種類設定してください。";
			}
			// 詰探索エンジンが未設定の場合
			if (mateEngine == null) {
				// チェックNG
				return "合議タイプが「詰探索エンジンとの合議」の場合、詰探索エンジンを設定してください。";
			}
		}

		// チェックOK
		return null;
	}

	/**
	 * 設定ファイルのパスを取得する
	 * 自分（jarファイル）と同じフォルダ内にある「SimpleGougiShogi.config」の絶対パスを返す
	 * 
	 * @return
	 */
	public String getConfigFilePath() {
		String classPath = System.getProperty("java.class.path");
		if (classPath.endsWith(".jar")) {
			return Utils.getMyDir().getAbsolutePath() + File.separator + Constants.CONFIG_FILENAME;
		} else {
			return Constants.CONFIG_FILENAME;
		}
	}

	/**
	 * USIエンジンリストをエンジン番号でソートする
	 * 
	 * @param usiEngineList
	 * @return
	 */
	private List<UsiEngine> sort(List<UsiEngine> usiEngineList) {
		List<UsiEngine> list = new ArrayList<UsiEngine>();

		for (int i = 1; i <= Constants.ENGINE_COUNT_MAX; i++) {
			UsiEngine engine = ShogiUtils.getEngine(usiEngineList, i);
			if (engine != null) {
				list.add(engine);
			}
		}

		return list;
	}

	// ------------------------------ 単純なGetter&Setter START ------------------------------

	public List<UsiEngine> getUsiEngineList() {
		return usiEngineList;
	}

	public String getGougiType() {
		return gougiType;
	}

	public void setGougiType(String gougiType) {
		this.gougiType = gougiType;
	}

	public MateEngine getMateEngine() {
		return mateEngine;
	}

	public int getPvMateEngineCount() {
		return pvMateEngineCount;
	}

	public void setPvMateEngineCount(int pvMateEngineCount) {
		this.pvMateEngineCount = pvMateEngineCount;
	}

	// ------------------------------ 単純なGetter&Setter END ------------------------------

}
