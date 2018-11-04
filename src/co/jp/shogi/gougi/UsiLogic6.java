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
 * USI���W�b�N6
 * �E���c�^�C�v���u�l�T���G���W���Ƃ̍��c�i�ǂ݋؂̋ǖʂ��l�T���j�v�̏ꍇ�Ɏg��
 * �E���ǖʂɉ����āA�ǂ݋؂̋ǖʂ��l�T������B�ǂ݋؂̋ǖʂŋl�݂��������ꍇ�A�ʏ�̒T���G���W������mateinfo�R�}���h�i���v���O�����p�̓Ǝ��̊g���R�}���h�j�𑗂�A�l�݂����邱�Ƃ�`����B
 */
public class UsiLogic6 extends UsiLogicCommon {

	/** Logger */
	protected static Logger logger = Logger.getLogger(UsiLogic6.class.getName());

	// �ǂ݋؋ǖʗp�̋l�T���̃J�E���^�[
	private int g_cnt_pvMate_tried = 0;
	private int g_cnt_pvMate_success = 0;
	private int g_cnt_pvMate_last_displayed = 0;

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

		// �ǂ݋؋ǖʗp�̋l�T���G���W�����X�g
		logger.info("�ǂ݋؋ǖʗp�̋l�T���G���W�����X�g�̋N��");
		List<MateEngine> pvMateEngineList = createPvMateEngineList(mateEngine);
		usiEngineList.addAll(pvMateEngineList);

		// sfen�ϊ��p��˂��牤�̋N��
		logger.info("sfen�ϊ��p��˂��牤�̋N��");
		UsiEngine sfenYaneuraOu = new UsiEngine();
		sfenYaneuraOu.setEngineNumber(99);
		sfenYaneuraOu.setExeFile(new File(Utils.getMyDir().getAbsolutePath() + File.separator + Constants.SFEN_YANEURAOU_FILENAME));
		sfenYaneuraOu.createProcess();

		// �l�T���ς݂̓ǂ݋؋ǖʃZ�b�g
		// �E�v�f�̗�uposition startpos moves 2g2f 3c3d�v
		Set<String> searchedPvPosSet = new HashSet<String>();
		// �l�T���ς݂̓ǂ݋؋ǖʁisfen�j�Z�b�g
		// �E�v�f�̗�usfen lnsgkgsnl/1r5b1/pppppp1pp/6p2/9/7P1/PPPPPPP1P/1B5R1/LNSGKGSNL b - 3�v
		Set<String> searchedPvSfenSet = new HashSet<String>();
		// mateinfo�R�}���h���X�g
		List<MateInfoBean> mateInfoList = new ArrayList<MateInfoBean>();

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
					// �S�G���W���̓ǂ݋؃��X�g���N���A
					// ShogiUtils.clearPvList(usiEngineList);
					// �W�����́iGUI���j����̃R�}���h�����݂̃G���W���ւ̃R�}���h���X�g�ɒǉ�����i�ugo�v�܂��́ugo ponder�v�̏ꍇ�j
					sysinToEnginesAtGoOrGoPonder(command, currentEngine);
					// �W�����́iGUI���j����̃R�}���h���l�T���G���W���ւ̃R�}���h���X�g�ɒǉ�����i�ugo�v�܂��́ugo ponder�v�̏ꍇ�j
					sysinToMateEngineAtGoOrGoPonder(mateEngine, sfenYaneuraOu);
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
				// �l�T���G���W���̒T�����t���O��false���Z�b�g
				mateEngine.setSearching(false);
				// �e�G���W������̃R�}���h��W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�����i�uisready�v�̏ꍇ�j
				enginesToSysoutAtIsReady(systemOutputThread, usiEngineList);
				logger.info("�uisready�v�̏ꍇ END");
			}

			// �W�����́iGUI���j����̃R�}���h�Ɂuusinewgame�v���܂܂��ꍇ
			if (Utils.contains(systemInputCommandList, "usinewgame")) {
				logger.info("�uusinewgame�v�̏ꍇ START");
				StateInfo.getInstance().setDuringGame(true);
				searchedPvPosSet = new HashSet<String>();
				searchedPvSfenSet = new HashSet<String>();
				// �S�G���W���̓ǂ݋؃��X�g���N���A
				ShogiUtils.clearPvList(usiEngineList);

				// �l�T���G���W���̒T�����t���O�𗎂Ƃ�
				mateEngine.setSearching(false);
				for (MateEngine pvMateEngine : pvMateEngineList) {
					pvMateEngine.setSearching(false);
				}

				// �ǂ݋؋ǖʗp�̋l�T���̃J�E���^�[�̃N���A
				g_cnt_pvMate_tried = 0;
				g_cnt_pvMate_success = 0;
				g_cnt_pvMate_last_displayed = 0;

				// mateinfo�R�}���h���X�g�̃N���A
				mateInfoList = new ArrayList<MateInfoBean>();

				logger.info("�uusinewgame�v�̏ꍇ END");
			}

			// �W�����́iGUI���j����̃R�}���h�Ɂugameover�v���܂܂��ꍇ
			if (Utils.containsStartsWith(systemInputCommandList, "gameover")) {
				logger.info("�ugameover�v�̏ꍇ START");
				StateInfo.getInstance().setDuringGame(false);
				// �S�G���W���̓ǂ݋؃��X�g���N���A
				ShogiUtils.clearPvList(usiEngineList);
				// mateinfo�R�}���h���X�g�̃N���A
				mateInfoList = new ArrayList<MateInfoBean>();
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
			mateEnginesToSysout(systemOutputThread, mateEngine, sfenYaneuraOu);
			// ���݂̃G���W������̃R�}���h��W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�����i�ʏ�̏ꍇ�j
			enginesToSysoutAtNormal(systemOutputThread, currentEngine);

			// �΋ǒ��̏ꍇ
			// if (StateInfo.getInstance().isDuringGame()) {
			// ���s�񐔂�K���Ɍ��炵�Ă���
			if (Math.random() < 0.1) {
				// �ǂ݋؋ǖʗp�̋l�T�������i�l�T���G���W���ցj
				pvMateToMateEngine(currentEngine, pvMateEngineList, sfenYaneuraOu, searchedPvPosSet, searchedPvSfenSet);
				// �ǂ݋؋ǖʗp�̋l�T�������i�l�T���G���W�����j
				pvMateFromMateEngine(currentEngine, pvMateEngineList, systemOutputThread, mateInfoList);
				// ���݂̃G���W����mateinfo�R�}���h�𑗐M
				sendMateInfoToCurrentEngine(currentEngine, mateInfoList);
			}
			// }

			// �ǂ݋؋ǖʗp�̋l�T���̃J�E���^�[��W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�����
			if (g_cnt_pvMate_tried % 50 == 0 && g_cnt_pvMate_tried > 0 && g_cnt_pvMate_tried != g_cnt_pvMate_last_displayed) {
				synchronized (systemOutputThread) {
					systemOutputThread.getCommandList().add("info string pvMateTriedCount=" + g_cnt_pvMate_tried + ", pvMateSuccessCount=" + g_cnt_pvMate_success);
				}
				g_cnt_pvMate_last_displayed = g_cnt_pvMate_tried;
			}

			// sleep
			Thread.sleep(10);
		}

		// sfen�ϊ��p��˂��牤��quit�R�}���h���M
		// �E�X���b�h�̔r������
		synchronized (sfenYaneuraOu.getOutputThread()) {
			sfenYaneuraOu.getOutputThread().getCommandList().add("quit");
		}

		// �O�̂��߃v���Z�X�I��
		Thread.sleep(500);
		sfenYaneuraOu.destroy();

		// �ǂ݋؋ǖʗp�̋l�T���G���W���̃v���Z�X�I��
		// �E����quit�R�}���h�����M����Ă���͂������A�O�̂��߁B
		// �E�ǂ݋؋ǖʗp�ł͂Ȃ����̋l�T���G���W���imateEngine�j��SimpleGougiShogiMain�Ńv���Z�X�I�����Ă���̂ŁA�����ł͕s�v�B�i�{���͓��ꂵ�������������D�D�D�j
		if (pvMateEngineList != null) {
			for (UsiEngine engine : pvMateEngineList) {
				engine.destroy();
			}
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
	 * �E���ǖʁi���߂́uposition�v�R�}���h�̋ǖʁj�Ługo mate�v�R�}���h�𑗐M����B
	 * 
	 * @param mateEngine
	 * @param sfenYaneuraOu
	 * @throws IOException
	 */
	protected void sysinToMateEngineAtGoOrGoPonder(MateEngine mateEngine, UsiEngine sfenYaneuraOu) throws IOException {
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

		// sfen�ϊ��p��˂��牤����sfen��������擾
		String sfen = getSfenFromYaneuraOu(sfenYaneuraOu, position);
		logger.info("sfen�ϊ��p��˂��牤����擾����sfen������[" + sfen + "]");

		// ���Y�G���W���ւ̃R�}���h���X�g�ɒǉ�
		// �E�X���b�h�̔r������
		synchronized (mateEngine.getOutputThread()) {
			// sfen�ϊ��p��˂��牤����擾����sfen��������l�T���G���W���֑��M
			mateEngine.getOutputThread().getCommandList().add(sfen);

			// �^�C���A�E�g��ݒ肵�āugo mate�v�R�}���h�𑗐M
			// mateEngine.getOutputThread().getCommandList().add("go mate infinite");
			mateEngine.getOutputThread().getCommandList().add("go mate " + StateInfo.getInstance().getMateTimeout());
		}

		// �T�����t���O���Z�b�g
		mateEngine.setSearching(true);
	}

	/**
	 * sfen�ϊ��p��˂��牤����sfen��������擾
	 * �i��j�uposition startpos moves 7g7f�v
	 * ���uposition sfen lnsgkgsnl/1r5b1/ppppppppp/9/9/2P6/PP1PPPPPP/1B5R1/LNSGKGSNL w - 2�v
	 * 
	 * @param sfenYaneuraOu
	 * @param position
	 * @return
	 */
	private String getSfenFromYaneuraOu(UsiEngine sfenYaneuraOu, String position) {
		// sfen�ϊ��p��˂��牤
		// �E�X���b�h�̔r������
		synchronized (sfenYaneuraOu.getOutputThread()) {
			// sfen�ϊ��p��˂��牤�Ɂuposition�v�R�}���h�𑗐M
			sfenYaneuraOu.getOutputThread().getCommandList().add(position);

			// sfen�ϊ��p��˂��牤�Ɂuprintsfen�v�R�}���h�i�Ǝ��Ɋg�������R�}���h�j�𑗐M
			logger.info("sfen�ϊ��p��˂��牤�Ɂuprintsfen�v�R�}���h�i�Ǝ��Ɋg�������R�}���h�j�𑗐M");
			sfenYaneuraOu.getOutputThread().getCommandList().add("printsfen");
		}

		// sfen�ϊ��p��˂��牤����usfen�v���Ԃ��Ă���܂ő҂�
		sfenYaneuraOu.getInputThread().waitUntilCommandStartsWith("sfen", 10 * 1000);
		logger.info("sfen�ϊ��p��˂��牤����usfen�v����M");

		String sfen = "";

		// �usfen�v���擾������A�폜����
		// �E�X���b�h�̔r������
		synchronized (sfenYaneuraOu.getInputThread()) {
			sfen = Utils.getItemStartsWith(sfenYaneuraOu.getInputThread().getCommandList(), "sfen");
			Utils.removeFromListWhereStartsWith(sfenYaneuraOu.getInputThread().getCommandList(), "sfen");
		}

		return "position " + sfen;
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

						// �uinfo�`pv�`�v�̏ꍇ�A�ǂ݋؂��G���W���ɕۑ�
						if (command.startsWith("info ") && command.indexOf(" pv ") >= 0) {
							// �i��j�uinfo depth 3 seldepth 4 multipv 1 score cp 502 nodes 774 nps 154800 time 5 pv 2g3g 4h3g 2c3d S*3f�v
							// �� �u2g3g 4h3g 2c3d S*3f�v
							String pv = command.substring(command.indexOf(" pv ") + " pv ".length()).trim();

							// �������A��Ղ̎w����Ȃǂŉ���̂悤�ȃR�}���h�������Ă����ꍇ�͏��O����B
							// �i��j�uinfo pv 7g7f 8c8d (38.62%) score cp 0 depth 32 multipv 1�v
							// �E�u pv �v���E�̕�����ɓǂ݋؈ȊO���܂܂�Ă���A��������̂܂�sfen�ϊ��p��˂��牤�֓n���ƌ��ʂ��ۏ؂���Ȃ����߁B
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
	 * �l�T���G���W������̃R�}���h��W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�����
	 * 
	 * @param systemOutputThread
	 * @param mateEngine
	 * @param sfenYaneuraOu
	 * @throws IOException
	 */
	private void mateEnginesToSysout(OutputStreamThread systemOutputThread, MateEngine mateEngine, UsiEngine sfenYaneuraOu) throws IOException {
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

						logger.info("Utils.equals(mateEngine.getLatestGoMatePosition(), StateInfo.getInstance().getLatestPosition())=" + Utils.equals(mateEngine.getLatestGoMatePosition(), StateInfo.getInstance().getLatestPosition()));

						// ���߂́ugo mate�v�R�}���h�̋ǖʂƒ��߂́uposition�v�R�}���h�̋ǖʂ���v����ꍇ
						// �E���ۂ̋ǖʂ͎��̎w����Ɉڂ��Ă��A�l�T���G���W���͑O�̋ǖʂ��v�l�������Ă���ꍇ������̂ŁB
						if (Utils.equals(mateEngine.getLatestGoMatePosition(), StateInfo.getInstance().getLatestPosition())) {
							// �i��j�ucheckmate R*6a 5a6a 2a4a 6a6b 8b7a�v�̏ꍇ
							// �E�ucheckmate nomate�v�ucheckmate timeout�v�ucheckmate�̂݁v�ucheckmate notimplemented�v�̏ꍇ�͏���
							if (command.startsWith("checkmate ") && !("checkmate nomate".equals(command) || "checkmate timeout".equals(command) || "checkmate".equals(command) || "checkmate notimplemented".equals(command))) {
								// checkmate�̎w������Z�b�g
								mateEngine.setCheckmateMoves(command.substring("checkmate ".length()));

								// �l�T���G���W���̓ǂ݋؁i�]���l���܂ށj���쐬���ďo�͂���
								// �i��j�uinfo score mate 5 pv R*6a 5a6a 2a4a 6a6b 8b7a�v
								systemOutputThread.getCommandList().add(mateEngine.createPv());
							}
						}

						// ���̑��̏ꍇ�i���߂́ugo mate�v�R�}���h�̋ǖʂƒ��߂́uposition�v�R�}���h�̋ǖʂ��قȂ�ꍇ�j
						// �E���ۂ̋ǖʂ͎��̎w����Ɉڂ��Ă��A�l�T���G���W���͑O�̋ǖʂ��v�l�������Ă����ꍇ
						else {
							// �΋ǒ��̏ꍇ
							if (StateInfo.getInstance().isDuringGame()) {
								// ���ǖʁi���߂́uposition�v�R�}���h�̋ǖʁj�Ługo mate�v�R�}���h�𑗐M
								sysinToMateEngineAtGoOrGoPonder(mateEngine, sfenYaneuraOu);
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
				// �ǂ݋؋ǖʗp�̋l�T���̃J�E���^�[��W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�����
				systemOutputThread.getCommandList().add("info string pvMateTriedCount=" + g_cnt_pvMate_tried + ", pvMateSuccessCount=" + g_cnt_pvMate_success);
				g_cnt_pvMate_last_displayed = g_cnt_pvMate_tried;

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
					// �i��j�uinfo string [X] [�l�ݖ���] [NanohaTsumeUSI]�v
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

					// �i��j�uinfo string [O] [5��l��] [�U���� �U��(51) �S��(21) �U��(61) �V��(82)] [NanohaTsumeUSI]�v
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

	/*
	 * (non-Javadoc)
	 * �e�G���W������̃R�}���h��W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�����i�uusi�v�̏ꍇ�j2
	 * @see co.jp.shogi.gougi.UsiLogicCommon#enginesToSysoutAtUsi2(co.jp.shogi.gougi.OutputStreamThread, java.util.List)
	 */
	@Override
	protected void enginesToSysoutAtUsi2(OutputStreamThread systemOutputThread, List<UsiEngine> usiEngineList) {
		// �Ǝ��I�v�V������ǉ�
		// �E�X���b�h�̔r������
		synchronized (systemOutputThread) {
			// �l�T���G���W���̃^�C���A�E�g�i�~���b�j
			systemOutputThread.getCommandList().add("option name G_MateTimeout type spin default 10000 min 1 max 1000000");
			// �ǂ݋؋ǖʗp�̋l�T���G���W���̃^�C���A�E�g�i�~���b�j
			systemOutputThread.getCommandList().add("option name G_PvMateTimeout type spin default 5000 min 1 max 1000000");
			// mateinfo�R�}���h�̑��M��
			systemOutputThread.getCommandList().add("option name G_MateInfoCount type spin default 100 min 1 max 1000");
			// mateinfo�R�}���h�̑��M�Ԋu�i�~���b�j
			systemOutputThread.getCommandList().add("option name G_MateInfoInterval type spin default 100 min 1 max 100000");
		}
	}

	/**
	 * �ǂ݋؋ǖʗp�̋l�T���G���W�����X�g�̋N��
	 * @param mateEngine
	 * @return
	 * @throws IOException
	 */
	private List<MateEngine> createPvMateEngineList(MateEngine mateEngine) throws IOException {
		List<MateEngine> pvMateEngineList = new ArrayList<MateEngine>();

		for (int i = 0; i < GougiConfig.getInstance().getPvMateEngineCount(); i++) {
			MateEngine pvMateEngine = new MateEngine();
			pvMateEngine.setEngineNumber(11 + i);
			// ���s�t�@�C���͕��ʂ̋l�ݒT���G���W���Ɠ���
			pvMateEngine.setExeFile(mateEngine.getExeFile());
			// �N��
			pvMateEngine.createProcess();
			// ���X�g�ɒǉ�
			pvMateEngineList.add(pvMateEngine);
		}

		return pvMateEngineList;
	}

	/**
	 * �ǂ݋؋ǖʗp�̋l�T�������i�l�T���G���W���ցj
	 * @param currentEngine
	 * @param pvMateEngineList
	 * @param sfenYaneuraOu
	 * @param searchedPvPosSet
	 * @param searchedPvSfenSet
	 */
	private void pvMateToMateEngine(UsiEngine currentEngine, List<MateEngine> pvMateEngineList, UsiEngine sfenYaneuraOu, Set<String> searchedPvPosSet, Set<String> searchedPvSfenSet) {
		// �T�����ł͂Ȃ��l�T���G���W�������݂��Ȃ��ꍇ
		if (ShogiUtils.getMateEngineNotSearching(pvMateEngineList) == null) {
			return;
		}

		// �ǂ݋؃��X�g�����[�v
		// �E�V������������
		for (int i = currentEngine.getPvList().size() - 1; i >= 0; i--) {
			// ���߂́uposition�v�R�}���h�̋ǖʁi�ugo�iponder�ł͂Ȃ��j�v���ugo ponder�v���͖��Ȃ��j
			// �i��j�uposition startpos moves 2g2f 3c3d�v
			String pos = StateInfo.getInstance().getLatestPosition();

			// �������������ɔ��肵�������悢���D�D�D
			// if (pos.equals("position startpos")) {
			if (pos.indexOf(" moves ") < 0) {
				pos += " moves";
			}
			// logger.info("i=" + i + ", pos=" + pos);

			// pv�̗�F�u7g7f 4c4d 3i4h�v
			String pv = currentEngine.getPvList().get(i);
			String[] sa = pv.split(" ", -1);

			for (int j = 0; j < sa.length; j++) {
				pos += " " + sa[j];
				// logger.info("i=" + i + ", j=" + j + ", pos=" + pos);

				// �T�����ł͂Ȃ��l�T���G���W����Ԃ�
				MateEngine pvMateEngine = ShogiUtils.getMateEngineNotSearching(pvMateEngineList);
				if (pvMateEngine == null) {
					return;
				}

				// ���T���̋ǖʂ̏ꍇ
				if (!searchedPvPosSet.contains(pos)) {
					// �l�T���ς݂̓ǂ݋؋ǖʃZ�b�g�ɒǉ�
					searchedPvPosSet.add(pos);

					// sfen�ϊ��p��˂��牤����sfen��������擾
					String sfen = getSfenFromYaneuraOu(sfenYaneuraOu, pos);
					logger.info("sfen�ϊ��p��˂��牤����擾����sfen������[" + sfen + "]");

					// ���T���̋ǖʂ̏ꍇ
					if (!searchedPvSfenSet.contains(sfen)) {
						// �l�T���ς݂̓ǂ݋؋ǖʁisfen�j�Z�b�g�ɒǉ�
						searchedPvSfenSet.add(sfen);

						logger.info("searchedPvPosSet.size()=" + searchedPvPosSet.size());
						logger.info("searchedPvSfenSet.size()=" + searchedPvSfenSet.size());

						// �l�T���G���W����checkmate�̎w������N���A
						pvMateEngine.setCheckmateMoves(null);
						// �l�T���G���W���ɒ��߂́ugo mate�v�R�}���h�̋ǖʂ��Z�b�g
						pvMateEngine.setLatestGoMatePosition(pos);
						pvMateEngine.setLatestGoMatePositionSfen(sfen);

						// ���Y�G���W���ւ̃R�}���h���X�g�ɒǉ�
						// �E�X���b�h�̔r������
						synchronized (pvMateEngine.getOutputThread()) {
							// sfen�ϊ��p��˂��牤����擾����sfen��������l�T���G���W���֑��M
							pvMateEngine.getOutputThread().getCommandList().add(sfen);

							// �^�C���A�E�g��ݒ肵�āugo mate�v�R�}���h�𑗐M
							// mateEngine.getOutputThread().getCommandList().add("go mate infinite");
							pvMateEngine.getOutputThread().getCommandList().add("go mate " + StateInfo.getInstance().getPvMateTimeout());
						}

						// �T�����t���O���Z�b�g
						pvMateEngine.setSearching(true);
						// �ǂ݋؋ǖʗp�̋l�T���̃J�E���^�[
						g_cnt_pvMate_tried++;
					}
				}
			}
		}
	}

	/**
	 * �ǂ݋؋ǖʗp�̋l�T�������i�l�T���G���W�����j
	 * @param currentEngine
	 * @param pvMateEngineList
	 * @param systemOutputThread
	 * @param mateInfoList
	 */
	private void pvMateFromMateEngine(UsiEngine currentEngine, List<MateEngine> pvMateEngineList, OutputStreamThread systemOutputThread, List<MateInfoBean> mateInfoList) {
		for (MateEngine pvMateEngine : pvMateEngineList) {
			InputStreamThread processInputThread = pvMateEngine.getInputThread();
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
				for (String command : processInputCommandList) {
					// �R�}���h���ucheckmate�v�Ŏn�܂�ꍇ
					if (command.startsWith("checkmate")) {
						// �T�����t���O�𗎂Ƃ�
						pvMateEngine.setSearching(false);

						// �i��j�ucheckmate R*6a 5a6a 2a4a 6a6b 8b7a�v�̏ꍇ
						// �E�ucheckmate nomate�v�ucheckmate timeout�v�ucheckmate�̂݁v�ucheckmate notimplemented�v�̏ꍇ�͏���
						if (command.startsWith("checkmate ") && !("checkmate nomate".equals(command) || "checkmate timeout".equals(command) || "checkmate".equals(command) || "checkmate notimplemented".equals(command))) {
							// �W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�
							// �E�X���b�h�̔r������
							synchronized (systemOutputThread) {
								// �W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�
								systemOutputThread.getCommandList().add("info string pvMate : " + command);
							}

							// checkmate�̎w������Z�b�g
							pvMateEngine.setCheckmateMoves(command.substring("checkmate ".length()));

							// �umateinfo�v�R�}���h���쐬
							// �i��j�umateinfo position sfen ln1g1g1n1/2s1k4/p1ppp3p/5ppR1/P8/2P1LKP2/3PPP2P/5+rS2/L5sNL b BGSN2Pbg2p 1 checkmate 5f5c+ 5b5c N*6e 5c4b S*4c 4b5a G*4b 4a4b 4c4b+ 5a4b 2d2b+ S*3b G*5c 4b4a B*5b 6a5b 5c5b 4a5b 2b3b L*4b S*5c 5b6a G*6b�v
							String mateinfo = "mateinfo " + pvMateEngine.getLatestGoMatePositionSfen() + " " + command;

							// MateInfoBean�𐶐����Amateinfo�R�}���h���X�g�֒ǉ�
							mateInfoList.add(new MateInfoBean(mateinfo));

							// �ǂ݋؋ǖʗp�̋l�T���̃J�E���^�[
							g_cnt_pvMate_success++;

							// -----
						}
					}
				}
			}
		}
	}

	/**
	 * ���݂̃G���W����mateinfo�R�}���h�𑗐M
	 * @param currentEngine
	 * @param mateInfoList
	 */
	private void sendMateInfoToCurrentEngine(UsiEngine currentEngine, List<MateInfoBean> mateInfoList) {

		// ----- mateinfo�R�}���h�𑗐M

		// mateinfo�R�}���h���X�g�����[�v
		for (MateInfoBean mateInfoBean : mateInfoList) {
			// �O���mateinfo�R�}���h���M�����莞�Ԉȏ�o�߂����ꍇ
			if (System.currentTimeMillis() > mateInfoBean.getPrevTime() + StateInfo.getInstance().getMateInfoInterval()) {
				logger.info((mateInfoBean.getCount() + 1) + "��ڂ�mateinfo�R�}���h���M�F" + mateInfoBean.getCommand());

				// mateinfo�R�}���h�𑗐M
				// �X���b�h�̔r������
				synchronized (currentEngine.getOutputThread()) {
					currentEngine.getOutputThread().getCommandList().add(mateInfoBean.getCommand());
				}

				// ���M�񐔂ƑO�񑗐M�������X�V
				mateInfoBean.setCount(mateInfoBean.getCount() + 1);
				mateInfoBean.setPrevTime(System.currentTimeMillis());
			}
		}

		// ----- �ݒ肳�ꂽ���M�񐔂܂ő��M���I����MateInfoBean���폜

		// mateInfoList�̗v�f�����[�v���ō폜����̂�Iterator���g��
		Iterator<MateInfoBean> it = mateInfoList.iterator();

		// mateinfo�R�}���h���X�g�����[�v
		while (it.hasNext()) {
			MateInfoBean mateInfoBean = it.next();

			if (mateInfoBean.getCount() >= StateInfo.getInstance().getMateInfoCount()) {
				logger.info("�ݒ肳�ꂽ���M�񐔂܂ő��M���I����MateInfoBean���폜�F" + mateInfoBean.getCount() + "��A" + mateInfoBean.getCommand());

				it.remove();
			}
		}

	}

}
