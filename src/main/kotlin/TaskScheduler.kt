import kotlinx.coroutines.*
import java.util.concurrent.CountDownLatch

typealias Fn = () -> Unit

data class Task(val f: Fn, val timeoutMillis: Long)

data class TaskImpl(val task: Task, val scheduledAt: Long, val isInterval: Boolean) : Comparable<TaskImpl> {
    override fun compareTo(other: TaskImpl): Int {
        return this.scheduledAt.compareTo(other.scheduledAt)
    }
}

val NULL_TASK =
    TaskImpl(
        task = Task(f = {}, timeoutMillis = Long.MAX_VALUE),
        scheduledAt = Long.MIN_VALUE,
        isInterval = false
    )

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
            tasks.insert(
                TaskImpl(
                    Task(f = task, timeoutMillis = timeoutMillis),
                    scheduledAt = System.currentTimeMillis() + timeoutMillis,
                    isInterval = false
                )
            )
        }
    }

    fun scheduleInterval(task: Fn, timeoutMillis: Long) {
        synchronized(tasks) {
            tasks.insert(
                TaskImpl(
                    task = Task(f = task, timeoutMillis = timeoutMillis),
                    scheduledAt = System.currentTimeMillis() + timeoutMillis,
                    isInterval = true
                )
            )
        }
    }

    fun rescheduleInterval(task: Task) {
        scheduleInterval(task.f, task.timeoutMillis)
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
                task.task.f()
                if (task.isInterval) {
                    rescheduleInterval(task.task)
                }
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