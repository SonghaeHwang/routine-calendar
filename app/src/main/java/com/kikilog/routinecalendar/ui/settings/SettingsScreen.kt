package com.kikilog.routinecalendar.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.kikilog.routinecalendar.R
import com.kikilog.routinecalendar.ui.common.SubScaffold

/**
 * 설정 화면. 3단계에서는 개인정보 안내와 앱 버전만 둔다.
 * 알림 시각 · 주 시작 요일 · 백업은 각 단계에서 추가한다(동작하지 않는 메뉴는 보이지 않는다).
 */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    // AGP 9 는 BuildConfig 를 기본으로 만들지 않아 패키지 정보에서 읽는다
    val versionName = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }

    SubScaffold(title = stringResource(R.string.settings), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_privacy_title)) },
                supportingContent = { Text(stringResource(R.string.settings_privacy_body)) },
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_version)) },
                supportingContent = { Text(versionName) },
            )
        }
    }
}
