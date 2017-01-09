package co.jp.shogi.gougi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * �����֘A�̃��[�e�B���e�B�N���X
 */
public class ShogiUtils {

	/**
	 * �S�G���W����bestmove�R�}���h���N���A����
	 * 
	 * @param usiEngineList
	 */
	public static void clearBestmove(List<UsiEngine> usiEngineList) {
		for (UsiEngine engine : usiEngineList) {
			engine.setBestmoveCommand(null);
		}
	}

	/**
	 * bestmove�R�}���h����̃G���W�������݂��邩�ۂ���Ԃ�
	 * 
	 * @param usiEngineList
	 * @return
	 */
	public static boolean containsEmptyBestmove(List<UsiEngine> usiEngineList) {
		for (UsiEngine engine : usiEngineList) {
			if (Utils.isEmpty(engine.getBestmoveCommand())) {
				return true;
			}
		}

		return false;
	}

	/**
	 * bestmove���Ƃ̌���Map�iponder�����������j���쐬����
	 * 
	 * @param usiEngineList
	 * @return
	 */
	public static Map<String, Integer> createBestmoveCountMap(List<UsiEngine> usiEngineList) {
		Map<String, Integer> map = new HashMap<String, Integer>();

		for (UsiEngine engine : usiEngineList) {
			String command = engine.getBestmoveCommandExceptPonder();
			Utils.addToCountMap(map, command, 1);
		}

		return map;
	}

	/**
	 * �G���W���ԍ�������engineNumber�̃G���W����Ԃ�
	 * 
	 * @param usiEngineList
	 * @param engineNumber
	 * @return
	 */
	public static UsiEngine getEngine(List<UsiEngine> usiEngineList, int engineNumber) {
		for (UsiEngine engine : usiEngineList) {
			if (engine.getEngineNumber() == engineNumber) {
				return engine;
			}
		}

		return null;
	}

	/**
	 * ���c�����s���A���c���ʂ�bestmove�R�}���h�iponder�����������j��Ԃ�
	 * �E���������c
	 * �E�O�ҎO�l�̏ꍇ�̓G���W��1�̎w������̗p����
	 * 
	 * @param usiEngineList
	 * @return
	 */
	public static String getGougiBestmoveCommandExceptPonder(List<UsiEngine> usiEngineList) {
		// bestmove���Ƃ̌���Map�iponder�����������j���쐬
		Map<String, Integer> bestmoveCountMap = createBestmoveCountMap(usiEngineList);

		// �O�ҎO�l�̏ꍇ
		if (bestmoveCountMap.size() >= 3) {
			// �G���W��1�̎w������̗p����
			return getEngine(usiEngineList, 1).getBestmoveCommandExceptPonder();
		}

		// ���̑��̏ꍇ�i�u3��0�v�܂��́u2��1�v�̏ꍇ�j
		else {
			for (String bestmoveCommand : bestmoveCountMap.keySet()) {
				// �u3��0�v�܂��́u2��1�v�������肦�Ȃ��̂ŁA2�[�ȏ�Ŋm��
				if (bestmoveCountMap.get(bestmoveCommand) >= 2) {
					return bestmoveCommand;
				}
			}
		}

		return null;
	}

	/**
	 * �R�}���h����I�v�V���������擾
	 * �i��j�uoption name Threads type spin default 4 min 1 max 256�v
	 * �� �uThreads�v
	 * �i��j�usetoption name E2_Threads value 1�v
	 * �� �uE2_Threads�v
	 * 
	 * @param command
	 * @return
	 */
	public static String getOptionName(String command) {
		try {
			// �uoption name �`�v�usetoption name �`�v�ȊO�̏ꍇ�Anull��Ԃ�
			if (!(command.startsWith("option name ") || command.startsWith("setoption name "))) {
				return null;
			}

			String[] sa = command.split(" ", -1);
			return sa[2].trim();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * move����{��ɕϊ����ĕԂ�
	 * �i��j�u7g7f�v���u�V�Z(77)�v
	 * �i��j�u2d2c+�v���u�Q�O��(24)�v
	 * �i��j�uP*8g�v���u�W�����Łv
	 * 
	 * @param move
	 * @return
	 */
	public static String getMoveDispJa(String move) {
		try {
			String s1 = move.substring(0, 1);
			String s2 = move.substring(1, 2);
			String s3 = move.substring(2, 3);
			String s4 = move.substring(3, 4);
			String s5 = null;

			if (move.length() > 4) {
				s5 = move.substring(4, 5);
			}

			// ��ł��̏ꍇ
			if ("*".equals(s2)) {
				StringBuilder sb = new StringBuilder();
				sb.append(Utils.getZenkaku(Utils.getIntValue(s3, 0)));
				sb.append(Utils.getKanji(Utils.getIntFromAlphabet(s4.charAt(0))));
				sb.append(getPieceTypeDisp(s1));
				sb.append("��");

				return sb.toString();
			}

			// ��ł��ł͂Ȃ��ꍇ�i��̈ړ���̏ꍇ�j
			else {
				StringBuilder sb = new StringBuilder();
				sb.append(Utils.getZenkaku(Utils.getIntValue(s3, 0)));
				sb.append(Utils.getKanji(Utils.getIntFromAlphabet(s4.charAt(0))));

				if ("+".equals(s5)) {
					sb.append("��");
				}

				sb.append("(");
				sb.append(Utils.getIntValue(s1, 0));
				sb.append(Utils.getIntFromAlphabet(s2.charAt(0)));
				sb.append(")");

				return sb.toString();
			}

		} catch (Exception e) {
			e.printStackTrace();
			return move;
		}
	}

	/**
	 * ��̓��{�ꖼ��Ԃ�
	 * �i��j�uK�v���u�ʁv
	 * 
	 * @param pieceType
	 * @return
	 */
	public static String getPieceTypeDisp(String pieceType) {
		if ("K".equals(pieceType)) {
			return "��";
		} else if ("R".equals(pieceType)) {
			return "��";
		} else if ("B".equals(pieceType)) {
			return "�p";
		} else if ("G".equals(pieceType)) {
			return "��";
		} else if ("S".equals(pieceType)) {
			return "��";
		} else if ("N".equals(pieceType)) {
			return "�j";
		} else if ("L".equals(pieceType)) {
			return "��";
		} else if ("P".equals(pieceType)) {
			return "��";
		} else {
			return "";
		}
	}

}
