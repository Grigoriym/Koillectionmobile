package com.grappim.koillectionmobile.feature.templates.domain

/** A named list of choices for a `choice-list` field. [choices] keeps the server's order. */
data class ChoiceList(val id: String, val name: String, val choices: List<String>)
