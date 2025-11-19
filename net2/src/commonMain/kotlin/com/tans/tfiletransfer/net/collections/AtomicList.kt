package com.tans.tfiletransfer.net.collections

import kotlinx.atomicfu.atomic

internal class AtomicList<T>(initial: List<T> = emptyList()) : MutableCollection<T> {
    private val ref = atomic(initial)
    val snapshot: List<T> get() = ref.value

    override fun add(element: T): Boolean {
        while (true) {
            val cur = ref.value
            val next = cur + element
            if (ref.compareAndSet(cur, next)) return true
        }
    }

    override fun remove(element: T): Boolean {
        while (true) {
            val cur = ref.value
            val idx = cur.indexOf(element)
            if (idx < 0) return false
            val next = cur.toMutableList().also { it.removeAt(idx) }.toList()
            if (ref.compareAndSet(cur, next)) return true
        }
    }

    override fun addAll(elements: Collection<T>): Boolean {
        while (true) {
            val cur = ref.value
            if (elements.isEmpty()) return false
            val next = cur.toMutableList().also { it.addAll(elements) }.toList()
            if (ref.compareAndSet(cur, next)) return true
        }
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        while (true) {
            val cur = ref.value
            val next = cur.filterNot { elements.contains(it) }
            if (next == cur) return false
            if (ref.compareAndSet(cur, next)) return true
        }
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        val keep = elements.toSet()
        while (true) {
            val cur = ref.value
            val next = cur.filter { keep.contains(it) }
            if (next == cur) return false
            if (ref.compareAndSet(cur, next)) return true
        }
    }

    override fun clear() {
        while (true) {
            val cur = ref.value
            if (cur.isEmpty()) return
            if (ref.compareAndSet(cur, emptyList())) return
        }
    }

    override val size: Int get() = ref.value.size
    override fun isEmpty(): Boolean = ref.value.isEmpty()
    override fun contains(element: T): Boolean = ref.value.contains(element)
    override fun containsAll(elements: Collection<T>): Boolean = elements.all { contains(it) }
    override fun iterator(): MutableIterator<T> {
        val list = snapshot
        var index = 0
        var last: T? = null
        return object : MutableIterator<T> {
            override fun hasNext(): Boolean = index < list.size
            override fun next(): T {
                val v = list[index]
                index += 1
                last = v
                return v
            }
            override fun remove() {
                val v = last ?: throw IllegalStateException()
                this@AtomicList.remove(v)
                last = null
            }
        }
    }
}