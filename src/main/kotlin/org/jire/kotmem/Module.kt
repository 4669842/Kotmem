package org.jire.kotmem

import org.jire.kotmem.unsafe.*

class Module(val process: Process, val unsafe: UnsafeModule) {

	val name by lazy { resolveModuleName(unsafe) }

	val address by lazy { resolveModuleAddress(unsafe) }

	operator inline fun <reified T> get(offset: Long) = process.get<T>(address + offset)

	operator inline fun <reified T> get(offset: Int): T = get(offset.toLong())

	operator inline fun <reified T> set(offset: Long, data: T) = process.set(address + offset, data)

	operator inline fun <reified T> set(offset: Int, data: T): Unit = set(offset.toLong(), data)

}