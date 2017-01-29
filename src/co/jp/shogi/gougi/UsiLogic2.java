package co.jp.shogi.gougi;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * USI���W�b�N2
 */
public class UsiLogic2 extends UsiLogicCommon {

	/** Logger */
	protected static Logger logger = Logger.getLogger(UsiLogic2.class.getName());

	/**
	 * execute���\�b�h
	 * 
	 * @param usiEngineList
	 * @param systemInputThread
	 * @param systemOutputThread
	 * @throws Exception
	 */
	public void execute(List<UsiEngine> usiEngineList, InputStreamThread systemInputThread, OutputStreamThread systemOutputThread) throws Exception {

		while (true) {
			// �ۗ����̕W�����́iGUI���j����̃R�}���h���X�g
			// �Ego�R�}���h�Ago ponder�R�}���h�Aponder����stop�R�}���h�Aponderhit�R�}���h�͈�U���̃��X�g�ɂ��߂�
			List<String> pendingSysinCommandList = new ArrayList<String>();

			// �W�����́iGUI���j����̃R�}���h���e�G���W���ւ̃R�}���h���X�g�ɒǉ�����
			List<String> systemInputCommandList = sysinToEngines(systemInputThread, usiEngineList, pendingSysinCommandList);

			// �ۗ����̕W�����́iGUI���j����̃R�}���h���X�g�����[�v
			for (String command : pendingSysinCommandList) {
				logger.info("�ۗ����̕W�����́iGUI���j����̃R�}���h���X�g�̃R�}���h=" + command);

				// �ugo�v�R�}���h�̏ꍇ
				if (command.startsWith("go") && !command.startsWith("go ponder")) {
					logger.info("�ugo�v�̏ꍇ START");
					// �����̎��
					StateInfo.getInstance().setMyTurn(true);
					// �őP��̌����O
					StateInfo.getInstance().setBefore_exchange_flg(true);
					// �S�G���W����bestmove�A���߂̓ǂ݋؂��N���A
					ShogiUtils.clearBestmoveLatestPv(usiEngineList);
					// �S�G���W���́y�őP��̌����O�̒l�zbestmove�A���߂̓ǂ݋؂��N���A
					ShogiUtils.clear_before_exchange_BestmoveLatestPv(usiEngineList);
					// �W�����́iGUI���j����̃R�}���h���e�G���W���ւ̃R�}���h���X�g�ɒǉ�����i�ugo�v�̏ꍇ�j
					sysinToEnginesAtGo(command, usiEngineList);
					logger.info("�ugo�v�̏ꍇ END");
				}

				// �ugo ponder�v�R�}���h�̏ꍇ
				else if (command.startsWith("go ponder")) {
					logger.info("�ugo ponder�v�̏ꍇ START");
					// ����̎��
					StateInfo.getInstance().setMyTurn(false);
					// �őP��̌����O
					StateInfo.getInstance().setBefore_exchange_flg(true);
					// �S�G���W����bestmove�A���߂̓ǂ݋؂��N���A
					ShogiUtils.clearBestmoveLatestPv(usiEngineList);
					// �S�G���W���́y�őP��̌����O�̒l�zbestmove�A���߂̓ǂ݋؂��N���A
					ShogiUtils.clear_before_exchange_BestmoveLatestPv(usiEngineList);
					// ponder���{���t���O���Z�b�g
					StateInfo.getInstance().setPondering(true);
					// �W�����́iGUI���j����̃R�}���h���e�G���W���ւ̃R�}���h���X�g�ɒǉ�����i�ugo ponder�v�̏ꍇ�j
					sysinToEnginesAtGoPonder(command, usiEngineList);
					logger.info("�ugo ponder�v�̏ꍇ END");
				}

				// ponder����stop�̏ꍇ
				else if (StateInfo.getInstance().isPondering() && "stop".equals(command)) {
					logger.info("ponder����stop�̏ꍇ START");
					// �����̎��
					StateInfo.getInstance().setMyTurn(true);
					// stop����ponder�I������
					endPonderAtStop(systemInputThread, systemOutputThread, usiEngineList);
					logger.info("ponder����stop�̏ꍇ END");
				}

				// ponder����ponderhit�̏ꍇ
				else if (StateInfo.getInstance().isPondering() && "ponderhit".equals(command)) {
					logger.info("ponder����ponderhit�̏ꍇ START");
					// �����̎��
					StateInfo.getInstance().setMyTurn(true);
					// �S�G���W���ɃR�}���h���M
					ShogiUtils.sendCommandToAllEngines(usiEngineList, command);
					logger.info("ponder����ponderhit�̏ꍇ END");
				}
			}

			// �W�����́iGUI���j����̃R�}���h�Ɂuquit�v���܂܂��ꍇ
			if (Utils.contains(systemInputCommandList, "quit")) {
				// ���[�v�𔲂���
				break;
			}

			// �W�����́iGUI���j����̃R�}���h�Ɂuusi�v���܂܂��ꍇ
			if (Utils.contains(systemInputCommandList, "usi")) {
				logger.info("�uusi�v�̏ꍇ START");
				// �e�G���W������̃R�}���h��W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�����i�uusi�v�̏ꍇ�j
				enginesToSysoutAtUsi(systemOutputThread, usiEngineList);
				logger.info("�uusi�v�̏ꍇ END");
			}

			// �W�����́iGUI���j����̃R�}���h�Ɂuisready�v���܂܂��ꍇ
			if (Utils.contains(systemInputCommandList, "isready")) {
				logger.info("�uisready�v�̏ꍇ START");
				// �N���A����
				StateInfo.getInstance().clear();
				// �e�G���W������̃R�}���h��W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�����i�uisready�v�̏ꍇ�j
				enginesToSysoutAtIsReady(systemOutputThread, usiEngineList);
				logger.info("�uisready�v�̏ꍇ END");
			}

			// �W�����́iGUI���j����̃R�}���h�Ɂuusinewgame�v���܂܂��ꍇ
			if (Utils.contains(systemInputCommandList, "usinewgame")) {
				logger.info("�uusinewgame�v�̏ꍇ START");
				StateInfo.getInstance().setDuringGame(true);
				logger.info("�uusinewgame�v�̏ꍇ END");
			}

			// �W�����́iGUI���j����̃R�}���h�Ɂugameover�v���܂܂��ꍇ
			if (Utils.containsStartsWith(systemInputCommandList, "gameover")) {
				logger.info("�ugameover�v�̏ꍇ START");
				StateInfo.getInstance().setDuringGame(false);
				logger.info("�ugameover�v�̏ꍇ END");
			}

			// �����̎�Ԃ̏ꍇ
			// �E�������A�΋ǒ��Ɍ���
			if (StateInfo.getInstance().isMyTurn() && StateInfo.getInstance().isDuringGame()) {
				// ���c�����s
				if (executeGougi(systemOutputThread, usiEngineList, GougiConfig.getInstance().getGougiType())) {
					// ����Ԃ֍X�V
					StateInfo.getInstance().setMyTurn(false);
					// �S�G���W����bestmove�A���߂̓ǂ݋؂��N���A
					ShogiUtils.clearBestmoveLatestPv(usiEngineList);
					// �S�G���W���́y�őP��̌����O�̒l�zbestmove�A���߂̓ǂ݋؂��N���A
					ShogiUtils.clear_before_exchange_BestmoveLatestPv(usiEngineList);
				}
			}

			// �e�G���W������̃R�}���h��W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�����i�ʏ�̏ꍇ�j
			enginesToSysoutAtNormal(systemOutputThread, usiEngineList);

			// sleep
			Thread.sleep(10);
		}
	}

	/* (non-Javadoc)
	 * �W�����́iGUI���j����̃R�}���h���e�G���W���ւ̃R�}���h���X�g�ɒǉ�����i�ugo�v�̏ꍇ�j
	 * 
	 * @see co.jp.shogi.gougi.UsiLogicCommon#sysinToEnginesAtGo(java.lang.String, java.util.List) */
	protected void sysinToEnginesAtGo(String goCommand, List<UsiEngine> usiEngineList) {
		// ���߂́uposition�v�R�}���h�̋ǖʂ��擾
		String position = StateInfo.getInstance().getLatestPosition();
		// ���߂́ugo�iponder�ł͂Ȃ��j�v�R�}���h�̋ǖʂɃZ�b�g
		StateInfo.getInstance().setLatestGoNotPonderPosition(position);

		for (UsiEngine engine : usiEngineList) {
			// �e�G���W���ւ̃R�}���h���X�g�ɒǉ�
			// �E�X���b�h�̔r������
			synchronized (engine.getOutputThread()) {
				engine.getOutputThread().getCommandList().add(position);

				// �������Ԃ�0.45�{����
				// engine.getOutputThread().getCommandList().add(goCommand);
				engine.getOutputThread().getCommandList().add("go " + StateInfo.getInstance().getGoTimeOption(0.45));
			}
		}
	}

	/**
	 * �W�����́iGUI���j����̃R�}���h���e�G���W���ւ̃R�}���h���X�g�ɒǉ�����i�ugo ponder�v�̏ꍇ�j
	 * 
	 * @param goCommand
	 * @param usiEngineList
	 */
	protected void sysinToEnginesAtGoPonder(String goCommand, List<UsiEngine> usiEngineList) {
		// ���߂́uposition�v�R�}���h�̋ǖʂ��擾
		String position = StateInfo.getInstance().getLatestPosition();
		// ���߂́ugo ponder�v�R�}���h�̋ǖʂɃZ�b�g
		StateInfo.getInstance().setLatestGoPonderPosition(position);

		for (UsiEngine engine : usiEngineList) {
			// �e�G���W���ւ̃R�}���h���X�g�ɒǉ�
			// �E�X���b�h�̔r������
			synchronized (engine.getOutputThread()) {
				engine.getOutputThread().getCommandList().add(position);

				// �������Ԃ�0.45�{����
				// engine.getOutputThread().getCommandList().add(goCommand);
				engine.getOutputThread().getCommandList().add("go ponder " + StateInfo.getInstance().getGoTimeOption(0.45));
			}
		}
	}

	/**
	 * stop����ponder�I������
	 * 
	 * @param systemInputThread
	 * @param systemOutputThread
	 * @param usiEngineList
	 */
	private void endPonderAtStop(InputStreamThread systemInputThread, OutputStreamThread systemOutputThread, List<UsiEngine> usiEngineList) {
		boolean firstFlg = true;
		String bestmoveCommand = "bestmove";

		// �e�G���W�������[�v
		for (UsiEngine engine : usiEngineList) {
			logger.info("GUI����stop��M���A���Y�G���W���Ɂustop�v�R�}���h�𑗐M[" + engine.getUsiName() + "]");

			// ���Y�G���W���Ɂustop�v�R�}���h�𑗐M
			// �E�X���b�h�̔r������
			synchronized (engine.getOutputThread()) {
				engine.getOutputThread().getCommandList().add("stop");
			}

			// ���Y�G���W������ubestmove�v���Ԃ��Ă���܂ő҂�
			engine.getInputThread().waitUntilCommandStartsWith("bestmove", 10 * 1000);
			logger.info("GUI����stop��M���A���Y�G���W���Ɂustop�v�R�}���h�𑗐M������A�ubestmove�v��M[" + engine.getUsiName() + "]");

			// 1���ڂ̏ꍇ
			if (firstFlg) {
				firstFlg = false;

				// �ubestmove�v���擾
				// �E�X���b�h�̔r������
				synchronized (engine.getInputThread()) {
					bestmoveCommand = Utils.getItemStartsWith(engine.getInputThread().getCommandList(), "bestmove");
				}
			}

			// �ubestmove�v���폜
			// �E�X���b�h�̔r������
			synchronized (engine.getInputThread()) {
				Utils.removeFromListWhereStartsWith(engine.getInputThread().getCommandList(), "bestmove");
				logger.info("GUI����stop��M���A���Y�G���W���Ɂustop�v�R�}���h�𑗐M������A�ubestmove�v��M�������A�R�}���h���X�g����폜[" + engine.getUsiName() + "]");
			}
		}

		// GUI�Ɂubestmove�v��1�񂾂��Ԃ�
		// �E�X���b�h�̔r������
		synchronized (systemOutputThread) {
			systemOutputThread.getCommandList().add(bestmoveCommand);
		}
	}

	/**
	 * �e�G���W������̃R�}���h��W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�����i�ʏ�̏ꍇ�j
	 * 
	 * @param systemOutputThread
	 * @param usiEngineList
	 */
	private void enginesToSysoutAtNormal(OutputStreamThread systemOutputThread, List<UsiEngine> usiEngineList) {
		// �e�G���W�������[�v
		for (UsiEngine engine : usiEngineList) {
			InputStreamThread processInputThread = engine.getInputThread();
			List<String> processInputCommandList = null;

			// �G���W������̃R�}���h���X�g����ł͂Ȃ��ꍇ�A���[�J���ϐ��ɃR�s�[���Ă���N���A
			// �E�X���b�h�̔r������
			synchronized (processInputThread) {
				if (!processInputThread.getCommandList().isEmpty()) {
					processInputCommandList = new ArrayList<String>(processInputThread.getCommandList());
					processInputThread.getCommandList().clear();
				}
			}

			// �R�}���h�����݂���ꍇ
			if (processInputCommandList != null) {
				// �W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�
				// �E�X���b�h�̔r������
				synchronized (systemOutputThread) {
					for (String command : processInputCommandList) {
						// �ubestmove�v�̏ꍇ�A���̒i�K�ł�GUI�֕Ԃ����A�G���W���ɕۑ����Ă����B�i3�����Ă��獇�c���ʂ�GUI�֕Ԃ��̂Łj
						// �E�������A�v�l���I��������Ƃ�GUI���猩���������悳�����Ȃ̂ŁA�uinfo string�v�ŕԂ��Ă����B
						if (command.startsWith("bestmove")) {
							// bestmove���G���W���ɕۑ�
							engine.setBestmoveCommand(command);
							// �i��j�uinfo string bestmove 7g7f [�V�Z(77)] [�]���l 123] [Gikou 20160606]�v
							systemOutputThread.getCommandList().add("info string " + engine.getBestmoveScoreDisp(false, false));
						}

						// ���̑��̏ꍇ�A�W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�
						else {
							systemOutputThread.getCommandList().add(command);

							// �uinfo�`score�`�v�̏ꍇ�A���߂̓ǂ݋؂��G���W���ɕۑ�
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
	 * ���c�����s
	 * 
	 * @param systemOutputThread
	 * @param usiEngineList
	 * @param gougiType
	 * @return
	 */
	private boolean executeGougi(OutputStreamThread systemOutputThread, List<UsiEngine> usiEngineList, String gougiType) {
		// bestmove���Ԃ��Ă��Ă��Ȃ��G���W�������݂���ꍇ
		if (ShogiUtils.containsEmptyBestmove(usiEngineList)) {
			return false;
		}

		// ----- �ȉ��A�S�G���W������bestmove���Ԃ��Ă����ꍇ

		// �őP��̌����O�̏ꍇ
		if (StateInfo.getInstance().isBefore_exchange_flg()) {
			// ���c�^�C�v�u�e�X�̍őP����������ĕ]���l�̍��v�Ŕ���i2�ҁj�v�̏ꍇ�A�G���W���͕K��2��
			UsiEngine engine1 = usiEngineList.get(0);
			UsiEngine engine2 = usiEngineList.get(1);

			String bestmove1 = engine1.getBestmove();
			String bestmove2 = engine2.getBestmove();

			logger.info("bestmove1=" + bestmove1);
			logger.info("bestmove2=" + bestmove2);

			// ���̒i�K�ň�v�����ꍇ�A���c����
			// �E�����ꂩ�������̏ꍇ�����c�����Ƃ���
			if (bestmove1.equals(bestmove2) || "resign".equals(bestmove1) || "resign".equals(bestmove2)) {
				logger.info("�őP��̌����O�����A���݂��̍őP�肪��v�����̂�bestmove����W���o�́iGUI���j�֑��M����");

				// �]���l�̑傫�ȕ������c���ʂ̃G���W���Ƃ���
				// �E�ǂ����I��ł��w����͓��������Aponder�͈قȂ邱�Ƃ�����
				UsiEngine resultEngine = (engine1.getLatestScore() > engine2.getLatestScore()) ? engine1 : engine2;

				// bestmove����W���o�́iGUI���j�֑��M�i�őP��̌����O�̏ꍇ�j
				sendBestMoveBeforeExchange(systemOutputThread, usiEngineList, resultEngine);

				// ���c����
				return true;
			}

			// �S�G���W���ɂ��āAbestmove�ƒ��߂̓ǂ݋؂��y�őP��̌����O�̒l�z�֕ۑ�������A�N���A
			for (UsiEngine engine : usiEngineList) {
				engine.save_before_exchange_Value();
				engine.clearBestmoveLatestPv();
			}

			// ���߂́ugo�iponder�ł͂Ȃ��j�v�R�}���h�̋ǖ�
			// �i��j�uposition startpos moves 1g1f 4a3b 6i7h�v
			String pos = StateInfo.getInstance().getLatestGoNotPonderPosition();

			// �őP��̌����i���݂��̍őP���t���j
			String pos1 = ShogiUtils.appendMove(pos, bestmove2);
			String pos2 = ShogiUtils.appendMove(pos, bestmove1);

			// �S�G���W���ɂ��āA�ugo�v�R�}���h���G���W���֑��M
			for (UsiEngine engine : usiEngineList) {
				String sendPosition = (engine == engine1) ? pos1 : pos2;
				logger.info("���݂��̍őP����������āugo�v�R�}���h���G���W���֑��M����B[" + engine.getEngineDisp() + "]" + sendPosition);

				// �E�X���b�h�̔r������
				synchronized (engine.getOutputThread()) {
					engine.getOutputThread().getCommandList().add(sendPosition);

					// �������Ԃ�0.45�{����
					// �E���ԃI�v�V�����̐������ւ���
					engine.getOutputThread().getCommandList().add("go " + ShogiUtils.reverseGoTimeOption(StateInfo.getInstance().getGoTimeOption(0.45)));
				}
			}

			// �t���O���u�őP��̌����O�ł͂Ȃ��v�ɍX�V
			StateInfo.getInstance().setBefore_exchange_flg(false);

			// ���̒i�K�ł�false��Ԃ�
			return false;
		}

		// �őP��̌�����̏ꍇ
		else {
			// ���c���s
			GougiLogic gougiLogic = new GougiLogic(gougiType, usiEngineList);
			UsiEngine resultEngine = gougiLogic.execute();

			// bestmove����W���o�́iGUI���j�֑��M�i�őP��̌�����̏ꍇ�j
			sendBestMoveAfterExchange(systemOutputThread, usiEngineList, resultEngine);

			// ���c����
			return true;
		}
	}

	/**
	 * bestmove����W���o�́iGUI���j�֑��M�i�őP��̌����O�̏ꍇ�j
	 * 
	 * @param systemOutputThread
	 * @param usiEngineList
	 * @param resultEngine
	 */
	private void sendBestMoveBeforeExchange(OutputStreamThread systemOutputThread, List<UsiEngine> usiEngineList, UsiEngine resultEngine) {
		// ���c���ʂ�W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�
		// �E�X���b�h�̔r������
		synchronized (systemOutputThread) {
			// ���c�Ŏw���肪�̗p���ꂽ�G���W���̂����A�G���W���ԍ����ŏ��̃G���W���̓ǂ݋؁i�]���l���܂ށj���ēx�o�͂��Ă���
			// �E���������ł́Abestmove���O�̓ǂ݋؁i�uinfo�`score�`pv�`�v�j���]���l�E�ǂ݋؂Ƃ��ĕ\������邽�߁A�w���肪�̗p����Ȃ������G���W�������߂��Ǝw����Ɠǂ݋؂���������̂ŁB
			if (!Utils.isEmpty(resultEngine.getLatestPv())) {
				systemOutputThread.getCommandList().add(resultEngine.getLatestPv());
			}

			// ���c���ʂ�bestmove�R�}���h���擾�iponder�����������j
			String bestmoveCommand = resultEngine.getBestmoveCommandExceptPonder();

			// �e�G���W����bestmove���Q�l���Ƃ���GUI�֏o�͂���
			// �EGUI�Ō��₷���悤�ɃG���W���͋t���ɂ��Ă����i�G���W��1����ɕ\�������悤�Ɂj
			for (int i = usiEngineList.size() - 1; i >= 0; i--) {
				UsiEngine engine = usiEngineList.get(i);

				// �i��j�ubestmove 7g7f�v
				String engineBest = engine.getBestmoveCommandExceptPonder();
				// ���̃G���W���̎w���肪�̗p���ꂽ���ۂ��B�ۂƃo�c�����A�uO�v�ƁuX�v�ő�ւ���B
				String hantei = engineBest.equals(bestmoveCommand) ? "O" : "X";

				// �i��j�uinfo string [O] bestmove 7g7f [�V�Z(77)] [�]���l 123] [Gikou 20160606]�v
				// ���uinfo string [O] bestmove 7g7f ponder 3c3d [�V�Z(77) �R�l(33)] [�]���l 123] [Gikou 20160606]�v
				// systemOutputThread.getCommandList().add("info string [" + hantei + "] " + engine.getBestmoveScoreDisp(false, false));
				systemOutputThread.getCommandList().add("info string [" + hantei + "] " + engine.getBestmoveScoreDisp(true, false));
			}

			// bestmove��GUI�֕Ԃ�
			// �Eponder�����������ꍇ�Ə����Ȃ��ꍇ
			// systemOutputThread.getCommandList().add(bestmoveCommand);
			systemOutputThread.getCommandList().add(resultEngine.getBestmoveCommand());
		}

		// GUI�ɕԂ��I���܂ő҂�
		systemOutputThread.waitUntilEmpty(10 * 1000);
	}

	/**
	 * bestmove����W���o�́iGUI���j�֑��M�i�őP��̌�����̏ꍇ�j
	 * 
	 * @param systemOutputThread
	 * @param usiEngineList
	 * @param resultEngine
	 */
	private void sendBestMoveAfterExchange(OutputStreamThread systemOutputThread, List<UsiEngine> usiEngineList, UsiEngine resultEngine) {
		// ���c���ʂ�W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�
		// �E�X���b�h�̔r������
		synchronized (systemOutputThread) {
			// ���c�Ŏw���肪�̗p���ꂽ�G���W���̂����A�G���W���ԍ����ŏ��̃G���W���̓ǂ݋؁i�]���l���܂ށj���ēx�o�͂��Ă���
			// �E���������ł́Abestmove���O�̓ǂ݋؁i�uinfo�`score�`pv�`�v�j���]���l�E�ǂ݋؂Ƃ��ĕ\������邽�߁A�w���肪�̗p����Ȃ������G���W�������߂��Ǝw����Ɠǂ݋؂���������̂ŁB
			// �E�y�őP��̌����O�̒l�z���g�p����
			if (!Utils.isEmpty(resultEngine.getBefore_exchange_latestPv())) {
				systemOutputThread.getCommandList().add(resultEngine.getBefore_exchange_latestPv());
			}

			// ���c�^�C�v�u�e�X�̍őP����������ĕ]���l�̍��v�Ŕ���i2�ҁj�v�̏ꍇ�A�G���W���͕K��2��
			UsiEngine engine1 = usiEngineList.get(0);
			UsiEngine engine2 = usiEngineList.get(1);

			// �����O�̍őP��
			String bf_bestmove1 = engine1.get_before_exchange_Bestmove();
			String bf_bestmove2 = engine2.get_before_exchange_Bestmove();

			// ���c���ʂ̍őP��Abestmove�R�}���h
			// String result_bestmove = (engine1 == resultEngine) ? bf_bestmove1 : bf_bestmove2;
			String result_bestmoveCommand = (engine1 == resultEngine) ? engine1.getBefore_exchange_bestmoveCommand() : engine2.getBefore_exchange_bestmoveCommand();

			// �����O�̕]���l
			int bf_score1 = engine1.get_before_exchange_LatestScore();
			int bf_score2 = engine2.get_before_exchange_LatestScore();

			// ������̕]���l
			// �E�G���W�����t�ɂ��āA-1�{����
			int af_score1 = -engine2.getLatestScore();
			int af_score2 = -engine1.getLatestScore();

			// �]���l�̕��ϒl
			int avg_score1 = (bf_score1 + af_score1) / 2;
			int avg_score2 = (bf_score2 + af_score2) / 2;

			// ����
			String hantei1 = (engine1 == resultEngine) ? "O" : "X";
			String hantei2 = (engine2 == resultEngine) ? "O" : "X";

			// �e�G���W����bestmove���Q�l���Ƃ���GUI�֏o�͂���
			// �EGUI�Ō��₷���悤�ɃG���W���͋t���ɂ��Ă����i�����O�̃G���W��1�̍őP�肪��ɕ\�������悤�Ɂj
			// �i��j�uinfo string [O] bestmove 7g7f [�V�Z(77)] [�]���l 150 �i100, 200�j]�v
			// �E�����̊��ʓ��� �i�G���W��1�̕]���l, �G���W��2�̕]���l�j
			// �E���̂��߁A���L�̍Ō��2�̈����͈Ӑ}�I��bf��af���t�ɂ��Ă���
			systemOutputThread.getCommandList().add(getBestmoveScoreDisp(bf_bestmove2, hantei2, avg_score2, af_score2, bf_score2));
			systemOutputThread.getCommandList().add(getBestmoveScoreDisp(bf_bestmove1, hantei1, avg_score1, bf_score1, af_score1));

			// bestmove��GUI�֕Ԃ�
			// �Eponder�����������ꍇ�Ə����Ȃ��ꍇ
			// systemOutputThread.getCommandList().add("bestmove " + result_bestmove);
			systemOutputThread.getCommandList().add(result_bestmoveCommand);
		}

		// GUI�ɕԂ��I���܂ő҂�
		systemOutputThread.waitUntilEmpty(10 * 1000);
	}

	/**
	 * �Q�l���Ƃ���GUI�֏o�͂��镶�����Ԃ�
	 * �i��j�uinfo string [O] bestmove 7g7f [�V�Z(77)] [�]���l 150 �i100, 200�j]�v
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
		sb.append("] [�]���l ");
		sb.append(avg_score);
		sb.append(" �i");
		sb.append(score1);
		sb.append(", ");
		sb.append(score2);
		sb.append("�j]");

		return sb.toString();
	}

}
