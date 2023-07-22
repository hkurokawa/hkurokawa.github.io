---
layout: post
title:  "companion object vs. top-level"
date:   2017-12-01 08:27:38.000000000 -0800
categories: 
---
JavaにはあるけれどKotlinにないものの1つに、クラスメソッドやクラス変数があります。
この記事では、そのクラスメソッドとクラス変数をKotlinではどう定義すべきかという話をします。

より詳細に言えば、メソッドや定数[^1]をtop-levelで宣言するのと[companion object](https://kotlinlang.org/docs/reference/object-declarations.html)に宣言するのとどちらが良いかという話です。

[^1]: なお、この記事ではpublicもしくはinternalなメソッドや定数について考えます。privateなものについてはどちらも大差ないという考えからです。

<!-- more -->

## Java -> Kotlin 自動変換はクラスメソッドとクラス変数をcompanion objectに変換する

JavaからKotlinへの自動変換を使ったことのある人は、Javaのクラスメソッドや定数がcompanion objectに変換されることに気付いたでしょう。

こういうJavaクラスがあったとして、自動変換をすると、

```java
package shape;

public class Circle {
  public static final double PI = 3.14159265358979323846;
  private final double r;

  private Circle(double r) {
    this.r = r;
  }

  public static Circle newCircle(double r) {
    return new Circle(r);
  }

  public double area() {
    return PI * r * r;
  }
}
```

つぎのようになります。

```kotlin
package shape

class Circle(private val r: Double) {
  fun area(): Double {
    return PI * r * r
  }

  companion object {
    val PI = 3.14159265358979323846

    fun newCircle(r: Double): Circle {
      return Circle(r)
    }
  }
}
```

これはKotlinコードとしては（多少不自然さはあるものの）まったく正しいコードです。
呼び出し方も `Circle.newCircle()` もしくは `Circle.PI` とすればよいのでJavaに馴れた目にも違和感がありません。

そのせいか、自分が目にするKotlinコードの多くは、Javaのクラスメソッドやクラス変数に相当するものをcompanion objectで実装する傾向があるように思います。

ただ、個人的な感覚からすると、このcompanion objectは余計な印象があります。この印象を分析すると、2つの理由があります。

1. 作らなくてもいいオブジェクトを生成している
2. このcompanion objectがどういう振る舞いを期待されているか分からない

2について補足すると、ファクトリーメソッドを持ちつつ、そのインスタンス生成に関係ない`PI`という変数を持っているのがやや気持ち悪く感じます。
そもそも`PI`という定数（ここでは変数になっていますが）がなにかのクラスに関連付いているというのも微妙な気分になります。

## top-level関数およびプロパティ
一方、Kotlinではtop-levelで関数およびプロパティを宣言することができます。
これは特定のクラスに属さない関数や定数を定義するのに自然な選択です。

さきほどの例だと、こうなるでしょう。

```kotlin
package shape

val PI = 3.14159265358979323846

fun newCircle(r: Double): Circle {
  return Circle(r)
}

class Circle(private val r: Double) {
  fun area() = PI * r * r
}
```

いかがでしょうか？クラス設計という点からは、`Circle`クラスのみになり、理解しやすい気がします。ただ後述するように使う側からすると使いにくさが出てくる面もあります。
なお、この例では`Circle`クラスのコンストラクタがpublicですが、こういった`newCircle`メソッドのようなファクトリーメソッドがある場合は、コンストラクタをprivateにするケースが大半です（さもないとせっかくのファクトリーメソッドを迂回されてしまいます）。

その場合、このtop-level版のコードはコンパイルできません。なぜなら`newCircle`は`Circle`クラスのprivateコンストラクタにアクセスできないからです。

しかし、ここでは、どちらがいいか、どちらがKotlinらしいか、という議論はやめておきましょう。

かわりに、両者を比較する上での材料を集めてみます。

## バイトコードを見てみる
まずは、両者の書き方によって、バイトコードに差が出るか見てみましょう。

まずはcompanion object版です（関係のなさそうなところは端折っています）。

```java
public final class shape/Circle {
  // access flags 0x1A
  private final static D PI = 3.141592653589793

  // access flags 0x19
  public final static Lshape/Circle$Companion; Companion
}

public final class shape/Circle$Companion {
  // access flags 0x11
  public final getPI()D
   L0
    LINENUMBER 9 L0
    INVOKESTATIC shape/Circle.access$getPI$cp ()D
    DRETURN
   L1
    LOCALVARIABLE this Lshape/Circle$Companion; L0 L1 0
    MAXSTACK = 2
    MAXLOCALS = 1

  // access flags 0x11
  public final newCircle(D)Lshape/Circle;
  @Lorg/jetbrains/annotations/NotNull;() // invisible
   L0
    LINENUMBER 12 L0
    NEW shape/Circle
    DUP
    DLOAD 1
    INVOKESPECIAL shape/Circle.<init> (D)V
    ARETURN
   L1
    LOCALVARIABLE this Lshape/Circle$Companion; L0 L1 0
    LOCALVARIABLE r D L0 L1 1
    MAXSTACK = 4
    MAXLOCALS = 3
}
```

`Circle`クラスが`Circle$Companion`型のクラス変数`Companion`を持ち、`newCircle()`も`PI`もその変数経由でしかアクセスできなくなっています。
`Companion`という名前がクラス名と変数名両方に使われていてちょっと紛らわしいですね。

また、`PI`が`Circle`クラスのprivate static変数になっているのはちょっと面白いところです。
`Circle`クラス内からアクセスするときのオーバーヘッドを減らすためでしょうか（なお`area()`メソッド内で`PI`を使っているところは数値リテラルに置き換えられていました）。

とはいえ、おおむね予想通りだったのではないかと思います。さらに[これからJavaで書かれたAndroidアプリケーションのソースコードをKotlinに書き換える際に気をつける、やるべきこと2点 - へいへいブログ](http://shaunkawano.hatenablog.com/entry/2017/11/07/101611)にもあるように、
`val PI`の前に`const`を付けると、`PI`は`Circle`クラスのpublic static変数になりメソッド呼び出しを1つ減らせます。

ではtop-level版はどうなるでしょうか。

```java
public final class shape/Circle {
  ...
}

public final class shape/CircleKt {
  // access flags 0x1A
  private final static D PI = 3.141592653589793

  // access flags 0x19
  public final static getPI()D
   L0
    LINENUMBER 3 L0
    GETSTATIC shape/CircleKt.PI : D
    DRETURN
   L1
    MAXSTACK = 2
    MAXLOCALS = 0

  // access flags 0x19
  public final static newCircle(D)Lshape/Circle;
  @Lorg/jetbrains/annotations/NotNull;() // invisible
   L0
    LINENUMBER 6 L0
    NEW shape/Circle
    DUP
    DLOAD 0
    INVOKESPECIAL shape/Circle.<init> (D)V
    ARETURN
   L1
    LOCALVARIABLE r D L0 L1 0
    MAXSTACK = 4
    MAXLOCALS = 2
}
```

`CompanionObject`がなくなったかわりに`CircleKt`クラスが登場し、そのクラス変数およびクラスメソッドになっています。
こちらも[公式ドキュメント](https://kotlinlang.org/docs/reference/java-to-kotlin-interop.html)にある通りなのでびっくりはないと思います。
バイトコード的には、companion object版にあった`Companion`変数がなくなったので、すこしだけオーバーヘッドはなくなったかもしれません（クラスロード時にインスタンス化されるだけなので微々たるものだと思いますが）。

なお、さきほどと同様に`val PI`の前に`const`を付けると、`CircleKt`の`getPI()`メソッドはなくなり`PI`はpublic static変数になります。

結論として、両者をバイトコードから比較したときに、どちらが良い、とは言えなさそうです。わずかにtop-level版の方がオーバーヘッドは小さい程度でしょうか。

## 使うときのメリット・デメリット
では使う側からはどういう違いがあるでしょうか。
それぞれのバージョンを呼び出すコードはつぎのようになります。

まずcompanion object版はこうなります。

```kotlin
import shape.Circle

fun main(args: Array<String>) {
  val c = Circle.newCircle(1.0)
  println("PI = $Circle.PI")
  println(c.area())
}
```

つぎにtop-level版です。

```kotlin
import shape.PI
import shape.newCircle

fun main(args: Array<String>) {
  val c = newCircle(1.0)
  println("PI = $PI")
  println(c.area())
}
```

これは好みが分かれそうです。おそらく、Javaに馴れた目から見るとcompanion object版の方が自然でしょう。
とくに`Circle`というクラス名で修飾するのに比べてpackageでしか名前空間を定義できないのは心許なく感じるかもしれません。

ここで、自分の好みを言うと、自分は実はtop-level版でも大して気になりません。
ただ、AndroidのFragment生成メソッドのように`newFragment`といった同名のメソッドが大量にあると、それらに別名を付けるのは面倒そうです。
しかし、`PI`のような定数ならば名前が被ることも少ないだろうしパッケージ修飾で十分かなと思います。

使う側の観点からは好み次第といったところでしょうか。

## 教科書やブログを調べてみる
それでは、ここで教科書やブログでは、両者についてどう比較しているか見てみましょう。

まずは[Kotlin in Action]()です。つい最近邦訳が出ましたが、残念ながら英語版しかないので、そこからの引用です。ちょっと長いですが、まさにドンピシャな部分があったので全パラグラフを載せます。後ろに拙訳を載せました（繰り返しになりますが日本語版を持っていないのです :bow:）

> Classes in Kotlin can’t have static members; Java’s static keyword isn’t part of the Kotlin language. As a replacement, Kotlin relies on package-level functions (which can replace Java’s static methods in many situations) and object declarations (which replace Java static methods in other cases, as well as static fields).
> In most cases, it’s recommended that you use top-level functions. But top-level functions can’t access private members of a class, as illustrated by figure 4.5 . Thus, if you need to write a function that can be called without having a class instance but needs access to the internals of a class, you can write it as a member of an object declaration inside that class. An example of such a function would be a factory method.

> Kotlinのクラスはスタティックメンバーを持てません。JavaのstaticキーワードはKotlinの言語仕様にないのです。代わりに、Kotlinはpackageレベルの関数（Javaの一般的なスタティックメソッドの代替）とobject宣言（Javaの特殊なスタティックメソッドとスタティックフィールドの代替）があります。
> 一般に、top-levelの関数を使うことをおすすめします。しかしtop-level関数は図4.5に示すようにクラスのプライベートメンバーにアクセスできません。そのため、クラスインスタンスは持つ必要はないけれど、クラス内部にアクセスしたい場合には、そのクラスのobjectを宣言し、関数をそのobject内に宣言します。そのような関数の典型例はファクトリーメソッドでしょう。

*4.4.2. Companion objects: a place for factory methods and static members, Kotlin in Actions*

いかがでしょうか？関数については明示的にtop-levelにすべきとありますね。一方でスタティック変数については明言はされていませんがobjectに定義することを想定しているようです。また、objectにメソッドを定義するのはクラス内部にアクセスしたい場合すなわちファクトリーメソッドの場合であると書いてあります。

ここからは推測にすぎませんが、どうもKotlinの作者は、companion objectはファクトリーメソッドのためにあると考える節があります。
Scalaから借りてきた概念というのもあると思うのですが、class objectやdefault objectのように名付けに苦労しつつも通常のobjectではない特別なobjectを作ろうとしていたのは、ファクトリーメソッドが`クラス名.ファクトリーメソッド()`という形式で呼べるようにしたかったのではないかと思います[^2]

[^2]: 完全に推測なので違うようだったらぜひご指摘ください

巷間のブログではどうでしょうか？1つ参考になるブログ記事に[Where Should I Keep My Constants in Kotlin?](https://blog.egorand.me/where-do-i-put-my-constants-in-kotlin/)という記事があります。
これは余計なオブジェクトを生成しないという観点からobjectに定数を定義する方法とtop-levelに定義する方法を比較し、後者の方がよいとしています。
一方、コメントを見るとobjectに名前を付けることで定数のグルーピングが出来て便利という意見もあるようです。

## 標準ライブラリをgrepしてみる
最後にやや飛び道具的ですが、標準ライブラリを見てみます。
標準ライブラリに従わなければならないという法はありませんが、それらがcompanion objectをどう使っているか調べれば、きっとヒントが得られるでしょう。

ここでは [https://github.com/JetBrains/kotlin](https://github.com/JetBrains/kotlin) の現時点での最新のコード[^3]でつぎのコマンドを実行してみます。

```
$ find ./libraries/stdlib -name '*.kt' | xargs grep -l 'companion object'
```

結果は15件と意外と少ないことに気付きます。テストを除いた `.kt` ファイルは131ファイルあるので、それと比べても少なめなことが分かります。

一方、top-level関数が定義されているファイルは適当に `egrep -l '^public inline fun'` とやっても52件見つかります。ただし、これは標準ライブラリという性質および拡張関数も含まれることを考えると公平な比較ではありません。参考程度にしておきます。

さて、ではcompanion objectは実際にどういう箇所で使っているのでしょうか。

たとえば、このへんなどは参考になりそうです。一種のファクトリーメソッドですが、キャッシュ用のプロパティを持っています。

```kotlin
public enum class CharCategory(public val value: Int, public val code: String) {
   ...
   public companion object {
        private val categoryMap by lazy { CharCategory.values().associateBy { it.value } }

        public fun valueOf(category: Int): CharCategory = categoryMap[category] ?: throw IllegalArgumentException("Category #$category is not defined.")
    }
}
```

*https://github.com/JetBrains/kotlin/blob/0b37c9e83cd09008db5908fd47583cd62e9fc17b/libraries/stdlib/src/kotlin/text/CharCategory.kt#L164*

あるいは、ちょっと変わったものだとこのあたりでしょうか。

```kotlin
expect class Regex {
    ...
    
    companion object {
        fun fromLiteral(literal: String): Regex
        fun escape(literal: String): String
        fun escapeReplacement(literal: String): String
    }
}
```

*https://github.com/JetBrains/kotlin/blob/a39f2f82718dd278eba9a82df4a5632abb1f4044/libraries/stdlib/common/src/kotlin/TextH.kt#L61*

```kotlin
public interface ContinuationInterceptor : CoroutineContext.Element {
    companion object Key : CoroutineContext.Key<ContinuationInterceptor>
    ...
}
```

https://github.com/JetBrains/kotlin/blob/a39f2f82718dd278eba9a82df4a5632abb1f4044/libraries/stdlib/src/kotlin/coroutines/experimental/ContinuationInterceptor.kt#L29

前者は、`Regex`クラスのインタフェース（expect class）を定義しているものですが、`companion object`にファクトリーメソッドだけでなく`escape`メソッドも定義しています。
これは、実装をこの場で提供できないがためにこうなっているのかもしれません。

後者は、coroutineの実装です。
詳細は述べませんが、`CoroutineContext.Element`を継承しているインタフェースはどれもcompanion objectが`CoroutineContext.Key`を継承しており、
クラス名をキーにしてContextからそのクラスのシングルトンを取得できるようになっています。

一方で、publicな定数はあまりcompanion objectに定義されていないようでした。せいぜい[KotlinVersionVersion.kt](https://github.com/JetBrains/kotlin/blob/0b37c9e83cd09008db5908fd47583cd62e9fc17b/libraries/stdlib/src/kotlin/util/KotlinVersion.kt#L66)に定義されていた`MAX_COMPONENT_VALUE`くらいでしょうか。これはその直後の変数`CURRENT`のために必要だったようですが。

むしろ、`const val`でgrepしてみるとtop-levelに多くの定数が定義されています。あるいは、[Typography](https://github.com/JetBrains/kotlin/blob/0b37c9e83cd09008db5908fd47583cd62e9fc17b/libraries/stdlib/src/kotlin/text/Typography.kt)のように名前のあるobject内に定数を定義してグルーピングしているものはありました。

[^3]: [https://github.com/JetBrains/kotlin/commit/a39f2f82718dd278eba9a82df4a5632abb1f4044](https://github.com/JetBrains/kotlin/commit/a39f2f82718dd278eba9a82df4a5632abb1f4044)

# まとめ
以上の調査結果をもとにpublic/internalなクラスメソッドや定数をどう定義するのがよいか考えてみましょう。

まず、バイトコード的には大差ありませんでした。しかし、使う側からすると、companion object版の方が名前の衝突に気を遣わなくてよい分、有利そうです。このとき定数には`const`を付けるのが重要でした。

一方、Kotlin in Actionや巷のブログ記事を読むと、メソッドについてはファクトリーメソッドはcompanion object、それ以外はtop-levelというのが主流のようです。定数についてはtop-levelがやや有利、グルーピングしたいならobjectに定義、という感じでしょうか。

最後に標準ライブラリの実装を調べてみたところ、そもそもcompanion objectを使っているファイルが少なかったものの、companion objectに定義されたpublic/internalなメソッドと定数は、top-levelのそれに比較してあまり見られない印象でした。これは標準ライブラリという性格も影響しているかもしれませんが興味深い結果です。

結論としては、メソッドはファクトリーメソッド以外はtop-level、publicな定数はグルーピングが必要ならobject、そうでないならtop-levelが良さそうです。

なにかこれ以外の観点や見落としがあったら、ぜひご指摘ください。
