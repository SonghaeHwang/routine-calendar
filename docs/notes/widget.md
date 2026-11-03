# 6단계 위젯 사전 조사 (Jetpack Glance)

> 조사일: 2026-11-03 · 대상: PLAN 6단계 「Glance 위젯: 오늘 + 지난 것, 위젯에서 체크」
> 버전은 Google Maven `maven-metadata.xml` 과 POM 을 직접 열어 확인했다. 출처는 각 절 끝에 있다(실제로 연 페이지만 적음).

## 결론 요약 (추천 설계)

1. **Glance 1.2.0(안정판, 2026-08-26)** 의 `glance-appwidget` + `glance-material3` 를 쓴다. 1.3.0 은 아직 alpha02 라 쓰지 않는다.
2. `widget/` 패키지에 `TodayWidget`(GlanceAppWidget) · `TodayWidgetReceiver` · `ToggleRoutineAction`(ActionCallback) 을 둔다. 줄 나누기는 이미 있는 `:core` 의 `buildTodayBoard` 를 그대로 쓰고, 위젯에는 `overdue + dueToday` 만 보여 준다.
3. `provideGlance` 에서 `(context.applicationContext as RoutineCalendarApp).container` 로 저장소에 접근한다. 먼저 `first()` 로 첫 값을 읽고, `provideContent` 안에서 `combine(observeRoutines(), todayClock.today)` 를 `collectAsState` 로 구독한다.
4. 체크 버튼은 `CheckBox(checked, onCheckedChange = actionRunCallback<ToggleRoutineAction>(params))` 로 만든다. 파라미터는 `routineId: Long` · `date: String(ISO)` · `doneToday: Boolean` 세 개이고, 콜백 안에서 `repository.check/uncheck` 를 부른 다음 `TodayWidget().update(context, glanceId)` 를 호출한다.
5. 앱에서 바꾼 내용을 위젯에 알리는 일은 **한 곳에서** 한다. `AppContainer` 의 `appScope` 에서 `observeRoutines()` 변화를 구독해(첫 값 제외, 짧은 debounce) `TodayWidget().updateAll(context)` 를 부른다. 이렇게 하면 화면마다 갱신 호출을 따로 넣지 않아도 된다(대안은 「열린 질문」 1).
6. 자정 처리: WorkManager `OneTimeWorkRequest`(다음 자정까지 지연, 고유 이름 `REPLACE`)가 `updateAll` 을 하고 다음 날 작업을 다시 예약한다. `updatePeriodMillis` 는 30분보다 짧게 할 수 없으므로 하루 1회(`86400000`)로 보조만 맡긴다. 정확한 알람 권한은 쓰지 않는다.
7. 크기는 `SizeMode.Responsive`(작은 2×2 · 넓은 4×2 · 큰 4×3 정도), 색은 `GlanceTheme`(Android 12 이상 동적 색, 다크 모드 자동)으로 처리한다. 위젯을 누르면 `actionStartActivity<MainActivity>` + 「오늘 탭」 파라미터로 앱을 연다.
8. 미리보기는 `previewLayout`(XML)과 `previewImage` 를 기본으로 두고, Android 15 이상에서는 `providePreview` + `setWidgetPreviews` 로 생성 미리보기를 더한다(One UI 런처에서 생성 미리보기가 보이는지는 확인 안 됨).

---

## 1. 버전 · 호환성

| 항목 | 확인한 값 | 메모 |
|---|---|---|
| `androidx.glance:glance-appwidget` | **1.2.0** 안정판 (2026-08-26), 최신 1.3.0-alpha02 (2026-07-01) | testing · preview · material3 도 같은 판으로 묶여 나옴 |
| 1.2.0 POM 의존성 | `kotlin-stdlib 2.0.21` · compose `runtime 1.7.8` · `work-runtime-ktx 2.7.1` · `datastore-preferences 1.0.0` | 모두 최소 판이다. 프로젝트의 BOM 2026.09.00 · DataStore 1.3.0 쪽이 더 높아서 그 판으로 올라간다 |
| Kotlin 2.2.10 | stdlib 2.0.21 로 만든 라이브러리라 **메타데이터 문제 없음** | kotlinx-serialization 때 겪은 「더 새 Kotlin 으로 만든 판」 문제와 반대 방향이라 괜찮다 |
| minSdk | 1.2.0-beta01 부터 라이브러리 기본 minSdk 가 23 | 우리 minSdk 26 이므로 문제 없음 |
| AGP 9 / compileSdk 37 | 1.3.0-alpha01 은 compileSdk 37 이라 AGP 9.2 이상이 필요(릴리스 노트) | 우리는 AGP 9.5 라 나중에 1.3 으로 올려도 괜찮다 |
| WorkManager | Glance 가 `work-runtime-ktx` 를 끌어온다. 최신 안정판 **2.12.0** | 5단계(알림)에서 넣을 판과 같은 버전 하나로 맞춘다 |
| Compose 플러그인 | Glance 도 Compose 컴파일러가 필요하다. 이미 `kotlin-compose` 플러그인과 `buildFeatures.compose = true` 가 있다 | 추가 설정 없음 |
| KMP | Glance 는 Android 전용 | 규칙 4대로 `:app/widget/` 에만 둔다. `:core` 에는 넣지 않는다 |

제안 (`libs.versions.toml`, 실제로 고치지 않음):

```toml
[versions]
glance = "1.2.0"
work = "2.12.0"

[libraries]
androidx-glance-appwidget = { group = "androidx.glance", name = "glance-appwidget", version.ref = "glance" }
androidx-glance-material3 = { group = "androidx.glance", name = "glance-material3", version.ref = "glance" }
androidx-glance-appwidget-testing = { group = "androidx.glance", name = "glance-appwidget-testing", version.ref = "glance" }
androidx-glance-testing = { group = "androidx.glance", name = "glance-testing", version.ref = "glance" }
androidx-work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "work" }
```

`app/build.gradle.kts` 에는 `implementation` 으로 glance-appwidget · glance-material3 · work-runtime-ktx 를, `testImplementation` 으로 testing 두 개를 넣는다. 「Glance 와 `androidx.compose.ui` 컴포저블을 섞지 말 것」(릴리스 노트)도 지킨다. import 이름이 같은 것(`Text` · `Column` · `Box`)이 많아서 `widget/` 파일에서는 `androidx.glance.*` 만 import 한다.

출처: [Glance 릴리스 노트](https://developer.android.com/jetpack/androidx/releases/glance) · Google Maven `androidx/glance/*/maven-metadata.xml` · `glance-appwidget-1.2.0.pom` · `androidx/work/work-runtime/maven-metadata.xml`

## 2. 목록 + 항목별 체크

- `LazyColumn` 은 `ListView`(RemoteViews 컬렉션)로 바뀐다. `items(list, itemId = { it.routine.id })` 로 id 를 주면 Android 12 이상에서 갱신할 때 스크롤 위치가 유지된다.
- `CheckBox(checked: Boolean, onCheckedChange: Action?, modifier, text, style, colors, maxLines)` 오버로드가 있어서 `actionRunCallback` 을 바로 넘길 수 있다.
- 람다 액션도 쓸 수 있지만 「WorkManager 워커 안에서 실행」되고 세션이 살아 있어야 한다. **`ActionCallback` 을 추천**한다. 클래스로 따로 있어서 시험에서 `assertHasRunCallbackClickAction` 으로 확인할 수 있다.
- `ActionCallback.onAction` 은 Glance 의 비동기 BroadcastReceiver 에서 돈다. 시간이 오래 걸리는 일은 Worker 로 넘기라고 되어 있다. 우리 일은 Room insert/delete 한 번이라 짧다.
- `ActionParameters` 값은 Intent extra 에 담긴다. `LocalDate` 는 그대로 못 넣으므로 ISO 문자열로 넘긴다.
- 날짜는 **위젯이 그릴 때의 오늘**을 넘긴다. `TodayViewModel.onToggle` 의 「자정 직전 체크가 다음 날로 저장되지 않게」 규칙과 같다. 다만 위젯이 자정 갱신을 놓친 채 어제 화면으로 남아 있으면 어제 날짜로 기록된다. 그래도 `check` 는 `doneOn <= today` 만 검사하므로 오류는 아니다.

```kotlin
private val RoutineIdKey = ActionParameters.Key<Long>("routineId")
private val DateKey = ActionParameters.Key<String>("date")
private val DoneKey = ActionParameters.Key<Boolean>("doneToday")

class ToggleRoutineAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[RoutineIdKey] ?: return
        val date = parameters[DateKey]?.let(LocalDate::parse) ?: return
        val repo = (context.applicationContext as RoutineCalendarApp).container.routineRepository
        runCatching { if (parameters[DoneKey] == true) repo.uncheck(id, date) else repo.check(id, date) }
        TodayWidget().update(context, glanceId)
    }
}
```

- 수동 DI: 콜백 · 위젯 · 워커는 모두 앱 프로세스에서 돈다. 그래서 `applicationContext` → `RoutineCalendarApp.container` 로 같은 싱글턴(DB · `TodayClock`)을 쓴다. 위젯을 그린 결과(RemoteViews)만 런처 프로세스에 표시된다. `container` 는 `lazy` 라 앱이 꺼진 상태에서 위젯을 눌러도 처음 쓸 때 열린다.

출처: [사용자 상호작용 처리](https://developer.android.com/develop/ui/compose/glance/user-interaction) · [Glance 로 UI 만들기](https://developer.android.com/develop/ui/compose/glance/build-ui) · [androidx.glance.appwidget 패키지 레퍼런스](https://developer.android.com/reference/kotlin/androidx/glance/appwidget/package-summary)

## 3. 데이터 → 위젯 갱신

**세션 수명 (공식 레퍼런스 원문 요지)**: `provideGlance` 는 `CoroutineWorker` 로 돌고, `provideContent` 를 부르기 전까지는 WorkManager 시간 제한(현재 10분)을 받는다. `provideContent` 를 부른 뒤에는 **약 45초 동안** 계속 다시 그리고, 클릭이나 갱신 요청이 오면 시간이 늘어난다. `update`/`updateAll` 은 이미 돌고 있는 `provideGlance` 를 다시 시작하지 않는다. 그래서 **첫 데이터를 미리 읽고, 구성 안에서 `collectAsState` 로 구독하라**고 하고, 앱의 다른 곳에서 데이터를 바꾸면 워커가 없을 수 있으니 `update` 를 부르라고 한다.

→ Room Flow 를 `provideContent` 안에서 구독하는 방식은 **공식 권장 방식**이다. 다만 세션이 살아 있는 약 45초 동안만 반영되므로 앱 쪽 `updateAll` 호출이 꼭 함께 있어야 한다.

```kotlin
override suspend fun provideGlance(context: Context, id: GlanceId) {
    val c = (context.applicationContext as RoutineCalendarApp).container
    val boards = combine(c.routineRepository.observeRoutines(), c.todayClock.today) { items, date ->
        date to buildTodayBoard(items.map { it.routine to it.completions }, date)
    }
    val initial = withContext(Dispatchers.IO) { boards.first() }   // provideGlance 는 메인 스레드
    provideContent {
        val (date, board) by boards.collectAsState(initial)
        GlanceTheme { TodayWidgetContent(date, board.overdue + board.dueToday) }
    }
}
```

- `provideGlance` 를 시작할 때 `todayClock.refresh()` 를 한 번 부른다. 프로세스가 오래 잠들어 있으면 `runMidnightUpdates` 가 늦게 깰 수 있다.
- **갱신 호출 위치**: 저장소 쓰기는 `add · update · setArchived · delete · check · uncheck` 와 앞으로 생길 백업 가져오기까지 여러 곳이다. 그래서 `AppContainer` 에 `WidgetSync` 를 두고, `observeRoutines(includeArchived = true).drop(1).debounce(500)` 를 받을 때마다 `TodayWidget().updateAll(appContext)` 를 부르는 방식을 추천한다. `RoutineCalendarApp.onCreate` 에서 한 번 시작한다. `RoutineRepository` 는 Android 를 모르는 채로 둘 수 있다.
- `updateIf`/`getGlanceIds` 는 위젯이 하나뿐이라 필요 없다. 위젯이 하나도 없을 때 `updateAll` 을 부르면 아무 일도 하지 않는다(확인 안 됨, 실기기에서 로그로 확인).
- **자정**: `updatePeriodMillis` 는 30분 미만을 지원하지 않고, 문서도 「1시간에 1번 이하」를 권한다. 정확히 자정에 바뀌게 하려면 WorkManager 에 `OneTimeWorkRequest` 를 `setInitialDelay(다음 자정까지 + 1분)` 로 넣는다. 작업은 `todayClock.refresh()` → `updateAll` → 다음 날 작업 예약 순서로 한다. 도즈 상태에서는 몇 분 늦을 수 있다(정확한 알람을 쓰지 않으므로 받아들인다). 위젯의 `onEnabled` 에서 예약하고 `onDisabled` 에서 취소한다.
- 시계 · 시간대 변경: `TIME_SET` · `ACTION_TIMEZONE_CHANGED` 는 매니페스트 receiver 로 받을 수 있는 예외 목록에 있다. `ACTION_DATE_CHANGED` 는 목록에 **없다**. 필요하면 앞의 두 개만 받아 `updateAll` 하고 자정 작업을 다시 예약한다(선택).
- 앱 업데이트 후 갱신은 Glance 의 `MyPackageReplacedReceiver` 가 맡는다(레퍼런스에 클래스만 확인, 동작 세부는 확인 안 됨).

출처: [GlanceAppWidget 레퍼런스](https://developer.android.com/reference/kotlin/androidx/glance/appwidget/GlanceAppWidget) · [GlanceAppWidget 상태 관리와 갱신](https://developer.android.com/develop/ui/compose/glance/glance-app-widget) · [암시적 브로드캐스트 예외](https://developer.android.com/develop/background-work/background-tasks/broadcasts/broadcast-exceptions)

## 4. 크기 · 다크 모드 · 미리보기 · 선택기 문구 · One UI

- **SizeMode**: `Single`(기본)은 크기를 바꿔도 내용이 같다. `Responsive` 는 정해 둔 크기 몇 개로 미리 그리고 시스템이 가장 잘 맞는 것을 고른다. 공식 문서는 「일반적으로 성능이 더 좋고 크기 변경이 부드럽다」고 한다. `Exact` 는 크기가 바뀔 때마다 다시 만든다. 목록 위젯이라 **Responsive 3개**(예: 110×110 → 제목 + 개수만, 250×110 → 목록, 250×250 → 머리글 「지난 것/오늘」 구분)를 추천한다. 단, 생성 미리보기 문서의 메모는 「위젯 자체에는 Exact 를 Responsive 보다 우선하라」고 되어 있어 문서끼리 다르다. 실기기에서 크기를 바꿔 보며 정한다.
- **다크 모드 · 색**: `GlanceTheme { }` 로 감싸면 Android 12 이상은 배경화면 동적 색, 그 밖에는 Material 기본 색을 쓰고 낮/밤이 자동으로 바뀐다. 앱 색을 쓰려면 `glance-material3` 의 `ColorProviders(light = LightColors, dark = DarkColors)` 를 쓴다. 바깥 컨테이너는 `Scaffold`(glance) 또는 `appWidgetBackground()` + `cornerRadius(android.R.dimen.system_app_widget_background_radius)` 로 만든다.
- **위젯 누르기 → 오늘 화면**: `actionStartActivity<MainActivity>(actionParametersOf(StartTabKey to "today"))`. 람다나 콜백 안에서 액티비티를 시작하면 Android 12 이상의 트램펄린 제한에 걸리므로 반드시 `actionStartActivity` 를 쓴다. `MainActivity` 는 지금 `onCreate` 에서 첫 화면 설정만 읽으므로, `intent` extra 와 `onNewIntent` 처리를 더해야 한다.
- **미리보기**: Android 12 이상은 `previewLayout`(XML), 11 이하는 `previewImage`, Android 15(API 35) 이상은 `providePreview` + `GlanceAppWidgetManager.setWidgetPreviews<TodayWidgetReceiver>(intSetOf(WIDGET_CATEGORY_HOME_SCREEN))` 를 쓴다. 기본 인자는 잠금 화면 · 검색창까지 포함하므로 홈 화면만 넘긴다. **호출 제한은 시간당 약 2회**이므로 `generatedPreviewCategories` 를 확인하고 앱을 처음 실행할 때 한 번만 부른다. 미리보기에는 실제 루틴 대신 예시 데이터(「각질제거 3일 지남 · 모델링팩」)를 쓰는 편이 안전하다. `previewSizeMode` 를 Responsive 로 두지 않으면 최소 크기 부분만 그려진다.
- **선택기 설명**: `android:description="@string/widget_description"`(Android 12 이상). 예: 「오늘 할 루틴과 지난 루틴을 보고 바로 체크해요」.
- **Android 17**: RemoteViews 이미지 메모리 한도(`6 × 너비 × 높이` 바이트)를 넘으면 `IllegalArgumentException` 이 난다. 우리 위젯은 벡터 아이콘만 쓰므로 해당되지 않는다.
- **One UI 런처**: 공식 · 삼성 문서에서 Glance 와 관련해 알려진 문제는 **찾지 못했다(확인 안 됨)**. 비공식 블로그(TechNetExperts, 출처 표시 없음)에 「One UI Home 은 생성 미리보기 대신 정적 `previewLayout` 을 보여 주고 캐시가 강하다(런처 데이터 삭제나 재설치 때만 갱신)」라는 주장이 있다. 근거가 약하니 **정적 `previewLayout` 을 제대로 만들어 두는 것**으로 대비하고, S26 에서 직접 확인한다.

출처: [Glance 로 UI 만들기](https://developer.android.com/develop/ui/compose/glance/build-ui) · [Glance 테마](https://developer.android.com/develop/ui/compose/glance/theme) · [생성 미리보기](https://developer.android.com/develop/ui/compose/glance/generated-previews) · [위젯 만들기(매니페스트 · 속성 · 메모리)](https://developer.android.com/develop/ui/compose/glance/create-app-widget) · [androidx.glance.appwidget 패키지 레퍼런스](https://developer.android.com/reference/kotlin/androidx/glance/appwidget/package-summary) · [TechNetExperts 「Android 15 Generated Widget Preview Issue」(비공식)](https://www.technetexperts.com/android-widget-picker-cache-fix/)

## 5. 매니페스트 · 시험

```xml
<receiver android:name=".widget.TodayWidgetReceiver" android:exported="false">
    <intent-filter><action android:name="android.appwidget.action.APPWIDGET_UPDATE" /></intent-filter>
    <meta-data android:name="android.appwidget.provider" android:resource="@xml/today_widget_info" />
</receiver>
```

공식 문서의 예시 두 개가 `exported` 를 `true`/`false` 로 다르게 적었다. 시스템이 보내는 `APPWIDGET_UPDATE` 는 `false` 여도 받는 것으로 알려져 있지만 **확인 안 됨**이다. `false` 로 시작하고, 실기기에서 위젯이 추가되지 않으면 `true` 로 바꾼다.

`res/xml/today_widget_info.xml` 핵심 속성: `initialLayout="@layout/glance_default_loading_layout"` · `minWidth/minHeight` + `targetCellWidth="3" targetCellHeight="2"` · `minResizeWidth/Height` · `resizeMode="horizontal|vertical"` · `updatePeriodMillis="86400000"` · `description` · `previewLayout` · `previewImage` · `widgetCategory="home_screen"`. 설정 화면이 없으므로 `configure` 는 넣지 않는다. 권한은 추가하지 않는다(규칙 1 · 2 그대로).

`GlanceAppWidgetReceiver` 주의: `onUpdate` 를 덮어쓰면 `super.onUpdate` 를 반드시 부르고, `onReceive` 에서 `goAsync` 를 직접 부르지 않는다. 리시버 콜백에는 10초 제한이 있다.

**시험**
- 단위 시험(`app/src/test`, 기기 없이): `runGlanceAppWidgetUnitTest { setAppWidgetSize(...); provideComposable { TodayWidgetContent(date, entries) }; onNode(hasText("각질제거")).assertExists() }`. 체크 버튼은 `assertHasRunCallbackClickAction<ToggleRoutineAction>(parameters)`, 빈 목록 문구, 크기별 머리글 여부를 확인한다. 클릭은 실행하지 않고 액션이 붙었는지만 검사한다. `LocalContext`(문자열 리소스)를 읽으면 `setContext` 가 필요하므로 이미 쓰는 Robolectric 을 그대로 쓴다. 기본 제한 시간은 1초다.
- 콜백 로직 시험: `ToggleRoutineAction` 의 저장소 부분을 함수로 떼어(예: `toggle(repo, id, date, done)`) 기존 Robolectric + 메모리 DB 시험 방식으로 확인한다.
- 실기기(S26) 확인 순서: ① `./gradlew installDebug` ② 홈 화면 길게 누르기 → 위젯 → 「Routine Calendar」에서 미리보기 · 설명 문구 확인 ③ 놓고 크기를 2×2 / 4×2 / 4×3 으로 바꿔 보기 ④ 위젯에서 체크 → 앱을 열어 오늘 화면에 반영됐는지 확인 ⑤ 앱에서 체크/취소 → 홈으로 나와 위젯 확인(**45초 넘게 기다린 뒤에도** 다시 해 본다. 세션이 끝난 뒤 `updateAll` 경로를 시험하기 위해서) ⑥ 다크 모드 · 배경화면을 바꿔 색 확인 ⑦ 기기 시간을 23:59 근처로 바꿔 자정 넘김 확인(`adb` 로 시간을 바꾸는 명령은 기기 상태를 바꾸므로 설정 앱에서 직접 바꾼다) ⑧ 앱을 강제 종료한 뒤 위젯 체크가 되는지 확인 ⑨ TalkBack 으로 체크 칸 읽기.
- 참고: `GlanceAppWidgetReceiver.ACTION_DEBUG_UPDATE` 로 adb 에서 강제 갱신할 수 있지만 루팅 기기나 에뮬레이터 전용이다(릴리스 노트). S26 에는 쓰지 않는다.

출처: [위젯 만들기](https://developer.android.com/develop/ui/compose/glance/create-app-widget) · [Glance 시험](https://developer.android.com/develop/ui/compose/glance/testing) · [Glance 릴리스 노트](https://developer.android.com/jetpack/androidx/releases/glance)

---

## 구현 순서 체크리스트

- [ ] ① 빌드 설정: glance 1.2.0 · glance-material3 · work 2.12.0 · glance 시험 라이브러리 → `./gradlew test assembleDebug`
- [ ] ② `widget/` 뼈대: Receiver · 빈 `TodayWidget`(글자 한 줄) · `today_widget_info.xml` · 매니페스트 · 설명 문자열 → S26 에 위젯이 놓이는지 확인
- [ ] ③ 내용 컴포저블 `TodayWidgetContent(date, entries)`: 지난 것(「N일 지남」) + 오늘, 빈 상태 문구, Responsive 3크기, `GlanceTheme` + 단위 시험
- [ ] ④ `provideGlance` 에서 첫 값 읽기 + `collectAsState` 구독
- [ ] ⑤ `ToggleRoutineAction` + 파라미터 + 시험(액션이 붙었는지, 저장소 토글)
- [ ] ⑥ 앱 → 위젯 갱신(`WidgetSync`, `RoutineCalendarApp.onCreate` 에서 시작)
- [ ] ⑦ 위젯 누르기 → 오늘 탭(`MainActivity` extra · `onNewIntent`)
- [ ] ⑧ 자정 갱신 워커(`onEnabled` 예약 · `onDisabled` 취소)
- [ ] ⑨ 미리보기: `previewLayout` · `previewImage` · Android 15 이상 `setWidgetPreviews`
- [ ] ⑩ 실기기 확인 9가지(5절) → PLAN 6단계 체크 · DEVLOG

## 열린 질문

1. 위젯 갱신 호출 위치: `AppContainer` 에서 Flow 를 구독하는 방식(한 곳, 빠뜨릴 일 없음, 다만 앱 프로세스가 살아 있는 동안 Room 구독이 계속 열림)과 ViewModel · 저장소 쓰기 뒤에 직접 `updateAll` 하는 방식(명시적, 빠뜨리기 쉬움) 중 어느 것으로 할지.
2. 위젯 목록 범위: 「지난 것 + 오늘」만 보일지, 비었을 때 「다가오는 7일」 가운데 가장 가까운 것 하나를 보여 줄지. 오늘 체크한 줄은 앱처럼 체크된 채로 남길지, 위젯에서는 숨길지.
3. 크기 방식: Responsive 와 Exact 중 무엇으로 할지(문서끼리 권고가 다름, 2×2 같은 작은 크기를 지원할지).
4. 색: 동적 색(배경화면)과 앱 고유 색(`ColorProviders`) 중 무엇을 쓸지.
5. 생성 미리보기(Android 15 이상)까지 할지, 정적 `previewLayout` 만으로 끝낼지. One UI 에서 생성 미리보기가 보이는지 직접 확인할지.
6. WorkManager 를 6단계에서 먼저 넣을지(5단계 알림과 같은 판·같은 초기화 방식으로 맞춰야 함). 지금 진행 순서상 5단계를 먼저 하면 이 문제는 없다.
7. 보관(archived) 루틴은 `buildTodayBoard` 에서 이미 빠진다. 앱이 잠금 상태일 때 위젯에 루틴 이름이 보이는 것(개인정보)이 괜찮은지. `home_screen` 만 쓰면 잠금 화면 위젯은 아니다.
