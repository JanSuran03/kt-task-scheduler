fun main(args: Array<String>) {
    testPriorityQueue()

    val scheduler = TaskScheduler()
    scheduler.schedule({ println("5s") }, 5000)
    scheduler.schedule({ println("2s") }, 2000)
    scheduler.schedule({ println("4s") }, 4000)
    scheduler.schedule({ println("1s") }, 1000)
    scheduler.schedule({ println("3s") }, 3000)
    scheduler.start()
    scheduler.waitBlocking()
}
