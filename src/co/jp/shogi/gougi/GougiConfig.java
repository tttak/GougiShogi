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

		try {
			br = new BufferedReader(new FileReader(new File(getConfigFilePath())));

			String line;

			// 1行ずつ読み込む
			while ((line = br.readLine()) != null) {
				logger.info(line);

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

	// ------------------------------ 単純なGetter&Setter END ------------------------------

}
