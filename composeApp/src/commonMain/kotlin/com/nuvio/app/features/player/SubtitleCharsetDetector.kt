package com.nuvio.app.features.player

import kotlin.math.min

/**
 * Detects and decodes subtitle text from various legacy character encodings.
 *
 * KMP-compatible version — no Android/Java NIO dependencies in common code.
 * Uses byte-to-char lookup tables for single-byte charsets (windows-125x, KOI8-R, windows-874).
 * Delegates multi-byte charset decoding to platform-specific implementations via expect/actual.
 *
 * Detection logic is entirely in commonMain.
 */
internal object SubtitleCharsetDetector {

    // ---- Single-byte charset lookup tables (byte 0x80-0xFF -> Unicode char) ----

    private val WIN1252_TABLE: CharArray = charArrayOf(
        '\u20AC', '\uFFFD', '\u201A', '\u0192', '\u201E', '\u2026', '\u2020', '\u2021',
        '\u02C6', '\u2030', '\u0160', '\u2039', '\u0152', '\uFFFD', '\u017D', '\uFFFD',
        '\uFFFD', '\u2018', '\u2019', '\u201C', '\u201D', '\u2022', '\u2013', '\u2014',
        '\u02DC', '\u2122', '\u0161', '\u203A', '\u0153', '\uFFFD', '\u017E', '\u0178',
        '\u00A0', '\u00A1', '\u00A2', '\u00A3', '\u00A4', '\u00A5', '\u00A6', '\u00A7',
        '\u00A8', '\u00A9', '\u00AA', '\u00AB', '\u00AC', '\u00AD', '\u00AE', '\u00AF',
        '\u00B0', '\u00B1', '\u00B2', '\u00B3', '\u00B4', '\u00B5', '\u00B6', '\u00B7',
        '\u00B8', '\u00B9', '\u00BA', '\u00BB', '\u00BC', '\u00BD', '\u00BE', '\u00BF',
        '\u00C0', '\u00C1', '\u00C2', '\u00C3', '\u00C4', '\u00C5', '\u00C6', '\u00C7',
        '\u00C8', '\u00C9', '\u00CA', '\u00CB', '\u00CC', '\u00CD', '\u00CE', '\u00CF',
        '\u00D0', '\u00D1', '\u00D2', '\u00D3', '\u00D4', '\u00D5', '\u00D6', '\u00D7',
        '\u00D8', '\u00D9', '\u00DA', '\u00DB', '\u00DC', '\u00DD', '\u00DE', '\u00DF',
        '\u00E0', '\u00E1', '\u00E2', '\u00E3', '\u00E4', '\u00E5', '\u00E6', '\u00E7',
        '\u00E8', '\u00E9', '\u00EA', '\u00EB', '\u00EC', '\u00ED', '\u00EE', '\u00EF',
        '\u00F0', '\u00F1', '\u00F2', '\u00F3', '\u00F4', '\u00F5', '\u00F6', '\u00F7',
        '\u00F8', '\u00F9', '\u00FA', '\u00FB', '\u00FC', '\u00FD', '\u00FE', '\u00FF'
    )

    private val WIN1250_TABLE: CharArray = charArrayOf(
        '\u20AC', '\uFFFD', '\u201A', '\uFFFD', '\u201E', '\u2026', '\u2020', '\u2021',
        '\uFFFD', '\u2030', '\u0160', '\u2039', '\u015A', '\u0164', '\u017D', '\u0179',
        '\uFFFD', '\u2018', '\u2019', '\u201C', '\u201D', '\u2022', '\u2013', '\u2014',
        '\uFFFD', '\u2122', '\u0161', '\u203A', '\u015B', '\u0165', '\u017E', '\u017A',
        '\u00A0', '\u02C7', '\u02D8', '\u0141', '\u00A4', '\u0104', '\u00A6', '\u00A7',
        '\u00A8', '\u00A9', '\u015E', '\u00AB', '\u00AC', '\u00AD', '\u00AE', '\u017B',
        '\u00B0', '\u00B1', '\u02DB', '\u0142', '\u00B4', '\u00B5', '\u00B6', '\u00B7',
        '\u00B8', '\u0105', '\u015F', '\u00BB', '\u013D', '\u02DD', '\u013E', '\u017C',
        '\u0154', '\u00C1', '\u00C2', '\u0102', '\u00C4', '\u0139', '\u0106', '\u00C7',
        '\u010C', '\u00C9', '\u0118', '\u00CB', '\u011A', '\u00CD', '\u00CE', '\u010E',
        '\u0110', '\u0143', '\u0147', '\u00D3', '\u00D4', '\u0150', '\u00D6', '\u00D7',
        '\u0158', '\u016E', '\u00DA', '\u0170', '\u00DC', '\u00DD', '\u0162', '\u00DF',
        '\u0155', '\u00E1', '\u00E2', '\u0103', '\u00E4', '\u013A', '\u0107', '\u00E7',
        '\u010D', '\u00E9', '\u0119', '\u00EB', '\u011B', '\u00ED', '\u00EE', '\u010F',
        '\u0111', '\u0144', '\u0148', '\u00F3', '\u00F4', '\u0151', '\u00F6', '\u00F7',
        '\u0159', '\u016F', '\u00FA', '\u0171', '\u00FC', '\u00FD', '\u0163', '\u02D9'
    )

    private val WIN1251_TABLE: CharArray = charArrayOf(
        '\u0402', '\u0403', '\u201A', '\u0453', '\u201E', '\u2026', '\u2020', '\u2021',
        '\u20AC', '\u2030', '\u0409', '\u2039', '\u040A', '\u040C', '\u040B', '\u040F',
        '\u0452', '\u2018', '\u2019', '\u201C', '\u201D', '\u2022', '\u2013', '\u2014',
        '\uFFFD', '\u2122', '\u0459', '\u203A', '\u045A', '\u045C', '\u045B', '\u045F',
        '\u00A0', '\u040E', '\u045E', '\u0408', '\u00A4', '\u0490', '\u00A6', '\u00A7',
        '\u0401', '\u00A9', '\u0404', '\u00AB', '\u00AC', '\u00AD', '\u00AE', '\u0407',
        '\u00B0', '\u00B1', '\u0406', '\u0456', '\u0491', '\u00B5', '\u00B6', '\u00B7',
        '\u0451', '\u2116', '\u0454', '\u00BB', '\u0458', '\u0405', '\u0455', '\u0457',
        '\u0410', '\u0411', '\u0412', '\u0413', '\u0414', '\u0415', '\u0416', '\u0417',
        '\u0418', '\u0419', '\u041A', '\u041B', '\u041C', '\u041D', '\u041E', '\u041F',
        '\u0420', '\u0421', '\u0422', '\u0423', '\u0424', '\u0425', '\u0426', '\u0427',
        '\u0428', '\u0429', '\u042A', '\u042B', '\u042C', '\u042D', '\u042E', '\u042F',
        '\u0430', '\u0431', '\u0432', '\u0433', '\u0434', '\u0435', '\u0436', '\u0437',
        '\u0438', '\u0439', '\u043A', '\u043B', '\u043C', '\u043D', '\u043E', '\u043F',
        '\u0440', '\u0441', '\u0442', '\u0443', '\u0444', '\u0445', '\u0446', '\u0447',
        '\u0448', '\u0449', '\u044A', '\u044B', '\u044C', '\u044D', '\u044E', '\u044F'
    )

    private val WIN1254_TABLE: CharArray = charArrayOf(
        '\u20AC', '\uFFFD', '\u201A', '\u0192', '\u201E', '\u2026', '\u2020', '\u2021',
        '\u02C6', '\u2030', '\u0160', '\u2039', '\u0152', '\uFFFD', '\uFFFD', '\uFFFD',
        '\uFFFD', '\u2018', '\u2019', '\u201C', '\u201D', '\u2022', '\u2013', '\u2014',
        '\u02DC', '\u2122', '\u0161', '\u203A', '\u0153', '\uFFFD', '\uFFFD', '\u0178',
        '\u00A0', '\u00A1', '\u00A2', '\u00A3', '\u00A4', '\u00A5', '\u00A6', '\u00A7',
        '\u00A8', '\u00A9', '\u00AA', '\u00AB', '\u00AC', '\u00AD', '\u00AE', '\u00AF',
        '\u00B0', '\u00B1', '\u00B2', '\u00B3', '\u00B4', '\u00B5', '\u00B6', '\u00B7',
        '\u00B8', '\u00B9', '\u00BA', '\u00BB', '\u00BC', '\u00BD', '\u00BE', '\u00BF',
        '\u00C0', '\u00C1', '\u00C2', '\u00C3', '\u00C4', '\u00C5', '\u00C6', '\u00C7',
        '\u00C8', '\u00C9', '\u00CA', '\u00CB', '\u00CC', '\u00CD', '\u00CE', '\u00CF',
        '\u011E', '\u00D1', '\u00D2', '\u00D3', '\u00D4', '\u00D5', '\u00D6', '\u00D7',
        '\u00D8', '\u00D9', '\u00DA', '\u00DB', '\u00DC', '\u0130', '\u015E', '\u00DF',
        '\u00E0', '\u00E1', '\u00E2', '\u00E3', '\u00E4', '\u00E5', '\u00E6', '\u00E7',
        '\u00E8', '\u00E9', '\u00EA', '\u00EB', '\u00EC', '\u00ED', '\u00EE', '\u00EF',
        '\u011F', '\u00F1', '\u00F2', '\u00F3', '\u00F4', '\u00F5', '\u00F6', '\u00F7',
        '\u00F8', '\u00F9', '\u00FA', '\u00FB', '\u00FC', '\u0131', '\u015F', '\u00FF'
    )

    private val WIN1255_TABLE: CharArray = charArrayOf(
        '\u20AC', '\uFFFD', '\u201A', '\u0192', '\u201E', '\u2026', '\u2020', '\u2021',
        '\u02C6', '\u2030', '\uFFFD', '\u2039', '\uFFFD', '\uFFFD', '\uFFFD', '\uFFFD',
        '\uFFFD', '\u2018', '\u2019', '\u201C', '\u201D', '\u2022', '\u2013', '\u2014',
        '\u02DC', '\u2122', '\uFFFD', '\u203A', '\uFFFD', '\uFFFD', '\uFFFD', '\uFFFD',
        '\u00A0', '\u00A1', '\u00A2', '\u00A3', '\u20AA', '\u00A5', '\u00A6', '\u00A7',
        '\u00A8', '\u00A9', '\u00D7', '\u00AB', '\u00AC', '\u00AD', '\u00AE', '\u00AF',
        '\u00B0', '\u00B1', '\u00B2', '\u00B3', '\u00B4', '\u00B5', '\u00B6', '\u00B7',
        '\u00B8', '\u00B9', '\u00F7', '\u00BB', '\u00BC', '\u00BD', '\u00BE', '\u00BF',
        '\u05B0', '\u05B1', '\u05B2', '\u05B3', '\u05B4', '\u05B5', '\u05B6', '\u05B7',
        '\u05B8', '\u05B9', '\uFFFD', '\u05BB', '\u05BC', '\u05BD', '\u05BE', '\u05BF',
        '\u05C0', '\u05C1', '\u05C2', '\u05C3', '\u05F0', '\u05F1', '\u05F2', '\u05F3',
        '\u05F4', '\uFFFD', '\uFFFD', '\uFFFD', '\uFFFD', '\uFFFD', '\uFFFD', '\uFFFD',
        '\u05D0', '\u05D1', '\u05D2', '\u05D3', '\u05D4', '\u05D5', '\u05D6', '\u05D7',
        '\u05D8', '\u05D9', '\u05DA', '\u05DB', '\u05DC', '\u05DD', '\u05DE', '\u05DF',
        '\u05E0', '\u05E1', '\u05E2', '\u05E3', '\u05E4', '\u05E5', '\u05E6', '\u05E7',
        '\u05E8', '\u05E9', '\u05EA', '\uFFFD', '\uFFFD', '\u200E', '\u200F', '\uFFFD'
    )

    private val WIN1256_TABLE: CharArray = charArrayOf(
        '\u20AC', '\u067E', '\u201A', '\u0192', '\u201E', '\u2026', '\u2020', '\u2021',
        '\u02C6', '\u2030', '\u0679', '\u2039', '\u0152', '\u0686', '\u0698', '\u0688',
        '\u06AF', '\u2018', '\u2019', '\u201C', '\u201D', '\u2022', '\u2013', '\u2014',
        '\u06A9', '\u2122', '\u0691', '\u203A', '\u0153', '\u200C', '\u200D', '\u06BA',
        '\u00A0', '\u060C', '\u00A2', '\u00A3', '\u00A4', '\u00A5', '\u00A6', '\u00A7',
        '\u00A8', '\u00A9', '\u06BE', '\u00AB', '\u00AC', '\u00AD', '\u00AE', '\u00AF',
        '\u00B0', '\u00B1', '\u00B2', '\u00B3', '\u00B4', '\u00B5', '\u00B6', '\u00B7',
        '\u00B8', '\u00B9', '\u061B', '\u00BB', '\u00BC', '\u00BD', '\u00BE', '\u061F',
        '\u06C1', '\u0621', '\u0622', '\u0623', '\u0624', '\u0625', '\u0626', '\u0627',
        '\u0628', '\u0629', '\u062A', '\u062B', '\u062C', '\u062D', '\u062E', '\u062F',
        '\u0630', '\u0631', '\u0632', '\u0633', '\u0634', '\u0635', '\u0636', '\u00D7',
        '\u0637', '\u0638', '\u0639', '\u063A', '\u0640', '\u0641', '\u0642', '\u0643',
        '\u00E0', '\u0644', '\u00E2', '\u0645', '\u0646', '\u0647', '\u0648', '\u00E7',
        '\u00E8', '\u00E9', '\u00EA', '\u00EB', '\u0649', '\u064A', '\u00EE', '\u00EF',
        '\u064B', '\u064C', '\u064D', '\u064E', '\u00F4', '\u064F', '\u0650', '\u00F7',
        '\u0651', '\u00F9', '\u0652', '\u00FB', '\u00FC', '\u200E', '\u200F', '\u06D2'
    )

    private val WIN1258_TABLE: CharArray = charArrayOf(
        '\u20AC', '\uFFFD', '\u201A', '\u0192', '\u201E', '\u2026', '\u2020', '\u2021',
        '\u02C6', '\u2030', '\uFFFD', '\u2039', '\u0152', '\uFFFD', '\uFFFD', '\uFFFD',
        '\uFFFD', '\u2018', '\u2019', '\u201C', '\u201D', '\u2022', '\u2013', '\u2014',
        '\u02DC', '\u2122', '\uFFFD', '\u203A', '\u0153', '\uFFFD', '\uFFFD', '\u0178',
        '\u00A0', '\u00A1', '\u00A2', '\u00A3', '\u00A4', '\u00A5', '\u00A6', '\u00A7',
        '\u00A8', '\u00A9', '\u00AA', '\u00AB', '\u00AC', '\u00AD', '\u00AE', '\u00AF',
        '\u00B0', '\u00B1', '\u00B2', '\u00B3', '\u00B4', '\u00B5', '\u00B6', '\u00B7',
        '\u00B8', '\u00B9', '\u00BA', '\u00BB', '\u00BC', '\u00BD', '\u00BE', '\u00BF',
        '\u00C0', '\u00C1', '\u00C2', '\u0102', '\u00C4', '\u00C5', '\u00C6', '\u00C7',
        '\u00C8', '\u00C9', '\u00CA', '\u00CB', '\u0300', '\u00CD', '\u00CE', '\u00CF',
        '\u0110', '\u00D1', '\u0309', '\u00D3', '\u00D4', '\u01A0', '\u00D6', '\u00D7',
        '\u00D8', '\u00D9', '\u00DA', '\u00DB', '\u00DC', '\u01AF', '\u0303', '\u00DF',
        '\u00E0', '\u00E1', '\u00E2', '\u0103', '\u00E4', '\u00E5', '\u00E6', '\u00E7',
        '\u00E8', '\u00E9', '\u00EA', '\u00EB', '\u0301', '\u00ED', '\u00EE', '\u00EF',
        '\u0111', '\u00F1', '\u0323', '\u00F3', '\u00F4', '\u01A1', '\u00F6', '\u00F7',
        '\u00F8', '\u00F9', '\u00FA', '\u00FB', '\u00FC', '\u01B0', '\u20AB', '\u00FF'
    )

    // Additional single-byte charset tables
    private val WIN1253_TABLE: CharArray = charArrayOf(
        '\u20AC', '\uFFFD', '\u201A', '\u0192', '\u201E', '\u2026', '\u2020', '\u2021',
        '\uFFFD', '\u2030', '\uFFFD', '\u2039', '\uFFFD', '\uFFFD', '\uFFFD', '\uFFFD',
        '\uFFFD', '\u2018', '\u2019', '\u201C', '\u201D', '\u2022', '\u2013', '\u2014',
        '\uFFFD', '\u2122', '\uFFFD', '\u203A', '\uFFFD', '\uFFFD', '\uFFFD', '\uFFFD',
        '\u00A0', '\u0385', '\u0386', '\u00A3', '\u00A4', '\u00A5', '\u00A6', '\u00A7',
        '\u00A8', '\u00A9', '\uFFFD', '\u00AB', '\u00AC', '\u00AD', '\u00AE', '\u2015',
        '\u00B0', '\u00B1', '\u00B2', '\u00B3', '\u0384', '\u00B5', '\u00B6', '\u00B7',
        '\u0388', '\u0389', '\u038A', '\u00BB', '\u038C', '\u00BD', '\u038E', '\u038F',
        '\u0390', '\u0391', '\u0392', '\u0393', '\u0394', '\u0395', '\u0396', '\u0397',
        '\u0398', '\u0399', '\u039A', '\u039B', '\u039C', '\u039D', '\u039E', '\u039F',
        '\u03A0', '\u03A1', '\uFFFD', '\u03A3', '\u03A4', '\u03A5', '\u03A6', '\u03A7',
        '\u03A8', '\u03A9', '\u03AA', '\u03AB', '\u03AC', '\u03AD', '\u03AE', '\u03AF',
        '\u03B0', '\u03B1', '\u03B2', '\u03B3', '\u03B4', '\u03B5', '\u03B6', '\u03B7',
        '\u03B8', '\u03B9', '\u03BA', '\u03BB', '\u03BC', '\u03BD', '\u03BE', '\u03BF',
        '\u03C0', '\u03C1', '\u03C2', '\u03C3', '\u03C4', '\u03C5', '\u03C6', '\u03C7',
        '\u03C8', '\u03C9', '\u03CA', '\u03CB', '\u03CC', '\u03CD', '\u03CE', '\uFFFD'
    )

    private val WIN874_TABLE: CharArray = charArrayOf(
        '\u20AC', '\uFFFD', '\uFFFD', '\uFFFD', '\uFFFD', '\u2026', '\uFFFD', '\uFFFD',
        '\uFFFD', '\uFFFD', '\uFFFD', '\uFFFD', '\uFFFD', '\uFFFD', '\uFFFD', '\uFFFD',
        '\uFFFD', '\u2018', '\u2019', '\u201C', '\u201D', '\u2022', '\u2013', '\u2014',
        '\uFFFD', '\uFFFD', '\uFFFD', '\uFFFD', '\uFFFD', '\uFFFD', '\uFFFD', '\uFFFD',
        '\u00A0', '\u0E01', '\u0E02', '\u0E03', '\u0E04', '\u0E05', '\u0E06', '\u0E07',
        '\u0E08', '\u0E09', '\u0E0A', '\u0E0B', '\u0E0C', '\u0E0D', '\u0E0E', '\u0E0F',
        '\u0E10', '\u0E11', '\u0E12', '\u0E13', '\u0E14', '\u0E15', '\u0E16', '\u0E17',
        '\u0E18', '\u0E19', '\u0E1A', '\u0E1B', '\u0E1C', '\u0E1D', '\u0E1E', '\u0E1F',
        '\u0E20', '\u0E21', '\u0E22', '\u0E23', '\u0E24', '\u0E25', '\u0E26', '\u0E27',
        '\u0E28', '\u0E29', '\u0E2A', '\u0E2B', '\u0E2C', '\u0E2D', '\u0E2E', '\u0E2F',
        '\u0E30', '\u0E31', '\u0E32', '\u0E33', '\u0E34', '\u0E35', '\u0E36', '\u0E37',
        '\u0E38', '\u0E39', '\u0E3A', '\uFFFD', '\uFFFD', '\uFFFD', '\uFFFD', '\u0E3F',
        '\u0E40', '\u0E41', '\u0E42', '\u0E43', '\u0E44', '\u0E45', '\u0E46', '\u0E47',
        '\u0E48', '\u0E49', '\u0E4A', '\u0E4B', '\u0E4C', '\u0E4D', '\u0E4E', '\u0E4F',
        '\u0E50', '\u0E51', '\u0E52', '\u0E53', '\u0E54', '\u0E55', '\u0E56', '\u0E57',
        '\u0E58', '\u0E59', '\u0E5A', '\u0E5B', '\uFFFD', '\uFFFD', '\uFFFD', '\uFFFD'
    )

    private val KOI8_R_TABLE: CharArray = charArrayOf(
        '\u2500', '\u2502', '\u250C', '\u2510', '\u2514', '\u2518', '\u251C', '\u2524',
        '\u252C', '\u2534', '\u253C', '\u2580', '\u2584', '\u2588', '\u258C', '\u2590',
        '\u2591', '\u2592', '\u2593', '\u2320', '\u25A0', '\u2219', '\u221A', '\u2248',
        '\u2264', '\u2265', '\u00A0', '\u2321', '\u00B0', '\u00B2', '\u00B7', '\u00F7',
        '\u2550', '\u2551', '\u2552', '\u0451', '\u2553', '\u2554', '\u2555', '\u2556',
        '\u2557', '\u2558', '\u2559', '\u255A', '\u255B', '\u255C', '\u255D', '\u255E',
        '\u255F', '\u2560', '\u2561', '\u0401', '\u2562', '\u2563', '\u2564', '\u2565',
        '\u2566', '\u2567', '\u2568', '\u2569', '\u256A', '\u256B', '\u256C', '\u00A9',
        '\u044E', '\u0430', '\u0431', '\u0446', '\u0434', '\u0435', '\u0444', '\u0433',
        '\u0445', '\u0438', '\u0439', '\u043A', '\u043B', '\u043C', '\u043D', '\u043E',
        '\u043F', '\u044F', '\u0440', '\u0441', '\u0442', '\u0443', '\u0436', '\u0432',
        '\u044C', '\u044B', '\u0437', '\u0448', '\u044D', '\u0449', '\u0447', '\u044A',
        '\u042E', '\u0410', '\u0411', '\u0426', '\u0414', '\u0415', '\u0424', '\u0413',
        '\u0425', '\u0418', '\u0419', '\u041A', '\u041B', '\u041C', '\u041D', '\u041E',
        '\u041F', '\u042F', '\u0420', '\u0421', '\u0422', '\u0423', '\u0416', '\u0412',
        '\u042C', '\u042B', '\u0417', '\u0428', '\u042D', '\u0429', '\u0427', '\u042A'
    )

    private const val MAX_SAMPLE_BYTES = 4096

    fun decode(
        bytes: ByteArray,
        offset: Int = 0,
        length: Int = bytes.size - offset,
        languageHint: String? = null
    ): String {
        if (length <= 0) return ""

        // Check for BOM
        if (length >= 3 && (bytes[offset].toInt() and 0xFF) == 0xEF &&
            (bytes[offset + 1].toInt() and 0xFF) == 0xBB &&
            (bytes[offset + 2].toInt() and 0xFF) == 0xBF
        ) {
            val utf8 = decodeUtf8(bytes, offset + 3, length - 3)
            return repairDoubleEncodedUtf8IfNeeded(utf8, languageHint)
        }
        if (length >= 2 && (bytes[offset].toInt() and 0xFF) == 0xFF &&
            (bytes[offset + 1].toInt() and 0xFF) == 0xFE
        ) {
            return decodeUtf16(bytes, offset + 2, length - 2, littleEndian = true)
        }
        if (length >= 2 && (bytes[offset].toInt() and 0xFF) == 0xFE &&
            (bytes[offset + 1].toInt() and 0xFF) == 0xFF
        ) {
            return decodeUtf16(bytes, offset + 2, length - 2, littleEndian = false)
        }

        if (isFastValidUtf8(bytes, offset, length)) {
            val utf8 = decodeUtf8(bytes, offset, length)
            return repairDoubleEncodedUtf8IfNeeded(utf8, languageHint)
        }

        val hintedCharset = resolveCharsetFromLanguageHint(bytes, offset, length, languageHint)
        val charset = hintedCharset ?: detectUniversalCharset(bytes, offset, length)
        val decoded = decodeWithCharset(bytes, offset, length, charset)
        return repairDoubleEncodedUtf8IfNeeded(decoded, languageHint)
    }

    fun normalizeToUtf8(
        bytes: ByteArray,
        offset: Int = 0,
        length: Int = bytes.size - offset,
        languageHint: String? = null
    ): ByteArray {
        val decoded = decode(bytes, offset, length, languageHint)
        // Encode to UTF-8 bytes via manual encoding
        val utf8Bytes = decoded.encodeToByteArray()
        return utf8Bytes
    }

    fun repairDoubleEncodedUtf8IfNeeded(text: String, languageHint: String? = null): String {
        if (text.isEmpty()) return text

        val normalized = languageHint?.trim()?.lowercase()?.substringBefore('-')?.substringBefore('_')
        val isHebrewHint = normalized == "heb" || normalized == "he" || normalized == "iw" ||
            normalized?.startsWith("hebrew") == true

        // Double-encoded UTF-8 subtitles (where Windows-1255 Hebrew bytes were decoded as Latin-1 and saved as UTF-8)
        // are an issue specific to Hebrew providers like Ktuvit.
        if (!isHebrewHint && !normalized.isNullOrBlank()) {
            return text
        }

        val words = text.split("\\s+".toRegex()).filter { it.length >= 2 && it.any { c -> c.isLetter() } }
        if (words.isEmpty()) return text

        // In true double-encoded Hebrew text, dialogue words consist almost exclusively of Latin-1 chars in \u00E0..\u00FA
        val mojibakeWordsCount = words.count { word ->
            word.all { c -> (c in '\u00E0'..'\u00FA') || c in "<i></i>-.,!?:'\"<>/\\_()" }
        }

        val threshold = if (isHebrewHint) 5 else 10
        if (mojibakeWordsCount >= threshold && (mojibakeWordsCount * 2 >= words.size)) {
            val win1252Bytes = encodeToWin1252(text)
            if (win1252Bytes == null) return text
            val hebrewCandidate = decodeWithCharset(win1252Bytes, 0, win1252Bytes.size, "windows-1255")
            val hebrewChars = hebrewCandidate.count { it in '\u0590'..'\u05FF' }
            if (hebrewChars >= 10) {
                return hebrewCandidate
            }
        }

        return text
    }

    private fun resolveCharsetFromLanguageHint(
        bytes: ByteArray,
        offset: Int,
        length: Int,
        languageHint: String?
    ): String? {
        if (languageHint.isNullOrBlank()) return null
        val normalized = languageHint.trim().lowercase()
        val lang = normalized.substringBefore('-').substringBefore('_')

        return when {
            lang == "heb" || lang == "he" || lang == "iw" || lang.startsWith("hebrew") -> "windows-1255"
            lang == "ara" || lang == "ar" || lang.startsWith("arabic") -> "windows-1256"
            lang == "ell" || lang == "el" || lang == "gre" || lang.startsWith("greek") -> "windows-1253"
            lang == "tur" || lang == "tr" || lang.startsWith("turkish") -> "windows-1254"
            lang == "rus" || lang == "ru" || lang == "ukr" || lang == "uk" || lang == "bel" || lang == "be" ||
                lang == "bul" || lang == "bg" || lang == "mkd" || lang == "mk" || lang == "srp" || lang == "sr" ||
                lang.startsWith("russian") || lang.startsWith("ukrainian") || lang.startsWith("bulgarian") ||
                lang.startsWith("serbian") || lang.startsWith("cyrillic") -> {
                if (hasKoi8Vowels(bytes, offset, length)) "KOI8-R" else "windows-1251"
            }
            lang == "por" || lang == "pt" || lang.startsWith("portuguese") ||
                lang == "spa" || lang == "es" || lang.startsWith("spanish") ||
                lang == "fra" || lang == "fre" || lang == "fr" || lang.startsWith("french") ||
                lang == "deu" || lang == "ger" || lang == "de" || lang.startsWith("german") ||
                lang == "ita" || lang == "it" || lang.startsWith("italian") ||
                lang == "nld" || lang == "dut" || lang == "nl" || lang.startsWith("dutch") ||
                lang == "eng" || lang == "en" || lang.startsWith("english") ||
                lang == "dan" || lang == "da" || lang.startsWith("danish") ||
                lang == "swe" || lang == "sv" || lang.startsWith("swedish") ||
                lang == "nor" || lang == "no" || lang.startsWith("norwegian") ||
                lang == "fin" || lang == "fi" || lang.startsWith("finnish") ||
                lang == "cat" || lang == "ca" || lang.startsWith("catalan") ||
                lang == "glg" || lang == "gl" || lang.startsWith("galician") ||
                lang == "eus" || lang == "baq" || lang == "eu" || lang.startsWith("basque") -> "windows-1252"
            lang == "tha" || lang == "th" || lang.startsWith("thai") -> "windows-874"
            lang == "vie" || lang == "vi" || lang.startsWith("vietnamese") -> "windows-1258"
            lang == "pol" || lang == "pl" || lang == "ces" || lang == "cs" || lang == "cze" || lang == "hun" ||
                lang == "hu" || lang == "slv" || lang == "sl" || lang == "hrv" || lang == "hr" || lang == "ron" ||
                lang == "ro" || lang == "rum" || lang == "slk" || lang == "sk" || lang.startsWith("polish") ||
                lang.startsWith("czech") || lang.startsWith("hungarian") || lang.startsWith("romanian") ||
                lang.startsWith("croatian") || lang.startsWith("slovak") || lang.startsWith("slovenian") -> "windows-1250"
            lang == "zho" || lang == "zh" || lang == "chi" || lang.startsWith("chinese") -> {
                if (normalized.contains("tw") || normalized.contains("hk") || normalized.contains("traditional") || normalized.contains("hant")) {
                    "Big5"
                } else if (normalized.contains("cn") || normalized.contains("sg") || normalized.contains("simplified") || normalized.contains("hans")) {
                    "GB18030"
                } else {
                    if (isCjkClean(bytes, offset, length, "Big5")) "Big5" else "GB18030"
                }
            }
            lang == "jpn" || lang == "ja" || lang.startsWith("japanese") -> "Shift_JIS"
            lang == "kor" || lang == "ko" || lang.startsWith("korean") -> "EUC-KR"
            else -> null
        }
    }

    private fun hasKoi8Vowels(bytes: ByteArray, offset: Int, length: Int): Boolean {
        val sampleLength = min(length, MAX_SAMPLE_BYTES)
        val end = offset + sampleLength
        var russianVowels = 0
        var koi8Vowels = 0
        for (i in offset until end) {
            val b = bytes[i].toInt() and 0xFF
            if (b == 0xEE || b == 0xE0 || b == 0xE5 || b == 0xE8 || b == 0xFF || b == 0xFB) russianVowels++
            if (b == 0xCF || b == 0xC1 || b == 0xC5 || b == 0xC9 || b == 0xD5 || b == 0xDF) koi8Vowels++
        }
        return koi8Vowels > russianVowels && koi8Vowels >= 3
    }

    private fun isFastValidUtf8(bytes: ByteArray, offset: Int, length: Int): Boolean {
        var i = offset
        val end = offset + length
        while (i < end) {
            val b1 = bytes[i++].toInt() and 0xFF
            if (b1 <= 0x7F) continue
            if (b1 in 0xC2..0xDF) {
                if (i >= end || (bytes[i++].toInt() and 0xC0) != 0x80) return false
            } else if (b1 in 0xE0..0xEF) {
                if (i + 1 >= end || (bytes[i++].toInt() and 0xC0) != 0x80 || (bytes[i++].toInt() and 0xC0) != 0x80) return false
            } else if (b1 in 0xF0..0xF4) {
                if (i + 2 >= end || (bytes[i++].toInt() and 0xC0) != 0x80 || (bytes[i++].toInt() and 0xC0) != 0x80 || (bytes[i++].toInt() and 0xC0) != 0x80) return false
            } else {
                return false
            }
        }
        return true
    }

    private fun detectUniversalCharset(bytes: ByteArray, offset: Int, length: Int): String {
        var startOffset = offset
        val totalEnd = offset + length
        while (startOffset < totalEnd && (bytes[startOffset].toInt() and 0xFF) < 0x80) {
            startOffset++
        }
        if (startOffset >= totalEnd) return "UTF-8"

        val sampleOffset = if (startOffset > offset) (startOffset - min(100, startOffset - offset)) else offset
        val sampleLength = min(totalEnd - sampleOffset, MAX_SAMPLE_BYTES)
        val end = sampleOffset + sampleLength

        var asciiLetters = 0
        var nonAscii = 0
        var consecutiveNonAscii = 0
        var maxConsecutiveNonAscii = 0

        for (i in sampleOffset until end) {
            val b = bytes[i].toInt() and 0xFF
            if ((b in 'a'.code..'z'.code) || (b in 'A'.code..'Z'.code)) {
                asciiLetters++
                consecutiveNonAscii = 0
            } else if (b >= 0x80) {
                nonAscii++
                consecutiveNonAscii++
                if (consecutiveNonAscii > maxConsecutiveNonAscii) {
                    maxConsecutiveNonAscii = consecutiveNonAscii
                }
            } else {
                consecutiveNonAscii = 0
            }
        }

        if (nonAscii == 0) return "UTF-8"

        // Hebrew words can coincidentally form valid GB18030 pairs. Prefer the strong
        // single-byte signature before probing CJK when every high byte is a Hebrew letter.
        val highBytes = (sampleOffset until end).map { bytes[it].toInt() and 0xFF }.filter { it >= 0x80 }
        if (highBytes.size >= 4 && highBytes.all { it in 0xE0..0xFA } && asciiLetters < nonAscii) {
            return "windows-1255"
        }

        // Japanese Shift_JIS check (Hiragana in 0x82 0x9F..0xF1)
        var hiraganaCount = 0
        var sjisIdx = sampleOffset
        while (sjisIdx < end - 1) {
            val b1 = bytes[sjisIdx].toInt() and 0xFF
            val b2 = bytes[sjisIdx + 1].toInt() and 0xFF
            if (b1 == 0x82 && b2 in 0x9F..0xF1) {
                hiraganaCount++
                sjisIdx++
            }
            sjisIdx++
        }
        if (hiraganaCount >= 2) {
            return "Shift_JIS"
        }

        // Korean EUC-KR syllables check (Hangul block match)
        val hangulScore = countBlockMatches(bytes, sampleOffset, sampleLength, "EUC-KR",
            unicodeRangeStart = 0xAC00, unicodeRangeEnd = 0xD7AF) // Hangul Syllables
        val gbHanziScore = countBlockMatches(bytes, sampleOffset, sampleLength, "GB18030",
            unicodeRangeStart = 0x4E00, unicodeRangeEnd = 0x9FFF) // CJK Unified Ideographs

        if (hangulScore >= 4 && hangulScore >= gbHanziScore && maxConsecutiveNonAscii >= 4) {
            return "EUC-KR"
        }

        // Thai single-byte consonants check (0xA1..0xBF)
        var thaiConsonantsEarly = 0
        for (i in sampleOffset until end) {
            val b = bytes[i].toInt() and 0xFF
            if (b in 0xA1..0xBF) thaiConsonantsEarly++
        }
        if (thaiConsonantsEarly * 4 >= nonAscii && thaiConsonantsEarly >= 4) {
            return "windows-874"
        }

        if (gbHanziScore >= 4 && maxConsecutiveNonAscii >= 6) {
            var big5TrailCount = 0
            var i = sampleOffset
            while (i < end - 1) {
                val b1 = bytes[i].toInt() and 0xFF
                val b2 = bytes[i + 1].toInt() and 0xFF
                if (b1 in 0xA1..0xF9 && b2 in 0x40..0x7E) {
                    big5TrailCount++
                    i++
                }
                i++
            }
            if (big5TrailCount >= 2 && isCjkClean(bytes, sampleOffset, sampleLength, "Big5")) {
                return "Big5"
            }
            return "GB18030"
        }

        // Latin-character based charset detection
        if (asciiLetters >= nonAscii || maxConsecutiveNonAscii <= 2) {
            var turkishScore = 0
            var ceScore = 0
            var vietnameseScore = 0
            for (i in sampleOffset until end) {
                val b = bytes[i].toInt() and 0xFF
                if (b == 0xCC || b == 0xD2 || b == 0xF2 || b == 0xF5) vietnameseScore++
                if (b == 0xF0 || b == 0xFE || b == 0xFD || b == 0xD0 || b == 0xDE || b == 0xDD) turkishScore++
                if (b == 0xB9 || b == 0xB3 || b == 0x9C || b == 0x9F || b == 0x9A || b == 0x9E || b == 0x8C || b == 0x8F || b == 0x8A || b == 0x8E || b == 0x8D || b == 0x9D || b == 0xCF || b == 0xEF || b == 0xBE) ceScore++
            }
            if (turkishScore >= 2 && turkishScore > ceScore) return "windows-1254"
            if (ceScore >= 2 && ceScore >= vietnameseScore) return "windows-1250"
            if (vietnameseScore >= 2) return "windows-1258"
            return "windows-1252"
        }

        // Statistical evaluation for non-Latin single-byte alphabets
        var thaiConsonants = 0
        var hebrewLetters = 0
        var arabicAlCount = 0
        var russianVowels = 0
        var koi8Vowels = 0
        var greekVowels = 0
        var bytes0xC0to0xDF = 0

        for (i in sampleOffset until end) {
            val b = bytes[i].toInt() and 0xFF
            if (b in 0xA1..0xBF) thaiConsonants++
            if (b in 0xE0..0xFA) hebrewLetters++
            if (b in 0xC0..0xDF) bytes0xC0to0xDF++
            if (b == 0xEE || b == 0xE0 || b == 0xE5 || b == 0xE8 || b == 0xFF || b == 0xFB || b == 0xF3 || b == 0xFD || b == 0xFE) russianVowels++
            if (b == 0xCF || b == 0xC1 || b == 0xC5 || b == 0xC9 || b == 0xD5 || b == 0xDF) koi8Vowels++
            if (b == 0xE1 || b == 0xEF || b == 0xE5 || b == 0xE7 || b == 0xFD || b == 0xFE || b == 0xE9 || b == 0xF5 || b == 0xF9 || b == 0xDC || b == 0xDD || b == 0xDE || b == 0xDF || b == 0xFA || b == 0xFB || b == 0xFC) greekVowels++
            if (i < end - 1 && b == 0xC7 && (bytes[i + 1].toInt() and 0xFF) == 0xE1) arabicAlCount++
        }

        // Hebrew check: consonants (0xE0..0xFA) make up all non-ASCII bytes
        if (hebrewLetters == nonAscii && hebrewLetters >= 4 && bytes0xC0to0xDF == 0) {
            return "windows-1255"
        }

        // Thai check
        if (thaiConsonants * 4 >= nonAscii && thaiConsonants >= 4) {
            return "windows-874"
        }

        // Arabic check
        if (arabicAlCount > 0) {
            return "windows-1256"
        }

        // Cyrillic check
        if (koi8Vowels > russianVowels && koi8Vowels >= 3) {
            return "KOI8-R"
        }
        if (russianVowels > greekVowels && russianVowels >= 4) {
            return "windows-1251"
        }

        // Greek check
        if (greekVowels > russianVowels && greekVowels >= 4) {
            return "windows-1253"
        }

        // Turkish / Central European / Vietnamese / Western European fallback
        var turkishScore = 0
        var ceScore = 0
        var vietnameseScore = 0
        for (i in sampleOffset until end) {
            val b = bytes[i].toInt() and 0xFF
            if (b == 0xCC || b == 0xD2 || b == 0xF2 || b == 0xF5) vietnameseScore++
            if (b == 0xF0 || b == 0xFE || b == 0xFD || b == 0xD0 || b == 0xDE || b == 0xDD) turkishScore++
            if (b == 0xB9 || b == 0xB3 || b == 0x9C || b == 0x9F || b == 0x9A || b == 0x9E || b == 0x8C || b == 0x8F || b == 0x8A || b == 0x8E || b == 0x8D || b == 0x9D || b == 0xCF || b == 0xEF || b == 0xBE) ceScore++
        }
        if (turkishScore > ceScore && turkishScore > 0) return "windows-1254"
        if (ceScore > 0 && ceScore >= vietnameseScore) return "windows-1250"
        if (vietnameseScore >= 2) return "windows-1258"

        return "windows-1252"
    }

    private fun isCjkClean(bytes: ByteArray, offset: Int, length: Int, cs: String): Boolean {
        return try {
            val decoded = decodeWithCharset(bytes, offset, length, cs)
            // Check for replacement character U+FFFD which indicates decoding failure
            !decoded.contains('\uFFFD')
        } catch (_: Exception) {
            false
        }
    }

    private fun countBlockMatches(
        bytes: ByteArray, offset: Int, length: Int, cs: String,
        unicodeRangeStart: Int, unicodeRangeEnd: Int
    ): Int {
        return try {
            val decoded = decodeWithCharset(bytes, offset, length, cs)
            var matched = 0
            for (i in decoded.indices) {
                val code = decoded[i].code
                if (code in unicodeRangeStart..unicodeRangeEnd) {
                    matched++
                }
            }
            matched
        } catch (_: Exception) {
            0
        }
    }

    // ---- Decoding helpers ----

    private fun decodeUtf8(bytes: ByteArray, offset: Int, length: Int): String {
        return bytes.copyOfRange(offset, offset + length).decodeToString()
    }

    private fun decodeUtf16(bytes: ByteArray, offset: Int, length: Int, littleEndian: Boolean): String {
        val chars = mutableListOf<Char>()
        var i = offset
        val end = offset + (length - (length % 2))
        while (i < end) {
            val low = bytes[i].toInt() and 0xFF
            val high = bytes[i + 1].toInt() and 0xFF
            val codePoint = if (littleEndian) (high shl 8) or low else (low shl 8) or high
            chars.add(codePoint.toChar())
            i += 2
        }
        return chars.joinToString("")
    }

    private fun getCharsetTable(charset: String): CharArray? = when (charset) {
        "windows-1252" -> WIN1252_TABLE
        "windows-1250" -> WIN1250_TABLE
        "windows-1251" -> WIN1251_TABLE
        "windows-1254" -> WIN1254_TABLE
        "windows-1255" -> WIN1255_TABLE
        "windows-1256" -> WIN1256_TABLE
        "windows-1258" -> WIN1258_TABLE
        "windows-1253" -> WIN1253_TABLE
        "windows-874" -> WIN874_TABLE
        "KOI8-R" -> KOI8_R_TABLE
        else -> null
    }

    /**
     * Decodes bytes to string using the specified charset.
     * Single-byte charsets use built-in lookup tables.
     * Multi-byte charsets delegate to [platformDecode].
     */
    private fun decodeWithCharset(bytes: ByteArray, offset: Int, length: Int, charset: String): String {
        val table = getCharsetTable(charset)
        if (table != null) {
            // Single-byte charset: use lookup table
            val sb = StringBuilder(length)
            val end = offset + length
            for (i in offset until end) {
                val b = bytes[i].toInt() and 0xFF
                sb.append(if (b < 0x80) b.toChar() else table[b - 0x80])
            }
            return sb.toString()
        }
        // A failed native probe must not look like successfully decoded CJK text.
        return decodeSubtitleCharset(bytes, offset, length, charset).orEmpty()
    }

    // Encode a string to Windows-1252 bytes (for double-encoded Hebrew repair)
    private fun encodeToWin1252(text: String): ByteArray? {
        try {
            val bytes = ByteArray(text.length)
            for (i in text.indices) {
                val c = text[i]
                val b = when (c.code) {
                    in 0x00..0x7F -> c.code
                    in 0xA0..0xFF -> c.code // Same in Unicode and Win1252
                    0x20AC -> 0x80
                    0x201A -> 0x82
                    0x0192 -> 0x83
                    0x201E -> 0x84
                    0x2026 -> 0x85
                    0x2020 -> 0x86
                    0x2021 -> 0x87
                    0x02C6 -> 0x88
                    0x2030 -> 0x89
                    0x0160 -> 0x8A
                    0x2039 -> 0x8B
                    0x0152 -> 0x8C
                    0x017D -> 0x8E
                    0x2018 -> 0x91
                    0x2019 -> 0x92
                    0x201C -> 0x93
                    0x201D -> 0x94
                    0x2022 -> 0x95
                    0x2013 -> 0x96
                    0x2014 -> 0x97
                    0x02DC -> 0x98
                    0x2122 -> 0x99
                    0x0161 -> 0x9A
                    0x203A -> 0x9B
                    0x0153 -> 0x9C
                    0x017E -> 0x9E
                    0x0178 -> 0x9F
                    else -> return null // Unmappable character
                }
                bytes[i] = b.toByte()
            }
            return bytes
        } catch (_: Exception) {
            return null
        }
    }
}
