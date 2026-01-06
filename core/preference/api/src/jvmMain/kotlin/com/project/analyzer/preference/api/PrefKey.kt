package com.project.analyzer.preference.api

public sealed interface Key<T>

@JvmInline
public value class StringPrefKey(public val name: String) : Key<String>

@JvmInline
public value class IntPrefKey(public val name: String) : Key<Int>

@JvmInline
public value class LongPrefKey(public val name: String) : Key<Long>

@JvmInline
public value class FloatPrefKey(public val name: String) : Key<Float>

@JvmInline
public value class DoublePrefKey(public val name: String) : Key<Double>

@JvmInline
public value class BooleanPrefKey(public val name: String) : Key<Boolean>

@JvmInline
public value class StringSetPrefKey(public val name: String) : Key<Set<String>>

public val String.str: StringPrefKey get() = StringPrefKey(this)
public val String.int: IntPrefKey get() = IntPrefKey(this)
public val String.long: LongPrefKey get() = LongPrefKey(this)
public val String.float: FloatPrefKey get() = FloatPrefKey(this)
public val String.double: DoublePrefKey get() = DoublePrefKey(this)
public val String.bool: BooleanPrefKey get() = BooleanPrefKey(this)
public val String.strSet: StringSetPrefKey get() = StringSetPrefKey(this)
