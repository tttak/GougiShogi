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

	/** ponder�̗L�� */
	private boolean ponderFlg = false;

	/** bestmove�R�}���h �i��j�ubestmove 3i4h ponder 3c8h+�v */
	private String bestmoveCommand;
	/** ���߂̓ǂ݋� �i��j�uinfo depth 3 seldepth 4 multipv 1 score cp 502 nodes 774 nps 154800 time 5 pv 2g3g 4h3g 2c3d S*3f�v */
	private String lastPv;

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
	public void clearBestmoveLastPv() {
		this.bestmoveCommand = null;
		this.lastPv = null;
	}

	/**
	 * ���߂̕]���l�i������j���擾
	 * 
	 * @return
	 */
	public String getLastStrScore() {
		return ShogiUtils.getStrScoreFromInfoPv(lastPv);
	}

	/**
	 * ���߂̕]���l�i���l�j���擾
	 * 
	 * @return
	 */
	public int getLastScore() {
		try {
			// ���߂̕]���l�i������j���擾
			// �i��j�u502�v�u-1234�v�umate 3�v�umate -2�v�u""�v
			String strScore = getLastStrScore();

			if (Utils.isEmpty(strScore)) {
				return Constants.SCORE_NONE;
			}

			if (strScore.startsWith("mate ")) {
				// �i��j�u3�v�u-2�v
				int mate = Integer.parseInt(Utils.getSplitResult(strScore, " ", 1));

				if (mate > 0) {
					// �i��j99997
					return Constants.SCORE_MATE - mate;
				} else if (mate < 0) {
					// �i��j-99998
					return -(Constants.SCORE_MATE + mate);
				} else {
					return Constants.SCORE_NONE;
				}
			}

			else {
				// �i��j�u502�v�u-1234�v
				return Integer.parseInt(strScore);
			}

		} catch (Exception e) {
			return Constants.SCORE_NONE;
		}
	}

	/**
	 * bestmove�̎w������擾
	 * �i��j�ubestmove 3i4h ponder 3c8h+�v���u3i4h�v
	 * 
	 * @return
	 */
	public String getBestmove() {
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
	 * bestmove�A�]���l�̕\���p��������擾
	 * �i��j�ubestmove 7g7f [�V�Z(77)] [�]���l 123] [Gikou 20160606]�v
	 * 
	 * @return
	 */
	public String getBestmoveScoreDisp() {
		// �i��j�ubestmove 7g7f�v
		String bestmoveExceptPonder = getBestmoveCommandExceptPonder();
		if (bestmoveExceptPonder == null) {
			bestmoveExceptPonder = "";
		}

		// �i��j�u7g7f�v
		String move = Utils.getSplitResult(bestmoveExceptPonder, " ", 1);
		// �i��j�u7g7f�v���u�V�Z(77)�v
		// �E�{���́uinfo string�v�őS�p������NG��������Ȃ����D�D�D
		String moveDispJa = ShogiUtils.getMoveDispJa(move);
		// �i��j�ubestmove 7g7f [�V�Z(77)] [�]���l 123] [Gikou 20160606]�v
		return bestmoveExceptPonder + " [" + moveDispJa + "] [�]���l " + getLastStrScore() + "] [" + usiName + "]";
	}

	/**
	 * bestmove�Aponder�A�]���l�̕\���p��������擾
	 * �i��j�ubestmove 7g7f ponder 3c3d [�V�Z(77) �R�l(33)] [�]���l 123] [Gikou 20160606]�v
	 * 
	 * @return
	 */
	public String getBestmovePonderScoreDisp() {
		StringBuilder sb = new StringBuilder();
		sb.append(bestmoveCommand);
		sb.append(" [");
		sb.append(ShogiUtils.getMoveDispJa(getBestmove()));

		String ponderMove = getPonderMove();
		if (getPonderMove() != null) {
			sb.append(" ");
			sb.append(ShogiUtils.getMoveDispJa(ponderMove));
		}

		sb.append("] [�]���l ");
		sb.append(getLastStrScore());
		sb.append("] [");
		sb.append(usiName);
		sb.append("]");
		return sb.toString();
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

	public String getLastPv() {
		return lastPv;
	}

	public void setLastPv(String lastPv) {
		this.lastPv = lastPv;
	}

	public boolean isPonderFlg() {
		return ponderFlg;
	}

	public void setPonderFlg(boolean ponderFlg) {
		this.ponderFlg = ponderFlg;
	}

	// ------------------------------ �P����Getter&Setter END ------------------------------

}
