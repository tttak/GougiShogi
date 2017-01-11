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

			// �G���W�����X�g�쐬
			usiEngineList = createEngineList();

			// �G���W����3�ł͂Ȃ��ꍇ�A�ُ�I��
			if (usiEngineList.size() != 3) {
				systemOutputThread.getCommandList().add("info string [Error] " + new GougiConfig().getConfigFilePath());
				return;
			}

			// �eUSI�G���W���̃v���Z�X���N������
			startEngineProcess(usiEngineList);

			// quit�t���O�Ago�t���O
			boolean quitFlg = false;
			boolean goFlg = false;

			while (!quitFlg) {
				// usi�t���O�Aisready�t���O
				boolean usiFlg = false;
				boolean isreadyFlg = false;

				// �W�����́iGUI���j����̃R�}���h���e�G���W���ւ̃R�}���h���X�g�ɒǉ�����
				List<String> systemInputCommandList = sysinToEngines(systemInputThread, usiEngineList);

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
					// ���c�����s
					if (executeGougi(systemOutputThread, usiEngineList)) {
						// ���c�����̏ꍇ�A�ugo�v�t���O�𗎂Ƃ�
						goFlg = false;
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
	 * �G���W�����X�g�쐬
	 * 
	 * @return
	 * @throws IOException
	 */
	private List<UsiEngine> createEngineList() throws IOException {
		GougiConfig gougiConfig = new GougiConfig();
		// �ݒ�t�@�C���Ǎ���
		gougiConfig.readConfigFile();
		return gougiConfig.getUsiEngineList();
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
	 * @return
	 */
	private List<String> sysinToEngines(InputStreamThread systemInputThread, List<UsiEngine> usiEngineList) {
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
						// �R�}���h��ҏW�i�usetoption�v�Ή��j
						String cmd = editSysinCommand(command, engine);
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
	 * �R�}���h��ҏW
	 * �E�ʏ��command�����̂܂ܕԂ�
	 * �E�usetoption�v�̏ꍇ�A�Ⴆ�΁uE2_OwnBook�v���uOwnBook�v�֖߂��i�uoption name�v�̂Ƃ��ɁuOwnBook�v���uE2_OwnBook�v�ɕϊ������̂Łj
	 * �i��j�uoption name E2_OwnBook type check default true�v
	 * ���uoption name OwnBook type check default true�v�i�G���W��2�̏ꍇ�̂݁j
	 * 
	 * @param command
	 * @param engine
	 * @return
	 */
	private String editSysinCommand(String command, UsiEngine engine) {
		// �usetoption�v�ȊO�̏ꍇ
		if (!command.startsWith("setoption ")) {
			// command�����̂܂ܕԂ�
			return command;
		}

		// ----- �ȉ��A�usetoption�v�̏ꍇ
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
			return null;
		}

		// �i��j�uUSI_Hash�v
		if (option.startsWith("USI")) {
			// command�����̂܂ܕԂ�
			return command;
		}

		// �i��j�uE2_�v�Ŏn�܂�ꍇ
		if (option.startsWith(engine.getOptionNamePrefix())) {
			// �i��j�uE2_OwnBook�v���uOwnBook�v
			String str = option.substring(engine.getOptionNamePrefix().length());

			// ���Y�G���W����USI�I�v�V�����Ɋ܂܂��ꍇ�i�O�̂��߃`�F�b�N�j
			if (engine.getOptionSet().contains(str)) {
				String prefix = "setoption name " + engine.getOptionNamePrefix();
				// �i��j�usetoption name OwnBook value true�v
				return "setoption name " + command.substring(prefix.length()).trim();
			}
		}

		// �i��j�uE2_�v�Ŏn�܂�I�v�V�����́A�G���W��1�ƃG���W��3�̏ꍇ��null��Ԃ��i�G���W��2�p�̃I�v�V�����̂͂��Ȃ̂Łj
		return null;
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
							// �i��j�uinfo string bestmove 7g7f [�V�Z(77)] [�]���l 123] [Gikou 20160606]�v
							systemOutputThread.getCommandList().add("info string " + engine.getBestmoveScoreDisp());
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
	 * @return
	 */
	private boolean executeGougi(OutputStreamThread systemOutputThread, List<UsiEngine> usiEngineList) {
		// bestmove���Ԃ��Ă��Ă��Ȃ��G���W�������݂���ꍇ
		if (ShogiUtils.containsEmptyBestmove(usiEngineList)) {
			return false;
		}

		// ���c�����s���A���c���ʂ�bestmove�R�}���h���擾�iponder�����������j
		// �E���c���̂͂���ŏI��
		String bestmoveCommand = ShogiUtils.getGougiBestmoveCommandExceptPonder(usiEngineList);

		if (!Utils.isEmpty(bestmoveCommand)) {
			// ���c���ʂ�W���o�́iGUI���j�ւ̃R�}���h���X�g�ɒǉ�
			// �E�X���b�h�̔r������
			synchronized (systemOutputThread) {
				// ���c�Ŏw���肪�̗p���ꂽ�G���W���̂����A�G���W���ԍ����ŏ��̃G���W���̓ǂ݋؁i�]���l���܂ށj���ēx�o�͂��Ă���
				// �E���������ł́Abestmove���O�̓ǂ݋؁i�uinfo�`score�`pv�`�v�j���]���l�E�ǂ݋؂Ƃ��ĕ\������邽�߁A�w���肪�̗p����Ȃ������G���W�������߂��Ǝw����Ɠǂ݋؂���������̂ŁB
				UsiEngine minEngine = ShogiUtils.getMinEngine(usiEngineList, bestmoveCommand);
				if (minEngine != null) {
					if (!Utils.isEmpty(minEngine.getLastPv())) {
						systemOutputThread.getCommandList().add(minEngine.getLastPv());
					}
				}

				// �e�G���W����bestmove���Q�l���Ƃ���GUI�֏o�͂���
				// �EGUI�Ō��₷���悤�ɃG���W���͋t���ɂ��Ă����i�G���W��1����ɕ\�������悤�Ɂj
				for (int i = usiEngineList.size() - 1; i >= 0; i--) {
					UsiEngine engine = usiEngineList.get(i);

					// �i��j�ubestmove 7g7f�v
					String engineBest = engine.getBestmoveCommandExceptPonder();
					// ���̃G���W���̎w���肪�̗p���ꂽ���ۂ��B�ۂƃo�c�����A�uO�v�ƁuX�v�ő�ւ���B
					String hantei = engineBest.equals(bestmoveCommand) ? "O" : "X";
					// �i��j�uinfo string [O] bestmove 7g7f [�V�Z(77)] [�]���l 123] [Gikou 20160606]�v
					systemOutputThread.getCommandList().add("info string [" + hantei + "] " + engine.getBestmoveScoreDisp());
				}

				// bestmove��GUI�֕Ԃ�
				systemOutputThread.getCommandList().add(bestmoveCommand);
			}

			// ���c����
			return true;
		}

		return false;
	}

}
