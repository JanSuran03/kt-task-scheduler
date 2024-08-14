import kotlinx.coroutines.*
import java.util.concurrent.CountDownLatch

typealias Fn = () -> Unit

data class Task(val f: Fn, val scheduledAt: Long) : Comparable<Task> {
    override fun compareTo(other: Task): Int {
        return this.scheduledAt.compareTo(other.scheduledAt)
    }
}

val NULL_TASK = Task(scheduledAt = Long.MIN_VALUE, f = {})

class TaskScheduler {
    private var tasks = PriorityQueue(NULL_TASK)
    private var currentJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val latch = CountDownLatch(1)
    private var hanging: Boolean = false

    fun start() {
        currentJob = scope.launch {
            while (isActive) {
                processTasks()
            }
            latch.countDown()
        }
    }

    fun schedule(task: Fn, timeoutMillis: Long) {
        synchronized(tasks) {
            tasks.insert(Task(task, System.currentTimeMillis() + timeoutMillis))
        }
    }

    fun stop() {
        synchronized(tasks) {
            tasks.clear()
        }
        currentJob?.cancel()
        latch.countDown()
    }

    private suspend fun processTasks() {
        while (currentJob!!.isActive) {
            val delayMillis =
                synchronized(tasks) {
                    if (tasks.isEmpty()) {
                        if (hanging) {
                            latch.countDown()
                            null
                        } else {
                            Long.MAX_VALUE
                        }
                    } else {
                        val now = System.currentTimeMillis()
                        val task = tasks.getMin()
                        if (task.scheduledAt <= now) {
                            null
                        } else {
                            task.scheduledAt - now
                        }
                    }
                }
            if (delayMillis != null && delayMillis > 0) {
                withContext(Dispatchers.IO) {
                    try {
                        delay(delayMillis)
                    } catch (e: CancellationException) {

                    }
                }
            } else {
                val task = tasks.extractMin()
                task.f()
            }
        }
    }

    fun waitBlocking() {
        synchronized(tasks) {
            if (tasks.isEmpty()) {
                currentJob?.cancel()
                return
            }
            hanging = true
        }
        hanging = true
        latch.await()
        stop()
    }
}