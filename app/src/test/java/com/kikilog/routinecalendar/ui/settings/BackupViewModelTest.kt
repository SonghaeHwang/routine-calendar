package com.kikilog.routinecalendar.ui.settings

import android.net.Uri
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kikilog.routinecalendar.backup.BackupProblem
import com.kikilog.routinecalendar.backup.BackupRepository
import com.kikilog.routinecalendar.backup.ImportMode
import com.kikilog.routinecalendar.backup.ImportOutcome
import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.Interval
import com.kikilog.routinecalendar.core.model.IntervalUnit
import com.kikilog.routinecalendar.data.AppDatabase
import com.kikilog.routinecalendar.data.RoutineDraft
import com.kikilog.routinecalendar.data.RoutineRepository
import com.kikilog.routinecalendar.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BackupViewModelTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val today = LocalDate.parse("2026-11-03")
    private lateinit var db: AppDatabase
    private lateinit var routines: RoutineRepository

    /** 가짜 파일들: Uri 문자열 → 내용 */
    private val files = mutableMapOf<String, String>()
    private val fileUri = Uri.parse("content://test/backup.json")

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .addCallback(AppDatabase.SeedOnCreate)
            .allowMainThreadQueries()
            .build()
        routines = RoutineRepository(db.routineDao(), today = { today }, now = { 1L })
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(): BackupViewModel {
        val settings = SettingsRepository(
            PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { folder.newFile("settings.preferences_pb") }),
        )
        val repository = BackupRepository(db.backupDao(), settings, File(folder.root, "before-import.json"), now = { 9L }, appVersion = "1.0")
        return BackupViewModel(
            repository,
            read = { uri -> files[uri.toString()] ?: throw IOException("없음") },
            write = { uri, text -> files[uri.toString()] = text },
            today = { today },
        )
    }

    private suspend fun add(title: String) =
        routines.addStartingOn(RoutineDraft(title, Category.HOUSE, Interval(1, IntervalUnit.WEEK), repeats = true, notify = true), today).first

    private suspend fun titles() = routines.observeRoutines(includeArchived = true).first().map { it.routine.title }.sorted()

    @Test
    fun `내보내기 이름에 오늘 날짜`() = runTest {
        assertEquals("routine-calendar-2026-11-03.json", viewModel().suggestedFileName())
    }

    @Test
    fun `내보낸 파일을 고르면 확인을 기다리고, 합치기를 고르면 더한다`() = runTest {
        val viewModel = viewModel()
        add("빨래")
        viewModel.exportTo(fileUri)
        assertEquals(BackupMessage.Exported, viewModel.state.first { it.message != null }.message)
        viewModel.dismissMessage()

        add("젤팩")
        // 같은 DB 에 합치므로 같은 이름(빨래)은 더해지지 않는다
        viewModel.importFrom(fileUri)
        val pending = viewModel.state.first { it.pending != null }.pending!!
        assertEquals(1, pending.routines.size)

        viewModel.confirmImport(ImportMode.MERGE)
        val message = viewModel.state.first { it.message != null }.message
        assertTrue(message is BackupMessage.Imported && message.outcome is ImportOutcome.Merged)
        assertEquals(listOf("빨래", "젤팩"), titles())
        assertNull(viewModel.state.value.pending)
        assertTrue(viewModel.state.first { it.hasSnapshot }.hasSnapshot)
    }

    @Test
    fun `덮어쓰고 되돌리기`() = runTest {
        val viewModel = viewModel()
        add("빨래")
        viewModel.exportTo(fileUri)
        viewModel.state.first { it.message != null }
        viewModel.dismissMessage()
        add("젤팩")

        viewModel.importFrom(fileUri)
        viewModel.state.first { it.pending != null }
        viewModel.confirmImport(ImportMode.REPLACE)
        viewModel.state.first { it.message != null }
        assertEquals(listOf("빨래"), titles())
        viewModel.dismissMessage()

        viewModel.requestRestore()
        assertTrue(viewModel.state.first { it.confirmRestore }.confirmRestore)
        viewModel.confirmRestore()
        assertEquals(BackupMessage.Restored, viewModel.state.first { it.message != null }.message)
        assertEquals(listOf("빨래", "젤팩"), titles())
    }

    @Test
    fun `백업 파일이 아니거나 열 수 없으면 이유를 알리고 아무것도 바꾸지 않는다`() = runTest {
        val viewModel = viewModel()
        add("빨래")
        files[fileUri.toString()] = "{\"hello\": 1}"

        viewModel.importFrom(fileUri)
        assertEquals(BackupMessage.Unreadable(BackupProblem.NOT_BACKUP), viewModel.state.first { it.message != null }.message)
        viewModel.dismissMessage()

        viewModel.importFrom(Uri.parse("content://test/none.json"))
        assertEquals(BackupMessage.Unreadable(null), viewModel.state.first { it.message != null }.message)
        assertEquals(listOf("빨래"), titles())
        assertNull(viewModel.state.value.pending)
    }
}
