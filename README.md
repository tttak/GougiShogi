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
	- YaneuraOu_sfen変換用.exe （合議タイプ「詰探索エンジンとの合議（「脊尾詰」対応版）」及び「詰探索エンジンとの合議（読み筋の局面も詰探索）」の場合にのみ使用します）

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
「SimpleGougiShogi.config」で「多数決合議（3者）」「楽観合議」「悲観合議」「楽観合議と悲観合議を交互」「2手前の評価値からの上昇分の楽観合議」「2手前の評価値からの上昇分の悲観合議」「各々の最善手を交換して評価値の合計で判定（2者）」「数手ごとに対局者交代」「2手前の評価値から一定値以上下降したら対局者交代」「2手前の評価値から一定値以上上昇したら対局者交代」「詰探索エンジンとの合議」「詰探索エンジンとの合議（「脊尾詰」対応版）」「詰探索エンジンとの合議（読み筋の局面も詰探索）」のいずれかを設定してください。  

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

- 数手ごとに対局者交代  
合議とは少し違うかもしれませんが、数手ごとに対局者を交代します。  
何手ごとに交代するかは、将棋所等のエンジン設定画面で「G_ChangePlayerPlys」の値で指定してください。  

- 2手前の評価値から一定値以上下降したら対局者交代  
同じ将棋ソフトの現局面の評価値と2手前の評価値を比較し、一定値以上下降した場合に対局者を交代します。  
上記「一定値」は、将棋所等のエンジン設定画面で「G_ChangePlayerScoreDiff」で指定してください。  

- 2手前の評価値から一定値以上上昇したら対局者交代  
同じ将棋ソフトの現局面の評価値と2手前の評価値を比較し、一定値以上上昇した場合に対局者を交代します。  
上記「一定値」は、将棋所等のエンジン設定画面で「G_ChangePlayerScoreDiff」で指定してください。  

- 詰探索エンジンとの合議  
通常の将棋ソフトと詰探索エンジンで合議を行います。  
詰探索エンジンが詰みを見つけた場合にのみ、その指し手を採用します。  
将棋所等のエンジン設定画面で、詰探索エンジンのタイムアウト（ミリ秒）を「G_MateTimeout」で指定してください。  

- 詰探索エンジンとの合議 （「脊尾詰」対応版）  
上記「詰探索エンジンとの合議」の「脊尾詰」対応版です。  
「SimpleGougiShogi.jar」や「SimpleGougiShogi.config」と同じフォルダに「YaneuraOu_sfen変換用.exe」を配置しておいてください。  
「YaneuraOu_sfen変換用.exe」は、例えば「position startpos moves 7g7f」から「position sfen lnsgkgsnl/1r5b1/ppppppppp/9/9/2P6/PP1PPPPPP/1B5R1/LNSGKGSNL w - 2」のように、局面の情報をsfen文字列へ変換するためにのみ使用しています。  
変換後のsfen文字列を「脊尾詰」に送信します。  

- 詰探索エンジンとの合議（読み筋の局面も詰探索）  
上記「詰探索エンジンとの合議」および「詰探索エンジンとの合議 （「脊尾詰」対応版）」では現局面だけを詰探索エンジンに送りますが、  
合議タイプ「詰探索エンジンとの合議（読み筋の局面も詰探索）」では、通常の探索エンジンから送られてきた読み筋の局面も詰探索エンジンに送ります。  
読み筋の局面で詰みが見つかった場合、通常の探索エンジンへmateinfoコマンド（当プログラム用の独自の拡張コマンド）で通知します。  
  - 読み筋の局面の詰探索に使用する詰探索エンジンの個数を「SimpleGougiShogi.config」で指定してください。  
  - 将棋所等のエンジン設定画面で、詰探索エンジンのタイムアウト（ミリ秒）を「G_MateTimeout」（現局面用）および「G_PvMateTimeout」（読み筋の局面用）で指定してください。  
  - 「SimpleGougiShogi.jar」や「SimpleGougiShogi.config」と同じフォルダに「YaneuraOu_sfen変換用.exe」を配置しておいてください。  
  - 通常の探索エンジン側がmateinfoコマンド（当プログラム用の独自の拡張コマンド）に対応している必要があります。  
    技巧とやねうら王のmateinfoコマンドのサンプルプログラム（mateinfoコマンドを受信すると置換表にmateの評価値を登録する）  
    - https://github.com/tttak/Gikou/commit/c3c93483c56148363e7defbfff6d48ba86906583
    - https://github.com/tttak/Gikou/releases
    - https://github.com/tttak/YaneuraOu/commit/bac386907212f8a5a300da495c1fa5f8d5a482fb
    - https://github.com/tttak/YaneuraOu/tree/MateInfo/exe/2017Early

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

- 合議アルゴリズム「詰探索エンジンとの合議」については、今のところ「なのは詰め」でのみ動作を確認しています。  

- 合議アルゴリズム「詰探索エンジンとの合議 （「脊尾詰」対応版）」で使用する「YaneuraOu_sfen変換用.exe」のソース  
https://github.com/tttak/YaneuraOu/commit/50bc1b7a375fc073d6c35e9b0a9a7b573124b9f9  

- 合議アルゴリズム「詰探索エンジンとの合議」および「詰探索エンジンとの合議 （「脊尾詰」対応版）」について、現状では以下の仕様になっています。  
（以下、例として技巧と脊尾詰の合議の場合で記載しますが、他のソフトでも同様です）
  - 脊尾詰が詰みを見つけた場合、脊尾詰の指し手を採用します。その他の場合、技巧の指し手を採用します。
  - 将棋所等のエンジン設定画面（脊尾詰のエンジン設定ではなくSimpleGougiShogiのエンジン設定）で、詰探索エンジンのタイムアウト（ミリ秒）を「G_MateTimeout」で指定してください。（タイムアウトを短くし過ぎると、脊尾詰が即詰みを逃す場合があります。）
  - 技巧から指し手が返ってきた時点で将棋所等に指し手を返します。
    - 技巧より先に脊尾詰から指し手が返ってきた場合、すぐに将棋所等に指し手を返すのではなく、技巧から指し手が返ってくるのを待ちます。
    - 脊尾詰より先に技巧から指し手が返ってきた場合、その時点で将棋所等に指し手を返しますが、脊尾詰はその後も思考を続けます。
      - その後、脊尾詰が詰みを見つけたとしても、指し手には反映されません。（技巧から短時間で指し手が返ってきた場合、結果的に即詰みを逃す場合があります。）
      - 局面が次の指し手に進んでも脊尾詰は前局面を思考し続けますので、脊尾詰のタイムアウトを長くし過ぎると脊尾詰が現局面を思考できず、結果的に即詰みを逃す場合があります。
      - 脊尾詰から返ってきたのが前局面の思考結果だった場合、その時点で現局面の思考開始コマンドを脊尾詰に送るようにしています。

