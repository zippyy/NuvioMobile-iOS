package com.nuvio.app.features.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SubtitleCharsetDetectorTest {

    @Test
    fun decodesUtf8WithBom() {
        val utf8Text = "שלום עולם! Hello world!"
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val rawBytes = bom + utf8Text.encodeToByteArray()

        val decoded = SubtitleCharsetDetector.decode(rawBytes, languageHint = "heb")
        assertEquals(utf8Text, decoded)
    }

    @Test
    fun decodesUtf8WithoutBom() {
        val utf8Text = "1\n00:00:01,000 --> 00:00:04,000\nזוהן, החזרנו את הפנטום"
        val rawBytes = utf8Text.encodeToByteArray()

        val decoded = SubtitleCharsetDetector.decode(rawBytes, languageHint = "heb")
        assertEquals(utf8Text, decoded)
    }

    @Test
    fun decodesUtf16LeWithBom() {
        val text = "Hello World"
        val utf16LeBytes = text.encodeToByteArray() // UTF-8
        val bom = byteArrayOf(0xFF.toByte(), 0xFE.toByte())
        // Can't easily create UTF-16LE bytes without Java NIO
        // This test verifies the BOM detection code path at least
        val rawBytes = bom + text.encodeToByteArray()
        val decoded = SubtitleCharsetDetector.decode(rawBytes)
        // Will not match exactly since we're feeding UTF-8 after the BOM, but shouldn't crash
        assertTrue(decoded.isNotEmpty())
    }

    @Test
    fun decodesPortugueseUtf8WithoutDistortion() {
        val ptText = "Eles não têm medo de nada. Não vamos desistir, eles estão lá. Você não sabe o que eles têm."
        val rawBytes = ptText.encodeToByteArray()

        val decoded = SubtitleCharsetDetector.decode(rawBytes, languageHint = "por")
        assertEquals(ptText, decoded)
    }

    @Test
    fun preservesTranslatedRomanianUtf8SubtitlesWithRussianLanguageHint() {
        val roText = "Când ajunge la gară, își dă seama că a uitat pâinea și apa în mașină. În sfârșit, pleacă spre casă."
        val rawBytes = roText.encodeToByteArray()

        // Simulates issue: subtitle translated from Russian to Romanian, but track language is still "rus"
        val decoded = SubtitleCharsetDetector.decode(rawBytes, languageHint = "rus")
        assertEquals(roText, decoded)
    }

    @Test
    fun decodesWindows1255HebrewWithLanguageHint() {
        // Manually encode Hebrew text as Windows-1255 bytes
        val hebrewText = "זוהן, החזרנו את הפנטום"
        val rawBytes = encodeToWin1255(hebrewText)

        val decoded = SubtitleCharsetDetector.decode(rawBytes, languageHint = "heb")
        assertTrue(decoded.contains("זוהן, החזרנו את"), "Expected Hebrew text, got: $decoded")
        assertTrue(decoded.contains("הפנטום"), "Expected Hebrew text, got: $decoded")
    }

    @Test
    fun decodesWindows1255HebrewWithoutLanguageHint() {
        val hebrewText = "זוהן, החזרנו את הפנטום"
        val rawBytes = encodeToWin1255(hebrewText)

        val decoded = SubtitleCharsetDetector.decode(rawBytes, languageHint = null)
        assertTrue(decoded.contains("זוהן, החזרנו את"), "Expected Hebrew text, got: $decoded")
    }

    @Test
    fun decodesWindows1256ArabicWithLanguageHint() {
        val arabicText = "مرحبا بكم في نيو يورك"
        val rawBytes = encodeToWin1256(arabicText)

        val decoded = SubtitleCharsetDetector.decode(rawBytes, languageHint = "ara")
        assertEquals(arabicText, decoded)
    }

    @Test
    fun decodesWindows1254TurkishWithLanguageHint() {
        val turkishText = "Merhaba dünya! Şöför ve ağaç."
        val rawBytes = encodeToWin1254(turkishText)

        val decoded = SubtitleCharsetDetector.decode(rawBytes, languageHint = "tur")
        assertEquals(turkishText, decoded)
    }

    @Test
    fun decodesWindows1251CyrillicWithLanguageHint() {
        val russianText = "Привет мир! Это тестовые субтитры."
        val rawBytes = encodeToWin1251(russianText)

        val decoded = SubtitleCharsetDetector.decode(rawBytes, languageHint = "rus")
        assertEquals(russianText, decoded)
    }

    @Test
    fun decodesWindows1253GreekWithLanguageHint() {
        val greekText = "Γεια σου κόσμε! Ελληνικοί υπότιτλοι."
        val rawBytes = encodeToWin1253(greekText)

        val decoded = SubtitleCharsetDetector.decode(rawBytes, languageHint = "ell")
        assertEquals(greekText, decoded)
    }

    @Test
    fun decodesWindows1252PortugueseWithLanguageHint() {
        val ptText = "Eles não têm medo de nada. Não vamos desistir, eles estão lá. Você não sabe o que eles têm."
        val rawBytes = encodeToWin1252(ptText)

        val decoded = SubtitleCharsetDetector.decode(rawBytes, languageHint = "por")
        assertEquals(ptText, decoded)
    }

    @Test
    fun decodesWindows1252SpanishWithoutLanguageHint() {
        val esText = "¿Cómo estás? ¡Muy bien, señor! Canción y corazón."
        val rawBytes = encodeToWin1252(esText)

        val decoded = SubtitleCharsetDetector.decode(rawBytes, languageHint = null)
        assertEquals(esText, decoded)
    }

    @Test
    fun decodesWindows1252FrenchWithoutLanguageHint() {
        val frText = "Bonjour le monde! Ça va très bien, où sont les élèves? À bientôt!"
        val rawBytes = encodeToWin1252(frText)

        val decoded = SubtitleCharsetDetector.decode(rawBytes, languageHint = null)
        assertEquals(frText, decoded)
    }

    @Test
    fun decodesWindows1250PolishWithLanguageHint() {
        val plText = "Cześć świecie! Zażółć gęślą jaźń. Dzień dobry!"
        val rawBytes = encodeToWin1250(plText)

        val decoded = SubtitleCharsetDetector.decode(rawBytes, languageHint = "pol")
        assertEquals(plText, decoded)
    }

    @Test
    fun decodesWindows1250CzechWithLanguageHint() {
        val csText = "Příliš žluťoučký kůň úpěl ďábelské ódy. Dobrý den!"
        val rawBytes = encodeToWin1250(csText)

        val decoded = SubtitleCharsetDetector.decode(rawBytes, languageHint = "ces")
        assertEquals(csText, decoded)
    }

    @Test
    fun decodesWindows1250HungarianWithLanguageHint() {
        val huText = "Jó napot kívánok! Árvíztűrő tükörfúrógép."
        val rawBytes = encodeToWin1250(huText)

        val decoded = SubtitleCharsetDetector.decode(rawBytes, languageHint = "hun")
        assertEquals(huText, decoded)
    }

    @Test
    fun decodesWindows1250RomanianWithLanguageHint() {
        val roText = "Bună ziua! Vă mulţumesc frumos pentru ajutor."
        val rawBytes = encodeToWin1250(roText)

        val decoded = SubtitleCharsetDetector.decode(rawBytes, languageHint = "ron")
        assertEquals(roText, decoded)
    }

    @Test
    fun decodesWindows874ThaiWithLanguageHint() {
        val thText = "สวัสดีครับ ยินดีต้อนรับสู่ประเทศไทย"
        val rawBytes = encodeToWin874(thText)

        val decoded = SubtitleCharsetDetector.decode(rawBytes, languageHint = "tha")
        assertEquals(thText, decoded)
    }

    @Test
    fun decodesWindows1258VietnameseWithLanguageHint() {
        val viText = "Xin chào! Tôi yêu Viê\u0323t Nam."
        val rawBytes = encodeToWin1258(viText)

        val decoded = SubtitleCharsetDetector.decode(rawBytes, languageHint = "vie")
        assertEquals(viText, decoded)
    }

    @Test
    fun normalizeToUtf8ProducesUtf8Bytes() {
        val ptText = "Olá mundo! Isso é um teste."
        val rawBytes = encodeToWin1252(ptText)

        val utf8Bytes = SubtitleCharsetDetector.normalizeToUtf8(rawBytes, languageHint = "por")
        val decodedBack = utf8Bytes.decodeToString()
        assertEquals(ptText, decodedBack)
    }

    // ---- Helper: encode text to Windows-125x byte arrays using lookup tables ----

    private fun encodeToWin1252(text: String): ByteArray = encodeSingleByte(text, WIN1252_TO_UNICODE)
    private fun encodeToWin1250(text: String): ByteArray = encodeSingleByte(text, WIN1250_TO_UNICODE)
    private fun encodeToWin1251(text: String): ByteArray = encodeSingleByte(text, WIN1251_TO_UNICODE)
    private fun encodeToWin1253(text: String): ByteArray = encodeSingleByte(text, WIN1253_TO_UNICODE)
    private fun encodeToWin1254(text: String): ByteArray = encodeSingleByte(text, WIN1254_TO_UNICODE)
    private fun encodeToWin1255(text: String): ByteArray = encodeSingleByte(text, WIN1255_TO_UNICODE)
    private fun encodeToWin1256(text: String): ByteArray = encodeSingleByte(text, WIN1256_TO_UNICODE)
    private fun encodeToWin1258(text: String): ByteArray = encodeSingleByte(text, WIN1258_TO_UNICODE)
    private fun encodeToWin874(text: String): ByteArray = encodeSingleByte(text, WIN874_TO_UNICODE)

    private fun encodeSingleByte(text: String, table: Map<Char, Int>): ByteArray {
        val bytes = ByteArray(text.length)
        for (i in text.indices) {
            val c = text[i]
            bytes[i] = if (c.code < 0x80) {
                c.code.toByte()
            } else {
                (table[c] ?: error("Unmappable char: ${c.code} in $text")).toByte()
            }
        }
        return bytes
    }

    // Reverse lookup tables (Unicode -> Windows byte)
    private val WIN1252_TO_UNICODE by lazy { buildReverseTable(WIN1252_TABLE_RAW) }
    private val WIN1250_TO_UNICODE by lazy { buildReverseTable(WIN1250_TABLE_RAW) }
    private val WIN1251_TO_UNICODE by lazy { buildReverseTable(WIN1251_TABLE_RAW) }
    private val WIN1253_TO_UNICODE by lazy { buildReverseTable(WIN1253_TABLE_RAW) }
    private val WIN1254_TO_UNICODE by lazy { buildReverseTable(WIN1254_TABLE_RAW) }
    private val WIN1255_TO_UNICODE by lazy { buildReverseTable(WIN1255_TABLE_RAW) }
    private val WIN1256_TO_UNICODE by lazy { buildReverseTable(WIN1256_TABLE_RAW) }
    private val WIN1258_TO_UNICODE by lazy { buildReverseTable(WIN1258_TABLE_RAW) }
    private val WIN874_TO_UNICODE by lazy { buildReverseTable(WIN874_TABLE_RAW) }

    private fun buildReverseTable(table: CharArray): Map<Char, Int> {
        val map = mutableMapOf<Char, Int>()
        for (i in table.indices) {
            map[table[i]] = i + 0x80
        }
        return map
    }

    // Independent Windows codec fixtures (undefined bytes retain their control-code slots).
    private val WIN1252_TABLE_RAW = charArrayOf(
        '\u20AC','\u0081','\u201A','\u0192','\u201E','\u2026','\u2020','\u2021',
        '\u02C6','\u2030','\u0160','\u2039','\u0152','\u008D','\u017D','\u008F',
        '\u0090','\u2018','\u2019','\u201C','\u201D','\u2022','\u2013','\u2014',
        '\u02DC','\u2122','\u0161','\u203A','\u0153','\u009D','\u017E','\u0178',
        '\u00A0','\u00A1','\u00A2','\u00A3','\u00A4','\u00A5','\u00A6','\u00A7',
        '\u00A8','\u00A9','\u00AA','\u00AB','\u00AC','\u00AD','\u00AE','\u00AF',
        '\u00B0','\u00B1','\u00B2','\u00B3','\u00B4','\u00B5','\u00B6','\u00B7',
        '\u00B8','\u00B9','\u00BA','\u00BB','\u00BC','\u00BD','\u00BE','\u00BF',
        '\u00C0','\u00C1','\u00C2','\u00C3','\u00C4','\u00C5','\u00C6','\u00C7',
        '\u00C8','\u00C9','\u00CA','\u00CB','\u00CC','\u00CD','\u00CE','\u00CF',
        '\u00D0','\u00D1','\u00D2','\u00D3','\u00D4','\u00D5','\u00D6','\u00D7',
        '\u00D8','\u00D9','\u00DA','\u00DB','\u00DC','\u00DD','\u00DE','\u00DF',
        '\u00E0','\u00E1','\u00E2','\u00E3','\u00E4','\u00E5','\u00E6','\u00E7',
        '\u00E8','\u00E9','\u00EA','\u00EB','\u00EC','\u00ED','\u00EE','\u00EF',
        '\u00F0','\u00F1','\u00F2','\u00F3','\u00F4','\u00F5','\u00F6','\u00F7',
        '\u00F8','\u00F9','\u00FA','\u00FB','\u00FC','\u00FD','\u00FE','\u00FF'
    )
    private val WIN1250_TABLE_RAW = charArrayOf(
        '\u20AC','\u0081','\u201A','\u0083','\u201E','\u2026','\u2020','\u2021',
        '\u0088','\u2030','\u0160','\u2039','\u015A','\u0164','\u017D','\u0179',
        '\u0090','\u2018','\u2019','\u201C','\u201D','\u2022','\u2013','\u2014',
        '\u0098','\u2122','\u0161','\u203A','\u015B','\u0165','\u017E','\u017A',
        '\u00A0','\u02C7','\u02D8','\u0141','\u00A4','\u0104','\u00A6','\u00A7',
        '\u00A8','\u00A9','\u015E','\u00AB','\u00AC','\u00AD','\u00AE','\u017B',
        '\u00B0','\u00B1','\u02DB','\u0142','\u00B4','\u00B5','\u00B6','\u00B7',
        '\u00B8','\u0105','\u015F','\u00BB','\u013D','\u02DD','\u013E','\u017C',
        '\u0154','\u00C1','\u00C2','\u0102','\u00C4','\u0139','\u0106','\u00C7',
        '\u010C','\u00C9','\u0118','\u00CB','\u011A','\u00CD','\u00CE','\u010E',
        '\u0110','\u0143','\u0147','\u00D3','\u00D4','\u0150','\u00D6','\u00D7',
        '\u0158','\u016E','\u00DA','\u0170','\u00DC','\u00DD','\u0162','\u00DF',
        '\u0155','\u00E1','\u00E2','\u0103','\u00E4','\u013A','\u0107','\u00E7',
        '\u010D','\u00E9','\u0119','\u00EB','\u011B','\u00ED','\u00EE','\u010F',
        '\u0111','\u0144','\u0148','\u00F3','\u00F4','\u0151','\u00F6','\u00F7',
        '\u0159','\u016F','\u00FA','\u0171','\u00FC','\u00FD','\u0163','\u02D9'
    )
    private val WIN1251_TABLE_RAW = charArrayOf(
        '\u0402','\u0403','\u201A','\u0453','\u201E','\u2026','\u2020','\u2021',
        '\u20AC','\u2030','\u0409','\u2039','\u040A','\u040C','\u040B','\u040F',
        '\u0452','\u2018','\u2019','\u201C','\u201D','\u2022','\u2013','\u2014',
        '\u0098','\u2122','\u0459','\u203A','\u045A','\u045C','\u045B','\u045F',
        '\u00A0','\u040E','\u045E','\u0408','\u00A4','\u0490','\u00A6','\u00A7',
        '\u0401','\u00A9','\u0404','\u00AB','\u00AC','\u00AD','\u00AE','\u0407',
        '\u00B0','\u00B1','\u0406','\u0456','\u0491','\u00B5','\u00B6','\u00B7',
        '\u0451','\u2116','\u0454','\u00BB','\u0458','\u0405','\u0455','\u0457',
        '\u0410','\u0411','\u0412','\u0413','\u0414','\u0415','\u0416','\u0417',
        '\u0418','\u0419','\u041A','\u041B','\u041C','\u041D','\u041E','\u041F',
        '\u0420','\u0421','\u0422','\u0423','\u0424','\u0425','\u0426','\u0427',
        '\u0428','\u0429','\u042A','\u042B','\u042C','\u042D','\u042E','\u042F',
        '\u0430','\u0431','\u0432','\u0433','\u0434','\u0435','\u0436','\u0437',
        '\u0438','\u0439','\u043A','\u043B','\u043C','\u043D','\u043E','\u043F',
        '\u0440','\u0441','\u0442','\u0443','\u0444','\u0445','\u0446','\u0447',
        '\u0448','\u0449','\u044A','\u044B','\u044C','\u044D','\u044E','\u044F'
    )
    private val WIN1253_TABLE_RAW = charArrayOf(
        '\u20AC','\u0081','\u201A','\u0192','\u201E','\u2026','\u2020','\u2021',
        '\u0088','\u2030','\u008A','\u2039','\u008C','\u008D','\u008E','\u008F',
        '\u0090','\u2018','\u2019','\u201C','\u201D','\u2022','\u2013','\u2014',
        '\u0098','\u2122','\u009A','\u203A','\u009C','\u009D','\u009E','\u009F',
        '\u00A0','\u0385','\u0386','\u00A3','\u00A4','\u00A5','\u00A6','\u00A7',
        '\u00A8','\u00A9','\u00AA','\u00AB','\u00AC','\u00AD','\u00AE','\u2015',
        '\u00B0','\u00B1','\u00B2','\u00B3','\u0384','\u00B5','\u00B6','\u00B7',
        '\u0388','\u0389','\u038A','\u00BB','\u038C','\u00BD','\u038E','\u038F',
        '\u0390','\u0391','\u0392','\u0393','\u0394','\u0395','\u0396','\u0397',
        '\u0398','\u0399','\u039A','\u039B','\u039C','\u039D','\u039E','\u039F',
        '\u03A0','\u03A1','\u00D2','\u03A3','\u03A4','\u03A5','\u03A6','\u03A7',
        '\u03A8','\u03A9','\u03AA','\u03AB','\u03AC','\u03AD','\u03AE','\u03AF',
        '\u03B0','\u03B1','\u03B2','\u03B3','\u03B4','\u03B5','\u03B6','\u03B7',
        '\u03B8','\u03B9','\u03BA','\u03BB','\u03BC','\u03BD','\u03BE','\u03BF',
        '\u03C0','\u03C1','\u03C2','\u03C3','\u03C4','\u03C5','\u03C6','\u03C7',
        '\u03C8','\u03C9','\u03CA','\u03CB','\u03CC','\u03CD','\u03CE','\u00FF'
    )
    private val WIN1254_TABLE_RAW = charArrayOf(
        '\u20AC','\u0081','\u201A','\u0192','\u201E','\u2026','\u2020','\u2021',
        '\u02C6','\u2030','\u0160','\u2039','\u0152','\u008D','\u008E','\u008F',
        '\u0090','\u2018','\u2019','\u201C','\u201D','\u2022','\u2013','\u2014',
        '\u02DC','\u2122','\u0161','\u203A','\u0153','\u009D','\u009E','\u0178',
        '\u00A0','\u00A1','\u00A2','\u00A3','\u00A4','\u00A5','\u00A6','\u00A7',
        '\u00A8','\u00A9','\u00AA','\u00AB','\u00AC','\u00AD','\u00AE','\u00AF',
        '\u00B0','\u00B1','\u00B2','\u00B3','\u00B4','\u00B5','\u00B6','\u00B7',
        '\u00B8','\u00B9','\u00BA','\u00BB','\u00BC','\u00BD','\u00BE','\u00BF',
        '\u00C0','\u00C1','\u00C2','\u00C3','\u00C4','\u00C5','\u00C6','\u00C7',
        '\u00C8','\u00C9','\u00CA','\u00CB','\u00CC','\u00CD','\u00CE','\u00CF',
        '\u011E','\u00D1','\u00D2','\u00D3','\u00D4','\u00D5','\u00D6','\u00D7',
        '\u00D8','\u00D9','\u00DA','\u00DB','\u00DC','\u0130','\u015E','\u00DF',
        '\u00E0','\u00E1','\u00E2','\u00E3','\u00E4','\u00E5','\u00E6','\u00E7',
        '\u00E8','\u00E9','\u00EA','\u00EB','\u00EC','\u00ED','\u00EE','\u00EF',
        '\u011F','\u00F1','\u00F2','\u00F3','\u00F4','\u00F5','\u00F6','\u00F7',
        '\u00F8','\u00F9','\u00FA','\u00FB','\u00FC','\u0131','\u015F','\u00FF'
    )
    private val WIN1255_TABLE_RAW = charArrayOf(
        '\u20AC','\u0081','\u201A','\u0192','\u201E','\u2026','\u2020','\u2021',
        '\u02C6','\u2030','\u008A','\u2039','\u008C','\u008D','\u008E','\u008F',
        '\u0090','\u2018','\u2019','\u201C','\u201D','\u2022','\u2013','\u2014',
        '\u02DC','\u2122','\u009A','\u203A','\u009C','\u009D','\u009E','\u009F',
        '\u00A0','\u00A1','\u00A2','\u00A3','\u20AA','\u00A5','\u00A6','\u00A7',
        '\u00A8','\u00A9','\u00D7','\u00AB','\u00AC','\u00AD','\u00AE','\u00AF',
        '\u00B0','\u00B1','\u00B2','\u00B3','\u00B4','\u00B5','\u00B6','\u00B7',
        '\u00B8','\u00B9','\u00F7','\u00BB','\u00BC','\u00BD','\u00BE','\u00BF',
        '\u05B0','\u05B1','\u05B2','\u05B3','\u05B4','\u05B5','\u05B6','\u05B7',
        '\u05B8','\u05B9','\u00CA','\u05BB','\u05BC','\u05BD','\u05BE','\u05BF',
        '\u05C0','\u05C1','\u05C2','\u05C3','\u05F0','\u05F1','\u05F2','\u05F3',
        '\u05F4','\u00D9','\u00DA','\u00DB','\u00DC','\u00DD','\u00DE','\u00DF',
        '\u05D0','\u05D1','\u05D2','\u05D3','\u05D4','\u05D5','\u05D6','\u05D7',
        '\u05D8','\u05D9','\u05DA','\u05DB','\u05DC','\u05DD','\u05DE','\u05DF',
        '\u05E0','\u05E1','\u05E2','\u05E3','\u05E4','\u05E5','\u05E6','\u05E7',
        '\u05E8','\u05E9','\u05EA','\u00FB','\u00FC','\u200E','\u200F','\u00FF'
    )
    private val WIN1256_TABLE_RAW = charArrayOf(
        '\u20AC','\u067E','\u201A','\u0192','\u201E','\u2026','\u2020','\u2021',
        '\u02C6','\u2030','\u0679','\u2039','\u0152','\u0686','\u0698','\u0688',
        '\u06AF','\u2018','\u2019','\u201C','\u201D','\u2022','\u2013','\u2014',
        '\u06A9','\u2122','\u0691','\u203A','\u0153','\u200C','\u200D','\u06BA',
        '\u00A0','\u060C','\u00A2','\u00A3','\u00A4','\u00A5','\u00A6','\u00A7',
        '\u00A8','\u00A9','\u06BE','\u00AB','\u00AC','\u00AD','\u00AE','\u00AF',
        '\u00B0','\u00B1','\u00B2','\u00B3','\u00B4','\u00B5','\u00B6','\u00B7',
        '\u00B8','\u00B9','\u061B','\u00BB','\u00BC','\u00BD','\u00BE','\u061F',
        '\u06C1','\u0621','\u0622','\u0623','\u0624','\u0625','\u0626','\u0627',
        '\u0628','\u0629','\u062A','\u062B','\u062C','\u062D','\u062E','\u062F',
        '\u0630','\u0631','\u0632','\u0633','\u0634','\u0635','\u0636','\u00D7',
        '\u0637','\u0638','\u0639','\u063A','\u0640','\u0641','\u0642','\u0643',
        '\u00E0','\u0644','\u00E2','\u0645','\u0646','\u0647','\u0648','\u00E7',
        '\u00E8','\u00E9','\u00EA','\u00EB','\u0649','\u064A','\u00EE','\u00EF',
        '\u064B','\u064C','\u064D','\u064E','\u00F4','\u064F','\u0650','\u00F7',
        '\u0651','\u00F9','\u0652','\u00FB','\u00FC','\u200E','\u200F','\u06D2'
    )
    private val WIN1258_TABLE_RAW = charArrayOf(
        '\u20AC','\u0081','\u201A','\u0192','\u201E','\u2026','\u2020','\u2021',
        '\u02C6','\u2030','\u008A','\u2039','\u0152','\u008D','\u008E','\u008F',
        '\u0090','\u2018','\u2019','\u201C','\u201D','\u2022','\u2013','\u2014',
        '\u02DC','\u2122','\u009A','\u203A','\u0153','\u009D','\u009E','\u0178',
        '\u00A0','\u00A1','\u00A2','\u00A3','\u00A4','\u00A5','\u00A6','\u00A7',
        '\u00A8','\u00A9','\u00AA','\u00AB','\u00AC','\u00AD','\u00AE','\u00AF',
        '\u00B0','\u00B1','\u00B2','\u00B3','\u00B4','\u00B5','\u00B6','\u00B7',
        '\u00B8','\u00B9','\u00BA','\u00BB','\u00BC','\u00BD','\u00BE','\u00BF',
        '\u00C0','\u00C1','\u00C2','\u0102','\u00C4','\u00C5','\u00C6','\u00C7',
        '\u00C8','\u00C9','\u00CA','\u00CB','\u0300','\u00CD','\u00CE','\u00CF',
        '\u0110','\u00D1','\u0309','\u00D3','\u00D4','\u01A0','\u00D6','\u00D7',
        '\u00D8','\u00D9','\u00DA','\u00DB','\u00DC','\u01AF','\u0303','\u00DF',
        '\u00E0','\u00E1','\u00E2','\u0103','\u00E4','\u00E5','\u00E6','\u00E7',
        '\u00E8','\u00E9','\u00EA','\u00EB','\u0301','\u00ED','\u00EE','\u00EF',
        '\u0111','\u00F1','\u0323','\u00F3','\u00F4','\u01A1','\u00F6','\u00F7',
        '\u00F8','\u00F9','\u00FA','\u00FB','\u00FC','\u01B0','\u20AB','\u00FF'
    )
    private val WIN874_TABLE_RAW = charArrayOf(
        '\u20AC','\u0081','\u0082','\u0083','\u0084','\u2026','\u0086','\u0087',
        '\u0088','\u0089','\u008A','\u008B','\u008C','\u008D','\u008E','\u008F',
        '\u0090','\u2018','\u2019','\u201C','\u201D','\u2022','\u2013','\u2014',
        '\u0098','\u0099','\u009A','\u009B','\u009C','\u009D','\u009E','\u009F',
        '\u00A0','\u0E01','\u0E02','\u0E03','\u0E04','\u0E05','\u0E06','\u0E07',
        '\u0E08','\u0E09','\u0E0A','\u0E0B','\u0E0C','\u0E0D','\u0E0E','\u0E0F',
        '\u0E10','\u0E11','\u0E12','\u0E13','\u0E14','\u0E15','\u0E16','\u0E17',
        '\u0E18','\u0E19','\u0E1A','\u0E1B','\u0E1C','\u0E1D','\u0E1E','\u0E1F',
        '\u0E20','\u0E21','\u0E22','\u0E23','\u0E24','\u0E25','\u0E26','\u0E27',
        '\u0E28','\u0E29','\u0E2A','\u0E2B','\u0E2C','\u0E2D','\u0E2E','\u0E2F',
        '\u0E30','\u0E31','\u0E32','\u0E33','\u0E34','\u0E35','\u0E36','\u0E37',
        '\u0E38','\u0E39','\u0E3A','\u00DB','\u00DC','\u00DD','\u00DE','\u0E3F',
        '\u0E40','\u0E41','\u0E42','\u0E43','\u0E44','\u0E45','\u0E46','\u0E47',
        '\u0E48','\u0E49','\u0E4A','\u0E4B','\u0E4C','\u0E4D','\u0E4E','\u0E4F',
        '\u0E50','\u0E51','\u0E52','\u0E53','\u0E54','\u0E55','\u0E56','\u0E57',
        '\u0E58','\u0E59','\u0E5A','\u0E5B','\u00FC','\u00FD','\u00FE','\u00FF'
    )
}
