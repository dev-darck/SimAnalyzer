package com.project.analyzer.ui.modifier

@JvmInline
public value class UiTestTag(public val value: String) {

    public fun child(vararg parts: Any?): UiTestTag = uiTestTagOf(this, *parts)
}
