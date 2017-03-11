package co.jp.shogi.gougi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * USI���W�b�N4
 * �E���c�^�C�v���u�l�T���G���W���Ƃ̍��c�v�̏ꍇ�Ɏg��
 * �E�u�Ȃ̂͋l�߁v�͋l�T������stop�R�}���h�𑗂��Ă��v�l�𑱂���悤�Ȃ̂ŁA�^�C���A�E�g��ݒ肷�邱�Ƃɂ���
 */
public class UsiLogic4 extends UsiLogicCommon {

	/** Logger */
	protected static Logger logger = Logger.getLogger(UsiLogic4.class.getName());

	/**
	 * execute���\�b�h
	 * 
	 * @param usiEngineList
	 * @param systemInputThread
	 * @param systemOutputThread
	 * @throws Exception
	 */
	public void execute(List<UsiEngine> usiEngineList, InputStreamThread systemInputThread, OutputStreamThread systemOutputThread) throws Exception {
		// ���݂̃G���W���i�ʏ�j
		// �E���c�^�C�v���u�l�T���G���W���Ƃ̍��c�v�̏ꍇ�A�G���W�����X�g��1���݂̂̂͂�
		UsiEngine currentEngine = usiEngineList.get(0);

		// �l�T���G���W��
		logger.info("�l�T���G���W���̋N��");
		MateEngine mateEngine = GougiConfig.getInstance().getMateEngine();
		mateEngine.createProcess();

		// �l�T���G���W�����G���W�����X�g�ɒǉ�����
		usiEngineList.add(mateEngine);

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
					// �W�����́iGUI���j����̃R�}���h���l�T���G���W���ւ̃R�}���h���X�g�ɒǉ�����i�ugo�v�܂��́ugo ponder�v�̏ꍇ�j
					sysinToMateEngineAtGoOrGoPonder(command, mateEngine);
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

			// �����̎�Ԃ̏ꍇ
			// �E�������A�΋ǒ��Ɍ���
			if (StateInfo.getInstance().isMyTurn() && StateInfo.getInstance().isDuringGame()) {
				// ���c�����s
				if (executeGougi(systemOutputThread, currentEngine, mateEngine)) {
					// ����Ԃ֍X�V
					StateInfo.getInstance().setMyTurn(false);
					// �S�G���W����bestmove�A���߂̓ǂ݋؂��N���A
					// �E�y�O��̒l�zbestmove�A���߂̓ǂ݋؂̓N���A���Ȃ�
					ShogiUtils.clearBestmoveLatestPv(usiEngineList);
				}
			}

			// �l�T���G���W������̃R�}���h��W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�����
			mateEnginesToSysout(systemOutputThread, mateEngine);
			// ���݂̃G���W������̃R�}���h��W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�����i�ʏ�̏ꍇ�j
			enginesToSysoutAtNormal(systemOutputThread, currentEngine);

			// sleep
			Thread.sleep(10);
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
	 * �W�����́iGUI���j����̃R�}���h�𓖊Y�G���W���ւ̃R�}���h���X�g�ɒǉ�����i�ugo�v�܂��́ugo ponder�v�̏ꍇ�j
	 * 
	 * @param goCommand
	 * @param engine
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

		// �ugo ponder�v����uponderhit�v�܂ł̎��Ԃ��Z������ƃG���W�������F���ł�����timeup����ꍇ������悤�Ȃ̂ŁA�����҂�
		try {
			Thread.sleep(20);
		} catch (InterruptedException e) {
			// ignore
		}
	}

	/**
	 * �W�����́iGUI���j����̃R�}���h���l�T���G���W���ւ̃R�}���h���X�g�ɒǉ�����i�ugo�v�܂��́ugo ponder�v�̏ꍇ�j
	 * 
	 * @param goCommand
	 * @param mateEngine
	 * @throws IOException
	 */
	protected void sysinToMateEngineAtGoOrGoPonder(String goCommand, MateEngine mateEngine) throws IOException {
		// �܂��O�ǖʂ�T�����̏ꍇ�A�������Ȃ�
		if (mateEngine.isSearching()) {
			return;
		}

		// ���߂́uposition�v�R�}���h�̋ǖʂ��擾
		String position = StateInfo.getInstance().getLatestPosition();

		// �l�T���G���W����checkmate�̎w������N���A
		mateEngine.setCheckmateMoves(null);
		// �l�T���G���W���ɒ��߂́ugo mate�v�R�}���h�̋ǖʂ��Z�b�g
		mateEngine.setLatestGoMatePosition(position);

		// ���Y�G���W���ւ̃R�}���h���X�g�ɒǉ�
		// �E�X���b�h�̔r������
		synchronized (mateEngine.getOutputThread()) {
			mateEngine.getOutputThread().getCommandList().add(position);

			// �^�C���A�E�g��ݒ肵�āugo mate�v�R�}���h�𑗐M
			// mateEngine.getOutputThread().getCommandList().add("go mate infinite");
			mateEngine.getOutputThread().getCommandList().add("go mate " + StateInfo.getInstance().getMateTimeout());
		}

		// �T�����t���O���Z�b�g
		mateEngine.setSearching(true);
	}

	/**
	 * ���Y�G���W������̃R�}���h��W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�����i�ʏ�̏ꍇ�j
	 * 
	 * @param systemOutputThread
	 * @param engine
	 */
	private void enginesToSysoutAtNormal(OutputStreamThread systemOutputThread, UsiEngine engine) {
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
					// �ubestmove�v�̏ꍇ�A���̒i�K�ł�GUI�֕Ԃ����A�G���W���ɕۑ����Ă����B
					if (command.startsWith("bestmove")) {
						// bestmove���G���W���ɕۑ�
						engine.setBestmoveCommand(command);
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

	/**
	 * �l�T���G���W������̃R�}���h��W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�����
	 * 
	 * @param systemOutputThread
	 * @param mateEngine
	 */
	private void mateEnginesToSysout(OutputStreamThread systemOutputThread, MateEngine mateEngine) {
		InputStreamThread processInputThread = mateEngine.getInputThread();
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
					// �W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�
					systemOutputThread.getCommandList().add("info string " + command);

					// �R�}���h���ucheckmate�v�Ŏn�܂�ꍇ
					if (command.startsWith("checkmate")) {
						// �T�����t���O�𗎂Ƃ�
						mateEngine.setSearching(false);
					}

					// �i��j�ucheckmate R*6a 5a6a 2a4a 6a6b 8b7a�v�̏ꍇ
					// �E�ucheckmate nomate�v�ucheckmate timeout�v�ucheckmate�̂݁v�ucheckmate notimplemented�v�̏ꍇ�͏���
					if (command.startsWith("checkmate ") && !("checkmate nomate".equals(command) || "checkmate timeout".equals(command) || "checkmate".equals(command) || "checkmate notimplemented".equals(command))) {
						logger.info("Utils.equals(mateEngine.getLatestGoMatePosition(), StateInfo.getInstance().getLatestPosition())=" + Utils.equals(mateEngine.getLatestGoMatePosition(), StateInfo.getInstance().getLatestPosition()));

						// ���߂́ugo mate�v�R�}���h�̋ǖʂƒ��߂́uposition�v�R�}���h�̋ǖʂ���v����ꍇ
						// �E���ۂ̋ǖʂ͎��̎w����Ɉڂ��Ă��A�l�T���G���W���͑O�̋ǖʂ��v�l�������Ă���ꍇ������̂ŁB
						if (Utils.equals(mateEngine.getLatestGoMatePosition(), StateInfo.getInstance().getLatestPosition())) {
							// checkmate�̎w������Z�b�g
							mateEngine.setCheckmateMoves(command.substring("checkmate ".length()));

							// �l�T���G���W���̓ǂ݋؁i�]���l���܂ށj���쐬���ďo�͂���
							// �i��j�uinfo score mate 5 pv R*6a 5a6a 2a4a 6a6b 8b7a�v
							systemOutputThread.getCommandList().add(mateEngine.createPv());
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
	 * @param currentEngine
	 * @param mateEngine
	 * @return
	 */
	private boolean executeGougi(OutputStreamThread systemOutputThread, UsiEngine currentEngine, MateEngine mateEngine) {
		// currentEngine����bestmove���Ԃ��Ă��Ă��Ȃ��ꍇ
		if (Utils.isEmpty(currentEngine.getBestmoveCommand())) {
			return false;
		}

		// currentEngine����bestmove���Ԃ��Ă��Ă���ꍇ
		else {
			// ���c���ʂ�W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�
			// �E�X���b�h�̔r������
			synchronized (systemOutputThread) {
				// �l�T���G���W�����l�݂𔭌����Ă��Ȃ��ꍇ
				// �E�ʏ�̒T���G���W���̎w������̗p����
				if (Utils.isEmpty(mateEngine.getCheckmateMoves())) {
					// ���c�Ŏw���肪�̗p���ꂽ�G���W���̓ǂ݋؁i�]���l���܂ށj���ēx�o�͂��Ă���
					// �E���������ł́Abestmove���O�̓ǂ݋؁i�uinfo�`score�`pv�`�v�j���]���l�E�ǂ݋؂Ƃ��ĕ\������邽�߁A�w���肪�̗p����Ȃ������G���W�������߂��Ǝw����Ɠǂ݋؂���������̂ŁB
					// �E�l�T���G���W���Ƃ̍��c�̏ꍇ�͂����čēx�o�͂���K�v�͂Ȃ���������Ȃ����A�ꉞ�o�͂��Ă���
					if (!Utils.isEmpty(currentEngine.getLatestPv())) {
						systemOutputThread.getCommandList().add(currentEngine.getLatestPv());
					}

					// ----- [O][X]�̏o��
					// �E�ʏ�̒T���G���W���̕�����ɂ���
					// �i��j�uinfo string [X] [�l�ݖ���]�@[NanohaTsumeUSI]�v
					systemOutputThread.getCommandList().add("info string [X] " + mateEngine.getMateDisp());
					// �i��j�uinfo string [O] bestmove 7g7f ponder 3c3d [�V�Z(77) �R�l(33)] [�]���l 123 �i�O��100 ����23�j] [Gikou 20160606]�v
					systemOutputThread.getCommandList().add("info string [O] " + currentEngine.getBestmoveScoreDisp(true, true));
					// -----

					// bestmove��GUI�֕Ԃ�
					systemOutputThread.getCommandList().add(currentEngine.getBestmoveCommand());
				}

				// �l�T���G���W�����l�݂𔭌������ꍇ
				// �E�l�T���G���W���̎w������̗p����
				else {
					// �l�T���G���W���̓ǂ݋؁i�]���l���܂ށj���쐬���ďo�͂���
					// �i��j�uinfo score mate 5 pv R*6a 5a6a 2a4a 6a6b 8b7a�v
					// �E���������ł́Abestmove���O�̓ǂ݋؁i�uinfo�`score�`pv�`�v�j���]���l�E�ǂ݋؂Ƃ��ĕ\������邽�߁A�w���肪�̗p����Ȃ������G���W�������߂��Ǝw����Ɠǂ݋؂���������̂ŁB
					systemOutputThread.getCommandList().add(mateEngine.createPv());

					// �l�T���G���W���̎w����
					String mateBestmoveCommand = mateEngine.createBestmoveCommand();

					// ----- [O][X]�̏o��
					// �E�l�T���G���W���̕�����ɂ���

					// currentEngine�̎w���肪�̗p���ꂽ���ۂ��B�ۂƃo�c�����A�uO�v�ƁuX�v�ő�ւ���B
					String hantei = mateBestmoveCommand.equals(currentEngine.getBestmoveCommandExceptPonder()) ? "O" : "X";
					// �i��j�uinfo string [X] bestmove 7g7f ponder 3c3d [�V�Z(77) �R�l(33)] [�]���l 123 �i�O��100 ����23�j] [Gikou 20160606]�v
					systemOutputThread.getCommandList().add("info string [" + hantei + "] " + currentEngine.getBestmoveScoreDisp(true, true));

					// �i��j�uinfo string [O] [5��l��]�@[�U���� �U��(51) �S��(21) �U��(61) �V��(82)]�@[NanohaTsumeUSI]�v
					systemOutputThread.getCommandList().add("info string [O] " + mateEngine.getMateDisp());
					// -----

					// bestmove��GUI�֕Ԃ�
					systemOutputThread.getCommandList().add(mateBestmoveCommand);
				}
			}

			// �y�O��̒l�z��ۑ�
			currentEngine.savePrevValue();
			// GUI�ɕԂ��I���܂ő҂�
			systemOutputThread.waitUntilEmpty(10 * 1000);

			// ���c����
			return true;
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
			// �l�T���G���W���̃^�C���A�E�g�i�~���b�j
			systemOutputThread.getCommandList().add("option name G_MateTimeout type spin default 10000 min 1 max 1000000");
		}
	}

}
