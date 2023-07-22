---
layout: post
title:  "Picasso にパッチを当てた話"
date:  	2016-12-07 00:00:00.000000000 -0700
categories: 
---

以下は元々Fablic在籍時の2016-12-07に会社の技術ブログinFablicに投稿した記事（[http://in.fablic.co.jp/entry/2016/12/07/115501](http://in.fablic.co.jp/entry/2016/12/07/115501)）でした。inFablicが閉鎖されてしまったため、会社の了承を取った上で転載しております。

元記事にリンクを貼っていただいていた方に対しては大変お手数なのですが、こちらにリンクし直していただければ幸いです。

-----

こんにちは。Androidエンジニアの黒川([@hydrakecat](https://twitter.com/hydrakecat))です。

この記事は、[Fablic Advent Calendar 2016](http://qiita.com/advent-calendar/2016/fablic) のエントリーです。

本日は、Androidのライブラリにパッチを当てた話をしたいと思います。先日、[Picasso](https://github.com/square/picasso)という画像ライブラリのバグにより、弊社の[フリル](https://play.google.com/store/apps/details?id=jp.co.fablic.fril&hl=ja)で一部のユーザーさんに問題が発生しました。この記事では、その問題を解決するために、なぜパッチを当てることにしたのか、どのように行ったのか、どういう問題があったのか、という話をいたします。



<!-- more -->


## PRにするか、手元でパッチを当てるか

OSSのライブラリを使う際に、よく挙げられるメリットとして「いざとなったらソースコードを読んで修正できる」というものがあります。これは真なのですが、実際に修正をして、自分のプロダクトに適用しようとすると意外に大変です。

まず、レポジトリをフォークして、開発環境を整えなければなりません。次に、修正を作ります。最後にPRにして投げる、あるいは手元でパッチを当ててビルドをするかを選ばなければなりません。前者の場合は（自分がコントリビュータになれるという魅力は措いておいて）正式に採用されれば今後もメンテナンスされることが保証されています。また、オーナーを含む多くの人がレビューしてくれることで、品質もある程度保証されるでしょう。王道と言えます。一方で、手元でパッチを当てた場合は、たいてい本家に入った変更への追随が問題になります。乖離が発生する可能性もありますし、品質という意味でも不安が残ります。

では、どのようなときに手元でパッチを当てることになるでしょうか。それは、本家がいつまで経ってもリリースしてくれなさそうなときです。

Picassoは、最後のリリース2.5.2が行われたのが2015年3月で、すでに1半年以上も新しいバージョンが出ていません。開発が止まっているかというと、そんなことはなく、バージョン2.6.0として多くの変更が入っているのですが、まだ2.6.0のリリーススケジュールは決まっていないようです((Issue が立っていますが、「まだ終わっていない」とのことです [https://github.com/square/picasso/issues/1499](https://github.com/square/picasso/issues/1499)))。なお、幸い、該当の問題はすでに修正され2.6.0のブランチにマージされていたため、2.6.0では直ることがほぼ確実となっています。

そこで、私たちは、本家をフォークしたレポジトリで修正を作り、2.6.0がリリースされるまでは、こちらでビルドしたものを使うことにしました。差分は最小限にして正式リリースを待つ作戦です。

ちなみに、ここでは修正した問題の詳細については触れませんが、興味のある方は、[こちらのissue](https://github.com/square/picasso/issues/364)と[PR](https://github.com/square/picasso/pull/1035)をご覧ください。一言で言うと、一部の画像を読み込もうとすると `java.io.IOException: Cannot reset` が起きるというものです。

## 開発環境の構築

まず、レポジトリをフォークし、開発環境を構築します。フォークしたものが、[こちら](https://github.com/Fablic/picasso)になります。Picassoは2.5.2現在ではMavenビルドを採用しているため、Mavenベースのプロジェクトを作成する必要があります。しかしながら、Android Studioを使いつつMavenプロジェクトを同時に使うのは至難の技です。というか、1日格闘したけれど無理でした。ここでは2.6.0がGradle移行していたのに目を付け、[Gradle移行の変更](https://github.com/Fablic/picasso/pull/3)を引っ張ってくることでGradle移行させました。

自分がGradleに慣れているというのもありますが、Mavenに慣れていないならGradleにしておく方が余計な時間を取られることがないかもしれません。

## 修正のマージ

今回は修正は自分で作成したものではないため、内容を読み込み、理解しておく必要があります。なにか問題が発生したら自分で直さなければなりません。今回の修正は `MarkableInputStream` のみの修正であり、それほど難しい修正でもなかったので、この部分は難なく済みました。

UTが通ることを確認して、手元で問題が直っていることを確認したら完了です。

## ライブラリの利用方法の選択

修正したライブラリを利用するタイミングで2つの選択肢があります。1つは手元でビルドしてJARファイルなりAARファイルなりをGradleのファイル依存(File Dependency)として指定する方法。もう1つは何らかの形で社内もしくは社外に公開し、外部モジュール(External Module Dependency)として利用する方法です。

今回は、社内の他のプロジェクトでも利用するかもしれないこと、修正や履歴の管理が容易なこと、他に利用したい人がいるかもしれないこと、の3点からGitHubで公開して利用する形にしました。

## ライブラリの公開

次にプロジェクトをGradleの外部モジュールとして利用するために、公開する必要があります。いくつか方法はありますが、ここでは最も手軽な[JitPack](https://jitpack.io/)を利用します。

JitPackは、Gradleプラグインを設定すれば簡単にプロジェクトをライブラリとして公開できるサービスです。詳細な利用方法は他に譲りますが、手順としては以下だけで済みます。

1. [https://jitpack.io](https://jitpack.io) で対象のGitHubレポジトリのURLを入力する
2. [https://jitpack.io/docs/ANDROID/](https://jitpack.io/docs/ANDROID/) を参照しつつレポジトリの `build.gradle` を変更
3. Gitのタグを作成し、プッシュする

そして、ライブラリ利用側では、[https://jitpack.io/docs/ANDROID/](https://jitpack.io/docs/ANDROID/) に従って build.gradle に自分のライブラリを指定すれば完了です。今回は、以下のように build.gradle に書きます。

```
    dependencies {
	        compile 'com.github.Fablic.picasso:picasso:2.5.4'
	}
```

ここで、いくつかTipsがあります。

1つは、タグを作成する前の状態でライブラリを利用したい場合、タグではなくコミットハッシュを指定できるというものです。これはバージョンに相当する箇所（上記では `2.5.4`）にコミットハッシュを指定すれば良いのですが、JitPackページの **Commits** というところをクリックして、使いたいコミットの横にある **Get It** をクリックすることでも調べることができます。なお、 `-SNAPSHOT` を用いれば、常に最新のコミットのものが使われます。詳しくは[ドキュメント](https://jitpack.io/docs/#building-with-jitpack)を参照してください。

![JitPack Commits](https://cloud.githubusercontent.com/assets/6446183/20919265/3f2828de-bbde-11e6-8ad1-e9f5b6fea79d.png)

もう1つのTipは、プライベートレポジトリでもJitPackは利用可能ということです。こちらは試していないのですが、[ドキュメント](https://jitpack.io/docs/PRIVATE/)によればアクセストークンを使うことで可能となるようです。

## ビルドエラーの解消

以上で終わりでしょうか？運が良ければ、終わりですが、運が悪ければ、アプリのビルド時にエラーが出るかもしれません。

1つの可能性は、アプリが依存している別のライブラリがパッチを当てた元のライブラリに依存している場合です。つまり、パッチを当てた元のライブラリをP、パッチが当たったものをP' としたときに `アプリ -> ライブラリP'` だけでなく `アプリ -> ライブラリA -> ライブラリP` という依存が発生している場合です。

すでに見たように、ライブラリP'の Group ID はライブラリPとは異なります。従ってライブラリAの依存先としてP'だけでなく元のライブラリPもダウンロードされ、クラスの衝突が起こるのです。

私たちのケースでは、 `picasso2-okhttp3-downloader` が Picasso 2.5.3 に依存しているために発生しました。具体的には、以下のようなエラーが出ました。

```
Error:Execution failed for task ':app:transformClassesAndResourcesWithProguardForStagingDebug'.
> java.io.IOException: Can't write [/Users/hiroshi/workspaces/android/sample-project/app/build/intermediates/transforms/proguard/staging/debug/jars/3/1f/main.jar] (Can't read [/Users/hiroshi/.gradle/caches/modules-2/files-2.1/com.squareup.picasso/picasso/2.5.2/7446d06ec8d4f7ffcc53f1da37c95f200dcb9387/picasso-2.5.2.jar(;;;;;;**.class)] (Duplicate zip entry [picasso-2.5.2.jar:com/squareup/picasso/Action$RequestWeakReference.class]))
```

この場合、一番手っ取り早い解決策は、ライブラリAの依存からライブラリPを除いてしまうことです。以下のように `build.gradle` を書くことで実現できます。ただし、バージョンがそれほど乖離していないことは確認しましょう。下手をするとランタイムエラーが発生してしまいます。

```
    compile('com.jakewharton.picasso:picasso2-okhttp3-downloader:1.1.0') {
        exclude group: 'com.squareup.picasso', module: 'picasso'
    }
```

以上で、パッチを当てたライブラリが使えるようになります。自分のアプリで問題が直っていることを確認して、いざリリースをしましょう。

## おわりに

この記事では、Picassoを例に、OSSのライブラリにパッチを当てて運用するにはどうしたら良いか紹介しました。Picassoのようにリリース間隔が長くなってしまったライブラリの場合は、必要とする修正がなかなか取り込まれずヤキモキすることがあります。そのような場合は、必要に応じてフォークすることも選択肢の1つとなってきます。もちろん、コードベースがあまりに乖離するとメンテナンスに苦労するため、積極的に推奨できる方法ではありません。各自で行う場合はご注意ください。