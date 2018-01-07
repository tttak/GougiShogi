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
			logger.warning(ex.getMessage());
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
			// ���c�^�C�v���ȉ��̂����ꂩ�̏ꍇ
			// �E�u���育�Ƃɑ΋ǎҌ��v
			// �E�u���ՁE���ՁE�I�Ղő΋ǎҌ��v
			// �E�u2��O�̕]���l������l�ȏ㉺�~������΋ǎҌ��v
			// �E�u2��O�̕]���l������l�ȏ�㏸������΋ǎҌ��v
			else if (Constants.GOUGI_TYPE_CHANGE_PLAYER_PLYS.equals(gougiConfig.getGougiType()) || Constants.GOUGI_TYPE_CHANGE_PLAYER_JOBAN_CHUUBAN_SHUUBAN.equals(gougiConfig.getGougiType()) || Constants.GOUGI_TYPE_CHANGE_PLAYER_SCORE_DOWN.equals(gougiConfig.getGougiType()) || Constants.GOUGI_TYPE_CHANGE_PLAYER_SCORE_UP.equals(gougiConfig.getGougiType())) {
				// USI���W�b�N3�����s
				UsiLogic3 usiLogic3 = new UsiLogic3();
				usiLogic3.execute(usiEngineList, systemInputThread, systemOutputThread);
			}
			// ���c�^�C�v���u�l�T���G���W���Ƃ̍��c�v�̏ꍇ
			else if (Constants.GOUGI_TYPE_MATE.equals(gougiConfig.getGougiType())) {
				// USI���W�b�N4�����s
				UsiLogic4 usiLogic4 = new UsiLogic4();
				usiLogic4.execute(usiEngineList, systemInputThread, systemOutputThread);
			}
			// ���c�^�C�v���u�l�T���G���W���Ƃ̍��c�i�u�Ҕ��l�v�Ή��Łj�v�̏ꍇ
			else if (Constants.GOUGI_TYPE_MATE_SEOTSUME.equals(gougiConfig.getGougiType())) {
				// USI���W�b�N5�����s
				UsiLogic5 usiLogic5 = new UsiLogic5();
				usiLogic5.execute(usiEngineList, systemInputThread, systemOutputThread);
			}
			// ���c�^�C�v���u�l�T���G���W���Ƃ̍��c�i�ǂ݋؂̋ǖʂ��l�T���j�v�̏ꍇ
			else if (Constants.GOUGI_TYPE_MATE_PV.equals(gougiConfig.getGougiType())) {
				// USI���W�b�N6�����s
				UsiLogic6 usiLogic6 = new UsiLogic6();
				usiLogic6.execute(usiEngineList, systemInputThread, systemOutputThread);
			}
			// ���̑��̏ꍇ
			// ���Ȃ킿�A���c�^�C�v���ȉ��̂����ꂩ�̏ꍇ
			// �E�u���������c�i3�ҁj�v
			// �E�u�y�ύ��c�v
			// �E�u�ߊύ��c�v
			// �E�u�y�ύ��c�Ɣߊύ��c�����݁v
			// �E�u2��O�̕]���l����̏㏸���̊y�ύ��c�v
			// �E�u2��O�̕]���l����̏㏸���̔ߊύ��c�v
			else {
				// USI���W�b�N1�����s
				UsiLogic1 usiLogic1 = new UsiLogic1();
				usiLogic1.execute(usiEngineList, systemInputThread, systemOutputThread);
			}

		} finally {
			Thread.sleep(500);

			// USI�G���W�����X�g
			if (usiEngineList != null) {
				for (UsiEngine engine : usiEngineList) {
					engine.destroy();
				}
			}

			// �l�T���G���W��
			MateEngine mateEngine = GougiConfig.getInstance().getMateEngine();
			if (mateEngine != null) {
				mateEngine.destroy();
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
			// �v���Z�X�̍쐬�i�N���j
			engine.createProcess();
		}
	}

}
