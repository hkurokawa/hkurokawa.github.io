---
layout: post
title:  "レビューしてもらいやすいPRの書き方"
date:  	2017-10-05 00:00:00.000000000 -0700
categories: 
---

以下は元々Fablic在籍時の2017-10-05に会社の技術ブログinFablicに投稿した記事（http://in.fablic.co.jp/entry/2017/10/05/090000）でした。inFablicが閉鎖されてしまったため、会社の了承を取った上で転載しております。

元記事にリンクを貼っていただいていた方に対しては大変お手数なのですが、こちらにリンクし直していただければ幸いです。

-----

![ogp image](/assets/images/pr_review_guide_header.jpg)

こんにちは。Androidエンジニアの黒川（[@hydrakecat](https://twitter.com/hydrakecat)）です。

みなさん、Pull Request（PR）は書いているでしょうか？

PRを出したあとの1つの悩みに、なかなかレビューしてもらえないというもがあります。
長い間放置されて、ようやくレビューしてもらったときには、既に自分の変更内容を忘れていたり、ベースブランチとのコンフリクトが大量に起きていたり、とレビューが溜まるのは、レビュイーとレビュワー双方にとって良いことではありません。

レビュワーをランダムに指定したり、レビュータイムを設けたり、という工夫もありますが、PR自体の書き方を工夫することで、レビューしやすくすることも効果的です。


<!-- more -->


レビュワーはアサインされてPRを開いたタイミングで、時間がどれくらいかかりそうか見積もります。
そのときに、見積もりが難しそう、あるいは時間がかかりそうなほど、後回しにされる確率は高くなります。
逆にいえば、レビューに必要な情報が揃っており、レビューしやすく書かれたPRは、レビューにかかる時間が短そうと判断され、放置されることが少なくなるはずです。

この記事では、レビューしてもらいやすいPRを書くにはどうしたらよいか、という話をしたいと思います。
簡単のために、GitHubのPRを例にしますが、適宜自分の使っているコード管理ホスティングサービスに読み替えていただければ幸いです。

<!-- more -->

# レビューしやすいPR、レビューしにくいPR
レビューしてもらいやすいPRと、レビューしてもらいにくいPRの違いはなんでしょうか？

たとえば自分がよく知らないレポジトリのPRのレビューを依頼されたとします。送られてきたPRリンクを開いて、つぎのようなものだったらどうでしょうか？

![Bad example for PR description](/assets/images/pr_review_guide_bad_example.png)

どうでしょう。そっ閉じしたくならないでしょうか。すくなくとも、レビューを始める前にPR作者に話を聞きにいくことになるでしょう。

自分が考えるレビューしにくいPRの特徴はつぎのようなものです。

- なぜ変更しているか分からない
- 何を変更しているか分からない
- 差分が巨大
- 複数の目的が混ざっている（バグ修正、リファクタリング、コードフォーマットなど）
- 確認手順が不明

一方、レビューしやすいPRはつぎのようなものです。

![Good example for PR description](/assets/images/pr_review_guide_good_example.png)

これだったら、いますぐレビューできそうな気分になりますね。

経験的には、レビューしやすいPRはつぎのような特徴があります。

- 変更の目的が説明されている（対応するissueが書いてある）
- 変更内容が説明されている
- 差分が小さい
- 確認手順が書いてある

# レビューしやすいPRにするために出来ること
## 1. 変更の目的を書く
なぜその変更が必要なのか、きちんと説明しましょう。

たまに説明が空欄のPRや、変更内容は書いてあるけれど、背景がまったく書かれていないPRも見かけます。
レビュワーによってはすでに文脈が共有できていることもありますが、将来誰かがコードの変更履歴を追ってあなたのPRに辿りつくこともあり得ます。
GitHubの[How to write the perfect pull request](https://github.com/blog/1943-how-to-write-the-perfect-pull-request)には「歴史的な経緯を知っていることを前提にするな」と書いてありますが、文脈は容易に歴史の闇に飲み込まれるものです。
できるだけ前提知識がなくても理解できるように書きましょう。

なお、もし対応するissueがあるなら、言葉で説明するかわりに、そのissue番号を書くのでもよいです（そのissueに詳しく背景が書かれていれば、ですが）。

豆知識ですが、GitHubはPRの説明に "Resolves #&lt;issue番号&gt;" や "Closes #&lt;issue番号&gt;" と書くと、PRがマージされたタイミングで自動的に対応するissueも閉じてくれます([https://help.github.com/articles/closing-issues-using-keywords/](https://help.github.com/articles/closing-issues-using-keywords/))。
活用してみましょう。

## 2. 変更内容を書く
タイトルには簡潔に変更内容を書きましょう。略語の使用や内輪にしか通じない用語は避けたほうが無難です。

そして、どのような変更を行ったか、箇条書きでよいので書きます。

コミットメッセージやコードの差分を見れば変更内容は一目瞭然ということはありますが、それでもどんな変更が含まれているか自然言語で書いておく方がよいと筆者は考えます。
とくに、あとに述べる「差分を小さくする」という方針が守れない場合は、コミットの粒度を揃えるのと並んで、どんな変更が含まれるか書いておくことは、レビュワーの大きな助けになります。

特に議論したいポイントや予め知っておいてほしいこと、参考にしたブログ記事やスタックオーバーフローのリンクがあれば明示的に述べておくのも大事です。

1つのテクニックとして、自分でPRの差分にコメントを書くという方法もあります。
[How to write the perfect pull request](https://github.com/blog/1943-how-to-write-the-perfect-pull-request)には、特に見てもらいたい箇所に `:eyes:` 絵文字（👀 ）のコメントを付けるテクニックが紹介されています。
たとえば実装方法として迷いがあったり、レビュワーと相談したいことがあったら自分で先にコメントをしておくのは有用です。
ただし、コードのコメントに書くべきものをPRのコメントにするのは避けましょう。あくまでレビュワーとのコミュニケーションを助けるために活用することをおすすめします。

## 3. 変更によって期待される結果を書く
レビュワーがコードを把握する上で、正しい挙動はどのようなものか、を理解しておくことは大きな助けになります。

[9 Tips for Opening a Better Pull Request](https://www.mutuallyhuman.com/blog/2014/09/29/9-tips-for-opening-a-better-pull-request)では「レビュワーが期待すべきことを伝えろ」「レビュワーに再現手順を伝えろ」という言葉になっています。

たとえば、画面のレイアウトを変更したのなら、スクリーンショットを載せることで、どのように見えるべきなのかをレビュワーが把握できます。
あるいはAPIを追加したのなら、どういうリクエストにどういうレスポンスが返ってくるべきかを伝えることで、レビュワーは変更の理解が容易になります。
バグ修正であれば、バグの再現手順を書いておきましょう（きちんとissueを書いているのなら、そこに書かれているはずなので簡単ですね 😉 ）

すこしキツい言い方になりますが、この部分は、書くのが面倒ではあっても、書けないはずはありません。
期待される結果が書けないということは、PR作者が自分の実装が正しく動いていることを確認していない、あるいは、確認できないということを意味するからです。

ただ、たしかに容易に再現しない問題に対して予防的なコードを入れることはあります。
もし何らかの理由で結果を確認することが難しいのなら、それも一言書いておきましょう。

なお、仕様が他できちんと定義されているなら、この記述は不要です。
チームによっては別のサービスで仕様がきちんと管理されていることもあるでしょう。
その場合は仕様へのリンクがあれば十分です。

ここまでの1〜3はPRの説明文に関するものでした。これらを運用するときによい方法があります。
それは、[GitHubのPRレンプレート](https://github.com/blog/2111-issue-and-pull-request-templates)を活用することです。
説明に書かなければならない項目をテンプレートに含めることで、PR作者はそれらを埋めるだけで自動的にレビューしやすいPRが出来上がります。

## 4. 差分を小さく、目的を1つに絞る
レビュワーからすると、一般に差分が小さいほどレビューは容易になります。
経験的には、差分が大きくなると、二次関数的にレビューの困難さが増す気がします。

一方、どれくらいの差分が適切かというのは議論が分かれるところですし、コードフォーマットやメソッドのリネームのように差分が多くてもレビューが容易な変更もあります。
[10 tips for better Pull Requests](http://blog.ploeh.dk/2015/01/15/10-tips-for-better-pull-requests/)という記事には、変更しているファイル数が1ダース以下なら悪くはないと述べられています。

ただ、1つ言えることは、変更が大きいPRはしばしば、複数の目的を含んでしまっているということです。そして、複数の目的を含んだPRはレビューが困難になります。

たとえば、機能追加なのにバグ修正を含んでいたり、コードフォーマットが含まれているといった場合です。
あるいは、当初の目的を実現するために必要なリファクタリングも同じPRに含んでいる場合もあります。
これらの場合、多少面倒ですが、PRを複数に分けることで、1つ1つのPRを小さく、より単一の目的にフォーカスしたものにできます。

複数PRに分ける際に議論になるのが、PR間の依存です。たとえば機能awesome-featureを実現するためにリファクタリングbig-refactoringが必要だとします。
そのとき、big-refactoringを別PRにするとして、すぐにマージできればよいのですが、そうでない場合は機能awesome-featureの開発が滞ります。

これは人によって意見が分かれると思いますが、筆者はブランチbig-refactoringからブランチawesome-featureを生やすのが良いのではないかと思います。
レビューの際にはawesome-featureのPRのベースブランチをbig-refactoringにすることで、big-refactoringとawesome-featureの差分だけをレビューしてもらい、
big-refactoringがマージされた後にベースブランチに切り替えることで最終的な確認ができます。

```
        -------> awesome-feture
       /
    --+--------> big-refactoring
   /
--+------------> develop
```

このやり方の欠点の1つは、big-refactoringにあとから変更が入ると、その変更をawesome-featureが適宜取り込む必要があることです。
また、PRの依存が1つくらいなら苦にならないのですが、さらにminor-refactoring、micro-refactoringと依存が深くなると変更に追従するだけで大変になります。

```
                --> awesome-feture
               /
            --+---> micro-refactoring
           /
        --+-------> minor-refactoring
       /
    --+-----------> big-refactoring
   /
--+---------------> develop
```

基本的にレビューとマージのサイクルを早くすることで、PR間の依存が深くならないようにしましょう。

## 5. コミットの粒度とコミットメッセージを適切にする
これはPRレビューに限った話ではありませんし、巷間でも口うるさく言われることなので、ここでは詳細に述べません。
しかし、PRレビューの上でもコミットメッセージはとても有益な情報になります。

レビュワーの中には、差分だけでなくコミット単位でレビューする人もいます（筆者もそうです！）。
それによって、PR作者の変更の意図や何を考えていたかが分かり、より適切なレビューができるからです。

また、さきほど述べたように、1つのPRの差分はできるだけ小さくすべきですが、時によると、複数の目的を含んだ変更を1つのPRに入れざるを得ない場合もあります。

そのようなときは、レビュワーに謝罪（言い訳）しつつ、かわりにコミット単位でレビューしてもらうようにお願いしましょう。
たとえば、機能追加のPRにリファクタリングが混ざっていたとしても、それが独立しているコミットになっていればレビューは容易になります。
逆に、コミットメッセージにはリファクタリングと書きながら、こっそりバグ修正が含まれていたりすると、レビューは困難になります。

余力があれば、コミットをrebaseしてキレイにしておくこともおすすめです。
とくに実装中は足したコードをあとで削ったり、バグに気付いて修正するということはよくあります。
そのようなときに、適切にsquashすることで、レビュワーが無駄な労力を使うことを防ぐことができます。

# さいごに
いかがでしたでしょうか？

レビューしやすいPRを出すことで、レビューにかかる時間も縮まり、コード自体の質も上がりやすくなります。
逆にレビューしにくいPRは長い間放置された挙句にリリース直前に慌ててレビューすることもままあります。
そうすると、結果的にレビュワーとレビュイーの双方に負担がかかりますし、最悪の場合はノールックLGTMされてバグの温床になるかもしれません。

一方、PR作者の立場からすると、PRの説明に時間をかけるのは面倒に思えます。
すべての変更は自明でバグもないと自信たっぷりなこともあるでしょう。
余計なことをいわずに黙ってLGTMしろ、という気分のときもあるかもしれません。

しかし、たとえば[The (written) unwritten guide to pull requests](https://www.atlassian.com/blog/git/written-unwritten-guide-pull-requests)にあるように、
PRを商品、レビュワーを顧客と考えてみてはどうでしょうか。

顧客（レビュワー）がその商品（PR）を買う（承認する）ことを目標と考えたら、顧客の立場になって、商品の魅力を訴えようと思いますよね。
自分がレビュワーの立場になって考えれば、PRをより魅力的にすることは可能なはずです。
なぜなら、私たちも日常的にPRのレビューをしているからです。

ぜひ、自分のかわいい成果を売り込むことを考えて、レビューしやすいPRを書いてください。

最後になりましたが、ここに書いたことはあくまでも一般的な話です。
サービスのフェーズや、チームの成熟度、運用方法によって最適なPRの書き方も変わってくるでしょう。
もしかしたら、PRレビューそのものをなくすのが最善ということもあります。

筆者も上に挙げたことを全てのPRが守るべきとは思いませんし、依然として模索中の部分もあります。もっとこうした方がよいというフィードバックがあれば、ぜひ頂ければ幸いです。

# 参考文献
参考にした記事のリストです。
ここで述べたPRの書き方以外にも、レビュープロセスにおけるコミュニケーションを題材にしているのもあり、どれも興味深い記事でした。

- [The (written) unwritten guide to pull requests - Atlassian Blog](https://www.atlassian.com/blog/git/written-unwritten-guide-pull-requests)
- [How to write the perfect pull request](https://github.com/blog/1943-how-to-write-the-perfect-pull-request)
- [guides/code-review at master · thoughtbot/guides](https://github.com/thoughtbot/guides/tree/master/code-review)
- [10 tips for better Pull Requests](http://blog.ploeh.dk/2015/01/15/10-tips-for-better-pull-requests/)
- [9 Tips for Opening a Better Pull Request \| Mutually Human](https://www.mutuallyhuman.com/blog/2014/09/29/9-tips-for-opening-a-better-pull-request)
