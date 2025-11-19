package com.tans.tfiletransfer.net.collections

import kotlinx.atomicfu.atomic

internal class AtomicMap<K, V>(initial: Map<K, V> = emptyMap()) : MutableMap<K, V> {
    private val ref = atomic(initial)
    val snapshot: Map<K, V> get() = ref.value

    override val size: Int get() = ref.value.size
    override fun isEmpty(): Boolean = ref.value.isEmpty()
    override fun containsKey(key: K): Boolean = ref.value.containsKey(key)
    override fun containsValue(value: V): Boolean = ref.value.containsValue(value)
    override fun get(key: K): V? = ref.value[key]

    override fun put(key: K, value: V): V? {
        while (true) {
            val cur = ref.value
            val old = cur[key]
            val next = cur.toMutableMap().also { it[key] = value }.toMap()
            if (ref.compareAndSet(cur, next)) return old
        }
    }

    override fun remove(key: K): V? {
        while (true) {
            val cur = ref.value
            val old = cur[key] ?: return null
            val next = cur.toMutableMap().also { it.remove(key) }.toMap()
            if (ref.compareAndSet(cur, next)) return old
        }
    }

    override fun putAll(from: Map<out K, V>) {
        if (from.isEmpty()) return
        while (true) {
            val cur = ref.value
            val m = cur.toMutableMap()
            for ((k, v) in from) m[k] = v
            val next = m.toMap()
            if (ref.compareAndSet(cur, next)) return
        }
    }

    override fun clear() {
        while (true) {
            val cur = ref.value
            if (cur.isEmpty()) return
            if (ref.compareAndSet(cur, emptyMap())) return
        }
    }

    override val keys: MutableSet<K> = object : MutableSet<K> {
        override val size: Int get() = ref.value.size
        override fun isEmpty(): Boolean = ref.value.isEmpty()
        override fun contains(element: K): Boolean = ref.value.containsKey(element)
        override fun containsAll(elements: Collection<K>): Boolean = elements.all { contains(it) }
        override fun add(element: K): Boolean { throw UnsupportedOperationException() }
        override fun addAll(elements: Collection<K>): Boolean { throw UnsupportedOperationException() }
        override fun clear() { this@AtomicMap.clear() }
        override fun remove(element: K): Boolean = this@AtomicMap.remove(element) != null
        override fun removeAll(elements: Collection<K>): Boolean {
            var changed = false
            for (e in elements) changed = this@AtomicMap.remove(e) != null || changed
            return changed
        }
        override fun retainAll(elements: Collection<K>): Boolean {
            val keep = elements.toSet()
            var changed = false
            for (k in snapshot.keys) {
                if (!keep.contains(k)) changed = this@AtomicMap.remove(k) != null || changed
            }
            return changed
        }
        override fun iterator(): MutableIterator<K> {
            val it = snapshot.keys.iterator()
            var last: K? = null
            return object : MutableIterator<K> {
                override fun hasNext(): Boolean = it.hasNext()
                override fun next(): K {
                    val v = it.next()
                    last = v
                    return v
                }
                override fun remove() {
                    val k = last ?: throw IllegalStateException()
                    this@AtomicMap.remove(k)
                    last = null
                }
            }
        }
    }

    override val values: MutableCollection<V> = object : MutableCollection<V> {
        override val size: Int get() = ref.value.size
        override fun isEmpty(): Boolean = ref.value.isEmpty()
        override fun contains(element: V): Boolean = ref.value.containsValue(element)
        override fun containsAll(elements: Collection<V>): Boolean = elements.all { contains(it) }
        override fun add(element: V): Boolean { throw UnsupportedOperationException() }
        override fun addAll(elements: Collection<V>): Boolean { throw UnsupportedOperationException() }
        override fun clear() { this@AtomicMap.clear() }
        override fun remove(element: V): Boolean {
            while (true) {
                val cur = ref.value
                val target = cur.entries.firstOrNull { it.value == element } ?: return false
                val next = cur.toMutableMap().also { it.remove(target.key) }.toMap()
                if (ref.compareAndSet(cur, next)) return true
            }
        }
        override fun removeAll(elements: Collection<V>): Boolean {
            var changed = false
            for (e in elements) changed = remove(e) || changed
            return changed
        }
        override fun retainAll(elements: Collection<V>): Boolean {
            val keep = elements.toSet()
            var changed = false
            while (true) {
                val cur = ref.value
                val toRemove = cur.entries.filter { !keep.contains(it.value) }
                if (toRemove.isEmpty()) return changed
                val next = cur.toMutableMap().also { for (e in toRemove) it.remove(e.key) }.toMap()
                if (ref.compareAndSet(cur, next)) changed = true
            }
        }
        override fun iterator(): MutableIterator<V> {
            val it = snapshot.values.iterator()
            var last: V? = null
            return object : MutableIterator<V> {
                override fun hasNext(): Boolean = it.hasNext()
                override fun next(): V {
                    val v = it.next()
                    last = v
                    return v
                }
                override fun remove() {
                    val v = last ?: throw IllegalStateException()
                    this@AtomicMap.values.remove(v)
                    last = null
                }
            }
        }
    }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> = object : MutableSet<MutableMap.MutableEntry<K, V>> {
        override val size: Int get() = ref.value.size
        override fun isEmpty(): Boolean = ref.value.isEmpty()
        override fun contains(element: MutableMap.MutableEntry<K, V>): Boolean {
            val cur = ref.value
            val v = cur[element.key] ?: return false
            return v == element.value
        }
        override fun containsAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean = elements.all { contains(it) }
        override fun add(element: MutableMap.MutableEntry<K, V>): Boolean { throw UnsupportedOperationException() }
        override fun addAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean { throw UnsupportedOperationException() }
        override fun clear() { this@AtomicMap.clear() }
        override fun remove(element: MutableMap.MutableEntry<K, V>): Boolean {
            while (true) {
                val cur = ref.value
                val v = cur[element.key] ?: return false
                if (v != element.value) return false
                val next = cur.toMutableMap().also { it.remove(element.key) }.toMap()
                if (ref.compareAndSet(cur, next)) return true
            }
        }
        override fun removeAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean {
            var changed = false
            for (e in elements) changed = remove(e) || changed
            return changed
        }
        override fun retainAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean {
            val keep = elements.associateBy { it.key }
            var changed = false
            while (true) {
                val cur = ref.value
                val toRemove = cur.entries.filter { e ->
                    val k = e.key
                    val ke = keep[k]
                    ke == null || ke.value != e.value
                }
                if (toRemove.isEmpty()) return changed
                val next = cur.toMutableMap().also { for (e in toRemove) it.remove(e.key) }.toMap()
                if (ref.compareAndSet(cur, next)) changed = true
            }
        }
        override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> {
            val it = snapshot.entries.iterator()
            var last: MutableMap.MutableEntry<K, V>? = null
            return object : MutableIterator<MutableMap.MutableEntry<K, V>> {
                override fun hasNext(): Boolean = it.hasNext()
                override fun next(): MutableMap.MutableEntry<K, V> {
                    val e = it.next()
                    val entry = object : MutableMap.MutableEntry<K, V> {
                        override val key: K get() = e.key
                        override val value: V get() = ref.value[key] ?: e.value
                        override fun setValue(newValue: V): V = this@AtomicMap.put(key, newValue) ?: e.value
                        override fun equals(other: Any?): Boolean {
                            if (other !is MutableMap.MutableEntry<*, *>) return false
                            return key == other.key && value == other.value
                        }
                        override fun hashCode(): Int = key.hashCode() xor value.hashCode()
                    }
                    last = entry
                    return entry
                }
                override fun remove() {
                    val e = last ?: throw IllegalStateException()
                    this@AtomicMap.remove(e.key)
                    last = null
                }
            }
        }
    }
}