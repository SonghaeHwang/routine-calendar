package com.kikilog.routinecalendar.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kikilog.routinecalendar.core.model.Category

/**
 * `category` 테이블(DB 버전 3). 사용자가 고칠 수 있는 분류.
 * 루틴 · 빠른 선택의 `category_id` 가 이 id 를 가리킨다. 외래 키는 두지 않는다 — 분류를 지울 때
 * [CategoryDao.deleteMovingTo] 가 항목을 「기타」로 옮긴 뒤 지운다.
 * @property color 0xAARRGGBB
 */
@Entity(tableName = "category")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int,
    val position: Int,
    val removable: Boolean,
)

fun CategoryEntity.toModel(): Category = Category(id, name, color, position, removable)

fun Category.toEntity(): CategoryEntity = CategoryEntity(id, name, colorArgb, position, removable)
