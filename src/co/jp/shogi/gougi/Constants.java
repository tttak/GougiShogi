package co.jp.shogi.gougi;

import java.util.ArrayList;
import java.util.List;

/**
 * �萔��`
 */
public class Constants {

	/** usi�R�}���h�ɕԂ��uid name�v */
	public static final String USI_ID_NAME = "SimpleGougiShogi_20170113";
	/** usi�R�}���h�ɕԂ��uid author�v */
	public static final String USI_ID_AUTHOR = "t";

	/** �ݒ�t�@�C���̃t�@�C���� */
	public static final String CONFIG_FILENAME = "SimpleGougiShogi.config";

	/** USI�G���W�����X�g�̍ő匏�� */
	public static final int MAX_ENGINE_COUNT = 10;
	/** ���������c�i3�ҁj�̏ꍇ��USI�G���W�����X�g�̌��� */
	public static final int TASUUKETSU_3_ENGINE_COUNT = 3;

	/** �y���c�^�C�v�z���������c�i3�ҁj */
	public static final String GOUGI_TYPE_TASUUKETSU_3 = "���������c�i3�ҁj";
	/** �y���c�^�C�v�z�y�ύ��c */
	public static final String GOUGI_TYPE_RAKKAN = "�y�ύ��c";
	/** �y���c�^�C�v�z�ߊύ��c */
	public static final String GOUGI_TYPE_HIKAN = "�ߊύ��c";

	/** ���c�^�C�v�̃��X�g */
	public static final List<String> GOUGI_TYPE_LIST;
	static {
		GOUGI_TYPE_LIST = new ArrayList<String>();
		GOUGI_TYPE_LIST.add(GOUGI_TYPE_TASUUKETSU_3);
		GOUGI_TYPE_LIST.add(GOUGI_TYPE_RAKKAN);
		GOUGI_TYPE_LIST.add(GOUGI_TYPE_HIKAN);
	}

	/** �]���l���ݒ� */
	public static final int SCORE_NONE = Integer.MIN_VALUE + 1;
	/** Mate�̕]���l */
	public static final int SCORE_MATE = 100000;

}
