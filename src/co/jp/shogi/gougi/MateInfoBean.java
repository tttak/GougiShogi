package co.jp.shogi.gougi;

/**
 * ���c�^�C�v�u�l�T���G���W���Ƃ̍��c�i�ǂ݋؂̋ǖʂ��l�T���j �v��mateinfo�R�}���h�̑��M�󋵂�ێ�����N���X
 */
public class MateInfoBean {

	/** mateinfo�R�}���h */
	private String command;

	/** mateinfo�R�}���h�̑��M�� */
	private int count = 0;

	/** mateinfo�R�}���h�̑O�񑗐M�����iSystem.currentTimeMillis()���g�p�j */
	private long prevTime;

	/**
	 * �R���X�g���N�^
	 * @param command
	 */
	public MateInfoBean(String command) {
		this.command = command;
	}

	// ------------------------------ �P����Getter&Setter START ------------------------------

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public long getPrevTime() {
		return prevTime;
	}

	public void setPrevTime(long prevTime) {
		this.prevTime = prevTime;
	}

	// ------------------------------ �P����Getter&Setter END ------------------------------

}
