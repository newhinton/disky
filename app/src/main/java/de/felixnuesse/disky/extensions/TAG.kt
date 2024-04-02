package de.felixnuesse.disky.extensions

fun Any.tag(): String { return this::class.java.simpleName }