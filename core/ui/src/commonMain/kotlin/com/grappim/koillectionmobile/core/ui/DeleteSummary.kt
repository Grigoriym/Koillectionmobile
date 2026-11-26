package com.grappim.koillectionmobile.core.ui

/**
 * What the server deletes with a nested container (a collection, a wishlist or an album):
 * [childCount] containers inside it (every level), and [contentCount] items, wishes or photos in it
 * and in those containers.
 */
data class DeleteSummary(val childCount: Int, val contentCount: Int)

/**
 * The ids of every container under [rootId], at every level. [all] holds each container as its
 * `(id, parentId)` pair. [rootId] itself is not in the result.
 */
fun descendantIds(rootId: String, all: List<Pair<String, String?>>): List<String> {
    val childrenByParent = all.groupBy({ it.second }, { it.first })
    val found = mutableListOf<String>()
    val queue = ArrayDeque(listOf(rootId))
    while (queue.isNotEmpty()) {
        childrenByParent[queue.removeFirst()].orEmpty().forEach { child ->
            found += child
            queue.addLast(child)
        }
    }
    return found
}
