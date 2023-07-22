---
layout: post
title:  "Kotlin Inline Functions"
date:   2015-12-07 15:01:17.000000000 -0800
categories: 
---
これは[Kotlin Advent Calendar 2015](http://www.adventar.org/calendars/857)の第8日目の記事です。昨日は n_yunoue さんによる[ことりん、ビッグウェーブに乗る](http://labs.septeni.co.jp/entry/2015/12/07/120530)でした。

今日は inline functions の話を書きたいと思います。本題に入る前に function literal と function expression の違いについて説明して、その後に `inline` modifier やそれにまつわる Kotlin の機能（`reified` modifier や `crossline` modifier）について説明します。

<!-- more -->

# Difference between Function Literal & Function Expression
Kotlin を書き始めた最初は、function の様々な書き方に戸惑わなかったでしょうか。以下はすべて正しい書き方です。

```kotlin
// Return the longest element
list.maxBy { it.length }
list.maxBy { a -> a.length }
list.maxBy { a: String -> a.length }
list.maxBy (fun(a) = a.length )
list.maxBy (fun(a): Int = a.length )
list.maxBy (fun(a): Int { return a.length })
list.maxBy (fun(a: String): Int { return a.length })
```

さらに言えば、関数を宣言する方法もいくつかあるのですが、ここでは省略しておきましょう。

さて、標題の function literal と function expression の違いです。上記のどれが function literal で、どれが function expression でしょうか？そうです、ブレース（`{}`）に囲まれた関数（最初の3つ）はすべて function literal で、残りの `fun` キーワードが使われている例は function expression です。簡単ですね。

では、この両者の違いは何でしょうか？実は、両者の違いは return 文の解釈のみに現れるようです[^1]。

[^1]: 少なくとも Kotlin のコンパイラを grep した限りは [https://github.com/JetBrains/kotlin/blob/4b8017e34b0842d717df82bbeb1a7f465e8a46fc/compiler/backend/src/org/jetbrains/kotlin/codegen/ExpressionCodegen.java#L1843-L1843](https://github.com/JetBrains/kotlin/blob/4b8017e34b0842d717df82bbeb1a7f465e8a46fc/compiler/backend/src/org/jetbrains/kotlin/codegen/ExpressionCodegen.java#L1843-L1843) くらいしか大きな影響はないようでした。他にも違いがあることをご存知の方はご指摘いただけたら幸いです。

例えば、以下のように function literal の中では素の return 文は禁止されており、コンパイルエラーになります。これは、「素の return 文は直近の名前付き関数（named function）か function expression を脱出する」と定められており、lambda からの脱出には使えないからです。`return@ordinaryFunction` のようなラベルを付ければ、return 文の使用は可能です。

```kotlin
fun foo() {
  ordinaryFunction {
     return // ERROR: can not make `foo` return here
  }
}

fun foo() {
  ordinaryFunction {
     return@ordinaryFunction // OK: exit the lambda
  }
}
```

一方、function expression や名前付き関数の場合は、このような素の return 文は問題なく使えます。

```kotlin
fun foo() {
  ordinaryFunction (fun() {
     return // OK: exit the function
  )
}

fun foo() {
  ordinaryFunction (fun() {
     return@ordinaryFunction // OK: exit the function
  )
}
```

この素の return 文の解釈の差については後で触れるので覚えておいてください。

# Inline Modifier
本題の `inline` modifier です。この modifier はご存知の通り、引数で渡された関数をそのままインライン展開することを示します。コードで表すと、以下のようになります。`inline` modifier が付くと、渡された引数 f を関数オブジェクトとして扱うのではなく、そのまま中身を展開します。

```kotlin
    inline fun inlineFunction(f: () -> Unit) {
        f()
    }
```

実際にバイトコードでどうなるか確認してみましょう。IntelliJ では `Kotlin Bytecode` というコマンドを実行すると、Kotlin がどんなバイトコードになるのか簡単に確認できます。特に、Kotlin コード側でポインタを移動すると対応するバイトコードの箇所がハイライトされるのが便利です。

まず、`inline` modifier が付かない場合。このような Kotlin コードが、

```kotlin
ordinalFunction(fun() {
    println("calling fun")
    return
})
```

こうなります。

```
ALOAD 0
GETSTATIC LambdaTest$testLambda02$1.INSTANCE : LLambdaTest$testLambda02$1;
CHECKCAST kotlin/jvm/functions/Function0
INVOKEVIRTUAL LambdaTest.ordinalFunction (Lkotlin/jvm/functions/Function0;)V
```

ここで `LambdaTest$testLambda02$1.INSTANCE` というのが無名関数オブジェクトになり、下の方で以下のように定義されます。

```
// ================LambdaTest$testLambda02$1.class =================
// class version 50.0 (50)
// access flags 0x30
// signature Lkotlin/jvm/internal/Lambda;Lkotlin/jvm/functions/Function0<Lkotlin/Unit;>;
// declaration: LambdaTest$testLambda02$1 extends kotlin.jvm.internal.Lambda implements kotlin.jvm.functions.Function0<kotlin.Unit>
final class LambdaTest$testLambda02$1 extends kotlin/jvm/internal/Lambda  implements kotlin/jvm/functions/Function0  {
  ...
  // access flags 0x11
  public final invoke()V
   L0
    LINENUMBER 36 L0
    LDC "calling fun"
    INVOKESTATIC kotlin/io/ConsoleKt.println (Ljava/lang/Object;)V
   L1
    LINENUMBER 37 L1
    RETURN
   L2
    LOCALVARIABLE this LLambdaTest$testLambda02$1; L0 L2 0
    MAXSTACK = 1
    MAXLOCALS = 1
 ...
}
```

一方、`inline` modifier が付くと、以下のような Kotlin コードが、

```kotlin
inlineFunction(fun() {
    println("calling fun in inline function")
    return
})
```

こうなります。

```
 LINENUMBER 45 L2
 ALOAD 0
 ASTORE 1
 NOP
L3
 LINENUMBER 9 L3
L4
 LINENUMBER 46 L4
 LDC "calling fun in inline function"
 INVOKESTATIC kotlin/io/ConsoleKt.println (Ljava/lang/Object;)V
L5
 LINENUMBER 47 L5
L6
 GETSTATIC kotlin/Unit.INSTANCE : Lkotlin/Unit;
 POP
L7
 LINENUMBER 10 L7
L8
L9
```

関数呼び出しが消えて、直接 function expression の中身が実行されているのが分かります。同様の function literal を渡しても同じバイトコードになるのが分かるでしょう。

## non-local return
ここで、ようやく、さきほどの return 文の話に戻ります。`inline` modifier がないとき、以下の Kotlin コードはコンパイルエラーになりました。

```kotlin
fun foo() {
  ordinaryFunction {
     return // ERROR: can not make `foo` return here
  }
}
```

一方、`inline` modifier が付いた関数の内部では、このような素の return 文が許されます。ただし、function expression 内の return 文と異なり、外側の `foo()` メソッドから脱出してしまいます。

```kotlin
fun foo() {
    inlineFunction {
        println("calling lambda in inline function")
        return // OK: exit foo()
    }

    println("never reach here")
}
```

これが non-local return と呼ばれるものです。少々乱暴に見えるかもしれませんが、たとえば `forEach` のようなメソッド内で利用すると、通常の for 文と同じような処理ができて便利です。

```kotlin
fun hasZeros(ints: List<Int>): Boolean {
  ints.forEach {
    if (it == 0) return true // returns from hasZeros
  }
  return false
}
```

なお、この non-local return もバイトコードを見てみると、以下のようになります。`println("calling lambda in inline function")` の後に `RETURN` が呼ばれているのが分かります。function expression を引数に渡したときは、ただの `POP` だったところです。

```
    LINENUMBER 44 L13
    ALOAD 0
    ASTORE 1
    NOP
   L14
    LINENUMBER 9 L14
   L15
    LINENUMBER 45 L15
    LDC "calling lambda in inline function"
    INVOKESTATIC kotlin/io/ConsoleKt.println (Ljava/lang/Object;)V
   L16
    LINENUMBER 46 L16
    RETURN
```

## noinline modifier
ここからは、すこし細かい `inline` modifier に関連するいくつかの文法の話です。さきほどの inline function は引数の lambda をすべてインライン展開しましたが、してほしくない場合もあります。そのような場合は `noinline` modifier を引数の前に指定してやります。

```kotlin
inline fun foo(f: () -> Unit, noinline g: () -> Unit) {
    f()
    g()
}
```

実際に、以下のように上記の関数を呼び出すと、

```kotlin
foo(fun() {
    println("calling 1st fun in inline function")
    return
}, fun() {
    println("calling 2nd fun in inline function")
    return
})
```

バイトコードが以下のようになります。2つ目の lambda が関数オブジェクトとして扱われているのが分かります。

```
    LINENUMBER 40 L2
    ALOAD 0
    ASTORE 1
    GETSTATIC LambdaTest$testLambda$4.INSTANCE : LLambdaTest$testLambda$4;
    CHECKCAST kotlin/jvm/functions/Function0
    ASTORE 2
    NOP
   L3
    LINENUMBER 13 L3
   L4
    LINENUMBER 41 L4
    LDC "calling 1st fun in inline function"
    INVOKESTATIC kotlin/io/ConsoleKt.println (Ljava/lang/Object;)V
   L5
    LINENUMBER 42 L5
   L6
    GETSTATIC kotlin/Unit.INSTANCE : Lkotlin/Unit;
    POP
   L7
    LINENUMBER 14 L7
    ALOAD 2
    INVOKEINTERFACE kotlin/jvm/functions/Function0.invoke ()Ljava/lang/Object;
    POP
```

## crossline modifier
inline function 内で lambda を扱う場合、その lambda をそのまま inline function 内で実行するのではなく、他の文脈で実行する場合があります。その場合は crossline modifier を付けないとコンパイルエラーになります。

```kotlin
    inline fun bar(f: () -> Unit) {
        var g = object: Runnable {
            override fun run() {
                f() // ERROR: Can't inline 'f' here: it may contain non-local returns. Add 'crossinline' modifier to parameter declaration 'f'
            }
        }
    }
    
    inline fun bar(crossline f: () -> Unit) {
        var g = object: Runnable {
            override fun run() {
                f() // OK
            }
        }
    }
```

## reified modifier
inline function では、総称型の具体的な型が確定するため、仮型引数をあたかも実際のクラス型であるかのように扱えます。こう言っても何かよく分からないと思うので実例を見てみましょう。

```kotlin
fun <U> isInstance(t: Any): Boolean {
    return t is U // ERROR: Cannot check for instance of erased type: U
}

inline fun <reified U> isInstance(t: Any): Boolean {
    return t is U // OK
}
```

Java に慣れている人には周知だと思いますが、Java の総称型はコンパイル時に型引数が決定して型消去が行われるため、ランタイム時には型が分かりません。このため、わざわざ具体的な型を別の引数で指定するなどの苦労をしていました（List.toArray() で配列を別途渡すアレです）。しかし、inline function と、この `reified` modifier を使うと、そのような苦労から解法されます。素晴しい。現時点では inline function 限定ですが、そのうち他でも使えるようになるのでしょうか。

なお、`reified` modifier の詳細については、[kotlin/reified-type-parameters.md at master · JetBrains/kotlin](https://github.com/JetBrains/kotlin/blob/master/spec-docs/reified-type-parameters.md) に書かれています。

# まとめ
この記事では、Kotlin の `inline` modifier とそれに関連する挙動について述べました。特に、

- `non-local` return
- `crossline` modifier
- `reified` modifier

については、inline function 特有なものなので覚えておきましょう。さらに、non-local return については function literal と function expression の違いが重要でした。

Kotlin を学び始めて、まだ一ヶ月の私の感想ですが、Kotlin 的な書き方をしようとすると、どうしても lambda を多用することになります。そのため、パフォーマンスが重要な箇所では inline は必須と言って良いでしょう。

同時に、non-local return の挙動は、私には少々やり過ぎにうつります。まず、function literal と function expression の違いがそこにしかないなら、そのための文法の使い分けは現時点では過剰に思えます。かつ、return 文が lambda からの脱出に見えるので、人によっては予想外の挙動をする可能性があるでしょう。もちろん、inline function でない場合はコンパイラエラーにすることで不用意な return 文は禁止していますが、function literal 内では素の return 文は許さず、ラベルを付けることを必須にしても良かったのではないかと思います。

一方で、`reified` modifier は inline 限定とはいえ、とてもありがたい機能です。Java プログラミングでは型情報が消去されないように様々なテクニックが使われていますが、そういったものに頼らずに済むのは Java プログラマにとって朗報でしょう。

引き続き Kotlin の勉強をしていきたいと思います。

明日は[@RyotaMurohoshi](https://twitter.com/ryotamurohoshi)さんによる「Kotlin×Androidではない何か」です。

# Reference
- [Higher-Order Functions and Lambdas](https://kotlinlang.org/docs/reference/lambdas.html)
- [Inline Functions](https://kotlinlang.org/docs/reference/inline-functions.html)
- [kotlin/reified-type-parameters.md at master · JetBrains/kotlin](https://github.com/JetBrains/kotlin/blob/master/spec-docs/reified-type-parameters.md)