package co.jp.shogi.gougi;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * USI���W�b�N3
 */
public class UsiLogic3 extends UsiLogicCommon {

	/** Logger */
	protected static Logger logger = Logger.getLogger(UsiLogic3.class.getName());

	/**
	 * execute���\�b�h
	 * 
	 * @param usiEngineList
	 * @param systemInputThread
	 * @param systemOutputThread
	 * @throws Exception
	 */
	public void execute(List<UsiEngine> usiEngineList, InputStreamThread systemInputThread, OutputStreamThread systemOutputThread) throws Exception {
		// ���݂̃G���W��
		UsiEngine currentEngine = usiEngineList.get(0);
		// ���݂̃G���W���̎w���萔
		int currentEnginePlys = 0;

		while (true) {
			// �ۗ����̕W�����́iGUI���j����̃R�}���h���X�g
			// �Ego�R�}���h�Ago ponder�R�}���h�Aponder����stop�R�}���h�Aponderhit�R�}���h�͈�U���̃��X�g�ɂ��߂�
			List<String> pendingSysinCommandList = new ArrayList<String>();
			// �W�����́iGUI���j����̃R�}���h���e�G���W���ւ̃R�}���h���X�g�ɒǉ�����
			List<String> systemInputCommandList = sysinToEngines(systemInputThread, usiEngineList, pendingSysinCommandList);

			// �ۗ����̕W�����́iGUI���j����̃R�}���h���X�g�����[�v
			for (String command : pendingSysinCommandList) {
				logger.info("�ۗ����̕W�����́iGUI���j����̃R�}���h���X�g�̃R�}���h=" + command);

				// �ugo�v�R�}���h�܂��́ugo ponder�v�R�}���h�̏ꍇ
				if (command.startsWith("go")) {
					logger.info("�ugo�v�܂��́ugo ponder�v�̏ꍇ START");
					// �����̎��
					StateInfo.getInstance().setMyTurn(true);
					// �S�G���W����bestmove�A���߂̓ǂ݋؂��N���A
					ShogiUtils.clearBestmoveLatestPv(usiEngineList);
					// �W�����́iGUI���j����̃R�}���h�����݂̃G���W���ւ̃R�}���h���X�g�ɒǉ�����i�ugo�v�܂��́ugo ponder�v�̏ꍇ�j
					sysinToEnginesAtGoOrGoPonder(command, currentEngine);
					logger.info("�ugo�v�܂��́ugo ponder�v�̏ꍇ END");
				}

				// ponder����stop�̏ꍇ
				else if (StateInfo.getInstance().isPondering() && "stop".equals(command)) {
					logger.info("ponder����stop�̏ꍇ START");
					StateInfo.getInstance().setPondering(false);
					// �����̎��
					StateInfo.getInstance().setMyTurn(true);
					// stop����ponder�I������
					endPonderAtStop(systemInputThread, systemOutputThread, currentEngine);
					logger.info("ponder����stop�̏ꍇ END");
				}

				// ponder����ponderhit�̏ꍇ
				else if (StateInfo.getInstance().isPondering() && "ponderhit".equals(command)) {
					logger.info("ponder����ponderhit�̏ꍇ START");
					StateInfo.getInstance().setPondering(false);
					// �����̎��
					StateInfo.getInstance().setMyTurn(true);
					// ���݂̃G���W���ɃR�}���h�����̂܂ܑ��M
					// �E�X���b�h�̔r������
					synchronized (currentEngine.getOutputThread()) {
						currentEngine.getOutputThread().getCommandList().add(command);
					}
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

			// ���c�����͕s�v

			// ���݂̃G���W������̃R�}���h��W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�����i�ʏ�̏ꍇ�j
			// �Ebestmove��Ԃ����ꍇ�Atrue���Ԃ�
			if (enginesToSysoutAtNormal(systemOutputThread, currentEngine)) {
				// ���݂̃G���W���̎w���萔���C���N�������g
				currentEnginePlys++;
				// ���̃G���W����I��
				UsiEngine nextEngine = selectNextEngine(usiEngineList, StateInfo.getInstance().getChangePlayerPlys(), currentEngine, currentEnginePlys);

				// �΋ǎ҂���サ���ꍇ
				if (nextEngine != currentEngine) {
					logger.info("�΋ǎҌ��F[" + currentEngine.getUsiName() + "]��[" + nextEngine.getUsiName() + "]");
					currentEnginePlys = 0;
					currentEngine = nextEngine;
				}

				// ����Ԃ֍X�V
				StateInfo.getInstance().setMyTurn(false);
				// �S�G���W����bestmove�A���߂̓ǂ݋؂��N���A
				ShogiUtils.clearBestmoveLatestPv(usiEngineList);
			}

			// sleep
			Thread.sleep(10);
		}
	}

	/* (non-Javadoc)
	 * �e�G���W������̃R�}���h��W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�����i�uusi�v�̏ꍇ�j2
	 * 
	 * @see co.jp.shogi.gougi.UsiLogicCommon#enginesToSysoutAtUsi2(co.jp.shogi.gougi.OutputStreamThread, java.util.List) */
	@Override
	protected void enginesToSysoutAtUsi2(OutputStreamThread systemOutputThread, List<UsiEngine> usiEngineList) {
		// �Ǝ��I�v�V������ǉ�
		// �E�X���b�h�̔r������
		synchronized (systemOutputThread) {
			// �΋ǎ҂���シ��萔
			systemOutputThread.getCommandList().add("option name G_ChangePlayerPlys type spin default 1 min 1 max 1000");
		}
	}

	/**
	 * stop����ponder�I������
	 * 
	 * @param systemInputThread
	 * @param systemOutputThread
	 * @param engine
	 */
	private void endPonderAtStop(InputStreamThread systemInputThread, OutputStreamThread systemOutputThread, UsiEngine engine) {
		logger.info("GUI����stop��M���A���Y�G���W���Ɂustop�v�R�}���h�𑗐M[" + engine.getUsiName() + "]");

		// ���Y�G���W���Ɂustop�v�R�}���h�𑗐M
		// �E�X���b�h�̔r������
		synchronized (engine.getOutputThread()) {
			engine.getOutputThread().getCommandList().add("stop");
		}

		// ���Y�G���W������ubestmove�v���Ԃ��Ă���܂ő҂�
		engine.getInputThread().waitUntilCommandStartsWith("bestmove", 10 * 1000);
		logger.info("GUI����stop��M���A���Y�G���W���Ɂustop�v�R�}���h�𑗐M������A�ubestmove�v��M[" + engine.getUsiName() + "]");

		String bestmoveCommand = "bestmove";

		// �ubestmove�v���擾������A�폜����
		// �E�X���b�h�̔r������
		synchronized (engine.getInputThread()) {
			bestmoveCommand = Utils.getItemStartsWith(engine.getInputThread().getCommandList(), "bestmove");
			Utils.removeFromListWhereStartsWith(engine.getInputThread().getCommandList(), "bestmove");
			logger.info("GUI����stop��M���A���Y�G���W���Ɂustop�v�R�}���h�𑗐M������A�ubestmove�v��M�������A�R�}���h���X�g����폜[" + engine.getUsiName() + "]");
		}

		// GUI�Ɂubestmove�v��1�񂾂��Ԃ�
		// �E�X���b�h�̔r������
		synchronized (systemOutputThread) {
			systemOutputThread.getCommandList().add(bestmoveCommand);
		}
	}

	/**
	 * ���̃G���W����I������
	 * 
	 * @param usiEngineList
	 * @param changePlayerPlys
	 * @param currentEngine
	 * @param currentEnginePlys
	 * @return
	 */
	private UsiEngine selectNextEngine(List<UsiEngine> usiEngineList, int changePlayerPlys, UsiEngine currentEngine, int currentEnginePlys) {
		// ���݂̃G���W���̎w���萔���u�΋ǎ҂���シ��萔�v�ɒB���Ȃ��ꍇ
		if (currentEnginePlys < changePlayerPlys) {
			return currentEngine;
		}

		// ���݂̃G���W���̎w���萔���u�΋ǎ҂���シ��萔�v�ɒB�����ꍇ
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
	 * �W�����́iGUI���j����̃R�}���h�𓖊Y�G���W���ւ̃R�}���h���X�g�ɒǉ�����i�ugo�v�܂��́ugo ponder�v�̏ꍇ�j
	 * 
	 * @param goCommand
	 * @param usiEngineList
	 */
	protected void sysinToEnginesAtGoOrGoPonder(String goCommand, UsiEngine engine) {
		// ���߂́uposition�v�R�}���h�̋ǖʂ��擾
		String position = StateInfo.getInstance().getLatestPosition();

		// ���Y�G���W���ւ̃R�}���h���X�g�ɒǉ�
		// �E�X���b�h�̔r������
		synchronized (engine.getOutputThread()) {
			engine.getOutputThread().getCommandList().add(position);
			engine.getOutputThread().getCommandList().add(goCommand);
		}
	}

	/**
	 * ���Y�G���W������̃R�}���h��W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�����i�ʏ�̏ꍇ�j
	 * 
	 * @param systemOutputThread
	 * @param engine
	 * @return
	 */
	private boolean enginesToSysoutAtNormal(OutputStreamThread systemOutputThread, UsiEngine engine) {
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

		// bestmove��Ԃ������ۂ�
		boolean bestmoveFlg = false;

		// �R�}���h�����݂���ꍇ
		if (processInputCommandList != null) {
			// �W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�
			// �E�X���b�h�̔r������
			synchronized (systemOutputThread) {
				for (String command : processInputCommandList) {
					// �ubestmove�v�̏ꍇ
					if (command.startsWith("bestmove")) {
						// bestmove���G���W���ɕۑ�
						engine.setBestmoveCommand(command);
						// �i��j�ubestmove 7g7f ponder 3c3d [�V�Z(77) �R�l(33)] [�]���l 123] [Gikou 20160606]�v
						systemOutputThread.getCommandList().add("info string " + engine.getBestmoveScoreDisp(true, false));
						// bestmove�t���O���Z�b�g
						bestmoveFlg = true;
					}

					// �uinfo�`score�`�v�̏ꍇ�A���߂̓ǂ݋؂��G���W���ɕۑ�
					else if (command.startsWith("info ") && command.indexOf("score ") >= 0) {
						engine.setLatestPv(command);
					}

					// �W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�
					systemOutputThread.getCommandList().add(command);
				}
			}
		}

		return bestmoveFlg;
	}

}
