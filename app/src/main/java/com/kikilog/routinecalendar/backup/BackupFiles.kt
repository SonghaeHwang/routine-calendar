package com.kikilog.routinecalendar.backup

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import java.io.ByteArrayOutputStream
import java.io.IOException

/** 사용자가 고른 파일에 쓴다(저장소 접근 프레임워크 — 저장소 권한 없음). 있던 내용은 지운다 */
suspend fun writeBackupFile(resolver: ContentResolver, uri: Uri, text: String) = withContext(Dispatchers.IO) {
    resolver.openOutputStream(uri, "wt")?.use { it.write(text.encodeToByteArray()) }
        ?: throw IOException("파일을 열 수 없음")
}

/** 사용자가 고른 파일을 읽는다. [MAX_BACKUP_BYTES] 보다 크면 끝까지 읽지 않고 거부한다 */
suspend fun readBackupFile(resolver: ContentResolver, uri: Uri): String = withContext(Dispatchers.IO) {
    val bytes = resolver.openInputStream(uri)?.use { input ->
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            out.write(buffer, 0, read)
            if (out.size() > MAX_BACKUP_BYTES) throw BackupFormatException(BackupProblem.TOO_LARGE, "파일이 너무 큼")
        }
        out.toByteArray()
    } ?: throw IOException("파일을 열 수 없음")
    bytes.decodeToString()
}

/** 내보낼 때 제안하는 파일 이름. 같은 이름이 있으면 시스템이 번호를 붙인다 */
fun backupFileName(date: LocalDate): String = "routine-calendar-$date.json"
