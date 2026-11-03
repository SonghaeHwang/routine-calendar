package com.kikilog.routinecalendar.core.model

/**
 * 분류 색 계열. 계열마다 진한 색 → 연한 색 5단계. 화면에서는 계열을 고른 뒤 단계를 고른다.
 * 값은 Material 색표에서 가져왔다(900 · 700 · 500 · 300 · 100).
 *
 * [DEFAULT_CATEGORIES] 와 같은 파일에 두면 클래스 초기화가 서로 물려 NullPointerException 이 나므로 따로 둔다.
 */
enum class ColorFamily(val label: String, val shades: List<Int>) {
    RED("빨강", argb(0xB71C1C, 0xD32F2F, 0xF44336, 0xE57373, 0xFFCDD2)),
    ORANGE("주황", argb(0xE65100, 0xF57C00, 0xFF9800, 0xFFB74D, 0xFFE0B2)),
    YELLOW("노랑", argb(0xF57F17, 0xFBC02D, 0xFFEB3B, 0xFFF176, 0xFFF9C4)),
    GREEN("초록", argb(0x33691E, 0x689F38, 0x8BC34A, 0xAED581, 0xDCEDC8)),
    BLUE("파랑", argb(0x0D47A1, 0x1976D2, 0x2196F3, 0x64B5F6, 0xBBDEFB)),
    NAVY("남색", argb(0x1A237E, 0x303F9F, 0x3F51B5, 0x7986CB, 0xC5CAE9)),
    PURPLE("보라", argb(0x4A148C, 0x7B1FA2, 0x9C27B0, 0xBA68C8, 0xE1BEE7)),
    PINK("분홍", argb(0x880E4F, 0xC2185B, 0xE91E63, 0xF06292, 0xF8BBD0)),
    GRAY("회색", argb(0x212121, 0x616161, 0x9E9E9E, 0xBDBDBD, 0xE0E0E0)),
    ;

    /** 자동 배정 · 처음 고를 때의 색(가운데 단계) */
    val defaultColor: Int get() = shades[2]

    companion object {
        /** 이 색이 속한 계열. 목록에 없는 색이면 null */
        fun of(colorArgb: Int): ColorFamily? = entries.firstOrNull { colorArgb in it.shades }
    }
}

private fun argb(vararg rgb: Int): List<Int> = rgb.map { (0xFF shl 24) or it }
