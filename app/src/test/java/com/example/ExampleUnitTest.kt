package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testRegexActionTags() {
        val sampleWithTag = "Haan jaan, torch on kar di aapke liye! [TORCH_ON]"
        val regex = "\\[(TORCH_ON|TORCH_OFF|OPEN_YOUTUBE|OPEN_WHATSAPP)\\]".toRegex()
        val match = regex.find(sampleWithTag)
        assertNotNull(match)
        assertEquals("TORCH_ON", match?.groups?.get(1)?.value)

        val cleanSpeech = sampleWithTag.replace(regex, "").trim()
        assertEquals("Haan jaan, torch on kar di aapke liye!", cleanSpeech)
    }

    @Test
    fun testActionDetectionPatterns() {
        val prompt1 = "jaan youtube open karo"
        assertTrue(prompt1.contains("youtube"))

        val prompt2 = "torch jala do"
        assertTrue(prompt2.contains("torch") || prompt2.contains("light"))
    }
}
