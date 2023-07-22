import org.junit.Test as test

class LambdaTest {
    fun ordinalFunction(f: () -> Unit) {
        f()
    }

    inline fun inlineFunction(f: () -> Unit) {
        f()
    }

    @test fun testLambda() {
        var s = 0
        listOf(1, 2, 3, 4, 5, 6).forEach { s += it }
        print(s)
    }

    @test fun testLambda01() {
        // Return an element whose length is the longest
        println(arrayOf("be", "is", "are", "was", "were").maxBy { it.length })
        println(arrayOf("be", "is", "are", "was", "were").maxBy { it -> it.length })
        println(arrayOf("be", "is", "are", "was", "were").maxBy { a -> a.length })
        println(arrayOf("be", "is", "are", "was", "were").maxBy { a: String -> a.length })
        println(arrayOf("be", "is", "are", "was", "were").maxBy (fun(a) = a.length))
        println(arrayOf("be", "is", "are", "was", "were").maxBy (fun(a): Int = a.length))
        println(arrayOf("be", "is", "are", "was", "were").maxBy (fun(a): Int {
            return a.length
        }))
        println(arrayOf("be", "is", "are", "was", "were").maxBy (fun(a: String): Int {
            return a.length
        }))
    }

    @test fun testLambda02() {
        ordinalFunction(fun() {
            println("calling fun")
            return
        })

        ordinalFunction {
            println("calling lambda")
            return@ordinalFunction
        }

        inlineFunction(fun() {
            println("calling fun in inline function")
            return
        })

        val f = {
            println("calling named lambda in inline function")
            return
        }

        inlineFunction {
            println("calling lambda in inline function")
            return
        }

        println("end")
    }
}