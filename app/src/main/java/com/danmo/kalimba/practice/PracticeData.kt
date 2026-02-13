package com.danmo.kalimba.practice

object PracticeData {

    private fun note(keyId: String, pitch: Int, octave: Octave) =
        PracticeNote(keyId, pitch, octave)

    private fun rest() = PracticeNote("", 0, Octave.MIDDLE, true)

    val SEGMENTS = listOf(
        // 第1段：1 1 5 5 6 6 5
        PracticeSegment(1, "第一段乐句", listOf(
            note("d1m", 1, Octave.MIDDLE),
            note("d1m", 1, Octave.MIDDLE),
            note("d5m", 5, Octave.MIDDLE),
            note("d5m", 5, Octave.MIDDLE),
            note("u6m", 6, Octave.MIDDLE),
            note("u6m", 6, Octave.MIDDLE),
            note("d5m", 5, Octave.MIDDLE),
        )),

        // 第2段：4 4 3 3 2 2 1
        PracticeSegment(2, "第二段乐句", listOf(
            note("u4m", 4, Octave.MIDDLE),
            note("u4m", 4, Octave.MIDDLE),
            note("d3m", 3, Octave.MIDDLE),
            note("d3m", 3, Octave.MIDDLE),
            note("u2m", 2, Octave.MIDDLE),
            note("u2m", 2, Octave.MIDDLE),
            note("d1m", 1, Octave.MIDDLE),
        )),

        // 第3段：5 5 4 4 3 3 2
        PracticeSegment(3, "第三段乐句", listOf(
            note("d5m", 5, Octave.MIDDLE),
            note("d5m", 5, Octave.MIDDLE),
            note("u4m", 4, Octave.MIDDLE),
            note("u4m", 4, Octave.MIDDLE),
            note("d3m", 3, Octave.MIDDLE),
            note("d3m", 3, Octave.MIDDLE),
            note("u2m", 2, Octave.MIDDLE),
        )),

        // 第4段：5 5 4 4 3 3 2
        PracticeSegment(4, "第四段乐句", listOf(
            note("d5m", 5, Octave.MIDDLE),
            note("d5m", 5, Octave.MIDDLE),
            note("u4m", 4, Octave.MIDDLE),
            note("u4m", 4, Octave.MIDDLE),
            note("d3m", 3, Octave.MIDDLE),
            note("d3m", 3, Octave.MIDDLE),
            note("u2m", 2, Octave.MIDDLE),
        )),

        // 第5段：1 1 5 5 6 6 5
        PracticeSegment(5, "第五段乐句", listOf(
            note("d1m", 1, Octave.MIDDLE),
            note("d1m", 1, Octave.MIDDLE),
            note("d5m", 5, Octave.MIDDLE),
            note("d5m", 5, Octave.MIDDLE),
            note("u6m", 6, Octave.MIDDLE),
            note("u6m", 6, Octave.MIDDLE),
            note("d5m", 5, Octave.MIDDLE),
        )),

        // 第6段：4 4 3 3 2 2 1
        PracticeSegment(6, "第六段乐句", listOf(
            note("u4m", 4, Octave.MIDDLE),
            note("u4m", 4, Octave.MIDDLE),
            note("d3m", 3, Octave.MIDDLE),
            note("d3m", 3, Octave.MIDDLE),
            note("u2m", 2, Octave.MIDDLE),
            note("u2m", 2, Octave.MIDDLE),
            note("d1m", 1, Octave.MIDDLE),
        )),
    )
}