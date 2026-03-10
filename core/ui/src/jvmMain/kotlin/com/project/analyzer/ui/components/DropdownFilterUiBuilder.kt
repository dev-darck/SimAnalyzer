package com.project.analyzer.ui.components

public fun buildDropdownFilterUi(
    label: String,
    selectedId: String,
    options: List<DropdownOptionUi>,
): DropdownFilterUi {
    val selectedLabel = options.firstOrNull { option -> option.id == selectedId }?.label
        ?: options.firstOrNull()?.label
        ?: label

    return DropdownFilterUi(
        label = label,
        selectedId = selectedId,
        selectedLabel = selectedLabel,
        options = options,
    )
}
