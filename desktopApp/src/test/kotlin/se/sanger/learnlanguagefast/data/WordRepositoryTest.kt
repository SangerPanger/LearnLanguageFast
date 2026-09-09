package se.sanger.learnlanguagefast.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import se.sanger.learnlanguagefast.model.GameSettings
import kotlin.test.*

class WordRepositoryTest {

    private fun newRepo() = WordRepository(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY))

    @Test
    fun `test 1 - words are added to the chosen list`() {
        val repo = newRepo()
        val list = repo.createList("Polska A1")!!
        repo.insertWord(list.id, "hund", "pies")
        repo.insertWord(list.id, "katt", "kot")

        val words = repo.getWordsForList(list.id)
        assertEquals(2, words.size)
        assertTrue(words.all { it.listId == list.id })
        assertEquals(listOf("pies", "kot"), words.map { it.targetWord })
    }

    @Test
    fun `test 2 - glossary for one list does not show words from another`() {
        val repo = newRepo()
        val a1 = repo.createList("Polska A1")!!
        val verb = repo.createList("Polska Verb")!!
        repo.insertWord(a1.id, "hund", "pies")
        repo.insertWord(verb.id, "springa", "biegać")

        val a1Words = repo.getWordsForList(a1.id)
        assertEquals(1, a1Words.size)
        assertEquals("hund", a1Words[0].sourceWord)
        assertFalse(a1Words.any { it.sourceWord == "springa" })
    }

    @Test
    fun `test 3 and 4 - words for lists merges only selected lists`() {
        val repo = newRepo()
        val a1 = repo.createList("Polska A1")!!
        val verb = repo.createList("Polska Verb")!!
        val other = repo.createList("Mat")!!
        repo.insertWord(a1.id, "hund", "pies")
        repo.insertWord(verb.id, "springa", "biegać")
        repo.insertWord(other.id, "bröd", "chleb")

        val both = repo.getWordsForLists(setOf(a1.id, verb.id))
        assertEquals(setOf("hund", "springa"), both.map { it.sourceWord }.toSet())

        val onlyA1 = repo.getWordsForLists(setOf(a1.id))
        assertEquals(listOf("hund"), onlyA1.map { it.sourceWord })

        assertTrue(repo.getWordsForLists(emptySet()).isEmpty())
    }

    @Test
    fun `retrain words for lists respects both list and flag`() {
        val repo = newRepo()
        val a1 = repo.createList("A1")!!
        val a2 = repo.createList("A2")!!
        repo.insertWord(a1.id, "hund", "pies", retrain = true)
        repo.insertWord(a1.id, "katt", "kot", retrain = false)
        repo.insertWord(a2.id, "hus", "dom", retrain = true)

        val retrain = repo.getRetrainWordsForLists(setOf(a1.id))
        assertEquals(listOf("hund"), retrain.map { it.sourceWord })
        assertEquals(2, repo.getRetrainWords().size)
    }

    @Test
    fun `test 5 - import skips existing duplicates and imports new words`() {
        val repo = newRepo()
        val list = repo.createList("Polska A1")!!
        repo.insertWord(list.id, "hund", "pies")

        val result = repo.importCsv(list.id, "hund,pies\nhus,dom\n")
        assertEquals(ImportResult(imported = 1, skippedDuplicates = 1, invalidRows = 0), result)
        assertEquals(2, repo.getWordsForList(list.id).size)
    }

    @Test
    fun `test 6 - duplicates inside the csv are skipped`() {
        val repo = newRepo()
        val list = repo.createList("Tom")!!

        val result = repo.importCsv(list.id, "hund,pies\nhund,pies\nkatt,kot\n")
        assertEquals(ImportResult(imported = 2, skippedDuplicates = 1, invalidRows = 0), result)
    }

    @Test
    fun `test 7 - duplicate check is case-insensitive and trimmed`() {
        val repo = newRepo()
        val list = repo.createList("Polska A1")!!
        repo.insertWord(list.id, "Hund", "Pies")

        val result = repo.importCsv(list.id, " hund , pies \n")
        assertEquals(0, result.imported)
        assertEquals(1, result.skippedDuplicates)
        assertTrue(repo.isDuplicateInList(list.id, " HUND", "pies "))

        // Original casing is preserved
        assertEquals("Pies", repo.getWordsForList(list.id)[0].targetWord)
    }

    @Test
    fun `test 8 - same pair allowed in two different lists`() {
        val repo = newRepo()
        val a1 = repo.createList("Polska A1")!!
        val djur = repo.createList("Djur")!!
        repo.insertWord(a1.id, "hund", "pies")

        val result = repo.importCsv(djur.id, "hund,pies\n")
        assertEquals(1, result.imported)
        assertEquals(1, repo.getWordsForList(djur.id).size)
        assertEquals(1, repo.getWordsForList(a1.id).size)
    }

    @Test
    fun `test 9 - export then import round trip preserves fields`() {
        val repo = newRepo()
        val src = repo.createList("Source")!!
        repo.insertWord(src.id, "hund", "pies", retrain = false, perfectCount = 3)
        repo.insertWord(src.id, "katt", "kot", retrain = true, perfectCount = 1)
        repo.insertWord(src.id, "hej, du", "cześć \"ty\"")

        val csv = repo.exportCsv(src.id)
        assertTrue(csv.startsWith("sourceWord,targetWord,retrain,perfectCount"))

        val dst = repo.createList("Target")!!
        val result = repo.importCsv(dst.id, csv)
        assertEquals(ImportResult(3, 0, 0), result)

        val imported = repo.getWordsForList(dst.id)
        val hund = imported.first { it.sourceWord == "hund" }
        val katt = imported.first { it.sourceWord == "katt" }
        val hej = imported.first { it.sourceWord == "hej, du" }
        assertEquals("pies", hund.targetWord)
        assertFalse(hund.retrain)
        assertEquals(3, hund.perfectCount)
        assertTrue(katt.retrain)
        assertEquals(1, katt.perfectCount)
        assertEquals("cześć \"ty\"", hej.targetWord)
    }

    @Test
    fun `test 10 - polish unicode characters are preserved`() {
        val repo = newRepo()
        val list = repo.createList("Polska")!!
        val csv = "\uFEFFsourceWord,targetWord\nmąż,mąż\nżona,żona\nksiążka,książka\nłódź,łódź\n"

        val result = repo.importCsv(list.id, csv)
        assertEquals(4, result.imported)
        val targets = repo.getWordsForList(list.id).map { it.targetWord }
        assertEquals(listOf("mąż", "żona", "książka", "łódź"), targets)

        val exported = repo.exportCsv(list.id)
        assertTrue(exported.contains("łódź,łódź"))
    }

    @Test
    fun `import handles optional retrain column, invalid rows and blank lines`() {
        val repo = newRepo()
        val list = repo.createList("L")!!
        val csv = """
            sourceWord,targetWord,retrain
            hund,pies,false
            
            katt,kot,true
            bara-ett-ord
            ,tom
            hus,dom
        """.trimIndent()

        val result = repo.importCsv(list.id, csv)
        assertEquals(ImportResult(imported = 3, skippedDuplicates = 0, invalidRows = 2), result)
        val words = repo.getWordsForList(list.id)
        assertTrue(words.first { it.sourceWord == "katt" }.retrain)
        assertFalse(words.first { it.sourceWord == "hund" }.retrain)
        assertFalse(words.first { it.sourceWord == "hus" }.retrain)
        assertTrue(words.all { it.perfectCount == 0 })
    }

    @Test
    fun `import supports semicolon separated csv from excel`() {
        val repo = newRepo()
        val list = repo.createList("Excel")!!
        val result = repo.importCsv(list.id, "sourceWord;targetWord\r\nhund;pies\r\nkatt;kot\r\n")
        assertEquals(2, result.imported)
    }

    @Test
    fun `list titles must be unique case-insensitively and non-blank`() {
        val repo = newRepo()
        assertNotNull(repo.createList("Polska A1"))
        assertNull(repo.createList(" polska a1 "))
        assertNull(repo.createList("   "))
        assertEquals(1, repo.getAllLists().size)
        assertEquals("Polska A1", repo.findListByTitle(" POLSKA a1")!!.title)
    }

    @Test
    fun `migration - old database gets a Default list and keeps its words`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        // Simulate schema from the previous app version (no WordList, no listId, no perfectCount)
        driver.execute(
            null,
            """CREATE TABLE WordEntry (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                sourceWord TEXT NOT NULL,
                targetWord TEXT NOT NULL,
                retrain INTEGER NOT NULL DEFAULT 0
            )""",
            0
        )
        driver.execute(null, "INSERT INTO WordEntry(sourceWord, targetWord, retrain) VALUES ('hund', 'pies', 1)", 0)
        driver.execute(null, "INSERT INTO WordEntry(sourceWord, targetWord, retrain) VALUES ('katt', 'kot', 0)", 0)

        val repo = WordRepository(driver)

        val lists = repo.getAllLists()
        assertEquals(1, lists.size)
        assertEquals(WordRepository.DEFAULT_LIST_TITLE, lists[0].title)

        val words = repo.getWordsForList(lists[0].id)
        assertEquals(2, words.size)
        assertTrue(words.first { it.sourceWord == "hund" }.retrain)
        assertEquals(0, words[0].perfectCount)

        // Running the migration again must be a no-op
        val repo2 = WordRepository(driver)
        assertEquals(1, repo2.getAllLists().size)
        assertEquals(2, repo2.getAllWords().size)
    }

    @Test
    fun `game settings use defaults and persist across repository restart`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        val firstRepository = WordRepository(driver)
        assertEquals(GameSettings(), firstRepository.getGameSettings())

        val changed = GameSettings(
            failsafeEnabled = true,
            failsafeMistakes = 5,
            hardcoreEnabled = true,
            repeaterEnabled = true,
            repeaterCount = 4,
            flowEnabled = true,
            imprintEnabled = true
        )
        firstRepository.saveGameSettings(changed)

        val restartedRepository = WordRepository(driver)
        assertEquals(changed, restartedRepository.getGameSettings())
    }
}
