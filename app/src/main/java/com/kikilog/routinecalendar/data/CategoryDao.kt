package com.kikilog.routinecalendar.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CategoryDao {

    @Query("SELECT * FROM category ORDER BY position, id")
    abstract fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM category ORDER BY position, id")
    abstract suspend fun all(): List<CategoryEntity>

    @Query("SELECT * FROM category WHERE id = :id")
    abstract suspend fun byId(id: Long): CategoryEntity?

    @Query("SELECT COALESCE(MAX(position), -1) FROM category")
    abstract suspend fun maxPosition(): Int

    @Insert
    abstract suspend fun insert(category: CategoryEntity): Long

    @Query("UPDATE category SET name = :name, color = :color WHERE id = :id")
    abstract suspend fun update(id: Long, name: String, color: Int): Int

    @Query("UPDATE category SET position = :position WHERE id = :id")
    protected abstract suspend fun setPosition(id: Long, position: Int)

    /** [ids] 순서대로 position 을 0, 1, 2… 로 다시 매긴다(한 트랜잭션) */
    @Transaction
    open suspend fun reorder(ids: List<Long>) {
        ids.forEachIndexed { index, id -> setPosition(id, index) }
    }

    /** 이 분류에 든 루틴(보관 포함)과 빠른 선택 수 */
    @Query(
        "SELECT (SELECT COUNT(*) FROM routine WHERE category_id = :id) + (SELECT COUNT(*) FROM preset WHERE category_id = :id)",
    )
    abstract suspend fun itemCount(id: Long): Int

    @Query("UPDATE routine SET category_id = :to WHERE category_id = :from")
    protected abstract suspend fun moveRoutines(from: Long, to: Long)

    @Query("UPDATE preset SET category_id = :to WHERE category_id = :from")
    protected abstract suspend fun movePresets(from: Long, to: Long)

    @Query("DELETE FROM category WHERE id = :id AND removable = 1")
    protected abstract suspend fun deleteRemovable(id: Long): Int

    /** 루틴 · 빠른 선택을 [to] 로 옮긴 뒤 분류를 지운다(한 트랜잭션). 지울 수 없는 분류면 아무것도 바꾸지 않고 false */
    @Transaction
    open suspend fun deleteMovingTo(id: Long, to: Long): Boolean {
        val target = byId(id) ?: return false
        if (!target.removable || id == to) return false
        moveRoutines(id, to)
        movePresets(id, to)
        return deleteRemovable(id) > 0
    }
}
