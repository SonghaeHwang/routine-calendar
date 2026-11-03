package com.kikilog.routinecalendar.ui.edit

import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.Interval
import com.kikilog.routinecalendar.core.model.IntervalUnit
import kotlinx.datetime.LocalDate

enum class TitleError { EMPTY }

enum class AmountError { INVALID, TOO_LARGE }

/** 주기 숫자의 최댓값. 개월 단위로 펼칠 때 숫자가 지나치게 커지지 않게 막는다. */
const val MAX_INTERVAL_AMOUNT = 365

/**
 * 등록 · 수정 화면 상태. 주기 숫자는 입력 칸 글자 그대로([amountText]) 들고 있다가 저장할 때 검사한다.
 * @property routineId null 이면 등록, 아니면 수정
 * @property hasCompletions 수정할 루틴에 완료 기록이 있는지. 있으면 처음 할 날 칸을 숨긴다
 * @property repeatsLocked 기록이 있는 반복 루틴 — 반복을 끄면 저장하자마자 끝남이 되므로 잠근다
 * @property firstDue 수정 모드에서 기록이 없을 때만 보이는 처음 할 날
 * @property showErrors 저장을 한 번 누른 뒤부터 오류를 보여 준다
 */
data class EditUiState(
    val today: LocalDate,
    val routineId: Long? = null,
    val isLoading: Boolean = false,
    val notFound: Boolean = false,
    val hasCompletions: Boolean = false,
    val repeatsLocked: Boolean = false,
    val firstDue: LocalDate? = null,
    /** 수정 중인 루틴이 보관됐는지(더보기 메뉴 문구에 쓴다) */
    val archived: Boolean = false,
    /** 보관 · 삭제로 화면을 닫아야 할 때 */
    val closed: Boolean = false,
    val title: String = "",
    val category: Category = Category.HOUSE,
    val amountText: String = "1",
    val unit: IntervalUnit = IntervalUnit.WEEK,
    val repeats: Boolean = true,
    val notify: Boolean = true,
    val lastDoneOn: LocalDate? = null,
    val showErrors: Boolean = false,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    /** 저장한 루틴의 다음 예정일. 저장 후 안내 문구에 쓴다 */
    val savedNextDue: LocalDate? = null,
    val saveFailed: Boolean = false,
) {
    val titleError: TitleError? get() = if (title.isBlank()) TitleError.EMPTY else null

    val amountError: AmountError?
        get() {
            val amount = amountText.toIntOrNull() ?: return AmountError.INVALID
            return when {
                amount < 1 -> AmountError.INVALID
                amount > MAX_INTERVAL_AMOUNT -> AmountError.TOO_LARGE
                else -> null
            }
        }

    val isValid: Boolean get() = titleError == null && amountError == null

    val isEdit: Boolean get() = routineId != null

    /** 검사를 통과했을 때만 쓴다 */
    val interval: Interval? get() = amountText.toIntOrNull()?.takeIf { amountError == null }?.let { Interval(it, unit) }
}
