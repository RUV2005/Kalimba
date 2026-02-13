package com.danmo.kalimba.nmn

object NmnData {

    /**
     * 创建一个初始的空段落
     */
    fun createEmptySegment(id: Int): NmnSegment {
        return NmnSegment(
            id = id,
            name = "", // 段名为空，不显示
            notes = emptyList()
        )
    }
}