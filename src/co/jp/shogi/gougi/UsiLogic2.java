package co.jp.shogi.gougi;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * USIロジック2
 */
public class UsiLogic2 extends UsiLogicCommon {

	/** Logger */
	protected static Logger logger = Logger.getLogger(UsiLogic2.class.getName());

	/**
	 * executeメソッド
	 * 
	 * @param usiEngineList
	 * @param systemInputThread
	 * @param systemOutputThread
	 * @throws Exception
	 */
	public void execute(List<UsiEngine> usiEngineList, InputStreamThread systemInputThread, OutputStreamThread systemOutputThread) throws Exception {

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
					// 最善手の交換前
					StateInfo.getInstance().setBefore_exchange_flg(true);
					// 全エンジンのbestmove、直近の読み筋をクリア
					ShogiUtils.clearBestmoveLatestPv(usiEngineList);
					// 全エンジンの【最善手の交換前の値】bestmove、直近の読み筋をクリア
					ShogiUtils.clear_before_exchange_BestmoveLatestPv(usiEngineList);
					// 標準入力（GUI側）からのコマンドを各エンジンへのコマンドリストに追加する（「go」の場合）
					sysinToEnginesAtGo(command, usiEngineList);
					logger.info("「go」の場合 END");
				}

				// 「go ponder」コマンドの場合
				else if (command.startsWith("go ponder")) {
					logger.info("「go ponder」の場合 START");
					// 相手の手番
					StateInfo.getInstance().setMyTurn(false);
					// 最善手の交換前
					StateInfo.getInstance().setBefore_exchange_flg(true);
					// 全エンジンのbestmove、直近の読み筋をクリア
					ShogiUtils.clearBestmoveLatestPv(usiEngineList);
					// 全エンジンの【最善手の交換前の値】bestmove、直近の読み筋をクリア
					ShogiUtils.clear_before_exchange_BestmoveLatestPv(usiEngineList);
					// ponder実施中フラグをセット
					StateInfo.getInstance().setPondering(true);
					// 標準入力（GUI側）からのコマンドを各エンジンへのコマンドリストに追加する（「go ponder」の場合）
					sysinToEnginesAtGoPonder(command, usiEngineList);
					logger.info("「go ponder」の場合 END");
				}

				// ponder時のstopの場合
				else if (StateInfo.getInstance().isPondering() && "stop".equals(command)) {
					logger.info("ponder時のstopの場合 START");
					// 自分の手番
					StateInfo.getInstance().setMyTurn(true);
					// stop時のponder終了処理
					endPonderAtStop(systemInputThread, systemOutputThread, usiEngineList);
					logger.info("ponder時のstopの場合 END");
				}

				// ponder時のponderhitの場合
				else if (StateInfo.getInstance().isPondering() && "ponderhit".equals(command)) {
					logger.info("ponder時のponderhitの場合 START");
					// 自分の手番
					StateInfo.getInstance().setMyTurn(true);
					// 全エンジンにコマンド送信
					ShogiUtils.sendCommandToAllEngines(usiEngineList, command);
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
				if (executeGougi(systemOutputThread, usiEngineList, GougiConfig.getInstance().getGougiType())) {
					// 相手番へ更新
					StateInfo.getInstance().setMyTurn(false);
					// 全エンジンのbestmove、直近の読み筋をクリア
					ShogiUtils.clearBestmoveLatestPv(usiEngineList);
					// 全エンジンの【最善手の交換前の値】bestmove、直近の読み筋をクリア
					ShogiUtils.clear_before_exchange_BestmoveLatestPv(usiEngineList);
				}
			}

			// 各エンジンからのコマンドを標準出力（GUI側）へのコマンドリストに追加する（通常の場合）
			enginesToSysoutAtNormal(systemOutputThread, usiEngineList);

			// sleep
			Thread.sleep(10);
		}
	}

	/* (non-Javadoc)
	 * 標準入力（GUI側）からのコマンドを各エンジンへのコマンドリストに追加する（「go」の場合）
	 * 
	 * @see co.jp.shogi.gougi.UsiLogicCommon#sysinToEnginesAtGo(java.lang.String, java.util.List) */
	protected void sysinToEnginesAtGo(String goCommand, List<UsiEngine> usiEngineList) {
		// 直近の「position」コマンドの局面を取得
		String position = StateInfo.getInstance().getLatestPosition();
		// 直近の「go（ponderではない）」コマンドの局面にセット
		StateInfo.getInstance().setLatestGoNotPonderPosition(position);

		for (UsiEngine engine : usiEngineList) {
			// 各エンジンへのコマンドリストに追加
			// ・スレッドの排他制御
			synchronized (engine.getOutputThread()) {
				engine.getOutputThread().getCommandList().add(position);

				// 持ち時間を0.45倍する
				// engine.getOutputThread().getCommandList().add(goCommand);
				engine.getOutputThread().getCommandList().add("go " + StateInfo.getInstance().getGoTimeOption(0.45));
			}
		}
	}

	/**
	 * 標準入力（GUI側）からのコマンドを各エンジンへのコマンドリストに追加する（「go ponder」の場合）
	 * 
	 * @param goCommand
	 * @param usiEngineList
	 */
	protected void sysinToEnginesAtGoPonder(String goCommand, List<UsiEngine> usiEngineList) {
		// 直近の「position」コマンドの局面を取得
		String position = StateInfo.getInstance().getLatestPosition();
		// 直近の「go ponder」コマンドの局面にセット
		StateInfo.getInstance().setLatestGoPonderPosition(position);

		for (UsiEngine engine : usiEngineList) {
			// 各エンジンへのコマンドリストに追加
			// ・スレッドの排他制御
			synchronized (engine.getOutputThread()) {
				engine.getOutputThread().getCommandList().add(position);

				// 持ち時間を0.45倍する
				// engine.getOutputThread().getCommandList().add(goCommand);
				engine.getOutputThread().getCommandList().add("go ponder " + StateInfo.getInstance().getGoTimeOption(0.45));
			}
		}
	}

	/**
	 * stop時のponder終了処理
	 * 
	 * @param systemInputThread
	 * @param systemOutputThread
	 * @param usiEngineList
	 */
	private void endPonderAtStop(InputStreamThread systemInputThread, OutputStreamThread systemOutputThread, List<UsiEngine> usiEngineList) {
		boolean firstFlg = true;
		String bestmoveCommand = "bestmove";

		// 各エンジンをループ
		for (UsiEngine engine : usiEngineList) {
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
		}

		// GUIに「bestmove」を1回だけ返す
		// ・スレッドの排他制御
		synchronized (systemOutputThread) {
			systemOutputThread.getCommandList().add(bestmoveCommand);
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
						// 「bestmove」の場合、この段階ではGUIへ返さず、エンジンに保存しておく。（3つ揃ってから合議結果をGUIへ返すので）
						// ・ただし、思考が終わったことがGUIから見えた方がよさそうなので、「info string」で返しておく。
						if (command.startsWith("bestmove")) {
							// bestmoveをエンジンに保存
							engine.setBestmoveCommand(command);
							// （例）「info string bestmove 7g7f [７六(77)] [評価値 123] [Gikou 20160606]」
							systemOutputThread.getCommandList().add("info string " + engine.getBestmoveScoreDisp(false, false));
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
		// bestmoveが返ってきていないエンジンが存在する場合
		if (ShogiUtils.containsEmptyBestmove(usiEngineList)) {
			return false;
		}

		// ----- 以下、全エンジンからbestmoveが返ってきた場合

		// 最善手の交換前の場合
		if (StateInfo.getInstance().isBefore_exchange_flg()) {
			// 合議タイプ「各々の最善手を交換して評価値の合計で判定（2者）」の場合、エンジンは必ず2個
			UsiEngine engine1 = usiEngineList.get(0);
			UsiEngine engine2 = usiEngineList.get(1);

			String bestmove1 = engine1.getBestmove();
			String bestmove2 = engine2.getBestmove();

			logger.info("bestmove1=" + bestmove1);
			logger.info("bestmove2=" + bestmove2);

			// この段階で一致した場合、合議完了
			// ・いずれかが投了の場合も合議完了とする
			if (bestmove1.equals(bestmove2) || "resign".equals(bestmove1) || "resign".equals(bestmove2)) {
				logger.info("最善手の交換前だが、お互いの最善手が一致したのでbestmove等を標準出力（GUI側）へ送信する");

				// 評価値の大きな方を合議結果のエンジンとする
				// ・どちらを選んでも指し手は同じだが、ponderは異なることがある
				UsiEngine resultEngine = (engine1.getLatestScore() > engine2.getLatestScore()) ? engine1 : engine2;

				// bestmove等を標準出力（GUI側）へ送信（最善手の交換前の場合）
				sendBestMoveBeforeExchange(systemOutputThread, usiEngineList, resultEngine);

				// 合議完了
				return true;
			}

			// 全エンジンについて、bestmoveと直近の読み筋を【最善手の交換前の値】へ保存した後、クリア
			for (UsiEngine engine : usiEngineList) {
				engine.save_before_exchange_Value();
				engine.clearBestmoveLatestPv();
			}

			// 直近の「go（ponderではない）」コマンドの局面
			// （例）「position startpos moves 1g1f 4a3b 6i7h」
			String pos = StateInfo.getInstance().getLatestGoNotPonderPosition();

			// 最善手の交換（お互いの最善手を付加）
			String pos1 = ShogiUtils.appendMove(pos, bestmove2);
			String pos2 = ShogiUtils.appendMove(pos, bestmove1);

			// 全エンジンについて、「go」コマンドをエンジンへ送信
			for (UsiEngine engine : usiEngineList) {
				String sendPosition = (engine == engine1) ? pos1 : pos2;
				logger.info("お互いの最善手を交換して「go」コマンドをエンジンへ送信する。[" + engine.getEngineDisp() + "]" + sendPosition);

				// ・スレッドの排他制御
				synchronized (engine.getOutputThread()) {
					engine.getOutputThread().getCommandList().add(sendPosition);

					// 持ち時間を0.45倍する
					// ・時間オプションの先後を入れ替える
					engine.getOutputThread().getCommandList().add("go " + ShogiUtils.reverseGoTimeOption(StateInfo.getInstance().getGoTimeOption(0.45)));
				}
			}

			// フラグを「最善手の交換前ではない」に更新
			StateInfo.getInstance().setBefore_exchange_flg(false);

			// この段階ではfalseを返す
			return false;
		}

		// 最善手の交換後の場合
		else {
			// 合議実行
			GougiLogic gougiLogic = new GougiLogic(gougiType, usiEngineList);
			UsiEngine resultEngine = gougiLogic.execute();

			// bestmove等を標準出力（GUI側）へ送信（最善手の交換後の場合）
			sendBestMoveAfterExchange(systemOutputThread, usiEngineList, resultEngine);

			// 合議完了
			return true;
		}
	}

	/**
	 * bestmove等を標準出力（GUI側）へ送信（最善手の交換前の場合）
	 * 
	 * @param systemOutputThread
	 * @param usiEngineList
	 * @param resultEngine
	 */
	private void sendBestMoveBeforeExchange(OutputStreamThread systemOutputThread, List<UsiEngine> usiEngineList, UsiEngine resultEngine) {
		// 合議結果を標準出力（GUI側）へのコマンドリストに追加
		// ・スレッドの排他制御
		synchronized (systemOutputThread) {
			// 合議で指し手が採用されたエンジンのうち、エンジン番号が最小のエンジンの読み筋（評価値を含む）を再度出力しておく
			// ・将棋所等では、bestmove直前の読み筋（「info～score～pv～」）が評価値・読み筋として表示されるため、指し手が採用されなかったエンジンが直近だと指し手と読み筋が矛盾するので。
			if (!Utils.isEmpty(resultEngine.getLatestPv())) {
				systemOutputThread.getCommandList().add(resultEngine.getLatestPv());
			}

			// 合議結果のbestmoveコマンドを取得（ponder部分を除く）
			String bestmoveCommand = resultEngine.getBestmoveCommandExceptPonder();

			// 各エンジンのbestmoveを参考情報としてGUIへ出力する
			// ・GUIで見やすいようにエンジンは逆順にしておく（エンジン1が上に表示されるように）
			for (int i = usiEngineList.size() - 1; i >= 0; i--) {
				UsiEngine engine = usiEngineList.get(i);

				// （例）「bestmove 7g7f」
				String engineBest = engine.getBestmoveCommandExceptPonder();
				// このエンジンの指し手が採用されたか否か。丸とバツだが、「O」と「X」で代替する。
				String hantei = engineBest.equals(bestmoveCommand) ? "O" : "X";

				// （例）「info string [O] bestmove 7g7f [７六(77)] [評価値 123] [Gikou 20160606]」
				// →「info string [O] bestmove 7g7f ponder 3c3d [７六(77) ３四(33)] [評価値 123] [Gikou 20160606]」
				// systemOutputThread.getCommandList().add("info string [" + hantei + "] " + engine.getBestmoveScoreDisp(false, false));
				systemOutputThread.getCommandList().add("info string [" + hantei + "] " + engine.getBestmoveScoreDisp(true, false));
			}

			// bestmoveをGUIへ返す
			// ・ponder部分を除く場合と除かない場合
			// systemOutputThread.getCommandList().add(bestmoveCommand);
			systemOutputThread.getCommandList().add(resultEngine.getBestmoveCommand());
		}

		// GUIに返し終わるまで待つ
		systemOutputThread.waitUntilEmpty(10 * 1000);
	}

	/**
	 * bestmove等を標準出力（GUI側）へ送信（最善手の交換後の場合）
	 * 
	 * @param systemOutputThread
	 * @param usiEngineList
	 * @param resultEngine
	 */
	private void sendBestMoveAfterExchange(OutputStreamThread systemOutputThread, List<UsiEngine> usiEngineList, UsiEngine resultEngine) {
		// 合議結果を標準出力（GUI側）へのコマンドリストに追加
		// ・スレッドの排他制御
		synchronized (systemOutputThread) {
			// 合議で指し手が採用されたエンジンのうち、エンジン番号が最小のエンジンの読み筋（評価値を含む）を再度出力しておく
			// ・将棋所等では、bestmove直前の読み筋（「info～score～pv～」）が評価値・読み筋として表示されるため、指し手が採用されなかったエンジンが直近だと指し手と読み筋が矛盾するので。
			// ・【最善手の交換前の値】を使用する
			if (!Utils.isEmpty(resultEngine.getBefore_exchange_latestPv())) {
				systemOutputThread.getCommandList().add(resultEngine.getBefore_exchange_latestPv());
			}

			// 合議タイプ「各々の最善手を交換して評価値の合計で判定（2者）」の場合、エンジンは必ず2個
			UsiEngine engine1 = usiEngineList.get(0);
			UsiEngine engine2 = usiEngineList.get(1);

			// 交換前の最善手
			String bf_bestmove1 = engine1.get_before_exchange_Bestmove();
			String bf_bestmove2 = engine2.get_before_exchange_Bestmove();

			// 合議結果の最善手、bestmoveコマンド
			// String result_bestmove = (engine1 == resultEngine) ? bf_bestmove1 : bf_bestmove2;
			String result_bestmoveCommand = (engine1 == resultEngine) ? engine1.getBefore_exchange_bestmoveCommand() : engine2.getBefore_exchange_bestmoveCommand();

			// 交換前の評価値
			int bf_score1 = engine1.get_before_exchange_LatestScore();
			int bf_score2 = engine2.get_before_exchange_LatestScore();

			// 交換後の評価値
			// ・エンジンを逆にして、-1倍する
			int af_score1 = -engine2.getLatestScore();
			int af_score2 = -engine1.getLatestScore();

			// 評価値の平均値
			int avg_score1 = (bf_score1 + af_score1) / 2;
			int avg_score2 = (bf_score2 + af_score2) / 2;

			// 判定
			String hantei1 = (engine1 == resultEngine) ? "O" : "X";
			String hantei2 = (engine2 == resultEngine) ? "O" : "X";

			// 各エンジンのbestmoveを参考情報としてGUIへ出力する
			// ・GUIで見やすいようにエンジンは逆順にしておく（交換前のエンジン1の最善手が上に表示されるように）
			// （例）「info string [O] bestmove 7g7f [７六(77)] [評価値 150 （100, 200）]」
			// ・末尾の括弧内は （エンジン1の評価値, エンジン2の評価値）
			// ・そのため、下記の最後の2つの引数は意図的にbfとafを逆にしている
			systemOutputThread.getCommandList().add(getBestmoveScoreDisp(bf_bestmove2, hantei2, avg_score2, af_score2, bf_score2));
			systemOutputThread.getCommandList().add(getBestmoveScoreDisp(bf_bestmove1, hantei1, avg_score1, bf_score1, af_score1));

			// bestmoveをGUIへ返す
			// ・ponder部分を除く場合と除かない場合
			// systemOutputThread.getCommandList().add("bestmove " + result_bestmove);
			systemOutputThread.getCommandList().add(result_bestmoveCommand);
		}

		// GUIに返し終わるまで待つ
		systemOutputThread.waitUntilEmpty(10 * 1000);
	}

	/**
	 * 参考情報としてGUIへ出力する文字列を返す
	 * （例）「info string [O] bestmove 7g7f [７六(77)] [評価値 150 （100, 200）]」
	 * 
	 * @param bf_bestmove
	 * @param hantei
	 * @param avg_score
	 * @param score1
	 * @param score2
	 * @return
	 */
	private String getBestmoveScoreDisp(String bf_bestmove, String hantei, int avg_score, int score1, int score2) {
		StringBuilder sb = new StringBuilder();

		sb.append("info string [");
		sb.append(hantei);
		sb.append("] bestmove ");
		sb.append(bf_bestmove);
		sb.append(" [");
		sb.append(ShogiUtils.getMoveDispJa(bf_bestmove));
		sb.append("] [評価値 ");
		sb.append(avg_score);
		sb.append(" （");
		sb.append(score1);
		sb.append(", ");
		sb.append(score2);
		sb.append("）]");

		return sb.toString();
	}

}
