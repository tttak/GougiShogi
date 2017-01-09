package co.jp.shogi.gougi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.logging.LogManager;

/**
 * ���[�e�B���e�B�N���X
 */
public class Utils {

	/**
	 * logger�̏�����
	 * 
	 * @param name
	 * @throws SecurityException
	 * @throws IOException
	 */
	public static void initLogger(String name) throws SecurityException, IOException {
		// logging.properties�Ǎ���
		InputStream is = Utils.class.getResourceAsStream(name);

		try {
			LogManager.getLogManager().readConfiguration(is);
		} finally {
			close(is);
		}
	}

	/**
	 * �N���[�Y����
	 * 
	 * @param r
	 */
	public static void close(Reader r) {
		try {
			if (r != null) {
				r.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * �N���[�Y����
	 * 
	 * @param w
	 */
	public static void close(Writer w) {
		try {
			if (w != null) {
				w.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * �N���[�Y����
	 * 
	 * @param is
	 */
	public static void close(InputStream is) {
		try {
			if (is != null) {
				is.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * �T�u�v���Z�X�̏I��
	 * 
	 * @param process
	 */
	public static void destroy(Process process) {
		try {
			if (process != null) {
				process.destroy();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * �����̃p�X�i�t�H���_�j���擾
	 * �ijar�t�@�C���̏ꍇ�Ajar�t�@�C���̑��݂���t�H���_�j
	 * 
	 * @return
	 */
	public static File getMyDir() {
		String classPath = System.getProperty("java.class.path");
		return new File(new File(classPath).getAbsolutePath()).getParentFile();
	}

	/**
	 * prefix�Ŏn�܂镶����list�ɑ��݂��邩�ۂ���Ԃ�
	 * 
	 * @param list
	 * @param prefix
	 * @return
	 */
	public static boolean containsStartsWith(List<String> list, String prefix) {
		if (list == null) {
			return false;
		}

		for (String str : list) {
			if (str.startsWith(prefix)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * �����񂪋󂩔ۂ��̔���
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isEmpty(String str) {
		return str == null || str.isEmpty();
	}

	/**
	 * map��key�̒l��value��ǉ�����B
	 * �v�f�����݂��Ȃ��ꍇ�͒l��value��map�ɒǉ�����B
	 * 
	 * @param map
	 * @param key
	 * @param value
	 */
	public static void addToCountMap(Map<String, Integer> map, String key, int value) {
		int count = 0;

		if (map.containsKey(key)) {
			count = map.get(key);
		}

		map.put(key, count + value);
	}

	/**
	 * ���l�������ɕϊ�
	 * �i��j�u3���O�v
	 * 
	 * @param num
	 * @return
	 */
	public static String getKanji(int num) {
		if (!(1 <= num && num <= 9)) {
			return "";
		}

		return "���O�l�ܘZ������".substring(num - 1, num);
	}

	/**
	 * ���l��S�p�ɕϊ�
	 * �i��j�u3���R�v
	 * 
	 * @param num
	 * @return
	 */
	public static String getZenkaku(int num) {
		if (!(1 <= num && num <= 9)) {
			return "";
		}

		return "�P�Q�R�S�T�U�V�W�X".substring(num - 1, num);
	}

	/**
	 * �������int�ɕϊ�����B�ϊ��Ɏ��s�����ꍇ�AdefaultValue��Ԃ��B
	 * 
	 * @param s
	 * @param defaultValue
	 * @return
	 */
	public static int getIntValue(String s, int defaultValue) {
		try {
			return Integer.parseInt(s);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	/**
	 * �A���t�@�x�b�g�𐔒l�ɕϊ�
	 * �i��j�uc��3�v
	 * 
	 * @param c
	 * @return
	 */
	public static int getIntFromAlphabet(char c) {
		return c - 'a' + 1;
	}

	/**
	 * �������split���Aindex�Ԗڂ̗v�f��Ԃ�
	 * 
	 * @param str
	 * @param regex
	 * @param index
	 * @return
	 */
	public static String getSplitResult(String str, String regex, int index) {
		try {
			String[] sa = str.split(regex, -1);
			return sa[index];
		} catch (Exception e) {
			return "";
		}
	}

}
