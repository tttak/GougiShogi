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
			logger.warning(ex.getMessage());
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
			// 合議タイプが以下のいずれかの場合
			// ・「数手ごとに対局者交代」
			// ・「序盤・中盤・終盤で対局者交代」
			// ・「2手前の評価値から一定値以上下降したら対局者交代」
			// ・「2手前の評価値から一定値以上上昇したら対局者交代」
			else if (Constants.GOUGI_TYPE_CHANGE_PLAYER_PLYS.equals(gougiConfig.getGougiType()) || Constants.GOUGI_TYPE_CHANGE_PLAYER_JOBAN_CHUUBAN_SHUUBAN.equals(gougiConfig.getGougiType()) || Constants.GOUGI_TYPE_CHANGE_PLAYER_SCORE_DOWN.equals(gougiConfig.getGougiType()) || Constants.GOUGI_TYPE_CHANGE_PLAYER_SCORE_UP.equals(gougiConfig.getGougiType())) {
				// USIロジック3を実行
				UsiLogic3 usiLogic3 = new UsiLogic3();
				usiLogic3.execute(usiEngineList, systemInputThread, systemOutputThread);
			}
			// 合議タイプが「詰探索エンジンとの合議」の場合
			else if (Constants.GOUGI_TYPE_MATE.equals(gougiConfig.getGougiType())) {
				// USIロジック4を実行
				UsiLogic4 usiLogic4 = new UsiLogic4();
				usiLogic4.execute(usiEngineList, systemInputThread, systemOutputThread);
			}
			// 合議タイプが「詰探索エンジンとの合議（「脊尾詰」対応版）」の場合
			else if (Constants.GOUGI_TYPE_MATE_SEOTSUME.equals(gougiConfig.getGougiType())) {
				// USIロジック5を実行
				UsiLogic5 usiLogic5 = new UsiLogic5();
				usiLogic5.execute(usiEngineList, systemInputThread, systemOutputThread);
			}
			// 合議タイプが「詰探索エンジンとの合議（読み筋の局面も詰探索）」の場合
			else if (Constants.GOUGI_TYPE_MATE_PV.equals(gougiConfig.getGougiType())) {
				// USIロジック6を実行
				UsiLogic6 usiLogic6 = new UsiLogic6();
				usiLogic6.execute(usiEngineList, systemInputThread, systemOutputThread);
			}
			// その他の場合
			// すなわち、合議タイプが以下のいずれかの場合
			// ・「多数決合議（3者）」
			// ・「楽観合議」
			// ・「悲観合議」
			// ・「楽観合議と悲観合議を交互」
			// ・「2手前の評価値からの上昇分の楽観合議」
			// ・「2手前の評価値からの上昇分の悲観合議」
			else {
				// USIロジック1を実行
				UsiLogic1 usiLogic1 = new UsiLogic1();
				usiLogic1.execute(usiEngineList, systemInputThread, systemOutputThread);
			}

		} finally {
			Thread.sleep(500);

			// USIエンジンリスト
			if (usiEngineList != null) {
				for (UsiEngine engine : usiEngineList) {
					engine.destroy();
				}
			}

			// 詰探索エンジン
			MateEngine mateEngine = GougiConfig.getInstance().getMateEngine();
			if (mateEngine != null) {
				mateEngine.destroy();
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
			// プロセスの作成（起動）
			engine.createProcess();
		}
	}

}
