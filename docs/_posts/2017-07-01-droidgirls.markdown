---
layout: post
title:  "DroidGirlsでRxJavaのハンズオンをしてきました"
date:   2017-07-01 23:42:07.000000000 -0700
categories: 
---
DroidGirlsで「RxJava2」を題材に解説とハンズオンをしてきました（[connpassのページ](https://droidgirls.connpass.com/event/57601/)）。

<!--more-->

# 内容
題材の解説は30分、ハンズオンは1時間半弱ということで、今回はRx初心者が一通りRxJavaを使えるようになることを目的としました。
RxJava2を使っていますが、解説自体ではRxJava2の詳細には踏み込まずにRxの基本的な概念とよく使うオペレータを解説しています。

<iframe class="speakerdeck-iframe" frameborder="0" src="https://speakerdeck.com/player/2c870d45b24047bab58a61afdf52a65a" title="RxJava 入門" allowfullscreen="true" style="border: 0px; background: padding-box padding-box rgba(0, 0, 0, 0.1); margin: 0px; padding: 0px; border-radius: 6px; box-shadow: rgba(0, 0, 0, 0.2) 0px 5px 40px; width: 100%; height: auto; aspect-ratio: 560 / 420;" data-ratio="1.3333333333333333"></iframe>

課題は [https://github.com/DroidGirls/Meetup/tree/master/6_rxjava](https://github.com/DroidGirls/Meetup/tree/master/6_rxjava) にあるので、腕に覚えのある方もない方も良かったら挑戦してみてください。
もっと良いやり方があるよ、ここが間違っているよ、という指摘があれば、ぜひ教えてください。

# 感想
初めてだったのですが、人数も場所もちょうど良く、色々な人とお話できてたのしかったです。招いていただいたDroidGirlsの運営の方々、ありがとうございました。

とはいえ、反省もないわけではなく、というか、だいぶあって、ここでは一人で反省をしたいと思います。

## ハンズオンのむずかしさ
課題を4つ、発展課題を2つ用意しました。時間内に課題4までいけるかなと思っていましたが、ほとんどの人は課題2か3くらいまでだったようです。

できるだけ、RxJavaを使うところ以外はこちらで済ませているつもりだったのですが、自分の準備のマズさもあって（最初はJSONを表すオブジェクトをリスト用に変換するユーティリティメソッドがなかったり subscribeOn/observeOn の説明が不十分だったり）、思ったよりも手がかかってしまったようです。

また、質問も最初はなかなか出ず、もうちょっと質問しやすい雰囲気を作れば良かったかなと反省しています。歩き回ったり、詰まっていそうな人に声をかけるなどをすれば良かったです。

自分は、みんなの進捗を把握する余裕もあまり持てなかったのですが、ハンズオンの途中で[@joooi13](https://twitter.com/joooi13)さんが進捗を見つつ課題の解答を紹介する形式を取っていただいたのが、とても助かりました。
ハンズオンとはいえ、ずっと詰まったまま時間を過ごすのは勿体ないので、適宜解説を挟んで、前に進めた方が良いように思いました。

## Rxのむずかしさ
今回は、Rxの難しい部分をできるだけスキップしたのですが、それでも、自分から見ても初見で理解するのはキツいだろうなというボリュームになってしまいました。

つまづきやすい箇所はある程度把握していたつもりだったのですが、subscribeOn/observeOnの使い分けについて聞かれたときに、うまく答えられなかったのが心残りです。
[Reactive Programming with RxJava](http://shop.oreilly.com/product/0636920042228.do)を読んでいると、Observableはそもそも非同期で提供されるべきで、subscribeOnを使うケースはほとんどない、という前提があるようです。個人的には実行スレッドの制御を使い手に委ねる方が良いと思っているのですが、初めて触れるのなら、まずはobserveOnだけから始める方が良いのかもしれません。

## 次に向けて
Rxのオペレータを言葉やマーブルダイアグラムで理解するのは困難で、最終的には手を動かすしかありません。
一方、RxJavaとAndroidという組み合わせは準備に手間がかかりビルド時間もすぐとは言えません。

たとえばRxJSのようにすぐに実行できるような環境でtry-and-errorを繰り返せると良いのかなと思っています。

あと、準備はもう少し念入りにします（自戒）。
