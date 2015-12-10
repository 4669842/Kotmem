package org.jire.kotmem

import com.sun.jna.Native
import com.sun.jna.platform.win32.Win32Exception
import org.jire.kotmem.unsafe.*
import java.nio.*
import java.util.*
import kotlin.reflect.KClass

class Process(val unsafe: UnsafeProcess) {

	val modules by lazy { HashSet<Module>().addAll(resolveModules(unsafe) as Collection<Module>) }

	private val modulesByName = HashMap<String, Module>()
	private val bufferCache = HashMap<KClass<*>, ByteBuffer>()

	fun bufferOf(type: KClass<*>, bytes: Int): ByteBuffer {
		var buf = bufferCache[type]
		if (buf == null) {
			buf = ByteBuffer.allocateDirect(bytes)
			bufferCache.put(type, buf)
		} else buf.clear()
		return buf!!.order(ByteOrder.nativeOrder())
	}

	operator inline fun <reified T : Any> get(address: Long) = lock {
		val type = T::class
		val dataType = dataTypeOf(type)
		val bytes = dataType.bytes
		val buf = bufferOf(type, bytes)
		if (!readProcessMemory(unsafe, address, buf, bytes))
			throw Win32Exception(Native.getLastError())
		buf.rewind()
		return dataType.read(buf)
	}

	operator inline fun <reified T : Any> get(address: Int): T = get<T>(address.toLong())

	operator inline fun <reified T : Any> set(address: Long, data: T) = lock {
		val type = T::class
		val dataType = dataTypeOf(type)
		val bytes = dataType.bytes
		val buf = bufferOf(type, bytes)
		dataType.write(buf, data)
		buf.flip()
		if (!writeProcessMemory(unsafe, address, buf, bytes))
			throw Win32Exception(Native.getLastError())
	}

	operator inline fun <reified T : Any> set(address: Int, data: T): Unit = set(address.toLong(), data)

	operator fun get(moduleName: String): Module {
		if (modulesByName.contains(moduleName)) return modulesByName[moduleName]!!
		val module = Module(this, resolveModule(unsafe, moduleName))
		modulesByName.put(moduleName, module)
		return module
	}

}