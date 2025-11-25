package com.tans.tfiletransfer.net.collections

import kotlinx.atomicfu.atomic

internal class AtomicList<T>(initial: List<T> = emptyList()) : MutableList<T> {
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

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        while (true) {
            val cur = ref.value
            if (elements.isEmpty()) return false
            val m = cur.toMutableList()
            m.addAll(index, elements)
            val next = m.toList()
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

    override fun get(index: Int): T = ref.value[index]

    override fun set(index: Int, element: T): T {
        while (true) {
            val cur = ref.value
            val old = cur[index]
            val m = cur.toMutableList()
            m[index] = element
            val next = m.toList()
            if (ref.compareAndSet(cur, next)) return old
        }
    }

    override fun add(index: Int, element: T) {
        while (true) {
            val cur = ref.value
            val m = cur.toMutableList()
            m.add(index, element)
            val next = m.toList()
            if (ref.compareAndSet(cur, next)) return
        }
    }

    override fun removeAt(index: Int): T {
        while (true) {
            val cur = ref.value
            val m = cur.toMutableList()
            val old = m.removeAt(index)
            val next = m.toList()
            if (ref.compareAndSet(cur, next)) return old
        }
    }

    override fun indexOf(element: T): Int = ref.value.indexOf(element)
    override fun lastIndexOf(element: T): Int = ref.value.lastIndexOf(element)

    override fun listIterator(): MutableListIterator<T> = listIterator(0)

    override fun listIterator(index: Int): MutableListIterator<T> {
        val list = snapshot
        var i = index
        var lastIndex = -1
        return object : MutableListIterator<T> {
            override fun hasNext(): Boolean = i < list.size
            override fun next(): T {
                val v = list[i]
                lastIndex = i
                i += 1
                return v
            }
            override fun hasPrevious(): Boolean = i > 0
            override fun previous(): T {
                i -= 1
                val v = list[i]
                lastIndex = i
                return v
            }
            override fun nextIndex(): Int = i
            override fun previousIndex(): Int = i - 1
            override fun remove() {
                if (lastIndex < 0) throw IllegalStateException()
                this@AtomicList.removeAt(lastIndex)
                lastIndex = -1
            }
            override fun set(element: T) {
                if (lastIndex < 0) throw IllegalStateException()
                this@AtomicList.set(lastIndex, element)
            }
            override fun add(element: T) {
                this@AtomicList.add(i, element)
                i += 1
                lastIndex = -1
            }
        }
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> = ref.value.subList(fromIndex, toIndex).toMutableList()
}