package com.kikilog.routinecalendar.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/** 루틴과 완료 기록을 함께 다룬다. 테이블이 둘뿐이라 DAO 하나로 둔다. */
@Dao
abstract class RoutineDao {

    @Insert
    abstract suspend fun insertRoutine(routine: RoutineEntity): Long

    /** 같은 루틴 · 같은 날이 이미 있으면 무시하고 -1 을 돌려준다. 없는 루틴이면 FK 위반으로 예외가 난다. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertCompletion(completion: CompletionEntity): Long

    /** 등록: 루틴과 초기 완료 기록을 한 트랜잭션으로 저장한다. */
    @Transaction
    open suspend fun insertWithInitialCompletion(
        routine: RoutineEntity,
        initialDoneOn: LocalDate?,
        createdAt: Long,
    ): Long {
        val id = insertRoutine(routine)
        if (initialDoneOn != null) {
            insertCompletion(CompletionEntity(routineId = id, doneOn = initialDoneOn, createdAt = createdAt))
        }
        return id
    }

    @Update(entity = RoutineEntity::class)
    abstract suspend fun updateRoutine(edit: RoutineEditColumns): Int

    @Query("UPDATE routine SET first_due = :firstDue WHERE id = :id")
    abstract suspend fun setFirstDue(id: Long, firstDue: LocalDate): Int

    /**
     * 수정. [firstDue] 는 완료 기록이 없을 때만 반영한다 — 기록이 있으면 다음 예정일은 기록으로 계산되므로.
     * 루틴 수정과 한 트랜잭션이다.
     */
    @Transaction
    open suspend fun updateWithFirstDue(edit: RoutineEditColumns, firstDue: LocalDate?): Int {
        val updated = updateRoutine(edit)
        if (updated > 0 && firstDue != null && completionsOf(edit.id).isEmpty()) {
            setFirstDue(edit.id, firstDue)
        }
        return updated
    }

    @Query("UPDATE routine SET archived = :archived WHERE id = :id")
    abstract suspend fun setArchived(id: Long, archived: Boolean): Int

    /** 완료 기록은 CASCADE 로 함께 지워진다. */
    @Query("DELETE FROM routine WHERE id = :id")
    abstract suspend fun deleteRoutine(id: Long): Int

    /** 여러 개를 한 번에(선택 삭제 · 전체 삭제). 완료 기록도 함께 지워진다. */
    @Query("DELETE FROM routine WHERE id IN (:ids)")
    abstract suspend fun deleteRoutines(ids: List<Long>): Int

    @Query("UPDATE routine SET archived = :archived WHERE id IN (:ids)")
    abstract suspend fun setArchived(ids: List<Long>, archived: Boolean): Int

    /** 설정 → 알림에 넣을 루틴 */
    @Query("UPDATE routine SET notify = :notify WHERE id IN (:ids)")
    abstract suspend fun setNotify(ids: List<Long>, notify: Boolean): Int

    @Query("DELETE FROM completion WHERE routine_id = :routineId AND done_on = :doneOn")
    abstract suspend fun deleteCompletion(routineId: Long, doneOn: LocalDate): Int

    @Transaction
    @Query("SELECT * FROM routine WHERE archived = 0 OR :includeArchived ORDER BY id")
    abstract fun observeWithCompletions(includeArchived: Boolean): Flow<List<RoutineWithCompletionsRow>>

    @Transaction
    @Query("SELECT * FROM routine WHERE id = :id")
    abstract fun observeOneWithCompletions(id: Long): Flow<RoutineWithCompletionsRow?>

    @Query("SELECT * FROM completion WHERE routine_id = :routineId ORDER BY done_on")
    abstract suspend fun completionsOf(routineId: Long): List<CompletionEntity>

    @Query("SELECT * FROM routine WHERE id = :id")
    abstract suspend fun routine(id: Long): RoutineEntity?
}
