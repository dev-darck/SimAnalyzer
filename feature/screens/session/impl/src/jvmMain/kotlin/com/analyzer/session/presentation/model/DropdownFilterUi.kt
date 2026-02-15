package com.analyzer.session.presentation.model

data class DropdownFilterUi(
    val label: String,
    val selectedId: String,
    val selectedLabel: String,
    val options: List<DropdownOptionUi>,
)
