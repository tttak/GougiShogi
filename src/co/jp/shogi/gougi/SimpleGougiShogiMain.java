package co.jp.shogi.gougi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * �V���v�����c�����̃��C���N���X
 */
public class SimpleGougiShogiMain {

	/** Logger */
	protected static Logger logger = Logger.getLogger(SimpleGougiShogiMain.class.getName());

	/**
	 * main���\�b�h
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			// logger�̏�����
			Utils.initLogger("/co/jp/shogi/gougi/logging.properties");

			logger.info("main() START!!");

			// execute���\�b�h���s
			SimpleGougiShogiMain obj = new SimpleGougiShogiMain();
			obj.execute();

			logger.info("main() END!!");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * execute���\�b�h
	 * 
	 * @throws Exception
	 */
	private void execute() throws Exception {
		// USI�G���W�����X�g
		List<UsiEngine> usiEngineList = null;

		try {
			// �W�����́iGUI���j����̓��͗p�X���b�h�쐬
			InputStreamThread systemInputThread = new InputStreamThread(System.in, "systemInputThread");
			systemInputThread.setDaemon(true);
			systemInputThread.start();

			// �W���o�́iGUI���j�ւ̏o�͗p�X���b�h�쐬
			OutputStreamThread systemOutputThread = new OutputStreamThread(System.out, "systemOutputThread");
			systemOutputThread.setDaemon(true);
			systemOutputThread.start();

			// �ݒ�t�@�C���Ǎ���
			GougiConfig gougiConfig = GougiConfig.getInstance();
			gougiConfig.readConfigFile();

			// �G���[���b�Z�[�W�擾�i�ݒ�t�@�C���̓��e�̃`�F�b�N�j
			// �E ���c�^�C�v�ƃG���W���������s�����̏ꍇ�A�ُ�I��������B
			String errorMessage = gougiConfig.getErrorMessage();
			if (errorMessage != null) {
				systemOutputThread.getCommandList().add("info string [Error] " + errorMessage + " [" + GougiConfig.getInstance().getConfigFilePath() + "]");
				logger.severe(errorMessage);
				Thread.sleep(500);
				return;
			}

			// �G���W�����X�g�擾
			usiEngineList = gougiConfig.getUsiEngineList();

			// �eUSI�G���W���̃v���Z�X���N������
			startEngineProcess(usiEngineList);

			// quit�t���O�Ago�t���O�Aponder�t���O
			boolean quitFlg = false;
			boolean goFlg = false;
			boolean ponderFlg = false;

			// �O��̍��c�^�C�v
			String prevGougiType = null;

			while (!quitFlg) {
				// usi�t���O�Aisready�t���O
				boolean usiFlg = false;
				boolean isreadyFlg = false;

				// �ۗ����̕W�����́iGUI���j����̃R�}���h���X�g
				// �Eponder����stop��ponderhit�͈�U���̃��X�g�ɂ��߂�
				List<String> pendingSysinCommandList = new ArrayList<String>();

				// �W�����́iGUI���j����̃R�}���h���e�G���W���ւ̃R�}���h���X�g�ɒǉ�����
				List<String> systemInputCommandList = sysinToEngines(systemInputThread, usiEngineList, ponderFlg, pendingSysinCommandList);

				// �ۗ����̕W�����́iGUI���j����̃R�}���h�����݂���ꍇ
				if (!pendingSysinCommandList.isEmpty()) {
					for (String command : pendingSysinCommandList) {
						// ponder����stop�܂���ponderhit�̏ꍇ
						if (ponderFlg && ("stop".equals(command) || "ponderhit".equals(command))) {
							ponderFlg = false;

							logger.info("ponder����stop�܂���ponderhit�̏ꍇ START");
							// ponder�I�����̏���
							endPonder(command, systemInputThread, systemOutputThread, usiEngineList);
							logger.info("ponder����stop�܂���ponderhit�̏ꍇ END");
						}
					}
				}

				if (systemInputCommandList != null) {
					// �W�����́iGUI���j����̃R�}���h�Ɂuquit�v���܂܂��ꍇ
					if (systemInputCommandList.contains("quit")) {
						quitFlg = true;
					}

					// �W�����́iGUI���j����̃R�}���h�Ɂuusi�v���܂܂��ꍇ
					if (systemInputCommandList.contains("usi")) {
						usiFlg = true;
					}

					// �W�����́iGUI���j����̃R�}���h�Ɂuisready�v���܂܂��ꍇ
					if (systemInputCommandList.contains("isready")) {
						isreadyFlg = true;
					}

					// �W�����́iGUI���j����̃R�}���h�Ɂugo�v�Ŏn�܂�R�}���h���܂܂��ꍇ
					if (Utils.containsStartsWith(systemInputCommandList, "go")) {
						goFlg = true;
						// �S�G���W����bestmove�A���߂̓ǂ݋؂��N���A
						ShogiUtils.clearBestmoveLastPv(usiEngineList);
					}

					// �W�����́iGUI���j����̃R�}���h�Ɂugo ponder�v���܂܂��ꍇ
					if (systemInputCommandList.contains("go ponder")) {
						ponderFlg = true;
					}
				}

				// �uusi�v�̏ꍇ
				if (usiFlg) {
					logger.info("�uusi�v�̏ꍇ START");
					// �e�G���W������̃R�}���h��W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�����i�uusi�v�̏ꍇ�j
					enginesToSysoutAtUsi(systemOutputThread, usiEngineList);
					logger.info("�uusi�v�̏ꍇ END");
				}

				// �uisready�v�̏ꍇ
				if (isreadyFlg) {
					logger.info("�uisready�v�̏ꍇ START");
					// �e�G���W������̃R�}���h��W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�����i�uisready�v�̏ꍇ�j
					enginesToSysoutAtIsReady(systemOutputThread, usiEngineList);
					logger.info("�uisready�v�̏ꍇ END");
				}

				// �ugo�v�̏ꍇ
				if (goFlg) {
					// ����̍��c�^�C�v���擾
					String currentGougiType = calcCurrentGougiType(prevGougiType);

					// ���c�����s
					if (executeGougi(systemOutputThread, usiEngineList, currentGougiType)) {
						// ���c�����̏ꍇ�A�ugo�v�t���O�𗎂Ƃ�
						goFlg = false;
						// �O��̍��c�^�C�v���X�V
						prevGougiType = currentGougiType;
					}
				}

				// �e�G���W������̃R�}���h��W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�����i�ʏ�̏ꍇ�j
				enginesToSysoutAtNormal(systemOutputThread, usiEngineList, goFlg);

				// sleep
				Thread.sleep(10);
			}
		} finally {
			Thread.sleep(500);

			if (usiEngineList != null) {
				for (UsiEngine engine : usiEngineList) {
					engine.destroy();
				}
			}
		}
	}

	/**
	 * �eUSI�G���W���̃v���Z�X���N������
	 * 
	 * @param usiEngineList
	 * @throws IOException
	 */
	private void startEngineProcess(List<UsiEngine> usiEngineList) throws IOException {
		// �e�G���W�������[�v
		for (UsiEngine engine : usiEngineList) {
			// �f�o�b�O�p
			logger.info("-----");
			logger.info(String.valueOf(engine.getEngineNumber()));
			logger.info(String.valueOf(engine.getExeFile()));
			logger.info(engine.getUsiName());
			logger.info("-----");

			// ProcessBuilder���쐬
			String[] cmds = { engine.getExeFile().getAbsolutePath() };
			ProcessBuilder pb = new ProcessBuilder(cmds);
			// exe�t�@�C���̃t�H���_����ƃf�B���N�g���Ƃ��Ďw��
			pb.directory(engine.getExeFile().getParentFile());
			pb.redirectErrorStream(true);

			// �G���W���̃v���Z�X���N��
			Process process = pb.start();
			engine.setProcess(process);

			// �G���W������̓��͗p�X���b�h�쐬
			InputStreamThread processInputThread = new InputStreamThread(process.getInputStream(), engine.getEngineDisp());
			processInputThread.setDaemon(true);
			processInputThread.start();
			engine.setInputThread(processInputThread);

			// �G���W���ւ̏o�͗p�X���b�h�쐬
			OutputStreamThread processOutputThread = new OutputStreamThread(process.getOutputStream(), engine.getEngineDisp());
			processOutputThread.setDaemon(true);
			processOutputThread.start();
			engine.setOutputThread(processOutputThread);
		}
	}

	/**
	 * �W�����́iGUI���j����̃R�}���h���e�G���W���ւ̃R�}���h���X�g�ɒǉ�����
	 * 
	 * @param systemInputThread
	 * @param usiEngineList
	 * @param ponderFlg
	 * @param pendingCommandList
	 * @return
	 */
	private List<String> sysinToEngines(InputStreamThread systemInputThread, List<UsiEngine> usiEngineList, boolean ponderFlg, List<String> pendingCommandList) {
		List<String> systemInputCommandList = null;

		// �W�����́iGUI���j����̓��͗p�X���b�h�̃R�}���h���X�g����ł͂Ȃ��ꍇ�A���[�J���ϐ��ɃR�s�[���Ă���N���A
		// �E�X���b�h�̔r������
		synchronized (systemInputThread) {
			if (!systemInputThread.getCommandList().isEmpty()) {
				systemInputCommandList = new ArrayList<String>(systemInputThread.getCommandList());
				systemInputThread.getCommandList().clear();
			}
		}

		// �R�}���h�����݂���ꍇ
		if (systemInputCommandList != null) {
			for (UsiEngine engine : usiEngineList) {
				// �e�G���W���ւ̃R�}���h���X�g�ɒǉ�
				// �E�X���b�h�̔r������
				synchronized (engine.getOutputThread()) {
					for (String command : systemInputCommandList) {
						// �W�����́iGUI���j����̃R�}���h��ҏW�isetoption�Ή��Aponder�Ή��j
						String cmd = editSysinCommand(command, engine, ponderFlg, pendingCommandList);
						if (!Utils.isEmpty(cmd)) {
							engine.getOutputThread().getCommandList().add(cmd);
						}
					}
				}
			}
		}

		return systemInputCommandList;
	}

	/**
	 * �W�����́iGUI���j����̃R�}���h��ҏW�isetoption�Ή��Aponder�Ή��j
	 * �E�ʏ��command�����̂܂ܕԂ��B
	 * �Esetoption�̏ꍇ��ponder�̏ꍇ�A�ҏW��̃R�}���h��Ԃ��B�inull��Ԃ����Ƃ�����j
	 * 
	 * @param command
	 * @param engine
	 * @param ponderFlg
	 * @param pendingCommandList
	 * @return
	 */
	private String editSysinCommand(String command, UsiEngine engine, boolean ponderFlg, List<String> pendingCommandList) {
		// �usetoption�v�̏ꍇ
		if (command.startsWith("setoption ")) {
			return editSysinCommandAtSetOption(command, engine);
		}

		// ponder����stop�܂���ponderhit�̏ꍇ�ApendingCommandList�ɒǉ����Anull��Ԃ�
		if (ponderFlg && ("stop".equals(command) || "ponderhit".equals(command))) {
			pendingCommandList.add(command);
			return null;
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
				setEnginePonderFlg(engine, command);
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
					setEnginePonderFlg(engine, command);
				}

				String prefix = "setoption name " + engine.getOptionNamePrefix();
				// �i��j�usetoption name OwnBook value true�v
				return "setoption name " + command.substring(prefix.length()).trim();
			}
		}

		// �i��j�uE2_�v�Ŏn�܂�I�v�V�����́A�G���W��1�ƃG���W��3�̏ꍇ��null��Ԃ��i�G���W��2�p�̃I�v�V�����̂͂��Ȃ̂Łj
		return null;
	}

	/**
	 * �G���W����ponder�t���O���Z�b�g
	 * �E�O��F���Y�G���W����USI_Ponder��setoption�R�}���h�ł��邱��
	 * 
	 * @param engine
	 * @param command
	 */
	private void setEnginePonderFlg(UsiEngine engine, String command) {
		try {
			// �i��j�usetoption name USI_Ponder value true�v���utrue�v
			// �i��j�usetoption name E2_USI_Ponder value true�v���utrue�v
			String val = Utils.getSplitResult(command, " ", 4);
			engine.setPonderFlg("true".equals(val));

			logger.info("[" + engine.getEngineDisp() + "]command=" + command);
			logger.info("[" + engine.getEngineDisp() + "]engine.isPonderFlg()=" + engine.isPonderFlg());
		} catch (Exception e) {
			// �������Ȃ�
		}
	}

	/**
	 * �e�G���W������̃R�}���h��W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�����i�uusi�v�̏ꍇ�j
	 * 
	 * @param systemOutputThread
	 * @param usiEngineList
	 */
	private void enginesToSysoutAtUsi(OutputStreamThread systemOutputThread, List<UsiEngine> usiEngineList) {
		// �uid name�`�v�Ɓuid author�`�v�͊e�G���W������ł͂Ȃ��A���̃v���O�����Ǝ��ŕԂ�
		// �E�X���b�h�̔r������
		synchronized (systemOutputThread) {
			systemOutputThread.getCommandList().add("id name " + Constants.USI_ID_NAME);
			systemOutputThread.getCommandList().add("id author " + Constants.USI_ID_AUTHOR);
		}

		// GUI�ɕԂ��I���܂ő҂�
		systemOutputThread.waitUntilEmpty();

		// �e�G���W�������[�v
		for (UsiEngine engine : usiEngineList) {
			InputStreamThread processInputThread = engine.getInputThread();

			// �G���W������uusiok�v���Ԃ��Ă���܂ő҂�
			processInputThread.waitUntilCommand("usiok");

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
				}
			}

			// �W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�
			// �E�X���b�h�̔r������
			synchronized (systemOutputThread) {
				systemOutputThread.getCommandList().addAll(new ArrayList<String>(systemOutputCommandList));
			}

			// GUI�ɕԂ��I���܂ő҂�
			systemOutputThread.waitUntilEmpty();
		}

		// GUI�Ɂuusiok�v��1�񂾂��Ԃ�
		// �E�X���b�h�̔r������
		synchronized (systemOutputThread) {
			systemOutputThread.getCommandList().add("usiok");
		}

		// GUI�ɕԂ��I���܂ő҂�
		systemOutputThread.waitUntilEmpty();
	}

	/**
	 * �e�G���W������̃R�}���h��W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�����i�uisready�v�̏ꍇ�j
	 * 
	 * @param systemOutputThread
	 * @param usiEngineList
	 */
	private void enginesToSysoutAtIsReady(OutputStreamThread systemOutputThread, List<UsiEngine> usiEngineList) {
		// �e�G���W�������[�v
		for (UsiEngine engine : usiEngineList) {
			InputStreamThread processInputThread = engine.getInputThread();

			// �ureadyok�v���Ԃ��Ă���܂ő҂�
			processInputThread.waitUntilCommand("readyok");
			// �ureadyok�v���폜
			processInputThread.getCommandList().remove("readyok");
		}

		// ���ׂẴG���W������ureadyok�v���Ԃ��Ă�����AGUI�Ɂureadyok�v��1�񂾂��Ԃ�
		// �E�X���b�h�̔r������
		synchronized (systemOutputThread) {
			systemOutputThread.getCommandList().add("readyok");
		}

		// GUI�ɕԂ��I���܂ő҂�
		systemOutputThread.waitUntilEmpty();
	}

	/**
	 * �e�G���W������̃R�}���h��W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�����i�ʏ�̏ꍇ�j
	 * 
	 * @param systemOutputThread
	 * @param usiEngineList
	 * @param goFlg
	 */
	private void enginesToSysoutAtNormal(OutputStreamThread systemOutputThread, List<UsiEngine> usiEngineList, boolean goFlg) {
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
						if (goFlg && command.startsWith("bestmove")) {
							// bestmove���G���W���ɕۑ�
							engine.setBestmoveCommand(command);
							// �i��j�uinfo string bestmove 7g7f [�V�Z(77)] [�]���l 123 �i�O��100 ����23�j] [Gikou 20160606]�v
							// [ponder�Ή�]
							// �i��j�uinfo string bestmove 7g7f ponder 3c3d [�V�Z(77) �R�l(33)] [�]���l 123 �i�O��100 ����23�j] [Gikou 20160606]�v
							systemOutputThread.getCommandList().add("info string " + engine.getBestmoveScoreDisp(true));
						}

						// ���̑��̏ꍇ�A�W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�
						else {
							systemOutputThread.getCommandList().add(command);

							// �uinfo�`score�`�v�̏ꍇ�A���߂̓ǂ݋؂��G���W���ɕۑ�
							if (command.startsWith("info ") && command.indexOf("score ") >= 0) {
								engine.setLastPv(command);
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
				if (!Utils.isEmpty(resultEngine.getLastPv())) {
					systemOutputThread.getCommandList().add(resultEngine.getLastPv());
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
					systemOutputThread.getCommandList().add("info string [" + hantei + "] " + engine.getBestmoveScoreDisp(true));

					// �y�O��̒l�z��ۑ�
					engine.savePrevValue();
				}

				// bestmove��GUI�֕Ԃ�
				// TODO [ponder�Ή�]
				systemOutputThread.getCommandList().add(bestmoveCommand);
				// systemOutputThread.getCommandList().add(resultEngine.getBestmoveCommand());
			}

			// ���c����
			return true;
		}

		return false;
	}

	/**
	 * ����̍��c�^�C�v���Z�o
	 * 
	 * @param prevGougiType
	 * @return
	 */
	private String calcCurrentGougiType(String prevGougiType) {
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
		// TODO

	}

}
