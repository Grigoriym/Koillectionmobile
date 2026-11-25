package com.grappim.koillectionmobile.feature.logs.domain

import kotlinx.datetime.LocalDateTime

/**
 * One row of the user's history: something was created or deleted. [loggedAt] is the date and
 * time as the server wrote them, with no time zone conversion.
 */
data class LogEntry(
    val id: String,
    val action: LogAction,
    val objectType: LogObjectType,
    val objectLabel: String,
    val loggedAt: LocalDateTime
)

/** `LogTypeEnum` in Koillection 1.8.5. [Unknown] is any other value, or none. */
enum class LogAction { Create, Delete, Unknown }

/**
 * The 11 classes of `HistoryFilterEnum::FILTERS_CLASS` in Koillection 1.8.5. [Unknown] is any
 * other class.
 */
enum class LogObjectType {
    Collection,
    Item,
    Template,
    ChoiceList,
    Inventory,
    Tag,
    TagCategory,
    Album,
    Photo,
    Wishlist,
    Wish,
    Unknown
}

/**
 * One page of logs, newest first. [nextPath] is the page to read after this one, for
 * `LogsRepository.getLogs`. It is null after the oldest page.
 */
data class LogPage(val logs: List<LogEntry>, val nextPath: String?)
