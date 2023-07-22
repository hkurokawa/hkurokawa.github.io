---
layout: post
title:  "RxJava は Subscriber を中心に捉えると理解しやすいんじゃないかという話"
date:   2015-12-21 09:28:45.000000000 -0800
categories: 
---
これは[RxJava Advent Calendar 2015](http://qiita.com/advent-calendar/2015/rxjava)の第22日目の記事です。昨日は [kazy](http://qiita.com/kazy) さんによる [RxJava 2.xについて](http://qiita.com/kazy/items/8b1e0bcc5cd9638876d4) でした。

この記事では、 RxJava を理解するために自分が RxJava をどのように捉えているか、という話をします。なお、自分は ReactiveX や Reactive Functional Programming について詳しいわけではないので、その方面の理解の助けになるものではありません。どちらかといえば、RxJava という特定のライブラリをこう理解しておけば全体の挙動を把握しやすいのではないか、という生活の知恵のようなものになります。RxJS や他のライブラリではまた異なる実装かもしれませんのでご留意ください。

RxJava は非常に強力でよく考えられたものですが、ソースを見て挙動が直感的に分かるとは言えないライブラリです。本稿が自分のような初学者の理解の助けになれば幸いです。

<!-- more -->

さて、最初にネタばらしをすると、この考えは、自分のオリジナルではなく、かの Jake Wharton による以下の講演にインスパイアされたものです。というか、そのまんまです。Jake の発表資料は[Presentation: Demystifying RxJava Subscribers - Jake Wharton](http://jakewharton.com/presentation/2015-11-05-oredev/)にあります。

<iframe src="https://player.vimeo.com/video/144812843" width="500" height="281" frameborder="0" webkitallowfullscreen mozallowfullscreen allowfullscreen></iframe>

また Jake かよ、と思われた方もいらっしゃるでしょうが、自分としても、毎度 Jake に影響されるのは忸怩たるものがあるのです。しかし、この講演を聴いて目から鱗が落ちたのも事実。興味を持った方はぜひ聴いてみてください。

この記事では、この Subscriber を中心に理解する、という考えを自分なりに咀嚼したものを紹介し、自分が RxJava に初めて触れたときに分かりにくかった挙動、たとえば `observeOn()` と `subscribeOn()` の違いをどう理解するか、という話をします。

# すべては Subscriber である

RxJava には多くのクラスが登場します。たとえば、ざっと挙げただけで、これくらいでしょうか。

- Observable
- Observer
- Subscriber
- Subscription
- Operator
- Subject

「ふむふむ。`Observable` を `Observer` が subscribe して、そのときの subscribe した状態を表すのが `Subscription`。`Operator` は `Observable` に適用してストリームの挙動を変更するものだな。」と、それぞれの概念はなんとなく分かりますが、実際のコードで、それらがどう関係しているのか分かりにくいのが難点です。なんとなくスッキリしません。ということで、実装を見ながら、これらのクラスの役割を見てみましょう。

## Under the Hood: Observer, Observable and Subscription

まず `Observable#subscribe(Observer)` のコードを見ましょう。以下のようになります。

```java
public final Subscription subscribe(final Observer<? super T> observer) {
    if (observer instanceof Subscriber) {
        return subscribe((Subscriber<? super T>)observer);
    }
    return subscribe(new Subscriber<T>() {
        @Override
        public void onCompleted() {
            observer.onCompleted();
        }
        @Override
        public void onError(Throwable e) {
            observer.onError(e);
        }
        @Override
        public void onNext(T t) {
            observer.onNext(t);
        }
    });
}
```

これだけです。Subscriber でラップされ、`subscribe(Subscriber)` を呼び出しているに過ぎません。実は、他の `subscribe(Action1)` や `subscribe()` なども同様です。すなわち、`subscribe(...)` 系のメソッドは `subscribe(Subscriber)` に収束することになります。

それでは、肝心の `Observable#subscribe(Subscriber)` はどうなっているのでしょうか。中心となるコードだけ抜き出すと以下のようになります。

```java
// new Subscriber so onStart it
subscriber.onStart();

// if not already wrapped
if (!(subscriber instanceof SafeSubscriber)) {
    // assign to `observer` so we return the protected version
    subscriber = new SafeSubscriber<T>(subscriber);
}

// allow the hook to intercept and/or decorate
onSubscribe.call(subscriber);
return subscriber;
```

実際には、`subscriber#onStart()` にフックするためにもう少し複雑なのですが、基本的な挙動は上のようになります。ここで `SafeSubscriber` でラップしているのは、`onCompleted()` で `unsubscribe` を行ったりといった Observable の契約（http://reactivex.io/documentation/contract.html）に従うためです。ここをとりあえず無視すると、`Observable` の役目は自身が保持している `onSubscribe` の呼び出しのみということになります。

さらに、この `onSubscribe` の実体ですが、これは以下の関数（クラスとしては `OnSubscribe`）です。つまり、Observable の役目は「subscribe(Subscriber) が呼び出されたら、その Subscriber に対して何らかの処理を行う」というだけなのです。

```
void call(Subscriber<? super T> t);
```

ここまでを整理してみましょう。

- `subscribe(Observer)` や `subscribe(Action1)` は最終的に `subscribe(Subscriber)` 呼び出しになる
- `Observable` は、Subscriber が subscribe したときに、それに対して何を行うかを表すもの

ここで具体的に subscriber に何を行うか、ですが、たとえば `Subscriber#onNext(T)` を呼び出したり `Subscriber#onCompleted()` や `Subscriber#onError(Throwable)` を呼び出したりということになります（以降では、これらのメソッド呼び出しをまとめてイベントの emit と呼称します）。もちろん、Observable の契約（http://reactivex.io/documentation/contract.html）に従う必要がありますが、そこは Observable の実装に任されています。

さらに、さきほどの `Observable#subscribe(Subscriber)` の最後の `return` 文を見れば分かりますが、戻り値の `Subscription` は、すなわち `subscriber` なのです。

- Subscription の実体は Subscriber である

ここで挙げた3つのクラスにおいて Subscriber がすべての中心になっていることがお分かりいただけたでしょうか。

### What happens on a simple subscribe()?

非常に単純な例で、これら Observable、Observer、Subscription の挙動を見てみましょう。なお、コード中のコメントの `↑` は subscribe 時の流れ、`↓` はイベントの emit 時の流れを表します。

```java
subscription = Observable
    .just("Hi!")                          // (1) ↑(3) ↓(3)
    .subscribe(new Observer<String>(){    // (2) ↑(2) ↓(4)
        @Override
        public void onCompleted() {
        }
        @Override
        public void onError(Throwable e) {
        }
        @Override
        public void onNext(String s) {
            System.out.println(s);
        }
    });
...
// 何らかのタイミングで以下を呼ぶ
subscription.unsubscribe();               // (5)
```

(1) まず、`Observable.just("Hi!")` の時点では何も起きません。ただ、subscribe されたときの挙動を定義した onSubscribe が生成され、未来の `subscribe()` に備えます。

(2) 次に observer が無名クラスで生成され、`subscribe()` メソッドに渡され実行されます。このとき、observer は subscriber でラップされ、それを引数として (3) さきほど生成された onSubscribe が実行されます。

ここで `Observable#just()` の中を覗くと、この onSubscribe が実行されたときに `s.onNext()` と `s.onCompleted()` を呼び出すだけということが分かります。

```java
protected ScalarSynchronousObservable(final T t) {
    super(new OnSubscribe<T>() {
        @Override
        public void call(Subscriber<? super T> s) {
            s.onNext(t);
            s.onCompleted();
        }
    });
}
```

ここに至って、ようやく (4) observer の `onNext()` が呼ばれます。そして最後に、(5) どこかのタイミングで subscriber の `unsubscribe()` が呼ばれます。

以上の処理を subscriber 視点からまとめて一般化すると以下になります。

1. observable を生成して subscribe 時に、渡された subscriber に対してイベントを emit する準備をする
2. observer を subscriber でラップして `subscribe()` に渡す
3. observable 内の `onSubscribe.call()` が呼ばれる
4. その `onSubscribe.call()` 内で subscriber の `onNext()` などが呼ばれる（イベントが emit される）
5. どこかで subscriber の `unsubscribe()` が呼ばれる

どうでしょうか。一連の流れが Subscriber を中心にすることで見通しが良くならないでしょうか。ポイントは、主要な実体が subscriber だけで、observable は、その subscriber のメソッドをどう呼ぶか（イベントをどう emit するか）、observer と subscription は subscriber の一側面に過ぎない、ということです。

## Under the Hood: Operator

さらに、Subscriber の視点から Operator を理解してみましょう。結論から言うと `Operator<R, T>` は「`Subscriber<R>` から `Subscriber<T>` への変換を規定するもの」と捉えることができます。が、これだけでは意味不明だと思いますので、実際のコードを見てみましょう。

なお、以下では、Observable を生成し、オペレーターをチェインして最後に subscribe するケースを考えます。このとき、元になる Observable の生成をソース、最後の `subscribe()` メソッドの呼び出しをシンク、と便宜的に呼称します。つまり、フローがソースからシンクへオペレーターをチェインして流れていくわけです。

それでは、以下のコードを考えます。Operator には `map()`、`filter()` など各種ありますが、ここでは `map()` を取り上げます。

```java
Observable
    .just("Hi!")
    .map(new Func1<String, Integer>() {
        @Override
        public Integer call(String s) {
            return s.length();
        }
    }).subscribe(new Observer<String>(){
        @Override
        public void onCompleted() {
        }
        @Override
        public void onError(Throwable e) {
        }
        @Override
        public void onNext(String s) {
            System.out.println(s);
        }
    });
```

文字列を受け取って長さに変換するだけの簡単な処理です。それでは実装を見てみましょう。まず `Observable#map()` の中を覗くと以下のようになっています。

```java
public final <R> Observable<R> map(Func1<? super T, ? extends R> func) {
    return lift(new OperatorMap<T, R>(func));
}
```

`OperatorMap` を `lift(Operator)` メソッドで処理しています。ここでは OperatorMap の実装はひとまず措いておいて、先に `Observable#lift(Operator)` の中を見てみます。そのあと OperatorMap の実装に戻ります。

### Observable#lift(Operator)

`Observable#lift(Operator)` の中身はエラー処理やフックなどを除くと以下だけになります。

```java
public final <R> Observable<R> lift(final Operator<? extends R, ? super T> operator) {
    return new Observable<R>(new OnSubscribe<R>() {
        @Override
        public void call(Subscriber<? super R> o) {
            Subscriber<? super T> st = operator.call(o);
            // new Subscriber created and being subscribed with so 'onStart' it
            st.onStart();
            onSubscribe.call(st);
        }
    });
}
```

ちょっと分かりにくいと思うので、1つずつ説明しましょう。まず `Operator<R, T>` に関しては、次のようなものだと思ってください。「`Subscriber<R>` を受け取って `Subscriber<T>` に変換する」もの。

そうですね、`R` や `T` だと分かり辛いので、ここでは実際の型、すなわち `T` に `String` を、`R` に `Integer` を入れましょう。そうすると、さきほどの `lift(Operator)` の実装は以下のようになります。

```java
public final <Integer> Observable<Integer> lift(final Operator<? extends Integer, ? super String> operator) {
    return new Observable<Integer>(new OnSubscribe<Integer>() {
        @Override
        public void call(Subscriber<? super Integer> o) {
            Subscriber<? super String> st = operator.call(o);
            // new Subscriber created and being subscribed with so 'onStart' it
            st.onStart();
            onSubscribe.call(st);
        }
    });
}
```

ここで operator は「`Subscriber<Integer>` を受け取って `Subscriber<String>` に変換する」ものだと思ってください。直感に反しますか？この map 関数は `String -> Integer` の処理のはずなのに、なぜ operator は `Subscriber<Integer> -> Subscriber<String>` なのでしょうか。それについては後ほど説明します。

さて、この `lift(Operator)` メソッドの中身はどこかで見たことのあるコードです。`OnSubscribe` を実装して Observable を新たに生成しています。つまり `lift(Operator)` メソッドは Observable 生成メソッドなわけです。operator でチェインするのは、すなわち、observable を次々に生成することに他ならないわけです。

先ほどの observable に関する理解を思い出しましょう。「observable は Subscriber が subscribe したときにそれに対して行う処理を定義したもの」でした。では、この `lift(Operator)` メソッドで生成した Observable が何を表しているかというと、次の処理になります。「subscribe されたら、渡された `Subscriber<Integer>` から `Subscriber<String>` を生成して、親の onSubscribe を呼び出す」。

`親の onSubscribe` というのは、`lift(Operator)` を呼び出している Observable、この例の場合は `Observable.just()` の戻り値になります。Observable が2つ登場するのでややこしいですね。今回の例で言えば、親の Observable は「subscribe されたら subscriber に対して `onNext("Hi!")` を呼び出す処理」を表し、`lift(Operator)` で返される Observable は「subscribe されたら subscriber を `Subscriber<String>` 型に変換し、それを親に渡す」、つまり、「`Subscriber<Integer>` 型の subscriber を `Subscriber<String>` 型に変換し `onNext("Hi!")` を実行する処理」を表すことになります。

ここまでくれば、なぜ operator が `Subscriber<Integer> -> Subscriber<String>` という形式になっているか分かります。各 Observable の subscribe の呼び出される順番はメソッドチェインの逆、つまりシンクから順にソースにさかのぼって呼び出されるからです。

さて、`lift(Operator)` メソッドで subscribe 時に順々にさかのぼって Observable の onSubscribe が呼ばれていくのが分かりました。では `Subscriber#onNext()` 時はどうなるのでしょうか。`Observable.just("Hi!")` 内で `subscriber#onNext()` が呼ばれたとき、どのようにメソッドチェインの下に伝わっていくのでしょうか。それは Operator の役割になります。

### OperatorMap

お待たせいたしました。ようやく OperatorMap の実装を見てみましょう。実装は以下のようになります（例に合わせてジェネリクスを実際の型に置き換えています）。ここで、`transformer` は `map(Func1)` 関数で渡された関数オブジェクトで中身は `(String s) -> { return s.length(); }` になります。

```java
@Override
public Subscriber<? super String> call(final Subscriber<? super Integer> o) {
    return new Subscriber<String>(o) {
        @Override
        public void onCompleted() {
            o.onCompleted();
        }
        @Override
        public void onError(Throwable e) {
            o.onError(e);
        }
        @Override
        public void onNext(String t) {
            try {
                o.onNext(transformer.call(t));
            } catch (Throwable e) {
                Exceptions.throwOrReport(e, this, t);
            }
        }
    };
}
```

拍子抜けでしょうか。見て分かるように、`Subscriber<Integer>` を受け取って、`Subscriber<String>` を返しています。そして、その返した subscriber の中身はというと、「`onNext(String)` を呼ばれたら、渡ってきた文字列に `transformer` を適用してから `Subscriber<Integer>#onNext(Integer)` を呼ぶ」というだけです。つまり、この Operator は、渡された subscriber をラップして新しい subscriber を返すわけです。

前節で見たように、`Observable#subscribe()` が呼ばれると、各 Operator を onSubscribe がチェインしてさかのぼっていきます。そして、最終的に `Subscriber#onNext()` が呼ばれると、こんどは Operator チェインを下って、つぎつぎにラップされている subscriber の `onNext()` を呼んでいきます。

このように、`Observable` の各種 Operator メソッドを呼ぶと、チェインが形成され、subscribe 時にはシンクからソースに向かって、イベントの emit 時にはソースからシンクに次々と情報が伝播していきます。

ちょっと面白いのは、この Operator の実装が、戻り値が `Subscriber` であることを除いて、Observable の実装に似ていることです。どちらも subscriber を渡されたときに、どんな処理を行うかを定義しているだけであり、実体はあくまで subscriber ということが分かります。

### What happens on a chained subscribe()?

最初に挙げたサンプルの場合、どのような処理が行われるか簡単におさらいしてみましょう。さきほどと同様に、コード中のコメントの `↑` は subscribe 時の流れ、`↓` はイベントの emit 時の流れを表します。

```java
1    Observable
2       .just("Hi!")                          // (1) ↑(5) ↓(5)
3       .map(new Func1<String, Integer>() {   // (2) ↑(4) ↓(6)
4           @Override
5           public Integer call(String s) {
6               return s.length();
7           }
8       }).subscribe(new Observer<String>(){  // (3) ↑(3) ↓(6)
9           ...
10      });
```

(1) まず、`Observable.just("Hi!")` の時点では何も起きません。これはさっきと同様です。

(2) 次に `map()` メソッドで新たな observable が生成されます。この observable は、onSubscribe が実行されたとき、渡された subscriber に OperatorMap で表される変換を施し、親（`Observable.just()` の戻り値）の onSubscribe を実効する、という振舞いをします。

(3) 次に シンクの `subscribe()` メソッドが呼ばれます。このとき、observer は subscriber でラップされ、それを引数として、さきほど生成された observable 内の onSubscribe が実行されます。

(4) (2) で定めたように、渡された subscriber を OperatorMap で変換し、親の onSubscribe （`Observable.just()` の onSubscribe）を実行します。

(5) `Observable.just()` の定義に従って、`subscriber.onNext("Hi!")` が呼ばれます

(6) この 3 行目で生成された subscriber は OperatorMap 内で生成されたもので、"Hi!" を数字の 3 に変換し、さらに子供の subscriber、つまり 8 行目で渡された subscriber の `onNext(3)` を呼び出します。

以上の処理を一般化して subscriber 視点からまとめると以下になります。

1. ソースの observable を生成して onSubscribe 時に subscriber に対して何をするか決める
2. operator メソッドによって、onSubscribe 時に、その observable の onSubscribe を呼び出す新しい observable を生成する
3. 2.を何回か繰り返す
4. シンクの `subscribe()` を呼んだタイミングで、observer を subscriber でラップして、一番近い observable の onSubscribe を実行する
5. その onSubscribe の実行内で、親の onSubscribe を実行する
6. 5.を何回か繰り返す
7. ソースの `OnSubscribe#call()` 内で subscriber の `onNext()` が呼ばれる
8. `onNext()` 内で子供の subscriber の `onNext()` が呼ばれる
9. 8.を何回か繰り返す
10. シンクの subscriber の `onNext()` が呼ばれる

### The Role of Operator

少し横道にそれて、Operator の責務について考えます。前節で見たように、Operator を実装する際は、基本的に子供の subscriber のイベントの emit をきちんと実行してやる必要があります。また、先ほどの例では触れませんでしたが、`unsubscribe()` も正しくハンドルしなければなりません。

ここで Subscription の実体が Subscriber だったことを思い出しましょう。シンクの `subscribe()` の戻り値である subscription の実体は、どの subscriber でしょうか？はい、答えは、シンクの subscriber です。つまり、`subscription#unsubscribe()` を呼んだときは、シンクの subscriber の `unsubscriber()` が呼ばれ、そのあと順に親の subscriber の unsubscribe を実行する責務になります。

それでは、OperatorMap はこれをどのように実装しているのでしょうか。特に子供の subscriber の `unsubscribe()` にフックしているようには見えません。実は、コンストラクタで `o` を渡しているのがミソになります。

```java
@Override
public Subscriber<? super String> call(final Subscriber<? super Integer> o) {
    return new Subscriber<String>(o) {
        ...
    };
}
```

Subscriber の実装を読むと分かりますが、このコンストラクタで渡された subscriber の `subscriptions` は親の subscriber と共有される挙動になっています。このため、子供の subscriber が unsubscribe されたときも正しく unsubscribe が親に伝播していくわけです。

もし親の subscriber が何らかのリソースを持っていて unsubscribe 時に解放する必要があるなら、自前で解放しなければなりません。たとえば `OperatorObserveOn` を見ると、子供の subscriber に対して `add(Subscription)` を呼んで、子供の `unsubscribe` 時にリソースを解放することを保証しています。

実は、もう1つ、BackPressure についても、正しく伝播する責務があるのですが、こちらはオプショナルなので、今回は割愛します。

なお、余談ですが、これらの責務は、自分でカスタム Observable を作る場合も同様です。

# Demystifying some behavirous in RxJava

ここまで、Observable、Observer、Subscription、Operator について Subscriber を中心に説明してきました。これらのクラスの見え方もだいぶ変わってきたのではないでしょうか。

最後に、RxJava で分かりにくいと自分が思ういくつかの挙動について、この考え方をもとに説明してみます。

## When is the first event emitted? What is Hot/Cold Observable?

初めて RxJava に触れたときに戸惑ったのが、「いつイベントが emit されるか」でした。多くの Observable は subscribe するまでは emit されないということを知らなかったために、たとえば HTTP 通信などはその場で呼ばれるのかと勘違いしていました。なんとなくイベントがずっと流れている状態をイメージしていたのです。

しかし、いままで見てきたように、Observable は「subscribe されたときに、Subscriber に対してどんな処理を行うか」を定義しただけのものです。したがって、多くの Observable は subscribe するまで何の処理も行いません。もちろん、生成直後から emit しても構いませんし、subscribe 時には emit せず、別のトリガーで emit するものもあります。いずれにせよ、Observable は subscribe してきた subscriber に対して、それぞれの性格に応じた振舞いをします。あるものは subscribe のタイミングでストリームの最初から最新までを流してくれるかもしれませんし、あるものは最新のもの1つを返すだけかもしれません。

たとえば、以下のコードで何が出力されるでしょうか？

```java
Observable o = Observable.range(0,10);
o.subscribeOn(Schedulers.newThread()).subscribe((i) -> { System.out.println(i + "from subscriber1"); })
o.subscribeOn(Schedulers.newThread()).subscribe((i) -> { System.out.println(i + "from subscriber2"); })
```

これは Observable の振舞い次第なのですが、`Observable.range()` の場合は、subscriber ごとに 0 から 9 までの整数を emit するため、出力は 0 から 9 までの数字が2つずつそれぞれ入り混じって表示されることになります。このように、subscribe されてから初めてイベントが emit され、subscriber が互いに独立している Observable を Cold Observable と呼びます。

上記のコードの結果:
```
0 from subscriber1
1 from subscriber1
0 from subscriber2
2 from subscriber1
1 from subscriber2
2 from subscriber2
3 from subscriber2
3 from subscriber1
4 from subscriber2
4 from subscriber1
5 from subscriber2
5 from subscriber1
6 from subscriber2
6 from subscriber1
7 from subscriber2
7 from subscriber1
8 from subscriber2
8 from subscriber1
9 from subscriber2
9 from subscriber1
```

一方、Hot Observable は subscribe 時にイベントの emit が始まるとは限りませんし、他の subscriber にも同じイベントが emit されます。より詳細を知りたい方は[Cold vs. Hot Observables](https://github.com/Reactive-Extensions/RxJS/blob/master/doc/gettingstarted/creating.md#cold-vs-hot-observables)を読んでみてください。

この Hot/Cold Observable についても、Observable が「subscribe 時に subscriber のメソッドを適当に呼び出すもの」と捉えると分かりやすいのではないでしょうか。

## Difference between `obserbeOn()/subscribeOn()`

もう1つ、自分がよく分からなかったのは `observeOn()` と `subscribeOn()` の挙動の違いです。[ReactiveX - Scheduler](http://reactivex.io/documentation/scheduler.html) には、

> the SubscribeOn operator designates which thread the Observable will begin operating on, no matter at what point in the chain of operators that operator is called. ObserveOn, on the other hand, affects the thread that the Observable will use below where that operator appears.
> 
> SubscribeOn は Observable が実行され始めるスレッドを指示し、オペレーターチェインのどこにあっても変わらない。一方、ObserveOn は、オペレーターチェインの中でそのオペレーターが現れた場所以降で Observable が実行されるスレッドを指示する 

と書いてありますが、なぜこのような挙動になるのでしょうか。下図はこの振舞いを図示したものです。

![](http://reactivex.io/documentation/operators/images/schedulers.png)
（[ReactiveX - Scheduler](http://reactivex.io/documentation/scheduler.html) より）

また、`subscribeOn()` が複数回登場したらどうなるのでしょうか？[詳解RxJava：Scheduler、非同期処理、subscribe/unsubscribe - Qiita](http://qiita.com/yuya_presto/items/c8c3d77ac958c9c8f67b) には「subscribeOnは複数回呼び出した場合、最終的に一番根っこ側で指定されたSchedulerで実行されることになります。」とあります。なぜ、このような振る舞いになるのでしょうか？

これを理解するには、上に見てきたように subscribe がチェインしていく時の流れとイベントの emit がチェインしていくときの流れを分けて考えるのが良いでしょう。

つまり、`subscribeOn()` は前者の subscribe チェイン時のスレッドを変更するオペレーターで、`observeOn()` は emit が実行されるスレッドを変更するオペレーターになるのです。`subscribeOn()` が呼ばれると、Observable 内の onSubscribe を実行するスレッドが変わります。この変更はオペレーターチェインの **上方向** のみに影響します。なぜなら、subscribe はオペレーターチェインのシンクからソース方向へ伝わるからです。一方、`observeOn()` は **下方向** のみに影響します。なぜなら emit はソースからシンクへ伝わるからです。

それでは、なぜ `subscribeOn()` を呼ぶと、Observable の実行スレッド、すなわち emit の実行スレッドが変更されるのでしょうか？これは単に Observable の実装に依存した挙動です。多くの Observable は onSubscribe 実行時に内部で `Observable#onNext()` を呼びます。したがって、その `onNext()` が実行されるスレッドも `subscribeOn()` で指定したスレッドになるのです。これは `observeOn()` か、その他の実行スレッドを変更するオペレーターが現れるまで変わりません。

言葉では分かりにくいと思うので、ためしに以下のコードを実行してみましょう。見て分かるように、onSubscribe 実行時のスレッドと、`onNext()` 実行時のスレッドを標準出力するオペレーターを合間に挟むことで上記の説明を確認しています。

```java
public void testObserveOnSubscribeOn() {
    Observable.just("Hi!")
            .observeOn(s5)
            .lift(new PrintThreadOperator<>("1")) // <- SubscribeThread[1]: s1, ObserveThread[1]: s5
            .subscribeOn(s1)
            .observeOn(s4)
            .lift(new PrintThreadOperator<>("2")) // <- SubscribeThread[2]: s2, ObserveThread[2]: s4
            .map(String::length)
            .subscribeOn(s2)
            .observeOn(s3)
            .lift(new PrintThreadOperator<>("3")) // <- SubscribeThread[3]: Main, ObserveThread[3]: s3
            .subscribe(integer -> {
                        System.out.println("VALUE: " + integer);
                    },
                    throwable -> {
                        System.err.println(throwable.getMessage());
                        throwable.printStackTrace();
                    });
}

private static class PrintThreadOperator<T> implements Observable.Operator<T, T> {
    private final String name;
    public PrintThreadOperator(String name) {
        this.name = name;
    }
    @Override
    public Subscriber<? super T> call(Subscriber<? super T> t) {
        System.out.println("SubscribeThread [" + name + "]: " + Thread.currentThread().getName());
        return new Subscriber<T>() {
            @Override
            public void onCompleted() {
                t.onCompleted();
            }
            @Override
            public void onError(Throwable e) {
                t.onError(e);
            }
            @Override
            public void onNext(T s) {
                System.out.println("ObserveThread [" + name + "]: " + Thread.currentThread().getName());
                t.onNext(s);
            }
        };
    }
}
```

結果は以下のようになり、説明を裏付けています。出力順を見ると subscribe 時にシンクからソースへ、emit 時にソースからシンクへ伝播していく様子も分かります。

```
SubscribeThread [3]: main
SubscribeThread [2]: s2
SubscribeThread [1]: s1
ObserveThread [1]: s5
ObserveThread [2]: s4
ObserveThread [3]: s3
VALUE: 3
```

このように、`subscribeOn` は実際には onSubscribe の実行スレッドを変更するオペレーターですし、一番ソースに近いオペレーターのみが Observable に影響を与えるのは事実ですが、それ以降の `subscribeOn` オペレーターが無意味なわけでもありません。たとえば途中のオペレーターが onSubscribe 実行時に重い処理をする場合はスレッド指定をすることもあるでしょう。とはいえ、通常の使い方ではソースの Observable の実行スレッドを変えることがメインの目的になると思われるため、オペレーターチェインのどこかで1回だけ使用するという使い方で問題はありません。

# まとめ

本稿では Subscriber を中心にして RxJava を理解する考えを示しました。それによって、Observable、Observer、Subscription、Operator が実際に何を行っているか分かりやすくなったと思います。また、最後に Cold/Hot Observable および `subscribeOn()/observeOn()` の挙動について、この考え方で見通しよく理解できる（と願っているのですが）ことを示しました。

最初に述べたように、これはあくまで RxJava をどう捉えるかという見方に過ぎません。また別の見方もあると思いますし、ともかくコードを読み込めば理解できること、とも言えます。自分の場合は、[RxNearby](https://github.com/hkurokawa/RxNearby) というライブラリを開発しているときにカスタム Observable や Operator を作る必要があったのですが、その際にどう実装したら良いか悩んだのが発端でした。ちょうど Jake の発表を聞き、そのあとに RxJava のコードを読むと非常に分かりやすかったので、今回共有いたしました。

RxJava はこの一年で一気にポピュラーになり、今後もこの Reactive Stream 系のライブラリは Android 開発における非同期ライブラリの主流になる可能性は高いのではないかと思います。一方で、「RxJava をどう理解するか」という文書は星の数ほどあり、それだけ多くの人が RxJava にモヤモヤ感を抱いていることの現れなのかもしれません。本稿もまた、その星の数ほどある記事の末座に連なるわけですが、いくばくかでも読んでくださった方の理解の一助になれば幸いです。

明日は [gfx](http://qiita.com/gfx) さんによる「RxJavaのミニマム実装（RxInTheBox）にSchedulersを実装してみるゾ！」です。とてもたのしみです。