package symspellkt

import com.darkrockstudios.symspellkt.api.DictionaryHolder
import com.darkrockstudios.symspellkt.common.*
import com.darkrockstudios.symspellkt.exception.SpellCheckException
import com.darkrockstudios.symspellkt.impl.InMemoryDictionaryHolder
import com.darkrockstudios.symspellkt.impl.SymSpell
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.util.*

class SymSpellTest {
	private lateinit var dictionaryHolder: DictionaryHolder
	private lateinit var symSpell: SymSpell
	private lateinit var damerauLevenshteinDistance: DamerauLevenshteinDistance

	@Before
	@Throws(IOException::class, SpellCheckException::class)
	fun setup() {
		val classLoader = SymSpellTest::class.java.classLoader

		val spellCheckSettings = SpellCheckSettings(
			countThreshold = 1L,
			maxEditDistance = 2.0,
			topK = 100,
			prefixLength = 10,
			verbosity = Verbosity.All,
		)

		damerauLevenshteinDistance = DamerauLevenshteinDistance()
		dictionaryHolder = InMemoryDictionaryHolder(spellCheckSettings, Murmur3HashFunction())

		symSpell = SymSpell(
			dictionaryHolder = dictionaryHolder,
			stringDistance = damerauLevenshteinDistance,
			spellCheckSettings = spellCheckSettings,
		)

		loadUniGramFile(
			File(classLoader.getResource("frequency_dictionary_en_82_765.txt")!!.file)
		)
		loadBiGramFile(
			File(classLoader.getResource("frequency_bigramdictionary_en_243_342.txt")!!.file)
		)
	}

	@Test
	@Throws(SpellCheckException::class)
	fun testMultiWordCorrection() {
		assertTypoAndCorrected(
			symSpell,
			"theq uick brown f ox jumps over the lazy dog",
			"the quick brown fox jumps over the lazy dog",
			2.0
		)

		assertTypoAndCorrected(
			symSpell,
			"Whereis th elove hehaD Dated FOREEVER forImuch of thepast who couqdn'tread in sixthgrade AND ins pired him",
			"where is the love he had dated forever for much of the past who couldn't read in sixth grade and inspired him",
			2.0
		)
	}

	@Test
	@Throws(SpellCheckException::class)
	fun testMultiWordCorrection2() {
		assertTypoAndCorrected(
			symSpell,
			"Whereis th elove hehaD",
			"where is the love he had",
			2.0
		)
	}

	@Test
	@Throws(SpellCheckException::class)
	fun testSingleWordCorrection() {
		assertTypoAndCorrected(
			symSpell,
			"bigjest", "biggest", 2.0
		)
		assertTypoAndCorrected(
			symSpell,
			"playrs", "players", 2.0
		)
		assertTypoAndCorrected(
			symSpell,
			"slatew", "slate", 2.0
		)
		assertTypoAndCorrected(
			symSpell,
			"ith", "with", 2.0
		)
		assertTypoAndCorrected(
			symSpell,
			"plety", "plenty", 2.0
		)
		assertTypoAndCorrected(
			symSpell,
			"funn", "fun", 2.0
		)
	}

	@Test
	@Throws(SpellCheckException::class)
	fun testDoubleWordCorrection() {
		val testPhrase = "couqdn'tread".lowercase(Locale.getDefault())
		val correctedPhrase = "couldn't read"
		val suggestionItems: List<SuggestionItem> = symSpell
			.lookupCompound(testPhrase.lowercase(Locale.getDefault()), 2.0)

		Assert.assertTrue(suggestionItems.isNotEmpty())
		Assert.assertEquals(correctedPhrase.lowercase(), suggestionItems[0].term.trim())
	}

	@Test
	fun testDoubleComparison() {
		Assert.assertTrue(SpellHelper.isEqualDouble(1.00999, 1.0))
		Assert.assertTrue(SpellHelper.isLessDouble(0.90999, 1.0))
		Assert.assertTrue(SpellHelper.isLessOrEqualDouble(0.7, 1.0))
	}

	@Test(expected = SpellCheckException::class)
	@Throws(SpellCheckException::class)
	fun testEdgeCases2() {
		val suggestionItems: List<SuggestionItem> = symSpell
			.lookupCompound("tes", 5.0)
		Assert.assertNotNull(suggestionItems)
		assertTypoAndCorrected(
			symSpell,
			"", "with", 2.0
		)
		assertTypoAndCorrected(
			symSpell,
			"", "with", 3.0
		)
	}

	@Test(expected = SpellCheckException::class)
	@Throws(SpellCheckException::class)
	fun testEdgeCases3() {
		val suggestionItems: List<SuggestionItem> = symSpell
			.lookupCompound("a", 5.0)
		Assert.assertNotNull(suggestionItems)
		assertTypoAndCorrected(
			symSpell,
			"", "with", 2.0
		)
		assertTypoAndCorrected(
			symSpell,
			"", "with", 3.0
		)
	}

	@Test
	@Throws(SpellCheckException::class)
	fun testLookup() {
		var suggestionItems = symSpell.lookup("hel")
		Assert.assertNotNull(suggestionItems)
		Assert.assertTrue(suggestionItems.size > 0)
		Assert.assertEquals(78, suggestionItems.size)

		suggestionItems = symSpell.lookup("hel", Verbosity.All)
		Assert.assertEquals(78, suggestionItems.size)
	}

	@Test
	@Throws(SpellCheckException::class)
	fun testLookupCloset() {
		val suggestionItems: List<SuggestionItem> = symSpell.lookup("resial", Verbosity.Closest)
		Assert.assertNotNull(suggestionItems)
		Assert.assertTrue(suggestionItems.isNotEmpty())
		Assert.assertEquals(3, suggestionItems.size)
	}

	@Test
	@Throws(Exception::class)
	fun testWordBreak() {
		val suggestionItems: Composition = symSpell
			.wordBreakSegmentation(
				"itwasabrightcolddayinaprilandtheclockswerestrikingthirteen", 10,
				2.0
			)
		Assert.assertNotNull(suggestionItems)
		Assert.assertEquals(
			"it was bright cold day in april and the clock were striking thirteen",
			suggestionItems.correctedString
		)
	}

	@Throws(IOException::class, SpellCheckException::class)
	private fun loadUniGramFile(file: File) {
		val br = BufferedReader(FileReader(file))
		br.forEachLine { line ->
			val arr = line.split("\\s+".toRegex())
			dictionaryHolder.addItem(DictionaryItem(arr[0], arr[1].toDouble(), -1.0))
		}
	}

	@Throws(IOException::class, SpellCheckException::class)
	private fun loadBiGramFile(file: File) {
		val br = BufferedReader(FileReader(file))
		br.forEachLine { line ->
			val arr = line.split("\\s+".toRegex())
			dictionaryHolder
				.addItem(DictionaryItem(arr[0] + " " + arr[1], arr[2].toDouble(), -1.0))
		}
	}

	companion object {
		@Throws(SpellCheckException::class)
		fun assertTypoAndCorrected(
			spellCheck: SymSpell, typo: String, correct: String,
			maxEd: Double
		) {
			val suggestionItems: List<SuggestionItem> = spellCheck
				.lookupCompound(typo.lowercase().trim(), maxEd)
			Assert.assertTrue(suggestionItems.isNotEmpty())
			Assert.assertEquals(
				correct.lowercase().trim(),
				suggestionItems[0].term.trim()
			)
		}
	}
}
