package co.jp.shogi.gougi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * �����֘A�̃��[�e�B���e�B�N���X
 */
public class ShogiUtils {

	/**
	 * �S�G���W����bestmove�A���߂̓ǂ݋؂��N���A����
	 * 
	 * @param usiEngineList
	 */
	public static void clearBestmoveLatestPv(List<UsiEngine> usiEngineList) {
		for (UsiEngine engine : usiEngineList) {
			engine.clearBestmoveLatestPv();
		}
	}

	/**
	 * �S�G���W���́y�őP��̌����O�̒l�zbestmove�A���߂̓ǂ݋؂��N���A����
	 * 
	 * @param usiEngineList
	 */
	public static void clear_before_exchange_BestmoveLatestPv(List<UsiEngine> usiEngineList) {
		for (UsiEngine engine : usiEngineList) {
			engine.clearBestmoveLatestPv();
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
	 * bestmove�iponder�����������j�������ƈ�v����G���W���̂����A�G���W���ԍ����ŏ��̃G���W����Ԃ�
	 * 
	 * @param usiEngineList
	 * @param bestmoveExceptPonder
	 * @return
	 */
	public static UsiEngine getMinEngine(List<UsiEngine> usiEngineList, String bestmoveExceptPonder) {
		if (Utils.isEmpty(bestmoveExceptPonder)) {
			return null;
		}

		// �G���W���ԍ����ɕ���ł���͂��Ȃ̂ŁA����ł悢
		for (UsiEngine engine : usiEngineList) {
			// bestmove�iponder�����������j�Ŕ�r
			if (bestmoveExceptPonder.equals(engine.getBestmoveCommandExceptPonder())) {
				return engine;
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
			if ("resign".equals(move)) {
				return "����";
			}

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

	/**
	 * �ǂ݋؂���]���l�i������j���擾
	 * �i��j�uinfo depth 3 seldepth 4 multipv 1 score cp 502 nodes 774 nps 154800 time 5 pv 2g3g 4h3g 2c3d S*3f�v
	 * �� �u502�v
	 * �i��j�uinfo depth 5 seldepth 4 time 2 nodes 1399 nps 699500 hashfull 657 score mate 3 multipv 1 pv 4i2i+ 3h3g G*4g�v
	 * �� �umate 3�v
	 * �i��j�uinfo depth 5 seldepth 3 time 1 nodes 14 nps 14000 hashfull 663 score mate -2 multipv 1 pv 5g5f G*4e�v
	 * �� �umate -2�v
	 * 
	 * @param infoPv
	 * @return
	 */
	public static String getStrScoreFromInfoPv(String infoPv) {
		try {
			if (Utils.isEmpty(infoPv)) {
				return "";
			}

			if (infoPv.indexOf("score cp ") >= 0) {
				// �i��j�uinfo depth 3 seldepth 4 multipv 1 score cp 502 nodes 774 nps 154800 time 5 pv 2g3g 4h3g 2c3d S*3f�v
				// �� �u502 nodes 774 nps 154800 time 5 pv 2g3g 4h3g 2c3d S*3f�v
				String str = infoPv.substring(infoPv.indexOf("score cp ") + "score cp ".length()).trim();
				// �i��j�u502�v
				return Utils.getSplitResult(str, " ", 0);
			}

			else if (infoPv.indexOf("score mate ") >= 0) {
				// �i��j�uinfo depth 5 seldepth 4 time 2 nodes 1399 nps 699500 hashfull 657 score mate 3 multipv 1 pv 4i2i+ 3h3g G*4g�v
				// �� �u3 multipv 1 pv 4i2i+ 3h3g G*4g�v
				String str = infoPv.substring(infoPv.indexOf("score mate ") + "score mate ".length()).trim();
				// �i��j�umate 3�v
				return "mate " + Utils.getSplitResult(str, " ", 0);
			}

			else {
				return "";
			}
		} catch (Exception e) {
			return "";
		}
	}

	/**
	 * �]���l�i������j����]���l�i���l�j���擾
	 * �i��j�u502�v���u502�v
	 * �i��j�u-1234�v���u-1234�v
	 * �i��j�umate 3�v���u99997�v
	 * �i��j�umate -2�v���u-99998�v
	 * �i��j�umate�v���uConstants.SCORE_NONE�v
	 * �i��j�u""�v���uConstants.SCORE_NONE�v
	 * 
	 * @param strScore
	 * @return
	 */
	public static int getScoreFromStrScore(String strScore) {
		try {
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
	 * �u���G���W����bestmove�̎w�����ponder�̎w���肪�Ƃ��ɑ��݂��A���A�������v���ۂ���Ԃ�
	 * 
	 * @param engine1
	 * @param engine2
	 * @return
	 */
	public static boolean equalsBestmoveAndPondermove(UsiEngine engine1, UsiEngine engine2) {
		if (Utils.isEmpty(engine1.getBestmove())) {
			return false;
		}
		if (Utils.isEmpty(engine2.getBestmove())) {
			return false;
		}
		if (Utils.isEmpty(engine1.getPonderMove())) {
			return false;
		}
		if (Utils.isEmpty(engine2.getPonderMove())) {
			return false;
		}

		return engine1.getBestmove().equals(engine2.getBestmove()) && engine1.getPonderMove().equals(engine2.getPonderMove());
	}

	/**
	 * �ǖʂɎw�����ǉ�
	 * �i��j�uposition startpos moves 7g7f�v + �u3c3d�v �� �uposition startpos moves 7g7f 3c3d�v
	 * �i��j�uposition startpos�v + �u7g7f�v �� �uposition startpos moves 7g7f�v
	 * 
	 * @param position
	 * @param move
	 * @return
	 */
	public static String appendMove(String position, String move) {
		if (position.contains(" moves ")) {
			return position + " " + move;
		} else {
			return position + " moves " + move;
		}
	}

	/**
	 * �S�G���W���ɂ��āA�u���Oponder���{�����ۂ��v�̃t���O���Z�b�g����
	 * 
	 * @param usiEngineList
	 * @param prePondering
	 */
	public static void setPrePondering(List<UsiEngine> usiEngineList, boolean prePondering) {
		for (UsiEngine engine : usiEngineList) {
			engine.setPrePondering(prePondering);
		}
	}

	/**
	 * �ugo�v�R�}���h�̎��ԃI�v�V�����̐������ւ���
	 * 
	 * �i��j�ubtime 60000 wtime 50000 byoyomi 10000�v
	 * ���uwtime 60000 btime 50000 byoyomi 10000�v
	 * �i��j�ubtime 40000 wtime 50000 binc 10000 winc 8000�v
	 * ���uwtime 40000 btime 50000 winc 10000 binc 8000�v
	 * 
	 * @param option
	 * @return
	 */
	public static String reverseGoTimeOption(String option) {
		try {
			String s = option;

			s = s.replaceAll("btime", "temp");
			s = s.replaceAll("wtime", "btime");
			s = s.replaceAll("temp", "wtime");

			s = s.replaceAll("binc", "temp");
			s = s.replaceAll("winc", "binc");
			s = s.replaceAll("temp", "winc");

			return s;
		} catch (Exception e) {
			return option;
		}
	}

	/**
	 * �S�G���W���ɃR�}���h���M
	 * 
	 * @param usiEngineList
	 * @param command
	 */
	public static void sendCommandToAllEngines(List<UsiEngine> usiEngineList, String command) {
		// �e�G���W���ւ̃R�}���h���X�g�ɒǉ�
		for (UsiEngine engine : usiEngineList) {
			// �E�X���b�h�̔r������
			synchronized (engine.getOutputThread()) {
				engine.getOutputThread().getCommandList().add(command);
			}
		}
	}

}
