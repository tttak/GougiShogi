package co.jp.shogi.gougi;

/**
 * �l�T���G���W���N���X
 */
public class MateEngine extends UsiEngine {

	/**
	 * checkmate�̎w���� �i��j�uR*6a 5a6a 2a4a 6a6b 8b7a�v
	 * �E�ucheckmate nomate�v�ucheckmate timeout�v�ucheckmate�̂݁v�ucheckmate notimplemented�v�̏ꍇ�̓Z�b�g����Ȃ�
	 */
	private String checkmateMoves;

	/** ���߂́ugo mate�v�R�}���h�̋ǖ� */
	private String latestGoMatePosition;

	/** �T�������ۂ� */
	private boolean searching;

	/**
	 * �\���p��������擾
	 * �i��j�u[5��l��]�@[�U���� �U��(51) �S��(21) �U��(61) �V��(82)]�@[NanohaTsumeUSI]�v
	 * �i��j�u[�l�ݖ���]�@[NanohaTsumeUSI]�v
	 * 
	 * @return
	 */
	public String getMateDisp() {
		StringBuilder sb = new StringBuilder();

		if (Utils.isEmpty(checkmateMoves)) {
			sb.append("[�l�ݖ���]");
		} else {
			sb.append("[");
			sb.append(getMateLength());
			sb.append("��l��]�@[");

			// �i��j�uR*6a 5a6a 2a4a 6a6b 8b7a�v
			String[] sa = checkmateMoves.split(" ", -1);

			for (int i = 0; i < sa.length; i++) {
				sb.append(ShogiUtils.getMoveDispJa(sa[i]));
				if (i < sa.length - 1) {
					sb.append(" ");
				}
			}

			sb.append("]");
		}

		sb.append(" [");
		sb.append(getUsiName());
		sb.append("]");

		return sb.toString();
	}

	/**
	 * �l�萔��Ԃ�
	 * @return
	 */
	public int getMateLength() {
		if (Utils.isEmpty(checkmateMoves)) {
			return -1;
		} else {
			// �i��j�uR*6a 5a6a 2a4a 6a6b 8b7a�v���u5�v
			String[] sa = checkmateMoves.split(" ", -1);
			return sa.length;
		}
	}

	/**
	 * bestmove���쐬���ĕԂ�
	 * �i��j�uR*6a�v
	 * 
	 * @return
	 */
	public String createBestmove() {
		return Utils.getSplitResult(checkmateMoves, " ", 0);
	}

	/**
	 * bestmove�R�}���h���쐬���ĕԂ�
	 * �i��j�ubestmove R*6a�v
	 * 
	 * @return
	 */
	public String createBestmoveCommand() {
		return "bestmove " + createBestmove();
	}

	/**
	 * �ǂ݋؂��쐬���ĕԂ�
	 * �i��j�uinfo score mate 5 pv R*6a 5a6a 2a4a 6a6b 8b7a�v
	 * 
	 * @return
	 */
	public String createPv() {
		if (Utils.isEmpty(checkmateMoves)) {
			return "";
		} else {
			return "info score mate " + getMateLength() + " pv " + checkmateMoves;
		}
	}

	// ------------------------------ �P����Getter&Setter START ------------------------------

	public String getCheckmateMoves() {
		return checkmateMoves;
	}

	public void setCheckmateMoves(String checkmateMoves) {
		this.checkmateMoves = checkmateMoves;
	}

	public String getLatestGoMatePosition() {
		return latestGoMatePosition;
	}

	public void setLatestGoMatePosition(String latestGoMatePosition) {
		this.latestGoMatePosition = latestGoMatePosition;
	}

	public boolean isSearching() {
		return searching;
	}

	public void setSearching(boolean searching) {
		this.searching = searching;
	}

	// ------------------------------ �P����Getter&Setter END ------------------------------

}
