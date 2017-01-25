package co.jp.shogi.gougi;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * シンプル合議将棋のメインクラス
 */
public class SimpleGougiShogiMain {

	/** Logger */
	protected static Logger logger = Logger.getLogger(SimpleGougiShogiMain.class.getName());

	/**
	 * mainメソッド
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			// loggerの初期化
			Utils.initLogger("/co/jp/shogi/gougi/logging.properties");

			logger.info("main() START!!");

			// executeメソッド実行
			SimpleGougiShogiMain obj = new SimpleGougiShogiMain();
			obj.execute();

			logger.info("main() END!!");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * executeメソッド
	 * 
	 * @throws Exception
	 */
	private void execute() throws Exception {
		// USIエンジンリスト
		List<UsiEngine> usiEngineList = null;

		try {
			// 標準入力（GUI側）からの入力用スレッド作成
			InputStreamThread systemInputThread = new InputStreamThread(System.in, "systemInputThread");
			systemInputThread.setDaemon(true);
			systemInputThread.start();

			// 標準出力（GUI側）への出力用スレッド作成
			OutputStreamThread systemOutputThread = new OutputStreamThread(System.out, "systemOutputThread");
			systemOutputThread.setDaemon(true);
			systemOutputThread.start();

			// 設定ファイル読込み
			GougiConfig gougiConfig = GougiConfig.getInstance();
			gougiConfig.readConfigFile();

			// エラーメッセージ取得（設定ファイルの内容のチェック）
			// ・ 合議タイプとエンジン件数が不整合の場合、異常終了させる。
			String errorMessage = gougiConfig.getErrorMessage();
			if (errorMessage != null) {
				systemOutputThread.getCommandList().add("info string [Error] " + errorMessage + " [" + GougiConfig.getInstance().getConfigFilePath() + "]");
				logger.severe(errorMessage);
				Thread.sleep(500);
				return;
			}

			// エンジンリスト取得
			usiEngineList = gougiConfig.getUsiEngineList();
			// 各USIエンジンのプロセスを起動する
			startEngineProcess(usiEngineList);

			// 合議タイプが「各々の最善手を交換して評価値の合計で判定（2者）」の場合
			if (Constants.GOUGI_TYPE_BESTMOVE_EXCHANGE_2.equals(gougiConfig.getGougiType())) {
				// USIロジック2を実行
				UsiLogic2 usiLogic2 = new UsiLogic2();
				usiLogic2.execute(usiEngineList, systemInputThread, systemOutputThread);
			}
			// その他の場合
			else {
				// USIロジック1を実行
				UsiLogic1 usiLogic1 = new UsiLogic1();
				usiLogic1.execute(usiEngineList, systemInputThread, systemOutputThread);
			}

		} finally {
			Thread.sleep(500);

			if (usiEngineList != null) {
				for (UsiEngine engine : usiEngineList) {
					engine.destroy();
				}
			}
		}
	}

	/**
	 * 各USIエンジンのプロセスを起動する
	 * 
	 * @param usiEngineList
	 * @throws IOException
	 */
	private void startEngineProcess(List<UsiEngine> usiEngineList) throws IOException {
		// 各エンジンをループ
		for (UsiEngine engine : usiEngineList) {
			// デバッグ用
			logger.info("-----");
			logger.info(String.valueOf(engine.getEngineNumber()));
			logger.info(String.valueOf(engine.getExeFile()));
			logger.info(engine.getUsiName());
			logger.info("-----");

			// ProcessBuilderを作成
			String[] cmds = { engine.getExeFile().getAbsolutePath() };
			ProcessBuilder pb = new ProcessBuilder(cmds);
			// exeファイルのフォルダを作業ディレクトリとして指定
			pb.directory(engine.getExeFile().getParentFile());
			pb.redirectErrorStream(true);

			// エンジンのプロセスを起動
			Process process = pb.start();
			engine.setProcess(process);

			// エンジンからの入力用スレッド作成
			InputStreamThread processInputThread = new InputStreamThread(process.getInputStream(), engine.getEngineDisp());
			processInputThread.setDaemon(true);
			processInputThread.start();
			engine.setInputThread(processInputThread);

			// エンジンへの出力用スレッド作成
			OutputStreamThread processOutputThread = new OutputStreamThread(process.getOutputStream(), engine.getEngineDisp());
			processOutputThread.setDaemon(true);
			processOutputThread.start();
			engine.setOutputThread(processOutputThread);
		}
	}

}
