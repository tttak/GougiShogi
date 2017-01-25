package co.jp.shogi.gougi;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * USI�G���W���N���X
 */
public class UsiEngine {

	/** ���s�t�@�C�� �i��j�uC:/����/Gikou/gikou.exe�v */
	private File exeFile;
	/** USI�� �i��j�uGikou 20160606�v�@ */
	private String usiName;
	/** �G���W���ԍ��i1,2,3,...,�G���W�������j */
	private int engineNumber;

	/** �v���Z�X */
	private Process process;
	/** input�X���b�h */
	private InputStreamThread inputThread;
	/** output�X���b�h */
	private OutputStreamThread outputThread;

	/** USI�I�v�V������Set */
	private Set<String> optionSet = new HashSet<String>();

	/** bestmove�R�}���h �i��j�ubestmove 3i4h ponder 3c8h+�v */
	private String bestmoveCommand;
	/** ���߂̓ǂ݋� �i��j�uinfo depth 3 seldepth 4 multipv 1 score cp 502 nodes 774 nps 154800 time 5 pv 2g3g 4h3g 2c3d S*3f�v */
	private String latestPv;

	/** �y�O��̒l�zbestmove�R�}���h �i��j�ubestmove 3i4h ponder 3c8h+�v */
	private String prev_bestmoveCommand;
	/** �y�O��̒l�z���߂̓ǂ݋� �i��j�uinfo depth 3 seldepth 4 multipv 1 score cp 502 nodes 774 nps 154800 time 5 pv 2g3g 4h3g 2c3d S*3f�v */
	private String prev_latestPv;

	// ----- ���c�^�C�v�u�e�X�̍őP����������ĕ]���l�̍��v�Ŕ���i2�ҁj�v�̏ꍇ�Ɏg�p
	/** �y�őP��̌����O�̒l�zbestmove�R�}���h �i��j�ubestmove 3i4h ponder 3c8h+�v */
	private String before_exchange_bestmoveCommand;
	/** �y�őP��̌����O�̒l�z���߂̓ǂ݋� �i��j�uinfo depth 3 seldepth 4 multipv 1 score cp 502 nodes 774 nps 154800 time 5 pv 2g3g 4h3g 2c3d S*3f�v */
	private String before_exchange_latestPv;
	// -----

	/** ponder�ݒ��on/off */
	private boolean ponderOnOff = false;
	/** ponder���{�����ۂ� */
	private boolean pondering = false;
	/** ponder���{���̋ǖ� */
	private String ponderingPosition;

	/** ���Oponder���{�����ۂ� */
	private boolean prePondering = false;

	/**
	 * �v���Z�X�̏I��
	 */
	public void destroy() {
		if (inputThread != null) {
			inputThread.close();
		}
		if (outputThread != null) {
			outputThread.close();
		}

		Utils.destroy(process);
	}

	/**
	 * ���̃G���W���̕\���p�������Ԃ�
	 * �i��j�uEngine1�v
	 * 
	 * @return
	 */
	public String getEngineDisp() {
		return "Engine" + getEngineNumber();
	}

	/**
	 * ���̃G���W����USI�I�v�V�����̃v���t�B�b�N�X��Ԃ�
	 * �i��j�uE1_�v
	 * 
	 * @return
	 */
	public String getOptionNamePrefix() {
		return "E" + getEngineNumber() + "_";
	}

	/**
	 * bestmove�R�}���h�iponder�����������j��Ԃ�
	 * �i��j�ubestmove 3i4h�v
	 * 
	 * @return
	 */
	public String getBestmoveCommandExceptPonder() {
		if (bestmoveCommand == null) {
			return null;
		}

		int index = bestmoveCommand.indexOf("ponder");
		if (index >= 0) {
			// �i��j�ubestmove 3i4h ponder 3c8h+�v���ubestmove 3i4h�v
			return bestmoveCommand.substring(0, index).trim();
		} else {
			return bestmoveCommand.trim();
		}
	}

	/**
	 * bestmove�A���߂̓ǂ݋؂̃N���A
	 */
	public void clearBestmoveLatestPv() {
		this.bestmoveCommand = null;
		this.latestPv = null;
	}

	/**
	 * �y�őP��̌����O�̒l�zbestmove�A���߂̓ǂ݋؂̃N���A
	 */
	public void clear_before_exchange_BestmoveLatestPv() {
		this.before_exchange_bestmoveCommand = null;
		this.before_exchange_latestPv = null;
	}

	/**
	 * ���߂̕]���l�i������j���擾
	 * 
	 * @return
	 */
	public String getLatestStrScore() {
		return ShogiUtils.getStrScoreFromInfoPv(latestPv);
	}

	/**
	 * ���߂̕]���l�i���l�j���擾
	 * 
	 * @return
	 */
	public int getLatestScore() {
		// ���߂̕]���l�i������j���擾
		// �i��j�u502�v�u-1234�v�umate 3�v�umate -2�v�u""�v
		String strScore = getLatestStrScore();

		return ShogiUtils.getScoreFromStrScore(strScore);
	}

	/**
	 * �y�O��̒l�z���߂̕]���l�i������j���擾
	 * 
	 * @return
	 */
	public String getPrevLatestStrScore() {
		return ShogiUtils.getStrScoreFromInfoPv(prev_latestPv);
	}

	/**
	 * �y�O��̒l�z���߂̕]���l�i���l�j���擾
	 * 
	 * @return
	 */
	public int getPrevLatestScore() {
		// �y�O��̒l�z���߂̕]���l�i������j���擾
		// �i��j�u502�v�u-1234�v�umate 3�v�umate -2�v�u""�v
		String strScore = getPrevLatestStrScore();

		return ShogiUtils.getScoreFromStrScore(strScore);
	}

	/**
	 * �y�őP��̌����O�̒l�z���߂̕]���l�i������j���擾
	 * 
	 * @return
	 */
	public String get_before_exchange_LatestStrScore() {
		return ShogiUtils.getStrScoreFromInfoPv(before_exchange_latestPv);
	}

	/**
	 * �y�őP��̌����O�̒l�z���߂̕]���l�i���l�j���擾
	 * 
	 * @return
	 */
	public int get_before_exchange_LatestScore() {
		// �y�őP��̌����O�̒l�z���߂̕]���l�i������j���擾
		// �i��j�u502�v�u-1234�v�umate 3�v�umate -2�v�u""�v
		String strScore = get_before_exchange_LatestStrScore();

		return ShogiUtils.getScoreFromStrScore(strScore);
	}

	/**
	 * 2��O�̕]���l����̏㏸�����擾
	 * 
	 * @return
	 */
	public int getScore2TemaeJoushou() {
		int current_score = getLatestScore();
		int prev_score = getPrevLatestScore();

		if (current_score == Constants.SCORE_NONE || prev_score == Constants.SCORE_NONE) {
			return Constants.SCORE_NONE;
		} else {
			return current_score - prev_score;
		}
	}

	/**
	 * bestmove�̎w������擾
	 * �i��j�ubestmove 3i4h ponder 3c8h+�v���u3i4h�v
	 * 
	 * @return
	 */
	public String getBestmove() {
		return getBestmove(bestmoveCommand);
	}

	/**
	 * �y�őP��̌����O�̒l�zbestmove�̎w������擾
	 * �i��j�ubestmove 3i4h ponder 3c8h+�v���u3i4h�v
	 * 
	 * @return
	 */
	public String get_before_exchange_Bestmove() {
		return getBestmove(before_exchange_bestmoveCommand);
	}

	/**
	 * bestmove�̎w������擾
	 * �i��j�ubestmove 3i4h ponder 3c8h+�v���u3i4h�v
	 * 
	 * @return
	 */
	private static String getBestmove(String bestmoveCommand) {
		try {
			if (bestmoveCommand == null) {
				return null;
			}

			if (!bestmoveCommand.startsWith("bestmove ")) {
				return null;
			}

			// �i��j�ubestmove 3i4h ponder 3c8h+�v���u3i4h�v
			return Utils.getSplitResult(bestmoveCommand, " ", 1).trim();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * ponder�̎w������擾
	 * �i��j�ubestmove 3i4h ponder 3c8h+�v���u3c8h+�v
	 * 
	 * @return
	 */
	public String getPonderMove() {
		try {
			if (bestmoveCommand == null) {
				return null;
			}

			int index = bestmoveCommand.indexOf("ponder ");
			if (index < 0) {
				return null;
			}

			// �i��j�ubestmove 3i4h ponder 3c8h+�v���u3c8h+�v
			return bestmoveCommand.substring(index + "ponder ".length()).trim();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * bestmove�Aponder�A�]���l�̕\���p��������擾
	 * �i��j����ponder��true�̏ꍇ�@�ubestmove 7g7f ponder 3c3d [�V�Z(77) �R�l(33)] [�]���l 123 �i�O��100 ����23�j] [Gikou 20160606]�v
	 * �i��j����ponder��false�̏ꍇ�ubestmove 7g7f [�V�Z(77)] [�]���l 123 �i�O��100 ����23�j] [Gikou 20160606]�v
	 * �E����prev_diff��true�̏ꍇ�A�i�O��100 ����23�j�̕������ȗ�����
	 * �E�{���́uinfo string�v�őS�p������NG��������Ȃ����D�D�D
	 * 
	 * @param ponder
	 * @param prev_diff
	 * @return
	 */
	public String getBestmoveScoreDisp(boolean ponder, boolean prev_diff) {
		StringBuilder sb = new StringBuilder();

		// �i��j�ubestmove 7g7f�v
		String bestmoveExceptPonder = getBestmoveCommandExceptPonder();
		if (bestmoveExceptPonder == null) {
			bestmoveExceptPonder = "";
		}

		if (ponder) {
			// �i��j�ubestmove 7g7f ponder 3c3d�v
			sb.append(bestmoveCommand);
		} else {
			// �i��j�ubestmove 7g7f�v
			sb.append(bestmoveExceptPonder);
		}

		sb.append(" [");
		// �i��j�u�V�Z(77)�v
		sb.append(ShogiUtils.getMoveDispJa(getBestmove()));

		// �i��j�u3c3d�v
		String ponderMove = getPonderMove();
		if (ponder) {
			if (ponderMove != null) {
				sb.append(" ");
				// �i��j�u�R�l(33)�v
				sb.append(ShogiUtils.getMoveDispJa(ponderMove));
			}
		}

		sb.append("] [�]���l ");
		sb.append(getLatestStrScore());

		if (prev_diff) {
			sb.append(" �i�O��");
			sb.append(getPrevLatestStrScore());
			sb.append(" ����");

			// 2��O�̕]���l����̏㏸��
			int score2TemaeJousho = getScore2TemaeJoushou();
			if (score2TemaeJousho == Constants.SCORE_NONE) {
				sb.append(" ");
			} else {
				sb.append(score2TemaeJousho);
			}

			sb.append("�j");
		}

		sb.append("] [");
		sb.append(usiName);
		sb.append("]");

		return sb.toString();
	}

	/**
	 * �y�O��̒l�z��ۑ�
	 */
	public void savePrevValue() {
		// �y�O��̒l�zbestmove�R�}���h
		prev_bestmoveCommand = bestmoveCommand;
		// �y�O��̒l�z���߂̓ǂ݋�
		prev_latestPv = latestPv;
	}

	/**
	 * �y�őP��̌����O�̒l�z��ۑ�
	 */
	public void save_before_exchange_Value() {
		// �y�őP��̌����O�̒l�zbestmove�R�}���h
		before_exchange_bestmoveCommand = bestmoveCommand;
		// �y�őP��̌����O�̒l�z���߂̓ǂ݋�
		before_exchange_latestPv = latestPv;
	}

	/**
	 * �N���A����
	 */
	public void clear() {
		bestmoveCommand = null;
		latestPv = null;
		prev_bestmoveCommand = null;
		prev_latestPv = null;
		before_exchange_bestmoveCommand = null;
		before_exchange_latestPv = null;
	}

	// ------------------------------ �P����Getter&Setter START ------------------------------

	public File getExeFile() {
		return exeFile;
	}

	public void setExeFile(File exeFile) {
		this.exeFile = exeFile;
	}

	public String getUsiName() {
		return usiName;
	}

	public void setUsiName(String usiName) {
		this.usiName = usiName;
	}

	public int getEngineNumber() {
		return engineNumber;
	}

	public void setEngineNumber(int engineNumber) {
		this.engineNumber = engineNumber;
	}

	public Process getProcess() {
		return process;
	}

	public void setProcess(Process process) {
		this.process = process;
	}

	public InputStreamThread getInputThread() {
		return inputThread;
	}

	public void setInputThread(InputStreamThread inputThread) {
		this.inputThread = inputThread;
	}

	public OutputStreamThread getOutputThread() {
		return outputThread;
	}

	public void setOutputThread(OutputStreamThread outputThread) {
		this.outputThread = outputThread;
	}

	public String getBestmoveCommand() {
		return bestmoveCommand;
	}

	public void setBestmoveCommand(String bestmoveCommand) {
		this.bestmoveCommand = bestmoveCommand;
	}

	public Set<String> getOptionSet() {
		return optionSet;
	}

	public void setOptionSet(Set<String> optionSet) {
		this.optionSet = optionSet;
	}

	public String getLatestPv() {
		return latestPv;
	}

	public void setLatestPv(String latestPv) {
		this.latestPv = latestPv;
	}

	public String getPrev_bestmoveCommand() {
		return prev_bestmoveCommand;
	}

	public void setPrev_bestmoveCommand(String prev_bestmoveCommand) {
		this.prev_bestmoveCommand = prev_bestmoveCommand;
	}

	public String getPrev_latestPv() {
		return prev_latestPv;
	}

	public void setPrev_latestPv(String prev_latestPv) {
		this.prev_latestPv = prev_latestPv;
	}

	public boolean isPonderOnOff() {
		return ponderOnOff;
	}

	public void setPonderOnOff(boolean ponderOnOff) {
		this.ponderOnOff = ponderOnOff;
	}

	public boolean isPondering() {
		return pondering;
	}

	public void setPondering(boolean pondering) {
		this.pondering = pondering;
	}

	public String getPonderingPosition() {
		return ponderingPosition;
	}

	public void setPonderingPosition(String ponderingPosition) {
		this.ponderingPosition = ponderingPosition;
	}

	public boolean isPrePondering() {
		return prePondering;
	}

	public void setPrePondering(boolean prePondering) {
		this.prePondering = prePondering;
	}

	public String getBefore_exchange_bestmoveCommand() {
		return before_exchange_bestmoveCommand;
	}

	public void setBefore_exchange_bestmoveCommand(String before_exchange_bestmoveCommand) {
		this.before_exchange_bestmoveCommand = before_exchange_bestmoveCommand;
	}

	public String getBefore_exchange_latestPv() {
		return before_exchange_latestPv;
	}

	public void setBefore_exchange_latestPv(String before_exchange_latestPv) {
		this.before_exchange_latestPv = before_exchange_latestPv;
	}

	// ------------------------------ �P����Getter&Setter END ------------------------------

}
