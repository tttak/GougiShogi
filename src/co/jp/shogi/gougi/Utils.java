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
 * ユーティリティクラス
 */
public class Utils {

	/**
	 * loggerの初期化
	 * 
	 * @param name
	 * @throws SecurityException
	 * @throws IOException
	 */
	public static void initLogger(String name) throws SecurityException, IOException {
		// logging.properties読込み
		InputStream is = Utils.class.getResourceAsStream(name);

		try {
			LogManager.getLogManager().readConfiguration(is);
		} finally {
			close(is);
		}
	}

	/**
	 * クローズ処理
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
	 * クローズ処理
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
	 * クローズ処理
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
	 * サブプロセスの終了
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
	 * 自分のパス（フォルダ）を取得
	 * （jarファイルの場合、jarファイルの存在するフォルダ）
	 * 
	 * @return
	 */
	public static File getMyDir() {
		String classPath = System.getProperty("java.class.path");
		return new File(new File(classPath).getAbsolutePath()).getParentFile();
	}

	/**
	 * prefixで始まる文字列がlistに存在するか否かを返す
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
	 * 文字列が空か否かの判定
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isEmpty(String str) {
		return str == null || str.isEmpty();
	}

	/**
	 * mapのkeyの値にvalueを追加する。
	 * 要素が存在しない場合は値がvalueでmapに追加する。
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
	 * 数値を漢字に変換
	 * （例）「3→三」
	 * 
	 * @param num
	 * @return
	 */
	public static String getKanji(int num) {
		if (!(1 <= num && num <= 9)) {
			return "";
		}

		return "一二三四五六七八九".substring(num - 1, num);
	}

	/**
	 * 数値を全角に変換
	 * （例）「3→３」
	 * 
	 * @param num
	 * @return
	 */
	public static String getZenkaku(int num) {
		if (!(1 <= num && num <= 9)) {
			return "";
		}

		return "１２３４５６７８９".substring(num - 1, num);
	}

	/**
	 * 文字列をintに変換する。変換に失敗した場合、defaultValueを返す。
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
	 * アルファベットを数値に変換
	 * （例）「c→3」
	 * 
	 * @param c
	 * @return
	 */
	public static int getIntFromAlphabet(char c) {
		return c - 'a' + 1;
	}

	/**
	 * 文字列をsplitし、index番目の要素を返す
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
