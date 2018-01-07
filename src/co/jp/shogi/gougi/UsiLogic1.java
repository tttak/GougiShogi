package co.jp.shogi.gougi;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * USI���W�b�N1
 * ���c�^�C�v���ȉ��̂����ꂩ�̏ꍇ�Ɏg��
 * �E�u���������c�i3�ҁj�v
 * �E�u�y�ύ��c�v
 * �E�u�ߊύ��c�v
 * �E�u�y�ύ��c�Ɣߊύ��c�����݁v
 * �E�u2��O�̕]���l����̏㏸���̊y�ύ��c�v
 * �E�u2��O�̕]���l����̏㏸���̔ߊύ��c�v
 */
public class UsiLogic1 extends UsiLogicCommon {

	/** Logger */
	protected static Logger logger = Logger.getLogger(UsiLogic1.class.getName());

	/**
	 * execute���\�b�h
	 * 
	 * @param usiEngineList
	 * @param systemInputThread
	 * @param systemOutputThread
	 * @throws Exception
	 */
	public void execute(List<UsiEngine> usiEngineList, InputStreamThread systemInputThread, OutputStreamThread systemOutputThread) throws Exception {
		// �O��̍��c�^�C�v
		String prevGougiType = null;

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
					// �S�G���W����bestmove�A���߂̓ǂ݋؂��N���A
					ShogiUtils.clearBestmoveLatestPv(usiEngineList);
					// �W�����́iGUI���j����̃R�}���h���e�G���W���ւ̃R�}���h���X�g�ɒǉ�����i�ugo�v�̏ꍇ�j
					sysinToEnginesAtGo(command, usiEngineList);
					// �S�G���W���ɂ��āA�u���Oponder���{�����ۂ��v�̃t���O�𗎂Ƃ�
					ShogiUtils.setPrePondering(usiEngineList, false);
					logger.info("�ugo�v�̏ꍇ END");
				}

				// �ugo ponder�v�R�}���h�̏ꍇ
				else if (command.startsWith("go ponder")) {
					logger.info("GUI����ugo ponder�v����M�������A�G���W���ւ͑��M���Ȃ��B���c�������Ɂugo ponder�v���M�ς݂Ȃ̂ŁB");

					StateInfo.getInstance().setLatestGoPonderPosition(StateInfo.getInstance().getLatestPosition());
					StateInfo.getInstance().setPondering(true);
					// �S�G���W���ɂ��āA�u���Oponder���{�����ۂ��v�̃t���O�𗎂Ƃ�
					ShogiUtils.setPrePondering(usiEngineList, false);
				}

				// ponder����stop�܂���ponderhit�̏ꍇ
				else if (StateInfo.getInstance().isPondering() && ("stop".equals(command) || "ponderhit".equals(command))) {
					logger.info("ponder����stop�܂���ponderhit�̏ꍇ START");
					StateInfo.getInstance().setPondering(false);
					// ponder�I�����̏���
					endPonder(command, systemInputThread, systemOutputThread, usiEngineList);
					// �����̎��
					StateInfo.getInstance().setMyTurn(true);
					logger.info("ponder����stop�܂���ponderhit�̏ꍇ END");
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
				// ����̍��c�^�C�v���擾
				String currentGougiType = calcCurrentGougiType(prevGougiType);

				// ���c�����s
				if (executeGougi(systemOutputThread, usiEngineList, currentGougiType)) {
					// �O��̍��c�^�C�v���X�V
					prevGougiType = currentGougiType;
					// ����Ԃ֍X�V
					StateInfo.getInstance().setMyTurn(false);
					// �S�G���W����bestmove�A���߂̓ǂ݋؂��N���A
					ShogiUtils.clearBestmoveLatestPv(usiEngineList);
				}
			}

			// �e�G���W������̃R�}���h��W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�����i�ʏ�̏ꍇ�j
			enginesToSysoutAtNormal(systemOutputThread, usiEngineList);

			// sleep
			Thread.sleep(10);
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
						// ���Oponder���{���̏ꍇ�A�ǂݔ�΂�
						// �E�W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ������ɁA����
						if (engine.isPrePondering()) {
							continue;
						}

						// �ubestmove�v�̏ꍇ�A���̒i�K�ł�GUI�֕Ԃ����A�G���W���ɕۑ����Ă����B�i3�����Ă��獇�c���ʂ�GUI�֕Ԃ��̂Łj
						// �E�������A�v�l���I��������Ƃ�GUI���猩���������悳�����Ȃ̂ŁA�uinfo string�v�ŕԂ��Ă����B
						// TODO
						// else if (StateInfo.getInstance().isMyTurn() && command.startsWith("bestmove")) {
						else if (command.startsWith("bestmove")) {
							// bestmove���G���W���ɕۑ�
							engine.setBestmoveCommand(command);
							// �i��j�uinfo string bestmove 7g7f [�V�Z(77)] [�]���l 123 �i�O��100 ����23�j] [Gikou 20160606]�v
							// [ponder�Ή�]
							// �i��j�uinfo string bestmove 7g7f ponder 3c3d [�V�Z(77) �R�l(33)] [�]���l 123 �i�O��100 ����23�j] [Gikou 20160606]�v
							systemOutputThread.getCommandList().add("info string " + engine.getBestmoveScoreDisp(true, true));

							// ���Y�G���W����pondering�t���O�𗎂Ƃ�
							// �E�O�̎肩���ponder��bestmove�������ꍇ���l��
							engine.setPondering(false);

							// [ponder�Ή�]
							// ���Y�G���W����ponder�ݒ�on�ŁA���A�ubestmove�v���uponder�v�t���������ꍇ
							if (engine.isPonderOnOff() && !Utils.isEmpty(engine.getPonderMove())) {
								// bestmove���Ԃ��Ă��Ă��Ȃ��G���W�������݂���ꍇ
								// �E�������A�΋ǒ��Ɍ���
								if (ShogiUtils.containsEmptyBestmove(usiEngineList) && StateInfo.getInstance().isDuringGame()) {
									// �G���W���ւ̃R�}���h���X�g�ɒǉ�
									// �E�X���b�h�̔r������
									synchronized (engine.getOutputThread()) {
										// �ugo ponder�v�R�}���h���G���W���֑��M
										// �i��j�uposition startpos moves 1g1f 4a3b 6i7h�v
										String pos = StateInfo.getInstance().getLatestGoNotPonderPosition();
										pos = ShogiUtils.appendMove(pos, engine.getBestmove());
										pos = ShogiUtils.appendMove(pos, engine.getPonderMove());

										engine.getOutputThread().getCommandList().add(pos);
										engine.getOutputThread().getCommandList().add("go ponder " + StateInfo.getInstance().getGoTimeOption());

										// ponder�����G���W���ɃZ�b�g
										engine.setPondering(true);
										engine.setPonderingPosition(pos);
										engine.setPrePondering(true);
									}
								}
							}
						}

						// ���̑��̏ꍇ�A�W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�
						else {
							systemOutputThread.getCommandList().add(command);

							// �uinfo�`score�`�v�̏ꍇ�A���߂̓ǂ݋؂��G���W���ɕۑ�
							// �E�������Aponder���{��������
							// �� ���Oponder���{���������B�i���ɏ��if�ŏ��O�ς݁j
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
	 * ���c�����s
	 * 
	 * @param systemOutputThread
	 * @param usiEngineList
	 * @param gougiType
	 * @return
	 */
	private boolean executeGougi(OutputStreamThread systemOutputThread, List<UsiEngine> usiEngineList, String gougiType) {
		// ���c���s
		GougiLogic gougiLogic = new GougiLogic(gougiType, usiEngineList);
		UsiEngine resultEngine = gougiLogic.execute();

		// ���c���s�����̏ꍇ
		// �Ebestmove���Ԃ��Ă��Ă��Ȃ��G���W�������݂���ꍇ�Ȃ�
		if (resultEngine == null) {
			return false;
		}

		// ���c���ʂ�bestmove�R�}���h���擾�iponder�����������j
		// �E���c���̂͂���ŏI��
		String bestmoveCommand = resultEngine.getBestmoveCommandExceptPonder();

		if (!Utils.isEmpty(bestmoveCommand)) {
			// ���c���ʂ�W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�
			// �E�X���b�h�̔r������
			synchronized (systemOutputThread) {
				// ���c�Ŏw���肪�̗p���ꂽ�G���W���̂����A�G���W���ԍ����ŏ��̃G���W���̓ǂ݋؁i�]���l���܂ށj���ēx�o�͂��Ă���
				// �E���������ł́Abestmove���O�̓ǂ݋؁i�uinfo�`score�`pv�`�v�j���]���l�E�ǂ݋؂Ƃ��ĕ\������邽�߁A�w���肪�̗p����Ȃ������G���W�������߂��Ǝw����Ɠǂ݋؂���������̂ŁB
				if (!Utils.isEmpty(resultEngine.getLatestPv())) {
					systemOutputThread.getCommandList().add(resultEngine.getLatestPv());
				}

				// �e�G���W����bestmove���Q�l���Ƃ���GUI�֏o�͂���
				// �EGUI�Ō��₷���悤�ɃG���W���͋t���ɂ��Ă����i�G���W��1����ɕ\�������悤�Ɂj
				for (int i = usiEngineList.size() - 1; i >= 0; i--) {
					UsiEngine engine = usiEngineList.get(i);

					// �i��j�ubestmove 7g7f�v
					String engineBest = engine.getBestmoveCommandExceptPonder();
					// ���̃G���W���̎w���肪�̗p���ꂽ���ۂ��B�ۂƃo�c�����A�uO�v�ƁuX�v�ő�ւ���B
					String hantei = engineBest.equals(bestmoveCommand) ? "O" : "X";

					// �i��j�uinfo string [O] bestmove 7g7f [�V�Z(77)] [�]���l 123 �i�O��100 ����23�j] [Gikou 20160606]�v
					// [ponder�Ή�]
					// �i��j�uinfo string [O] bestmove 7g7f ponder 3c3d [�V�Z(77) �R�l(33)] [�]���l 123 �i�O��100 ����23�j] [Gikou 20160606]�v
					systemOutputThread.getCommandList().add("info string [" + hantei + "] " + engine.getBestmoveScoreDisp(true, true));

					// �y�O��̒l�z��ۑ�
					engine.savePrevValue();
				}

				// bestmove��GUI�֕Ԃ�
				// TODO [ponder�Ή�]
				// systemOutputThread.getCommandList().add(bestmoveCommand);
				systemOutputThread.getCommandList().add(resultEngine.getBestmoveCommand());
			}

			// GUI�ɕԂ��I���܂ő҂�
			systemOutputThread.waitUntilEmpty(10 * 1000);

			// [ponder�Ή�]
			// ���c��������ponder����
			ponderAtGougiEnd(usiEngineList, resultEngine);

			// ���c����
			return true;
		}

		return false;
	}

	/**
	 * ���c��������ponder����
	 * 
	 * @param usiEngineList
	 * @param resultEngine
	 */
	private void ponderAtGougiEnd(List<UsiEngine> usiEngineList, UsiEngine resultEngine) {
		// �ubestmove�v���uponder�v�t���ł͂Ȃ������ꍇ�A�������Ȃ�
		if (Utils.isEmpty(resultEngine.getPonderMove())) {
			return;
		}

		// ponder�̋ǖ�
		// �i��j�uposition startpos moves 1g1f 4a3b 6i7h�v
		String pos = StateInfo.getInstance().getLatestGoNotPonderPosition();
		pos = ShogiUtils.appendMove(pos, resultEngine.getBestmove());
		pos = ShogiUtils.appendMove(pos, resultEngine.getPonderMove());

		// �e�G���W�������[�v
		for (UsiEngine engine : usiEngineList) {
			// ���Y�G���W����ponder�ݒ�on�̏ꍇ
			if (engine.isPonderOnOff()) {
				// ���Y�G���W����ponder���{���̏ꍇ
				if (engine.isPondering()) {
					// TODO
					// ���Y�G���W���ƍ��c���ʂ̃G���W����bestmove�̎w���肪�������ꍇ�iponder�̎w���肪�قȂ�ꍇ���܂ށj
					// �E���Y�G���W�������c���ʂ̃G���W���ł���ꍇ���܂�
					// if (engine.getBestmove().equals(resultEngine.getBestmove())) {
					// TODO
					// ���Y�G���W���ƍ��c���ʂ̃G���W����bestmove�̎w�����ponder�̎w���肪�Ƃ��ɑ��݂��A���A�������ꍇ
					// �E���Y�G���W�������c���ʂ̃G���W���ł���ꍇ���܂�
					if (ShogiUtils.equalsBestmoveAndPondermove(engine, resultEngine)) {
						logger.info("���c�������ɂ��̂܂�ponder�𑱂���[" + engine.getUsiName() + "]");
						// ���̂܂�ponder�𑱂���
						continue;
					}

					// ���̑��̏ꍇ�A�ustop�v���s
					else {
						// �ustop�v�R�}���h���G���W���֑��M
						// �E�X���b�h�̔r������
						synchronized (engine.getOutputThread()) {
							logger.info("���c�������Ɂustop�v�R�}���h���G���W���֑��M[" + engine.getUsiName() + "]");
							engine.getOutputThread().getCommandList().add("stop");
						}

						// �G���W������ubestmove�v���Ԃ��Ă���܂ő҂�
						engine.getInputThread().waitUntilCommandStartsWith("bestmove", 10 * 1000);
						logger.info("���c�������́ustop�v�R�}���h�Łubestmove�v��M[" + engine.getUsiName() + "]");

						// �ubestmove�v���폜
						// �E�X���b�h�̔r������
						synchronized (engine.getInputThread()) {
							Utils.removeFromListWhereStartsWith(engine.getInputThread().getCommandList(), "bestmove");
							logger.info("���c�������́ustop�v�R�}���h�Łubestmove�v��M��ɍ폜����[" + engine.getUsiName() + "]");
						}

						// ponder�����G���W���ɃZ�b�g
						engine.setPondering(false);
						engine.setPonderingPosition(null);
					}
				}

				// �΋ǒ��̏ꍇ
				if (StateInfo.getInstance().isDuringGame()) {
					// �ugo ponder�v���s
					// �G���W���ւ̃R�}���h���X�g�ɒǉ�
					// �E�X���b�h�̔r������
					synchronized (engine.getOutputThread()) {
						logger.info("���c�������Ɂugo ponder�v�R�}���h���G���W���֑��M[" + engine.getUsiName() + "]");

						// �ugo ponder�v�R�}���h���G���W���֑��M
						engine.getOutputThread().getCommandList().add(pos);
						engine.getOutputThread().getCommandList().add("go ponder " + StateInfo.getInstance().getGoTimeOption());

						// ponder�����G���W���ɃZ�b�g
						engine.setPondering(true);
						engine.setPonderingPosition(pos);
					}

					// �ugo ponder�v����uponderhit�v�܂ł̎��Ԃ��Z������ƃG���W�������F���ł�����timeup����ꍇ������悤�Ȃ̂ŁA�����҂�
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
	 * ponder�I�����̏���
	 * �E�O��F�R�}���h��stop�Aponderhit�̂����ꂩ�ł��邱��
	 * 
	 * @param command
	 * @param systemInputThread
	 * @param systemOutputThread
	 * @param usiEngineList
	 */
	private void endPonder(String command, InputStreamThread systemInputThread, OutputStreamThread systemOutputThread, List<UsiEngine> usiEngineList) {
		// ponderhit�̏ꍇ
		if ("ponderhit".equals(command)) {
			logger.info("GUI����ponderhit��M");

			// ���߂́ugo�iponder�ł͂Ȃ��j�v�R�}���h�̋ǖʂ��Z�b�g
			StateInfo.getInstance().setLatestGoNotPonderPosition(StateInfo.getInstance().getLatestGoPonderPosition());

			// �e�G���W�������[�v
			for (UsiEngine engine : usiEngineList) {
				// ���Y�G���W����ponder���{���̏ꍇ
				if (engine.isPondering()) {
					// TODO ���Y�G���W�������ۂ�ponderhit�����ꍇ
					// if (engine.getPonderingPosition().equals(StateInfo.getInstance().getLatestGoPonderPosition())) {
					logger.info("GUI����ponderhit��M���A���Y�G���W���Ɂuponderhit�v�R�}���h�𑗐M[" + engine.getUsiName() + "]");

					// ���Y�G���W���Ɂuponderhit�v�R�}���h�𑗐M
					// �E�X���b�h�̔r������
					synchronized (engine.getOutputThread()) {
						engine.getOutputThread().getCommandList().add("ponderhit");
					}

					// ���̃G���W����
					continue;
					// }

					// TODO ���Y�G���W�������ۂɂ�ponderhit���Ȃ������ꍇ
					// else {
					// logger.info("GUI����ponderhit��M���A���Y�G���W���Ɂustop�v�R�}���h�𑗐M[" + engine.getUsiName() + "]");
					//
					// // ���Y�G���W���Ɂustop�v�R�}���h�𑗐M
					// // �E�X���b�h�̔r������
					// synchronized (engine.getOutputThread()) {
					// engine.getOutputThread().getCommandList().add("stop");
					// }
					//
					// // ���Y�G���W������ubestmove�v���Ԃ��Ă���܂ő҂�
					// engine.getInputThread().waitUntilCommandStartsWith("bestmove", 10 * 1000);
					//
					// logger.info("GUI����ponderhit��M���A���Y�G���W���Ɂustop�v�R�}���h�𑗐M������A�ubestmove�v��M[" + engine.getUsiName() + "]");
					// }
				}

				logger.info("GUI����ponderhit��M���A�ugo�v�R�}���h���G���W���֑��M[" + engine.getUsiName() + "]");

				// �ugo�v�R�}���h���G���W���֑��M
				engine.getOutputThread().getCommandList().add(StateInfo.getInstance().getLatestGoPonderPosition());
				engine.getOutputThread().getCommandList().add("go " + StateInfo.getInstance().getGoTimeOption());

				// �G���W����ponder�����N���A
				engine.setPondering(false);
				engine.setPonderingPosition(null);
			}
		}

		// stop�̏ꍇ
		else if ("stop".equals(command)) {
			logger.info("GUI����stop��M");

			boolean firstFlg = true;
			String bestmoveCommand = "bestmove";

			// �e�G���W�������[�v
			for (UsiEngine engine : usiEngineList) {
				// ���Y�G���W����ponder���{���̏ꍇ
				if (engine.isPondering()) {
					// TODO ���Y�G���W�������ۂ�ponderhit���Ȃ������ꍇ
					// if (engine.getPonderingPosition().equals(StateInfo.getInstance().getLatestGoPonderPosition())) {
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
					// }
				}
			}

			// GUI�Ɂubestmove�v��1�񂾂��Ԃ�
			// �E�X���b�h�̔r������
			synchronized (systemOutputThread) {
				systemOutputThread.getCommandList().add(bestmoveCommand);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * �e�G���W������̃R�}���h��W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�����i�uusi�v�̏ꍇ�j2
	 * @see co.jp.shogi.gougi.UsiLogicCommon#enginesToSysoutAtUsi2(co.jp.shogi.gougi.OutputStreamThread, java.util.List)
	 */
	@Override
	protected void enginesToSysoutAtUsi2(OutputStreamThread systemOutputThread, List<UsiEngine> usiEngineList) {
		// �������Ȃ�
	}

}
