package co.jp.shogi.gougi;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * USI共通ロジック
 */
public abstract class UsiLogicCommon {

	/** Logger */
	protected static Logger logger = Logger.getLogger(UsiLogicCommon.class.getName());

	/**
	 * 標準入力（GUI側）からのコマンドを各エンジンへのコマンドリストに追加する
	 * 
	 * @param systemInputThread
	 * @param usiEngineList
	 * @param pendingSysinCommandList
	 * @return
	 */
	protected List<String> sysinToEngines(InputStreamThread systemInputThread, List<UsiEngine> usiEngineList, List<String> pendingSysinCommandList) {
		List<String> systemInputCommandList1 = null;

		// 標準入力（GUI側）からの入力用スレッドのコマンドリストが空ではない場合、ローカル変数にコピーしてからクリア
		// ・スレッドの排他制御
		synchronized (systemInputThread) {
			if (!systemInputThread.getCommandList().isEmpty()) {
				systemInputCommandList1 = new ArrayList<String>(systemInputThread.getCommandList());
				systemInputThread.getCommandList().clear();
			}
		}

		// 各エンジンへのコマンドリスト
		List<String> systemInputCommandList2 = new ArrayList<String>();

		// コマンドが存在する場合
		if (systemInputCommandList1 != null) {
			for (String command : systemInputCommandList1) {
				// positionコマンドの場合、StateInfoにセット
				// ・systemInputCommandList2には追加しない。
				if (command.startsWith("position ")) {
					StateInfo.getInstance().setLatestPosition(command);
					logger.info("StateInfo.getInstance().isSente()=" + StateInfo.getInstance().isSente());
					// 全エンジンの読み筋リストをクリア
					ShogiUtils.clearPvList(usiEngineList);
					continue;
				}

				// 「go」コマンドまたは「go ponder」コマンドの場合、pendingSysinCommandListに追加
				// ・「go」コマンドは「go」だけではなく「go btime 0 wtime 0 byoyomi 3000」などの場合があるので注意。
				// ・「go ponder」コマンドも「go ponder」だけではなく「go ponder btime 0 wtime 0 byoyomi 3000」などの場合があるので注意。
				// ・systemInputCommandList2には追加しない。
				else if (command.startsWith("go")) {
					// StateInfoのlatestSysinGoCommandにセット（時間計測用）
					StateInfo.getInstance().setLatestSysinGoCommand(command);
					pendingSysinCommandList.add(command);

					// 「go ponder」の直後に「ponderhit」や「stop」が来ることがあるので、この時点でponder実施中フラグをセットしておく
					if (command.startsWith("go ponder")) {
						// ponder実施中フラグをセット
						StateInfo.getInstance().setPondering(true);
					}

					continue;
				}

				// ponder時のstopまたはponderhitの場合、pendingSysinCommandListに追加
				// ・systemInputCommandList2には追加しない。
				else if (StateInfo.getInstance().isPondering() && ("stop".equals(command) || "ponderhit".equals(command))) {
					pendingSysinCommandList.add(command);
					continue;
				}

				// その他の場合、systemInputCommandList2へ追加
				else {
					systemInputCommandList2.add(command);
				}
			}
		}

		for (UsiEngine engine : usiEngineList) {
			// 各エンジンへのコマンドリストに追加
			// ・スレッドの排他制御
			synchronized (engine.getOutputThread()) {
				for (String command : systemInputCommandList2) {
					// 標準入力（GUI側）からのコマンドを編集（setoption対応）
					String cmd = editSysinCommand(command, engine);
					if (!Utils.isEmpty(cmd)) {
						engine.getOutputThread().getCommandList().add(cmd);
					}
				}
			}
		}

		return systemInputCommandList2;
	}

	/**
	 * 標準入力（GUI側）からのコマンドを編集（setoption対応）
	 * ・通常はcommandをそのまま返す。
	 * ・setoptionの場合、編集後のコマンドを返す。（nullを返すこともある）
	 * 
	 * @param command
	 * @param engine
	 * @return
	 */
	private String editSysinCommand(String command, UsiEngine engine) {
		// 「setoption」の場合
		if (command.startsWith("setoption ")) {
			return editSysinCommandAtSetOption(command, engine);
		}

		// その他の場合、commandをそのまま返す
		return command;
	}

	/**
	 * 標準入力（GUI側）からのコマンドを編集（setoption対応）
	 * ・前提：引数commandがsetoptionコマンドであること。
	 * ・setoptionコマンドで、例えば「E2_OwnBook」を「OwnBook」へ戻す（「option name」のときに「OwnBook」を「E2_OwnBook」に変換したので）
	 * （例）「setoption name E2_OwnBook value true」
	 * →「setoption name OwnBook value true」（エンジン2の場合のみ）
	 * 
	 * @param command
	 * @param engine
	 * @return
	 */
	private String editSysinCommandAtSetOption(String command, UsiEngine engine) {
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
			// 通常はありえないケースだが、commandをそのまま返しておく
			return command;
		}

		// （例）「USI_Hash」「USI_Ponder」
		if (option.startsWith("USI")) {
			// 「USI_Ponder」の場合、エンジンに値をセット
			if ("USI_Ponder".equals(option)) {
				setEnginePonderOnOff(engine, command);
			}

			// 詰探索エンジンの場合、「USI_Hash」を最大256MBに制限しておく
			if (engine instanceof MateEngine && "USI_Hash".equals(option)) {
				// （例）「setoption name USI_Hash value 1024」→「1024」
				int usi_hash = Utils.getIntValue(Utils.getSplitResult(command, " ", 4), 0);
				logger.info("usi_hash=" + usi_hash);
				if (usi_hash > 256) {
					return "setoption name USI_Hash value 256";
				}
			}

			// commandをそのまま返す
			return command;
		}

		// （例）「E2_」で始まる場合
		if (option.startsWith(engine.getOptionNamePrefix())) {
			// （例）「E2_OwnBook」→「OwnBook」
			String str = option.substring(engine.getOptionNamePrefix().length());

			// 当該エンジンのUSIオプションに含まれる場合（念のためチェック）
			if (engine.getOptionSet().contains(str)) {
				// 「USI_Ponder」の場合、エンジンに値をセット
				if ("USI_Ponder".equals(str)) {
					setEnginePonderOnOff(engine, command);
				}

				String prefix = "setoption name " + engine.getOptionNamePrefix();
				// （例）「setoption name OwnBook value true」
				return "setoption name " + command.substring(prefix.length()).trim();
			}
		}

		// 合議タイプ「数手ごとに対局者交代」対応
		if ("G_ChangePlayerPlys".equals(option)) {
			// （例）「setoption name G_ChangePlayerPlys value 3」→「3」
			int changePlayerPlys = Utils.getIntValue(Utils.getSplitResult(command, " ", 4), 1);
			logger.info("changePlayerPlys=" + changePlayerPlys);
			StateInfo.getInstance().setChangePlayerPlys(changePlayerPlys);
			return null;
		}

		// 合議タイプ「序盤・中盤・終盤で対局者交代」対応
		// ・中盤の対局者に交代する手数
		if ("G_ChuubanStartPlys".equals(option)) {
			// （例）「setoption name G_ChuubanStartPlys value 51」→「51」
			int chuubanStartPlys = Utils.getIntValue(Utils.getSplitResult(command, " ", 4), 51);
			logger.info("chuubanStartPlys=" + chuubanStartPlys);
			StateInfo.getInstance().setChuubanStartPlys(chuubanStartPlys);
			return null;
		}
		// ・終盤の対局者に交代する手数
		if ("G_ShuubanStartPlys".equals(option)) {
			// （例）「setoption name G_ShuubanStartPlys value 101」→「101」
			int shuubanStartPlys = Utils.getIntValue(Utils.getSplitResult(command, " ", 4), 101);
			logger.info("shuubanStartPlys=" + shuubanStartPlys);
			StateInfo.getInstance().setShuubanStartPlys(shuubanStartPlys);
			return null;
		}

		// 合議タイプ「2手前の評価値から一定値以上下降したら対局者交代」「2手前の評価値から一定値以上上昇したら対局者交代」対応
		if ("G_ChangePlayerScoreDiff".equals(option)) {
			// （例）「setoption name G_ChangePlayerScoreDiff value 200」→「200」
			int changePlayerScoreDiff = Utils.getIntValue(Utils.getSplitResult(command, " ", 4), 100);
			logger.info("G_ChangePlayerScoreDiff=" + changePlayerScoreDiff);
			StateInfo.getInstance().setChangePlayerScoreDiff(changePlayerScoreDiff);
			return null;
		}

		// 合議タイプ「詰探索エンジンとの合議」対応
		if ("G_MateTimeout".equals(option)) {
			// （例）「setoption name G_MateTimeout value 30000」→「30000」
			int mateTimeout = Utils.getIntValue(Utils.getSplitResult(command, " ", 4), 10000);
			logger.info("G_MateTimeout=" + mateTimeout);
			StateInfo.getInstance().setMateTimeout(mateTimeout);
			return null;
		}

		// 合議タイプ「詰探索エンジンとの合議（読み筋の局面も詰探索） 」対応
		if ("G_PvMateTimeout".equals(option)) {
			// （例）「setoption name G_PvMateTimeout value 30000」→「30000」
			int pvMateTimeout = Utils.getIntValue(Utils.getSplitResult(command, " ", 4), 5000);
			logger.info("G_PvMateTimeout=" + pvMateTimeout);
			StateInfo.getInstance().setPvMateTimeout(pvMateTimeout);
			return null;
		}

		// 合議タイプ「詰探索エンジンとの合議（読み筋の局面も詰探索） 」対応
		if ("G_MateInfoCount".equals(option)) {
			// （例）「setoption name G_MateInfoCount value 10」→「10」
			int mateInfoCount = Utils.getIntValue(Utils.getSplitResult(command, " ", 4), 10);
			logger.info("G_MateInfoCount=" + mateInfoCount);
			StateInfo.getInstance().setMateInfoCount(mateInfoCount);
			return null;
		}

		// 合議タイプ「詰探索エンジンとの合議（読み筋の局面も詰探索） 」対応
		if ("G_MateInfoInterval".equals(option)) {
			// （例）「setoption name G_MateInfoInterval value 1000」→「1000」
			int mateInfoInterval = Utils.getIntValue(Utils.getSplitResult(command, " ", 4), 1000);
			logger.info("G_MateInfoInterval=" + mateInfoInterval);
			StateInfo.getInstance().setMateInfoInterval(mateInfoInterval);
			return null;
		}

		// （例）「E2_」で始まるオプションは、エンジン1とエンジン3の場合はnullを返す（エンジン2用のオプションのはずなので）
		return null;
	}

	/**
	 * エンジンのponderのon/offをセット
	 * ・前提：当該エンジンのUSI_Ponderのsetoptionコマンドであること
	 * 
	 * @param engine
	 * @param command
	 */
	private void setEnginePonderOnOff(UsiEngine engine, String command) {
		try {
			// （例）「setoption name USI_Ponder value true」→「true」
			// （例）「setoption name E2_USI_Ponder value true」→「true」
			String val = Utils.getSplitResult(command, " ", 4);
			engine.setPonderOnOff("true".equals(val));

			logger.info("[" + engine.getEngineDisp() + "]command=" + command);
			logger.info("[" + engine.getEngineDisp() + "]engine.isPonderOnOff()=" + engine.isPonderOnOff());
		} catch (Exception e) {
			// 何もしない
		}
	}

	/**
	 * 標準入力（GUI側）からのコマンドを各エンジンへのコマンドリストに追加する（「go」の場合）
	 * 
	 * @param goCommand
	 * @param usiEngineList
	 */
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
				engine.getOutputThread().getCommandList().add(goCommand);
			}
		}
	}

	/**
	 * 各エンジンからのコマンドを標準出力（GUI側）へのコマンドリストに追加する（「usi」の場合）
	 * 
	 * @param systemOutputThread
	 * @param usiEngineList
	 */
	protected void enginesToSysoutAtUsi(OutputStreamThread systemOutputThread, List<UsiEngine> usiEngineList) {
		// 「id name～」と「id author～」は各エンジンからではなく、このプログラム独自で返す
		// ・スレッドの排他制御
		synchronized (systemOutputThread) {
			systemOutputThread.getCommandList().add("id name " + Constants.USI_ID_NAME);
			systemOutputThread.getCommandList().add("id author " + Constants.USI_ID_AUTHOR);
		}

		// GUIに返し終わるまで待つ
		systemOutputThread.waitUntilEmpty(10 * 1000);

		// 各エンジンをループ
		for (UsiEngine engine : usiEngineList) {
			InputStreamThread processInputThread = engine.getInputThread();

			// エンジンから「usiok」が返ってくるまで待つ
			processInputThread.waitUntilCommand("usiok", 60 * 1000);

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
					// エンジンの「USIオプションの送信済みコマンドリスト」に追加
					engine.getOptionCommandList().add(cmd);
				}
			}

			// 標準出力（GUI側）へのコマンドリストに追加
			// ・スレッドの排他制御
			synchronized (systemOutputThread) {
				systemOutputThread.getCommandList().addAll(new ArrayList<String>(systemOutputCommandList));
			}

			// GUIに返し終わるまで待つ
			systemOutputThread.waitUntilEmpty(10 * 1000);
		}

		// 各エンジンからのコマンドを標準出力（GUI側）へのコマンドリストに追加する（「usi」の場合）2
		enginesToSysoutAtUsi2(systemOutputThread, usiEngineList);

		// GUIに「usiok」を1回だけ返す
		// ・スレッドの排他制御
		synchronized (systemOutputThread) {
			systemOutputThread.getCommandList().add("usiok");
		}

		// GUIに返し終わるまで待つ
		systemOutputThread.waitUntilEmpty(10 * 1000);
	}

	/**
	 * 各エンジンからのコマンドを標準出力（GUI側）へのコマンドリストに追加する（「usi」の場合）2
	 * 
	 * @param systemOutputThread
	 * @param usiEngineList
	 */
	abstract protected void enginesToSysoutAtUsi2(OutputStreamThread systemOutputThread, List<UsiEngine> usiEngineList);

	/**
	 * 各エンジンからのコマンドを標準出力（GUI側）へのコマンドリストに追加する（「isready」の場合）
	 * 
	 * @param systemOutputThread
	 * @param usiEngineList
	 */
	protected void enginesToSysoutAtIsReady(OutputStreamThread systemOutputThread, List<UsiEngine> usiEngineList) {
		// 各エンジンをループ
		for (UsiEngine engine : usiEngineList) {
			InputStreamThread processInputThread = engine.getInputThread();

			// 「readyok」が返ってくるまで待つ
			processInputThread.waitUntilCommand("readyok", 60 * 1000);
			// 「readyok」を削除
			// ・スレッドの排他制御
			synchronized (processInputThread) {
				processInputThread.getCommandList().remove("readyok");
			}

			// ついでにここでクリア処理
			engine.clear();
		}

		// すべてのエンジンから「readyok」が返ってきたら、GUIに「readyok」を1回だけ返す
		// ・スレッドの排他制御
		synchronized (systemOutputThread) {
			systemOutputThread.getCommandList().add("readyok");
		}

		// GUIに返し終わるまで待つ
		systemOutputThread.waitUntilEmpty(10 * 1000);
	}

	/**
	 * 今回の合議タイプを算出
	 * 
	 * @param prevGougiType
	 * @return
	 */
	protected String calcCurrentGougiType(String prevGougiType) {
		// 設定値が「楽観合議と悲観合議を交互」の場合
		if (Constants.GOUGI_TYPE_RAKKAN_HIKAN_BYTURNS.equals(GougiConfig.getInstance().getGougiType())) {
			if (Utils.isEmpty(prevGougiType)) {
				return Constants.GOUGI_TYPE_RAKKAN;
			} else if (Constants.GOUGI_TYPE_RAKKAN.equals(prevGougiType)) {
				return Constants.GOUGI_TYPE_HIKAN;
			} else {
				return Constants.GOUGI_TYPE_RAKKAN;
			}
		}
		// その他の場合
		else {
			// 設定値をそのまま返す
			return GougiConfig.getInstance().getGougiType();
		}
	}

}
