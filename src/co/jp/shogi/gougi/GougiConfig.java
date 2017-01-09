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

	/** 設定ファイルのファイル名 */
	public final String FILENAME = "SimpleGougiShogi.config";

	/** USIエンジンリスト */
	private List<UsiEngine> usiEngineList = new ArrayList<UsiEngine>();

	/**
	 * 設定ファイル読込み
	 * 
	 * @throws IOException
	 */
	public void readConfigFile() throws IOException {
		List<UsiEngine> list = new ArrayList<UsiEngine>();
		BufferedReader br = null;

		try {
			br = new BufferedReader(new FileReader(new File(getConfigFilePath())));

			String line;

			// 1行ずつ読み込む
			while ((line = br.readLine()) != null) {
				logger.info(line);

				for (int i = 1; i <= 3; i++) {
					String str = "Engine" + i + ".Path=";

					// （例）「Engine1.Path=」で始まる行の場合
					if (line.startsWith(str)) {
						UsiEngine engine = new UsiEngine();
						engine.setEngineNumber(i);
						engine.setExeFile(new File(line.substring(str.length()).trim()));
						list.add(engine);
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
	 * 設定ファイルのパスを取得する
	 * 自分（jarファイル）と同じフォルダ内にある「SimpleGougiShogi.config」の絶対パスを返す
	 * 
	 * @return
	 */
	public String getConfigFilePath() {
		String classPath = System.getProperty("java.class.path");
		if (classPath.endsWith(".jar")) {
			return Utils.getMyDir().getAbsolutePath() + File.separator + FILENAME;
		} else {
			return FILENAME;
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

		for (int i = 1; i <= 3; i++) {
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

	// ------------------------------ 単純なGetter&Setter END ------------------------------

}
