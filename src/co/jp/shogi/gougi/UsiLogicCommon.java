package co.jp.shogi.gougi;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * USI���ʃ��W�b�N
 */
public abstract class UsiLogicCommon {

	/** Logger */
	protected static Logger logger = Logger.getLogger(UsiLogicCommon.class.getName());

	/**
	 * �W�����́iGUI���j����̃R�}���h���e�G���W���ւ̃R�}���h���X�g�ɒǉ�����
	 * 
	 * @param systemInputThread
	 * @param usiEngineList
	 * @param pendingSysinCommandList
	 * @return
	 */
	protected List<String> sysinToEngines(InputStreamThread systemInputThread, List<UsiEngine> usiEngineList, List<String> pendingSysinCommandList) {
		List<String> systemInputCommandList1 = null;

		// �W�����́iGUI���j����̓��͗p�X���b�h�̃R�}���h���X�g����ł͂Ȃ��ꍇ�A���[�J���ϐ��ɃR�s�[���Ă���N���A
		// �E�X���b�h�̔r������
		synchronized (systemInputThread) {
			if (!systemInputThread.getCommandList().isEmpty()) {
				systemInputCommandList1 = new ArrayList<String>(systemInputThread.getCommandList());
				systemInputThread.getCommandList().clear();
			}
		}

		// �e�G���W���ւ̃R�}���h���X�g
		List<String> systemInputCommandList2 = new ArrayList<String>();

		// �R�}���h�����݂���ꍇ
		if (systemInputCommandList1 != null) {
			for (String command : systemInputCommandList1) {
				// position�R�}���h�̏ꍇ�AStateInfo�ɃZ�b�g
				// �EsystemInputCommandList2�ɂ͒ǉ����Ȃ��B
				if (command.startsWith("position ")) {
					StateInfo.getInstance().setLatestPosition(command);
					logger.info("StateInfo.getInstance().isSente()=" + StateInfo.getInstance().isSente());
					// �S�G���W���̓ǂ݋؃��X�g���N���A
					ShogiUtils.clearPvList(usiEngineList);
					continue;
				}

				// �ugo�v�R�}���h�܂��́ugo ponder�v�R�}���h�̏ꍇ�ApendingSysinCommandList�ɒǉ�
				// �E�ugo�v�R�}���h�́ugo�v�����ł͂Ȃ��ugo btime 0 wtime 0 byoyomi 3000�v�Ȃǂ̏ꍇ������̂Œ��ӁB
				// �E�ugo ponder�v�R�}���h���ugo ponder�v�����ł͂Ȃ��ugo ponder btime 0 wtime 0 byoyomi 3000�v�Ȃǂ̏ꍇ������̂Œ��ӁB
				// �EsystemInputCommandList2�ɂ͒ǉ����Ȃ��B
				else if (command.startsWith("go")) {
					// StateInfo��latestSysinGoCommand�ɃZ�b�g�i���Ԍv���p�j
					StateInfo.getInstance().setLatestSysinGoCommand(command);
					pendingSysinCommandList.add(command);

					// �ugo ponder�v�̒���Ɂuponderhit�v��ustop�v�����邱�Ƃ�����̂ŁA���̎��_��ponder���{���t���O���Z�b�g���Ă���
					if (command.startsWith("go ponder")) {
						// ponder���{���t���O���Z�b�g
						StateInfo.getInstance().setPondering(true);
					}

					continue;
				}

				// ponder����stop�܂���ponderhit�̏ꍇ�ApendingSysinCommandList�ɒǉ�
				// �EsystemInputCommandList2�ɂ͒ǉ����Ȃ��B
				else if (StateInfo.getInstance().isPondering() && ("stop".equals(command) || "ponderhit".equals(command))) {
					pendingSysinCommandList.add(command);
					continue;
				}

				// ���̑��̏ꍇ�AsystemInputCommandList2�֒ǉ�
				else {
					systemInputCommandList2.add(command);
				}
			}
		}

		for (UsiEngine engine : usiEngineList) {
			// �e�G���W���ւ̃R�}���h���X�g�ɒǉ�
			// �E�X���b�h�̔r������
			synchronized (engine.getOutputThread()) {
				for (String command : systemInputCommandList2) {
					// �W�����́iGUI���j����̃R�}���h��ҏW�isetoption�Ή��j
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
	 * �W�����́iGUI���j����̃R�}���h��ҏW�isetoption�Ή��j
	 * �E�ʏ��command�����̂܂ܕԂ��B
	 * �Esetoption�̏ꍇ�A�ҏW��̃R�}���h��Ԃ��B�inull��Ԃ����Ƃ�����j
	 * 
	 * @param command
	 * @param engine
	 * @return
	 */
	private String editSysinCommand(String command, UsiEngine engine) {
		// �usetoption�v�̏ꍇ
		if (command.startsWith("setoption ")) {
			return editSysinCommandAtSetOption(command, engine);
		}

		// ���̑��̏ꍇ�Acommand�����̂܂ܕԂ�
		return command;
	}

	/**
	 * �W�����́iGUI���j����̃R�}���h��ҏW�isetoption�Ή��j
	 * �E�O��F����command��setoption�R�}���h�ł��邱�ƁB
	 * �Esetoption�R�}���h�ŁA�Ⴆ�΁uE2_OwnBook�v���uOwnBook�v�֖߂��i�uoption name�v�̂Ƃ��ɁuOwnBook�v���uE2_OwnBook�v�ɕϊ������̂Łj
	 * �i��j�usetoption name E2_OwnBook value true�v
	 * ���usetoption name OwnBook value true�v�i�G���W��2�̏ꍇ�̂݁j
	 * 
	 * @param command
	 * @param engine
	 * @return
	 */
	private String editSysinCommandAtSetOption(String command, UsiEngine engine) {
		// �i��j�usetoption name E2_OwnBook value true�v�̏ꍇ
		// �E�G���W��1�̏ꍇ�Anull��Ԃ�
		// �E�G���W��2�̏ꍇ�A�usetoption name OwnBook value true�v��Ԃ�
		// �E�G���W��3�̏ꍇ�Anull��Ԃ�
		//
		// �i��j�usetoption name USI_Hash value 128�v�̏ꍇ
		// �E�G���W��1�̏ꍇ�A�usetoption name USI_Hash value 128�v��Ԃ�
		// �E�G���W��2�̏ꍇ�A�usetoption name USI_Hash value 128�v��Ԃ�
		// �E�G���W��3�̏ꍇ�A�usetoption name USI_Hash value 128�v��Ԃ�

		// �R�}���h����I�v�V���������擾
		// �i��j�uE2_OwnBook�v�uUSI_Hash�v
		String option = ShogiUtils.getOptionName(command);

		if (Utils.isEmpty(option)) {
			// �ʏ�͂��肦�Ȃ��P�[�X�����Acommand�����̂܂ܕԂ��Ă���
			return command;
		}

		// �i��j�uUSI_Hash�v�uUSI_Ponder�v
		if (option.startsWith("USI")) {
			// �uUSI_Ponder�v�̏ꍇ�A�G���W���ɒl���Z�b�g
			if ("USI_Ponder".equals(option)) {
				setEnginePonderOnOff(engine, command);
			}

			// �l�T���G���W���̏ꍇ�A�uUSI_Hash�v���ő�256MB�ɐ������Ă���
			if (engine instanceof MateEngine && "USI_Hash".equals(option)) {
				// �i��j�usetoption name USI_Hash value 1024�v���u1024�v
				int usi_hash = Utils.getIntValue(Utils.getSplitResult(command, " ", 4), 0);
				logger.info("usi_hash=" + usi_hash);
				if (usi_hash > 256) {
					return "setoption name USI_Hash value 256";
				}
			}

			// command�����̂܂ܕԂ�
			return command;
		}

		// �i��j�uE2_�v�Ŏn�܂�ꍇ
		if (option.startsWith(engine.getOptionNamePrefix())) {
			// �i��j�uE2_OwnBook�v���uOwnBook�v
			String str = option.substring(engine.getOptionNamePrefix().length());

			// ���Y�G���W����USI�I�v�V�����Ɋ܂܂��ꍇ�i�O�̂��߃`�F�b�N�j
			if (engine.getOptionSet().contains(str)) {
				// �uUSI_Ponder�v�̏ꍇ�A�G���W���ɒl���Z�b�g
				if ("USI_Ponder".equals(str)) {
					setEnginePonderOnOff(engine, command);
				}

				String prefix = "setoption name " + engine.getOptionNamePrefix();
				// �i��j�usetoption name OwnBook value true�v
				return "setoption name " + command.substring(prefix.length()).trim();
			}
		}

		// ���c�^�C�v�u���育�Ƃɑ΋ǎҌ��v�Ή�
		if ("G_ChangePlayerPlys".equals(option)) {
			// �i��j�usetoption name G_ChangePlayerPlys value 3�v���u3�v
			int changePlayerPlys = Utils.getIntValue(Utils.getSplitResult(command, " ", 4), 1);
			logger.info("changePlayerPlys=" + changePlayerPlys);
			StateInfo.getInstance().setChangePlayerPlys(changePlayerPlys);
			return null;
		}

		// ���c�^�C�v�u���ՁE���ՁE�I�Ղő΋ǎҌ��v�Ή�
		// �E���Ղ̑΋ǎ҂Ɍ�シ��萔
		if ("G_ChuubanStartPlys".equals(option)) {
			// �i��j�usetoption name G_ChuubanStartPlys value 51�v���u51�v
			int chuubanStartPlys = Utils.getIntValue(Utils.getSplitResult(command, " ", 4), 51);
			logger.info("chuubanStartPlys=" + chuubanStartPlys);
			StateInfo.getInstance().setChuubanStartPlys(chuubanStartPlys);
			return null;
		}
		// �E�I�Ղ̑΋ǎ҂Ɍ�シ��萔
		if ("G_ShuubanStartPlys".equals(option)) {
			// �i��j�usetoption name G_ShuubanStartPlys value 101�v���u101�v
			int shuubanStartPlys = Utils.getIntValue(Utils.getSplitResult(command, " ", 4), 101);
			logger.info("shuubanStartPlys=" + shuubanStartPlys);
			StateInfo.getInstance().setShuubanStartPlys(shuubanStartPlys);
			return null;
		}

		// ���c�^�C�v�u2��O�̕]���l������l�ȏ㉺�~������΋ǎҌ��v�u2��O�̕]���l������l�ȏ�㏸������΋ǎҌ��v�Ή�
		if ("G_ChangePlayerScoreDiff".equals(option)) {
			// �i��j�usetoption name G_ChangePlayerScoreDiff value 200�v���u200�v
			int changePlayerScoreDiff = Utils.getIntValue(Utils.getSplitResult(command, " ", 4), 100);
			logger.info("G_ChangePlayerScoreDiff=" + changePlayerScoreDiff);
			StateInfo.getInstance().setChangePlayerScoreDiff(changePlayerScoreDiff);
			return null;
		}

		// ���c�^�C�v�u�l�T���G���W���Ƃ̍��c�v�Ή�
		if ("G_MateTimeout".equals(option)) {
			// �i��j�usetoption name G_MateTimeout value 30000�v���u30000�v
			int mateTimeout = Utils.getIntValue(Utils.getSplitResult(command, " ", 4), 10000);
			logger.info("G_MateTimeout=" + mateTimeout);
			StateInfo.getInstance().setMateTimeout(mateTimeout);
			return null;
		}

		// ���c�^�C�v�u�l�T���G���W���Ƃ̍��c�i�ǂ݋؂̋ǖʂ��l�T���j �v�Ή�
		if ("G_PvMateTimeout".equals(option)) {
			// �i��j�usetoption name G_PvMateTimeout value 30000�v���u30000�v
			int pvMateTimeout = Utils.getIntValue(Utils.getSplitResult(command, " ", 4), 5000);
			logger.info("G_PvMateTimeout=" + pvMateTimeout);
			StateInfo.getInstance().setPvMateTimeout(pvMateTimeout);
			return null;
		}

		// ���c�^�C�v�u�l�T���G���W���Ƃ̍��c�i�ǂ݋؂̋ǖʂ��l�T���j �v�Ή�
		if ("G_MateInfoCount".equals(option)) {
			// �i��j�usetoption name G_MateInfoCount value 10�v���u10�v
			int mateInfoCount = Utils.getIntValue(Utils.getSplitResult(command, " ", 4), 10);
			logger.info("G_MateInfoCount=" + mateInfoCount);
			StateInfo.getInstance().setMateInfoCount(mateInfoCount);
			return null;
		}

		// ���c�^�C�v�u�l�T���G���W���Ƃ̍��c�i�ǂ݋؂̋ǖʂ��l�T���j �v�Ή�
		if ("G_MateInfoInterval".equals(option)) {
			// �i��j�usetoption name G_MateInfoInterval value 1000�v���u1000�v
			int mateInfoInterval = Utils.getIntValue(Utils.getSplitResult(command, " ", 4), 1000);
			logger.info("G_MateInfoInterval=" + mateInfoInterval);
			StateInfo.getInstance().setMateInfoInterval(mateInfoInterval);
			return null;
		}

		// �i��j�uE2_�v�Ŏn�܂�I�v�V�����́A�G���W��1�ƃG���W��3�̏ꍇ��null��Ԃ��i�G���W��2�p�̃I�v�V�����̂͂��Ȃ̂Łj
		return null;
	}

	/**
	 * �G���W����ponder��on/off���Z�b�g
	 * �E�O��F���Y�G���W����USI_Ponder��setoption�R�}���h�ł��邱��
	 * 
	 * @param engine
	 * @param command
	 */
	private void setEnginePonderOnOff(UsiEngine engine, String command) {
		try {
			// �i��j�usetoption name USI_Ponder value true�v���utrue�v
			// �i��j�usetoption name E2_USI_Ponder value true�v���utrue�v
			String val = Utils.getSplitResult(command, " ", 4);
			engine.setPonderOnOff("true".equals(val));

			logger.info("[" + engine.getEngineDisp() + "]command=" + command);
			logger.info("[" + engine.getEngineDisp() + "]engine.isPonderOnOff()=" + engine.isPonderOnOff());
		} catch (Exception e) {
			// �������Ȃ�
		}
	}

	/**
	 * �W�����́iGUI���j����̃R�}���h���e�G���W���ւ̃R�}���h���X�g�ɒǉ�����i�ugo�v�̏ꍇ�j
	 * 
	 * @param goCommand
	 * @param usiEngineList
	 */
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
				engine.getOutputThread().getCommandList().add(goCommand);
			}
		}
	}

	/**
	 * �e�G���W������̃R�}���h��W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�����i�uusi�v�̏ꍇ�j
	 * 
	 * @param systemOutputThread
	 * @param usiEngineList
	 */
	protected void enginesToSysoutAtUsi(OutputStreamThread systemOutputThread, List<UsiEngine> usiEngineList) {
		// �uid name�`�v�Ɓuid author�`�v�͊e�G���W������ł͂Ȃ��A���̃v���O�����Ǝ��ŕԂ�
		// �E�X���b�h�̔r������
		synchronized (systemOutputThread) {
			systemOutputThread.getCommandList().add("id name " + Constants.USI_ID_NAME);
			systemOutputThread.getCommandList().add("id author " + Constants.USI_ID_AUTHOR);
		}

		// GUI�ɕԂ��I���܂ő҂�
		systemOutputThread.waitUntilEmpty(10 * 1000);

		// �e�G���W�������[�v
		for (UsiEngine engine : usiEngineList) {
			InputStreamThread processInputThread = engine.getInputThread();

			// �G���W������uusiok�v���Ԃ��Ă���܂ő҂�
			processInputThread.waitUntilCommand("usiok", 60 * 1000);

			List<String> processInputCommandList = null;

			// �G���W������̃R�}���h���X�g�����[�J���ϐ��ɃR�s�[���Ă���N���A
			// �E�X���b�h�̔r������
			synchronized (processInputThread) {
				processInputCommandList = new ArrayList<String>(processInputThread.getCommandList());
				processInputThread.getCommandList().clear();
			}

			List<String> systemOutputCommandList = new ArrayList<String>();

			// �G���W������̃R�}���h���X�g�����[�v
			// �E�R�}���h���X�g�ɂ́uusi�v�ւ̕ԐM�������Ă���͂��B
			// �E�uid name�`�v�uid author�`�v�uoption name�`�v�uusiok�v
			for (String command : processInputCommandList) {
				// �G���W������́uid name�`�v��GUI�֕Ԃ��Ȃ�
				if (command.startsWith("id name ")) {
					String usiName = command.substring("id name ".length());
					// �G���W����USI���ɃZ�b�g
					engine.setUsiName(usiName);
					logger.info(engine.getUsiName());
					continue;
				}

				// �uid name�`�v�ȊO�́uid�`�v�͓ǂݔ�΂�
				// �uusiok�v�͂��̒i�K�ł�GUI�֕Ԃ��Ȃ��i�S�G���W���́uusiok�v�������Ă���Ԃ��j
				if (command.startsWith("id ") || "usiok".equals(command)) {
					continue;
				}

				// �uoption name�`�v�̏ꍇ�A�I�v�V�������ɁuE1_�v�Ȃǂ�t�����Ă���GUI�֕Ԃ�
				if (command.startsWith("option name ")) {
					// �uoption name OwnBook type check default true�v���uoption name E1_OwnBook type check default true�v
					String cmd = "option name " + engine.getOptionNamePrefix() + command.substring("option name ".length()).trim();
					systemOutputCommandList.add(cmd);

					// �G���W���̃I�v�V�����Z�b�g�ɒǉ�
					engine.getOptionSet().add(ShogiUtils.getOptionName(command));
					// �G���W���́uUSI�I�v�V�����̑��M�ς݃R�}���h���X�g�v�ɒǉ�
					engine.getOptionCommandList().add(cmd);
				}
			}

			// �W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�
			// �E�X���b�h�̔r������
			synchronized (systemOutputThread) {
				systemOutputThread.getCommandList().addAll(new ArrayList<String>(systemOutputCommandList));
			}

			// GUI�ɕԂ��I���܂ő҂�
			systemOutputThread.waitUntilEmpty(10 * 1000);
		}

		// �e�G���W������̃R�}���h��W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�����i�uusi�v�̏ꍇ�j2
		enginesToSysoutAtUsi2(systemOutputThread, usiEngineList);

		// GUI�Ɂuusiok�v��1�񂾂��Ԃ�
		// �E�X���b�h�̔r������
		synchronized (systemOutputThread) {
			systemOutputThread.getCommandList().add("usiok");
		}

		// GUI�ɕԂ��I���܂ő҂�
		systemOutputThread.waitUntilEmpty(10 * 1000);
	}

	/**
	 * �e�G���W������̃R�}���h��W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�����i�uusi�v�̏ꍇ�j2
	 * 
	 * @param systemOutputThread
	 * @param usiEngineList
	 */
	abstract protected void enginesToSysoutAtUsi2(OutputStreamThread systemOutputThread, List<UsiEngine> usiEngineList);

	/**
	 * �e�G���W������̃R�}���h��W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�����i�uisready�v�̏ꍇ�j
	 * 
	 * @param systemOutputThread
	 * @param usiEngineList
	 */
	protected void enginesToSysoutAtIsReady(OutputStreamThread systemOutputThread, List<UsiEngine> usiEngineList) {
		// �e�G���W�������[�v
		for (UsiEngine engine : usiEngineList) {
			InputStreamThread processInputThread = engine.getInputThread();

			// �ureadyok�v���Ԃ��Ă���܂ő҂�
			processInputThread.waitUntilCommand("readyok", 60 * 1000);
			// �ureadyok�v���폜
			// �E�X���b�h�̔r������
			synchronized (processInputThread) {
				processInputThread.getCommandList().remove("readyok");
			}

			// ���łɂ����ŃN���A����
			engine.clear();
		}

		// ���ׂẴG���W������ureadyok�v���Ԃ��Ă�����AGUI�Ɂureadyok�v��1�񂾂��Ԃ�
		// �E�X���b�h�̔r������
		synchronized (systemOutputThread) {
			systemOutputThread.getCommandList().add("readyok");
		}

		// GUI�ɕԂ��I���܂ő҂�
		systemOutputThread.waitUntilEmpty(10 * 1000);
	}

	/**
	 * ����̍��c�^�C�v���Z�o
	 * 
	 * @param prevGougiType
	 * @return
	 */
	protected String calcCurrentGougiType(String prevGougiType) {
		// �ݒ�l���u�y�ύ��c�Ɣߊύ��c�����݁v�̏ꍇ
		if (Constants.GOUGI_TYPE_RAKKAN_HIKAN_BYTURNS.equals(GougiConfig.getInstance().getGougiType())) {
			if (Utils.isEmpty(prevGougiType)) {
				return Constants.GOUGI_TYPE_RAKKAN;
			} else if (Constants.GOUGI_TYPE_RAKKAN.equals(prevGougiType)) {
				return Constants.GOUGI_TYPE_HIKAN;
			} else {
				return Constants.GOUGI_TYPE_RAKKAN;
			}
		}
		// ���̑��̏ꍇ
		else {
			// �ݒ�l�����̂܂ܕԂ�
			return GougiConfig.getInstance().getGougiType();
		}
	}

}
