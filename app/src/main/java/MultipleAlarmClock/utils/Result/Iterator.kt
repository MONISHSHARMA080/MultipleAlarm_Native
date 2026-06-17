package com.coolApps.MultipleAlarmClock.utils.Result

import kotlin.collections.Iterator


/**
 * Generic iterator that works with any data type
 * @param Input The type of the data object being iterated
 * @param Output The type of values being iterated (e.g., Long, Int, etc.)
 */
class GenericDataIterator<Input, Output : Comparable<Output>>(
	private val data: Input,
	private val startValue: Output,
	private val endValue: Output,
	private val incrementFunction: (Input, Output) -> Output,
	val hasNextFunction: ((Input, Output) -> Boolean )? = null
) : Iterator<Output> {

	private var currentValue: Output = startValue

	override fun hasNext(): Boolean {
		return if (hasNextFunction == null) currentValue <= endValue else hasNextFunction(data, currentValue)
	}

	/**@throws NoSuchElementException */
	override fun next(): Output {
		if (!hasNext()) throw NoSuchElementException()
		val result = currentValue
		currentValue = incrementFunction(data, currentValue)
		return result
	}
	fun peek(): Output = currentValue
	fun reset() {
		currentValue = startValue
	}
}

