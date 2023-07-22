---
layout: post
title:  "神聖な牛をステーキにする"
date:   2017-08-31 07:52:39.000000000 -0700
categories: 
---
JavaZone 2017でKevlin Henneyという人が行った[Making Steaks from Sacred Cowsというトーク](https://vimeo.com/105758303)がたまたま目に止まってみたのだけれど、たいへん面白かった。

備忘として内容を簡単にメモする。なお、これは翻訳ではない。また、細部は理解できない部分もあったので、間違っている箇所があったら遠慮なく指摘いただけると幸いである。

<iframe src="https://player.vimeo.com/video/105758303" width="640" height="360" frameborder="0" webkitallowfullscreen mozallowfullscreen allowfullscreen></iframe>
<p><a href="https://vimeo.com/105758303">Making Steaks from Sacred Cows - Kevlin Henney</a> from <a href="https://vimeo.com/javazone">JavaZone</a> on <a href="https://vimeo.com">Vimeo</a>.</p>

<!--more-->

内容を一言でいえば「習慣としてみなが盲目的に従っているプログラミングのベストプラクティス（とされているもの）に疑問を投げかける」というものである。みなが神聖視している習慣が牛で、それを料理するというわけだ。本人も冒頭でいっているが、なかなか刺激的なタイトルだ。

なお、イベント名がJavaを含んでいるのもあって、言語はJavaが使われている。

## 1. import文のワイルドカード
import文でワイルドカードを使うのは悪いとされている。StackOverflowで「import文がたくさん並んでいてゴチャゴチャしているのだけれど、なんとかできないのか」という質問（オリジナルは[こちら](https://stackoverflow.com/questions/8485689/too-many-imports-spamming-my-code)）があったときに並んだ答えはつぎのようなものであった。

- パッケージ全体をインポートするより必要な個々のクラスをインポートすべきである
- IDEを利用していれば、折り畳んでくれる。パッケージをインポートするのは良いプラクティスではない

これらは、「なぜワイルドカードインポートが良くないのか」に答えていない。IDEが折り畳んでくれるから良いというのはナンセンスだ。我々はIDEを利用する側であって、IDEが我々のコーディングスタイルを決めるべきではない。コードは人間が読むためのもので、だから人間に読みやすいものであるべきだし、そのためには大量のimport文は書くべきではない。

実際のところ、大量のimport文を並べるよりもワイルドカードを使うべきであるとロバート・C・マーティン[^1]の[Clean Code](https://www.amazon.com/Clean-Code-Handbook-Software-Craftsmanship/dp/0132350882)でも述べられている。

中には「名前が衝突した場合に困る」という理由を述べるものもあるが、つぎのように明示的にインポートすれば済む話である。

```java
import java.awt.*;
import java.util.*;
import java.util.List;

...
List list;
```

[^1]: 後の方でも出てくるけれどアンクル・ボブとして著名なソフトウェア・エンジニア

### 感想
Javaの仕様によると、複数のワイルドカードimportされたパッケージでクラス名が被った場合はコンパイルエラーになるので、誤って期待と異なるパッケージのクラスが使われることはない。

しかし、自身のパッケージ内に同名のクラスがあった場合はそちらが優先される。つまり

```java
package foo;

class List {}
```

```java
package foo;

improt java.util.*;

...
List list;
```

とあった場合、この`List`は`java.util.List`ではなく`foo.List`が使われる。これがコーディングの際に自明かといわれると、ちょっと怪しいのではないだろうか。もちろんIDEを使っていれば気付くかもしれないけれど、IDEに魂を渡すなといっているのもKelvinである。

個人的には、いまどきのエディタは折り畳みくらいするだろうから、べつにワイルドカードを使わないでクラス1つ1つをインポートしてもよいんじゃないかと思っているし、おなじパッケージからいくつものクラスをインポートするならワイルドカードを使ってもよいと思っている。それこそ、あまりドグマティックになって議論するところでもないんじゃないかな、というのが正直な感想である。
