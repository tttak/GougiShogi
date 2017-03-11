package co.jp.shogi.gougi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * USIロジック4
 * ・合議タイプが「詰探索エンジンとの合議」の場合に使う
 * ・「なのは詰め」は詰探索中にstopコマンドを送っても思考を続けるようなので、タイムアウトを設定することにする
 */
public class UsiLogic4 extends UsiLogicCommon {

	/** Logger */
	protected static Logger logger = Logger.getLogger(UsiLogic4.class.getName());

	/**
	 * executeメソッド
	 * 
	 * @param usiEngineList
	 * @param systemInputThread
	 * @param systemOutputThread
	 * @throws Exception
	 */
	public void execute(List<UsiEngine> usiEngineList, InputStreamThread systemInputThread, OutputStreamThread systemOutputThread) throws Exception {
		// 現在のエンジン（通常）
		// ・合議タイプが「詰探索エンジンとの合議」の場合、エンジンリストは1件のみのはず
		UsiEngine currentEngine = usiEngineList.get(0);

		// 詰探索エンジン
		logger.info("詰探索エンジンの起動");
		MateEngine mateEngine = GougiConfig.getInstance().getMateEngine();
		mateEngine.createProcess();

		// 詰探索エンジンをエンジンリストに追加する
		usiEngineList.add(mateEngine);

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
					// 標準入力（GUI側）からのコマンドを詰探索エンジンへのコマンドリストに追加する（「go」または「go ponder」の場合）
					sysinToMateEngineAtGoOrGoPonder(command, mateEngine);
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

			// 自分の手番の場合
			// ・ただし、対局中に限る
			if (StateInfo.getInstance().isMyTurn() && StateInfo.getInstance().isDuringGame()) {
				// 合議を実行
				if (executeGougi(systemOutputThread, currentEngine, mateEngine)) {
					// 相手番へ更新
					StateInfo.getInstance().setMyTurn(false);
					// 全エンジンのbestmove、直近の読み筋をクリア
					// ・【前回の値】bestmove、直近の読み筋はクリアしない
					ShogiUtils.clearBestmoveLatestPv(usiEngineList);
				}
			}

			// 詰探索エンジンからのコマンドを標準出力（GUI側）へのコマンドリストに追加する
			mateEnginesToSysout(systemOutputThread, mateEngine);
			// 現在のエンジンからのコマンドを標準出力（GUI側）へのコマンドリストに追加する（通常の場合）
			enginesToSysoutAtNormal(systemOutputThread, currentEngine);

			// sleep
			Thread.sleep(10);
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
	 * 標準入力（GUI側）からのコマンドを詰探索エンジンへのコマンドリストに追加する（「go」または「go ponder」の場合）
	 * 
	 * @param goCommand
	 * @param mateEngine
	 * @throws IOException
	 */
	protected void sysinToMateEngineAtGoOrGoPonder(String goCommand, MateEngine mateEngine) throws IOException {
		// まだ前局面を探索中の場合、何もしない
		if (mateEngine.isSearching()) {
			return;
		}

		// 直近の「position」コマンドの局面を取得
		String position = StateInfo.getInstance().getLatestPosition();

		// 詰探索エンジンのcheckmateの指し手をクリア
		mateEngine.setCheckmateMoves(null);
		// 詰探索エンジンに直近の「go mate」コマンドの局面をセット
		mateEngine.setLatestGoMatePosition(position);

		// 当該エンジンへのコマンドリストに追加
		// ・スレッドの排他制御
		synchronized (mateEngine.getOutputThread()) {
			mateEngine.getOutputThread().getCommandList().add(position);

			// タイムアウトを設定して「go mate」コマンドを送信
			// mateEngine.getOutputThread().getCommandList().add("go mate infinite");
			mateEngine.getOutputThread().getCommandList().add("go mate " + StateInfo.getInstance().getMateTimeout());
		}

		// 探索中フラグをセット
		mateEngine.setSearching(true);
	}

	/**
	 * 当該エンジンからのコマンドを標準出力（GUI側）へのコマンドリストに追加する（通常の場合）
	 * 
	 * @param systemOutputThread
	 * @param engine
	 */
	private void enginesToSysoutAtNormal(OutputStreamThread systemOutputThread, UsiEngine engine) {
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
					// 「bestmove」の場合、この段階ではGUIへ返さず、エンジンに保存しておく。
					if (command.startsWith("bestmove")) {
						// bestmoveをエンジンに保存
						engine.setBestmoveCommand(command);
					}

					// その他の場合、標準出力（GUI側）へのコマンドリストに追加
					else {
						systemOutputThread.getCommandList().add(command);

						// 「info～score～」の場合、直近の読み筋をエンジンに保存
						if (command.startsWith("info ") && command.indexOf("score ") >= 0) {
							engine.setLatestPv(command);
						}
					}
				}
			}
		}
	}

	/**
	 * 詰探索エンジンからのコマンドを標準出力（GUI側）へのコマンドリストに追加する
	 * 
	 * @param systemOutputThread
	 * @param mateEngine
	 */
	private void mateEnginesToSysout(OutputStreamThread systemOutputThread, MateEngine mateEngine) {
		InputStreamThread processInputThread = mateEngine.getInputThread();
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
					// 標準出力（GUI側）へのコマンドリストに追加
					systemOutputThread.getCommandList().add("info string " + command);

					// コマンドが「checkmate」で始まる場合
					if (command.startsWith("checkmate")) {
						// 探索中フラグを落とす
						mateEngine.setSearching(false);
					}

					// （例）「checkmate R*6a 5a6a 2a4a 6a6b 8b7a」の場合
					// ・「checkmate nomate」「checkmate timeout」「checkmateのみ」「checkmate notimplemented」の場合は除く
					if (command.startsWith("checkmate ") && !("checkmate nomate".equals(command) || "checkmate timeout".equals(command) || "checkmate".equals(command) || "checkmate notimplemented".equals(command))) {
						logger.info("Utils.equals(mateEngine.getLatestGoMatePosition(), StateInfo.getInstance().getLatestPosition())=" + Utils.equals(mateEngine.getLatestGoMatePosition(), StateInfo.getInstance().getLatestPosition()));

						// 直近の「go mate」コマンドの局面と直近の「position」コマンドの局面が一致する場合
						// ・実際の局面は次の指し手に移っても、詰探索エンジンは前の局面を思考し続けている場合があるので。
						if (Utils.equals(mateEngine.getLatestGoMatePosition(), StateInfo.getInstance().getLatestPosition())) {
							// checkmateの指し手をセット
							mateEngine.setCheckmateMoves(command.substring("checkmate ".length()));

							// 詰探索エンジンの読み筋（評価値を含む）を作成して出力する
							// （例）「info score mate 5 pv R*6a 5a6a 2a4a 6a6b 8b7a」
							systemOutputThread.getCommandList().add(mateEngine.createPv());
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
	 * @param currentEngine
	 * @param mateEngine
	 * @return
	 */
	private boolean executeGougi(OutputStreamThread systemOutputThread, UsiEngine currentEngine, MateEngine mateEngine) {
		// currentEngineからbestmoveが返ってきていない場合
		if (Utils.isEmpty(currentEngine.getBestmoveCommand())) {
			return false;
		}

		// currentEngineからbestmoveが返ってきている場合
		else {
			// 合議結果を標準出力（GUI側）へのコマンドリストに追加
			// ・スレッドの排他制御
			synchronized (systemOutputThread) {
				// 詰探索エンジンが詰みを発見していない場合
				// ・通常の探索エンジンの指し手を採用する
				if (Utils.isEmpty(mateEngine.getCheckmateMoves())) {
					// 合議で指し手が採用されたエンジンの読み筋（評価値を含む）を再度出力しておく
					// ・将棋所等では、bestmove直前の読み筋（「info～score～pv～」）が評価値・読み筋として表示されるため、指し手が採用されなかったエンジンが直近だと指し手と読み筋が矛盾するので。
					// ・詰探索エンジンとの合議の場合はあえて再度出力する必要はないかもしれないが、一応出力しておく
					if (!Utils.isEmpty(currentEngine.getLatestPv())) {
						systemOutputThread.getCommandList().add(currentEngine.getLatestPv());
					}

					// ----- [O][X]の出力
					// ・通常の探索エンジンの方を後にする
					// （例）「info string [X] [詰み無し]　[NanohaTsumeUSI]」
					systemOutputThread.getCommandList().add("info string [X] " + mateEngine.getMateDisp());
					// （例）「info string [O] bestmove 7g7f ponder 3c3d [７六(77) ３四(33)] [評価値 123 （前回100 差分23）] [Gikou 20160606]」
					systemOutputThread.getCommandList().add("info string [O] " + currentEngine.getBestmoveScoreDisp(true, true));
					// -----

					// bestmoveをGUIへ返す
					systemOutputThread.getCommandList().add(currentEngine.getBestmoveCommand());
				}

				// 詰探索エンジンが詰みを発見した場合
				// ・詰探索エンジンの指し手を採用する
				else {
					// 詰探索エンジンの読み筋（評価値を含む）を作成して出力する
					// （例）「info score mate 5 pv R*6a 5a6a 2a4a 6a6b 8b7a」
					// ・将棋所等では、bestmove直前の読み筋（「info～score～pv～」）が評価値・読み筋として表示されるため、指し手が採用されなかったエンジンが直近だと指し手と読み筋が矛盾するので。
					systemOutputThread.getCommandList().add(mateEngine.createPv());

					// 詰探索エンジンの指し手
					String mateBestmoveCommand = mateEngine.createBestmoveCommand();

					// ----- [O][X]の出力
					// ・詰探索エンジンの方を後にする

					// currentEngineの指し手が採用されたか否か。丸とバツだが、「O」と「X」で代替する。
					String hantei = mateBestmoveCommand.equals(currentEngine.getBestmoveCommandExceptPonder()) ? "O" : "X";
					// （例）「info string [X] bestmove 7g7f ponder 3c3d [７六(77) ３四(33)] [評価値 123 （前回100 差分23）] [Gikou 20160606]」
					systemOutputThread.getCommandList().add("info string [" + hantei + "] " + currentEngine.getBestmoveScoreDisp(true, true));

					// （例）「info string [O] [5手詰め]　[６一飛打 ６一(51) ４一(21) ６二(61) ７一(82)]　[NanohaTsumeUSI]」
					systemOutputThread.getCommandList().add("info string [O] " + mateEngine.getMateDisp());
					// -----

					// bestmoveをGUIへ返す
					systemOutputThread.getCommandList().add(mateBestmoveCommand);
				}
			}

			// 【前回の値】を保存
			currentEngine.savePrevValue();
			// GUIに返し終わるまで待つ
			systemOutputThread.waitUntilEmpty(10 * 1000);

			// 合議完了
			return true;
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
			// 詰探索エンジンのタイムアウト（ミリ秒）
			systemOutputThread.getCommandList().add("option name G_MateTimeout type spin default 10000 min 1 max 1000000");
		}
	}

}
