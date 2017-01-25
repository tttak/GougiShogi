package co.jp.shogi.gougi;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * ���c���W�b�N
 */
public class GougiLogic {

	/** Logger */
	protected static Logger logger = Logger.getLogger(GougiLogic.class.getName());

	/** ���c�^�C�v */
	private String gougiType;

	/** USI�G���W�����X�g */
	private List<UsiEngine> usiEngineList;

	/**
	 * �R���X�g���N�^
	 * 
	 * @param gougiType
	 * @param usiEngineList
	 */
	public GougiLogic(String gougiType, List<UsiEngine> usiEngineList) {
		super();
		this.gougiType = gougiType;
		this.usiEngineList = usiEngineList;
	}

	/**
	 * ���c�����s
	 * 
	 * @return
	 */
	public UsiEngine execute() {
		// ���������c�i3�ҁj�̏ꍇ
		if (Constants.GOUGI_TYPE_TASUUKETSU_3.equals(gougiType)) {
			return gougi_tasuuketsu_3();
		}

		// �y�ύ��c�̏ꍇ
		else if (Constants.GOUGI_TYPE_RAKKAN.equals(gougiType)) {
			return gougi_rakkan();
		}

		// �ߊύ��c�̏ꍇ
		else if (Constants.GOUGI_TYPE_HIKAN.equals(gougiType)) {
			return gougi_hikan();
		}

		// 2��O�̕]���l����̏㏸���̊y�ύ��c�̏ꍇ
		else if (Constants.GOUGI_TYPE_2TEMAE_JOUSHOU_RAKKAN.equals(gougiType)) {
			return gougi_2temae_joushou_rakkan();
		}

		// 2��O�̕]���l����̏㏸���̔ߊύ��c�̏ꍇ
		else if (Constants.GOUGI_TYPE_2TEMAE_JOUSHOU_HIKAN.equals(gougiType)) {
			return gougi_2temae_joushou_hikan();
		}

		// �u�e�X�̍őP����������ĕ]���l�̍��v�Ŕ���i2�ҁj�v�̏ꍇ
		else if (Constants.GOUGI_TYPE_BESTMOVE_EXCHANGE_2.equals(gougiType)) {
			return gougi_bestmove_exchange_2();
		}

		// ���̑��̏ꍇ
		else {
			return null;
		}
	}

	/**
	 * ���������c�i3�ҁj�����s
	 * �E�������Ō���
	 * �E�O�ҎO�l�̏ꍇ�̓G���W��1�̎w������̗p����
	 * �����̃��\�b�h�͖߂�l���G���W�������A�������Ō��肳�ꂽ�ꍇ�́A���̎w����̒��ŃG���W���ԍ����ŏ��̃G���W����Ԃ�
	 * 
	 * @return
	 */
	private UsiEngine gougi_tasuuketsu_3() {
		// bestmove���Ԃ��Ă��Ă��Ȃ��G���W�������݂���ꍇ
		if (ShogiUtils.containsEmptyBestmove(usiEngineList)) {
			return null;
		}

		// bestmove���Ƃ̌���Map�iponder�����������j���쐬
		Map<String, Integer> bestmoveCountMap = ShogiUtils.createBestmoveCountMap(usiEngineList);

		// �O�ҎO�l�̏ꍇ
		if (bestmoveCountMap.size() >= Constants.ENGINE_COUNT_TASUUKETSU_3) {
			// �G���W��1�̎w������̗p����
			return usiEngineList.get(0);
		}

		// ���̑��̏ꍇ�i�u3��0�v�܂��́u2��1�v�̏ꍇ�j
		else {
			for (String bestmoveCommand : bestmoveCountMap.keySet()) {
				// �u3��0�v�܂��́u2��1�v�������肦�Ȃ��̂ŁA2�[�ȏ�Ŋm��
				if (bestmoveCountMap.get(bestmoveCommand) >= 2) {
					// bestmove�iponder�����������j����v����G���W���̂����A�G���W���ԍ����ŏ��̃G���W����Ԃ�
					return ShogiUtils.getMinEngine(usiEngineList, bestmoveCommand);
				}
			}
		}

		return null;
	}

	/**
	 * �y�ύ��c�����s
	 * 
	 * @return
	 */
	private UsiEngine gougi_rakkan() {
		// bestmove���Ԃ��Ă��Ă��Ȃ��G���W�������݂���ꍇ
		if (ShogiUtils.containsEmptyBestmove(usiEngineList)) {
			return null;
		}

		// �߂�l�p
		UsiEngine result = null;
		// �]���l�̍ő�l
		int max_score = Integer.MIN_VALUE;

		// �]���l���ő�̃G���W�������߂�
		for (UsiEngine engine : usiEngineList) {
			int score = engine.getLatestScore();
			if (score != Constants.SCORE_NONE && score > max_score) {
				result = engine;
				max_score = score;
			}
		}

		// ���̒i�K�ō��c���ʂ����܂�Ȃ��ꍇ�A�G���W��1���Z�b�g
		// �E���ׂẴG���W�����uSCORE_NONE�v�̏ꍇ�Ȃ�
		if (result == null) {
			result = usiEngineList.get(0);
		}

		return result;
	}

	/**
	 * �ߊύ��c�����s
	 * 
	 * @return
	 */
	private UsiEngine gougi_hikan() {
		// bestmove���Ԃ��Ă��Ă��Ȃ��G���W�������݂���ꍇ
		if (ShogiUtils.containsEmptyBestmove(usiEngineList)) {
			return null;
		}

		// �߂�l�p
		UsiEngine result = null;
		// �]���l�̍ŏ��l
		int min_score = Integer.MAX_VALUE;

		// �]���l���ŏ��̃G���W�������߂�
		for (UsiEngine engine : usiEngineList) {
			int score = engine.getLatestScore();
			if (score != Constants.SCORE_NONE && score < min_score) {
				result = engine;
				min_score = score;
			}
		}

		// ���̒i�K�ō��c���ʂ����܂�Ȃ��ꍇ�A�G���W��1���Z�b�g
		// �E���ׂẴG���W�����uSCORE_NONE�v�̏ꍇ�Ȃ�
		if (result == null) {
			result = usiEngineList.get(0);
		}

		return result;
	}

	/**
	 * 2��O�̕]���l����̏㏸���̊y�ύ��c�����s
	 * 
	 * @return
	 */
	private UsiEngine gougi_2temae_joushou_rakkan() {
		// bestmove���Ԃ��Ă��Ă��Ȃ��G���W�������݂���ꍇ
		if (ShogiUtils.containsEmptyBestmove(usiEngineList)) {
			return null;
		}

		// �߂�l�p
		UsiEngine result = null;
		// �u2��O�̕]���l����̏㏸���v�̍ő�l
		int max_score = Integer.MIN_VALUE;

		// �u2��O�̕]���l����̏㏸���v���ő�̃G���W�������߂�
		for (UsiEngine engine : usiEngineList) {
			int score = engine.getScore2TemaeJoushou();
			if (score != Constants.SCORE_NONE && score > max_score) {
				result = engine;
				max_score = score;
			}
		}

		// ���̒i�K�ō��c���ʂ����܂�Ȃ��ꍇ�A�G���W��1���Z�b�g
		// �E���ׂẴG���W�����uSCORE_NONE�v�̏ꍇ�Ȃ�
		if (result == null) {
			result = usiEngineList.get(0);
		}

		return result;
	}

	/**
	 * 2��O�̕]���l����̏㏸���̔ߊύ��c�����s
	 * 
	 * @return
	 */
	private UsiEngine gougi_2temae_joushou_hikan() {
		// bestmove���Ԃ��Ă��Ă��Ȃ��G���W�������݂���ꍇ
		if (ShogiUtils.containsEmptyBestmove(usiEngineList)) {
			return null;
		}

		// �߂�l�p
		UsiEngine result = null;
		// �u2��O�̕]���l����̏㏸���v�̍ŏ��l
		int min_score = Integer.MAX_VALUE;

		// �u2��O�̕]���l����̏㏸���v���ŏ��̃G���W�������߂�
		for (UsiEngine engine : usiEngineList) {
			int score = engine.getScore2TemaeJoushou();
			if (score != Constants.SCORE_NONE && score < min_score) {
				result = engine;
				min_score = score;
			}
		}

		// ���̒i�K�ō��c���ʂ����܂�Ȃ��ꍇ�A�G���W��1���Z�b�g
		// �E���ׂẴG���W�����uSCORE_NONE�v�̏ꍇ�Ȃ�
		if (result == null) {
			result = usiEngineList.get(0);
		}

		return result;
	}

	/**
	 * �u�e�X�̍őP����������ĕ]���l�̍��v�Ŕ���i2�ҁj�v�̍��c�����s
	 * 
	 * @return
	 */
	private UsiEngine gougi_bestmove_exchange_2() {
		// bestmove���Ԃ��Ă��Ă��Ȃ��G���W�������݂���ꍇ
		if (ShogiUtils.containsEmptyBestmove(usiEngineList)) {
			return null;
		}

		// ���c�^�C�v�u�e�X�̍őP����������ĕ]���l�̍��v�Ŕ���i2�ҁj�v�̏ꍇ�A�G���W���͕K��2��
		UsiEngine engine1 = usiEngineList.get(0);
		UsiEngine engine2 = usiEngineList.get(1);

		// �����O�̕]���l
		int bf_score1 = engine1.get_before_exchange_LatestScore();
		int bf_score2 = engine2.get_before_exchange_LatestScore();

		// ������̕]���l
		// �E�G���W�����t�ɂ��āA-1�{����
		int af_score1 = -engine2.getLatestScore();
		int af_score2 = -engine1.getLatestScore();

		// �]���l�̘a
		int sum_score1 = bf_score1 + af_score1;
		int sum_score2 = bf_score2 + af_score2;

		// �]���l�̘a�Ŕ��肷��
		if (sum_score1 >= sum_score2) {
			return engine1;
		} else {
			return engine2;
		}
	}

	// ------------------------------ �P����Getter&Setter START ------------------------------

	public String getGougiType() {
		return gougiType;
	}

	public void setGougiType(String gougiType) {
		this.gougiType = gougiType;
	}

	public List<UsiEngine> getUsiEngineList() {
		return usiEngineList;
	}

	public void setUsiEngineList(List<UsiEngine> usiEngineList) {
		this.usiEngineList = usiEngineList;
	}

	// ------------------------------ �P����Getter&Setter END ------------------------------

}
