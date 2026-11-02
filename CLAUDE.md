# Routine Calendar

자취 생활의 주기 루틴(집안일 · 뷰티 · 소모품 구매)을 한 번 등록하면 월 캘린더에서 예정일과 완료 체크를 함께 보는 Android 앱.
직접 쓰고 포트폴리오로 제출하며, Google Play 에 출시한다. 수익은 목표가 아니다.

- 패키지: `com.kikilog.routinecalendar` (출시 후 변경 불가)
- 제작 계획 · 데이터 구조 · 개발 순서: [docs/PLAN.md](docs/PLAN.md) — 작업 전에 먼저 읽는다
- 진행 상황은 PLAN.md 의 단계 체크박스로 관리한다
- 개발 일지: [docs/DEVLOG.md](docs/DEVLOG.md) — 단계를 끝내거나 하루 작업을 마칠 때 그날 절에 "한 일 · 진행 중 · 막힌 점 · 메모"를 덧붙인다. 막힌 점에는 증상(오류 원문) · 원인 · 해결을 함께 적는다. 기기 IP · 시리얼 · 페어링 코드 · 키는 적지 않는다

## 기술 선택 (확정)

- Kotlin + Jetpack Compose (Material 3), Android 만 출시. AGP 9 · Gradle 9 · Kotlin 내장 플러그인 구성
- minSdk 26 · compile/targetSdk 37
- 저장: Room (로컬 전용). 설정값: DataStore
- 날짜: `kotlinx-datetime` (`java.time` 대신 — iOS 이전 대비)
- 알림: WorkManager 하루 1회 요약 알림. 정확한 알람 권한(`SCHEDULE_EXACT_ALARM` · `USE_EXACT_ALARM`)은 쓰지 않는다
- 위젯: Jetpack Glance
- 백업: JSON 내보내기/가져오기 (`kotlinx-serialization`, 저장소 접근 프레임워크 — 저장소 권한 없이)
- 의존성 주입: 라이브러리 없이 수동(`AppContainer`). 규모가 커지면 다시 검토

## 지켜야 할 규칙

1. **로컬 전용.** 서버 · 로그인 · 네트워크 호출을 넣지 않는다. `INTERNET` 권한을 추가하지 않는다.
2. **권한은 알림(`POST_NOTIFICATIONS`) 하나.** 거절해도 앱의 나머지 기능은 동작해야 한다.
3. **분석 · 광고 · 크래시 수집 SDK 금지.** Play 데이터 보안 양식에 "수집 · 공유 없음" 으로 적기 위해서다.
4. **iOS 로 옮길 길을 열어 둔다.** 주기 계산과 도메인 모델은 `:core` 모듈(순수 Kotlin, `android.*` import 금지)에 둔다. 알림 · 위젯 · Room 같은 Android 전용 코드는 `:app` 의 별도 패키지에 모은다. 새 라이브러리는 KMP 지원 여부를 확인하고 고른다.
5. **버전은 `gradle/libs.versions.toml` 에서만** 관리한다. build 파일에 버전 문자열을 직접 쓰지 않는다.
6. **비밀값은 저장소에 넣지 않는다.** 업로드 키(`*.jks` · `*.keystore`) · `keystore.properties` · `local.properties` 는 `.gitignore` 에 있다. 커밋 전에 `git diff --staged` 로 확인한다.
7. **간단함 우선.** 할 일 · 습관 · 가계부 같은 기능을 덧붙이지 않는다. 새 기능은 PLAN.md 의 "하지 않을 것" 과 먼저 대조한다.
8. **시험.** 주기 계산(`:core`)은 단위 시험을 먼저 쓴다. 커밋 전에 `./gradlew test assembleDebug` 가 통과해야 한다.

## 빌드 · 실행

- JDK: Android Studio 내장 JBR (`C:\Program Files\Android\Android Studio\jbr`)
- SDK: `E:\Android\Sdk` (`local.properties`)
- 명령줄: `./gradlew testDebugUnitTest assembleDebug` · `./gradlew installDebug`
- 시험 기기: Galaxy S26 (SM-S942N, Android 17 / API 37), 무선 디버깅. 에뮬레이터는 쓰지 않는다

## 커밋 규칙

- Conventional Commits. 머리말 `type(scope):` 은 영어, 설명은 한국어. 제목 50자 안팎, 마침표 없음
  - type: `feat` · `fix` · `docs` · `refactor` · `test` · `build` · `chore`
  - scope: `core` · `data` · `ui` · `calendar` · `notify` · `widget` · `backup` · `release`
- 본문에는 왜 바꿨는지와 주요 변경점을 `-` 목록으로. 한 커밋에는 한 가지 일만
- 예: `feat(core): 완료일 기준 다음 예정일 계산 추가`
- 커밋 · 푸시는 사용자가 요청할 때만 한다. 강제 푸시하지 않는다
