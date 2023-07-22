---
layout: post
title:  "順列を列挙するアルゴリズム"
date:   2018-04-18 05:26:53.000000000 -0700
categories: 
---
順列（英語だとpermutation）を列挙したいことがある。たとえば、$$ {\\{8, 1, 3\\}} $$という数字の並びがあったとして、その順列は3!=6通りあることは中学で習ったが、そのすべてを列挙したい場合はどうしたらいいだろうか。数がすくなければ手で列挙できるが、一般的な正の整数$$ {n} $$に対してそのようなアルゴリズムを構築することを考える。

ちょうどこの前のSRM 733 Div1 Easyで出てきたのと、いま読んでいる "The Algorithm Design Manual" でも出てきたので、ネットで調べた情報と合わせて書きのこしておく。

なお、この順列はコンピュータサイエンス（CS）ではたいへん重要で、あちらこちらで使うらしく、Knuthはその著 "The Art of Computer Algorithm" で、「Permsと省略したが、それによってCSの教科書は薄くなり安くなった」と冗談めいた脚注を残している。

<!-- more -->

以下では、$$ {n} $$個の順列、というときに$$ {\\{0, 1, \dots, n - 1\\}} $$というリストの順列のみを考える。任意の相異なる要素のリストの順列を考えるにはそれで十分であることは容易に分かるだろう。

# Rank/Unrank を使う方法

さて、それぞれの並びに数字を1対1対応させることができれば、その数字をインクリメントすることですべての順列は列挙できる。この数字を順列から生成することをRank、逆に数字から順列を生成することをUnrankと呼ぶ。

Rankの直観的な構成方法は、それぞれの順列を辞書順（Lexicographic Order）に並べて、その並び順に0始まりの数字を対応させる方法だ。

たとえば$$ {n=3} $$の場合、最初が0で始まる順列は2!=2通り、1で始まる順列も2通りなので、2始まりの最初の順列$$ {\\{2, 0, 1\\}} $$は5番目になる。そのつぎの$$ {\\{2, 1, 0\\}} $$が6番目で、これが最後になる。

これを、適当な数学っぽい感じで書くとつぎのようになる。なお、$$ {Rank(\mathbf{p})} $$は$$ {\mathbf{p}} $$の長さが1のときは0を返すとしておく。

$$ { \displaystyle
Rank(\mathbf{p}) = Order(\mathbf{p}, p_0) \dot (n - 1)! + Rank(\\{p_1, p_2, \dots, p_{n-1}\\}) \text{ if } n > 0 \text{, } 0\text{ otherwise, where } n = len(p)
} $$

ここで$$ {Order(\mathbf{p},p_0)} $$というのは$$ {\mathbf{p}} $$内で$$ {p_0} $$が何番目に大きいかを返す関数である。というのも、この再帰関数$$ {Rank} $$に渡ってくる$$ {\mathbf{p}} $$はところどころ歯抜けになっている可能性があり、それを考慮する必要があるからだ。

これをコードに落とすとこんな感じになる。

```java
public int rank(int[] p) {
  final int n = p.length;
  long fact = 1;
  for (int i = 2; i < n; i++) {
    fact *= i;
  }
  final List<Integer> digits = new LinkedList<>();
  for (int i = 0; i < n; i++) {
    digits.add(i);
  }
  int r = 0;
  for (int i = 0; i < p.length - 1; i++) {
    int q = digits.indexOf(p[i]);
    r += q * fact;
    digits.remove(q);
    fact /= (n - 1 - i);
  }
  return r;
}
```

一方、Unrankの方は単純で、$$ {i=n-1} $$から$$ {i=1} $$まで与えられたrankを$$ {i!} $$で割り、商をリストに足して余りをつぎのrankにする、というのを繰り返せばよい。

```java
public int[] unrank(long rank, int n) {
  long fact = 1;
  for (int i = 2; i < n; i++) {
    fact *= i;
  }
  final List<Integer> digits = new LinkedList<>();
  for (int i = 0; i < n; i++) {
    digits.add(i);
  }

  int[] ret = new int[n];
  for (int i = 0; i < n; i++) {
    int p = (int) (rank / fact);
    ret[i] = digits.get(p);
    digits.remove(p);
    rank %= fact;
    if (i < n - 1) fact /= (n - 1 - i);
  }
  return ret;
}
```

これらのアルゴリズムはすぐに分かると思うが$$ {O(n^2)} $$のオーダーになってしまう。
