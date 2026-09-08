package se.sanger.learnlanguagefast.data

import kotlin.test.Test
import kotlin.test.assertEquals

class CsvTest {

    @Test
    fun `parses simple rows and skips blank lines`() {
        val rows = Csv.parse("a,b\n\nc,d\r\n\r\ne,f")
        assertEquals(listOf(listOf("a", "b"), listOf("c", "d"), listOf("e", "f")), rows)
    }

    @Test
    fun `parses quoted fields with commas, quotes and newlines`() {
        val rows = Csv.parse("\"hej, du\",\"say \"\"hi\"\"\"\n\"multi\nline\",x")
        assertEquals(listOf("hej, du", "say \"hi\""), rows[0])
        assertEquals(listOf("multi\nline", "x"), rows[1])
    }

    @Test
    fun `detects semicolon delimiter`() {
        val rows = Csv.parse("hund;pies\nkatt;kot")
        assertEquals(listOf(listOf("hund", "pies"), listOf("katt", "kot")), rows)
    }

    @Test
    fun `strips BOM and keeps polish characters`() {
        val rows = Csv.parse("\uFEFFżółć,łódź")
        assertEquals(listOf(listOf("żółć", "łódź")), rows)
    }

    @Test
    fun `formatRow escapes when needed and round trips`() {
        val fields = listOf("plain", "with, comma", "with \"quote\"", " padded ")
        val line = Csv.formatRow(fields)
        assertEquals("plain,\"with, comma\",\"with \"\"quote\"\"\",\" padded \"", line)
        assertEquals(listOf(fields), Csv.parse(line))
    }
}
