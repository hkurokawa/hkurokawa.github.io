---
layout: post
title:  "ChromeOS用SKK（chrome-skk）をWeb Storeに公開しました"
date:   2023-03-19 00:00:00.000000000 -0700
categories: chrome-skk
---

[先日の記事](https://hydrakecat.hatenablog.jp/entry/2022/08/07/ChromeOS%E7%94%A8SKK%E3%82%92%E5%85%AC%E9%96%8B%E3%81%97%E3%81%BE%E3%81%97%E3%81%9F)でManifest V3の対応に時間がかかるのでWeb Storeの公開は先になりそうですと書いたのですが、多少進展があってギリギリ使い物になるかなくらいになったので公開しました。

[https://chrome.google.com/webstore/detail/skk-japanese-input/gdfnmlnbnmgdliccidmiphhpicaecffj](https://chrome.google.com/webstore/detail/skk-japanese-input/gdfnmlnbnmgdliccidmiphhpicaecffj)

<!-- more -->

詳細は [https://github.com/hkurokawa/chrome-skk](https://github.com/hkurokawa/chrome-skk) の README を読んでほしいのですが、最新版には以下のような既知の問題があります。

* 30秒以上同じウィンドウで何も入力していないと SKK 自体が動かなくなる（直接入力しかできなくなる）
* タブやウィンドウを切り替えると直るが、直後にキーボードのキーを押下して文字が入力されるまで数秒のラグがある

これは回避策があって、Chrome のタブで Extension のオプションページを開き、開発者ツールを起動した状態で放置しておく、というものです。ハックなので、Chrome のバージョンが上がると動かなくなるかもしれませんが、とりあえず 112.0.5615.29 (Official Build) beta では動いています。

このハックが嫌な場合は [https://github.com/hkurokawa/chrome-skk/releases](https://github.com/hkurokawa/chrome-skk/releases) から v0.x 系の zip をダウンロードして手動でインストールしてください。今後も v0.x 系は Manifest V3 に移行しないで、重要度の高いバグ修正は入れてメンテナンスしていこうと考えています。

要望や問題がありましたらお気軽に [https://github.com/hkurokawa/chrome-skk/issues](https://github.com/hkurokawa/chrome-skk/issues) でご報告ください。