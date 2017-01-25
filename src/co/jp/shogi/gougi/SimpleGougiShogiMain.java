package co.jp.shogi.gougi;

import java.io.IOException;
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

			// ���c�^�C�v���u�e�X�̍őP����������ĕ]���l�̍��v�Ŕ���i2�ҁj�v�̏ꍇ
			if (Constants.GOUGI_TYPE_BESTMOVE_EXCHANGE_2.equals(gougiConfig.getGougiType())) {
				// USI���W�b�N2�����s
				UsiLogic2 usiLogic2 = new UsiLogic2();
				usiLogic2.execute(usiEngineList, systemInputThread, systemOutputThread);
			}
			// ���̑��̏ꍇ
			else {
				// USI���W�b�N1�����s
				UsiLogic1 usiLogic1 = new UsiLogic1();
				usiLogic1.execute(usiEngineList, systemInputThread, systemOutputThread);
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

}
