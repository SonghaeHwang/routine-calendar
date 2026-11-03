package com.kikilog.routinecalendar.data

import com.kikilog.routinecalendar.core.schedule.initialSchedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

/**
 * 루틴 저장소. 다음 예정일은 저장하지 않고, 화면이 [observeRoutines] 결과에 `:core` 계산을 얹어 쓴다.
 *
 * @param today 오늘 날짜(기기 시간대). 시험에서는 고정값을 넣는다
 * @param now 현재 시각 epoch millis (created_at)
 */
class RoutineRepository(
    private val dao: RoutineDao,
    private val today: () -> LocalDate,
    private val now: () -> Long,
) {

    /** 오늘 화면 · 루틴 목록 · 캘린더 공용. 루틴과 완료 기록 전체를 함께 흘려보낸다. */
    fun observeRoutines(includeArchived: Boolean = false): Flow<List<RoutineWithCompletions>> =
        dao.observeWithCompletions(includeArchived).map { rows -> rows.map { it.toModel() } }

    fun observeRoutine(id: Long): Flow<RoutineWithCompletions?> =
        dao.observeOneWithCompletions(id).map { it?.toModel() }

    /**
     * 등록. [lastDoneOn] 이 있으면 첫 예정일은 그날 + 주기이고, 반복하는 루틴이면 그날을 완료 기록으로 함께 저장한다.
     * @throws IllegalArgumentException 제목이 비었거나 [lastDoneOn] 이 오늘보다 늦을 때
     */
    suspend fun add(draft: RoutineDraft, lastDoneOn: LocalDate?): Long {
        val valid = draft.validated()
        val schedule = initialSchedule(valid.interval, valid.repeats, today(), lastDoneOn)
        val createdAt = now()
        return dao.insertWithInitialCompletion(
            routine = valid.toEntity(schedule.firstDue, createdAt),
            initialDoneOn = schedule.initialDoneOn,
            createdAt = createdAt,
        )
    }

    /**
     * 이름 · 분류 · 주기 · 반복 · 알림을 바꾼다. 기록은 그대로. 바꿨으면 true.
     * [firstDue] 는 완료 기록이 없을 때만 반영한다(자동 재계산은 하지 않는다 — 개월 빼기는 되돌릴 수 없어서).
     * @throws IllegalArgumentException 제목이 비었거나, 기록이 있는 반복 루틴의 반복을 끄려 할 때(저장하자마자 끝남이 되므로)
     */
    suspend fun update(id: Long, draft: RoutineDraft, firstDue: LocalDate? = null): Boolean {
        val valid = draft.validated()
        val current = dao.routine(id) ?: return false
        if (current.repeats && !valid.repeats) {
            require(dao.completionsOf(id).isEmpty()) { "이미 한 기록이 있는 루틴은 반복을 끌 수 없습니다" }
        }
        return dao.updateWithFirstDue(valid.toEditColumns(id), firstDue) > 0
    }

    suspend fun setArchived(id: Long, archived: Boolean): Boolean =
        dao.setArchived(id, archived) > 0

    /** 완료 기록도 함께 지워진다. */
    suspend fun delete(id: Long): Boolean =
        dao.deleteRoutine(id) > 0

    /** 여러 개를 한 번에 지운다(선택 삭제 · 전체 삭제). 지운 개수를 돌려준다 */
    suspend fun delete(ids: Collection<Long>): Int =
        if (ids.isEmpty()) 0 else dao.deleteRoutines(ids.toList())

    /** 여러 개를 한 번에 보관 · 보관 해제 */
    suspend fun setArchived(ids: Collection<Long>, archived: Boolean): Int =
        if (ids.isEmpty()) 0 else dao.setArchived(ids.toList(), archived)

    /**
     * 완료 체크. 새로 기록했으면 true, 그날 이미 있으면 false.
     * @throws IllegalArgumentException [doneOn] 이 오늘보다 늦을 때
     */
    suspend fun check(routineId: Long, doneOn: LocalDate = today()): Boolean {
        require(doneOn <= today()) { "아직 오지 않은 날은 체크할 수 없습니다: $doneOn" }
        val rowId = dao.insertCompletion(
            CompletionEntity(routineId = routineId, doneOn = doneOn, createdAt = now()),
        )
        return rowId != -1L
    }

    /** 체크 취소. 지웠으면 true. 다음 예정일은 Flow 를 통해 다시 계산된다. */
    suspend fun uncheck(routineId: Long, doneOn: LocalDate): Boolean =
        dao.deleteCompletion(routineId, doneOn) > 0

    private fun RoutineDraft.validated(): RoutineDraft {
        val trimmed = title.trim()
        require(trimmed.isNotEmpty()) { "루틴 이름이 비어 있습니다" }
        return copy(title = trimmed)
    }
}
