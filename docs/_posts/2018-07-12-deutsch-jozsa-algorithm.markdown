---
layout: post
title:  "量子コンピューティングのDeutsch–Jozsaアルゴリズムを追う"
date:   2018-07-12 06:57:43.000000000 -0700
categories: 
---

先日、[Microsoft Q# Coding Contest - Summer 2018](https://codeforces.com/contest/1002)というコーディングコンテンストに参加しました。
その中でWarm Upとして出された問題の1つがDeutsch–Jozsaアルゴリズムを実装するものだったのですが、なかなかおもしろいアルゴリズムだったので追ってみたメモです。
なお、筆者はQ#も量子コンピューティングもこのコンテストに参加するまで触ったことがない素人なので、勘違いなどもあるかと思います。
その際はご指摘いただければ幸いです。

<!-- more -->

# Deutsch–Jozsaアルゴリズムとは
簡単にいうと、与えられた関数[tex:{ f(x) }]の性質を判定するアルゴリズムです。より具体的には、[tex:{ f(x) }]が(i)常に0もしくは1を返すコンスタントな関数か、(ii)半分のxについては0を残り半分については1を返すバランスした関数か、を判定するアルゴリズムになります。

実用上どのようないいことがあるかははっきりしませんが、古典コンピュータでは多項式時間で解けない（であろう）が量子コンピュータなら多項式時間で解ける、とされており、量子コンピュータの優位性を示すアルゴリズムとなっているようです。

なお、以下の説明は[Wikipediaの記事](https://en.wikipedia.org/wiki/Deutsch%E2%80%93Jozsa_algorithm)と[DeutschとJozsaによる論文](http://www.math.zju.edu.cn:8080/wjd/notespapers/10.1.1.655.5997.pdf)に依拠しております。

# 前提知識
さて、ここから問題の詳細な定義とアルゴリズムの説明をするのですが、量子コンピューティングの用語を使います。もし量子コンピューティングをよく知らない方は以下のドキュメントを読むことをおすすめします。
とくにQuantum Computing ConceptsはQ#のドキュメントの一部ですが、説明が明快で分かりやすかったです。

最後にQ#を使ったコードも載せますが、Q#のことをそれほど知らなくても読めると思います。

- [Quantum Computing Concepts | Microsoft Docs](https://docs.microsoft.com/en-us/quantum/quantum-concepts-1-intro?view=qsharp-preview)
- [Introduction to Quantum Oracles - Codeforces](https://codeforces.com/blog/entry/60319)

# 解く問題
任意の[tex:{n}]ビットのQubit [tex:{ \lvert x\rangle ^ {\otimes n} = \lvert x\_0 \rangle \otimes \lvert x\_1 \rangle \otimes \ldots \otimes \lvert x_{n-1} \rangle}]を与えたら[tex:{1}]か[tex:{0}]を出力する関数[tex:{ f(x) }]を考えます。
その関数のオラクル[tex:{ U_f }]を次のように定義します：
$$
U_f ( \lvert x\rangle \otimes \lvert y\rangle ) = \lvert x\rangle \otimes ( \lvert y\rangle \oplus f(x) )
$$

ここで[tex:{ \lvert y\rangle }]は1ビットのqubit、[tex:{ \otimes }]はテンソル積（あるいはクロネッカー積）、[tex:{ \oplus }]はXORを表します。
つまり[tex:{ f(x) }]が[tex:{1}]なら[tex:{\lvert y\rangle}]を反転し、[tex:{0}]ならそのままです。

さて、このときに[tex:{ f(x) }]がコンスタントな関数かバランスした関数かどちらかであることが分かっているとします。つまり、(i)すべての[tex:{ \lvert x\rangle }]に対して[tex:{0}]もしくは[tex:{1}]を返すか、あるいは(ii)半々の確率で[tex:{0}]と[tex:{1}]を返すとします。
解きたい問題は、オラクル[tex:{ U_f }]が与えられたときに(i)か(ii)か判定せよ、という問題です。

# アルゴリズム
## 補題：[tex:{ \matrix{H}^{\otimes n} \lvert x\rangle^{\otimes n}}]の一般項
実際にアルゴリズムを見る前に[tex:{n}]ビットの[tex:{ \lvert x\rangle }]にアマダール変換[tex:{\matrix{H}}]を施した場合、どうなるか見ておきます。
あとでアルゴリズムを見る際に理解しやすくなるはずです。

まず1ビットのアマダール変換が次のようになることはご存知だと思います。

$$
\begin{align\*}
\matrix{H} \lvert 0 \rangle = \lvert + \rangle = \left( \lvert 0 \rangle + \lvert 1 \rangle \right) \\\\
\matrix{H} \lvert 1 \rangle = \lvert - \rangle = \left( \lvert 0 \rangle - \lvert 1 \rangle \right)
\end{align\*}
$$

したがって、[tex:{ \lvert x\rangle }]の各ビットにアマダール変換をした場合は次のようになります。

$$
\begin{align\*}
\matrix{H}^{\otimes n} \lvert x \rangle &= \matrix{H} \lvert x\_0 \rangle \otimes \matrix{H} \lvert x\_1 \rangle \otimes \ldots \otimes \matrix{H} \lvert x\_{n-1} \rangle \\\\
                                        &= \dfrac{1}{\sqrt{2}} \left( \lvert 0 \rangle + (-1)^{x\_0} \lvert 1 \rangle \right) \otimes \ldots \otimes \dfrac{1}{\sqrt{2}} \left( \lvert 0 \rangle + (-1)^{x\_{n-1}} \lvert 1 \rangle \right) \\\\
                                        &= \dfrac{1}{\sqrt{2\^n}} \sum\_{y=0}\^{2\^n-1} \left(-1\right)\^{x\cdot y} \lvert y \rangle
\end{align\*}
$$

ここで[tex:{ x \cdot y = \sum\_{i=0}^{n-1} x\_i y\_i}]とします。すなわち[tex:{x}]と[tex:{y}]の各ビットの積を足し合わせたものになります。

## Deutsch-Jozsaアルゴリズム
それでは本題のアルゴリズムをみてみましょう。全体の流れは次のようになります。

1. [tex:{\lvert 0 \rangle}]に初期化した[tex:{n}]ビットの[tex:{x}]と、[tex:{\lvert 1 \rangle}]に初期化した[tex:{1}]ビットの[tex:{y}]を用意する
2. それぞれに[tex:{\matrix{H}^{\otimes n}}]と[tex:{\matrix{H}}]を作用させる
3. [tex:{U_f}]に入力として与える
4. [tex:{x}]と[tex:{y}]に対して[tex:{\matrix{H}^{\otimes n}}]を作用させる
5. [tex:{x}]を測定し、[tex:{\lvert 0 \rangle}]だったらコンスタントな関数、そうでなかったらバランスした関数と分かる

実際に数式で確認してみましょう。まず、[tex:{\lvert 0 \rangle}]の各ビットにアマダール変換[tex:{\matrix{H}}]を施すと、さきほどの補題から次のようになります。

$$
\begin{align}
\matrix{H}^{\otimes n} \lvert 0 \rangle &= \dfrac{1}{\sqrt{2\^n}} \sum\_{k=0}\^{2\^n-1} \left(-1\right)\^{0\cdot k} \lvert k \rangle \\\\
                                        &= \dfrac{1}{\sqrt{2\^n}} \sum\_{k=0}\^{2\^n-1} \lvert k \rangle
\end{align}
$$

ここで、[tex:{0\cdot k = 0 }]であることを使いました。

つぎにこの[tex:{\dfrac{1}{\sqrt{2\^n}} \sum\_{k=0}\^{2\^n-1} \lvert k \rangle}]と[tex:{\lvert - \rangle}]をそれぞれ[tex:{x}]、[tex:{y}]として[tex:{U_f}]に与えます。

$$
\begin{align}
U_f \left( \lvert x \rangle \otimes \lvert y \rangle  \right)
     &= U_f \left( \dfrac{1}{\sqrt{2\^n}} \sum\_{k=0}\^{2\^n-1} \lvert k \rangle \otimes \lvert - \rangle \right) \\\\
     &= \dfrac{1}{\sqrt{2\^n}} \sum\_{k=0}\^{2\^n-1} \lvert k \rangle \otimes \dfrac{1}{\sqrt{2}} \left( \left( \lvert 0 \rangle - \lvert 1 \rangle \right) \oplus f(x) \right)
\end{align}
$$

ここで、[tex:{f(x)}]が[tex:{0}]もしくは[tex:{1}]しか取らないことを思い出して、
[tex:{\left( \lvert 0 \rangle - \lvert 1 \rangle \right) \oplus f(x)}]がそれぞれの場合にどうなるか考えると、つぎのようになります。

$$
\begin{align}
U_f \left( \lvert x \rangle \otimes \lvert y \rangle  \right)
     &= \dfrac{1}{\sqrt{2\^n}} \sum\_{k=0}\^{2\^n-1} \lvert k \rangle \otimes \dfrac{1}{\sqrt{2}} \left( \left( \lvert 0 \rangle - \lvert 1 \rangle \right) \oplus f(x) \right) \\\\
     &= \dfrac{1}{\sqrt{2\^n}} \sum\_{k=0}\^{2\^n-1} \lvert k \rangle \otimes (-1)\^{f(x)} \dfrac{1}{\sqrt{2}} \left( \lvert 0 \rangle - \lvert 1 \rangle \right) \\\\
     &= \dfrac{1}{\sqrt{2\^n}} \sum\_{k=0}\^{2\^n-1} \lvert k \rangle \otimes (-1)\^{f(x)} \lvert - \rangle \\\\
     &= \dfrac{1}{\sqrt{2\^n}} \sum\_{k=0}\^{2\^n-1} (-1)\^{f(k)} \lvert k \rangle \otimes \lvert - \rangle
\end{align}
$$

最後の式の変形は意外に思えるかもしれませんが、[tex:{\sum\_{k=0}\^{2\^n-1} \lvert k \rangle}]の部分を展開して、
[tex:{\lvert x \rangle}]がそれぞれの状態の重ね合わせであることを考えると納得がいくのではないかと思います。

なお、ここまでは誤解を防ぐために[tex:{k}]を使いましたが、以降では[tex:{x}]に置きかえます。すなわち、

$$
U_f \left( \lvert x \rangle \otimes \lvert y \rangle  \right) = \dfrac{1}{\sqrt{2\^n}} \sum\_{x=0}\^{2\^n-1} (-1)\^{f(x)} \lvert x \rangle \otimes \lvert - \rangle
$$

となります。

さて、ここまでがステップ3です。ここからさらに全体に対してアマダール変換を再度施します。補題を思い出して機械的に変形するとつぎのようになります。

$$
\begin{align}
\matrix{H}\^{\otimes n + 1} U_f \left( \lvert x \rangle \otimes \lvert y \rangle  \right)
     &= \left( \matrix{H}\^{\otimes n} \otimes \matrix{H} \right) \left( \dfrac{1}{\sqrt{2\^n}} \sum\_{x=0}\^{2\^n-1} (-1)\^{f(x)} \lvert x \rangle \otimes \lvert - \rangle \right) \\\\
     &= \dfrac{1}{\sqrt{2\^n}} \sum\_{x=0}\^{2\^n-1} (-1)\^{f(x)} \dfrac{1}{\sqrt{2\^n}} \sum\_{k=0}\^{2\^n-1} (-1)\^{x \cdot k} \lvert k \rangle \otimes \lvert 1 \rangle \\\\
     &= \dfrac{1}{2\^n} \sum\_{x=0}\^{2\^n-1} (-1)\^{f(x)} \sum\_{k=0}\^{2\^n-1} (-1)\^{x \cdot k} \lvert k \rangle \otimes \lvert 1 \rangle
\end{align}
$$

ここで、[tex:{k}]を[tex:{y}]に置き換えます。また、最後の1ビットは無視して先頭の[tex:{n}]ビットだけに注目すると、つぎのようになります。

$$
\begin{align}
\dfrac{1}{2\^n} \sum\_{x=0}\^{2\^n-1} (-1)\^{f(x)} \sum\_{y=0}\^{2\^n-1} (-1)\^{x \cdot y} \lvert y \rangle
= \sum\_{y=0}\^{2\^n-1} \left( \dfrac{1}{2\^n} \sum\_{x=0}\^{2\^n-1} (-1)\^{f(x)} (-1)\^{x \cdot y} \right) \lvert y \rangle
\end{align}
$$

つまり、[tex:{0}]から[tex:{2\^n-1}]までの[tex:{y}]の重ね合わせになっており、それぞれの係数が[tex:{\dfrac{1}{2\^n} \sum\_{x=0}\^{2\^n-1} (-1)\^{f(x)} (-1)\^{x \cdot y}}]となっている状態になります。

このとき、この[tex:{n}]ビットを測定して[tex:{\lvert 0 \rangle \^{\otimes n}}]が得られる確率がいくつになるか考えてみましょう。
[tex:{y}]が[tex:{0}]の場合の係数の二乗を考えればいいので次のようになります。

$$
\begin{align}
\left| \dfrac{1}{2\^n} \sum\_{x=0}\^{2\^n-1} (-1)\^{f(x)} (-1)\^{x \cdot 0} \right|^{2}
    &= \left| \dfrac{1}{2\^n} \sum\_{x=0}\^{2\^n-1} (-1)\^{f(x)} \right|^{2}
\end{align}
$$

ここで、[tex:{f(x)}]がコンスタントな関数であれば、値が[tex:{1}]になることがわかります。
一方、もしバランスした関数であれば半分の[tex:{x}]については[tex:{0}]を返し、残りの[tex:{x}]については[tex:{1}]を返すので和をとると打ち消しあって[tex:{0}]になることがわかります。

以上でアルゴリズムの解説は終了です。

最後にこのアルゴリズムをQ#で実装してみます。

```
namespace Solution
{
    open Microsoft.Quantum.Canon;
    open Microsoft.Quantum.Primitive;

    operation DeutschJozsa (N : Int, Uf : ((Qubit[], Qubit) => ())) : Bool
    {
        body
        {
            mutable isConstant = true;
            using(qs = Qubit[N + 1])
            {
                let x = qs[0..N-1];
                let y = qs[N];

                // ステップ1：yを|1>に初期化
                X(y);
                
                // ステップ2：xとyにアマダール変換を施す
                ApplyToEach(H, qs);

                // ステップ3：Ufを作用させる
                Uf(x, y);

                // ステップ4：xとyにアマダール変換を施す
                ApplyToEach(H, qs);

                // ステップ5：xが|0>であるかどうか調べる
                for(i in 0..N-1)
                {
                    if M(x[i]) == One
                    {
                        set isConstant = false;
                    }
                }
                
                // 最後にすべてを|0>に初期化しないとエラーになる
                ResetAll(qs);
            }
            return isConstant;
        }
    }
}
```

# 感想
同様の問題を古典コンピュータで決定的に解くことを考えると、[tex:{2^{n-1} + 1}]回[tex:{f(x)}]を実行しなければならない（バランスかどうかを調べるにはすべてケースの半分+1を調べることが必要）ことになります。
一方、Deutsch–Jozsaアルゴリズムは多項式時間しかかかりません。たとえ古典コンピュータで確率的に解いたとしても量子コンピュータの方が高速であることが論文[^1]で示されています。

ということで、めでたしめでたしなのですが、個人的にはなぜ量子コンピュータが古典コンピュータより高速なのか、ということが未だに腑に落ちていません。
よく使われる説明は「すべての組み合わせを重ね合わせ状態で表せるか」というものなのですが、森前先生のドキュメント[^2]にもあるように、これは不正確な言説のようです。

おそらく量子計算理論をきちんと勉強しないと理解できなさそうなので、もし分かったら追記します。

[^1] "Rapid solution of problems by quantum computation", David Deutsch1, Richard Jozsa, Proc. R. Soc. Lond. A (1992) 439, 553-558
[^2] http://tomoyukimorimae.web.fc2.com/nigate.pdf