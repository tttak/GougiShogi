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
				UsiEngine nextEngine = selectNextEngine(GougiConfig.getInstance().getGougiType(), usiEngineList, StateInfo.getInstance().getChangePlayerPlys(), currentEngine, currentEnginePlys);

				// 対局者が交代した場合
				if (nextEngine != currentEngine) {
					logger.info("対局者交代：[" + currentEngine.getUsiName() + "]→[" + nextEngine.getUsiName() + "]");
					currentEnginePlys = 0;
					currentEngine = nextEngine;

					// bestmove、直近の読み筋などをクリア
					// ・特に【前回の値】bestmove、直近の読み筋がクリアされる
					currentEngine.clear();
				}
				// 対局者が交代しなかった場合
				else {
					// 【前回の値】を保存
					// ・selectNextEngine()内で【前回の値】を使用するので、このタイミングで呼ぶ
					currentEngine.savePrevValue();
				}

				// 相手番へ更新
				StateInfo.getInstance().setMyTurn(false);
				// 全エンジンのbestmove、直近の読み筋をクリア
				// ・【前回の値】bestmove、直近の読み筋はクリアしない
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
			// 合議タイプ
			String gougiType = GougiConfig.getInstance().getGougiType();

			// 合議タイプが「数手ごとに対局者交代」の場合
			if (Constants.GOUGI_TYPE_CHANGE_PLAYER_PLYS.equals(gougiType)) {
				// 対局者を交代する手数
				systemOutputThread.getCommandList().add("option name G_ChangePlayerPlys type spin default 1 min 1 max 1000");
			}

			// 合議タイプが以下のいずれかの場合
			// ・「2手前の評価値から一定値以上下降したら対局者交代」
			// ・「2手前の評価値から一定値以上上昇したら対局者交代」
			else if (Constants.GOUGI_TYPE_CHANGE_PLAYER_SCORE_DOWN.equals(gougiType) || Constants.GOUGI_TYPE_CHANGE_PLAYER_SCORE_UP.equals(gougiType)) {
				// 対局者を交代する評価値の差分
				systemOutputThread.getCommandList().add("option name G_ChangePlayerScoreDiff type spin default 100 min 1 max 1000000");
			}
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
	 * @param gougiType
	 * @param usiEngineList
	 * @param changePlayerPlys
	 * @param currentEngine
	 * @param currentEnginePlys
	 * @return
	 */
	private UsiEngine selectNextEngine(String gougiType, List<UsiEngine> usiEngineList, int changePlayerPlys, UsiEngine currentEngine, int currentEnginePlys) {
		// 合議タイプ「数手ごとに対局者交代」の場合
		if (Constants.GOUGI_TYPE_CHANGE_PLAYER_PLYS.equals(gougiType)) {
			// 現在のエンジンの指し手数が「対局者を交代する手数」に達しない場合
			if (currentEnginePlys < changePlayerPlys) {
				// 対局者を交代しない
				return currentEngine;
			}

			// 現在のエンジンの指し手数が「対局者を交代する手数」に達した場合
			else {
				// 対局者交代
				int currentIndex = ShogiUtils.getEngineListIndex(usiEngineList, currentEngine);
				return selectNextIndexEngine(usiEngineList, currentIndex);
			}
		}

		// その他の場合
		// ・合議タイプ「2手前の評価値から一定値以上下降したら対局者交代」「2手前の評価値から一定値以上上昇したら対局者交代」のいずれかの場合
		else {
			logger.info("gougiType=" + gougiType);
			logger.info("currentEnginePlys=" + currentEnginePlys);

			// 現在のエンジンの指し手数が1手の場合
			if (currentEnginePlys <= 1) {
				// 対局者を交代しない
				return currentEngine;
			}

			// 当該エンジンの2手前の評価値からの上昇分
			int score2TemaeJoushou = currentEngine.getScore2TemaeJoushou();

			logger.info("score2TemaeJoushou=" + score2TemaeJoushou);
			logger.info("StateInfo.getInstance().getChangePlayerScoreDiff()=" + StateInfo.getInstance().getChangePlayerScoreDiff());

			// 評価値未設定の場合
			if (score2TemaeJoushou == Constants.SCORE_NONE) {
				// 対局者を交代しない
				return currentEngine;
			}

			// 合議タイプ「2手前の評価値から一定値以上下降したら対局者交代」の場合
			if (Constants.GOUGI_TYPE_CHANGE_PLAYER_SCORE_DOWN.equals(gougiType)) {
				if (score2TemaeJoushou <= (-1) * StateInfo.getInstance().getChangePlayerScoreDiff()) {
					// 対局者交代
					int currentIndex = ShogiUtils.getEngineListIndex(usiEngineList, currentEngine);
					return selectNextIndexEngine(usiEngineList, currentIndex);
				}
			}

			// 合議タイプ「2手前の評価値から一定値以上上昇したら対局者交代」の場合
			else if (Constants.GOUGI_TYPE_CHANGE_PLAYER_SCORE_UP.equals(gougiType)) {
				if (score2TemaeJoushou >= StateInfo.getInstance().getChangePlayerScoreDiff()) {
					// 対局者交代
					int currentIndex = ShogiUtils.getEngineListIndex(usiEngineList, currentEngine);
					return selectNextIndexEngine(usiEngineList, currentIndex);
				}
			}

			// 対局者を交代しない
			return currentEngine;
		}
	}

	/**
	 * 次のインデックスのエンジンを選択する
	 * 
	 * @param usiEngineList
	 * @param currentIndex
	 * @return
	 */
	private UsiEngine selectNextIndexEngine(List<UsiEngine> usiEngineList, int currentIndex) {
		int nextIndex = currentIndex + 1;
		if (nextIndex >= usiEngineList.size()) {
			nextIndex = 0;
		}
		return usiEngineList.get(nextIndex);
	}

	/**
	 * 標準入力（GUI側）からのコマンドを当該エンジンへのコマンドリストに追加する（「go」または「go ponder」の場合）
	 * 
	 * @param goCommand
	 * @param engine
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

		// 「go ponder」から「ponderhit」までの時間が短すぎるとエンジン側が認識できずにtimeupする場合があるようなので、少し待つ
		try {
			Thread.sleep(20);
		} catch (InterruptedException e) {
			// ignore
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
						// （例）「info string [O] bestmove 7g7f ponder 3c3d [７六(77) ３四(33)] [評価値 123 （前回100 差分23）] [Gikou 20160606]」
						systemOutputThread.getCommandList().add("info string " + engine.getBestmoveScoreDisp(true, true));
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
