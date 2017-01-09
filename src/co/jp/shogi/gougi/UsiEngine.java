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
	/** �G���W���ԍ��i1�`3�̂����ꂩ�j */
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

	// ------------------------------ �P����Getter&Setter END ------------------------------

}
