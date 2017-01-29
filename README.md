# シンプル合議将棋

コンピュータ将棋のシンプルな合議プログラムです。  
将棋所等と複数の将棋ソフト（USIエンジン）の間を中継し、合議結果を将棋所等に返します。  

※ご使用は自己責任でお願いいたします。

## 動作環境
当プログラムはJavaで作成されていますので、実行にはJREが必要です。  

## 使用方法

1. githubからexeフォルダ内のファイルをダウンロードし、任意のフォルダに配置します。  
	- SimpleGougiShogi.bat
	- SimpleGougiShogi.jar
	- SimpleGougiShogi.config

2. 「SimpleGougiShogi.config」で合議タイプと将棋ソフト（USIエンジン）を設定します。  
	設定例
	```
	Gougi.Type=多数決合議（3者）
	Engine1.Path=C:\将棋\Apery\ukamuse_sdt4.exe
	Engine2.Path=C:\将棋\Gikou\gikou.exe
	Engine3.Path=C:\将棋\nozomi\nozomi.exe
	```

3. 将棋所、ShogiGUI等に当プログラムをエンジン登録します。  
	- 将棋所の場合  
	「SimpleGougiShogi.jar」をエンジン登録します。  
	- ShogiGUIの場合  
	「SimpleGougiShogi.bat」をエンジン登録します。  

4.  エンジン設定画面で3種類の将棋ソフトのオプションを設定します。  
例えばエンジン2のオプション「OwnBook」は、エンジン設定画面で「E2_OwnBook」と表示されます。  

5.  対局を開始します。

## 合議アルゴリズム
「SimpleGougiShogi.config」で「多数決合議（3者）」「楽観合議」「悲観合議」「楽観合議と悲観合議を交互」「2手前の評価値からの上昇分の楽観合議」「2手前の評価値からの上昇分の悲観合議」「各々の最善手を交換して評価値の合計で判定（2者）」のいずれかを設定してください。  

- 多数決合議（3者）  
3種類の将棋ソフトの多数決で指し手を決定します。  
	- 「3対0」の場合、その指し手が採用されます。  
	- 「2対1」の場合、2票の方の指し手が採用されます。  
	- 「1対1対1」の場合、「エンジン1」の指し手が採用されます。  

- 楽観合議  
複数の将棋ソフトのうち、評価値の最も高いソフトの指し手を採用します。  
評価値の最も高いソフトが同点で複数存在する場合、エンジン番号の小さなソフトが優先されます。  

- 悲観合議  
複数の将棋ソフトのうち、評価値の最も低いソフトの指し手を採用します。  
評価値の最も低いソフトが同点で複数存在する場合、エンジン番号の小さなソフトが優先されます。  

- 楽観合議と悲観合議を交互  
楽観合議と悲観合議を交互に行います。  

- 2手前の評価値からの上昇分の楽観合議  
複数の将棋ソフトのうち、「現局面の評価値 - 2手前の評価値」の最も大きなソフトの指し手を採用します。  
「現局面の評価値 - 2手前の評価値」の最も大きなソフトが同点で複数存在する場合、エンジン番号の小さなソフトが優先されます。  

- 2手前の評価値からの上昇分の悲観合議  
複数の将棋ソフトのうち、「現局面の評価値 - 2手前の評価値」の最も小さなソフトの指し手を採用します。  
「現局面の評価値 - 2手前の評価値」の最も小さなソフトが同点で複数存在する場合、エンジン番号の小さなソフトが優先されます。  

- 各々の最善手を交換して評価値の合計で判定（2者）  
2種類の将棋ソフトで、まず持ち時間の0.45倍の時間で思考し、最善手が一致した場合はその指し手を採用します。  
最善手が一致しなかった場合はお互いの最善手を交換して1手進めた局面について持ち時間の0.45倍の時間で思考し、両ソフトの評価値の合計が大きい方の指し手を採用します。  


## 留意事項
- 短い時間の対局では切れ負けすることがありますのでご留意頂ければと思います。  

- ponder（相手の手番中に先読み）にも一応は対応していますが、今のところ動作が不安定ですのでご留意頂ければと思います。  

- 合議アルゴリズム「各々の最善手を交換して評価値の合計で判定（2者）」で技巧を使用し、かつ、ponderを使用した場合、  
対局中に技巧が異常終了してしまう場合があるようです。  

- 将棋所等の「読み筋」欄に、各将棋ソフトから返された指し手及び評価値と合議結果が表示されます。  
	表示例  
	`[O] bestmove 7g7f [７六(77)] [評価値 50 （前回40 差分10）] [ukamuse_SDT4]`  
	`[X] bestmove 2g2f [２六(27)] [評価値 -8 （前回20 差分-28）] [Gikou 20160606]`  
	`[O] bestmove 7g7f [７六(77)] [評価値 33 （前回-5 差分38）] [nozomi 20161015] `  
	※駒打ちではなく駒の移動の指し手の場合、駒の種類は表示されず、移動元と移動先のみが表示されます。  

