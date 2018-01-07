package co.jp.shogi.gougi;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * USIロジック1
 * 合議タイプが以下のいずれかの場合に使う
 * ・「多数決合議（3者）」
 * ・「楽観合議」
 * ・「悲観合議」
 * ・「楽観合議と悲観合議を交互」
 * ・「2手前の評価値からの上昇分の楽観合議」
 * ・「2手前の評価値からの上昇分の悲観合議」
 */
public class UsiLogic1 extends UsiLogicCommon {

	/** Logger */
	protected static Logger logger = Logger.getLogger(UsiLogic1.class.getName());

	/**
	 * executeメソッド
	 * 
	 * @param usiEngineList
	 * @param systemInputThread
	 * @param systemOutputThread
	 * @throws Exception
	 */
	public void execute(List<UsiEngine> usiEngineList, InputStreamThread systemInputThread, OutputStreamThread systemOutputThread) throws Exception {
		// 前回の合議タイプ
		String prevGougiType = null;

		while (true) {
			// 保留中の標準入力（GUI側）からのコマンドリスト
			// ・goコマンド、go ponderコマンド、ponder時のstopコマンド、ponderhitコマンドは一旦このリストにためる
			List<String> pendingSysinCommandList = new ArrayList<String>();

			// 標準入力（GUI側）からのコマンドを各エンジンへのコマンドリストに追加する
			List<String> systemInputCommandList = sysinToEngines(systemInputThread, usiEngineList, pendingSysinCommandList);

			// 保留中の標準入力（GUI側）からのコマンドリストをループ
			for (String command : pendingSysinCommandList) {
				logger.info("保留中の標準入力（GUI側）からのコマンドリストのコマンド=" + command);

				// 「go」コマンドの場合
				if (command.startsWith("go") && !command.startsWith("go ponder")) {
					logger.info("「go」の場合 START");
					// 自分の手番
					StateInfo.getInstance().setMyTurn(true);
					// 全エンジンのbestmove、直近の読み筋をクリア
					ShogiUtils.clearBestmoveLatestPv(usiEngineList);
					// 標準入力（GUI側）からのコマンドを各エンジンへのコマンドリストに追加する（「go」の場合）
					sysinToEnginesAtGo(command, usiEngineList);
					// 全エンジンについて、「事前ponder実施中か否か」のフラグを落とす
					ShogiUtils.setPrePondering(usiEngineList, false);
					logger.info("「go」の場合 END");
				}

				// 「go ponder」コマンドの場合
				else if (command.startsWith("go ponder")) {
					logger.info("GUIから「go ponder」を受信したが、エンジンへは送信しない。合議完了時に「go ponder」送信済みなので。");

					StateInfo.getInstance().setLatestGoPonderPosition(StateInfo.getInstance().getLatestPosition());
					StateInfo.getInstance().setPondering(true);
					// 全エンジンについて、「事前ponder実施中か否か」のフラグを落とす
					ShogiUtils.setPrePondering(usiEngineList, false);
				}

				// ponder時のstopまたはponderhitの場合
				else if (StateInfo.getInstance().isPondering() && ("stop".equals(command) || "ponderhit".equals(command))) {
					logger.info("ponder時のstopまたはponderhitの場合 START");
					StateInfo.getInstance().setPondering(false);
					// ponder終了時の処理
					endPonder(command, systemInputThread, systemOutputThread, usiEngineList);
					// 自分の手番
					StateInfo.getInstance().setMyTurn(true);
					logger.info("ponder時のstopまたはponderhitの場合 END");
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
				// 今回の合議タイプを取得
				String currentGougiType = calcCurrentGougiType(prevGougiType);

				// 合議を実行
				if (executeGougi(systemOutputThread, usiEngineList, currentGougiType)) {
					// 前回の合議タイプを更新
					prevGougiType = currentGougiType;
					// 相手番へ更新
					StateInfo.getInstance().setMyTurn(false);
					// 全エンジンのbestmove、直近の読み筋をクリア
					ShogiUtils.clearBestmoveLatestPv(usiEngineList);
				}
			}

			// 各エンジンからのコマンドを標準出力（GUI側）へのコマンドリストに追加する（通常の場合）
			enginesToSysoutAtNormal(systemOutputThread, usiEngineList);

			// sleep
			Thread.sleep(10);
		}
	}

	/**
	 * 各エンジンからのコマンドを標準出力（GUI側）へのコマンドリストに追加する（通常の場合）
	 * 
	 * @param systemOutputThread
	 * @param usiEngineList
	 */
	private void enginesToSysoutAtNormal(OutputStreamThread systemOutputThread, List<UsiEngine> usiEngineList) {
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
						// 事前ponder実施中の場合、読み飛ばす
						// ・標準出力（GUI側）へのコマンドリストに追加せずに、次へ
						if (engine.isPrePondering()) {
							continue;
						}

						// 「bestmove」の場合、この段階ではGUIへ返さず、エンジンに保存しておく。（3つ揃ってから合議結果をGUIへ返すので）
						// ・ただし、思考が終わったことがGUIから見えた方がよさそうなので、「info string」で返しておく。
						// TODO
						// else if (StateInfo.getInstance().isMyTurn() && command.startsWith("bestmove")) {
						else if (command.startsWith("bestmove")) {
							// bestmoveをエンジンに保存
							engine.setBestmoveCommand(command);
							// （例）「info string bestmove 7g7f [７六(77)] [評価値 123 （前回100 差分23）] [Gikou 20160606]」
							// [ponder対応]
							// （例）「info string bestmove 7g7f ponder 3c3d [７六(77) ３四(33)] [評価値 123 （前回100 差分23）] [Gikou 20160606]」
							systemOutputThread.getCommandList().add("info string " + engine.getBestmoveScoreDisp(true, true));

							// 当該エンジンのponderingフラグを落とす
							// ・前の手からのponderのbestmoveだった場合を考慮
							engine.setPondering(false);

							// [ponder対応]
							// 当該エンジンがponder設定onで、かつ、「bestmove」が「ponder」付きだった場合
							if (engine.isPonderOnOff() && !Utils.isEmpty(engine.getPonderMove())) {
								// bestmoveが返ってきていないエンジンが存在する場合
								// ・ただし、対局中に限る
								if (ShogiUtils.containsEmptyBestmove(usiEngineList) && StateInfo.getInstance().isDuringGame()) {
									// エンジンへのコマンドリストに追加
									// ・スレッドの排他制御
									synchronized (engine.getOutputThread()) {
										// 「go ponder」コマンドをエンジンへ送信
										// （例）「position startpos moves 1g1f 4a3b 6i7h」
										String pos = StateInfo.getInstance().getLatestGoNotPonderPosition();
										pos = ShogiUtils.appendMove(pos, engine.getBestmove());
										pos = ShogiUtils.appendMove(pos, engine.getPonderMove());

										engine.getOutputThread().getCommandList().add(pos);
										engine.getOutputThread().getCommandList().add("go ponder " + StateInfo.getInstance().getGoTimeOption());

										// ponder情報をエンジンにセット
										engine.setPondering(true);
										engine.setPonderingPosition(pos);
										engine.setPrePondering(true);
									}
								}
							}
						}

						// その他の場合、標準出力（GUI側）へのコマンドリストに追加
						else {
							systemOutputThread.getCommandList().add(command);

							// 「info～score～」の場合、直近の読み筋をエンジンに保存
							// ・ただし、ponder実施中を除く
							// → 事前ponder実施中を除く。（既に上のifで除外済み）
							// if (!engine.isPondering() && command.startsWith("info ") && command.indexOf("score ") >= 0) {
							// if (!StateInfo.getInstance().isPondering() && command.startsWith("info ") && command.indexOf("score ") >= 0) {
							if (command.startsWith("info ") && command.indexOf("score ") >= 0) {
								engine.setLatestPv(command);
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
				if (!Utils.isEmpty(resultEngine.getLatestPv())) {
					systemOutputThread.getCommandList().add(resultEngine.getLatestPv());
				}

				// 各エンジンのbestmoveを参考情報としてGUIへ出力する
				// ・GUIで見やすいようにエンジンは逆順にしておく（エンジン1が上に表示されるように）
				for (int i = usiEngineList.size() - 1; i >= 0; i--) {
					UsiEngine engine = usiEngineList.get(i);

					// （例）「bestmove 7g7f」
					String engineBest = engine.getBestmoveCommandExceptPonder();
					// このエンジンの指し手が採用されたか否か。丸とバツだが、「O」と「X」で代替する。
					String hantei = engineBest.equals(bestmoveCommand) ? "O" : "X";

					// （例）「info string [O] bestmove 7g7f [７六(77)] [評価値 123 （前回100 差分23）] [Gikou 20160606]」
					// [ponder対応]
					// （例）「info string [O] bestmove 7g7f ponder 3c3d [７六(77) ３四(33)] [評価値 123 （前回100 差分23）] [Gikou 20160606]」
					systemOutputThread.getCommandList().add("info string [" + hantei + "] " + engine.getBestmoveScoreDisp(true, true));

					// 【前回の値】を保存
					engine.savePrevValue();
				}

				// bestmoveをGUIへ返す
				// TODO [ponder対応]
				// systemOutputThread.getCommandList().add(bestmoveCommand);
				systemOutputThread.getCommandList().add(resultEngine.getBestmoveCommand());
			}

			// GUIに返し終わるまで待つ
			systemOutputThread.waitUntilEmpty(10 * 1000);

			// [ponder対応]
			// 合議完了時のponder処理
			ponderAtGougiEnd(usiEngineList, resultEngine);

			// 合議完了
			return true;
		}

		return false;
	}

	/**
	 * 合議完了時のponder処理
	 * 
	 * @param usiEngineList
	 * @param resultEngine
	 */
	private void ponderAtGougiEnd(List<UsiEngine> usiEngineList, UsiEngine resultEngine) {
		// 「bestmove」が「ponder」付きではなかった場合、何もしない
		if (Utils.isEmpty(resultEngine.getPonderMove())) {
			return;
		}

		// ponderの局面
		// （例）「position startpos moves 1g1f 4a3b 6i7h」
		String pos = StateInfo.getInstance().getLatestGoNotPonderPosition();
		pos = ShogiUtils.appendMove(pos, resultEngine.getBestmove());
		pos = ShogiUtils.appendMove(pos, resultEngine.getPonderMove());

		// 各エンジンをループ
		for (UsiEngine engine : usiEngineList) {
			// 当該エンジンがponder設定onの場合
			if (engine.isPonderOnOff()) {
				// 当該エンジンがponder実施中の場合
				if (engine.isPondering()) {
					// TODO
					// 当該エンジンと合議結果のエンジンのbestmoveの指し手が等しい場合（ponderの指し手が異なる場合を含む）
					// ・当該エンジンが合議結果のエンジンである場合を含む
					// if (engine.getBestmove().equals(resultEngine.getBestmove())) {
					// TODO
					// 当該エンジンと合議結果のエンジンのbestmoveの指し手とponderの指し手がともに存在し、かつ、等しい場合
					// ・当該エンジンが合議結果のエンジンである場合を含む
					if (ShogiUtils.equalsBestmoveAndPondermove(engine, resultEngine)) {
						logger.info("合議完了時にそのままponderを続ける[" + engine.getUsiName() + "]");
						// そのままponderを続ける
						continue;
					}

					// その他の場合、「stop」実行
					else {
						// 「stop」コマンドをエンジンへ送信
						// ・スレッドの排他制御
						synchronized (engine.getOutputThread()) {
							logger.info("合議完了時に「stop」コマンドをエンジンへ送信[" + engine.getUsiName() + "]");
							engine.getOutputThread().getCommandList().add("stop");
						}

						// エンジンから「bestmove」が返ってくるまで待つ
						engine.getInputThread().waitUntilCommandStartsWith("bestmove", 10 * 1000);
						logger.info("合議完了時の「stop」コマンドで「bestmove」受信[" + engine.getUsiName() + "]");

						// 「bestmove」を削除
						// ・スレッドの排他制御
						synchronized (engine.getInputThread()) {
							Utils.removeFromListWhereStartsWith(engine.getInputThread().getCommandList(), "bestmove");
							logger.info("合議完了時の「stop」コマンドで「bestmove」受信後に削除完了[" + engine.getUsiName() + "]");
						}

						// ponder情報をエンジンにセット
						engine.setPondering(false);
						engine.setPonderingPosition(null);
					}
				}

				// 対局中の場合
				if (StateInfo.getInstance().isDuringGame()) {
					// 「go ponder」実行
					// エンジンへのコマンドリストに追加
					// ・スレッドの排他制御
					synchronized (engine.getOutputThread()) {
						logger.info("合議完了時に「go ponder」コマンドをエンジンへ送信[" + engine.getUsiName() + "]");

						// 「go ponder」コマンドをエンジンへ送信
						engine.getOutputThread().getCommandList().add(pos);
						engine.getOutputThread().getCommandList().add("go ponder " + StateInfo.getInstance().getGoTimeOption());

						// ponder情報をエンジンにセット
						engine.setPondering(true);
						engine.setPonderingPosition(pos);
					}

					// 「go ponder」から「ponderhit」までの時間が短すぎるとエンジン側が認識できずにtimeupする場合があるようなので、少し待つ
					try {
						Thread.sleep(20);
					} catch (InterruptedException e) {
						// ignore
					}
				}
			}
		}
	}

	/**
	 * ponder終了時の処理
	 * ・前提：コマンドはstop、ponderhitのいずれかであること
	 * 
	 * @param command
	 * @param systemInputThread
	 * @param systemOutputThread
	 * @param usiEngineList
	 */
	private void endPonder(String command, InputStreamThread systemInputThread, OutputStreamThread systemOutputThread, List<UsiEngine> usiEngineList) {
		// ponderhitの場合
		if ("ponderhit".equals(command)) {
			logger.info("GUIからponderhit受信");

			// 直近の「go（ponderではない）」コマンドの局面をセット
			StateInfo.getInstance().setLatestGoNotPonderPosition(StateInfo.getInstance().getLatestGoPonderPosition());

			// 各エンジンをループ
			for (UsiEngine engine : usiEngineList) {
				// 当該エンジンがponder実施中の場合
				if (engine.isPondering()) {
					// TODO 当該エンジンが実際にponderhitした場合
					// if (engine.getPonderingPosition().equals(StateInfo.getInstance().getLatestGoPonderPosition())) {
					logger.info("GUIからponderhit受信時、当該エンジンに「ponderhit」コマンドを送信[" + engine.getUsiName() + "]");

					// 当該エンジンに「ponderhit」コマンドを送信
					// ・スレッドの排他制御
					synchronized (engine.getOutputThread()) {
						engine.getOutputThread().getCommandList().add("ponderhit");
					}

					// 次のエンジンへ
					continue;
					// }

					// TODO 当該エンジンが実際にはponderhitしなかった場合
					// else {
					// logger.info("GUIからponderhit受信時、当該エンジンに「stop」コマンドを送信[" + engine.getUsiName() + "]");
					//
					// // 当該エンジンに「stop」コマンドを送信
					// // ・スレッドの排他制御
					// synchronized (engine.getOutputThread()) {
					// engine.getOutputThread().getCommandList().add("stop");
					// }
					//
					// // 当該エンジンから「bestmove」が返ってくるまで待つ
					// engine.getInputThread().waitUntilCommandStartsWith("bestmove", 10 * 1000);
					//
					// logger.info("GUIからponderhit受信時、当該エンジンに「stop」コマンドを送信した後、「bestmove」受信[" + engine.getUsiName() + "]");
					// }
				}

				logger.info("GUIからponderhit受信時、「go」コマンドをエンジンへ送信[" + engine.getUsiName() + "]");

				// 「go」コマンドをエンジンへ送信
				engine.getOutputThread().getCommandList().add(StateInfo.getInstance().getLatestGoPonderPosition());
				engine.getOutputThread().getCommandList().add("go " + StateInfo.getInstance().getGoTimeOption());

				// エンジンのponder情報をクリア
				engine.setPondering(false);
				engine.setPonderingPosition(null);
			}
		}

		// stopの場合
		else if ("stop".equals(command)) {
			logger.info("GUIからstop受信");

			boolean firstFlg = true;
			String bestmoveCommand = "bestmove";

			// 各エンジンをループ
			for (UsiEngine engine : usiEngineList) {
				// 当該エンジンがponder実施中の場合
				if (engine.isPondering()) {
					// TODO 当該エンジンが実際にponderhitしなかった場合
					// if (engine.getPonderingPosition().equals(StateInfo.getInstance().getLatestGoPonderPosition())) {
					logger.info("GUIからstop受信時、当該エンジンに「stop」コマンドを送信[" + engine.getUsiName() + "]");

					// 当該エンジンに「stop」コマンドを送信
					// ・スレッドの排他制御
					synchronized (engine.getOutputThread()) {
						engine.getOutputThread().getCommandList().add("stop");
					}

					// 当該エンジンから「bestmove」が返ってくるまで待つ
					engine.getInputThread().waitUntilCommandStartsWith("bestmove", 10 * 1000);
					logger.info("GUIからstop受信時、当該エンジンに「stop」コマンドを送信した後、「bestmove」受信[" + engine.getUsiName() + "]");

					// 1件目の場合
					if (firstFlg) {
						firstFlg = false;

						// 「bestmove」を取得
						// ・スレッドの排他制御
						synchronized (engine.getInputThread()) {
							bestmoveCommand = Utils.getItemStartsWith(engine.getInputThread().getCommandList(), "bestmove");
						}
					}

					// 「bestmove」を削除
					// ・スレッドの排他制御
					synchronized (engine.getInputThread()) {
						Utils.removeFromListWhereStartsWith(engine.getInputThread().getCommandList(), "bestmove");
						logger.info("GUIからstop受信時、当該エンジンに「stop」コマンドを送信した後、「bestmove」受信したが、コマンドリストから削除[" + engine.getUsiName() + "]");
					}
					// }
				}
			}

			// GUIに「bestmove」を1回だけ返す
			// ・スレッドの排他制御
			synchronized (systemOutputThread) {
				systemOutputThread.getCommandList().add(bestmoveCommand);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 各エンジンからのコマンドを標準出力（GUI側）へのコマンドリストに追加する（「usi」の場合）2
	 * @see co.jp.shogi.gougi.UsiLogicCommon#enginesToSysoutAtUsi2(co.jp.shogi.gougi.OutputStreamThread, java.util.List)
	 */
	@Override
	protected void enginesToSysoutAtUsi2(OutputStreamThread systemOutputThread, List<UsiEngine> usiEngineList) {
		// 何もしない
	}

}
