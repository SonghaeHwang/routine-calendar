package com.kikilog.routinecalendar.core.preset

import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.Interval
import com.kikilog.routinecalendar.core.model.IntervalUnit.DAY
import com.kikilog.routinecalendar.core.model.IntervalUnit.MONTH
import com.kikilog.routinecalendar.core.model.IntervalUnit.WEEK

/**
 * 등록 화면의 빠른 선택. 고르면 값이 입력 칸에 복사되고 사용자가 고칠 수 있다.
 * 목록 자체도 설정에서 고칠 수 있어 저장소에 둔다. [DEFAULT_PRESETS] 는 처음 채우는 값이다.
 * @property interval null 이면 주기를 직접 입력한다(리들샷)
 * @property id 저장소의 번호. 기본값 목록은 0
 */
data class Preset(
    val title: String,
    val category: Category,
    val interval: Interval?,
    val repeats: Boolean = true,
    val id: Long = 0,
)

/** PLAN.md "프리셋" 표. 분류 순서(집안일 → 뷰티 → 구매)대로 화면에 보인다. */
val DEFAULT_PRESETS: List<Preset> = listOf(
    Preset("청소기", Category.HOUSE, Interval(1, WEEK)),
    Preset("빨래", Category.HOUSE, Interval(1, WEEK)),
    Preset("화장실 청소", Category.HOUSE, Interval(2, WEEK)),
    Preset("이불 세탁", Category.HOUSE, Interval(2, MONTH)),
    Preset("세탁조 청소", Category.HOUSE, Interval(1, MONTH)),
    Preset("냉장고 정리", Category.HOUSE, Interval(1, MONTH)),
    Preset("분리수거", Category.HOUSE, Interval(1, WEEK)),

    Preset("모델링팩", Category.BEAUTY, Interval(1, WEEK)),
    Preset("젤팩", Category.BEAUTY, Interval(1, WEEK)),
    Preset("각질제거", Category.BEAUTY, Interval(10, DAY)),
    Preset("리들샷", Category.BEAUTY, interval = null),

    Preset("화장지", Category.SUPPLY, Interval(2, MONTH)),
    Preset("세제", Category.SUPPLY, Interval(2, MONTH)),
    Preset("칫솔 교체", Category.SUPPLY, Interval(3, MONTH)),
)
