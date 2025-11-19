package com.tans.tfiletransfer.net.collections

import kotlinx.atomicfu.atomic

internal class AtomicSet<T>(initial: Set<T> = emptySet()) : MutableCollection<T> {
    private val ref = atomic(initial)
    val snapshot: Set<T> get() = ref.value

    override fun add(element: T): Boolean {
        while (true) {
            val cur = ref.value
            val next = if (cur.contains(element)) cur else cur + element
            if (next === cur) return false
            if (ref.compareAndSet(cur, next)) return true
        }
    }

    override fun remove(element: T): Boolean {
        while (true) {
            val cur = ref.value
            val next = cur - element
            if (cur === next) return false
            if (ref.compareAndSet(cur, next)) return true
        }
    }

    override fun addAll(elements: Collection<T>): Boolean {
        while (true) {
            val cur = ref.value
            val next = cur.toMutableSet().apply { addAll(elements) }.toSet()
            if (next == cur) return false
            if (ref.compareAndSet(cur, next)) return true
        }
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        while (true) {
            val cur = ref.value
            val next = cur.toMutableSet().apply { removeAll(elements) }.toSet()
            if (next == cur) return false
            if (ref.compareAndSet(cur, next)) return true
        }
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        val keep = elements.toSet()
        while (true) {
            val cur = ref.value
            val next = cur.filter { keep.contains(it) }.toSet()
            if (next == cur) return false
            if (ref.compareAndSet(cur, next)) return true
        }
    }

    override fun clear() {
        while (true) {
            val cur = ref.value
            if (cur.isEmpty()) return
            if (ref.compareAndSet(cur, emptySet())) return
        }
    }

    override val size: Int get() = ref.value.size
    override fun isEmpty(): Boolean = ref.value.isEmpty()
    override fun contains(element: T): Boolean = ref.value.contains(element)
    override fun containsAll(elements: Collection<T>): Boolean = elements.all { contains(it) }
    override fun iterator(): MutableIterator<T> {
        val it = snapshot.iterator()
        var last: T? = null
        return object : MutableIterator<T> {
            override fun hasNext(): Boolean = it.hasNext()
            override fun next(): T {
                val v = it.next()
                last = v
                return v
            }
            override fun remove() {
                val v = last ?: throw IllegalStateException()
                this@AtomicSet.remove(v)
                last = null
            }
        }
    }
}