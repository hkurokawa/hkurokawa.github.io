---
layout: post
title:  "浮動小数点数のパーサを書いてみた"
date:   2020-10-31 19:06:45.439851967 -0700
categories: 
---

先日、こういうツイートを見かけた。で、さいきん浮動小数点数づいているのもあって自分も浮動小数点数の文字列表記をパースして単精度および倍精度浮動小数点数に変換するパーサを書いてみた。

<blockquote class="twitter-tweet"><p lang="ja" dir="ltr">非常に苦労したので書いた<br>もっといいやり方がありそうだし、もっとうまい説明ができるかもしれないけどこれが限界でした・・・<br><br>文字列少数点数表記を IEEE754 倍精度浮動小数点数にエンコードする方法｜Sukesan1984 <a href="https://twitter.com/hashtag/note?src=hash&amp;ref_src=twsrc%5Etfw">#note</a> <a href="https://t.co/2v5f1eMzea">https://t.co/2v5f1eMzea</a></p>&mdash; Sukesan1984 (@sukesan1984) <a href="https://twitter.com/sukesan1984/status/1319270055390048256?ref_src=twsrc%5Etfw">October 22, 2020</a></blockquote> <script async src="https://platform.twitter.com/widgets.js" charset="utf-8"></script>

この記事では、自分が実装したパーサを説明する。説明はいいからコードを見せろ、という人はこちらをどうぞ。

[https://github.com/hkurokawa/FloatingPointNumberParser](https://github.com/hkurokawa/FloatingPointNumberParser)

<!--more-->

# ゴール
今回は浮動小数点数の理解を深めるために自分でパーサを書くのが目的である。したがって、パフォーマンスは気にしない。また、シンタックスエラーも気にしないことにした[^1]。さいわい先ほど引用したツイートへの[補足ツイート](https://twitter.com/sukesan1984/status/1319289881110048776?s=20)にもあるがGo言語の標準ライブラリのテストケースがちょうどよさそうなので、このテストが通ることを目標とする。

[https://golang.org/src/strconv/atof_test.go](https://golang.org/src/strconv/atof_test.go)

[^1]: たとえばJavaの浮動小数点数リテラル表記としては正しくない `1.p` といった表記も今回は受容している。

# 実装方針
基本的には[Wikipediaの記事](https://ja.wikipedia.org/wiki/%E5%8D%98%E7%B2%BE%E5%BA%A6%E6%B5%AE%E5%8B%95%E5%B0%8F%E6%95%B0%E7%82%B9%E6%95%B0#%E5%8D%81%E9%80%B2%E8%A1%A8%E7%8F%BE%E3%81%8B%E3%82%89_binary32_%E3%83%95%E3%82%A9%E3%83%BC%E3%83%9E%E3%83%83%E3%83%88%E3%81%B8%E3%81%AE%E5%A4%89%E6%8F%9B)にあるのと同じことをすればよい。

入力を10進数表記の文字列としよう（2進数表記でも同じ議論が成り立つ）。たとえば `0.15625` といった文字列だ。これを2進数表記にしたい。最初に気付くのは、無限精度のままこの10進数表記の値を扱う必要があるということだ[^2]。一見、floatなら仮数部が23桁なので2進数表記で小数点以下23桁、切り上げや切り下げを考えても24桁まで見ればよさそうに思えるが、そうではない。最終的に最近接丸めをするには小数点以下24位が0だったとしても、そのさらに先で0以外の値があればそれは結果に影響する。したがって10進数表記の文字列を1文字目から順に見ていったとして、どこかでこれ以上読まなくてよいという場所は存在しない（ずっと0が続くのか、それともどこかで0以外が登場するのかは知る必要がある）。

[^2]: 厳密には無限精度である必要はないのだが、最終的に10進数表記でどれだけの有効桁数を考慮すればよいかというのはそれほど自明ではない。今回の実装では簡単のために無限精度で扱う。

以上のことから、まず10進数表記の文字列はそのまま無限精度の10進数の値として格納することにする。

つづいてこれを2進数表記に変換することを考えよう。整数部分については既知なので以降では小数部分についてだけ考える。

もっともナイーブな方法は変換したい値が[tex:{0.5}]より大きいか調べ、大きければ小数点以下第1位のビットを立てて[tex:{0.5}]を引く。つぎにその引いた結果と[tex:{0.25}]を比較し、それより大きければ小数点以下第2位のビットを立てる、という方法だ。この操作を行うと、たとえば10進表記の[tex:{0.15625}]という値は2進表記で[tex:{0.00101}]となることがわかる。

これは2進数の定義そのものなので理解はしやすいが、無限精度の10進数で引き算をやるのはあまりぞっとしない。ちょっと考えると、この[tex:{0.5}]を引いて、つぎは[tex:{0.25}]を引いて、という操作は、対象の値を[tex:{2}]倍して[tex:{1}]を引いて、さらに[tex:{2}]倍して[tex:{1}]を引くという操作と等価なことがわかる。じっさい、さきほどの[tex:{0.15625}]にこの操作を行うと同じ結果が得られる[^3]。

[^3]: 0.15625 → 0.3125 → 0.625 → 1.25 → 0.25 → 0.5 → 1.0 → 0.0

さて、以上で無限精度の10進数を2進表記する方法がわかった。また、浮動小数点数の定義上、この値を[tex: a \times 2^{k} ]という形式にする必要がある。ただし[tex:{a}]は範囲[tex:{[1, 2)}]に収まる実数。すなわち、対象の数に[tex:{2}]もしくは[tex:{\frac{1}{2}}]を掛けて[tex:{[1, 2)}]の範囲に収める必要がある。このことから、無限精度の10進数が[tex:{2}]倍および[tex:{\frac{1}{2}}]倍の演算をサポートしている必要がある。

# parseFloatのおおまかな実装
以下では、さきほど述べた[tex:{2}]倍および[tex:{\frac{1}{2}}]倍の演算をサポートした無限精度の10進数を表すクラス `BigDecimal` があるとする。これを使って値を浮動小数点数に変換する。

まずは[tex:{[1, 2)}]の範囲に収めよう。[tex:{2}]倍あるいは[tex:{\frac{1}{2}}]倍した場合はそれに合わせて指数部を増減させる。

```java
    BigDeciaml d = new BigDecimal(s); // 10進数文字列表記を無限精度の10進数に保存する
    int exponent = 0;
    while (d.isEqualToOrGreaterThanTwo()) { // 2以上なら2未満になるまで1/2を掛ける
      d.divideByTwo();
      exponent++;
    }
    while (d.isLessThanOne()) { // 1未満なら1以上になるまで2を掛ける
      d.multiplyByTwo();
      exponent--;
    }
    exponent += 127; // バイアスを足す
```

さて、ここで `d` は[tex:{[1, 2)}]の範囲に入っているはずである。あとはこの小数部を2進数表記すればよい。なお `d.discardNumberPart()` は整数部分を0クリアするメソッドである。

```java
    d.discardNumberPart();
    int mantissa = 0;
    for (int i = 22; i >= 0; i--) {
      d.multiplyByTwo();
      if (!d.isLessThanOne()) {
        mantissa |= 1 << i;
        d.discardNumberPart();
      }
    }
```

`d`を2倍して[tex:{1.0}]以上であればビットを立て、[tex:{1}]を引いている。

以上でほぼ終わりである。あとは符号部を組み合わせれば単精度浮動小数点数になる。

```java
    int sign = d.isNegative() ? 1 : 0;
    int bits = mantissa | (exponent << 23) | (sign << 31);
    return Float.intBitsToFloat(bits);
```

## いくつかのエッジケースの処理
ここまでで "1.0" や "3.1415" といった文字列はパースできるはずである。しかし、いくつかのエッジケースについて考慮が漏れている。

まずは "0.0" が与えられた場合。この場合はいくら2倍しても[tex:{1}]以上になることはないので無限ループになる。したがって "0" に等しい場合はearly returnする必要がある。

```java
    if (d.isZero()) {
      return Float.intBitsToFloat(sign << 31);
    }
```

続いて、オーバーフローの場合。単精度浮動小数点数では[tex:{2^{127}}]を超える数を表すことはできない。したがって `+Infinity` もしくは `-Infinity` を返す必要がある。

```java
    if (exponent > 127) return d.isNegative() ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;
```

## 最近接偶数丸め
さらに最近接偶数丸めも対応する必要がある。まず仮数部で表せる桁の最下位の1つ下の桁が2進数表記で[tex:{1}]かどうかを調べる。これはいままでと同様に[tex:{2}]倍して[tex:{1}]以上になるか調べればよい。ここまでで対象の数が単精度浮動小数点数で表せる数のちょうど中間か中間よりも少しだけ0より遠い側にあることが分かる。

```java
    // 仮数部はすべて詰め終わっており、dには0.xxxxという形でそれより下の小数部が格納されている
    // 最近接偶数丸め
    if (!d.isZero()) {
      d.multiplyByTwo();
      if (!d.isLessThanOne()) { // dは1.xxxxという形
        d.discardNumberPart();
        if (d.isZero()) {
          // ちょうど中間なので偶数になるように丸める
          if ((mantissa & 1) == 1) {
            mantissa++;
          }
        } else {
          // 0より遠い側なので最近接にするために仮数部を1増やす
          mantissa++;
        }
      }
    }
```

以上で最近接偶数丸めができた。めでたしめでたし、となりたいところだが、そうは問屋が卸さない。実は仮数部を1増やしたことによって仮数部がオーバーフローする可能性があるのだ。すなわち仮数部23ビットすべてに`1`が立っている状態で[tex:{1}]を足せばとうぜん仮数部は足りなくなる。この場合は指数部を[tex:{1}]増やして仮数部は[tex:{0}]にクリアする必要がある。さらに指数部を[tex:{1}]増やしたことによって全体がオーバーフローしないかチェックする必要もある。

```java
   if (mantissa == 0x800000) {
     // 仮数部が23ビットを超えている
     mantissa = 0;
     exponent++;
     if (exponent > 127) {
       return d.isNegative() ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;
     }
   }
```

さて、ここまでで正規化数はすべてパースできるはずだ。最後に非正規化数をサポートする。

## 非正規化数
非正規化数は対象の値が[tex:{2^{-126}}]より小さい場合に必要になる。まず2進数表記で[tex:{0.xx\ldots x \times 2^{-126}}]という形にして、そのうえで指数部は[tex:{0}]にクリアしておく。

```java
    if (exponent >= -126) {
      // 正規化数
      // バイアスを足す
      exponent += 127;
    } else {
      // 非正規化数
      while (exponent < -126) {
        d.divideByTwo();
        exponent++;
      }
      exponent = 0;
    }
```

# 完成版
以上をすべて行った文字列をパースして単精度浮動小数点数を返すコードが以下である。今後バグが見つかった場合は[GitHubレポジトリ](https://github.com/hkurokawa/FloatingPointNumberParser)を更新していくので、最新版のコードが見たい方はそちらを参照いただきたい。

```java
  public float parseFloat(String s) {
    BigNumber d = BigNumber.parse(s); // 10進数文字列表記を無限精度の10進数に保存する
    int sign = d.isNegative() ? 1 : 0;
    if (d.isZero()) {
      return Float.intBitsToFloat(sign << 31);
    }
    int mantissa = 0;
    int exponent = 0;
    while (d.isEqualToOrGreaterThanTwo()) { // 2以上なら2未満になるまで1/2を掛ける
      d.divideByTwo();
      exponent++;
    }
    while (d.isLessThanOne()) { // 1未満なら1以上になるまで2を掛ける
      d.multiplyByTwo();
      exponent--;
    }
    if (exponent > 127) return d.isNegative() ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;
    if (exponent >= -126) {
      // 正規化数
      // バイアスを足す
      exponent += 127;
    } else {
      // 非正規化数
      while (exponent < -126) {
        d.divideByTwo();
        exponent++;
      }
      exponent = 0;
    }
    d.discardNumberPart();
    for (int i = 22; i >= 0; i--) {
      d.multiplyByTwo();
      if (!d.isLessThanOne()) {
        mantissa |= 1 << i;
        d.discardNumberPart();
      }
    }
    // 仮数部はすべて詰め終わっており、dには0.xxxxという形でそれより下の小数部が格納されている
    // 最近接偶数丸め
    if (!d.isZero()) {
      d.multiplyByTwo();
      if (!d.isLessThanOne()) { // dは1.xxxxという形
        d.discardNumberPart();
        if (d.isZero()) {
          // ちょうど中間なので偶数になるように丸める
          if ((mantissa & 1) == 1) {
            mantissa++;
          }
        } else {
          // 0より遠い側なので最近接にするために仮数部を1増やす
          mantissa++;
        }
        if (mantissa == 0x800000) {
          // 仮数部が23ビットを超えている
          mantissa = 0;
          exponent++;
          if (exponent > 127) {
            return d.isNegative() ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;
          }
        }
      }
    }

    int bits = mantissa | (exponent << 23) | (sign << 31);
    return Float.intBitsToFloat(bits);
  }
```

# 今後の課題
冒頭にも書いたが、この実装はパフォーマンスを無視している。たとえば `BigDecimal` の演算はその桁数を [tex:{n}] とすれば毎回 [tex:{O(n)}] かかってしまう。したがって[tex:{2}]で割って[tex:{2}]未満にする操作は [tex:{O(n \log n)}] かかるので "1e+400000" みたいな文字列を与えられると処理が現実的な時間で終わらない。

また、これは手抜きなのだが E 表記や p 表記の指数部は `Integer.parseInt(String)` を呼んでいるだけなのでそこでオーバーフローする。

今回は[Goのテストケース](https://golang.org/src/strconv/atof_test.go)を[そのままコピー](https://github.com/hkurokawa/FloatingPointNumberParser/blob/main/src/test/java/ParserTest.java)したが、これらの指数部が大きすぎるテストケースはコメントアウトせざるを得なかった。

ひとまず考えられる改善としては指数部のパース時にfloatもしくはdoubleで表せる範囲を超えていることが確実になった時点でパースを打ち切ることだろう。そのうえで無限精度の10進数型の演算についてはいくつかアルゴリズムがあるようなので、それを試してみようと思う[^4]。

[^4]: 有名なのは Will Clinger, "How to Read Floating Point Numbers Accurately", ACM SIGPLAN '90, pp.92--101, 1990. らしい。

# 感想
というわけで、自分で浮動小数点数のパーサを書いてみた。ハマりポイントは最近接偶数丸めで仮数部が桁あふれをするところで、これはGoのテストケースを実行するまで気付かなかったので、自分で実装してよかったと思うポイントだ。

あと、自分でテストケースを書こうとすると、たとえば無限精度で [tex:{2^{-53}}] の値が欲しくなることがある。適当な電卓では表示が打ち切られてしまうので [https://keisan.casio.com/calculator](https://keisan.casio.com/calculator) のような有効桁数を指定できる電卓を使う必要があった。

最後に、面識はありませんが、きっかけとなった記事を書いてくれたSukesan1984に感謝します。この記事が読者のなにかしら参考になれば幸いです。
