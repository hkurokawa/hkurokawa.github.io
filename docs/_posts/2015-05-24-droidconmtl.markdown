---
layout: post
title:  "Droidcon Montreal まとめ"
date:   2015-05-24 04:20:56.000000000 -0700
categories: 
---
すでに一ヶ月以上も経って、いまさら感満載だけれど、先月行ってきた Droidcon Montreal についてまとめておく。

手短に、技術的に気になった発表を中心に紹介したいと思う。

# TL;DR
- あこがれの Jake と Jesse と握手できたし、運営の Marcos にいろいろ聞けたので当初の目標は達成。
- Jake の okio の話と Matt の Debug Module の話が一番面白かった。Square の人は発表がうまい。
- Montreal は良いところ。また夏に、できたらカナダGPの時期に行きたい。
- [yanzm さんの記事](http://y-anz-m.blogspot.jp/2015/04/droidcon-montreal.html)も参照。

# Droidcon Montreal とは
[公式サイト](http://www.droidcon.ca/)

Droidcon は、世界各地で行っている Android 技術者のカンファレンス。それぞれの国や地域で Droidcon Italy や Droidcon Montreal のように、地域名を冠して開催している。

Droidcon Montreal は初開催で、4月9日と10日の二日にわたって開催された。モントリオールだけでなくカナダの Droidcon としても初めてらしい。参加者の多くは地元のカナダの人かアメリカの人だったようで、アジア人は目立っていた。でも、おかげで、お昼のときに「日本からわざわざこのために来たの？遠いねぇ」と話しかけられて、初めて日本人で得したと思った。

様子は、以下のハイライト動画と [flickr](https://www.flickr.com/photos/droidconmtl/sets/) を見ると分かると思う。ちなみに、自分がしっかり映っていることに気付いたので、暇な人は探してみてください。

<iframe width="560" height="315" src="https://www.youtube.com/embed/aVRzbH8TgdE" frameborder="0" allowfullscreen></iframe>

# 1日目
## Welcome to Droidcon! by Marcos Damasceno and Philippe Breault (Mirego)
Droicon の歴史や、今回の Droidcon Montreal にまつわる数字について。参加者の多様性について言及したときに日本人と台湾人の数が出ていたが、やはりアジア人は少数だったようだ。

## Keynote: An Open Source Advantage by Jake Wharton and Jesse Wilson (Square)
オープンソース活動の進め方やどのようにオープンソースライブラリを選ぶかについての発表。

動画
<iframe width="560" height="315" src="https://www.youtube.com/embed/PCxz2LEmuL4" frameborder="0" allowfullscreen></iframe>

スライド
<script async class="speakerdeck-embed" data-id="018fdc6a90ef4a56b76d56c45b5c8e4d" data-ratio="1.77777777777778" src="//speakerdeck.com/assets/embed.js"></script>

ライブラリの機能について、全員の言う「必須機能」を実装していたら肥大化してしまうので全員が必要な機能だけ実装して、残りは実装しない、必要に応じて拡張可能にしておくことが望ましい。という意見が印象に残った。Square のライブラリはまさにそういう感じで、必要最小限の機能に絞っているところがクールだし、使いやすいところだと思う。とはいえ、拡張可能な API の設計というのが、これが本当に難しいと Jake も言っていたけれど。

ちなみに、このキーノートの後に、@zaki50 さんが Jake と Jesse に和菓子を渡しに行くのに付いていって、握手してもらったり写真を撮ってもらった。これで今回の目的の半分は達成。

## A Few "OK" Libraries by Jake Wharton (Square)

OK ライブラリ、つまり okio, okhttp, moshi, retrofit についての説明。Jake 節全開でおもしろい。

<script async class="speakerdeck-embed" data-id="a1ff81b8a55549e9b96ede49a7887a41" data-ratio="1.77777777777778" src="//speakerdeck.com/assets/embed.js"></script>

okio は java.io.* や java.nio.* の代替として生まれた I/O API ライブラリ。java.io.* の不満点は

- skip は何回も呼ばないと指定したバイト数だけスキップすることができない
- isMarkable とか、色々面倒なメソッドがあって抽象化されきっていない
- DataInputStream と InputStreamReader って一緒に使えないんだぜ？

とのこと。java.io.* や java.nio.* の問題点を、HTTP リクエストを読み込む場合を例に説明していた。

okio の概要としては、

- ByteString というバイト配列を扱うセグメントと、その連結リストで表される Buffer で、読み込んだ/書き込むデータを保持する
- Sink と Source がそれらの読み書きを表すインタフェース（Java の InputStream と OutputStream に対応する）を提供する

という感じで、API がシンプルになっている点と、バッファをセグメントの連結リストにしているところが特徴に見える。

API のシンプルさについては、たとえば、BufferedSource に数値型や文字列型の読み書きの API があるので、この手のデータ処理は書きやすい（標準パッケージだと DataInputStream で似た API が提供されている）。あと、[この Jesse のブログ記事](https://publicobject.com/2015/05/16/no-beer-emoji-for-java-io-reader/)にもあるように、サロゲートペアなどを扱うのも容易になっているとのこと。

セグメントの連結リストについては、推測だけれど、バッファをセグメントの連結リストにすることで、たとえばバッファの容量が足りなくなったときにセグメントプールのセグメントを使い回せるので、配列の確保などのコストが減るのだろう。Java 標準パッケージの Buffer でも似たようなことができるが、あちらは自分でポジション移動や容量の変更などをやらないといけないので、ちょっと面倒ということだろうか。

その後は、okio をベースにしたライブラリ、OkHttp、Retrofit、Moshi の説明。

後続のライブラリは置いておくとして、okio の印象は、java.io.* の使い難さ、たとえば API のイケてなさとか InputStream と Reader の曖昧さ（まぁ、Java は歴史が長いので……）などに業を煮やして自分で作っちゃった、という印象。個人的には API のシンプルさ以外に、これをわざわざ使う理由があれば使っても良いかな、という程度。パフォーマンス上有利になるんだったら使いたいけれど、どうなんだろう。

## HTTP in a Hostile World by Jesse Wilson (Square)

こちらは、同じく Square の Jesse による HTTP の世界の話。

<script async class="speakerdeck-embed" data-id="a5f37da026f80132f6ff42a09a3cc4e8" data-ratio="1.33333333333333" src="//speakerdeck.com/assets/embed.js"></script>

内容は多岐に渡るので、上記のスライドを見てほしいが、気になった点としては、以下のあたり。

- Gzip をかけよう。サイズを 70% カットできる。
- 画像には `Cache-Control` ヘッダを付けよう。キャッシュするかどうかの目安は 50MB（大きすぎる気がするので聴き取りをミスったかも）。
- 送信データはローカルに保存してリトライできるようにしよう。
- セキュリティは非常に難しい問題。一部の ISP は広告を混ぜてくる（本当？）

# 2日目

## Debug Builds: A New Hope by Matt Precious (Square)

Square ばかりで恐縮だけれど、これで最後なので平にご容赦を。こちらは、Square の新人 Matt によるデバッグビルドの作り方の発表。内容は基本的に https://github.com/JakeWharton/u2020 の説明。

本題と逸れるが、早めに会場に着いたら、Matt が Jake とリハーサルしている現場に出くわした。細かいところも指導していて、Jake のプレゼンに対する意識が伺えたところが面白かった。

<script async class="speakerdeck-embed" data-id="b61471e3f5234458a9264fc62999c2bd" data-ratio="1.77777777777778" src="//speakerdeck.com/assets/embed.js"></script>

量が多いので、以下箇条書きで。

- どのスクリーンでも debug build は右から左にスワイプすることで Debug Drawer を表示できる
  - AppContainer という interface を用意し、各 Activity に Dagger で Inject することで実現している
- Endpoint の切り替え
  - ApiModule というクラスを用意して、それ経由で Endpoint を与えるようにする
  - DebugApiModule では、StringPreference から使用する Endpoint を決定するようにする
  - なお、エンドポイント変更後は ProcessPheonix を利用してアプリ自体を再起動する
- Proxy を挟む
  - Charles など Proxy を挟んでネットワーク通信を解析・改竄をしたい場合に使用する
  - OkHttpClient#setProxy() を利用する（SSLSocketFactory には Proxy が解析できるように安全でない設定を行う）
- Mock Mode
  - サーバーと直接通信しないモード
  - API EndPoint に MOCK を足して、エンドポイントが MOCK だったら、Retrofit のサービス取得時に MockService を返すようにする
- エラーケース
  - 正常系はこれでモックできたが、異常系はどうするか
  - 遅延と通信エラーについては、値を指定できるようにして、MockRestAdapter をインスタンス化するときに、それらの保存された値を見るようにする
  - レスポンスを変更したい場合は MockService に結果をセットできるようにする
- Intent の捕捉
  - 外部に飛ぶ Intent を捕捉して中身を見る画面を用意する
  - 捕捉フラグが true になっていたら、すべての intent を捕まえて、中身を表示する画面に遷移する
- Bug Reporting
  - 2本指で長押しするとレポートダイアログが開く。スクリーンショットと詳細を書き込める。
  - Telescope というライブラリを用い、すべての Activity の画面の親に TelescopeLayout を差し込むことで、レポート時にスクリーンショットを受け取ることができる
- その他の要素
  - Debug Activity
    - Main Actiivty が起動しない場合に、Debug View のみの Activity を起動できるようにする
  - Contextual Actions
    - 正常なデビットカード番号を入力できた/できない、を切り替えられるように、正常番号を自動入力するボタンを Debug View に用意する

これはなかなかエキサイティングな発表だった。やっていることは豊富だが、基本的に Dagger や Retrofit を利用しているだけ。それでも、ここまで出来るのがすごいし、Dagger を使いたくなる。こういうのを見せられると Square 教の信者になってしまいそう。

## getBounds(), the Drawable Story by Jamie Huson and Lisa Neigut (Etsy)

Etsy の中の人による Drawable の話。スライドは上がっていないが、下記の Devoxx 2013 でした話の続きらしい。

<script async class="speakerdeck-embed" data-id="bd2215f0241e013299fe7e85eb08a929" data-ratio="1.77777777777778" src="//speakerdeck.com/assets/embed.js"></script>

Custom Drawable の作り方や Drawable と View の連携などの基本的な話に始まって、後半は Lollipop で導入された種々の API について。

レンダリングのパフォーマンスについては https://source.android.com/devices/graphics/index.html や [Android Graphics Pipeline: From Button to Framebuffer (Part 1) | inovex Blog ](https://blog.inovex.de/android-graphics-pipeline-from-button-to-framebuffer-part-1/) を読もう、とのこと。

## Mastering Recyclerview Layouts by Dave Smith (New Circle, Inc.)

Lollipop から追加された Recyclerview と、それをカスタマイズする方法。非常に分かりやすいプレゼンで、RecyclerView 初心者の自分には、とても良かった。

ところで、プレゼン中に「誰か Custom LayoutManager 作ったことある人いる？」と会場に尋ねて、手を挙げた人に「ああ、みんな髪の毛あって良かった」とジョークを言っていたのだけれど、やはり Custom LayoutManager は大変なのか……。

<iframe src="//www.slideshare.net/slideshow/embed_code/key/5ds2H14x5UwZPz" width="425" height="355" frameborder="0" marginwidth="0" marginheight="0" scrolling="no" style="border:1px solid #CCC; border-width:1px; margin-bottom:5px; max-width: 100%;" allowfullscreen> </iframe> <div style="margin-bottom:5px"> <strong> <a href="//www.slideshare.net/devunwired/mastering-recyclerview-layouts" title="Mastering RecyclerView Layouts" target="_blank">Mastering RecyclerView Layouts</a> </strong> from <strong><a href="//www.slideshare.net/devunwired" target="_blank">Dave Smith</a></strong> </div>

資料がよく出来ているので、手元のメモだけ。

- RecyclerView は LayoutManager が必須（というかセットになっている）
- Custom LayoutManager を作る上で気を付けるべきこと
  - View を detatchAndScrapView() で Scrap Heap に移動しても、親 View への参照は残っているし、必要ならすぐに戻せてコストは安い
  - 一方、removeAndRecycleView() をしてしまうと、親 View への参照は消えて、メタデータなども消えるので、戻すコストは高くなる
  - fillGaps() の中では、すべての View を scrap してからレイアウトし直すのが良い
  - View に state はできるだけ持たせない方が良い
- コードは https://github.com/devunwired/recyclerview-playground を参照
- ブログ記事 [Building A RecyclerView LayoutManager – Redux | Wires Are Obsolete](http://wiresareobsolete.com/2015/02/recyclerview-layoutmanager-redux/) も読んでね

## Introduction to Functional Reactive Programming on Android by Juan Gomez (Netflix)

本場 Netflix の人による RFP だ、というので期待していたが、やはり Introduction ということで入門止まり。残念。Cold/Hot Observable の話を除けば [ninjinkun が翻訳したこの記事](http://ninjinkun.hatenablog.com/entry/introrxja)と大差ない内容という印象。

聴いている Jake がとても退屈そうなのが印象深かった。

<script async class="speakerdeck-embed" data-id="f20ac08ce1f349d39d6bd572e81d2a7a" data-ratio="1.33333333333333" src="//speakerdeck.com/assets/embed.js"></script>

## Retrofit: Obliterating HTTP Boilerplate by Jacob Tabak (Timehop)

Retrofit の非常によくまとまった解説。Retrofit を使ったことある人にはすでに知っている内容も多かったが、後半のテスト方法はなかなか興味深かった。

<script async class="speakerdeck-embed" data-id="2ad21376bc95451fa9807619994509a5" data-ratio="1.77777777777778" src="//speakerdeck.com/assets/embed.js"></script>

以下、メモ。

- Retrofit は Annotation Processor ではなく Java の Proxy を使っている(リフレクション)
- Retrofit のリクエストの種類は3種類
  - Synchronous
  - Asynchronous
  - RxJava (← New!)
- RxJava の利点
  - 依存リクエストを1つにまとめられる
  - 複数のリクエストの待ち合わせが簡単
  - エラーハンドラーが1つで良い
  - スレッドの指定が楽
  - レスポンスが返るタイミングを制御可能(テストの時に役立つ)
- Testing
  - Retrofit-Mock という Retrofit の付属ライブラリを利用すると楽
  - MockWebServer を用意しよう
  - Espresso + Dagger で、別スレッドの完了を待つことができる
  - Mockito + RxPublishSubject でレスポンスのタイミングを制御したテストが書ける
- 2.0
  - Retrofit 2.0 では、Rx サポートでいびつになったところが直るよ!

## おまけ

今回の Droidcon Montreal に行った目的の1つは、いつか Droidcon Tokyo を開催する上で参考にしよう、というもの。途中で運営の Marcos を捕まえて20分くらい運営の話を聞けたのが良かった。その話は、またいずれどこかで。

下の写真は、Marcos を質問攻めにする筆者。

[f:id:hydrakecat:20150524201723j:plain]

## おまけ（その2）

モントリオールといえば、カナダGP、ジル・ヴィルヌーヴ・サーキット! ということで、お昼の時間に行ってきた。会場からは地下鉄と徒歩で20分くらい、とそんなに遠くないのだけれど、いかんせん、雪の中（4月はまだ雪が残っているのだ）行くには適している場所ではなかった。

それでも、テレビでよく見るバックストレッチが見れたし、行って良かった。次は是非開催中に行きたい。

[f:id:hydrakecat:20150524201821j:plain]