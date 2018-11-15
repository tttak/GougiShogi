package co.jp.shogi.gougi;

import java.util.ArrayList;
import java.util.List;

/**
 * �萔��`
 */
public class Constants {

	/** usi�R�}���h�ɕԂ��uid name�v */
	public static final String USI_ID_NAME = "GougiShogi_20181115";
	/** usi�R�}���h�ɕԂ��uid author�v */
	public static final String USI_ID_AUTHOR = "t";

	/** �ݒ�t�@�C���̃t�@�C���� */
	public static final String CONFIG_FILENAME = "GougiShogi.config";

	/** USI�G���W�����X�g�̍ő匏�� */
	public static final int ENGINE_COUNT_MAX = 10;
	/** �u���������c�i3�ҁj�v�̏ꍇ��USI�G���W�����X�g�̌��� */
	public static final int ENGINE_COUNT_TASUUKETSU_3 = 3;
	/** �u�e�X�̍őP����������ĕ]���l�̍��v�Ŕ���i2�ҁj�v�̏ꍇ��USI�G���W�����X�g�̌��� */
	public static final int ENGINE_COUNT_BESTMOVE_EXCHANGE_2 = 2;
	/** �u���ՁE���ՁE�I�Ղő΋ǎҌ��v�̏ꍇ��USI�G���W�����X�g�̌��� */
	public static final int ENGINE_COUNT_CHANGE_PLAYER_JOBAN_CHUUBAN_SHUUBAN = 3;
	/** �u�l�T���G���W���Ƃ̍��c�v�̏ꍇ��USI�G���W�����X�g�̌��� */
	public static final int ENGINE_COUNT_MATE = 1;

	/** �y���c�^�C�v�z���������c�i3�ҁj */
	public static final String GOUGI_TYPE_TASUUKETSU_3 = "���������c�i3�ҁj";
	/** �y���c�^�C�v�z�y�ύ��c */
	public static final String GOUGI_TYPE_RAKKAN = "�y�ύ��c";
	/** �y���c�^�C�v�z�ߊύ��c */
	public static final String GOUGI_TYPE_HIKAN = "�ߊύ��c";
	/** �y���c�^�C�v�z�y�ύ��c�Ɣߊύ��c������ */
	public static final String GOUGI_TYPE_RAKKAN_HIKAN_BYTURNS = "�y�ύ��c�Ɣߊύ��c������";
	/** �y���c�^�C�v�z2��O�̕]���l����̏㏸���̊y�ύ��c */
	public static final String GOUGI_TYPE_2TEMAE_JOUSHOU_RAKKAN = "2��O�̕]���l����̏㏸���̊y�ύ��c";
	/** �y���c�^�C�v�z2��O�̕]���l����̏㏸���̔ߊύ��c */
	public static final String GOUGI_TYPE_2TEMAE_JOUSHOU_HIKAN = "2��O�̕]���l����̏㏸���̔ߊύ��c";
	/** �y���c�^�C�v�z�e�X�̍őP����������ĕ]���l�̍��v�Ŕ���i2�ҁj */
	public static final String GOUGI_TYPE_BESTMOVE_EXCHANGE_2 = "�e�X�̍őP����������ĕ]���l�̍��v�Ŕ���i2�ҁj";
	/** �y���c�^�C�v�z���育�Ƃɑ΋ǎҌ�� */
	public static final String GOUGI_TYPE_CHANGE_PLAYER_PLYS = "���育�Ƃɑ΋ǎҌ��";
	/** �y���c�^�C�v�z���ՁE���ՁE�I�Ղő΋ǎҌ�� */
	public static final String GOUGI_TYPE_CHANGE_PLAYER_JOBAN_CHUUBAN_SHUUBAN = "���ՁE���ՁE�I�Ղő΋ǎҌ��";
	/** �y���c�^�C�v�z2��O�̕]���l������l�ȏ㉺�~������΋ǎҌ�� */
	public static final String GOUGI_TYPE_CHANGE_PLAYER_SCORE_DOWN = "2��O�̕]���l������l�ȏ㉺�~������΋ǎҌ��";
	/** �y���c�^�C�v�z2��O�̕]���l������l�ȏ�㏸������΋ǎҌ�� */
	public static final String GOUGI_TYPE_CHANGE_PLAYER_SCORE_UP = "2��O�̕]���l������l�ȏ�㏸������΋ǎҌ��";
	/** �y���c�^�C�v�z�l�T���G���W���Ƃ̍��c */
	public static final String GOUGI_TYPE_MATE = "�l�T���G���W���Ƃ̍��c";
	/** �y���c�^�C�v�z�l�T���G���W���Ƃ̍��c�i�u�Ҕ��l�v�Ή��Łj */
	public static final String GOUGI_TYPE_MATE_SEOTSUME = "�l�T���G���W���Ƃ̍��c�i�u�Ҕ��l�v�Ή��Łj";
	/** �y���c�^�C�v�z�l�T���G���W���Ƃ̍��c�i�ǂ݋؂̋ǖʂ��l�T���j */
	public static final String GOUGI_TYPE_MATE_PV = "�l�T���G���W���Ƃ̍��c�i�ǂ݋؂̋ǖʂ��l�T���j";

	/** ���c�^�C�v�̃��X�g */
	public static final List<String> GOUGI_TYPE_LIST;
	static {
		GOUGI_TYPE_LIST = new ArrayList<String>();
		GOUGI_TYPE_LIST.add(GOUGI_TYPE_TASUUKETSU_3);
		GOUGI_TYPE_LIST.add(GOUGI_TYPE_RAKKAN);
		GOUGI_TYPE_LIST.add(GOUGI_TYPE_HIKAN);
		GOUGI_TYPE_LIST.add(GOUGI_TYPE_RAKKAN_HIKAN_BYTURNS);
		GOUGI_TYPE_LIST.add(GOUGI_TYPE_2TEMAE_JOUSHOU_RAKKAN);
		GOUGI_TYPE_LIST.add(GOUGI_TYPE_2TEMAE_JOUSHOU_HIKAN);
		GOUGI_TYPE_LIST.add(GOUGI_TYPE_BESTMOVE_EXCHANGE_2);
		GOUGI_TYPE_LIST.add(GOUGI_TYPE_CHANGE_PLAYER_PLYS);
		GOUGI_TYPE_LIST.add(GOUGI_TYPE_CHANGE_PLAYER_JOBAN_CHUUBAN_SHUUBAN);
		GOUGI_TYPE_LIST.add(GOUGI_TYPE_CHANGE_PLAYER_SCORE_DOWN);
		GOUGI_TYPE_LIST.add(GOUGI_TYPE_CHANGE_PLAYER_SCORE_UP);
		GOUGI_TYPE_LIST.add(GOUGI_TYPE_MATE);
		GOUGI_TYPE_LIST.add(GOUGI_TYPE_MATE_SEOTSUME);
		GOUGI_TYPE_LIST.add(GOUGI_TYPE_MATE_PV);
	}

	/** �]���l���ݒ� */
	public static final int SCORE_NONE = Integer.MIN_VALUE + 1;
	/** Mate�̕]���l */
	public static final int SCORE_MATE = 100000;

	/** sfen�ϊ��p��˂��牤 */
	public static final String SFEN_YANEURAOU_FILENAME = "YaneuraOu_sfen�ϊ��p.exe";

	/** ���c�^�C�v�u�l�T���G���W���Ƃ̍��c�i�ǂ݋؂̋ǖʂ��l�T���j�v�̏ꍇ�ɁA�ǂ݋؋ǖʂ�T������l�T���G���W���̐��̃f�t�H���g�l */
	public static final int PV_MATE_ENGINE_CNT_DEFAULT = 3;

}
