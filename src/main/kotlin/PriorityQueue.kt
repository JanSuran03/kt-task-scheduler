import java.lang.UnsupportedOperationException
import kotlin.random.Random

class PriorityQueue<T : Comparable<T>>(nullElement: T) {
    private var data: ArrayList<T> = arrayListOf(nullElement)

    fun getMin(): T {
        if (data.size > 1) {
            return data[1]
        } else {
            throw UnsupportedOperationException("Priority queue is empty")
        }
    }

    private fun bubbleDown(index: Int) {
        var cur = index
        while (2 * cur < data.size) {
            var child = 2 * cur
            if (child < data.size - 1 && data[child + 1] < data[child]) {
                child++
            }
            if (data[child] < data[cur]) {
                val tmp = data[child]
                data[child] = data[cur]
                data[cur] = tmp
                cur = child
            } else {
                break
            }
        }
    }

    private fun bubbleUp(index: Int) {
        var cur = index
        while (cur > 1) {
            val parent = cur shr 1
            if (data[cur] < data[parent]) {
                val tmp = data[parent]
                data[parent] = data[cur]
                data[cur] = tmp
                cur = parent
            } else {
                break
            }
        }
    }

    fun clear() {
        val initElement = data[0]
        data.clear()
        data.add(initElement)
    }

    fun buildFromArray(initData: Iterable<T>) {
        clear()
        data.addAll(initData)
        for (index in (data.size - 1) shr 1 downTo 1) {
            bubbleDown(index)
        }
    }

    fun extractMin(): T {
        if (data.size == 1) {
            throw UnsupportedOperationException("Priority queue is empty")
        } else {
            val ret = data[1]
            data[1] = data.last()
            data.removeLast()
            bubbleDown(1)
            return ret
        }
    }

    fun insert(x: T) {
        data.add(x)
        bubbleUp(data.size - 1)
    }

    fun isNotEmpty() = data.size > 1
    fun isEmpty() = !isNotEmpty()
}

fun testPriorityQueue() {
    for (size in 1..30) {
        for (i in 1..3) {
            val lst: ArrayList<Int> = arrayListOf()
            for (j in 1..size) {
                lst.add(Random.nextInt(100))
            }
            val q = PriorityQueue(0)
            q.buildFromArray(lst)
            val res: ArrayList<Int> = arrayListOf()
            while (q.isNotEmpty()) {
                res.add(q.extractMin())
            }
            for (j in 0..<(res.size - 1)) {
                assert(res[j] <= res[j + 1])
            }
        }
        for (i in 1..3) {
            val q = PriorityQueue(0)
            q.insert(Random.nextInt(100))
            val res: ArrayList<Int> = arrayListOf()
            while (q.isNotEmpty()) {
                res.add(q.extractMin())
            }
            for (j in 0..<(res.size - 1)) {
                assert(res[j] <= res[j + 1])
            }
        }
    }
}
