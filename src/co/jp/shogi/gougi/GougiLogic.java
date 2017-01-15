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
		if (bestmoveCountMap.size() >= Constants.TASUUKETSU_3_ENGINE_COUNT) {
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
			int score = engine.getLastScore();
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

		// �]���l���ő�̃G���W�������߂�
		for (UsiEngine engine : usiEngineList) {
			int score = engine.getLastScore();
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