---
layout: post
title:  "RxJava 1.x → 2.x 移行ガイド"
date:  	2017-04-27 00:00:00.000000000 -0700
categories: 
---

以下は元々Fablic在籍時の2017-04-27に会社の技術ブログinFablicに投稿した記事（[http://in.fablic.co.jp/entry/2017/04/27/110000](http://in.fablic.co.jp/entry/2017/04/27/110000)）でした。inFablicが閉鎖されてしまったため、会社の了承を取った上で転載しております。

元記事にリンクを貼っていただいていた方に対しては大変お手数なのですが、こちらにリンクし直していただければ幸いです。

-----

![OGP image](/assets/images/rxjava_migration_1_2.jpg)

こんにちは。Androidエンジニアの黒川（[@hydrakecat](https://twitter.com/hydrakecat)）です。

この記事では、RxJava 1.xから 2.xへのマイグレーションについて説明します。

私が開発に携わっている[フリル](https://fril.jp/)というフリマサービスのAndroidアプリでは、つい先日のアップデートでRxJava 2.0.8への移行を済ませました。
幸い、いまのところ問題は起きていませんが、マイグレーションにあたっては、当初予想していたよりも多くの作業が発生しました。この記事では、その知見を共有したいと思います。

<!-- more -->

# RxJava 2.xについて
[RxJava](https://github.com/ReactiveX/RxJava)は Reactive Extenstion（Rx）のJava実装で、非同期処理用のライブラリです。

RxJava 1.0 がリリースされたのは、[2014年の11月](https://github.com/ReactiveX/RxJava/releases/tag/v1.0.0)ですが、それ以来、多くの開発者に使われてきました。そして、とうとう2017年1月に、バージョン1.0のEnd of Life（EOL）が[発表されました](https://github.com/ReactiveX/RxJava#version-1x-javadoc)。RxJava 1.xの開発はバグ修正も含めて2018年3月31日に終了するとのことです。

代わって、いまメインになっているのがRxJava 2.xです。RxJava 2.xがどういうものか、というのは、手前味噌ながら[私のDroidKaigiでの講演資料](https://speakerdeck.com/hkurokawa/whats-new-in-rxjava-2)（この記事の末尾に埋め込んであります）を見ていただくとして、マイグレーションの際に気を付けなければならない大きな変更はつぎの三つになります。

1. パッケージ名・クラス名の変更
2. `null` が非許容になった
3. エラーハンドリングの変更

これらの大きな変更がどういうものか、どのように移行を行っていったら良いか、順に述べたいと思います。

# 移行の前に
もしあなたが、これからまさに移行を始めようとしているなら、その前に [What's different in 2.0 · ReactiveX/RxJava Wiki](https://github.com/ReactiveX/RxJava/wiki/What's-different-in-2.0) を読むことをおすすめします。長いですし、英語ですが、移行に必要な情報はすべてまとまっています。

この記事を読み終えた後でも良いので目を通しておきましょう。

# build.gradleの変更
さて、まずは `build.gradle` につぎの行を足しましょう。バージョン名は最新ものを指定してください。

```groovy
dependencies {
    compile 'io.reactivex.rxjava2:rxjava:2.0.9'
}
```

また、RxAndroid、Retrofit[^1]、RxBidningといったRx系のライブラリを使っている場合は一緒にバージョンも上げてしまいましょう。RxAndroidとRxBindingは2.0.0以降、Retrofitは`adapter-rxjava2`というモジュールを入れることでRxJava 2.xが使えるようになります。もしお使いのライブラリがまだ2.x対応していない場合は後述する1.xと2.xの共存の道をとることになります。とりあえずは、そのままにしておいて大丈夫です。

ここで、わざとコンパイルエラーを起こすために、バージョン 1.xのRxJavaを消してしまう手もあります。ただ、RxJava 1.xに依存するライブラリを使っている場合は自動的に1.xのRxJavaが入ってきます。その場合はbuild.gradleから削除することは可能ですが、1.xのRxJavaを使ってもコンパイルエラーにならないことに気をつけてください。

[^1]: 正確にはRetrofitのretrofit-adaptersでadapter-rxjavaを使っている場合です。https://github.com/square/retrofit/tree/master/retrofit-adapters/rxjava

# パッケージ名・クラス名の変更
RxJava 1.xを消していないのなら、この時点では、まだコンパイルエラーは起きていないはずです。ここからRxJava 2.xへの移行が始まります。

まずRxJava 2.xでは、パッケージ名が `rx.` から `io.reactivex.` になりました。たとえば次のようになっています。

```diff
--- MainActivity.java
+++ MainActivity.java
@@ -3,4 +3,4 @@
 +import io.reactivex.Observable;
 +import io.reactivex.disposables.Disposable;
 +import io.reactivex.disposables.Disposables;
 +import io.reactivex.schedulers.Schedulers;
 -import rx.Observable;		
 -import rx.Subscription;		
 -import rx.schedulers.Schedulers;		
 -import rx.subscriptions.Subscriptions;
```

IDEの置換機能でも良いですし、sedなどでも良いので、一括で `import rx.Observable;` は `import io.reactivex.Observable;` に、といった具合に書き換えていきます。

注意すべき点としては、 `rx.Subscription` が `io.reactivex.disposables.Diposable` に変わったことです。クラス名もさることながら、`Subscription` と `Subscriptions` が `rx` と `rx.subscriptions` パッケージに分かれていたのに対して、 `Disposable` と `Disposables` どちらも `io.reactivex.disposables` パッケージになっています。

可能ならば `unsubscribe()` メソッドも `dispose()` メソッドに書き換えましょう。つぎのステップ以降のコンパイルエラーが減らせます。

また、変数名も合わせて変えるなら `s`、`subscription`、`subscriptions`といった変数名を `d`、`disposable`、`disposables` に書き換えなければなりません。
このあたりになると単純な一括置換は意図しない変更になある可能性があるので注意しましょう。

ここまで行うと、コンパイルエラーが山のように出るはずです。つぎの項以降では典型的なコンパイルエラーと直し方を説明します。

# オペレータの変更に伴う移行作業
RxJava 2.xではオペレータ名やその戻り値が変わったものがいくつかあります。
https://github.com/ReactiveX/RxJava/wiki/What's-different-in-2.0#operator-differences にすべてが載っていますが、代表的なものとして、つぎのようなものがあります。

## toBlocking().y()
1.xでは `toBlocking()` は `BlockingObservable` という型を返していましたが、2.xではそのクラスはなくなり、代わりに `toBlocking().y()` に相当するオペレータが追加されています。
たとえば `toBlocking().first()` は `blockingFirst()` といった具合です。
これらについては、ひとつひとつ確認しながら書き換えていきましょう。

## toCompletable()
`toCompletable()` というオペレータは `ignoreElements()` というオペレータになりました。

## toSingle()
`toSingle()` というオペレータはなくなりました。`singleOrError()` に書き換えましょう。

## single() / first() / last()
引数なしの `first()`、`single()`、`last()` というオペレータはなくなりました。必要に応じて `firstOrError()` などに書き換えます。
ただし、戻り値は `Single` 型になるので、別途対応が必要となります。面倒なら `toObservable()` を足しましょう。
なお、他の選択肢として `singleElement()` などとすることもできます。その場合はRxJava 2.xから導入された `Maybe` 型が返ります。

## toMap() / toList()
これらのオペレータの戻り値は `Observable` から `Single` に変わっています。ここでは、ひとまず `toMap().toObservable()` のように `Observable` 型に変換しておきましょう。
きちんとやるなら、 `Single` 型にして、戻り値を受けとる箇所もすべて書き換える必要があります。

# その他のコンパイルエラーを起こす変更
## subscribe(Subscriber) がなくなった
つぎのように `subscribe()` メソッドの引数がラムダ式であれば、特になにもしなくて良いですが、 `Subscriber` 型だった場合は対応が必要です。

```java
// ラムダ式を使っている場合はコンパイルが通る
Disposable d = observable.subscribe(i -> {
  /* なんらかの処理 */
}, throwable -> {
  /* エラー処理 */
}, () -> {
});

// そうでない場合はコンパイルエラーになる
Disposable d = observable.subscribe(new Subscriber<Integer>() {
  @Override public void onNext(Integer integer) {
    /* なんらかの処理 */
  }

  @Override public void onError(Throwable e) {
    /* エラー処理 */
  }

  @Override public void onCompleted() {
  }
});
```

特別な要求がなければ `Subscriber` は、 `DisposableObserver` にして、 `subscribeWith(Disposable)` メソッドを使います。`subscribe`メソッドの戻り値は `void` 型なので `subscribeWith()` を使っていることに気をつけましょう。

```java
Disposable d = observable.subscribeWith(new DisposableObserver<Integer>() {
  @Override public void onNext(Integer integer) {
    /* なんらかの処理 */
  }

  @Override public void onError(Throwable e) {
    /* エラー処理 */
  }

  @Override public void onCompleted() {
  }
});
```

なお、`Subscriber` クラスは2.xでもありますが、 `Flowable` のためのクラスで `Observable` に対しては `Observer` を使います。

## Actions.empty()がなくなった
`subscribe` メソッドにラムダ式を指定するときに、何もしないメソッドを返す `Actions.empty()` というメソッドが便利だったのですが、残念ながらなくなりました[^2]。
もし引数なしの何もしないメソッドを指定したい場合は `() -> {}` と書きましょう。
あるいは引数がある場合は、 [RxJava2Extenstions](https://github.com/akarnokd/RxJava2Extensions) というライブラリを入れて `FunctionsEx.emptyConsumer()` を使う手もあります。

```java
observable.subscribe(FunctionsEx.emptyConsumer(), FunctionsEx.emptyConsumer(), () -> {});
```

[^2]: これは、2.xでは出来るだけ余計な依存は入れないとの方針によるそうです https://github.com/ReactiveX/RxJava/issues/4406#issuecomment-241669574

## Schedulers.immediate()がなくなった
`Schedulers.immediate()` はそもそもスケジューラの要求を満たしていなかったとのことで、なくなりました[^3]。
基本的にテストでしか使っていなかったと思いますが、ドキュメントにあるように `Schedulers.trampoline()` を代わりに使いましょう。

[^3]: https://github.com/ReactiveX/RxJava/wiki/What's-different-in-2.0#schedulers

## onCompleted()がonComplete()になった
細かいですが `onCompleted()` の `d` が取れて `onComplete()` になりました。それに伴い、`doOnCompleted()` も `doOnComplete()` になっています。ちまちまと、`d`を取る作業をします。

## fromEmitter() がなくなった
これまで、多くの記事で `Observable.create(OnSubscribe)` を使うな、と言われてきましたが、2.xでは安全に使うことができます。
つぎのように、素直に`create()`を使えば良くなりました。

```java
Observable.create((ObservableOnSubscribe<Info>) emitter -> {
  Info info = /* なんらかの処理 */;

  emitter.onNext(info);
  emitter.onComplete();
});
```

dispose時に解放したいリソースがある場合は `ObservableOnSubscribe#setDisposable(Disposable)` を呼んでセットしておきましょう。
なお `setDisposalbe(Disposable)` と `setCancellable(Cancellable)` の2種類がありますが、Exceptionをthrowするかという違いくらいで、基本的にどちらを使っても良いようです[^4]。

[^4]: https://github.com/ReactiveX/RxJava/issues/4812

# nullが非許容になったことへの対応
さて、ここまででコンパイルエラーはなくなったのではないでしょうか。まだ残っている場合は、ここまでで触れなかった変更によるものです。ドキュメントを読みながら解決してみてください。

この項では、2.xでの大きな変更の2つ目、「`null` が非許容になったこと」への対応方法を述べます。

2.xでは、[Reactive Streams](http://www.reactive-streams.org/)に準拠するために、`onNext()` で `null` が渡ってきてはいけないことになりました。
`Subject.onNext(null)` がダメなのはもちろん、作成した `Observable` が `null` を排出するのもダメですし、`map()` オペレータで `null` を返すのもダメです。

厄介なことは、コンパイル時には、この潜在的な間違いを検出できないことです。基本的には実際に動かして `null` が排出されてクラッシュしないことを確認するしかありません。
しかしながら、特定の条件下でしか `null` が返らないケースもあります。そういったケースに対して、いくつかの検出方法があります。

## Observable&lt;Void&gt;、Subject&lt;Void&gt;を探す
もし、あなたのアプリのコードに`Observable<Void>`や`Subject<Void>`という記述があるなら、ほぼ間違いなく `null` を返しているはずです。

よくあるケースは、`Subject<Void>`を使っているケースです。

```java
Subject<Void> subject = PublishSubject.create();
subject.onNext(null);
```

これに対する対応は後ほど述べますが、比較的容易に対応できます。

## @NonNullアノテーションによる警告を利用する
`Emitter.onNext(T)` などのいくつかのメソッドには、引数に `@NonNull` アノテーションが付いています。したがって、もしそのようなメソッドに `null` を渡していれば、Android Studio上で警告として検出することができます。

このワーニングだけに注目したい場合は、 **Constant conditions & exceptions** という警告に注目しましょう。

[f:id:hydrakecat:20170424190425p:plain]

必要に応じて`null`以外の何かの値を返すようにしましょう。もしかしたらSingleやCompletableに置き換えるのが近道かもしれません。

## nullチェックを探す
もし、Observableで返すインスタンスの型が簡単に絞り込めるなら、IntelliJのStructual Searchを使う手もあります。これは、 `$Inst == null` もしくは `$Inst != null` という形式の null チェックを探すものです。必要ならもっと複雑な式も書けます。ここで **Edit Variables** というボタンをクリックすると2枚目のスクリーンショットのように `$Inst` の型を指定することができます。たとえば、あなたのアプリが `com.myapp.network.model` 以下のクラスしかObservableで返さないことが分かっているなら、つぎのような検索でnullチェックの場所を洗い出すことができます。

[f:id:hydrakecat:20170425182151p:plain]
[f:id:hydrakecat:20170425182209p:plain]

このnullチェックによって、潜在的にnullを返すObservableが見つかるはずなので、あとはそのnullを返している場所を探しましょう。なお、無闇にnullチェックをしている場合は、この手は使えません。

## map()、onErrorReturn()に気をつける
さきほど述べましたように `map()` オペレータで `null` を返しているケースもよくあります。特に暗黙的に `null` を返すケースに気をつけましょう。
たとえば、つぎのコードでは `map()` オペレータ内でMap型の `hash` から値を引いていますが、キーが `hash` になければ `null` が返ってエラーになってしまいます。

```java
observable.map(hash::get)
```

また、実際に作業していると、意図的に `null` を返す箇所としては `map()` オペレータよりも `onErrorReturn()` オペレータの方が多いようでした。
これはエラーが起きたときに無視したいが、代替となる値として `null` が選ばれやすいためでしょう。
`onErrorReturn()` を検索して、 `null` を返していないか調べておくと良いと思います。

```java
observable.getItems()
  .onErrorReturn(throwable -> {
    Log.e(TAG, "アイテムの取得に失敗しました: " + e.getMessage());
    return null;
  });
```

## nullを使わないように書き換える
さて、`null` を使っている箇所が見つかったら、`null`を使わないように書き換えます。

`Subject<Void>`のように、渡す値がとくにないので `null` を使っている場合は、[公式ドキュメント](https://github.com/ReactiveX/RxJava/wiki/What's-different-in-2.0#nulls)にあるように書き換えます。
たとえばつぎのようになります。

```
enum Irrelevant { INSTANCE }

PublishSubject<Irrelevant> subject = PublishSubject.create();
subject.onNext(Irrelevant.INSTANCE);
```

また、 `onErrorReturn()` で `null` を返すような、エラー状態を`null`で表している場合は、`null`以外の値でエラー状態を表さなければなりません。

さきほどの `onErrorReturn()` の例でいえば、つぎのようになります。

```java
observable.getItems()
  .onErrorReturn(throwable -> {
    Log.e(TAG, "アイテムの取得に失敗しました: " + e.getMessage());
    return Item.ERROR;
  });
```

このとき、つぎのように、エラー用のオブジェクトをあらかじめ用意しておきます。

```java
public class Item {
  public static final Item ERROR = new Item();
  ...
}
```

# エラーハンドリングの変更への対応
2.xでの大きな変更の3つ目は、エラーハンドリングの変更です。
詳細は[公式ドキュメント](https://github.com/ReactiveX/RxJava/wiki/What's-different-in-2.0#error-handling)に譲りますが、対応が必要になるほとんどのケースは、
`Disposable` が `dispose` された後にエラーが発生した場合です。

たとえば、つぎのようなコードで、 `Info` の取得中にエラーが発生し、 `onError()` が呼ばれ、しかも既に `Disposable` が破棄されていた（`dispose()`が呼ばれていた）場合、そのままだとアプリがクラッシュしてしまいます。

```java
return Observable.create((ObservableOnSubscribe<Info>) emitter -> {
  try {
    Info info = /* なんらかの処理 */;
    emitter.onNext(adId);
    emitter.onComplete();
  } catch (Exception e) {
    emitter.onError(e);
  }
});
```

1つの方法は `Emitter#isDisposed()` ですでに `Disposable` が破棄されているかチェックすることです。あるいは、つぎのようにグローバルにエラーハンドラをセットすることで回避できます。
`Application#onCreate()`などでセットしておきましょう。

```java
RxJavaPlugins.setErrorHandler(e -> {
  if (e instanceof UndeliverableException) {
    e = e.getCause();
  }
  Log.w(TAG, "Undeliverable exception: " + e.getMessage());
});
```

# RxJava 1.xとRxJava 2.xを共存させたいとき
最後に、使っているライブラリがRxJava 2.xに対応していないケースについて述べます。
もし使っているライブラリがRxJava 1.xのみに対応していて2.xに対応していない場合でも2.xへ移行することは比較的容易です。

まず [RxJava2Interop](https://github.com/akarnokd/RxJava2Interop) を依存ライブラリに含めましょう。

```groovy
dependencies {
    compile "com.github.akarnokd:rxjava2-interop:0.9.6"
}
```

その後に、RxJava 1.xのObservableをつぎのようにラップします。

```java
RxJavaInterop.toV2Observable(v1Observable)
```

これだけです。

ひとつ気をつけないといけないのは、1.xと2.xを共存させたときに、コンパイル時につぎのようなエラーが出ることがあることです。

```bash
com.android.build.api.transform.TransformException: com.android.builder.packaging.DuplicateFileException: Duplicate files copied in APK META-INF/rxjava.properties
    File1: /home/adi/.gradle/caches/modules-2/files-2.1/io.reactivex/rxjava/1.1.8/f9dbae366a6a3d6b5041c5e8db0dcdfdc35c27b5/rxjava-1.1.8.jar
    File2: /home/adi/.gradle/caches/modules-2/files-2.1/io.reactivex.rxjava2/rxjava/2.0.8/3ee37bb825446a3bafac68a46f2397a8affd9b68/rxjava-2.0.8.jar
```

その場合は、build.gradleにつぎの行を足して、`rxjava.properties`をexcludeしましょう[^5]。

```groovy
packagingOptions {
  exclude 'META-INF/rxjava.properties'
}
```

[^5]: 詳しくは https://github.com/ReactiveX/RxJava/issues/4445 を参照してください

# ObservableとFlowable、Single、Maybe、Completable
以上で2.xへの移行はほぼ完了です。「ほぼ」と言ったのは、いままでの作業はコードに最小限の変更しか施さないことを目的としていたからです。

RxJava 2.xでは、Reactive Streamsへ準拠することを念頭に、明示的にバックプレッシャーありのFlowableと、バックプレッシャーなしのObservableにクラスが分かれました。また、Single、Maybe、Completableという用途が特化したObservableも使いやすくなっています。

Flowableは強力ですが、AndroidアプリのコードではUIイベントか、あるいは単発の非同期処理で済むケースが大半です。あまりお世話になることはないかもしれません。
一方、Single、Maybe、Completableは、利用できる箇所が多くあります。これらの特性についてまとめると、つぎのようになります。

||onSuccess()/onComplete()/onError()がどう呼ばれるか|
|------|-----------------|
|[Single](https://github.com/ReactiveX/RxJava/wiki/What's-different-in-2.0#single)|(onSuccess \| onError)|
|[Maybe](https://github.com/ReactiveX/RxJava/wiki/What's-different-in-2.0#maybe)|(onSuccess \| onComplete \| onError)|
|[Completable](https://github.com/ReactiveX/RxJava/wiki/What's-different-in-2.0#completable)|(onComplete \| onError)|

Singleは値が1つ得られるかエラーになるかのどちらか、Maybeは値が0個か、1個か、エラーのどれか、Completableは値が0個かエラーのどちらか、の処理を表します。

たとえばHTTP GETのような場合はレスポンスが正常に返ってくるかエラーが返るかのどちらかなのでSingleを利用すべきです。
あるいはHTTP POSTでレスポンスに興味ないなら、正常に終了するかエラーが返るかなのでCompletableの利用が適しています。
2.xでは、これらの型に対するオペレータも充実しているので、ぜひ利用してみてください。

さらに2.xの変更の背景や詳細について知りたい方は、私がDroidKaigiで発表した資料に解説があるので、良かったら読んでみてください。

<iframe class="speakerdeck-iframe" frameborder="0" src="https://speakerdeck.com/player/c3ff72e36e344d4aa581584d7c3d01da" title="What's New in RxJava 2" allowfullscreen="true" style="border: 0px; background: padding-box padding-box rgba(0, 0, 0, 0.1); margin: 0px; padding: 0px; border-radius: 6px; box-shadow: rgba(0, 0, 0, 0.2) 0px 5px 40px; width: 100%; height: auto; aspect-ratio: 560 / 420;" data-ratio="1.3333333333333333"></iframe>

# 最後に
もしRxJava 1.xを利用していて、今後1年以上メンテナンスしなくてはならないのならRxJava 2.xへの移行はほぼ必須となります。

一方でnullを許容しないといった制約は、後になればなるほど移行を難しくする要因となります。
また、本記事では触れませんでしたがRxJava 2.xではパフォーマンスも向上しているというベンチマークの結果もあります。
可能ならば、早めの移行を検討しておくに越したことはないでしょう。

本記事がRxJava 2.x移行に際して、いくばくなりともお役に立てたのなら幸いです。

---

Fablicでは、RxJava 2.xのような最新のライブラリを使ってAndroidアプリの開発を行っています。このような意欲的な環境で共にプロダクトを作り上げていきたいアプリエンジニアの方のご応募もお待ちしております！
