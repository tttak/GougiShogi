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

	/** �ݒ�t�@�C���̃t�@�C���� */
	public final String FILENAME = "SimpleGougiShogi.config";

	/** USI�G���W�����X�g */
	private List<UsiEngine> usiEngineList = new ArrayList<UsiEngine>();

	/**
	 * �ݒ�t�@�C���Ǎ���
	 * 
	 * @throws IOException
	 */
	public void readConfigFile() throws IOException {
		List<UsiEngine> list = new ArrayList<UsiEngine>();
		BufferedReader br = null;

		try {
			br = new BufferedReader(new FileReader(new File(getConfigFilePath())));

			String line;

			// 1�s���ǂݍ���
			while ((line = br.readLine()) != null) {
				logger.info(line);

				for (int i = 1; i <= 3; i++) {
					String str = "Engine" + i + ".Path=";

					// �i��j�uEngine1.Path=�v�Ŏn�܂�s�̏ꍇ
					if (line.startsWith(str)) {
						UsiEngine engine = new UsiEngine();
						engine.setEngineNumber(i);
						engine.setExeFile(new File(line.substring(str.length()).trim()));
						list.add(engine);
					}
				}
			}

			// �\�[�g���ʂ�USI�G���W�����X�g�ɃZ�b�g
			usiEngineList = sort(list);
		} finally {
			Utils.close(br);
		}
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
			return Utils.getMyDir().getAbsolutePath() + File.separator + FILENAME;
		} else {
			return FILENAME;
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

		for (int i = 1; i <= 3; i++) {
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

	// ------------------------------ �P����Getter&Setter END ------------------------------

}
