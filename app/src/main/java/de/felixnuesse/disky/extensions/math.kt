package de.felixnuesse.disky.extensions

fun Any.divOrMin(a: Long, b: Long): Long {
    if (b != 0L) {
        return a.div(b)
    }

    return Long.MIN_VALUE
}