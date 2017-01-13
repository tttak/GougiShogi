package co.jp.shogi.gougi;

import java.io.IOException;
import java.util.ArrayList;
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

			// quitフラグ、goフラグ
			boolean quitFlg = false;
			boolean goFlg = false;

			while (!quitFlg) {
				// usiフラグ、isreadyフラグ
				boolean usiFlg = false;
				boolean isreadyFlg = false;

				// 標準入力（GUI側）からのコマンドを各エンジンへのコマンドリストに追加する
				List<String> systemInputCommandList = sysinToEngines(systemInputThread, usiEngineList);

				if (systemInputCommandList != null) {
					// 標準入力（GUI側）からのコマンドに「quit」が含まれる場合
					if (systemInputCommandList.contains("quit")) {
						quitFlg = true;
					}

					// 標準入力（GUI側）からのコマンドに「usi」が含まれる場合
					if (systemInputCommandList.contains("usi")) {
						usiFlg = true;
					}

					// 標準入力（GUI側）からのコマンドに「isready」が含まれる場合
					if (systemInputCommandList.contains("isready")) {
						isreadyFlg = true;
					}

					// 標準入力（GUI側）からのコマンドに「go」で始まるコマンドが含まれる場合
					if (Utils.containsStartsWith(systemInputCommandList, "go")) {
						goFlg = true;
						// 全エンジンのbestmove、直近の読み筋をクリア
						ShogiUtils.clearBestmoveLastPv(usiEngineList);
					}
				}

				// 「usi」の場合
				if (usiFlg) {
					logger.info("「usi」の場合 START");
					// 各エンジンからのコマンドを標準出力（GUI側）へのコマンドリストに追加する（「usi」の場合）
					enginesToSysoutAtUsi(systemOutputThread, usiEngineList);
					logger.info("「usi」の場合 END");
				}

				// 「isready」の場合
				if (isreadyFlg) {
					logger.info("「isready」の場合 START");
					// 各エンジンからのコマンドを標準出力（GUI側）へのコマンドリストに追加する（「isready」の場合）
					enginesToSysoutAtIsReady(systemOutputThread, usiEngineList);
					logger.info("「isready」の場合 END");
				}

				// 「go」の場合
				if (goFlg) {
					// 合議を実行
					if (executeGougi(systemOutputThread, usiEngineList, GougiConfig.getInstance().getGougiType())) {
						// 合議完了の場合、「go」フラグを落とす
						goFlg = false;
					}
				}

				// 各エンジンからのコマンドを標準出力（GUI側）へのコマンドリストに追加する（通常の場合）
				enginesToSysoutAtNormal(systemOutputThread, usiEngineList, goFlg);

				// sleep
				Thread.sleep(10);
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

	/**
	 * 標準入力（GUI側）からのコマンドを各エンジンへのコマンドリストに追加する
	 * 
	 * @param systemInputThread
	 * @param usiEngineList
	 * @return
	 */
	private List<String> sysinToEngines(InputStreamThread systemInputThread, List<UsiEngine> usiEngineList) {
		List<String> systemInputCommandList = null;

		// 標準入力（GUI側）からの入力用スレッドのコマンドリストが空ではない場合、ローカル変数にコピーしてからクリア
		// ・スレッドの排他制御
		synchronized (systemInputThread) {
			if (!systemInputThread.getCommandList().isEmpty()) {
				systemInputCommandList = new ArrayList<String>(systemInputThread.getCommandList());
				systemInputThread.getCommandList().clear();
			}
		}

		// コマンドが存在する場合
		if (systemInputCommandList != null) {
			for (UsiEngine engine : usiEngineList) {
				// 各エンジンへのコマンドリストに追加
				// ・スレッドの排他制御
				synchronized (engine.getOutputThread()) {
					for (String command : systemInputCommandList) {
						// コマンドを編集（「setoption」対応）
						String cmd = editSysinCommand(command, engine);
						if (!Utils.isEmpty(cmd)) {
							engine.getOutputThread().getCommandList().add(cmd);
						}
					}
				}
			}
		}

		return systemInputCommandList;
	}

	/**
	 * コマンドを編集
	 * ・通常はcommandをそのまま返す
	 * ・「setoption」の場合、例えば「E2_OwnBook」を「OwnBook」へ戻す（「option name」のときに「OwnBook」を「E2_OwnBook」に変換したので）
	 * （例）「option name E2_OwnBook type check default true」
	 * →「option name OwnBook type check default true」（エンジン2の場合のみ）
	 * 
	 * @param command
	 * @param engine
	 * @return
	 */
	private String editSysinCommand(String command, UsiEngine engine) {
		// 「setoption」以外の場合
		if (!command.startsWith("setoption ")) {
			// commandをそのまま返す
			return command;
		}

		// ----- 以下、「setoption」の場合
		// （例）「setoption name E2_OwnBook value true」の場合
		// ・エンジン1の場合、nullを返す
		// ・エンジン2の場合、「setoption name OwnBook value true」を返す
		// ・エンジン3の場合、nullを返す
		//
		// （例）「setoption name USI_Hash value 128」の場合
		// ・エンジン1の場合、「setoption name USI_Hash value 128」を返す
		// ・エンジン2の場合、「setoption name USI_Hash value 128」を返す
		// ・エンジン3の場合、「setoption name USI_Hash value 128」を返す

		// コマンドからオプション名を取得
		// （例）「E2_OwnBook」「USI_Hash」
		String option = ShogiUtils.getOptionName(command);

		if (Utils.isEmpty(option)) {
			return null;
		}

		// （例）「USI_Hash」
		if (option.startsWith("USI")) {
			// commandをそのまま返す
			return command;
		}

		// （例）「E2_」で始まる場合
		if (option.startsWith(engine.getOptionNamePrefix())) {
			// （例）「E2_OwnBook」→「OwnBook」
			String str = option.substring(engine.getOptionNamePrefix().length());

			// 当該エンジンのUSIオプションに含まれる場合（念のためチェック）
			if (engine.getOptionSet().contains(str)) {
				String prefix = "setoption name " + engine.getOptionNamePrefix();
				// （例）「setoption name OwnBook value true」
				return "setoption name " + command.substring(prefix.length()).trim();
			}
		}

		// （例）「E2_」で始まるオプションは、エンジン1とエンジン3の場合はnullを返す（エンジン2用のオプションのはずなので）
		return null;
	}

	/**
	 * 各エンジンからのコマンドを標準出力（GUI側）へのコマンドリストに追加する（「usi」の場合）
	 * 
	 * @param systemOutputThread
	 * @param usiEngineList
	 */
	private void enginesToSysoutAtUsi(OutputStreamThread systemOutputThread, List<UsiEngine> usiEngineList) {
		// 「id name～」と「id author～」は各エンジンからではなく、このプログラム独自で返す
		// ・スレッドの排他制御
		synchronized (systemOutputThread) {
			systemOutputThread.getCommandList().add("id name " + Constants.USI_ID_NAME);
			systemOutputThread.getCommandList().add("id author " + Constants.USI_ID_AUTHOR);
		}

		// GUIに返し終わるまで待つ
		systemOutputThread.waitUntilEmpty();

		// 各エンジンをループ
		for (UsiEngine engine : usiEngineList) {
			InputStreamThread processInputThread = engine.getInputThread();

			// エンジンから「usiok」が返ってくるまで待つ
			processInputThread.waitUntilCommand("usiok");

			List<String> processInputCommandList = null;

			// エンジンからのコマンドリストをローカル変数にコピーしてからクリア
			// ・スレッドの排他制御
			synchronized (processInputThread) {
				processInputCommandList = new ArrayList<String>(processInputThread.getCommandList());
				processInputThread.getCommandList().clear();
			}

			List<String> systemOutputCommandList = new ArrayList<String>();

			// エンジンからのコマンドリストをループ
			// ・コマンドリストには「usi」への返信が入っているはず。
			// ・「id name～」「id author～」「option name～」「usiok」
			for (String command : processInputCommandList) {
				// エンジンからの「id name～」はGUIへ返さない
				if (command.startsWith("id name ")) {
					String usiName = command.substring("id name ".length());
					// エンジンのUSI名にセット
					engine.setUsiName(usiName);
					logger.info(engine.getUsiName());
					continue;
				}

				// 「id name～」以外の「id～」は読み飛ばす
				// 「usiok」はこの段階ではGUIへ返さない（全エンジンの「usiok」が揃ってから返す）
				if (command.startsWith("id ") || "usiok".equals(command)) {
					continue;
				}

				// 「option name～」の場合、オプション名に「E1_」などを付加してからGUIへ返す
				if (command.startsWith("option name ")) {
					// 「option name OwnBook type check default true」→「option name E1_OwnBook type check default true」
					String cmd = "option name " + engine.getOptionNamePrefix() + command.substring("option name ".length()).trim();
					systemOutputCommandList.add(cmd);

					// エンジンのオプションセットに追加
					engine.getOptionSet().add(ShogiUtils.getOptionName(command));
				}
			}

			// 標準出力（GUI側）へのコマンドリストに追加
			// ・スレッドの排他制御
			synchronized (systemOutputThread) {
				systemOutputThread.getCommandList().addAll(new ArrayList<String>(systemOutputCommandList));
			}

			// GUIに返し終わるまで待つ
			systemOutputThread.waitUntilEmpty();
		}

		// GUIに「usiok」を1回だけ返す
		// ・スレッドの排他制御
		synchronized (systemOutputThread) {
			systemOutputThread.getCommandList().add("usiok");
		}

		// GUIに返し終わるまで待つ
		systemOutputThread.waitUntilEmpty();
	}

	/**
	 * 各エンジンからのコマンドを標準出力（GUI側）へのコマンドリストに追加する（「isready」の場合）
	 * 
	 * @param systemOutputThread
	 * @param usiEngineList
	 */
	private void enginesToSysoutAtIsReady(OutputStreamThread systemOutputThread, List<UsiEngine> usiEngineList) {
		// 各エンジンをループ
		for (UsiEngine engine : usiEngineList) {
			InputStreamThread processInputThread = engine.getInputThread();

			// 「readyok」が返ってくるまで待つ
			processInputThread.waitUntilCommand("readyok");
			// 「readyok」を削除
			processInputThread.getCommandList().remove("readyok");
		}

		// すべてのエンジンから「readyok」が返ってきたら、GUIに「readyok」を1回だけ返す
		// ・スレッドの排他制御
		synchronized (systemOutputThread) {
			systemOutputThread.getCommandList().add("readyok");
		}

		// GUIに返し終わるまで待つ
		systemOutputThread.waitUntilEmpty();
	}

	/**
	 * 各エンジンからのコマンドを標準出力（GUI側）へのコマンドリストに追加する（通常の場合）
	 * 
	 * @param systemOutputThread
	 * @param usiEngineList
	 * @param goFlg
	 */
	private void enginesToSysoutAtNormal(OutputStreamThread systemOutputThread, List<UsiEngine> usiEngineList, boolean goFlg) {
		// 各エンジンをループ
		for (UsiEngine engine : usiEngineList) {
			InputStreamThread processInputThread = engine.getInputThread();
			List<String> processInputCommandList = null;

			// エンジンからのコマンドリストが空ではない場合、ローカル変数にコピーしてからクリア
			// ・スレッドの排他制御
			synchronized (processInputThread) {
				if (!processInputThread.getCommandList().isEmpty()) {
					processInputCommandList = new ArrayList<String>(processInputThread.getCommandList());
					processInputThread.getCommandList().clear();
				}
			}

			// コマンドが存在する場合
			if (processInputCommandList != null) {
				// 標準出力（GUI側）へのコマンドリストに追加
				// ・スレッドの排他制御
				synchronized (systemOutputThread) {
					for (String command : processInputCommandList) {
						// 「bestmove」の場合、この段階ではGUIへ返さず、エンジンに保存しておく。（3つ揃ってから合議結果をGUIへ返すので）
						// ・ただし、思考が終わったことがGUIから見えた方がよさそうなので、「info string」で返しておく。
						if (goFlg && command.startsWith("bestmove")) {
							// bestmoveをエンジンに保存
							engine.setBestmoveCommand(command);
							// （例）「info string bestmove 7g7f [７六(77)] [評価値 123] [Gikou 20160606]」
							systemOutputThread.getCommandList().add("info string " + engine.getBestmoveScoreDisp());
						}

						// その他の場合、標準出力（GUI側）へのコマンドリストに追加
						else {
							systemOutputThread.getCommandList().add(command);

							// 「info～score～」の場合、直近の読み筋をエンジンに保存
							if (command.startsWith("info ") && command.indexOf("score ") >= 0) {
								engine.setLastPv(command);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * 合議を実行
	 * 
	 * @param systemOutputThread
	 * @param usiEngineList
	 * @param gougiType
	 * @return
	 */
	private boolean executeGougi(OutputStreamThread systemOutputThread, List<UsiEngine> usiEngineList, String gougiType) {
		// 合議実行
		GougiLogic gougiLogic = new GougiLogic(gougiType, usiEngineList);
		UsiEngine resultEngine = gougiLogic.execute();

		// 合議が不成立の場合
		// ・bestmoveが返ってきていないエンジンが存在する場合など
		if (resultEngine == null) {
			return false;
		}

		// 合議結果のbestmoveコマンドを取得（ponder部分を除く）
		// ・合議自体はこれで終了
		String bestmoveCommand = resultEngine.getBestmoveCommandExceptPonder();

		if (!Utils.isEmpty(bestmoveCommand)) {
			// 合議結果を標準出力（GUI側）へのコマンドリストに追加
			// ・スレッドの排他制御
			synchronized (systemOutputThread) {
				// 合議で指し手が採用されたエンジンのうち、エンジン番号が最小のエンジンの読み筋（評価値を含む）を再度出力しておく
				// ・将棋所等では、bestmove直前の読み筋（「info～score～pv～」）が評価値・読み筋として表示されるため、指し手が採用されなかったエンジンが直近だと指し手と読み筋が矛盾するので。
				if (!Utils.isEmpty(resultEngine.getLastPv())) {
					systemOutputThread.getCommandList().add(resultEngine.getLastPv());
				}

				// 各エンジンのbestmoveを参考情報としてGUIへ出力する
				// ・GUIで見やすいようにエンジンは逆順にしておく（エンジン1が上に表示されるように）
				for (int i = usiEngineList.size() - 1; i >= 0; i--) {
					UsiEngine engine = usiEngineList.get(i);

					// （例）「bestmove 7g7f」
					String engineBest = engine.getBestmoveCommandExceptPonder();
					// このエンジンの指し手が採用されたか否か。丸とバツだが、「O」と「X」で代替する。
					String hantei = engineBest.equals(bestmoveCommand) ? "O" : "X";
					// （例）「info string [O] bestmove 7g7f [７六(77)] [評価値 123] [Gikou 20160606]」
					systemOutputThread.getCommandList().add("info string [" + hantei + "] " + engine.getBestmoveScoreDisp());
				}

				// bestmoveをGUIへ返す
				systemOutputThread.getCommandList().add(bestmoveCommand);
			}

			// 合議完了
			return true;
		}

		return false;
	}

}
