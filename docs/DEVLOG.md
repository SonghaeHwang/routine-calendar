# 개발 일지

Routine Calendar 를 만들며 한 일을 날짜별로 남긴다. 형식: 한 일 · 진행 중 · 막힌 점 · 메모.
기기 IP · 시리얼 · 페어링 코드 · 키 같은 값은 적지 않는다.

---

## 2026-11-02 (월)

### 한 일

- **아이디어 정리 · 시장 조사**
  - 계기: 자취하며 이불 세탁 · 청소 · 빨래 · 화장실 청소, 모델링팩 · 각질제거 같은 주기 관리를 한눈에 볼 수 없었다. 손으로 적기는 싫고 종이는 잃어버리고 웹은 열기 번거롭다
  - 국내외 비슷한 앱 조사: Tody · Sweepy(각 앱스토어 평가 약 1.1만 개), LastWhen 같은 "마지막으로 한 지 며칠" 앱, Skincare Routine. 해외는 포화에 가깝고, 집안일 + 뷰티 + 소모품 구매를 한 월 캘린더에 모은 한국어 앱은 찾지 못함
  - 차별점으로 잡은 것: 완료일 기준 재계산 + 월 캘린더에 예정과 체크를 함께, 한국 자취 · 뷰티 프리셋
  - 목적 확정: 수익 없이 직접 쓰고 포트폴리오로 제출, Google Play 출시
- **보안 · 출시 요건 결정**
  - 서버 · 로그인 없이 기기 안에만 저장(로컬 전용). 권한은 알림 하나, 분석 · 광고 SDK 없음
  - Play 요건 확인: 수집이 없어도 데이터 보안 양식 + 개인정보처리방침 필요, 2023-11-13 이후 개인 계정은 테스터 12명 · 14일 비공개 테스트 후 프로덕션 신청
  - 알림은 날짜 단위라 정확한 알람 권한 없이 WorkManager 로 충분
- **기술 선택**
  - Kotlin + Jetpack Compose. 이유: 홈 화면 위젯(Glance) · 알림(WorkManager) · Room 을 공식 도구로 바로 쓸 수 있고, Android 만 출시
  - iOS 는 지금 내지 않는다. 대신 주기 계산 · 모델을 순수 Kotlin `:core` 모듈로 분리하고 `kotlinx-datetime` 을 써서 나중에 Kotlin Multiplatform 으로 옮길 길을 열어 둠 (iOS 는 Mac · Apple 계정 필요, 위젯 · 알림은 따로 작성)
  - 패키지 이름 `com.kikilog.routinecalendar` — 실명 대신 브랜드 "kikilog". 플레이스토어에 같은 패키지 없음 확인
- **개발 환경**
  - PC 확인: 메모리 30GB, C:(NVMe) 55GB 남음 · E:(같은 NVMe) 123GB 남음, Ryzen 7 3700U, 가상화 켜짐
  - Android Studio 2026.2.2 설치(winget). SDK 는 C: 공간을 아끼려고 `E:\Android\Sdk` 에, 에뮬레이터는 받지 않고 실기기(Galaxy S26, Android 17)로 시험
  - 가상환경(.venv) 대신 Gradle wrapper · 버전 카탈로그가 프로젝트별 버전을 고정한다는 것을 정리
- **무선 디버깅 연결**
  - SDK Platform-Tools(adb) 를 받아 Google 서명 확인 후 사용
  - 페어링 → 연결 → `adb devices` 에 기기 표시까지 확인
- **프로젝트 생성**
  - New Project(Empty Activity, Compose): AGP 9.5.0 · Gradle 9.6.0 · Kotlin 2.2.10(compose 플러그인) · minSdk 26 · compile/targetSdk 37
  - `.gitignore` 보강(서명 키 · `keystore.properties` · `.env` · `.kotlin/` · 출시 산출물), `.gitattributes` 추가
  - `testDebugUnitTest assembleDebug` 통과, S26 에 설치해 첫 실행 확인(콜드 스타트 약 0.34초)
  - `CLAUDE.md`(규칙 · 기술 선택 · 커밋 규칙), `docs/PLAN.md`(화면 · 데이터 구조 · 개발 순서 0~10단계) 작성
  - 첫 커밋 `ec58b1f chore: 프로젝트 뼈대와 제작 계획 추가`, GitHub private 저장소 만들고 push
  - PLAN 0단계(뼈대) 완료
- **PLAN 1단계 — 주기 계산(`:core`)**
  - `:core` 모듈 추가: 순수 Kotlin/JVM(`org.jetbrains.kotlin.jvm`, Java 11), `kotlinx-datetime` 0.8.0 을 `api` 로 노출. `:app` 이 `:core` 를 의존하도록 연결
  - 모델: `Category` · `Interval`(일/주/개월, `addTo(date, times)`) · `Routine` · `Completion`
  - 계산: `nextDue`(마지막 완료일 + 주기, 없으면 `firstDue`, 반복 없음은 완료 시 끝) · `occurrencesIn`(날짜 범위 안 예정일 추정) · `overdueDays` · `initialSchedule`(등록 때 첫 예정일과 함께 저장할 완료 기록)
  - 단위 시험 30개 통과(`IntervalTest` 7 · `ScheduleTest` 23): 일/주/개월 · 월말(1/31+1개월=2/28) · 윤년(2028-02-29) · 늦게/일찍 완료 · 반복 없음 · 체크 취소 후 재계산 · 다른 루틴 기록 무시 · 등록 규칙
  - `./gradlew test assembleDebug --rerun-tasks` 통과
  - 설계 결정
    - 개월 단위를 펼칠 때 앞 날짜에 계속 더하지 않고 `기준일 + 주기 × k` 로 계산 — 이어 더하면 1/31 → 2/28 → 3/28 로 밀린다. 지금은 1/31 → 2/28 → 3/31
    - `occurrencesIn` 은 달 대신 날짜 범위를 받는다. 캘린더 그리드에 앞뒤 달 날짜도 보이기 때문
    - `created_at` 은 계산에 쓰지 않아 `:core` 모델에서 뺀다. 2단계 Room 엔티티에만 둔다
    - 프리셋은 3단계로 미룸
  - PLAN 1단계 완료
- **커밋 · 푸시**
  - `core/build/` 가 무시되지 않아 빌드 산출물이 `git add` 에 잡히는 것을 발견 → `app/.gitignore` 와 같게 `core/.gitignore`(`/build`) 추가
  - 한 커밋에 한 가지 일 규칙대로 둘로 나눔: `a3ae70f feat(core): 주기 계산 모듈과 단위 시험 추가`(코드 · Gradle 12개 파일) · `8523273 docs: 1단계 완료와 개발 일지 기록`(PLAN · DEVLOG)
  - 커밋 규칙 변경: 작업 단위가 끝나고 `test assembleDebug` 가 통과하면 묻지 않고 커밋 · `origin/main` 푸시. 강제 푸시와 푸시한 커밋의 amend · rebase 는 계속 금지 → `756e5dc docs: 커밋 · 푸시를 묻지 않고 하도록 규칙 변경`
  - 세 커밋을 GitHub 에 푸시(`main` = `origin/main`)
- **PLAN 2단계 — 저장(Room)**
  - 계획: Plan 에이전트로 세부 계획을 세워 PLAN.md 2단계 아래에 요약(`d9eed32`). DataStore 는 처음 쓰는 4 · 5단계로 미룸
  - 빌드(`f8fd3c7`): Room 2.8.5(runtime · compiler) + KSP 2.3.13, Room Gradle 플러그인으로 스키마를 `app/schemas` 에. 시험용 Robolectric 4.17 · androidx.test core 1.7.0 · kotlinx-coroutines-test 1.11.0. 걱정했던 KSP 와 AGP 9 내장 Kotlin 충돌은 없었음(우회 설정 불필요)
  - 엔티티 · DB(`589affc`): `routine` · `completion` 테이블, FK `ON DELETE CASCADE`, `(routine_id, done_on)` 유일 인덱스. `LocalDate` 는 ISO 문자열, enum 은 이름, `created_at` 은 epoch millis(Kotlin 2.2 의 `kotlin.time.Instant` opt-in 을 피함). 스키마 `1.json` 커밋
  - DAO(`afbd699`): 등록 + 초기 완료 기록 트랜잭션, 같은 날 중복은 `IGNORE`(-1), 체크 취소는 `(routine_id, done_on)` 으로, 보관 제외 조회 Flow, 일부 열만 바꾸는 수정
  - 리포지토리(`2fb1cd3`): `observeRoutines` 가 루틴 + 완료 기록을 Flow 하나로 내보냄(오늘 · 캘린더가 같은 흐름에 `:core` 계산을 얹도록). `add` 는 `initialSchedule` 로 첫 예정일을 정하고 빈 제목 · 미래의 마지막으로 한 날을 거부, `check` 는 미래 날짜 거부 · 중복이면 false
  - `AppContainer` · `RoutineCalendarApp`(`3979058`): DB · 리포지토리를 lazy 로 생성, Manifest 에 Application 등록. 오늘 날짜는 `java.time.LocalDate.now().toKotlinLocalDate()`
  - 시험 31개 통과(기기 없이 PC 에서): `RoutineDaoTest` 11(Robolectric · 메모리 DB) · `RoutineRepositoryTest` 12 · `MappersTest` 7 · 기본 예제 1. `:core` 30개 포함 `./gradlew test assembleDebug` 통과
  - 규칙 추가(VS Code 쪽에서 `68c84ad` · `844b705`): 권한 확인 창 앞에 `[권한 확인 안내]` 를 한국어로 쓰고, 같은 목적의 명령은 한 번에 묶는다 → 시험이 통과할 때만 커밋 · 푸시하도록 한 명령으로 묶음
  - PLAN 2단계 완료. 휴대폰 설치 확인은 화면이 생기는 3단계에서 함께 하기로 함

### 진행 중

- PLAN 3단계(등록 · 루틴 목록 · 오늘 화면) — 저녁에 1~8번까지 진행, 2026-11-03 절에 이어서 기록

### 막힌 점

- **무선 디버깅이 켜지지 않음**
  - 증상: 개발자 옵션의 무선 디버깅을 켜려 하자 "보안 위험 자동 차단에서 차단했다"는 안내
  - 원인: 삼성 One UI 의 "보안 위험 자동 차단"이 USB · 무선 디버깅과 출처 모를 앱 설치를 막는다
  - 해결: 설정 → 보안 및 개인정보 보호 → 보안 위험 자동 차단을 끄고 무선 디버깅을 켬. 개발이 끝나면 다시 켜기로 함
- **페어링 포트를 몰라 연결이 안 됨**
  - 증상: 페어링 화면의 6자리 숫자를 포트로 착각 (포트는 65535 까지라 6자리일 수 없음 — 그 숫자는 페어링 코드)
  - 해결: `adb mdns services` 로 같은 와이파이의 기기를 찾아 페어링 포트(`_adb-tls-pairing`)와 연결 포트(`_adb-tls-connect`)를 알아낸 뒤 `adb pair` → `adb connect`
  - 메모: 연결 포트는 무선 디버깅을 껐다 켤 때마다 바뀐다. 페어링은 한 번만 하면 된다
- **프로젝트 파일을 직접 만들지 않고 마법사로 바꿈**
  - 원인: Google Maven 을 보니 최신이 AGP 9.5 — AGP 9 는 Kotlin 내장 · 새 DSL(`compileSdk { version = release(37) }`) 등 설정이 크게 바뀐 판이라, 손으로 버전 조합을 맞추면 빌드 오류 가능성이 높았다
  - 해결: Android Studio New Project 마법사로 만들고, 결과를 확인 · 보강하는 방식으로 진행
- **Windows 줄바꿈 경고**
  - 증상: `git add` 때 모든 파일에 "LF will be replaced by CRLF" 경고
  - 원인: Windows git 의 줄바꿈 자동 변환. `gradlew` 가 CRLF 가 되면 Linux CI 에서 실행되지 않는다
  - 해결: `.gitattributes` 로 `gradlew` 는 LF, `*.bat` 는 CRLF 로 고정하고 `git ls-files --eol` 로 확인
- **PLAN 의 등록 규칙이 반복 없는 루틴과 모순**
  - 증상: "마지막으로 한 날을 넣으면 그날을 완료 기록으로 저장" 을 반복 없음("화장지 2달 후")에도 적용하면, 등록하자마자 완료 기록이 생겨 루틴이 끝난 것으로 계산됨
  - 해결: 반복하는 루틴만 완료 기록을 남기고, 반복 없음은 `first_due = 그날 + 주기` 만 정한다. PLAN.md 문구도 고침. 시험 `등록 - 반복 없음, 마지막으로 한 날을 넣어도 기록은 남기지 않는다` 로 고정
- **빌드 파일의 `alias(libs.plugins.kotlin.jvm)` 에 빨간 줄**
  - 증상: `Argument type mismatch: actual type is 'JvmToolchainManagement', but 'Provider<PluginDependency>' was expected`
  - 원인: 버전 카탈로그에 `kotlin-jvm` 을 넣은 뒤 동기화 전이라 접근자가 아직 없어서, IDE 가 `jvm` 을 Gradle 의 `toolchainManagement.jvm` 으로 잘못 해석
  - 해결: Gradle 동기화 후 사라짐(코드 문제 아님)
- **Android Studio Claude 의 도구가 자주 시간 초과**
  - 증상: IDE 빌드 도구 · 셸 명령이 `The operation timed out.` 을 내고 멈춘 것처럼 보임. 실제로는 뒤에서 빌드가 계속 돌아 성공해 있었음
  - 해결: Gradle 을 셸에서 백그라운드로 돌리고 로그 파일과 `core/build/test-results` 의 시험 결과 XML(시각 · 실패 수)로 확인. 시간 초과를 실패로 보지 말고 결과 파일을 확인한다
- **`git commit` · `git push` 가 시간 초과로 멈춘 것처럼 보임**
  - 증상: `git log` · `git status` 는 1초 안에 끝나는데 `git commit -F <메시지 파일>` 과 `git push` 만 `The operation timed out.` 후 커밋이 없는 것으로 보임
  - 확인한 것: `.git/hooks` 에 hook 없음, 커밋 서명(`commit.gpgsign`) 설정 없음, `core.editor` 는 `code --wait` 지만 `-F` 를 써서 편집기는 열리지 않음
  - 결과: 시간이 지난 뒤 보니 두 커밋이 정상으로 만들어져 있었고, 푸시도 시간 초과 표시와 달리 올라가 있었음. 원인은 확실하지 않음 — IDE 가 AI 의 파일 변경을 "수락" 하기 전이었거나 셸 명령 승인 대기였을 가능성
  - 해결: 시간 초과가 떠도 다시 실행하지 말고 `git log --oneline` · `git status -sb` 로 결과부터 확인한다(중복 커밋 방지)
  - 추가 관찰: 사용자가 IDE 에서 변경을 수락하자 기다리던 커밋이 바로 만들어졌다(두 번). 새 파일만 담은 커밋은 기다리지 않고 바로 됐다. 그 뒤로는 셸 명령마다 승인 창이 뜨고, 승인하면 끝까지 실행됨 — 시간 초과는 대부분 승인 대기였던 것으로 봄
- **Robolectric DAO 시험 11개가 모두 시작 단계에서 실패**
  - 증상: `java.lang.RuntimeException: Failed to interact with raw FileDescriptor internals; perhaps JRE has changed?` / `Caused by: java.lang.IllegalAccessException: ... cannot access class jdk.internal.access.SharedSecrets (in module java.base) because module java.base does not export jdk.internal.access to unnamed module`. 로그에 `java.lang.System::load has been called by org.robolectric.nativeruntime.DefaultNativeRuntimeLoader` 경고도 있음
  - 원인: 시험 JVM 이 새 JDK(Gradle 데몬 툴체인 25)라 Java 모듈 시스템이 내부 패키지 접근을 막음. 시험 코드 · DAO 문제 아님(KSP 생성 · 컴파일은 통과)
  - 해결: `app/build.gradle.kts` 의 `testOptions.unitTests.all { it.jvmArgs(...) }` 에 `--add-exports=java.base/jdk.internal.access=ALL-UNNAMED` · `--add-opens=java.base/java.io=ALL-UNNAMED` · `--enable-native-access=ALL-UNNAMED` 추가(`4e8a830`). 시험 JVM 에만 적용되고 앱 빌드 · 새 JDK 다운로드와 무관. 경고도 사라짐
- **시험 결과 XML 을 IDE 검색 도구로 못 읽음**
  - 증상: `Blocked by AI exclude policies: .../app/build/test-results/...`
  - 해결: 셸로 결과 XML 을 scratchpad 로 복사해 읽고, 이후에는 시험 명령 끝에 XML 요약(tests · failures)을 출력하도록 묶음

### 메모

- Android Studio 의 AI 패널에서 Claude 를 고르면 Android Studio 도구(Gradle 빌드 · 기기 배포 · Compose 미리보기 · Logcat)를 쓸 수 있다. 앱 코드는 그쪽 Claude, 계획 · 문서 · 일지는 VS Code 의 Claude 로 나눠 쓴다. 두 Claude 가 같은 파일을 동시에 고치지 않는다
- 휴대폰이 필요 없는 작업(1단계 `:core` 단위 시험 등)에는 무선 디버깅을 끄고 자동 차단을 켜 둔다. 휴대폰 설치 · 화면 확인이 필요할 때만 켠다
- 2단계도 휴대폰 없이 끝냄: 시험은 모두 PC JVM(일반 JUnit · Robolectric)에서 돌고, `assembleDebug` 는 APK 를 만들기만 한다. 기기는 `installDebug` 로 직접 볼 때만 필요
- 정하지 않은 것: 앱 이름 · 아이콘, "주 N회" 지원 여부, Android 자동 백업 사용 여부
- Android Studio 의 Claude 는 "Claude Code 플러그인(JetBrains)" 이 아니라 Android Studio AI 패널에서 모델로 Claude 를 고른 방식. 안에서는 Claude Agent SDK 로 Claude Code 엔진이 돌고, Android Studio 가 자체 도구(`studio_*`)와 지시문을 얹는다
- 한국어 언어 팩: AI 도구로는 플러그인 설치 · IDE 언어 변경을 할 수 없어 직접 하기로 함 — Settings → Plugins → Marketplace 에서 "Korean Language Pack / 한국어 언어 팩" 설치 후 재시작(안 바뀌면 Appearance & Behavior → System Settings → Language and Region). Android Studio 는 IntelliJ 보다 플랫폼 버전이 늦어 "Incompatible" 일 수 있고, Android 전용 메뉴는 영어로 남을 수 있다

---

## 2026-11-03 (화)

### 한 일

- **PLAN 3단계 — 등록 · 루틴 목록 · 오늘 화면** (11-02 저녁 시작)
  - 계획: Plan 에이전트로 세부 계획 → PLAN 3단계에 요약. 정한 것 4가지는 예시 화면(글자 그림)을 보고 고름 — 선택 창의 미리보기가 Android Studio 패널에서 안 보여 대화로 다시 보여 줌
  - ① 기본 의존성 올림(activity 1.13 · lifecycle 2.11 · core-ktx 1.19 · Compose BOM 2026.09) ② Navigation 3 1.2.0 · lifecycle-viewmodel-navigation3 · kotlinx-serialization 1.9.0(Kotlin 2.2.10 과 맞는 판) ③ `:core` 오늘 화면 분류 `buildTodayBoard` · `weekEndOf` · `lastDoneOn` ④ 기본 프리셋 14개 ⑤ 오늘 날짜 흐름 `TodayClock`(자정 갱신 + 화면이 다시 보일 때 새로 읽기) ⑥ 날짜 · 주기 표시 형식과 한국어 문자열 ⑦ 하단 탭 뼈대(휴대폰 확인 ① 통과) ⑧ 등록 화면과 프리셋 ⑨ 루틴 목록 ⑩ 오늘 화면과 체크 ⑪ 수정 시 처음 할 날 변경(저장소) ⑫ 수정 화면 ⑬ 보관 · 삭제 · 보관함
  - 휴대폰에서 써 보며 바꾼 것(사용자 요청 · 발견)
    - 오늘 화면 아래 칸을 "이번 주(일~토)" → "다가오는 7일", 저장하면 「○○ 등록했어요 · 다음: 날짜」 안내 (아래 막힌 점)
    - 첫 화면을 캘린더 탭으로. 뒤로 가기 기준도 첫 화면. 첫 화면 고르기 설정은 4단계로 기록
    - 빠른 선택 편집: preset 테이블(DB 버전 2, 자동 마이그레이션 뒤 기본 14개 채움), 설정 → 빠른 선택 편집(고치기 · 추가 · 삭제 · 기본값 되돌리기). 휴대폰 업데이트 뒤 기존 루틴이 그대로 남는 것까지 확인
    - 오늘 화면 줄을 꼭 누르면 수정 화면, 수정(상세) 화면에 등록일(이미 저장 중이던 created_at)
    - 같은 이름 루틴: 이름 칸 안내 + 등록 시 확인 창 [기존 루틴 열기 · 그래도 추가 · 취소]
    - 루틴 목록 선택 모드: 꼭 누르면 선택, 위쪽 바에 선택 삭제 · 전체 삭제(확인 창), 더보기에 선택 보관
    - 나중에 할 것으로 기록: 분류별로 보기(8단계), 첫 화면 고르기(4단계)
  - 14번(Compose 화면 시험)은 건너뜀: ViewModel 시험(Robolectric + 메모리 DB)과 실기기 확인으로 충분하다고 판단
  - 시험: `:core` · `:app` 모두 통과. 주요 시험 — TodayBoardTest 14 · EditRoutineViewModelTest 24 · TodayViewModelTest 5 · RoutinesViewModelTest 5 · PresetRepositoryTest 7 · DatabaseMigrationTest 2 · PresetsViewModelTest 5 · PresetFormTest 4
  - 기기에 설치하는 방법을 `adb` 셸 명령에서 Android Studio 배포 도구로 바꿈(승인 창 없이 설치 · 실행)
- **PLAN 4단계 — 캘린더** (완료)
  - 4-1 `:core` 달력 칸(`monthGridDates` · `weekDays`)과 날짜별 예정 · 완료(`buildCalendarDays` · `calendarEntriesOn`)
  - 4-2 DataStore 설정 저장소(주 시작 요일 · 첫 화면). 첫 화면은 앱을 켤 때 한 번 읽어 그 탭으로 시작
  - 4-3 캘린더 화면: 월 이동 · 「오늘」 단추 · 칸마다 ● 예정(지난 날은 빨강) · ✓ 완료, 날짜를 누르면 아래에 그날 목록, 오늘까지는 체크 · 해제 가능, 길게 누르면 수정
  - 4-4 설정 화면에 주 시작 요일(일요일/월요일) · 첫 화면(오늘/캘린더/루틴) 단추 줄
  - 휴대폰 확인: 월 이동, 11월 3일 예정 4개 · 2일 완료 1개가 오늘 화면과 같음, 월요일로 바꾸면 캘린더가 월~일로 바로 바뀜(확인 뒤 일요일로 되돌림)
  - 커밋 전마다 규칙 검사(rule-checker 지시문을 일반 에이전트에 넘겨 실행) — 모두 통과, 새 의존성 · 권한 없음
  - 시험: `:app` 120개 모두 통과(CalendarViewModelTest 6 · SettingsViewModelTest 3 · SettingsRepositoryTest 3 추가)
- **PLAN 5단계 — 알림** (코드 완료, 휴대폰 확인 남음)
  - 사전 조사(`docs/notes/notify.md`)의 열린 질문 7개를 Plan 모드에서 물어 정함(기본 오전 8:00 · 30분 단위 · 0개면 안 보냄 · 늦어도 보냄 · 잠금 화면은 개수만 등)
  - ① WorkManager 2.12.0 ② `:core` 다음 알림 시각 · 요약 대상 ③ DataStore 알림 설정 ④⑤ 채널 · 알림 · Worker · 예약(24시간 주기 + 다음 시각 지정) ⑥ 설정 스위치 · 시각 · 「알림 켤까요?」 카드 ⑦ 알림을 누르면 앱으로
  - 병합 Manifest: 앱이 선언한 권한은 `POST_NOTIFICATIONS` 하나, WorkManager 가 일반 권한 4개를 넣음. `INTERNET` 없음
  - 사용자가 휴대폰에서 알림을 켜고 시각을 오후 2:00 으로 두고 확인 중
- **사용자 수정 요청 8가지** (5단계 도중, Plan 모드로 계획 → 질문 7개 → 9단위 커밋)
  - `:core` 루틴 시작일(`startSchedule`) · 예정일 미리보기(`nextDueDates`)
  - 분류를 고정 enum 에서 `category` 테이블로(DB 버전 3). 색 계열 9 × 5단계, 기본 집안일 초록 · 뷰티 분홍 · 구매 파랑 · 기타 회색
  - 분류 편집 화면(추가 · 이름 · 색 · 삭제 시 기타로 이동), 빠른 선택을 꾹 눌러 다른 분류로 끌어 옮기기
  - 루틴 정보 화면(루틴 탭 누르기 · 달력 꾹 누르기 → 「편집하기」)
  - 달력: 이름 변경 · 좌우로 밀어 달 이동 · 분류당 색 점(최대 4, 놓친 것 빨간 테두리, 한 것 같은 색 ✓)
  - 오늘 탭 숨김(코드 보존). 알림을 누르면 달력, 알림 카드도 달력으로
  - 등록 화면 「마지막으로 한 날」 → 「루틴 시작일」(기본 오늘, 앞날 가능)
  - 휴대폰 확인: 업데이트 뒤 기존 루틴의 분류가 그대로(마이그레이션 2→3), 빠른 선택 끌어 옮기기, 달력 색 점 · 밀어서 다음 달 · 꾹 눌러 루틴 정보, 하단 탭 2개
  - 시험: `:core` · `:app` 모두 통과(새 시험 CategoryTest · CategoryRepositoryTest · CategoriesViewModelTest · RoutineInfoViewModelTest · 마이그레이션 2→3 등)
- **PLAN 3 · 5단계 체크**: 사용자가 사흘 써 보기와 알림 도착을 확인
- **백업 결정(7단계용)**: 가져오기는 합치기도 지원, 가져오기 직전 현재 상태 자동 저장, `allowBackup` 켬 + 암호화 조건. `bmgr` 시험은 앱을 지우므로 실행 전 사용자 확인, 그 전에 JSON 내보내기
- **PLAN 6단계 — 위젯** (코드 완료, 휴대폰 확인 남음)
  - widget.md 의 열린 질문은 앱과 맞추는 쪽으로 정함(PLAN 6단계에 기록)
  - ① Glance 1.2.0(appwidget · material3 · 시험 두 개) ②~⑤ 「오늘 루틴」 위젯: 지난 것 + 오늘, 체크 칸(`ToggleRoutineAction`), 분류 색 점, 비었을 때 다음 예정 하나, Responsive 두 크기, 누르면 달력, 정적 미리보기 ⑥ `WidgetSync`(앱 변경 → `updateAll`) ⑧ 자정 갱신(24시간 주기 + 다음 0시 1분 지정)
  - 시험: WidgetStateTest 5 · TodayWidgetTest 6 · WidgetSyncTest 3 · WidgetRefreshTest 3. 병합 Manifest 권한 변화 없음(`INTERNET` 없음)
  - 사용자 요청으로 위젯 목록 아이콘을 고양이 그림으로. 앱 아이콘도 같은 그림으로 8단계에서 바꿈
- **PLAN 7단계 — 백업** (코드 완료, 휴대폰 확인 남음)
  - 합치기 규칙을 물어 정함: 같은 이름 루틴은 기록만 합침 · 되돌리기는 설정의 단추 · 합칠 때 앱 설정 유지
  - ① kotlinx-serialization-json 1.9.0 ② 형식 · 검사(`decodeBackup`: 새 판 · 손상 · 5MB 초과 거부, 없는 분류는 기타로, 같은 날 기록 하나) ③ `BackupDao`(한 트랜잭션 읽기 · 덮어쓰기 · 합치기) ④ `BackupRepository`(가져오기 전 자동 저장 · 되돌리기) ⑤ 설정 화면 백업 메뉴 ⑥ 자동 백업 XML(DB · DataStore 만, 클라우드는 암호화될 때만) · 개인정보 안내 문구
  - 시험: BackupFormatTest 10 · BackupRepositoryTest 5 · BackupViewModelTest 4. 저장소 권한 · 네트워크 없음(규칙 검사 통과)

### 진행 중

- PLAN 6단계 ⑩ 휴대폰 확인: 위젯 놓기 · 크기 · 위젯에서 체크 → 앱 · 앱에서 체크 → 위젯 · 다크 모드 · 앱 종료 뒤 체크 · 자정. 끝나면 6단계 체크
- PLAN 7단계 ⑦ 휴대폰 확인: 내보내기 → 가져오기(합치기 · 덮어쓰기) → 되돌리기, 내 파일 · Google Drive 선택기. ⑧ bmgr 시험은 앱을 지우므로 사용자 확인 뒤(그 전에 JSON 내보내기)
- 보류: 오늘 화면에서 체크한 줄을 원래 칸(지난 것 · 다가오는 7일)에 남길지 — 요청 후 "변경하지 말 것" 으로 보류. 지금은 체크하면 "오늘" 칸에 체크된 채로 남는다

### 막힌 점

- **등록한 루틴이 오늘 화면에 안 보여 등록이 안 된 것처럼 보임**
  - 증상: 화요일에 「젤팩 · 어제 함 · 1주마다」를 저장해도 오늘 화면 어디에도 없음. 여러 번 다시 시도해 젤팩이 세 개 생김. Logcat 에 오류 없음
  - 원인: 다음 예정일(다음 주 월요일)이 "이번 주(일~토)" 밖이라 어느 칸에도 안 들어감. 저장 후 안내도 없었음. 코드 오류가 아니라 화면 설계 문제
  - 해결: 아래 칸을 "다가오는 7일" 로, 저장 후 안내 추가. 나중에 같은 이름 확인 창도 넣음
- **`gradlew` 를 혼자 실행하면 Java 를 못 찾음**
  - 증상: `ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.`
  - 원인: 확인 창을 줄이려고 `$env:JAVA_HOME=...;` 를 붙이지 않게 되자, 이 PC 에 JAVA_HOME 이 없다는 게 드러남
  - 해결: Windows 사용자 환경 변수 JAVA_HOME 을 Android Studio JBR 로 설정하고 Android Studio 재시작
- **빌드가 겨쳐 실패**
  - 증상: `NoSuchFileException: ...mergeExtDexDebug\classes2.dex` / `Unable to delete directory '...compileDebugUnitTestKotlin\cacheable' ... a process has files open`
  - 원인: 시간 초과로 보인 빌드가 실제로는 돌고 있었는데, 그사이 IDE 빌드 도구로 두 번 더 실행해 세 빌드가 같은 `app/build` 를 동시에 씀
  - 해결: 빌드는 하나만 돌리고 끝날 때까지 기다린다
- **커밋 메시지의 `\"` 때문에 커밋 명령이 깨짐**
  - 증상: 본문에 큰따옴표를 넣은 `git commit -m …` 가 시간 초과, 실행 목록에도 없고 커밋도 안 됨
  - 원인: PowerShell 에서 `\"` 는 이스케이프가 아니라 문자열을 끝낸다. 명령이 깨져 허용 목록 밖 명령이 됨
  - 해결: `-m '…'` 작은따옴표, 메시지 안에 큰따옴표 · 달러 · 백틱 금지(인용은 「」). CLAUDE.md 커밋 규칙에 추가
- **환경 변수 하나 없이 여러 명령을 묶으면 확인 창이 뜨는 문제**
  - 원인: 허용 목록에 있는 단순한 명령도 `$변수=` · `if` · `;` 로 잇으면 통째로 새 명령이 됨
  - 해결: CLAUDE.md 에 따라 `gradlew` 와 git 명령을 하나씩 따로 실행 → 그 뒤 확인 창 없이 실행됨
- **사소한 것**
  - Navigation 3: `backStack.add(...)` 가 Boolean 을 돌려줘 `() -> Unit` 자리에 넘기면 `Argument type mismatch: actual type is 'Function0<Boolean>'` → 반환 타입을 명시
  - 자리 화면을 지우기 전에 문자열을 먼저 지워 `Unresolved reference 'edit_placeholder'` → 자리 화면 파일을 `git rm`
  - `LocalContext.current` 로 문자열을 읽으면 Lint 오류(설정 변경에 둔감) → `stringResource` 로 틀을 미리 읽어 `format`
  - 규칙 검사 에이전트를 부르면 `Agent type 'rule-checker' not found` → 이 세션에 등록되지 않아서. `.claude/agents/rule-checker.md` 지시문을 일반 에이전트에 넘겨 같은 검사를 돌림
  - 빌드 파일을 바꾼 뒤 배포하면 `Gradle sync needed for this project.` → Gradle 동기화 후 다시 배포
- **분류 색 표가 초기화되다 멈춤**
  - 증상: `CategoryTest` 전부 `java.lang.ExceptionInInitializerError` / `Caused by: java.lang.NullPointerException at CategoryTest.kt:21`
  - 원인: `ColorFamily` enum 과 `DEFAULT_CATEGORIES` 를 한 파일에 두었다. enum 생성자가 같은 파일의 `argb()` 를 부르면 파일 클래스가 먼저 초기화되고, 그때 `DEFAULT_CATEGORIES` 가 아직 만들어지는 중인 `ColorFamily.GREEN`(null)을 읽음
  - 해결: `ColorFamily` 를 `ColorFamily.kt` 로 따로 분리
- **DB 2→3 마이그레이션에서 기록이 지워질 위험(미리 막음)**
  - 원인: `routine` 테이블을 다시 만들려고 `DROP TABLE routine` 을 하면, 외래 키(completion → routine, ON DELETE CASCADE)가 켜져 있을 때 완료 기록이 함께 지워질 수 있다. SQLite 3.18(API 26)은 `DROP COLUMN` 도 없다
  - 해결: 루틴 · 기록을 백업 테이블로 복사 → completion → routine 순서로 지우고 다시 만든 뒤 복사. 시험으로 기록이 남는 것을 확인
- **등록 화면 시험이 가끔 실패**
  - 증상: `java.lang.IllegalStateException: Dispatchers.Main is used concurrently with setting it`(tearDown 의 `resetMain`), 뒤따라 `UninitializedPropertyAccessException: lateinit property db has not been initialized`
  - 원인: ViewModel 이 모으는 Room Flow 가 백그라운드 스레드에서 Main 으로 돌아오는 순간과 시험 끝의 `resetMain` 이 겹침
  - 해결: 그 시험의 Room 을 `setQueryExecutor { it.run() }` · `setTransactionExecutor { it.run() }` 로 같은 스레드에서 돌림
- **빠른 선택을 끌었는데 엉뚱한 분류에 놓임**(휴대폰)
  - 증상: 「분리수거」를 뷰티로 끌었는데 구매로 옮겨짐
  - 원인: 손가락이 화면 아래 가장자리(12%) 안에 머물러 자동 스크롤이 계속됨
  - 해결: 가장자리 8%, 속도 24 → 10px. 옮긴 항목은 집안일로 되돌림(끝 자리로 감)

### 메모

- 휴대폰 화면을 캡처할 때 알림창이 내려와 있으면 알림 내용이 찍힌다. 캡처 전에 미리 알리고, 찍힌 내용은 기록하지 않는다
- 탭 역할: 달력(첫 화면, 예전 이름 캘린더) · 루틴(등록한 루틴 전체를 관리하는 화면). 오늘 탭은 숨김(코드 보존)
- 일하는 방식: 단계마다 시험 → 커밋 → 푸시를 묻지 않고 이어서 진행, 휴대폰이 필요할 때만 멈춤
