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

- PLAN 3단계(등록 · 루틴 목록 · 오늘 화면) 준비

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
