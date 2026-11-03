# 5단계(알림) 사전 조사

> 조사일: 2026-11-03 · 대상: Galaxy S26 (One UI, Android 17 / API 37), minSdk 26 · targetSdk 37, Kotlin 2.2.10 · AGP 9.5
> 이 문서는 조사 결과와 제안이다. `libs.versions.toml` · Manifest 는 아직 고치지 않았다.

## 결론 요약 (추천 설계)

1. **WorkManager 2.12.0 (`work-runtime-ktx`) + 주기 작업 1개.** 고유 이름 `daily_summary` 로 `PeriodicWorkRequest`(24시간)를 `enqueueUniquePeriodicWork(..., UPDATE, ...)` 로 넣고, `setNextScheduleTimeOverride(다음 알림 시각 millis)` 로 "다음 1회" 시각을 정한다. 공식 문서가 "매일 아침 drift 없이" 쓰는 예로 든 방식이다.
2. **작업이 끝날 때마다 다음 날 시각을 다시 계산해서 `UPDATE` 로 재등록**한다. 시각 변경(설정) · 앱 시작 때도 같은 함수를 부른다. 시간대 · 서머타임은 매번 현재 시간대로 다시 계산해서 흡수한다. 계산 함수는 `:core` 에 순수 Kotlin(`kotlinx-datetime`)으로 두고 단위 시험한다.
3. **재부팅은 WorkManager 가 처리한다.** `RECEIVE_BOOT_COMPLETED` 는 라이브러리 Manifest 에서 자동으로 병합되므로 직접 선언하지 않는다. 런타임 권한은 여전히 `POST_NOTIFICATIONS` 하나다.
4. **"정해진 시각 근처"는 보장이 아니라 최선이다.** Doze 중에는 작업이 돌지 않고 화면을 켤 때(아침에 휴대폰을 집을 때) 몰아서 돈다. 삼성 "깊은 절전" 에 들어가면 알림이 아예 오지 않는다. 설정 화면에 안내 + 배터리 설정 열기 버튼을 두고, 배터리 최적화 예외를 직접 요청하지는 않는다(Play 정책).
5. **권한은 설정의 "알림 켜기" 스위치를 누를 때 요청한다**(첫 실행에 바로 묻지 않음). 거절하면 스위치 옆에 "알림이 꺼져 있음 · 설정 열기" 를 보여 주고 앱의 나머지는 그대로 쓴다. 두 번 거절하면 시스템 창이 다시 뜨지 않는다.
6. **알림 채널 1개("오늘 요약", 기본 중요도), `VISIBILITY_PRIVATE` + 공개 버전("오늘 할 루틴 3개 · 지난 것 1개" 처럼 개수만).** 같은 알림 ID 로 덮어써서 하루 1개만 남긴다.
7. **알림 탭은 `MainActivity` 를 명시 Intent(+ extra)로 연다.** URI 딥 링크 · intent-filter 는 만들지 않는다. `onCreate`/`onNewIntent` 에서 extra 를 읽어 Navigation 3 백 스택을 오늘 탭으로 바꾼다.
8. **시험:** Worker 내용은 `TestListenableWorkerBuilder`, 예약은 `WorkManagerTestInitHelper` + `TestDriver`, 실기기는 adb(`dumpsys jobscheduler` · 진단 broadcast · `deviceidle force-idle`).

---

## 1. WorkManager 로 "매일 특정 시각 근처"

**선택지 비교**

| 방식 | 장점 | 단점 |
|---|---|---|
| A. `PeriodicWorkRequest`(24h + flex) + `initialDelay` | 등록 한 번 | 주기는 "직전 실행 기준 간격"이라 실행이 늦어지면 다음 날도 밀린다(drift). initialDelay 는 첫 회만 적용 |
| B. 매번 `OneTimeWorkRequest` 재예약 | 매번 정확히 계산 | Worker 가 예외로 죽거나 재예약 전에 멈추면 사슬이 끊긴다. Worker 안에서 같은 이름으로 `REPLACE` 하면 실행 중인 자기 자신이 취소된다 |
| **C. Periodic 24h + `setNextScheduleTimeOverride` + `UPDATE` (추천)** | 다음 1회 시각을 지정하면서, 재지정을 못 해도 24h 주기로 계속 돈다 | WorkManager 2.9.0 이상 필요(2.12.0 이면 충족) |

**확인한 사실 (공식 문서)**

- 주기 작업 최소 간격 15분. flex 구간은 `repeatInterval - flexInterval` 부터 주기 끝까지이며, flex 는 `MIN_PERIODIC_FLEX_MILLIS` 이상이어야 한다(상수 값 자체는 레퍼런스 페이지에 숫자로 나오지 않음 — 확인 안 됨).
- `setInitialDelay` 를 주기 작업에 주면 **첫 실행만** 늦춰진다.
- `setNextScheduleTimeOverride(millis)` (2.9.0 추가): 다음 실행 시각을 interval · flex · initialDelay · backoff 보다 우선해 지정. **한 번만** 적용되고 그 뒤에는 원래 주기로 돌아간다. 문서가 "매일 아침 사용자가 일어나기 전에 drift 없이" 쓰는 예를 들며, 실행 중인 작업을 취소하지 않으려면 `ExistingPeriodicWorkPolicy.UPDATE` 를 쓰라고 한다. 직전 실행과 15분 이상 떨어지도록 강제된다. "정확히 그 시각에 실행되는 일은 거의 없다(Doze 등)" 고 명시.
- `ExistingPeriodicWorkPolicy`: `KEEP`(있으면 무시) · `UPDATE`(사양 갱신, **등록 시각 유지**, 실행 중이면 끊지 않고 다음 회차부터 적용) · `CANCEL_AND_REENQUEUE`(지우고 새로 넣음) · `REPLACE`(deprecated, `UPDATE` 사용 권장).
  - 앱 시작마다 `KEEP` 를 쓰면 시각 변경이 반영 안 된다 → **항상 `UPDATE` + override** 로 통일.
- 제약(Constraints)은 걸지 않는다. 걸면 조건이 맞을 때까지 실행되지 않고 회차를 건너뛸 수도 있다.

**시간대 · 서머타임**: 주기는 경과 시간 기준이고 override 는 `System.currentTimeMillis()` 기준 절대 시각이다. 그래서 시간대가 바뀌거나 서머타임 전환 날에는 "09:00" 이 어긋날 수 있다. 다만 실행할 때마다 **현재 시간대로 다음 09:00 을 다시 계산**하면 다음 날부터 바로잡힌다. 한국은 서머타임이 없어 실사용 영향은 해외 이동 정도다. 시간대 변경 broadcast(`ACTION_TIMEZONE_CHANGED`)를 받아 바로 재계산하는 방법도 있지만, 리시버가 하나 늘어나므로 처음에는 하지 않는다. (broadcast 가 암시적 broadcast 제한에서 예외인지는 확인 안 됨.)

**중복 · 늦은 실행 처리 (제안)**: DataStore 에 `last_notified_on`(날짜)을 저장해서 같은 날 두 번 보내지 않는다. 너무 늦게 돈 경우(예: 저녁) 그래도 보낼지는 열린 질문.

**버전 · 호환**

- 최신 안정판 **2.12.0 (2026-09-23)**. Google Maven `maven-metadata.xml` 로 직접 확인했고, 그 뒤에 나온 판은 없다. 2.12.0 에서 minSdk 24, compileSdk 33 이상 필요 → 이 프로젝트(26 / 37)는 충족.
- `work-runtime-2.12.0.module` 의 `kotlin-stdlib` 요구는 **2.1.20** → Kotlin 2.2.10 이 메타데이터를 읽을 수 있다(serialization 처럼 더 새 Kotlin 으로 만든 판 문제는 없음).
- `work-runtime` 이 `room-runtime 2.7.0` 에 runtime 으로 의존 → 프로젝트의 Room 2.8.5 로 맞춰진다(높은 판이 이김). 충돌 가능성은 낮지만 첫 빌드에서 확인.
- AGP 9 · 내장 Kotlin 과 관련된 WorkManager 쪽 주의사항은 릴리스 노트에 없음 (확인 안 됨 — 첫 빌드로 확인).
- WorkManager 는 Android 전용 라이브러리다(KMP 아님). 규칙 4에 따라 `:app/notify/` 안에만 두고, "다음 알림 시각 계산" · "요약 문구에 들어갈 목록"은 `:core` 에 둔다.
- 기본 초기화(App Startup)를 그대로 쓴다. Worker 는 `applicationContext as RoutineCalendarApp` 로 `AppContainer` 에 접근하면 되므로 별도 `WorkerFactory` 는 필요 없다.

**libs.versions.toml 제안 (적용 안 함)**

```toml
[versions]
work = "2.12.0"

[libraries]
androidx-work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "work" }
androidx-work-testing = { group = "androidx.work", name = "work-testing", version.ref = "work" }
```

`app/build.gradle.kts`: `implementation(libs.androidx.work.runtime.ktx)` · `testImplementation(libs.androidx.work.testing)`. 알림 빌더(`NotificationCompat`)는 이미 있는 `core-ktx` 1.19.1 에 들어 있다.

출처: [WorkManager 릴리스](https://developer.android.com/jetpack/androidx/releases/work) · [Define work requests](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work) · [Manage work](https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/manage-work) · [PeriodicWorkRequest](https://developer.android.com/reference/androidx/work/PeriodicWorkRequest) · [PeriodicWorkRequest.Builder](https://developer.android.com/reference/androidx/work/PeriodicWorkRequest.Builder) · [ExistingPeriodicWorkPolicy](https://developer.android.com/reference/androidx/work/ExistingPeriodicWorkPolicy) · [Google Maven work-runtime 메타데이터](https://dl.google.com/android/maven2/androidx/work/work-runtime/maven-metadata.xml) · [work-runtime-2.12.0.module](https://dl.google.com/android/maven2/androidx/work/work-runtime/2.12.0/work-runtime-2.12.0.module)

## 2. 재부팅 후 유지

- 공식 문서: 예약된 작업은 WorkManager 내부 SQLite 에 저장되고 **재부팅 후에도 다시 예약된다**. 앱에서 부팅 리시버를 만들 필요가 없다.
- `work-runtime-2.12.0.aar` 의 `AndroidManifest.xml` 을 직접 열어 보니 병합으로 들어오는 것:
  - 권한: `WAKE_LOCK` · `ACCESS_NETWORK_STATE` · `RECEIVE_BOOT_COMPLETED` · `FOREGROUND_SERVICE` — 모두 **설치 시 자동 허용되는 일반 권한**이고 런타임 요청 창이 없다. `INTERNET` 은 없다.
  - 구성요소: `InitializationProvider`(App Startup) · `SystemJobService` · `SystemForegroundService` · `ForceStopRunnable$BroadcastReceiver` · `RescheduleReceiver`(BOOT_COMPLETED, 기본 `enabled=false`) · `DiagnosticsReceiver`(`DUMP` 권한으로 보호).
- Play 영향: 권한 목록(스토어 "권한 정보")에 위 일반 권한이 보인다. 데이터 보안 양식은 "수집 · 공유하는 데이터" 를 묻는 것이라 이 권한들 때문에 바뀌지는 않는다고 본다 (Play 콘솔 화면으로는 확인 안 됨 — 9단계에서 확인).
- `FOREGROUND_SERVICE` 는 우리가 쓰지 않으므로 Manifest 에 `tools:node="remove"` 로 뺄 수 있다. 다만 라이브러리 동작에 영향이 없는지는 확인 안 됨 → 열린 질문.
- CLAUDE.md 규칙 2 "권한은 알림 하나" 는 "런타임 권한은 알림 하나" 로 읽는 것이 맞아 보인다. 문구를 고칠지는 열린 질문.

출처: [Persistent work 개요](https://developer.android.com/develop/background-work/background-tasks/persistent) · [work-runtime-2.12.0.aar](https://dl.google.com/android/maven2/androidx/work/work-runtime/2.12.0/work-runtime-2.12.0.aar)(내려받아 Manifest 확인)

## 3. Doze · 앱 대기 버킷 · 삼성 배터리 최적화

**Android 공통 (공식 문서)**

- Doze(화면 꺼짐 · 충전 안 함 · 가만히 둠): JobScheduler 가 돌지 않으므로 **WorkManager 작업도 돌지 않는다**. 가끔 열리는 유지보수 창에서 몰아서 실행되고, 창 간격은 시간이 지날수록 길어진다(구체 시간은 문서에 없음). 화면을 켜거나 충전기를 꽂으면 바로 풀린다 → 실사용에서는 "09:00 근처, 늦어도 아침에 휴대폰을 처음 켤 때".
- 앱 대기 버킷별 일반 작업 한도: Active 60분 중 20분 · Working set 4시간 중 10분 · Frequent 12시간 중 10분 · Rare 24시간 중 10분 · Restricted **하루 1번, 10분**(다른 앱 작업과 묶여서만 실행). 하루 1회 · 몇 초짜리 작업이라 Restricted 까지 가지 않으면 실행 횟수는 문제없다. 다만 Restricted 에서는 언제 돌지 알 수 없다.
- **Android 13+ 에서는 8일 동안 사용자가 앱과 상호작용하지 않으면 Restricted 로 간다.** 알림 탭은 상호작용으로 치지만, 밀어서 지우는 것은 치지 않는다. **활성 위젯이 있는 앱은 Restricted 에서 예외** → 6단계 위젯이 알림 안정성에도 도움이 된다.
- 사용자가 앱 배터리를 "제한" 으로 바꾸면 작업 실행이 막힌다.
- 배터리 최적화 예외 직접 요청(`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`)은 Play 정책상 핵심 기능이 망가지는 경우만 허용되고, 허용 사례 표에 이 앱 같은 경우는 없다 → **쓰지 않는다**. 대신 `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`(설정 화면 열기)는 쓸 수 있다.

**삼성 One UI**

- 경로(삼성 지원 페이지, One UI 판 표기 없음): 설정 > 배터리 및 디바이스 케어 > 배터리 > 백그라운드 사용 제한 — "사용하지 않는 앱을 절전 상태로 전환" · 절전 앱 · 깊은 절전 앱 · 절전 예외 앱.
- 삼성 개발자 문서: 약 **3일** 쓰지 않은 백그라운드 앱은 "절전"(버킷 제한 적용, Job · Alarm · 포그라운드 서비스 제한), 오래 쓰지 않으면 "깊은 절전"(현재 **16일**, 정책에 따라 바뀔 수 있음) — 깊은 절전 앱은 **알림을 포함해 아무것도 못 하고**, 사용자가 열어야 풀린다.
- dontkillmyapp.com(삼성): 기본 3일 기준 절전, "개발자 쪽 해결책 없음", 사용자에게 "절전 예외 앱" 등록을 안내하라고 함. One UI 7/8 · Android 16/17 내용은 없음.
- S26(One UI, Android 17)에서의 실제 지연 정도는 확인 안 됨 → 5단계 완료 기준에 "며칠 그대로 두고 알림 시각 기록" 을 넣는다.

**사용자 안내 (제안)**: 설정 > 알림 항목 아래에 한 줄 "알림이 늦거나 오지 않으면: 배터리 설정에서 이 앱을 '절전 예외 앱' 에 추가하세요" + [배터리 설정 열기] 버튼. 매번 띄우는 경고창은 만들지 않는다.

출처: [Power management resource limits](https://developer.android.com/topic/performance/power/power-details) · [Doze and App Standby](https://developer.android.com/training/monitoring-device-state/doze-standby) · [App Standby Buckets](https://developer.android.com/topic/performance/appstandby) · [Samsung Developer — App management](https://developer.samsung.com/mobile/app-management.html) · [Samsung 지원 — Sleeping apps](https://www.samsung.com/us/support/answer/ANS00088422/) · [dontkillmyapp.com/samsung](https://dontkillmyapp.com/samsung)

## 4. 권한 · 채널 · 잠금 화면 · 탭 이동

**POST_NOTIFICATIONS 요청 UX**

- targetSdk 33+ 이면 요청 시점을 앱이 정한다. 새로 설치하면 **알림은 기본 꺼짐**. 문서 권장: 앱을 먼저 둘러보게 한 뒤, 사용자 동작(종 버튼 등)에 맞춰 요청한다.
- 흐름: `checkSelfPermission` → 허용이면 끝 / `shouldShowRequestPermissionRationale()` 이 true 면 설명 UI(취소 버튼 포함) → `rememberLauncherForActivityResult(RequestPermission())` 로 요청.
- **두 번 넘게 거절하면 시스템 창이 다시 뜨지 않는다**(영구 거절, 플래그 `USER_FIXED`). 창을 밀어서 닫으면 상태가 바뀌지 않는다.
- 거절 시: 기능이 꺼진 자리에서 구체적으로 알리고, 화면을 막지 않고, 계속 조르지 않는다. 문서 예시 코드 주석에는 "결정을 바꾸라고 설정으로 보내지 말라" 는 말도 있다 → **사용자가 스위치를 다시 켤 때만** 앱 알림 설정(`Settings.ACTION_APP_NOTIFICATION_SETTINGS`, 공식 페이지로는 확인 안 됨)을 연다.
- 보내기 직전에 `NotificationManagerCompat.areNotificationsEnabled()` 로 확인하고, 꺼져 있으면 Worker 는 조용히 `success` 로 끝낸다(재시도 안 함).
- 제안 흐름: 등록한 루틴이 생긴 뒤 오늘 화면에 한 번만 보이는 카드 "매일 09:00 에 오늘 할 일을 알려 드릴까요? [켜기] [나중에]" + 설정의 알림 스위치. 첫 실행 때 바로 묻지 않는다.

**채널**

- 알림을 보내기 전에 채널이 있어야 하고, 이미 있는 채널을 다시 만들어도 아무 일이 없으므로 **앱 시작(`RoutineCalendarApp.onCreate`)에서 만든다**. minSdk 26 이라 SDK 분기는 필요 없다.
- 1개: id `daily_summary`, 이름 "오늘 요약", `IMPORTANCE_DEFAULT`. 중요도는 사용자가 바꿀 수 있고 앱은 나중에 못 바꾼다. 채널 id 는 바꾸지 않는다.

**잠금 화면**

- `VISIBILITY_PRIVATE`: 아이콘과 제목만 보인다. `setPublicVersion()` 으로 내용을 숨긴 대체 알림을 줄 수 있다. 최종 결정은 사용자 설정이 우선한다.
- 제안: 본문 = `InboxStyle`(또는 `BigTextStyle`)로 루틴 이름 목록, 공개 버전 = "오늘 할 루틴 3개 · 지난 것 1개". 할 것이 0개인 날은 보내지 않는다(열린 질문).
- One UI 잠금 화면의 기본 "내용 숨기기" 설정이 어떻게 보이는지는 확인 안 됨 → 실기기로 확인(5단계 완료 기준).

**탭 → 오늘 화면 (Navigation 3)**

- 공식 문서: 일반 화면을 열 때는 새 task 로 열어 뒤로 가기가 자연스럽게 하고, `PendingIntent` 에는 `FLAG_IMMUTABLE` 을 쓴다(예시는 `FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE`). `setAutoCancel(true)`.
- 이 앱은 Activity 가 하나이므로 `TaskStackBuilder` 가 필요 없다. `Intent(context, MainActivity::class.java)` + `FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK` + extra `open_tab=today` → `PendingIntent.getActivity(...)`.
- Navigation 3 는 1.2.0(이 프로젝트 버전)부터 `DeepLinkRequest`/`DeepLinkMatcher` 딥 링크를 지원하지만, 이는 URI + intent-filter 가 기준이다. 앱 내부 알림 하나에 외부 공개 URI 를 만들 필요는 없다 → **extra 를 읽어 `backStackFor(TODAY, startTab)` 로 백 스택을 바꾸는 방식**을 추천한다(4단계에서 준비한 함수 재사용). `onNewIntent` 도 처리한다(앱이 떠 있을 때).
- 알림 탭은 앱 대기 버킷을 Active 로 올린다(§3).

출처: [Notification runtime permission](https://developer.android.com/develop/ui/views/notifications/notification-permission) · [Request runtime permissions](https://developer.android.com/training/permissions/requesting) · [Create a notification](https://developer.android.com/develop/ui/views/notifications/build-notification) · [Start an Activity from a notification](https://developer.android.com/develop/ui/views/notifications/navigation) · [Navigation 3 deep links](https://developer.android.com/guide/navigation/navigation-3/deep-links)

## 5. 시험 방법

**단위 시험 (`app/src/test`, Robolectric)**

- `:core`: `nextNotifyAt(now, timeZone, notifyTime)` — 09:00 전/후, 자정 근처, 서머타임 시간대(예: `America/New_York` 3월 · 11월 전환일), 시간대 변경. kotlinx-datetime 이 서머타임 공백 시각을 어떻게 옮기는지는 확인 안 됨 → 시험으로 고정.
- Worker: `TestListenableWorkerBuilder<DailySummaryWorker>(context).build()` 후 `runBlocking { worker.doWork() }` — 메모리 DB 로 "보낼 게 있을 때 / 없을 때 / 알림 꺼짐 / 이미 오늘 보냄". (공식 문서는 CoroutineWorker 에는 이 빌더를 강하게 권장한다.)
- 예약: `WorkManagerTestInitHelper.initializeTestWorkManager(context, Configuration.Builder().setExecutor(SynchronousExecutor()).build())` → `getWorkInfosForUniqueWork("daily_summary")` 로 ENQUEUED 확인, `TestDriver.setPeriodDelayMet(id)` · `setInitialDelayMet(id)` 로 시간 경과 흉내.
- 공식 문서 예시는 `androidTestImplementation`(계측 시험) 기준이다. Robolectric 로컬 시험에서 그대로 되는지는 확인 안 됨 → 첫 시험에서 확인하고, 안 되면 Worker 내용 시험만 남긴다.

**실기기 (S26, adb)** — `PKG=com.kikilog.routinecalendar`

| 목적 | 명령 | 출처 |
|---|---|---|
| 예약 상태 보기 | `adb shell dumpsys jobscheduler` (`SystemJobService`, 패키지 이름으로 찾기 · 대기 버킷 · 만족 안 된 제약 표시) | 공식 |
| WorkManager 진단(디버그 빌드) | `adb shell am broadcast -a "androidx.work.diagnostics.REQUEST_DIAGNOSTICS" -p $PKG` → logcat `WM-DiagnosticsWrkr` | 공식 |
| 작업 바로 실행 | `adb shell cmd jobscheduler run -f $PKG <JOB_ID>` (JOB_ID 는 dumpsys 에서) | AOSP 도움말 · 블로그 (공식 가이드 아님) |
| Doze 진입 / 해제 | `adb shell dumpsys deviceidle force-idle` → `... unforce` → `adb shell dumpsys battery reset` | 공식 |
| 앱 대기 | `adb shell dumpsys battery unplug` · `adb shell am set-inactive $PKG true` / `false` · `am get-inactive $PKG` | 공식 |
| 버킷 확인 | `adb shell am get-standby-bucket $PKG` (`set-standby-bucket` 은 확인 안 됨) | 공식 |
| 권한 처음 상태로 | `adb shell pm revoke $PKG android.permission.POST_NOTIFICATIONS` + `pm clear-permission-flags $PKG android.permission.POST_NOTIFICATIONS user-set user-fixed` | 공식 |

- WorkManager 상세 로그: `Configuration.Builder().setMinimumLoggingLevel(Log.DEBUG)` (기본 초기화를 끄고 `Configuration.Provider` 구현 필요) — 디버그 확인용, 필수는 아님.
- 재부팅 확인: 알림 시각을 몇 분 뒤로 설정 → 재부팅 → 잠금 해제하지 않고/해제하고 각각 확인. (WorkManager 서비스는 `directBootAware=false` 라 첫 잠금 해제 전에는 돌지 않는다고 본다 — 확인 안 됨.)
- CLAUDE.md 규칙상 adb 로 기기 상태를 바꾸는 명령은 매번 확인 대상이다.

출처: [Integration testing](https://developer.android.com/develop/background-work/background-tasks/testing/persistent/integration-testing) · [Testing Worker implementation](https://developer.android.com/develop/background-work/background-tasks/testing/persistent/worker-impl) · [Debug WorkManager](https://developer.android.com/develop/background-work/background-tasks/testing/persistent/debug) · [Doze and App Standby](https://developer.android.com/training/monitoring-device-state/doze-standby) · [App Standby Buckets](https://developer.android.com/topic/performance/appstandby) · [Request runtime permissions](https://developer.android.com/training/permissions/requesting) · [DEV — Android adb commands: jobs](https://dev.to/neokleoys2005/android-adb-commands-part-3-jobs-1fgi)

---

## 구현 순서 체크리스트

- [ ] ① 빌드 설정: `work = "2.12.0"`, `work-runtime-ktx` · `work-testing` 추가 → `./gradlew test assembleDebug` 통과, Room 판 충돌 없는지 확인
- [ ] ② `:core` 다음 알림 시각 계산 + 요약 대상 고르기(`notify=true` · 보관 제외 · 오늘 + 지난 것, `TodayBoard` 재사용) + 단위 시험
- [ ] ③ DataStore: `notify_enabled` · `notify_time`(기본 09:00) · `last_notified_on`
- [ ] ④ `notify/` 패키지: 채널 생성(Application), 알림 만들기(InboxStyle + 공개 버전 + PendingIntent)
- [ ] ⑤ `DailySummaryWorker`(CoroutineWorker): 권한 · 오늘 이미 보냄 확인 → 알림 → 다음 시각 `UPDATE` 재등록 + Worker 시험
- [ ] ⑥ `NotifyScheduler`: `schedule()`(앱 시작 · 설정 변경 · Worker 끝) · `cancel()`(알림 끔) + 예약 시험
- [ ] ⑦ 설정 화면: 알림 스위치 · 시각 선택 · 권한 요청/거절 표시 · 배터리 안내와 설정 열기
- [ ] ⑧ 알림 탭 → 오늘 탭(`onCreate` · `onNewIntent`)
- [ ] ⑨ Manifest: `POST_NOTIFICATIONS` 선언, 병합 Manifest(`app/build/intermediates/merged_manifest`)에서 권한 목록 확인
- [ ] ⑩ 실기기: 몇 분 뒤 시각으로 알림 · 거절 후 앱 정상 · 재부팅 후 유지 · 잠금 화면 문구 · Doze 흉내 · 며칠 그대로 두고 도착 시각 기록 → DEVLOG
- [ ] ⑪ 문서: PLAN 5단계 세부 계획 · 체크, 개인정보처리방침 초안에 "알림은 기기 안에서만 만든다" 메모

## 열린 질문

1. 오늘 할 것도 지난 것도 0개인 날 — 알림을 보내지 않을까, "오늘은 할 일이 없어요" 를 보낼까? (추천: 보내지 않음)
2. 실행이 많이 늦어진 경우(예: 18시 이후) — 그래도 보낼까, 그날은 건너뛸까?
3. 권한 요청 시점 — 오늘 화면 카드 + 설정 스위치(추천)로 할까, 설정 스위치만 둘까?
4. 라이브러리가 넣는 `FOREGROUND_SERVICE` 를 `tools:node="remove"` 로 뺄까? (빼도 안전한지는 확인 필요)
5. CLAUDE.md 규칙 2 문구를 "런타임 권한은 알림 하나(라이브러리의 일반 권한은 허용)" 로 고칠까?
6. 잠금 화면 공개 버전에 개수만 보일까, 루틴 이름까지 보일까? (추천: 개수만)
7. 알림 시각 기본값은 09:00 그대로 둘까? 시각 선택 단위(분 단위 / 30분 단위)는?
