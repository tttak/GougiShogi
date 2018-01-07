package co.jp.shogi.gougi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * �ݒ�t�@�C���֘A�N���X
 */
public class GougiConfig {

	/** Logger */
	protected static Logger logger = Logger.getLogger(GougiConfig.class.getName());

	/** ���c�^�C�v */
	private String gougiType;
	/** USI�G���W�����X�g */
	private List<UsiEngine> usiEngineList;
	/** �l�T���G���W�� */
	private MateEngine mateEngine;

	/** ���c�^�C�v�u�l�T���G���W���Ƃ̍��c�i�ǂ݋؂̋ǖʂ��l�T���j�v�̏ꍇ�ɁA�ǂ݋؋ǖʂ�T������l�T���G���W���̐� */
	private int pvMateEngineCount = Constants.PV_MATE_ENGINE_CNT_DEFAULT;

	// ---------- Singleton�� START ----------

	private GougiConfig() {
	}

	private static class SingletonHolder {
		private static final GougiConfig instance = new GougiConfig();
	}

	public static GougiConfig getInstance() {
		return SingletonHolder.instance;
	}

	// ---------- Singleton�� END ----------

	/**
	 * �ݒ�t�@�C���Ǎ���
	 * 
	 * @throws IOException
	 */
	public void readConfigFile() throws IOException {
		List<UsiEngine> list = new ArrayList<UsiEngine>();
		BufferedReader br = null;

		// ������
		gougiType = null;
		usiEngineList = null;
		mateEngine = null;

		try {
			br = new BufferedReader(new FileReader(new File(getConfigFilePath())));

			String line;

			// 1�s���ǂݍ���
			while ((line = br.readLine()) != null) {
				logger.info(line);
				line = line.trim();

				// ���c�^�C�v�̐ݒ�
				if (line.startsWith("Gougi.Type=")) {
					// �����s�Őݒ肳��Ă����ꍇ�A�揟���Ƃ���
					if (gougiType == null) {
						gougiType = line.substring("Gougi.Type=".length()).trim();
					}
				}

				// �G���W���̐ݒ�
				for (int i = 1; i <= Constants.ENGINE_COUNT_MAX; i++) {
					String str = "Engine" + i + ".Path=";

					// �i��j�uEngine1.Path=�v�Ŏn�܂�s�̏ꍇ
					if (line.startsWith(str)) {
						// �����G���W���ԍ��������s�ɂ������ꍇ�A�揟���Ƃ���
						if (ShogiUtils.getEngine(list, i) == null) {
							UsiEngine engine = new UsiEngine();
							engine.setEngineNumber(i);
							engine.setExeFile(new File(line.substring(str.length()).trim()));
							list.add(engine);
						}
					}
				}

				// �l�T���G���W���̐ݒ�
				String str = "MateEngine.Path=";
				if (line.startsWith(str)) {
					// �l�T���G���W���̐ݒ肪�����s���݂���ꍇ�A�揟���Ƃ���
					if (mateEngine == null) {
						mateEngine = new MateEngine();
						mateEngine.setEngineNumber(0);
						mateEngine.setExeFile(new File(line.substring(str.length()).trim()));
					}
				}

				// ���c�^�C�v�u�l�T���G���W���Ƃ̍��c�i�ǂ݋؂̋ǖʂ��l�T���j�v�̏ꍇ�ɁA�ǂ݋؋ǖʂ�T������l�T���G���W���̐�
				str = "MateEngine.forPV.Count=";
				if (line.startsWith(str)) {
					pvMateEngineCount = Utils.getIntValue(line.substring(str.length()).trim(), Constants.PV_MATE_ENGINE_CNT_DEFAULT);
				}
			}

			// �\�[�g���ʂ�USI�G���W�����X�g�ɃZ�b�g
			usiEngineList = sort(list);
		} finally {
			Utils.close(br);
		}
	}

	/**
	 * �G���[���b�Z�[�W�擾�i�ݒ�t�@�C���̓��e�̃`�F�b�N�j
	 * 
	 * @return
	 */
	public String getErrorMessage() {
		// �f�o�b�O���O
		logger.info("gougiType=" + gougiType);

		if (usiEngineList == null) {
			logger.info("usiEngineList == null");
		} else {
			logger.info("usiEngineList.size()=" + usiEngineList.size());
		}

		// �G���W�������݂��Ȃ��ꍇ
		if (usiEngineList == null || usiEngineList.isEmpty()) {
			// �`�F�b�NNG
			return "�G���W���̐ݒ肪������܂���B";
		}

		// ���c�^�C�v���������Ȃ��ꍇ
		if (!Constants.GOUGI_TYPE_LIST.contains(gougiType)) {
			// �`�F�b�NNG
			return "���c�^�C�v������������܂���B";
		}

		// ���c�^�C�v���u���������c�i3�ҁj�v�̏ꍇ
		if (Constants.GOUGI_TYPE_TASUUKETSU_3.equals(gougiType)) {
			// �G���W����3�ł͂Ȃ��ꍇ
			if (usiEngineList.size() != Constants.ENGINE_COUNT_TASUUKETSU_3) {
				// �`�F�b�NNG
				return "���c�^�C�v���u���������c�i3�ҁj�v�̏ꍇ�A�G���W����3��ސݒ肵�Ă��������B";
			}
		}

		// ���c�^�C�v���u�e�X�̍őP����������ĕ]���l�̍��v�Ŕ���i2�ҁj�v�̏ꍇ
		if (Constants.GOUGI_TYPE_BESTMOVE_EXCHANGE_2.equals(gougiType)) {
			// �G���W����2�ł͂Ȃ��ꍇ
			if (usiEngineList.size() != Constants.ENGINE_COUNT_BESTMOVE_EXCHANGE_2) {
				// �`�F�b�NNG
				return "���c�^�C�v���u�e�X�̍őP����������ĕ]���l�̍��v�Ŕ���i2�ҁj�v�̏ꍇ�A�G���W����2��ސݒ肵�Ă��������B";
			}
		}

		// ���c�^�C�v���u���ՁE���ՁE�I�Ղő΋ǎҌ��v�̏ꍇ
		if (Constants.GOUGI_TYPE_CHANGE_PLAYER_JOBAN_CHUUBAN_SHUUBAN.equals(gougiType)) {
			// �G���W����3�ł͂Ȃ��ꍇ
			if (usiEngineList.size() != Constants.ENGINE_COUNT_CHANGE_PLAYER_JOBAN_CHUUBAN_SHUUBAN) {
				// �`�F�b�NNG
				return "���c�^�C�v���u���ՁE���ՁE�I�Ղő΋ǎҌ��v�̏ꍇ�A�G���W����3��ސݒ肵�Ă��������B";
			}
		}

		// ���c�^�C�v���u�l�T���G���W���Ƃ̍��c�v�̏ꍇ
		if (Constants.GOUGI_TYPE_MATE.equals(gougiType)) {
			// �G���W����1�ł͂Ȃ��ꍇ
			if (usiEngineList.size() != Constants.ENGINE_COUNT_MATE) {
				// �`�F�b�NNG
				return "���c�^�C�v���u�l�T���G���W���Ƃ̍��c�v�̏ꍇ�A�G���W���i�ʏ�j��1��ސݒ肵�Ă��������B";
			}
			// �l�T���G���W�������ݒ�̏ꍇ
			if (mateEngine == null) {
				// �`�F�b�NNG
				return "���c�^�C�v���u�l�T���G���W���Ƃ̍��c�v�̏ꍇ�A�l�T���G���W����ݒ肵�Ă��������B";
			}
		}

		// �`�F�b�NOK
		return null;
	}

	/**
	 * �ݒ�t�@�C���̃p�X���擾����
	 * �����ijar�t�@�C���j�Ɠ����t�H���_���ɂ���uSimpleGougiShogi.config�v�̐�΃p�X��Ԃ�
	 * 
	 * @return
	 */
	public String getConfigFilePath() {
		String classPath = System.getProperty("java.class.path");
		if (classPath.endsWith(".jar")) {
			return Utils.getMyDir().getAbsolutePath() + File.separator + Constants.CONFIG_FILENAME;
		} else {
			return Constants.CONFIG_FILENAME;
		}
	}

	/**
	 * USI�G���W�����X�g���G���W���ԍ��Ń\�[�g����
	 * 
	 * @param usiEngineList
	 * @return
	 */
	private List<UsiEngine> sort(List<UsiEngine> usiEngineList) {
		List<UsiEngine> list = new ArrayList<UsiEngine>();

		for (int i = 1; i <= Constants.ENGINE_COUNT_MAX; i++) {
			UsiEngine engine = ShogiUtils.getEngine(usiEngineList, i);
			if (engine != null) {
				list.add(engine);
			}
		}

		return list;
	}

	// ------------------------------ �P����Getter&Setter START ------------------------------

	public List<UsiEngine> getUsiEngineList() {
		return usiEngineList;
	}

	public String getGougiType() {
		return gougiType;
	}

	public void setGougiType(String gougiType) {
		this.gougiType = gougiType;
	}

	public MateEngine getMateEngine() {
		return mateEngine;
	}

	public int getPvMateEngineCount() {
		return pvMateEngineCount;
	}

	public void setPvMateEngineCount(int pvMateEngineCount) {
		this.pvMateEngineCount = pvMateEngineCount;
	}

	// ------------------------------ �P����Getter&Setter END ------------------------------

}
