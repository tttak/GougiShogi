package co.jp.shogi.gougi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * USIロジック6
 * ・合議タイプが「詰探索エンジンとの合議（読み筋の局面も詰探索）」の場合に使う
 * ・現局面に加えて、読み筋の局面も詰探索する。読み筋の局面で詰みを見つけた場合、通常の探索エンジン側へmateinfoコマンド（当プログラム用の独自の拡張コマンド）を送り、詰みがあることを伝える。
 */
public class UsiLogic6 extends UsiLogicCommon {

	/** Logger */
	protected static Logger logger = Logger.getLogger(UsiLogic6.class.getName());

	// 読み筋局面用の詰探索のカウンター
	private int g_cnt_pvMate_tried = 0;
	private int g_cnt_pvMate_success = 0;
	private int g_cnt_pvMate_last_displayed = 0;

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

		// 読み筋局面用の詰探索エンジンリスト
		logger.info("読み筋局面用の詰探索エンジンリストの起動");
		List<MateEngine> pvMateEngineList = createPvMateEngineList(mateEngine);
		usiEngineList.addAll(pvMateEngineList);

		// sfen変換用やねうら王の起動
		logger.info("sfen変換用やねうら王の起動");
		UsiEngine sfenYaneuraOu = new UsiEngine();
		sfenYaneuraOu.setEngineNumber(99);
		sfenYaneuraOu.setExeFile(new File(Utils.getMyDir().getAbsolutePath() + File.separator + Constants.SFEN_YANEURAOU_FILENAME));
		sfenYaneuraOu.createProcess();

		// 詰探索済みの読み筋局面セット
		// ・要素の例「position startpos moves 2g2f 3c3d」
		Set<String> searchedPvPosSet = new HashSet<String>();
		// 詰探索済みの読み筋局面（sfen）セット
		// ・要素の例「sfen lnsgkgsnl/1r5b1/pppppp1pp/6p2/9/7P1/PPPPPPP1P/1B5R1/LNSGKGSNL b - 3」
		Set<String> searchedPvSfenSet = new HashSet<String>();
		// mateinfoコマンドリスト
		List<MateInfoBean> mateInfoList = new ArrayList<MateInfoBean>();

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
					// 全エンジンの読み筋リストをクリア
					// ShogiUtils.clearPvList(usiEngineList);
					// 標準入力（GUI側）からのコマンドを現在のエンジンへのコマンドリストに追加する（「go」または「go ponder」の場合）
					sysinToEnginesAtGoOrGoPonder(command, currentEngine);
					// 標準入力（GUI側）からのコマンドを詰探索エンジンへのコマンドリストに追加する（「go」または「go ponder」の場合）
					sysinToMateEngineAtGoOrGoPonder(mateEngine, sfenYaneuraOu);
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
				// 詰探索エンジンの探索中フラグにfalseをセット
				mateEngine.setSearching(false);
				// 各エンジンからのコマンドを標準出力（GUI側）へのコマンドリストに追加する（「isready」の場合）
				enginesToSysoutAtIsReady(systemOutputThread, usiEngineList);
				logger.info("「isready」の場合 END");
			}

			// 標準入力（GUI側）からのコマンドに「usinewgame」が含まれる場合
			if (Utils.contains(systemInputCommandList, "usinewgame")) {
				logger.info("「usinewgame」の場合 START");
				StateInfo.getInstance().setDuringGame(true);
				searchedPvPosSet = new HashSet<String>();
				searchedPvSfenSet = new HashSet<String>();
				// 全エンジンの読み筋リストをクリア
				ShogiUtils.clearPvList(usiEngineList);

				// 詰探索エンジンの探索中フラグを落とす
				mateEngine.setSearching(false);
				for (MateEngine pvMateEngine : pvMateEngineList) {
					pvMateEngine.setSearching(false);
				}

				// 読み筋局面用の詰探索のカウンターのクリア
				g_cnt_pvMate_tried = 0;
				g_cnt_pvMate_success = 0;
				g_cnt_pvMate_last_displayed = 0;

				// mateinfoコマンドリストのクリア
				mateInfoList = new ArrayList<MateInfoBean>();

				logger.info("「usinewgame」の場合 END");
			}

			// 標準入力（GUI側）からのコマンドに「gameover」が含まれる場合
			if (Utils.containsStartsWith(systemInputCommandList, "gameover")) {
				logger.info("「gameover」の場合 START");
				StateInfo.getInstance().setDuringGame(false);
				// 全エンジンの読み筋リストをクリア
				ShogiUtils.clearPvList(usiEngineList);
				// mateinfoコマンドリストのクリア
				mateInfoList = new ArrayList<MateInfoBean>();
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
			mateEnginesToSysout(systemOutputThread, mateEngine, sfenYaneuraOu);
			// 現在のエンジンからのコマンドを標準出力（GUI側）へのコマンドリストに追加する（通常の場合）
			enginesToSysoutAtNormal(systemOutputThread, currentEngine);

			// 対局中の場合
			// if (StateInfo.getInstance().isDuringGame()) {
			// 実行回数を適当に減らしておく
			if (Math.random() < 0.1) {
				// 読み筋局面用の詰探索処理（詰探索エンジンへ）
				pvMateToMateEngine(currentEngine, pvMateEngineList, sfenYaneuraOu, searchedPvPosSet, searchedPvSfenSet);
				// 読み筋局面用の詰探索処理（詰探索エンジンより）
				pvMateFromMateEngine(currentEngine, pvMateEngineList, systemOutputThread, mateInfoList);
				// 現在のエンジンへmateinfoコマンドを送信
				sendMateInfoToCurrentEngine(currentEngine, mateInfoList);
			}
			// }

			// 読み筋局面用の詰探索のカウンターを標準出力（GUI側）へのコマンドリストに追加する
			if (g_cnt_pvMate_tried % 50 == 0 && g_cnt_pvMate_tried > 0 && g_cnt_pvMate_tried != g_cnt_pvMate_last_displayed) {
				synchronized (systemOutputThread) {
					systemOutputThread.getCommandList().add("info string pvMateTriedCount=" + g_cnt_pvMate_tried + ", pvMateSuccessCount=" + g_cnt_pvMate_success);
				}
				g_cnt_pvMate_last_displayed = g_cnt_pvMate_tried;
			}

			// sleep
			Thread.sleep(10);
		}

		// sfen変換用やねうら王にquitコマンド送信
		// ・スレッドの排他制御
		synchronized (sfenYaneuraOu.getOutputThread()) {
			sfenYaneuraOu.getOutputThread().getCommandList().add("quit");
		}

		// 念のためプロセス終了
		Thread.sleep(500);
		sfenYaneuraOu.destroy();

		// 読み筋局面用の詰探索エンジンのプロセス終了
		// ・既にquitコマンドが送信されているはずだが、念のため。
		// ・読み筋局面用ではない方の詰探索エンジン（mateEngine）はSimpleGougiShogiMainでプロセス終了しているので、ここでは不要。（本来は統一した方がいいが．．．）
		if (pvMateEngineList != null) {
			for (UsiEngine engine : pvMateEngineList) {
				engine.destroy();
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
	 * ・現局面（直近の「position」コマンドの局面）で「go mate」コマンドを送信する。
	 * 
	 * @param mateEngine
	 * @param sfenYaneuraOu
	 * @throws IOException
	 */
	protected void sysinToMateEngineAtGoOrGoPonder(MateEngine mateEngine, UsiEngine sfenYaneuraOu) throws IOException {
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

		// sfen変換用やねうら王からsfen文字列を取得
		String sfen = getSfenFromYaneuraOu(sfenYaneuraOu, position);
		logger.info("sfen変換用やねうら王から取得したsfen文字列[" + sfen + "]");

		// 当該エンジンへのコマンドリストに追加
		// ・スレッドの排他制御
		synchronized (mateEngine.getOutputThread()) {
			// sfen変換用やねうら王から取得したsfen文字列を詰探索エンジンへ送信
			mateEngine.getOutputThread().getCommandList().add(sfen);

			// タイムアウトを設定して「go mate」コマンドを送信
			// mateEngine.getOutputThread().getCommandList().add("go mate infinite");
			mateEngine.getOutputThread().getCommandList().add("go mate " + StateInfo.getInstance().getMateTimeout());
		}

		// 探索中フラグをセット
		mateEngine.setSearching(true);
	}

	/**
	 * sfen変換用やねうら王からsfen文字列を取得
	 * （例）「position startpos moves 7g7f」
	 * →「position sfen lnsgkgsnl/1r5b1/ppppppppp/9/9/2P6/PP1PPPPPP/1B5R1/LNSGKGSNL w - 2」
	 * 
	 * @param sfenYaneuraOu
	 * @param position
	 * @return
	 */
	private String getSfenFromYaneuraOu(UsiEngine sfenYaneuraOu, String position) {
		// sfen変換用やねうら王
		// ・スレッドの排他制御
		synchronized (sfenYaneuraOu.getOutputThread()) {
			// sfen変換用やねうら王に「position」コマンドを送信
			sfenYaneuraOu.getOutputThread().getCommandList().add(position);

			// sfen変換用やねうら王に「printsfen」コマンド（独自に拡張したコマンド）を送信
			logger.info("sfen変換用やねうら王に「printsfen」コマンド（独自に拡張したコマンド）を送信");
			sfenYaneuraOu.getOutputThread().getCommandList().add("printsfen");
		}

		// sfen変換用やねうら王から「sfen」が返ってくるまで待つ
		sfenYaneuraOu.getInputThread().waitUntilCommandStartsWith("sfen", 10 * 1000);
		logger.info("sfen変換用やねうら王から「sfen」を受信");

		String sfen = "";

		// 「sfen」を取得した後、削除する
		// ・スレッドの排他制御
		synchronized (sfenYaneuraOu.getInputThread()) {
			sfen = Utils.getItemStartsWith(sfenYaneuraOu.getInputThread().getCommandList(), "sfen");
			Utils.removeFromListWhereStartsWith(sfenYaneuraOu.getInputThread().getCommandList(), "sfen");
		}

		return "position " + sfen;
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

						// 「info～pv～」の場合、読み筋をエンジンに保存
						if (command.startsWith("info ") && command.indexOf(" pv ") >= 0) {
							// （例）「info depth 3 seldepth 4 multipv 1 score cp 502 nodes 774 nps 154800 time 5 pv 2g3g 4h3g 2c3d S*3f」
							// → 「2g3g 4h3g 2c3d S*3f」
							String pv = command.substring(command.indexOf(" pv ") + " pv ".length()).trim();

							// ただし、定跡の指し手などで下例のようなコマンドが送られてきた場合は除外する。
							// （例）「info pv 7g7f 8c8d (38.62%) score cp 0 depth 32 multipv 1」
							// ・「 pv 」より右の文字列に読み筋以外が含まれており、これをそのままsfen変換用やねうら王へ渡すと結果が保証されないため。
							if (pv.indexOf("(") < 0) {
								engine.getPvList().add(pv);
							}
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
	 * @param sfenYaneuraOu
	 * @throws IOException
	 */
	private void mateEnginesToSysout(OutputStreamThread systemOutputThread, MateEngine mateEngine, UsiEngine sfenYaneuraOu) throws IOException {
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

						logger.info("Utils.equals(mateEngine.getLatestGoMatePosition(), StateInfo.getInstance().getLatestPosition())=" + Utils.equals(mateEngine.getLatestGoMatePosition(), StateInfo.getInstance().getLatestPosition()));

						// 直近の「go mate」コマンドの局面と直近の「position」コマンドの局面が一致する場合
						// ・実際の局面は次の指し手に移っても、詰探索エンジンは前の局面を思考し続けている場合があるので。
						if (Utils.equals(mateEngine.getLatestGoMatePosition(), StateInfo.getInstance().getLatestPosition())) {
							// （例）「checkmate R*6a 5a6a 2a4a 6a6b 8b7a」の場合
							// ・「checkmate nomate」「checkmate timeout」「checkmateのみ」「checkmate notimplemented」の場合は除く
							if (command.startsWith("checkmate ") && !("checkmate nomate".equals(command) || "checkmate timeout".equals(command) || "checkmate".equals(command) || "checkmate notimplemented".equals(command))) {
								// checkmateの指し手をセット
								mateEngine.setCheckmateMoves(command.substring("checkmate ".length()));

								// 詰探索エンジンの読み筋（評価値を含む）を作成して出力する
								// （例）「info score mate 5 pv R*6a 5a6a 2a4a 6a6b 8b7a」
								systemOutputThread.getCommandList().add(mateEngine.createPv());
							}
						}

						// その他の場合（直近の「go mate」コマンドの局面と直近の「position」コマンドの局面が異なる場合）
						// ・実際の局面は次の指し手に移っても、詰探索エンジンは前の局面を思考し続けていた場合
						else {
							// 対局中の場合
							if (StateInfo.getInstance().isDuringGame()) {
								// 現局面（直近の「position」コマンドの局面）で「go mate」コマンドを送信
								sysinToMateEngineAtGoOrGoPonder(mateEngine, sfenYaneuraOu);
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
				// 読み筋局面用の詰探索のカウンターを標準出力（GUI側）へのコマンドリストに追加する
				systemOutputThread.getCommandList().add("info string pvMateTriedCount=" + g_cnt_pvMate_tried + ", pvMateSuccessCount=" + g_cnt_pvMate_success);
				g_cnt_pvMate_last_displayed = g_cnt_pvMate_tried;

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
					// （例）「info string [X] [詰み無し] [NanohaTsumeUSI]」
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

					// （例）「info string [O] [5手詰め] [６一飛打 ６一(51) ４一(21) ６二(61) ７一(82)] [NanohaTsumeUSI]」
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

	/*
	 * (non-Javadoc)
	 * 各エンジンからのコマンドを標準出力（GUI側）へのコマンドリストに追加する（「usi」の場合）2
	 * @see co.jp.shogi.gougi.UsiLogicCommon#enginesToSysoutAtUsi2(co.jp.shogi.gougi.OutputStreamThread, java.util.List)
	 */
	@Override
	protected void enginesToSysoutAtUsi2(OutputStreamThread systemOutputThread, List<UsiEngine> usiEngineList) {
		// 独自オプションを追加
		// ・スレッドの排他制御
		synchronized (systemOutputThread) {
			// 詰探索エンジンのタイムアウト（ミリ秒）
			systemOutputThread.getCommandList().add("option name G_MateTimeout type spin default 10000 min 1 max 1000000");
			// 読み筋局面用の詰探索エンジンのタイムアウト（ミリ秒）
			systemOutputThread.getCommandList().add("option name G_PvMateTimeout type spin default 5000 min 1 max 1000000");
			// mateinfoコマンドの送信回数
			systemOutputThread.getCommandList().add("option name G_MateInfoCount type spin default 100 min 1 max 1000");
			// mateinfoコマンドの送信間隔（ミリ秒）
			systemOutputThread.getCommandList().add("option name G_MateInfoInterval type spin default 100 min 1 max 100000");
		}
	}

	/**
	 * 読み筋局面用の詰探索エンジンリストの起動
	 * @param mateEngine
	 * @return
	 * @throws IOException
	 */
	private List<MateEngine> createPvMateEngineList(MateEngine mateEngine) throws IOException {
		List<MateEngine> pvMateEngineList = new ArrayList<MateEngine>();

		for (int i = 0; i < GougiConfig.getInstance().getPvMateEngineCount(); i++) {
			MateEngine pvMateEngine = new MateEngine();
			pvMateEngine.setEngineNumber(11 + i);
			// 実行ファイルは普通の詰み探索エンジンと同じ
			pvMateEngine.setExeFile(mateEngine.getExeFile());
			// 起動
			pvMateEngine.createProcess();
			// リストに追加
			pvMateEngineList.add(pvMateEngine);
		}

		return pvMateEngineList;
	}

	/**
	 * 読み筋局面用の詰探索処理（詰探索エンジンへ）
	 * @param currentEngine
	 * @param pvMateEngineList
	 * @param sfenYaneuraOu
	 * @param searchedPvPosSet
	 * @param searchedPvSfenSet
	 */
	private void pvMateToMateEngine(UsiEngine currentEngine, List<MateEngine> pvMateEngineList, UsiEngine sfenYaneuraOu, Set<String> searchedPvPosSet, Set<String> searchedPvSfenSet) {
		// 探索中ではない詰探索エンジンが存在しない場合
		if (ShogiUtils.getMateEngineNotSearching(pvMateEngineList) == null) {
			return;
		}

		// 読み筋リストをループ
		// ・新しい方から先に
		for (int i = currentEngine.getPvList().size() - 1; i >= 0; i--) {
			// 直近の「position」コマンドの局面（「go（ponderではない）」か「go ponder」かは問わない）
			// （例）「position startpos moves 2g2f 3c3d」
			String pos = StateInfo.getInstance().getLatestPosition();

			// もう少し厳密に判定した方がよいが．．．
			// if (pos.equals("position startpos")) {
			if (pos.indexOf(" moves ") < 0) {
				pos += " moves";
			}
			// logger.info("i=" + i + ", pos=" + pos);

			// pvの例：「7g7f 4c4d 3i4h」
			String pv = currentEngine.getPvList().get(i);
			String[] sa = pv.split(" ", -1);

			for (int j = 0; j < sa.length; j++) {
				pos += " " + sa[j];
				// logger.info("i=" + i + ", j=" + j + ", pos=" + pos);

				// 探索中ではない詰探索エンジンを返す
				MateEngine pvMateEngine = ShogiUtils.getMateEngineNotSearching(pvMateEngineList);
				if (pvMateEngine == null) {
					return;
				}

				// 未探索の局面の場合
				if (!searchedPvPosSet.contains(pos)) {
					// 詰探索済みの読み筋局面セットに追加
					searchedPvPosSet.add(pos);

					// sfen変換用やねうら王からsfen文字列を取得
					String sfen = getSfenFromYaneuraOu(sfenYaneuraOu, pos);
					logger.info("sfen変換用やねうら王から取得したsfen文字列[" + sfen + "]");

					// 未探索の局面の場合
					if (!searchedPvSfenSet.contains(sfen)) {
						// 詰探索済みの読み筋局面（sfen）セットに追加
						searchedPvSfenSet.add(sfen);

						logger.info("searchedPvPosSet.size()=" + searchedPvPosSet.size());
						logger.info("searchedPvSfenSet.size()=" + searchedPvSfenSet.size());

						// 詰探索エンジンのcheckmateの指し手をクリア
						pvMateEngine.setCheckmateMoves(null);
						// 詰探索エンジンに直近の「go mate」コマンドの局面をセット
						pvMateEngine.setLatestGoMatePosition(pos);
						pvMateEngine.setLatestGoMatePositionSfen(sfen);

						// 当該エンジンへのコマンドリストに追加
						// ・スレッドの排他制御
						synchronized (pvMateEngine.getOutputThread()) {
							// sfen変換用やねうら王から取得したsfen文字列を詰探索エンジンへ送信
							pvMateEngine.getOutputThread().getCommandList().add(sfen);

							// タイムアウトを設定して「go mate」コマンドを送信
							// mateEngine.getOutputThread().getCommandList().add("go mate infinite");
							pvMateEngine.getOutputThread().getCommandList().add("go mate " + StateInfo.getInstance().getPvMateTimeout());
						}

						// 探索中フラグをセット
						pvMateEngine.setSearching(true);
						// 読み筋局面用の詰探索のカウンター
						g_cnt_pvMate_tried++;
					}
				}
			}
		}
	}

	/**
	 * 読み筋局面用の詰探索処理（詰探索エンジンより）
	 * @param currentEngine
	 * @param pvMateEngineList
	 * @param systemOutputThread
	 * @param mateInfoList
	 */
	private void pvMateFromMateEngine(UsiEngine currentEngine, List<MateEngine> pvMateEngineList, OutputStreamThread systemOutputThread, List<MateInfoBean> mateInfoList) {
		for (MateEngine pvMateEngine : pvMateEngineList) {
			InputStreamThread processInputThread = pvMateEngine.getInputThread();
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
				for (String command : processInputCommandList) {
					// コマンドが「checkmate」で始まる場合
					if (command.startsWith("checkmate")) {
						// 探索中フラグを落とす
						pvMateEngine.setSearching(false);

						// （例）「checkmate R*6a 5a6a 2a4a 6a6b 8b7a」の場合
						// ・「checkmate nomate」「checkmate timeout」「checkmateのみ」「checkmate notimplemented」の場合は除く
						if (command.startsWith("checkmate ") && !("checkmate nomate".equals(command) || "checkmate timeout".equals(command) || "checkmate".equals(command) || "checkmate notimplemented".equals(command))) {
							// 標準出力（GUI側）へのコマンドリストに追加
							// ・スレッドの排他制御
							synchronized (systemOutputThread) {
								// 標準出力（GUI側）へのコマンドリストに追加
								systemOutputThread.getCommandList().add("info string pvMate : " + command);
							}

							// checkmateの指し手をセット
							pvMateEngine.setCheckmateMoves(command.substring("checkmate ".length()));

							// 「mateinfo」コマンドを作成
							// （例）「mateinfo position sfen ln1g1g1n1/2s1k4/p1ppp3p/5ppR1/P8/2P1LKP2/3PPP2P/5+rS2/L5sNL b BGSN2Pbg2p 1 checkmate 5f5c+ 5b5c N*6e 5c4b S*4c 4b5a G*4b 4a4b 4c4b+ 5a4b 2d2b+ S*3b G*5c 4b4a B*5b 6a5b 5c5b 4a5b 2b3b L*4b S*5c 5b6a G*6b」
							String mateinfo = "mateinfo " + pvMateEngine.getLatestGoMatePositionSfen() + " " + command;

							// MateInfoBeanを生成し、mateinfoコマンドリストへ追加
							mateInfoList.add(new MateInfoBean(mateinfo));

							// 読み筋局面用の詰探索のカウンター
							g_cnt_pvMate_success++;

							// -----
						}
					}
				}
			}
		}
	}

	/**
	 * 現在のエンジンへmateinfoコマンドを送信
	 * @param currentEngine
	 * @param mateInfoList
	 */
	private void sendMateInfoToCurrentEngine(UsiEngine currentEngine, List<MateInfoBean> mateInfoList) {

		// ----- mateinfoコマンドを送信

		// mateinfoコマンドリストをループ
		for (MateInfoBean mateInfoBean : mateInfoList) {
			// 前回のmateinfoコマンド送信から一定時間以上経過した場合
			if (System.currentTimeMillis() > mateInfoBean.getPrevTime() + StateInfo.getInstance().getMateInfoInterval()) {
				logger.info((mateInfoBean.getCount() + 1) + "回目のmateinfoコマンド送信：" + mateInfoBean.getCommand());

				// mateinfoコマンドを送信
				// スレッドの排他制御
				synchronized (currentEngine.getOutputThread()) {
					currentEngine.getOutputThread().getCommandList().add(mateInfoBean.getCommand());
				}

				// 送信回数と前回送信時刻を更新
				mateInfoBean.setCount(mateInfoBean.getCount() + 1);
				mateInfoBean.setPrevTime(System.currentTimeMillis());
			}
		}

		// ----- 設定された送信回数まで送信し終えたMateInfoBeanを削除

		// mateInfoListの要素をループ中で削除するのでIteratorを使う
		Iterator<MateInfoBean> it = mateInfoList.iterator();

		// mateinfoコマンドリストをループ
		while (it.hasNext()) {
			MateInfoBean mateInfoBean = it.next();

			if (mateInfoBean.getCount() >= StateInfo.getInstance().getMateInfoCount()) {
				logger.info("設定された送信回数まで送信し終えたMateInfoBeanを削除：" + mateInfoBean.getCount() + "回、" + mateInfoBean.getCommand());

				it.remove();
			}
		}

	}

}
