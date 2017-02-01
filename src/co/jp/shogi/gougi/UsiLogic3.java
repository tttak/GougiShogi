package co.jp.shogi.gougi;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * USIロジック3
 */
public class UsiLogic3 extends UsiLogicCommon {

	/** Logger */
	protected static Logger logger = Logger.getLogger(UsiLogic3.class.getName());

	/**
	 * executeメソッド
	 * 
	 * @param usiEngineList
	 * @param systemInputThread
	 * @param systemOutputThread
	 * @throws Exception
	 */
	public void execute(List<UsiEngine> usiEngineList, InputStreamThread systemInputThread, OutputStreamThread systemOutputThread) throws Exception {
		// 現在のエンジン
		UsiEngine currentEngine = usiEngineList.get(0);
		// 現在のエンジンの指し手数
		int currentEnginePlys = 0;

		while (true) {
			// 保留中の標準入力（GUI側）からのコマンドリスト
			// ・goコマンド、go ponderコマンド、ponder時のstopコマンド、ponderhitコマンドは一旦このリストにためる
			List<String> pendingSysinCommandList = new ArrayList<String>();
			// 標準入力（GUI側）からのコマンドを各エンジンへのコマンドリストに追加する
			List<String> systemInputCommandList = sysinToEngines(systemInputThread, usiEngineList, pendingSysinCommandList);

			// 保留中の標準入力（GUI側）からのコマンドリストをループ
			for (String command : pendingSysinCommandList) {
				logger.info("保留中の標準入力（GUI側）からのコマンドリストのコマンド=" + command);

				// 「go」コマンドまたは「go ponder」コマンドの場合
				if (command.startsWith("go")) {
					logger.info("「go」または「go ponder」の場合 START");
					// 自分の手番
					StateInfo.getInstance().setMyTurn(true);
					// 全エンジンのbestmove、直近の読み筋をクリア
					ShogiUtils.clearBestmoveLatestPv(usiEngineList);
					// 標準入力（GUI側）からのコマンドを現在のエンジンへのコマンドリストに追加する（「go」または「go ponder」の場合）
					sysinToEnginesAtGoOrGoPonder(command, currentEngine);
					logger.info("「go」または「go ponder」の場合 END");
				}

				// ponder時のstopの場合
				else if (StateInfo.getInstance().isPondering() && "stop".equals(command)) {
					logger.info("ponder時のstopの場合 START");
					StateInfo.getInstance().setPondering(false);
					// 自分の手番
					StateInfo.getInstance().setMyTurn(true);
					// stop時のponder終了処理
					endPonderAtStop(systemInputThread, systemOutputThread, currentEngine);
					logger.info("ponder時のstopの場合 END");
				}

				// ponder時のponderhitの場合
				else if (StateInfo.getInstance().isPondering() && "ponderhit".equals(command)) {
					logger.info("ponder時のponderhitの場合 START");
					StateInfo.getInstance().setPondering(false);
					// 自分の手番
					StateInfo.getInstance().setMyTurn(true);
					// 現在のエンジンにコマンドをそのまま送信
					// ・スレッドの排他制御
					synchronized (currentEngine.getOutputThread()) {
						currentEngine.getOutputThread().getCommandList().add(command);
					}
					logger.info("ponder時のponderhitの場合 END");
				}
			}

			// 標準入力（GUI側）からのコマンドに「quit」が含まれる場合
			if (Utils.contains(systemInputCommandList, "quit")) {
				// ループを抜ける
				break;
			}

			// 標準入力（GUI側）からのコマンドに「usi」が含まれる場合
			if (Utils.contains(systemInputCommandList, "usi")) {
				logger.info("「usi」の場合 START");
				// 各エンジンからのコマンドを標準出力（GUI側）へのコマンドリストに追加する（「usi」の場合）
				enginesToSysoutAtUsi(systemOutputThread, usiEngineList);
				logger.info("「usi」の場合 END");
			}

			// 標準入力（GUI側）からのコマンドに「isready」が含まれる場合
			if (Utils.contains(systemInputCommandList, "isready")) {
				logger.info("「isready」の場合 START");
				// クリア処理
				StateInfo.getInstance().clear();
				// 各エンジンからのコマンドを標準出力（GUI側）へのコマンドリストに追加する（「isready」の場合）
				enginesToSysoutAtIsReady(systemOutputThread, usiEngineList);
				logger.info("「isready」の場合 END");
			}

			// 標準入力（GUI側）からのコマンドに「usinewgame」が含まれる場合
			if (Utils.contains(systemInputCommandList, "usinewgame")) {
				logger.info("「usinewgame」の場合 START");
				StateInfo.getInstance().setDuringGame(true);
				logger.info("「usinewgame」の場合 END");
			}

			// 標準入力（GUI側）からのコマンドに「gameover」が含まれる場合
			if (Utils.containsStartsWith(systemInputCommandList, "gameover")) {
				logger.info("「gameover」の場合 START");
				StateInfo.getInstance().setDuringGame(false);
				logger.info("「gameover」の場合 END");
			}

			// 合議処理は不要

			// 現在のエンジンからのコマンドを標準出力（GUI側）へのコマンドリストに追加する（通常の場合）
			// ・bestmoveを返した場合、trueが返る
			if (enginesToSysoutAtNormal(systemOutputThread, currentEngine)) {
				// 現在のエンジンの指し手数をインクリメント
				currentEnginePlys++;
				// 次のエンジンを選択
				UsiEngine nextEngine = selectNextEngine(usiEngineList, StateInfo.getInstance().getChangePlayerPlys(), currentEngine, currentEnginePlys);

				// 対局者が交代した場合
				if (nextEngine != currentEngine) {
					logger.info("対局者交代：[" + currentEngine.getUsiName() + "]→[" + nextEngine.getUsiName() + "]");
					currentEnginePlys = 0;
					currentEngine = nextEngine;
				}

				// 相手番へ更新
				StateInfo.getInstance().setMyTurn(false);
				// 全エンジンのbestmove、直近の読み筋をクリア
				ShogiUtils.clearBestmoveLatestPv(usiEngineList);
			}

			// sleep
			Thread.sleep(10);
		}
	}

	/* (non-Javadoc)
	 * 各エンジンからのコマンドを標準出力（GUI側）へのコマンドリストに追加する（「usi」の場合）2
	 * 
	 * @see co.jp.shogi.gougi.UsiLogicCommon#enginesToSysoutAtUsi2(co.jp.shogi.gougi.OutputStreamThread, java.util.List) */
	@Override
	protected void enginesToSysoutAtUsi2(OutputStreamThread systemOutputThread, List<UsiEngine> usiEngineList) {
		// 独自オプションを追加
		// ・スレッドの排他制御
		synchronized (systemOutputThread) {
			// 対局者を交代する手数
			systemOutputThread.getCommandList().add("option name G_ChangePlayerPlys type spin default 1 min 1 max 1000");
		}
	}

	/**
	 * stop時のponder終了処理
	 * 
	 * @param systemInputThread
	 * @param systemOutputThread
	 * @param engine
	 */
	private void endPonderAtStop(InputStreamThread systemInputThread, OutputStreamThread systemOutputThread, UsiEngine engine) {
		logger.info("GUIからstop受信時、当該エンジンに「stop」コマンドを送信[" + engine.getUsiName() + "]");

		// 当該エンジンに「stop」コマンドを送信
		// ・スレッドの排他制御
		synchronized (engine.getOutputThread()) {
			engine.getOutputThread().getCommandList().add("stop");
		}

		// 当該エンジンから「bestmove」が返ってくるまで待つ
		engine.getInputThread().waitUntilCommandStartsWith("bestmove", 10 * 1000);
		logger.info("GUIからstop受信時、当該エンジンに「stop」コマンドを送信した後、「bestmove」受信[" + engine.getUsiName() + "]");

		String bestmoveCommand = "bestmove";

		// 「bestmove」を取得した後、削除する
		// ・スレッドの排他制御
		synchronized (engine.getInputThread()) {
			bestmoveCommand = Utils.getItemStartsWith(engine.getInputThread().getCommandList(), "bestmove");
			Utils.removeFromListWhereStartsWith(engine.getInputThread().getCommandList(), "bestmove");
			logger.info("GUIからstop受信時、当該エンジンに「stop」コマンドを送信した後、「bestmove」受信したが、コマンドリストから削除[" + engine.getUsiName() + "]");
		}

		// GUIに「bestmove」を1回だけ返す
		// ・スレッドの排他制御
		synchronized (systemOutputThread) {
			systemOutputThread.getCommandList().add(bestmoveCommand);
		}
	}

	/**
	 * 次のエンジンを選択する
	 * 
	 * @param usiEngineList
	 * @param changePlayerPlys
	 * @param currentEngine
	 * @param currentEnginePlys
	 * @return
	 */
	private UsiEngine selectNextEngine(List<UsiEngine> usiEngineList, int changePlayerPlys, UsiEngine currentEngine, int currentEnginePlys) {
		// 現在のエンジンの指し手数が「対局者を交代する手数」に達しない場合
		if (currentEnginePlys < changePlayerPlys) {
			return currentEngine;
		}

		// 現在のエンジンの指し手数が「対局者を交代する手数」に達した場合
		else {
			int index = ShogiUtils.getEngineListIndex(usiEngineList, currentEngine);
			index++;
			if (index >= usiEngineList.size()) {
				index = 0;
			}
			return usiEngineList.get(index);
		}
	}

	/**
	 * 標準入力（GUI側）からのコマンドを当該エンジンへのコマンドリストに追加する（「go」または「go ponder」の場合）
	 * 
	 * @param goCommand
	 * @param usiEngineList
	 */
	protected void sysinToEnginesAtGoOrGoPonder(String goCommand, UsiEngine engine) {
		// 直近の「position」コマンドの局面を取得
		String position = StateInfo.getInstance().getLatestPosition();

		// 当該エンジンへのコマンドリストに追加
		// ・スレッドの排他制御
		synchronized (engine.getOutputThread()) {
			engine.getOutputThread().getCommandList().add(position);
			engine.getOutputThread().getCommandList().add(goCommand);
		}
	}

	/**
	 * 当該エンジンからのコマンドを標準出力（GUI側）へのコマンドリストに追加する（通常の場合）
	 * 
	 * @param systemOutputThread
	 * @param engine
	 * @return
	 */
	private boolean enginesToSysoutAtNormal(OutputStreamThread systemOutputThread, UsiEngine engine) {
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

		// bestmoveを返したか否か
		boolean bestmoveFlg = false;

		// コマンドが存在する場合
		if (processInputCommandList != null) {
			// 標準出力（GUI側）へのコマンドリストに追加
			// ・スレッドの排他制御
			synchronized (systemOutputThread) {
				for (String command : processInputCommandList) {
					// 「bestmove」の場合
					if (command.startsWith("bestmove")) {
						// bestmoveをエンジンに保存
						engine.setBestmoveCommand(command);
						// （例）「bestmove 7g7f ponder 3c3d [７六(77) ３四(33)] [評価値 123] [Gikou 20160606]」
						systemOutputThread.getCommandList().add("info string " + engine.getBestmoveScoreDisp(true, false));
						// bestmoveフラグをセット
						bestmoveFlg = true;
					}

					// 「info～score～」の場合、直近の読み筋をエンジンに保存
					else if (command.startsWith("info ") && command.indexOf("score ") >= 0) {
						engine.setLatestPv(command);
					}

					// 標準出力（GUI側）へのコマンドリストに追加
					systemOutputThread.getCommandList().add(command);
				}
			}
		}

		return bestmoveFlg;
	}

}
