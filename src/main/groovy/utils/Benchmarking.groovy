package utils

class Benchmarking {
    def what, startTime, endTime

    Benchmarking(what) { this.what = what }

    def start() { startTime = System.nanoTime() }

    def stop() { endTime = System.nanoTime() }

    def print() {
        def diff = (endTime - startTime)
        def secDiff = Math.floor(diff / 1000000000L).toLong()
        def msDiff = Math.floor((diff - (secDiff * 1000000000L)) / 1000000L).toLong()
        def nsDiff = diff - ((secDiff * 1000000000) + (msDiff * 1000000L))

        println "$what takes ${((secDiff != 0) ? "${secDiff} sec " : "")}${((msDiff != 0) ? "${msDiff} ms " : "")}${nsDiff} ns"
    }
}