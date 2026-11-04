# 9단계 개인정보처리방침 · 데이터 보안 사전 조사

> 조사일: 2026-11-03 · 대상: PLAN 9단계(출시 준비 중 「개인정보처리방침 · Play 데이터 보안 양식」)
> 근거는 각 절 끝 출처(실제로 연 페이지만). 7단계 조사 [backup.md](backup.md) 4절을 기준으로 보완했다.
> 표시: **(확인 안 됨)** = 공식 문서에서 직접 확인하지 못한 내용 · **(추론)** = 문서 문장을 바탕으로 한 판단

## 결론 요약 (추천)

1. **방침은 꼭 있어야 한다.** Play 는 데이터를 전혀 다루지 않는 앱에도 방침 링크를 요구한다. 링크는 **Play Console 과 앱 안 두 곳**에 둔다. 공개 · 지역 제한 없는 웹 주소여야 하고 PDF 는 안 된다.
2. **방침 내용은 「수집 · 공유 없음」을 사실대로.** 기기 안 저장, 인터넷 권한 없음, 알림 권한(선택), 자동 백업 · 기기 이전 조건(클라우드는 종단간 암호화될 때만), JSON 내보내기, 삭제 방법, 연락처를 적는다. Play 필수 항목과 개인정보 보호법 제30조 항목을 같이 채운다.
3. **한국어 본문 + 영어 번역 한 페이지.** 한국 사용자에게는 한국어가 맞고, 영어는 Play 심사와 해외 사용자를 위한 보험이다 **(추론)**.
4. **올리는 곳: 공개 저장소를 따로 만든 GitHub Pages.** GitHub Free 에서는 Pages 저장소가 공개여야 한다. 앱 저장소(비공개)는 그대로 두고, 방침만 담은 공개 저장소를 만든다. 앱 이름이 임시이므로 주소에 앱 이름 대신 **패키지 이름**을 쓴다(예: `https://<GitHub 사용자 이름>.github.io/privacy/com.kikilog.routinecalendar/`).
5. **데이터 보안 양식: 첫 질문 「수집 또는 공유하나요?」에 「아니요」.** 근거는 ① 기기 안 처리는 신고 대상이 아님 ② 종단간 암호화된 데이터는 신고 대상이 아님 ③ 사용자가 자기 저장소에 직접 올리고 앱이 접근하지 않으면 신고 대상이 아님 ④ 병합 Manifest 에 `INTERNET` 이 없어 라이브러리(WorkManager · Glance · Room)도 밖으로 보낼 수 없음.
6. **계정 삭제 요구 사항은 해당 없음.** 앱 안에서 계정을 만들 수 없기 때문이다.
7. **앱 안 링크는 `INTERNET` 권한 없이 된다.** `ACTION_VIEW` 로 브라우저를 여는 것은 브라우저가 통신하는 것이고, `startActivity` 는 `<queries>` 도 필요 없다(공식 문서). Compose 의 `LocalUriHandler` 나 `Intent(ACTION_VIEW)` + `ActivityNotFoundException` 처리로 충분하다.
8. **그 밖의 앱 콘텐츠 답(예상):** 광고 없음 · 로그인 없음(앱 접근 제한 없음) · 대상 연령 18세 이상 · 콘텐츠 등급은 설문으로(폭력 · 사용자 간 소통 · 위치 공유 · 구매 모두 「아니요」) · 광고 ID 사용 안 함 · 정부 · 금융 · 건강 · 뉴스 앱 아님.

---

## 1. 앱 사실 (코드에서 확인, 2026-11-03)

| 항목 | 사실 | 확인한 곳 |
|---|---|---|
| 직접 선언한 권한 | `POST_NOTIFICATIONS` 하나 | `app/src/main/AndroidManifest.xml` |
| 병합된 권한 | `POST_NOTIFICATIONS` · `WAKE_LOCK` · `ACCESS_NETWORK_STATE` · `RECEIVE_BOOT_COMPLETED` · `FOREGROUND_SERVICE` · 앱 자체 서명 권한 `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`(androidx 가 넣는 것) | `app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml` |
| `INTERNET` · 정확한 알람 · `AD_ID` · 배터리 최적화 예외 | **없음** | 같은 병합 Manifest |
| 네트워크 코드 | `java.net` · OkHttp · Ktor · `ACTION_VIEW` 없음 | `*.kt` 검색 |
| 라이브러리 | AndroidX(Compose · Room · DataStore · WorkManager · Glance · Navigation3 · Lifecycle), kotlinx-datetime · serialization. 분석 · 광고 · 크래시 SDK 없음 | `gradle/libs.versions.toml` |
| 저장 | Room DB(루틴 · 완료 기록 · 분류 · 빠른 선택), DataStore(설정) — 기기 안 | PLAN 2절 |
| 자동 백업 | `allowBackup="true"`. 대상은 `databases/` 전체와 `datastore/settings.preferences_pb` 만. 클라우드는 Android 12+ `disableIfNoEncryptionCapabilities="true"`, 11 이하 `requireFlags="clientSideEncryption"`(이 속성은 API 28+. 공식 문서는 조건을 걸면 이전 판에서 백업이 안 된다고 함 — backup.md 3절. Android 8.x 기기에서는 **(확인 안 됨)**, 그래서 방침 초안에는 적지 않음). 기기 간 이전(`device-transfer`)은 같은 대상, 암호화 조건 없음 | `res/xml/data_extraction_rules.xml` · `backup_rules.xml` |
| 백업 제외 | 가져오기 전 자동 저장 파일 · WorkManager DB(`noBackupFilesDir`), Glance 위젯 내부 파일 | 같은 XML 주석 |
| JSON 백업 | 저장소 접근 프레임워크로 사용자가 고른 곳에 저장 · 읽기. 저장소 권한 없음 | backup.md 1절 |
| 앱 안 안내 | `settings_privacy_body`: 「모든 기록은 이 휴대폰에 저장돼요. 앱은 인터넷에 연결하지 않고 아무것도 수집 · 공유하지 않아요. 휴대폰의 Google 백업을 켜 두면 내 Google 계정에 암호화되어 저장될 수 있고, 개발자는 그 내용을 볼 수 없어요.」 | `res/values/strings.xml` |
| 앱 이름 | `app_name` = 「Routine Calendar」(임시) | 같은 파일 |

- `ACCESS_NETWORK_STATE` 는 연결 상태를 **읽는** 일반 권한이다. 통신은 `INTERNET` 없이 할 수 없다 **(추론)**. 이 앱은 WorkManager 에 네트워크 제약을 걸지 않는다(notify.md 1절).

## 2. Play 정책: 방침이 필요한 조건과 들어갈 항목

- 필요 조건(원문): "All apps must post a privacy policy link in the designated field within Play Console" · "Apps that do not access any personal and sensitive user data must still submit a privacy policy."
- 데이터 보안 도움말도 같다: "Even developers with apps that do not collect any user data must complete this form" — 그리고 방침 링크도 내야 하며, 양식과 방침에 「수집 · 공유 없음」을 적을 수 있다.
- 위치: Play Console 지정 칸 **과** "a privacy policy link or text within the app itself". 앱 콘텐츠 페이지도 "on your store listing and within your app" 이라고 쓴다.
- 페이지 조건: "active, publicly accessible and non-geofenced URL" · "(no PDFs) and is non-editable".
- 들어갈 항목(사용자 데이터 정책):
  - "Developer information and a privacy point of contact or a mechanism to submit inquiries."
  - 앱이 접근 · 수집 · 사용 · 공유하는 데이터 종류와 받는 곳
  - "Secure data handling procedures for personal and sensitive user data."
  - "The developer's data retention and deletion policy."
  - "Clear labeling as a privacy policy" — 제목에 「개인정보처리방침 / Privacy Policy」를 쓴다.
- 이 앱에 맞추면: 받는 곳 = 없음, 보관 = 기기 안(사용자가 지울 때까지), 삭제 = 앱 안 삭제 · 앱 데이터 삭제 · 앱 삭제, 보안 = 기기 밖으로 보내지 않음 + 클라우드 백업은 종단간 암호화.

출처: [User Data — Play Console Help](https://support.google.com/googleplay/android-developer/answer/10144311?hl=en) · [Provide information for Google Play's Data safety section](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en) · [Prepare your app for review](https://support.google.com/googleplay/android-developer/answer/9859455?hl=en)

## 3. 한국 개인정보 보호법 관점

- 방침 의무는 개인정보 보호법 제30조, 대상은 「개인정보처리자」다. 정의(제2조 제5호): "업무를 목적으로 개인정보파일을 운용하기 위하여 스스로 또는 다른 사람을 통하여 개인정보를 처리하는 공공기관, 법인, 단체 및 개인 등".
- 이 앱은 개발자가 개인정보를 받지도 저장하지도 않으므로 엄밀히는 처리자가 아닐 수 있다 **(추론)**. 그래도 Play 가 방침을 요구하므로, 제30조 항목 틀에 맞춰 「해당 없음」을 분명히 적는 편이 안전하다.
- 제30조 제1항 항목과 이 앱의 답:

| 제30조 항목 | 이 앱 |
|---|---|
| 처리 목적 | 개발자는 처리하지 않음. 앱은 기기 안에서 루틴 표시 · 알림에만 씀 |
| 처리 및 보유 기간 | 기기 안에 사용자가 지울 때까지 |
| 제3자 제공(해당 시) | 없음 |
| 파기 절차 · 방법 | 앱 안 삭제 · 앱 데이터 삭제 · 앱 삭제. 백업은 Google 계정에서 관리 |
| 민감정보 공개 가능성(해당 시) | 해당 없음 |
| 처리 위탁(해당 시) | 없음 |
| 가명정보(해당 시) | 해당 없음 |
| 정보주체 권리 · 행사 방법 | 모든 정보가 기기에 있어 사용자가 직접 열람 · 수정 · 삭제 |
| 보호책임자 · 연락처 | 개발자 kikilog, `<연락 이메일>` |
| 자동 수집 장치(쿠키 등) 설치 · 거부 | 없음 |

- 개인정보 보호위원회 「개인정보 처리방침 작성지침」은 2025-04-21 개정본이 나왔다(보도 기사 확인). 공개 위치를 첫 화면뿐 아니라 「설정」 메뉴 등으로 넓혔다 — 설정 화면 링크로 충분하다는 뜻으로 본다 **(추론)**. 원문 PDF 는 열어 보지 못했다 **(확인 안 됨)**. 「수집하지 않는 앱」 전용 서식이 있는지도 **(확인 안 됨)**.
- 정보통신망법 제22조의2: 정보통신서비스 제공자가 휴대폰 안 정보 · 기능에 접근하는 권한이 필요하면 알리고 동의를 받는다. 알림 권한이 시행령 대상 목록에 드는지는 **(확인 안 됨)**. 이 앱은 시스템 권한 창으로 묻고 거절해도 동작하므로, 방침에 「알림 권한(선택)」을 적어 두면 충분하다 **(추론)**.
- 언어: 법 · Play 어느 쪽도 영어를 요구하는 문장은 찾지 못했다 **(확인 안 됨)**. 한국어를 기준으로 하고 영어 번역을 같은 페이지 아래에 둔다 **(추론)**.

출처: [개인정보 보호법 제30조 (케이스노트)](https://casenote.kr/법령/개인정보_보호법/제30조) · [개인정보 보호법 제2조 (케이스노트)](https://casenote.kr/법령/개인정보_보호법/제2조) · [정보통신망법 제22조의2 (케이스노트)](https://casenote.kr/법령/정보통신망_이용촉진_및_정보보호_등에_관한_법률/제22조의2) · [개인정보위, 개인정보 처리방침 작성지침 개정본 공개 (뉴스서울)](https://newsseoul.co.kr/news/view/1065585619471455)
- 참고: 국가법령정보센터(law.go.kr) 본문은 열리지 않아 조문은 케이스노트에서 읽었다. 제출 전에 law.go.kr 에서 한 번 더 확인한다.

## 4. 방침을 올릴 곳 — GitHub Pages

- 요금(원문): "If the account that owns the repository uses GitHub Free or GitHub Free for organizations, the repository must be public." → **앱 저장소(비공개)로는 무료 Pages 불가.** 유료 플랜이면 비공개 저장소도 되지만, 그래도 사이트 자체는 "publicly available on the internet, even if the repository for the site is private".
- 주소 형태:
  - 사용자 · 조직 사이트: 저장소 이름 `<owner>.github.io` → `https://<owner>.github.io` (계정당 1개)
  - 프로젝트 사이트: 아무 저장소 → `https://<owner>.github.io/<저장소 이름>` (저장소당 1개)
- 만드는 법: 새 저장소(공개) → `index.md` 또는 `index.html` 을 맨 위에 → Settings > Pages 에서 게시 원본(브랜치 · 폴더) 지정 → 최대 10분 뒤 「Visit site」.
- 방문자 IP 를 GitHub 가 보안 목적으로 기록한다(원문 "the visitor's IP address is logged and stored for security purposes"). 이는 웹페이지 호스팅의 일이지 앱의 수집이 아니다 **(추론)**.

**추천 구성 (안)**

| 안 | 주소 | 장점 | 단점 |
|---|---|---|---|
| **A. 공개 저장소 `privacy`** (추천) | `https://<사용자>.github.io/privacy/com.kikilog.routinecalendar/` | 앱이 늘어도 폴더만 추가. 앱 이름이 바뀌어도 주소 그대로 | 주소가 조금 길다 |
| B. `<사용자>.github.io` 저장소 | `https://<사용자>.github.io/routine-calendar/privacy` | 짧다. 포트폴리오 사이트와 겸용 | 계정 대표 사이트를 방침이 차지 |
| C. 무료 조직 `kikilog` 만들기 | `https://kikilog.github.io/privacy/...` | 브랜드 이름과 맞음 | 조직 하나 더 관리. 이름 `kikilog` 가 비어 있는지 **(확인 안 됨)** |

- 앱 안 링크는 앱 업데이트 없이 바꿀 수 없으므로, **주소에 임시 앱 이름을 넣지 않는다**(패키지 이름은 출시 후 바뀌지 않음). Play Console 의 방침 주소는 나중에 바꿀 수 있다 **(추론)**.
- 대안(짧게): Google Sites(구글 계정만 있으면 무료, 주소 `sites.google.com/view/...`) · Notion 공개 페이지. 둘 다 「공개 · 편집 불가」 조건은 맞출 수 있으나 Notion 은 페이지 자체 추적 · 로그인 유도가 섞이고, 포트폴리오 관점에서는 Git 으로 이력이 남는 GitHub Pages 가 낫다 **(추론, 두 서비스 문서는 열어 보지 않음)**.

출처: [Creating a GitHub Pages site](https://docs.github.com/en/pages/getting-started-with-github-pages/creating-a-github-pages-site) · [About GitHub Pages](https://docs.github.com/en/pages/getting-started-with-github-pages/about-github-pages) · [What is GitHub Pages?](https://docs.github.com/en/pages/getting-started-with-github-pages/what-is-github-pages)

## 5. 데이터 보안(Data safety) 양식

### 질문 순서 (도움말 기준)

1. 데이터 수집 및 보안: 「앱이 필수 사용자 데이터 유형을 수집 또는 공유하나요?」 → 예라면 「전송 중 암호화되나요?」 · 「사용자가 데이터 삭제를 요청할 방법이 있나요?」
2. 데이터 유형: 수집 · 공유하는 유형 고르기
3. 데이터 사용 및 처리: 유형마다 수집/공유 · 일시 처리 · 필수/선택 · 목적
4. 선택 배지: Families 정책 약속 · 독립 보안 검토 · UPI(인도)

- 「아니요」를 고르면 2 · 3 과 암호화 · 삭제 질문이 건너뛰어지는지는 도움말에 적혀 있지 않다(원문은 "If not, select No" 뒤 Next) **(확인 안 됨)** — 실제 화면에서 확인한다.
- 계정 관련 질문(계정 생성 방법 · 계정 삭제 URL)의 정확한 문구와 「계정 만들기 없음」 선택지는 공식 도움말에서 찾지 못했다 **(확인 안 됨)**. 계정 삭제 정책 자체는 "If your app allows users to create an account from within your app" 에만 걸린다.

### 「수집 아님」 근거 (backup.md 4절 + 이번에 다시 확인)

- 수집의 정의: "'Collect' means transmitting data from your app off a user's device."
- 기기 안 처리 예외: "only processed locally on the user's device and not sent off device does not need to be disclosed." → 루틴 · 기록 · 설정 · 위젯 · 알림 전부.
- 종단간 암호화 예외: 보낸 사람과 받는 사람 외에는 읽을 수 없고 개발자 등 중간자가 읽을 수 없으면 신고하지 않는다 → 클라우드 자동 백업은 암호화 가능할 때만 일어나도록 XML 로 막아 두었다.
- 직접 업로드 FAQ: 사용자가 자기 Google Drive 등에 직접 올리고, 그 서비스 약관을 따르며, 앱이 그 데이터를 수집 · 접근하지 않으면 신고하지 않는다 → JSON 내보내기를 Drive 에 저장하는 경우.
- 기기 간 이전(D2D, `device-transfer`)은 암호화 조건이 없지만 사용자의 두 기기 사이 직접 이전이고 개발자에게 가지 않는다 **(추론)**. 「Android 시스템 백업 · 기기 이전」을 콕 집은 공식 문장은 이번에도 **찾지 못했다**.
- SDK · 라이브러리: 도움말은 앱 안 SDK 가 기기 밖으로 보내는 데이터도 신고하라고 한다. 이 앱의 라이브러리는 모두 기기 안에서만 동작하고, 병합 Manifest 에 `INTERNET` 이 없으므로 보낼 수 없다 **(추론)**.
- 「System services and private apps do not need to complete the Data safety form」 문장은 **앱 자체가 시스템 서비스인 경우**다. WorkManager 같은 라이브러리를 쓴다고 이 예외가 생기지 않는다. 이 앱은 양식을 그대로 작성한다.
- 양식 FAQ: "You do not need to declare collection or sharing unless data is actually collected and/or shared." 공유는 수집한 데이터를 제3자에게 넘기는 것.

출처: [Provide information for Google Play's Data safety section](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en) · [Understanding Google Play's app account deletion requirements](https://support.google.com/googleplay/android-developer/answer/13327111?hl=en) · [Back up user data with Auto Backup](https://developer.android.com/identity/data/autobackup)

## 6. 계정 삭제 요구 사항

- 정책 원문: "apps that allow account creation must provide a clear option for account deletion." 적용 조건은 "if a user can complete creating an app account directly in the app".
- 이 앱은 로그인 · 계정이 없다 → **해당 없음**. 앱 안 · 웹 삭제 경로를 만들 필요 없다.
- 다만 같은 도움말에 "All developers must complete new Data deletion questions in the Data safety form" 이 있다. 수집 「아니요」 뒤에 이 질문이 나오는지는 **(확인 안 됨)**. 나온다면 「계정 생성 없음」에 해당하는 선택지를 고른다.

출처: [Understanding Google Play's app account deletion requirements](https://support.google.com/googleplay/android-developer/answer/13327111?hl=en) · [User Data](https://support.google.com/googleplay/android-developer/answer/10144311?hl=en)

## 7. 그 밖의 앱 콘텐츠 항목

| 항목 | 묻는 것 | 이 앱 예상 답 | 비고 |
|---|---|---|---|
| 개인정보처리방침 | 방침 주소 | 4절 주소 | 스토어 · 앱 안 둘 다 |
| 광고 | 광고 포함 여부(서드파티 SDK 포함) | 아니요 | |
| 앱 접근 권한(Sign-in details) | 로그인 등으로 막힌 기능이 있으면 심사용 접속 정보 | 제한 없음(모든 기능 바로 사용) | |
| 대상 연령 및 콘텐츠 | 대상 연령대(5세 이하 · 6-8 · 9-12 · 13-15 · 16-17 · 18세 이상) | **18세 이상** 추천 | 13세 미만을 고르면 Families 정책. 13-17 도 지역에 따라 아동으로 볼 수 있음. 아이콘 · 스크린샷이 아이용처럼 보이면(고양이 그림 등) 반려될 수 있다고 도움말에 있음 — 아래 열린 질문 |
| 콘텐츠 등급 | IARC 설문 | 폭력 · 성 · 언어 · 약물 · 도박 없음, 사용자 간 소통 · 위치 공유 · 디지털 구매 없음 → 전체 이용가 예상 **(확인 안 됨, 설문 문항은 별도 도움말)** | 등급 없는 앱은 스토어에서 내려감 |
| 데이터 보안 | 5절 | 수집 · 공유 없음 | |
| 권한 선언 | 고위험 · 민감 권한 사용 시 | 해당 없음(일반 권한 + 알림만) **(추론)** | |
| 광고 ID | 광고 ID 사용 여부 | 아니요(`AD_ID` 권한 없음) | 선언 화면 문구 **(확인 안 됨)** |
| 정부 앱 · 금융 기능 · 건강 앱 · 뉴스 앱 | 해당 여부 | 모두 아니요 | 각 선언이 지금 필수인지 **(확인 안 됨)**. 뷰티 루틴은 건강 기능이 아님 **(추론)** |

출처: [Prepare your app for review](https://support.google.com/googleplay/android-developer/answer/9859455?hl=en) · [Target audience and app content](https://support.google.com/googleplay/android-developer/answer/9867159?hl=en) · [Content ratings](https://support.google.com/googleplay/android-developer/answer/9898843?hl=en) · [Advertising ID](https://support.google.com/googleplay/android-developer/answer/6048248?hl=en)

## 8. 앱 안 방침 링크 (INTERNET 권한 없이)

- 공식 문서: "Because the startActivity() method doesn't require package visibility to start another application's activity, you don't need to add a <queries> element" — URL 은 브라우저 앱이 열고, 통신도 브라우저가 한다. 앱에 `INTERNET` 이 필요 없다 **(추론, 권한은 통신하는 쪽 앱에 필요)**.
- 브라우저가 없으면 `ActivityNotFoundException` → 잡아서 주소를 보여 주거나 복사하게 한다.
- 브라우저가 있는지 **미리** 확인하려면(`queryIntentActivities`) `<queries>` 가 필요하다 → 하지 않는다(Manifest 를 늘리지 않음).
- Custom Tabs 는 `androidx.browser` 의존성이 늘어난다 → 쓰지 않는다(간단함 우선).
- 구현(안): Compose 의 `LocalUriHandler.current.openUri(url)` 을 `try` 로 감싼다. 주소는 `strings.xml` 에 `privacy_policy_url`(번역하지 않음 표시). 설정 화면 「개인정보」 칸 아래 「개인정보처리방침 보기」 줄.
- 오프라인이어도 방침 요지는 이미 `settings_privacy_body` 로 앱 안에 있다 — 정책의 "link or text within the app" 둘 다 충족 **(추론)**.

출처: [Package visibility — use cases (Open URLs)](https://developer.android.com/training/package-visibility/use-cases)

---

## 개인정보처리방침 초안 (한국어)

> 그대로 붙여 넣는 초안. `<앱 이름>` · `<시행일>` · `<연락 이메일>` 은 자리표시. 영어 번역은 확정 뒤 아래에 덧붙인다.

```markdown
# <앱 이름> 개인정보처리방침

시행일: <시행일>

kikilog(이하 「개발자」)는 Android 앱 <앱 이름>(패키지 com.kikilog.routinecalendar, 이하 「앱」)을 만들었습니다.
이 방침은 앱이 어떤 정보를 어떻게 다루는지 설명합니다.

## 1. 요약

- 앱은 개인정보를 수집하지 않고, 누구와도 공유하지 않습니다.
- 앱이 다루는 정보는 모두 사용자의 휴대폰 안에만 저장됩니다.
- 앱에는 인터넷 연결 권한이 없어 개발자나 다른 회사의 서버로 정보를 보낼 수 없습니다.
- 광고, 분석, 오류 보고 도구를 쓰지 않습니다. 회원가입과 로그인도 없습니다.

## 2. 앱이 휴대폰 안에서 다루는 정보

사용자가 직접 입력한 다음 정보를 휴대폰 안에 저장해 화면, 홈 화면 위젯, 알림에 보여 줍니다.

- 루틴: 이름, 분류, 주기, 반복 여부, 시작일, 알림 여부, 보관 여부, 등록 시각
- 완료 기록: 루틴을 한 날짜, 체크한 시각
- 분류와 빠른 선택 목록
- 설정: 주 시작 요일, 첫 화면, 알림 켜기와 시각

개발자는 이 정보를 받거나 볼 수 없습니다.

## 3. 권한

- 알림(선택): 하루 한 번 「오늘 할 루틴」 요약 알림을 보내는 데 씁니다. Android 13 이상에서는 처음 알림을 켤 때 휴대폰이 허용 여부를 묻습니다. 허용하지 않아도 알림을 뺀 모든 기능을 쓸 수 있고, 휴대폰 설정에서 언제든 바꿀 수 있습니다.
- 그 밖에 설치할 때 자동으로 허용되는 권한(절전 중 작업 유지, 재부팅 후 알림 다시 예약, 네트워크 연결 상태 확인, 백그라운드 작업)은 Android 의 작업 예약 기능(WorkManager)이 알림을 예약하는 데 씁니다. 이 권한으로 정보를 밖으로 보내지 않습니다.
- 앱은 연락처, 사진, 위치, 카메라, 마이크, 저장소 권한을 요청하지 않습니다.

## 4. 백업과 기기 이전

- 파일로 내보내기 · 가져오기: 사용자가 설정에서 직접 실행할 때만, 사용자가 고른 위치에 백업 파일(JSON)을 저장하거나 그 파일을 읽습니다. 저장 위치가 클라우드 서비스라면 그 파일은 해당 서비스의 약관과 개인정보처리방침을 따릅니다. 개발자는 이 파일에 접근하지 않습니다.
- Android 자동 백업: 휴대폰의 Google 백업이 켜져 있으면, Android 가 앱의 루틴 데이터와 설정을 사용자의 Google 계정에 백업할 수 있습니다. 앱은 이 백업이 종단간 암호화될 수 있을 때만(휴대폰에 화면 잠금이 설정된 경우 등) 클라우드에 올라가도록 설정되어 있습니다. 암호화된 백업은 개발자가 볼 수 없습니다.
- 기기 간 이전: 새 휴대폰으로 옮길 때 Android 의 기기 간 이전 기능이 같은 정보를 새 휴대폰으로 직접 옮길 수 있습니다. 이 정보는 개발자에게 전달되지 않습니다.

## 5. 보관 기간과 삭제 방법

정보는 사용자가 지울 때까지 휴대폰 안에 남습니다. 다음 방법으로 언제든 지울 수 있습니다.

- 앱 안에서 루틴, 완료 기록, 분류를 지웁니다.
- 휴대폰 설정 > 애플리케이션 > <앱 이름> > 저장공간 > 데이터 삭제를 누릅니다.
- 앱을 삭제하면 휴대폰 안의 앱 정보가 모두 지워집니다.
- Google 계정에 남은 휴대폰 백업은 Google 계정의 백업 관리 화면에서 지울 수 있습니다.
- 내보낸 백업 파일은 사용자가 저장한 위치에서 직접 지웁니다.

개발자는 정보를 갖고 있지 않으므로 따로 삭제를 요청할 필요가 없습니다.

## 6. 제3자 제공, 처리 위탁, 자동 수집 장치

- 제3자에게 제공하는 정보가 없습니다.
- 처리를 맡기는 업체가 없습니다.
- 쿠키나 광고 식별자 같은 자동 수집 장치를 쓰지 않습니다.

## 7. 사용자의 권리

모든 정보가 사용자의 휴대폰에 있으므로 사용자가 앱 안에서 직접 보고, 고치고, 지울 수 있습니다.

## 8. 아동

앱은 누구의 개인정보도 수집하지 않으며, 만 14세 미만 아동의 정보도 수집하지 않습니다.

## 9. 문의 (개인정보 보호책임자)

- 개발자: kikilog
- 이메일: <연락 이메일>

## 10. 방침의 변경

이 방침을 바꾸면 이 페이지에 새 시행일과 함께 알립니다. 앱이 정보를 수집하는 방식이 바뀌면 앱 업데이트 전에 먼저 이 방침을 고칩니다.
```

- 초안과 앱 사실이 맞는지: 인터넷 권한 없음(병합 Manifest) · 권한 목록(병합 Manifest) · 백업 대상과 암호화 조건 · Android 8.x 미지원(`backup_rules.xml` 주석) · 저장 항목(PLAN 2절). 앱 안 문구 `settings_privacy_body` 와 모순 없음.
- 「Google 계정 백업 관리 화면」: Drive 도움말은 Android 백업을 `myaccount.google.com/dashboard/android` 대시보드에서 관리한다고 안내한다. 앱 하나만 골라 지우는 방법은 그 페이지에 없었다 **(확인 안 됨)** — 그래서 초안에 구체적인 메뉴 이름을 쓰지 않았다.

출처: [Google Drive 도움말 — 백업 관리](https://support.google.com/drive/answer/6305834?hl=ko)

## 데이터 보안 양식 답 초안

| 질문 | 답 | 근거 |
|---|---|---|
| 앱이 필수 사용자 데이터 유형을 수집 또는 공유하나요? | **아니요** | 기기 안 처리 예외, `INTERNET` 없음, 분석 · 광고 · 크래시 SDK 없음 |
| 전송 중 암호화되나요? | (나오면) 해당 없음 — 보내는 데이터 없음 | 「아니요」면 묻지 않을 것으로 봄 **(확인 안 됨)** |
| 데이터 삭제 요청 방법이 있나요? | (나오면) 앱 안 삭제 · 앱 데이터 삭제로 사용자가 직접 지움 | 개발자가 가진 데이터 없음 |
| 계정 생성 방법 / 계정 삭제 URL | (나오면) 계정 생성 없음 | 로그인 · 계정 없음, 계정 삭제 정책 적용 조건 밖 |
| 데이터 유형 · 사용 및 처리 | 없음(건너뜀) | 수집 · 공유 없음 |
| 자동 백업(Google 계정) | 수집 아님 | 종단간 암호화 예외 + 앱 · 개발자 접근 없음. 암호화 불가 기기에서는 클라우드 백업을 끔(XML) |
| JSON 내보내기 | 수집 아님 | 기기 안 동작, 사용자가 고른 저장소로 직접 저장(직접 업로드 FAQ) |
| 기기 간 이전 | 수집 아님 **(추론)** | 사용자 기기 사이 이전, 개발자 미경유 |
| WorkManager · Glance · Room · DataStore | 신고 없음 | 기기 안 라이브러리, 통신 권한 없음 **(추론)** |
| Families 정책 약속 배지 | 고르지 않음 | 대상 연령 18세 이상 |
| 독립 보안 검토 배지 | 고르지 않음 | 선택 항목, 비용 대비 필요 없음 |
| 방침 주소 | 4절 GitHub Pages 주소 | 공개 · 지역 제한 없음 · HTML |

스토어에 보일 결과(예상): 「데이터 수집 안 함」 · 「제3자와 데이터 공유 안 함」 **(추론)**.

## 구현 순서 체크리스트

**사용자가 손으로 할 단계**

- [ ] ① 방침 공개 주소 방식 고르기(4절 A · B · C) — GitHub 사용자 이름 확인
- [ ] ② 공개 저장소 만들기, 초안을 `index.md` 로 올리기(시행일 · 앱 이름 · 연락 이메일 채움), Settings > Pages 켜기
- [ ] ③ 휴대폰 브라우저로 주소 열어 보기(로그인 없이 보이는지, 모바일에서 읽히는지)
- [ ] ④ 연락용 이메일 정하기(개인 주소 대신 별도 주소 추천 — 열린 질문)
- [ ] ⑤ Play Console 앱 콘텐츠: 방침 주소 → 광고 아니요 → 앱 접근 제한 없음 → 대상 연령 → 콘텐츠 등급 설문 → 데이터 보안(위 표) → 광고 ID 아니요 → 나머지 선언
- [ ] ⑥ 데이터 보안 미리보기에서 「수집 안 함 · 공유 안 함」 확인, 실제 질문 문구를 이 문서에 덧붙임(확인 안 됨 항목 정리)

**코드로 할 단계**

- [ ] ⑦ `strings.xml` 에 `privacy_policy_url`(`translatable="false"`) · 「개인정보처리방침 보기」 문구
- [ ] ⑧ 설정 화면 「개인정보」 칸에 링크 줄: `LocalUriHandler.openUri` + `ActivityNotFoundException` 처리(안내 문구). Manifest 변경 없음 · `<queries>` 없음 · 새 라이브러리 없음
- [ ] ⑨ 출시 빌드 병합 Manifest(`processReleaseManifest`)에서 `INTERNET` · `AD_ID` · 정확한 알람 권한이 없는지 다시 확인
- [ ] ⑩ `./gradlew test assembleDebug` → 휴대폰에서 링크 열림 확인 → 커밋 · DEVLOG
- [ ] ⑪ PLAN 9단계 체크 항목 갱신(방침 · 데이터 보안 완료 표시)

## 정한 것 (2026-11-04 사용자 답)

- 앱 이름: 「때됐냥」 (방침 제목의 `<앱 이름>` 자리)
- 연락 이메일: hwangsonghae99@gmail.com (사용자가 개인 주소를 골랑. Play 등록정보 · 방침에 공개됨)
- 방침을 올릴 저장소: `routine-calendar` (앱 저장소). 지금 비공개라 무료 GitHub Pages 를 쓰려면 공개로 바꿔야 한다 — 공개 전에 커밋 기록 점검 후 사용자가 GitHub 설정에서 직접 바꾼다(확인 대기)

## 열린 질문

1. 방침 주소를 어디에 둘지 — A 공개 저장소 `privacy`(추천) · B `<사용자>.github.io` · C 조직 `kikilog` 새로 만들기.
2. 연락 이메일을 무엇으로 할지 — 방침 · 스토어에 공개되므로 개인 주소 대신 앱 전용 주소를 만들지.
3. 대상 연령을 18세 이상으로 할지, 13세 이상까지 넓힐지(넓히면 지역별 아동 규정 검토가 늘어남).
4. 고양이 그림 아이콘이 「아이용으로 보이는 마케팅 요소」로 읽힐 위험을 받아들일지(대상 연령 심사).
5. 영어 번역을 같은 페이지 아래에 둘지, `/en/` 페이지로 나눌지.
6. 방침에 기기 간 이전(D2D)을 지금처럼 적을지 — `device-transfer` 에는 암호화 조건이 없다.
7. 앱 이름이 정해지면 방침 제목만 바꾸면 되는지(주소는 패키지 이름이라 그대로) — 8단계 이름 결정과 순서 맞추기.
