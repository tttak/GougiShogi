package co.jp.shogi.gougi;

import java.util.logging.Logger;

/**
 * ���݂̏�Ԃ�ێ�����N���X
 */
public class StateInfo {

	/** Logger */
	protected static Logger logger = Logger.getLogger(StateInfo.class.getName());

	/** �΋ǒ����ۂ� */
	private boolean duringGame;
	/** �����̎�Ԃ��ۂ� */
	private boolean myTurn;
	/** ponder���{�����ۂ� */
	private boolean pondering = false;

	/** ���߂́uposition�v�R�}���h�̋ǖʁi�ugo�iponder�ł͂Ȃ��j�v���ugo ponder�v���͖��Ȃ��j */
	// �i��j�uposition startpos moves 7g7f�v
	// �i��j�uposition startpos�v
	private String latestPosition;

	/** ���߂́ugo�iponder�ł͂Ȃ��j�v�R�}���h�̋ǖ� */
	private String latestGoNotPonderPosition;
	/** ���߂́ugo ponder�v�R�}���h�̋ǖ� */
	private String latestGoPonderPosition;

	// ----- ���Ԍv���p

	/** �W�����́iGUI�j����̒��߂́ugo�v�R�}���h�܂��́ugo ponder�v�R�}���h */
	private String latestSysinGoCommand;
	/** �W�����́iGUI�j����̒��߂́ugo�v�R�}���h�܂��́ugo ponder�v�R�}���h���̃^�C���X�^���v */
	private long nanoTimeAtlatestSysinGoCommand;

	// ----- ���c�^�C�v�u�e�X�̍őP����������ĕ]���l�̍��v�Ŕ���i2�ҁj�v�̏ꍇ�Ɏg�p
	/** �őP��̌����O���ۂ� */
	private boolean before_exchange_flg = true;

	// ----- ���c�^�C�v�u���育�Ƃɑ΋ǎҌ��v�̏ꍇ�Ɏg�p
	/** �΋ǎ҂���シ��萔 */
	private int changePlayerPlys = 1;

	// ----- ���c�^�C�v�u2��O�̕]���l������l�ȏ㉺�~������΋ǎҌ��v�u2��O�̕]���l������l�ȏ�㏸������΋ǎҌ��v�̏ꍇ�Ɏg�p
	/** �΋ǎ҂���シ��]���l�̍��� */
	private int changePlayerScoreDiff = 100;

	// ----- ���c�^�C�v�u�l�T���G���W���Ƃ̍��c�v�̏ꍇ�Ɏg�p
	/** �l�T���G���W���̃^�C���A�E�g�i�~���b�j */
	private int mateTimeout = 10000;

	// ---------- Singleton�� START ----------

	private StateInfo() {
	}

	private static class SingletonHolder {
		private static final StateInfo instance = new StateInfo();
	}

	public static StateInfo getInstance() {
		return SingletonHolder.instance;
	}

	// ---------- Singleton�� END ----------

	/**
	 * �N���A����
	 */
	public void clear() {
		myTurn = false;
		latestGoNotPonderPosition = null;
		latestGoPonderPosition = null;
		latestPosition = null;
	}

	/**
	 * �W�����́iGUI�j����̒��߂́ugo�v�R�}���h�܂��́ugo ponder�v�R�}���h���Z�b�g
	 * �E�^�C���X�^���v���Z�b�g����
	 * 
	 * @param latestSysinGoCommand
	 */
	public void setLatestSysinGoCommand(String latestSysinGoCommand) {
		this.latestSysinGoCommand = latestSysinGoCommand;
		this.nanoTimeAtlatestSysinGoCommand = System.nanoTime();
	}

	/**
	 * ��������肩�ۂ���Ԃ�
	 * �E���߂́uposition�v�R�}���h��moves�̌�̎w����̐��Ŕ��肷��
	 * �i��j�uposition startpos�v�@���@���
	 * �i��j�uposition startpos moves 7g7f�v�@���@���
	 * �i��j�uposition startpos moves 7g7f 3c3d�v�@���@���
	 * 
	 * @return
	 */
	public boolean isSente() {
		// ���߂́uposition�v�R�}���h�����݂��Ȃ��ꍇ�A����s�\����true��Ԃ��Ă���
		if (Utils.isEmpty(latestPosition)) {
			return true;
		}

		// ���߂́uposition�v�R�}���h���uposition startpos�v�Ŏn�܂�Ȃ��ꍇ�A����s�\����true��Ԃ��Ă���
		if (!latestPosition.startsWith("position startpos")) {
			return true;
		}

		// ���
		if (latestPosition.equals("position startpos")) {
			return true;
		}

		// ����s�\����true��Ԃ��Ă���
		if (!latestPosition.startsWith("position startpos moves ")) {
			return true;
		}

		// latestPosition�S�̂̃X�y�[�X�ŋ�؂��������Ŕ��肷��
		String[] sa = latestPosition.split(" ", -1);
		if (sa.length % 2 == 0) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * �ugo�v�R�}���h�܂��́ugo ponder�v�R�}���h�̎��ԃI�v�V������Ԃ�
	 * �E���߂́ugo�v�R�}���h�܂��́ugo ponder�v�R�}���h��btime�܂���wtime���������������ĕԂ�
	 * �E���߂́ugo�v�R�}���h�܂��́ugo ponder�v�R�}���h��M������̌o�ߎ��Ԃ�btime�܂���wtime��������i���������ʂ�0��菬�����ꍇ��0�j
	 * 
	 * �E���������ŁA���߂̃R�}���h����̌o�ߎ��Ԃ�1�b�̏ꍇ
	 * �i��j�ubtime 60000 wtime 50000 byoyomi 10000�v
	 * ���ubtime 59000 wtime 50000 byoyomi 10000�v
	 * �i��j�ubtime 40000 wtime 50000 binc 10000 winc 10000�v
	 * ���ubtime 39000 wtime 50000 binc 10000 winc 10000�v
	 * �i��j�ubtime 0 wtime 0 byoyomi 2000�v
	 * ���ubtime 0 wtime 0 byoyomi 2000�v
	 * 
	 * @return
	 */
	public String getGoTimeOption() {
		// �f�t�H���g�l
		final String DEFAULT_VALUE = "btime 0 wtime 0 byoyomi 10000";

		try {
			if (Utils.isEmpty(latestSysinGoCommand) || !latestSysinGoCommand.startsWith("go ")) {
				return DEFAULT_VALUE;
			}

			String[] sa = latestSysinGoCommand.split(" ", -1);
			int timeIndex = Integer.MIN_VALUE;

			for (int i = 0; i < sa.length; i++) {
				if (isSente() && "btime".equals(sa[i]) || !isSente() && "wtime".equals(sa[i])) {
					timeIndex = i;
					break;
				}
			}

			int time_before = Integer.parseInt(sa[timeIndex + 1]);
			int time_diff = (int) ((System.nanoTime() - nanoTimeAtlatestSysinGoCommand) / 1000000);
			int time_after = time_before - time_diff;

			logger.info("time_before=" + time_before);
			logger.info("time_diff=" + time_diff);
			logger.info("time_after=" + time_after);

			if (time_after < 0) {
				time_after = 0;
			}

			StringBuilder sb = new StringBuilder();

			for (int i = 0; i < sa.length; i++) {
				if ("go".equals(sa[i]) || "ponder".equals(sa[i])) {
					continue;
				}

				if (i == timeIndex + 1) {
					sb.append(time_after);
				} else {
					sb.append(sa[i]);
				}

				sb.append(" ");
			}

			return sb.toString().trim();
		} catch (Exception e) {
			return DEFAULT_VALUE;
		}
	}

	/**
	 * �ugo�v�R�}���h�̎��ԃI�v�V������Ԃ�
	 * �E���߂́ugo�v�R�}���h�ŁA�����̎������ԕ���������value�{����
	 * 
	 * �E���������ŁAvalue��0.4�̏ꍇ
	 * �i��j�ugo btime 60000 wtime 50000 byoyomi 10000�v
	 * ���ubtime 24000 wtime 50000 byoyomi 4000�v
	 * �i��j�ugo ponder btime 40000 wtime 50000 binc 10000 winc 10000�v
	 * ���ubtime 16000 wtime 50000 binc 4000 winc 10000�v
	 * �i��j�ugo btime 0 wtime 0 byoyomi 2000�v
	 * ���ubtime 0 wtime 0 byoyomi 800�v
	 * 
	 * @return
	 */
	public String getGoTimeOption(double value) {
		// �f�t�H���g�l
		final String DEFAULT_VALUE = "btime 0 wtime 0 byoyomi 10000";

		try {
			if (Utils.isEmpty(latestSysinGoCommand) || !latestSysinGoCommand.startsWith("go ")) {
				return DEFAULT_VALUE;
			}

			String[] sa = latestSysinGoCommand.split(" ", -1);
			StringBuilder sb = new StringBuilder();

			boolean nextFlg = false;

			for (int i = 0; i < sa.length; i++) {
				if ("go".equals(sa[i]) || "ponder".equals(sa[i])) {
					continue;
				}

				if (nextFlg) {
					nextFlg = false;
					sb.append((int) (Integer.parseInt(sa[i]) * value));
				} else {
					sb.append(sa[i]);
					if (isSente() && ("btime".equals(sa[i]) || "binc".equals(sa[i]))) {
						nextFlg = true;
					} else if (!isSente() && ("wtime".equals(sa[i]) || "winc".equals(sa[i]))) {
						nextFlg = true;
					} else if ("byoyomi".equals(sa[i])) {
						nextFlg = true;
					} else {
						nextFlg = false;
					}
				}

				sb.append(" ");
			}

			return sb.toString().trim();
		} catch (Exception e) {
			return DEFAULT_VALUE;
		}
	}

	// ------------------------------ �P����Getter&Setter START ------------------------------

	public boolean isMyTurn() {
		return myTurn;
	}

	public void setMyTurn(boolean myTurn) {
		this.myTurn = myTurn;
	}

	public String getLatestGoNotPonderPosition() {
		return latestGoNotPonderPosition;
	}

	public void setLatestGoNotPonderPosition(String latestGoNotPonderPosition) {
		this.latestGoNotPonderPosition = latestGoNotPonderPosition;
	}

	public String getLatestGoPonderPosition() {
		return latestGoPonderPosition;
	}

	public void setLatestGoPonderPosition(String latestGoPonderPosition) {
		this.latestGoPonderPosition = latestGoPonderPosition;
	}

	public String getLatestPosition() {
		return latestPosition;
	}

	public void setLatestPosition(String latestPosition) {
		this.latestPosition = latestPosition;
	}

	public boolean isPondering() {
		return pondering;
	}

	public void setPondering(boolean pondering) {
		this.pondering = pondering;
	}

	public String getLatestSysinGoCommand() {
		return latestSysinGoCommand;
	}

	public long getNanoTimeAtlatestSysinGoCommand() {
		return nanoTimeAtlatestSysinGoCommand;
	}

	public boolean isDuringGame() {
		return duringGame;
	}

	public void setDuringGame(boolean duringGame) {
		this.duringGame = duringGame;
	}

	public boolean isBefore_exchange_flg() {
		return before_exchange_flg;
	}

	public void setBefore_exchange_flg(boolean before_exchange_flg) {
		this.before_exchange_flg = before_exchange_flg;
	}

	public int getChangePlayerPlys() {
		return changePlayerPlys;
	}

	public void setChangePlayerPlys(int changePlayerPlys) {
		this.changePlayerPlys = changePlayerPlys;
	}

	public int getChangePlayerScoreDiff() {
		return changePlayerScoreDiff;
	}

	public void setChangePlayerScoreDiff(int changePlayerScoreDiff) {
		this.changePlayerScoreDiff = changePlayerScoreDiff;
	}

	public int getMateTimeout() {
		return mateTimeout;
	}

	public void setMateTimeout(int mateTimeout) {
		this.mateTimeout = mateTimeout;
	}

	// ------------------------------ �P����Getter&Setter END ------------------------------

}
