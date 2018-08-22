package com.github.fhenm.himataway.model

/**
 * Twitter4j の PagableResponseList の抽象化クラス(Twitter4j に依存したくないので作った)
 */
data class PagedResponseList<T, Q>(
        val items:List<T>,
        val hasNext: Boolean,
        val nextCursor: Q?
)