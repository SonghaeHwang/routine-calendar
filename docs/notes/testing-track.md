# 10단계 비공개 테스트 · 출시 사전 조사

> 조사일: 2026-11-04 · 대상: PLAN 10단계(「비공개 테스트 → 출시」)
> 근거는 각 절 끝 출처(실제로 연 페이지만). 9단계 조사 [privacy.md](privacy.md) 를 이어받는다.
> 표시: **(확인 안 됨)** = 공식 문서에서 직접 확인하지 못한 내용 · **(추론)** = 문서 문장을 바탕으로 한 판단

## 결론 요약 (추천)

1. **요건은 지금도 「12명 · 14일 연속」이다.** 2026-11-04 에 연 공식 도움말(영어 · 한국어)이 모두 12명이다. 검색 결과 요약에 「20명」이 보이지만 옛 판으로 보인다 **(추론)**. 신청하는 순간에도 12명 이상이 참여 중이어야 한다.
2. **내부 테스트는 요건에 들지 않는다.** 요건은 **비공개 테스트(closed)** 트랙만 센다. 내부 테스트는 먼저 내 폰으로 설치 · 서명을 확인하는 데만 쓴다. 내부 테스트에 참여한 사람은 빠질 때까지 비공개 테스트에 참여할 수 없다 — 지인을 내부 테스트에 넣지 않는다.
3. **테스터는 15~20명을 모은다.** 14일이 되기 전에 빠진 사람은 세지 않는다. 다시 들어오면 그 사람의 14일이 새로 시작된다. 여유 인원이 있으면 한두 명이 빠져도 일정이 밀리지 않는다 **(추론)**. 아이폰 사용자는 설치를 못 하므로 세지 않는다.
4. **목록은 이메일 목록으로 관리한다.** 지인 15~20명이면 Play Console 이메일 목록이 가장 단순하다. Google 그룹은 테스터가 그룹 가입을 한 번 더 해야 한다. 서로 테스트해 주는 커뮤니티는 쓰지 않거나, 써도 일부만 채우는 보조로 둔다(4절).
5. **의견은 앱 밖에서 받는다.** Play 비공개 테스트의 「비공개 의견」 + 참여 페이지에 보이는 **의견 이메일**(필수 입력) + 단톡방. 구글 폼은 선택. 앱 안 「의견 보내기」는 `ACTION_SENDTO` + `mailto:` 로 메일 앱을 여는 방식이면 `INTERNET` 권한 없이 된다(규칙 위반 없음).
6. **비공개 테스트 전에 9단계를 끝낸다.** 데이터 보안 양식은 비공개 · 공개 · 프로덕션 트랙 모두 필수다(내부 테스트만 면제). 방침 링크 · 콘텐츠 등급 · 대상 연령도 함께 채운다. 업로드는 AAB, 올릴 때마다 `versionCode` 를 1씩 올린다.
7. **14일 동안 고친 판을 1~2번 올린다.** 공식 권장은 「버그를 고치면서 비공개 테스트를 계속하라」다. 업데이트가 14일 집계를 끊는다는 공식 문장은 없다. 받은 의견 → 고친 점이 프로덕션 신청서의 핵심 답이 된다.
8. **신청은 15일차 이후, 답은 사실과 숫자로.** 검토는 보통 7일 이내. 「더 테스트가 필요」 답을 받으면 비공개 테스트를 이어 가며 참여를 늘리고 다시 신청한다.

---

## 1. 개인 계정 테스트 요건 (2026-11-04 기준)

- 대상: 2023-11-13 이후 만든 개인 개발자 계정. 조직 계정은 이 요건이 없다.
- 원문: "a closed test with a minimum of 12 testers who have been opted in continuously for at least 14 days."
- 신청 시점 조건: "At least 12 testers must be opted in to your closed test when you apply for production access."
- 「연속 14일 참여」 FAQ 원문:
  - "Testers who opt in, test for fewer than 14 days, and then opt out do not count toward the requirement."
  - 빠졌다가 다시 들어오면 "the 14 days must be consecutive to count".
- 「참여(opt-in)」의 뜻: 참여 페이지에서 **테스터가 되겠다고 동의한 상태**를 말한다. 설치 · 사용을 요건 문장으로 요구하지는 않는다. 다만 신청서가 「테스터가 모든 기능을 써 봤는지」 · 「실제 사용과 비슷했는지」를 묻고, 공식 도움말이 더 테스트가 필요한 이유로 "insufficient tester engagement" 를 든다. 그래서 **설치하고 실제로 써야 통과한다**고 보고 계획한다 **(추론)**.
- 한 명이 빠지면 「전체 14일이 처음부터 다시」인가: 공식 문장은 그 **한 사람**이 세지지 않는다는 것이다. 남은 사람이 12명 이상이고 각자 14일을 채웠다면 영향이 없다 **(추론)**. 「한 명만 빠져도 초기화」라는 국내 글은 12명 딱 맞게 모았을 때의 이야기로 읽힌다.
- 바뀐 점: 공식 페이지에 이력 표시가 없다 **(확인 안 됨)**. 처음 20명이었다가 12명으로 줄었다는 이야기는 블로그 · 포럼에만 있다(공식 발표 못 찾음). 2025~2026 에 다시 바뀌었다는 근거는 찾지 못했다. **신청 전 Play Console 화면의 문구를 다시 본다.**
- 그 밖의 신청 전 확인(공식): 정책 준수는 개발자 책임이고, 검토는 문제 해결 단계가 아니다. 앱 내용 · 대상 · 콘텐츠 등급 · 기능 안정성을 미리 점검한다.

### 내부 테스트 vs 비공개 테스트

| | 내부 테스트 (internal) | 비공개 테스트 (closed) |
|---|---|---|
| 요건 집계 | **안 셈** | **셈** |
| 인원 | 최대 100명, 이메일로 추가 | 이메일 목록(목록당 2,000명) · Google 그룹 · 조직 |
| 시작 조건 | 앱 설정을 끝내기 전에도 가능 | 앱 설정 완료 필요 |
| 업데이트 반영 | 몇 분 | 첫 게시 후 링크가 몇 시간 걸릴 수 있음 |
| 정책 · 보안 검토 | 표준 검토를 받지 않을 수 있음 | 문서에 명시 없음 **(확인 안 됨)** — 검토를 받는다고 보고 계획 **(추론)** |
| 데이터 보안 양식 | 면제 | 필수 |
| 주의 | 여기 참여한 사람은 빠질 때까지 비공개 · 공개 테스트에 참여 불가 | 검색으로 안 나옴, 링크를 보내야 함 |

출처: [App testing requirements for new personal developer accounts](https://support.google.com/googleplay/android-developer/answer/14151465?hl=en) · [같은 문서 한국어판](https://support.google.com/googleplay/android-developer/answer/14151465?hl=ko) · [Set up an open, closed, or internal test](https://support.google.com/googleplay/android-developer/answer/9845334?hl=en) · 참고(비공식): [Choicely — Google Play 12 tester rule](https://www.choicely.com/blog/google-play-12-tester-rule) · [Adalo 포럼 — 12 free testers](https://forum.adalo.com/t/google-play-policy-update-how-anyone-can-get-12-free-testers/53705)

## 2. 테스터 모으기 · 관리

### 이메일 목록 vs Google 그룹

| | 이메일 목록 (추천) | Google 그룹 |
|---|---|---|
| 만드는 곳 | Play Console 「테스터」 탭 | Google 그룹스(`이름@googlegroups.com`) |
| 테스터가 할 일 | 링크 → 참여 | **그룹 가입** → 링크 → 참여 |
| 사람 추가 · 삭제 | Console 에서 이메일 입력 | 그룹에서 관리, Console 은 그대로 |
| 맞는 경우 | 지인 20명 안팎 | 사람이 자주 바뀌거나 여러 앱에 같은 테스터를 쓸 때 |

- 테스터는 Google 계정이 필요하다. 목록에 넣는 이메일은 **테스터가 Play 스토어에 로그인한 계정**이어야 한다 **(추론)** — 회사 메일 · 네이버 메일을 받으면 안 된다.
- 그룹 가입 설정(누구나 가입 · 승인제)은 이번에 확인하지 않았다 **(확인 안 됨)**.

### 참여 링크 흐름 (테스터가 할 일)

1. 개발자가 보낸 **참여 링크(opt-in URL)** 를 Android 폰(또는 PC 브라우저)에서 연다. 목록에 넣은 Google 계정으로 로그인한다.
2. 「테스터 책임」 안내를 읽고 **참여(테스터 되기)** 를 누른다.
3. 같은 페이지의 Play 스토어 링크로 앱을 **설치**한다(검색으로는 안 나온다).
4. 14일 동안 **참여를 취소하지 않고**, 가능하면 앱을 지우지 않는다.
5. 의견은 Play 스토어(앱 정보 화면의 비공개 의견) · 의견 이메일 · 단톡방으로 보낸다.

- 참여 링크는 앱 상태가 「게시됨」일 때만 보인다(초안 · 게시 대기 중에는 없음). 첫 게시 뒤 몇 시간 걸릴 수 있다.
- 폰에 Google 계정이 여러 개면 Play 스토어 계정이 목록 계정과 다를 수 있다 — 안내문에 적는다 **(추론)**.

### 아이폰 사용자

- Android 앱이라 설치할 수 없다. 참여 동의는 웹에서 될 수 있지만, 실제로 쓰지 못하면 「참여도」가 낮게 잡힐 수 있다 **(추론)**. **세지 않는다.**
- 가족 공용 · 예전 Android 폰을 대신 쓰게 하는 방법은 가능하나 유지가 어렵다 **(추론)**.

### 지인 12명을 14일 유지하는 방법 (공식 권장 + 추론)

- 공식 권장: 14일 동안 참여를 유지해야 한다고 **미리 알린다** · 쓰는 방법과 원하는 의견을 **분명히 적는다** · 여러 기능을 써 보게 권한다 · 테스터 단톡방을 고려한다 · 대상 사용자와 비슷한 사람을 모은다.
- 이 앱에 맞추면 **(추론)**:
  - 「자취 중이거나 집안일 · 뷰티 루틴이 있는 사람」을 먼저 고른다(실제로 쓸 이유가 있음).
  - 첫날 「루틴 3개 등록 + 위젯 올리기 + 알림 켜기」 세 가지만 부탁한다. 위젯 · 알림이 있으면 매일 앱을 떠올린다.
  - 3 · 7 · 12일차에 짧게 다시 연락한다(14일 계획 참고).
  - 참여 현황은 Console 에서 매일 본다. 15~20명으로 시작한다.

### 서로 테스트해 주는 커뮤니티

- 국내외에 「품앗이」 카페 · 오픈채팅 · Reddit · 텔레그램 · 전용 앱 · 유료 대행(크몽 · Fiverr 등)이 있다.
- 공식 근거: 도움말은 개인 · 직업 인맥, 대상 커뮤니티, SNS 로 모으라고 한다. **서로 테스트하기나 유료 테스터를 직접 금지하거나 허용하는 공식 문장은 찾지 못했다 (확인 안 됨).**
- 주의(근거 있는 것만):
  - 신청서가 「테스터가 모든 기능을 썼는지 · 실제 사용과 비슷했는지 · 어떤 의견을 받았는지」를 묻는다. 설치만 하고 안 쓰는 테스터로는 답할 내용이 없다 **(추론)**.
  - 「참여도 부족」이 더 테스트하라는 이유로 공식 문서에 있다.
  - 커뮤니티 글(테스터 매칭 서비스 운영자가 쓴 홍보성 글): 상호 테스트 그룹은 5일쯤에 많이 빠지고, 「즉시 테스터」 서비스는 가짜 참여가 드러나 불이익이 있다고 주장한다 — **공식 근거 없음**.
  - 상호 테스트는 남의 앱도 14일 동안 써 줘야 한다(하루 수십 분). 시간 비용이 크다 **(추론)**.
- 결론: **지인 위주**로 하고, 모자라면 커뮤니티로 2~5명만 보탠다. 돈을 내는 대행은 쓰지 않는다(포트폴리오에 「실제 의견」을 쓸 수 없음) **(추론)**.

출처: [Set up an open, closed, or internal test](https://support.google.com/googleplay/android-developer/answer/9845334?hl=en) · [App testing requirements for new personal developer accounts](https://support.google.com/googleplay/android-developer/answer/14151465?hl=en) · 참고(비공식 · 홍보성): [DEV — Google Play rejected my app after 14 days of testing](https://dev.to/testerscommunity/google-play-rejected-my-app-after-14-days-of-testing-here-is-what-i-did-wrong-3c21)

## 3. 의견 받기 (인터넷 권한 · 분석 SDK 없이)

앱 안에서는 사용 통계 · 오류 보고를 받을 수 없다. 사람이 직접 보내 주는 의견이 전부다.

| 방법 | 장점 | 단점 | 추천 |
|---|---|---|---|
| Play 비공개 의견 | 테스터가 Play 스토어에서 별점 + 글. 개발자만 봄. 공개 평점에 영향 없음 | 별점을 함께 줘야 함. 짧은 글 위주 **(추론)** | 기본 |
| 의견 이메일 | 참여 페이지에 보임(**필수 입력**: 이메일 또는 URL). 스크린샷 첨부 가능 | 공개되므로 개인 주소 대신 앱 전용 주소 | 기본 |
| 테스터 단톡방 | 공식 권장. 빨리 묻고 답함 | 서로 영향을 줌 | 추천 |
| 구글 폼 | 질문을 정해 고르게 받음. 의견 URL 로 쓸 수 있음 | 응답자 정보 수집 설정 주의 | 선택(7 · 14일차 설문) |
| Play Console 통계 · ANR/비정상 종료 | 설치 수 · 참여 수 | 이 앱 안 수집은 없음. Android vitals 가 기기 쪽에서 모으는 비정상 종료 정보가 테스트 트랙에 보이는지 **(확인 안 됨)** | 참고 |

- Play 비공개 의견(사용자 도움말 기준): Play 스토어 → 앱 및 기기 관리 → 베타 → 앱 → 「의견 작성」. 별점과 글 둘 다 써야 보낼 수 있고 "only the developer can view your feedback". 이 안내는 **이미 출시된 앱의 베타** 기준이라, 출시 전 비공개 테스트에서 같은 화면인지는 **(확인 안 됨)**.

### 앱 안에 「의견 보내기」를 넣는다면

- 공식 문서: 메일 앱만 받게 하려면 `ACTION_SENDTO` + `"mailto:"`. 받는 사람 · 제목은 `EXTRA_EMAIL` · `EXTRA_SUBJECT`, 본문은 `EXTRA_TEXT`.

```kotlin
val intent = Intent(Intent.ACTION_SENDTO).apply {
    data = Uri.parse("mailto:")                               // 메일 앱만
    putExtra(Intent.EXTRA_EMAIL, arrayOf("<의견 이메일>"))
    putExtra(Intent.EXTRA_SUBJECT, "[<앱 이름>] 의견")
    putExtra(Intent.EXTRA_TEXT, "앱 버전: <versionName> · Android <버전>\n\n")
}
try { context.startActivity(intent) } catch (e: ActivityNotFoundException) { /* 주소 보여 주기 · 복사 */ }
```

- 규칙 영향:
  - 메일을 보내는 것은 **메일 앱**이다. 이 앱에 `INTERNET` 이 필요 없다(9단계 방침 링크와 같은 원리) **(추론)** → 규칙 1 · 2 위반 없음.
  - `startActivity` 만 쓰면 `<queries>` 가 필요 없다(privacy.md 8절, 패키지 가시성 문서). 공식 예시의 `resolveActivity` 로 미리 확인하면 Android 11+ 에서 `<queries>` 가 필요할 수 있으므로 **쓰지 말고 `try/catch`** 로 처리한다 **(추론)**.
  - 새 라이브러리 없음. Manifest 변경 없음.
  - 본문에 기기 정보(앱 버전 · Android 버전)를 미리 넣는 것은 사용자가 보기 전에 확인하고 직접 보내므로 「수집」이 아니다 **(추론)**. 루틴 내용은 자동으로 넣지 않는다.
  - 데이터 보안 양식: 사용자가 직접 보내는 이메일은 앱이 보내는 것이 아니다 **(추론)**. 다만 방침의 「문의」 항목에 「보낸 이메일은 답장에만 씁니다」 한 줄을 더하면 안전하다 **(추론)**.
- 이 기능은 PLAN 「하지 않을 것」에 걸리지 않지만, 넣을지는 열린 질문. 넣지 않아도 설정 화면의 연락 이메일로 충분하다.

출처: [Common intents — Compose an email](https://developer.android.com/guide/components/intents-common) · [Package visibility — use cases](https://developer.android.com/training/package-visibility/use-cases) · [Join or leave a beta test (Google Play Help)](https://support.google.com/googleplay/answer/7003180?hl=en) · [Set up an open, closed, or internal test](https://support.google.com/googleplay/android-developer/answer/9845334?hl=en)

## 4. 프로덕션 신청 질문과 답 쓰는 요령

공식 도움말은 문항 원문 대신 세 묶음의 내용만 적어 둔다. 실제 문구는 Console 화면에서 확인한다 **(확인 안 됨)**.

| 묶음 | 묻는 것 | 이 앱 답 방향 (초안) |
|---|---|---|
| 비공개 테스트 | 테스터를 모으기 쉬웠는지 | 지인 중 자취 · 뷰티 루틴이 있는 사람 N명, 쉬웠음/어려웠음 사실대로 |
| | 테스터가 모든 기능을 썼는지 | 등록 · 달력 체크 · 위젯 체크 · 알림 · 백업 각각 써 본 사람 수(설문으로 셈) |
| | 실제 사용과 비슷했는지 | 「하루 1~2번 위젯 · 달력에서 체크」 — 실제 대상 사용자와 같은 방식 |
| | 받은 의견 요약과 받은 방법 | Play 비공개 의견 · 이메일 · 단톡방 · 설문, 건수와 대표 의견 3~5개 |
| 앱 정보 | 대상 사용자 | 혼자 사는 사람의 주기 집안일 · 뷰티 · 소모품 구매 |
| | 가치 | 한 번 등록 → 완료일 기준 다음 날짜 자동 계산, 달력 · 위젯, 로컬 전용(가입 · 광고 없음) |
| | 첫해 예상 설치 수 | 작은 범위(가장 낮은 구간)를 사실대로 **(추론)** |
| 준비 상태 | 테스트로 바꾼 점 | 의견 기록 표의 「바꾼 점」 열에서 버전별로(예: 1.0.1 에서 ○○ 고침) |
| | 준비됐다고 판단한 근거 | 마지막 판에서 새 오류 보고 없음 · 단위 시험 통과 · 개발자 실사용 기간 · 고친 의견 수 |

- 요령 **(추론)**:
  - 짧고 구체적으로. 날짜 · 사람 수 · 고친 판 번호를 넣는다.
  - 「버그 없음」 대신 「받은 의견 N건 중 M건 반영, 나머지는 이유와 함께 보류」.
  - 사실이 아닌 것을 쓰지 않는다(테스터 수 · 사용 여부는 Console 에서 보인다고 보고 쓴다).
  - 답을 미리 이 문서 아래나 DEVLOG 에 적어 두고 붙여 넣는다.
- 거절 · 재신청:
  - 공식: 더 테스트가 필요하면 "you may need to continue running your closed test". 이유 예: 참여자 12명 미만, 참여도 부족.
  - 거절 뒤 자동으로 무엇이 초기화되는지, 재신청 대기 기간이 있는지는 공식 문서에 없다 **(확인 안 됨)**. 비공식 글: 거절 문구는 대개 「더 테스트가 필요」처럼 막연하고, 불이익은 없지만 다시 몇 주가 든다.
  - 대응: 테스트를 끄지 않고 계속 → 참여를 늘림(새 판 · 설문) → 받은 의견과 바꾼 점을 보강 → 다시 신청 **(추론)**.
- 검토 기간: "Review usually takes seven days or less, but can occasionally take longer." 신청이 승인되면 프로덕션 트랙이 열리고, 출시 판도 따로 검토를 받는다. 일부 계정은 더 오래 검토한다는 문장이 게시 개요에 있다.

출처: [App testing requirements for new personal developer accounts](https://support.google.com/googleplay/android-developer/answer/14151465?hl=en) · [같은 문서 한국어판](https://support.google.com/googleplay/android-developer/answer/14151465?hl=ko) · [Publishing overview](https://support.google.com/googleplay/android-developer/answer/9859751?hl=en) · 참고(비공식): [DEV — rejected after 14 days](https://dev.to/testerscommunity/google-play-rejected-my-app-after-14-days-of-testing-here-is-what-i-did-wrong-3c21)

## 5. 비공개 테스트용 빌드 · 제출

- **AAB**: Play 는 Android App Bundle 을 올리면 기기별 APK 를 만들어 준다. 한 번 빌드 · 서명 · 업로드.
- **서명**: 9단계에서 만든 업로드 키로 서명한다(Play 앱 서명). 지금 `app/build.gradle.kts` 에는 `release` 서명 설정이 없다 — 키 경로 · 비밀번호는 `keystore.properties`(gitignore 됨)에서 읽게 한다. 빌드 파일 수정이므로 따로 계획한다.
- **버전 코드**: 지금 `versionCode = 1` · `versionName = "1.0"`. 원문 "You can't upload an APK to the Play Store with a versionCode you have already used". 최대 2100000000. 올릴 때마다 1씩 올린다(예: 1 → 2 → 3, 이름은 1.0.0 → 1.0.1). 내부 테스트에 올린 번호도 다시 쓸 수 없다 **(추론)**.
- **앱 설정 먼저**: 비공개 테스트는 앱 설정 완료가 필요하다. 데이터 보안 양식 원문: "must complete the Data safety form, including apps on closed, open, or production testing tracks", "Apps that are active on internal testing tracks are exempt". 방침 링크는 「스토어와 앱 안」에 있어야 한다는 일반 규칙이라 비공개 테스트 때도 넣는다 **(추론)**. → **9단계를 끝내고 10단계 시작.**
- **스토어 등록정보**: 앱 이름(30자) · 짧은 설명(80자) · 자세한 설명(4,000자) · 연락 이메일 필수. 비공개 테스트 단계에서 이미 필요하다 **(추론)**.
- **검토에 보내기**: 변경은 자동으로 검토에 가지 않는다. 「게시 개요」에서 직접 보낸다. 오류 수준 문제가 있으면 게시가 막히고, 경고는 막지 않는다. 관리형 게시를 켜면 승인 뒤 게시 시점을 직접 고른다.
- **검토 기간**: 비공개 테스트 판의 검토 기간은 문서에 없다 **(확인 안 됨)**. 게시 개요는 "up to seven days or longer in rare cases", 새 계정은 더 오래 볼 수 있다고 한다. 첫 게시는 며칠 여유를 둔다.
- **출시 빌드 점검** (9단계 체크리스트 ⑨와 같음): `processReleaseManifest` 병합 결과에 `INTERNET` · `AD_ID` · 정확한 알람 권한이 없는지 다시 확인. R8 축소(`isMinifyEnabled`)는 지금 꺼져 있다 — 켜면 Room · serialization 규칙 확인이 필요해 이번에는 끈 채로 낸다 **(추론, 열린 질문)**.

출처: [Prepare and roll out a release / Create and set up your app](https://support.google.com/googleplay/android-developer/answer/9859152?hl=en) · [Version your app](https://developer.android.com/studio/publish/versioning) · [Provide information for Google Play's Data safety section](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en) · [Prepare your app for review](https://support.google.com/googleplay/android-developer/answer/9859455?hl=en) · [Publishing overview](https://support.google.com/googleplay/android-developer/answer/9859751?hl=en)

## 6. 업데이트와 14일 집계

- 공식 권장: "continue running closed tests while fixing bugs" · 프로덕션 전에 비공개 테스트에서 업데이트해 낮은 평점을 줄여라.
- 비공식 글은 「테스트 도중 작은 업데이트를 올리지 말라」고 한다. **공식 문서와 반대**이고 근거가 없다. 공식 쪽을 따른다.
- 다만 업데이트는 검토를 거치므로 하루 이상 걸릴 수 있다. 14일 동안 **최대 2번**(7일차 · 12일차 무렵)으로 묶는다 **(추론)**.

출처: [App testing requirements for new personal developer accounts](https://support.google.com/googleplay/android-developer/answer/14151465?hl=en)

---

## 테스터 안내문 초안 (한국어)

> 카톡 · 문자로 보내는 짧은 메시지. `<…>` 는 자리표시.

```text
안녕! 내가 만든 안드로이드 앱 「<앱 이름>」 테스트를 부탁하고 싶어.
이불 빨래 2달마다, 팩 1주마다 같은 주기 루틴을 한 번 등록하면
달력에 언제 할지 보여 주고 체크하는 앱이야. 광고 · 가입 없고 인터넷도 안 써.

구글 정책상 12명 이상이 14일 동안 테스트에 참여해야 출시할 수 있어.

[참여 방법] (안드로이드 폰만 가능해. 아이폰은 안 돼 ㅠ)
1. 이 링크를 열어 줘: <참여 링크>
   → 폰 Play 스토어에 로그인된 구글 계정(<보내 준 Gmail>)으로 열어야 해
2. 「테스터 되기(참여)」 누르기
3. 같은 페이지의 Play 스토어 링크로 앱 설치
4. 루틴 3개 등록 → 홈 화면에 위젯 올리기 → 알림 켜기 (5분이면 돼)

[부탁]
- <시작일>부터 14일 동안 참여 취소 · 앱 삭제는 하지 말아 줘
- 생각날 때 하루 한 번 위젯이나 달력에서 체크해 보기
- 불편한 점, 이상한 점, 이런 거 있으면 좋겠다 다 환영!

[의견 보내는 곳]
- 단톡방: <단톡방 링크>
- 이메일: <의견 이메일>
- Play 스토어 → 앱 정보 화면의 「비공개 의견」도 돼

고마워! 출시되면 커피 쏠게
```

- 「커피」 같은 보상이 테스트 대가로 문제 되는지 공식 근거 없음 **(확인 안 됨)** — 넣을지는 열린 질문.

## 14일 계획 · 의견 기록 양식

### 날짜별 할 일

| 날짜 | 할 일 | 확인 |
|---|---|---|
| D-7 ~ D-1 | 9단계 완료(방침 · 데이터 보안 · 등록정보). 내부 테스트로 내 S26 에 Play 설치 확인. 테스터 15~20명 Gmail 받기 · 아이폰 여부 확인 | 내부 테스트 설치 성공 |
| D-1 | 비공개 테스트 트랙 만들기 · 이메일 목록 · 의견 이메일 입력 · 1.0.0(코드 N) 검토 보내기 | 검토 통과 · 「게시됨」 · 참여 링크 확인 |
| **1일차** | 안내문 발송, 단톡방 열기. 저녁에 Console 참여자 수 확인, 설치 못 한 사람 개별 도움 | 참여 ≥ 15명 |
| 2일차 | 첫 의견 기록 표에 옮기기. 막히는 설치 문제 해결 | |
| **3일차** | 짧은 연락: 「위젯 · 알림 잘 되는지?」 첫 의견 정리 · 급한 버그 분류 | 참여 ≥ 13명 유지 |
| 4~6일차 | 의견 고치기(작은 것부터), 시험 · 커밋 | |
| **7일차** | 중간 설문(구글 폼 선택, 5문항) · 1.0.1 올리기(고친 점 안내 메시지) · DEVLOG 중간 정리 | 업데이트 반영 확인 |
| 8~11일차 | 남은 의견 처리, 써 본 기능 수 세기 | |
| 12일차 | (필요하면) 1.0.2 올리기. 「이틀 남았어, 지우지 말아 줘」 연락 | 참여 ≥ 12명 + 여유 |
| **14일차** | 마지막 설문 · 감사 메시지. 신청서 답 초안을 표에서 뽑아 작성 | 14일 채운 사람 수 |
| 15일차 | Console 에서 조건 충족 표시 확인 뒤 프로덕션 신청 | 신청 완료 |
| 15~22일차 | 검토 대기(보통 7일 이내). 테스트는 계속 유지 | 결과 메일 |

- 신청 뒤에도 테스터에게 「결과 나올 때까지 그대로 유지」를 부탁한다(신청 시점 12명 조건) **(추론)**.

### 의견 기록 표 (이 문서 또는 DEVLOG 에 붙임)

```markdown
| # | 받은 날 | 경로 | 테스터 | 기기 · Android | 내용(요약) | 종류 | 처리 | 바꾼 판 · 커밋 |
|---|---|---|---|---|---|---|---|---|
| 1 | 11-xx | 단톡방 | T03 | 갤럭시 A · 14 | 위젯 글자가 잘림 | 버그 | 고침 | 1.0.1 · fix(widget): … |
| 2 | 11-xx | Play 의견 | T07 | — | 주 N회 루틴 원함 | 요청 | 보류(PLAN 5절) | — |
```

- 경로: Play 의견 · 이메일 · 단톡방 · 설문. 테스터는 이름 대신 T01~T20 번호로 적는다(개인정보를 저장소에 넣지 않음). 번호 ↔ 이름 표는 저장소 밖에 둔다.
- 종류: 버그 · 불편 · 요청 · 칭찬. 처리: 고침 · 보류(이유) · 안 함(「하지 않을 것」과 대조).
- DEVLOG 연결: 그날 절 「한 일」에 「의견 #3 · #5 고침(1.0.1)」처럼 번호만 적고, 막힌 점은 기존 형식(증상 · 원인 · 해결)으로.
- 날마다 Console 참여자 수를 한 줄 기록: `11-xx 참여 17 · 설치 16`(숫자만).

## 구현 순서 체크리스트

**사용자가 손으로 할 단계**

- [ ] ① 9단계 완료 확인: 방침 공개 · 데이터 보안 · 콘텐츠 등급 · 대상 연령 · 스토어 등록정보(이름 · 설명 · 스크린샷 · 연락 이메일)
- [ ] ② 의견용 이메일 정하기(방침 연락처와 같게 할지)
- [ ] ③ 테스터 후보 15~20명에게 미리 묻기: Android 인지 · Play 스토어 Gmail · 14일 가능 여부
- [ ] ④ 내부 테스트 트랙에 첫 AAB 올려 **내 폰에만** 설치 확인(지인은 넣지 않음)
- [ ] ⑤ 비공개 테스트 트랙 만들기 → 이메일 목록 → 의견 이메일 · URL → 국가(대한민국) → 검토 보내기
- [ ] ⑥ 「게시됨」 뒤 참여 링크 복사 → 안내문 발송 · 단톡방
- [ ] ⑦ 14일 계획 표대로 연락 · 설문 · 참여자 수 기록
- [ ] ⑧ 15일차 프로덕션 신청(4절 답 초안) → 결과 기록
- [ ] ⑨ 승인 뒤 프로덕션 출시 판 검토 보내기(관리형 게시로 시점 고르기 선택)

**코드로 할 단계**

- [ ] ⑩ 출시 서명 설정: `keystore.properties` 읽어 `release` `signingConfig`(비밀값 커밋 금지, `git diff --staged` 확인)
- [ ] ⑪ `versionCode` · `versionName` 올리는 규칙 정하기(올릴 때마다 +1, 1.0.0 부터) — `libs.versions.toml` 이 아닌 `defaultConfig` 값이라 규칙 5 와 무관한지 확인 **(추론: 규칙 5 는 라이브러리 버전 대상)**
- [ ] ⑫ `./gradlew bundleRelease` 로 AAB 만들기 · `processReleaseManifest` 결과에서 금지 권한 없는지 확인
- [ ] ⑬ (선택) 설정 화면 「의견 보내기」: `ACTION_SENDTO` + `mailto:` + `try/catch`, `<queries>` 없음 · 새 라이브러리 없음
- [ ] ⑭ 의견을 고칠 때마다: 시험 → `./gradlew test assembleDebug` → 커밋 → 판 올려 비공개 테스트에 업로드
- [ ] ⑮ 의견 기록 표 · 신청서 답을 이 문서 · DEVLOG 에 반영, PLAN 10단계 체크

## 열린 질문

1. 테스터를 지인만으로 15~20명 모을 수 있는지. 모자라면 상호 테스트 커뮤니티를 몇 명까지 쓸지(유료 대행은 쓰지 않는 쪽 추천).
2. 의견용 이메일을 방침 연락처와 같은 앱 전용 주소로 할지.
3. 앱 안에 「의견 보내기」(메일 앱 열기)를 넣을지, 설정 화면에 주소만 보일지.
4. 테스터에게 작은 보상(커피 등)을 줄지 — 정책상 괜찮은지 공식 근거를 못 찾음.
5. 구글 폼 설문을 쓸지(7 · 14일차). 쓴다면 응답자 이메일 수집을 끌지.
6. 테스트 동안 판을 몇 번 올릴지(추천 최대 2번), 판 이름 규칙(1.0.0 → 1.0.1).
7. 출시 빌드에서 R8 축소를 켤지(켜면 Room · serialization keep 규칙 시험 필요).
8. 테스트 국가를 대한민국만으로 할지, 해외 테스터(아는 사람)가 있으면 넓힐지.
9. 앱 이름(8단계 미정)이 비공개 테스트 전에 확정되는지 — 테스터 안내문 · 스토어 등록정보에 필요.
