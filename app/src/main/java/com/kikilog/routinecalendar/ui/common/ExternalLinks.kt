package com.kikilog.routinecalendar.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

// 앱 밖(브라우저 · 메일 앱)으로 보내는 링크. 앱 자체는 인터넷에 연결하지 않는다 — 다른 앱을 열 뿐이다.

/** 개인정보처리방침(GitHub Pages, docs/privacy/index.md) */
const val PRIVACY_POLICY_URL = "https://songhaehwang.github.io/routine-calendar/privacy/"

/** 의견 · 문의를 받는 주소. 방침 · Play 등록정보에 공개된 연락처와 같다 */
const val FEEDBACK_EMAIL = "hwangsonghae99@gmail.com"

/** 방침을 브라우저로 연다. 열 앱이 없으면 false */
fun openPrivacyPolicy(context: Context): Boolean =
    startOrFalse(context, Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))

/** 메일 앱으로 의견 보내기 화면을 연다. 메일 앱이 없으면 false */
fun openFeedbackMail(context: Context, appVersion: String): Boolean =
    startOrFalse(context, feedbackMailIntent(appVersion))

/**
 * 받는 사람 · 제목 · 본문 첫머리(앱 · Android 판)를 채운 메일 작성 Intent.
 * mailto: 로 메일 앱만 고르게 하고, 보낼지는 사용자가 메일 앱에서 정한다.
 */
fun feedbackMailIntent(appVersion: String, androidVersion: String = Build.VERSION.RELEASE.orEmpty()): Intent =
    Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(FEEDBACK_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, "[때됐냥] 의견")
        putExtra(Intent.EXTRA_TEXT, "\n\n---\n앱 버전: $appVersion\nAndroid: $androidVersion\n")
    }

// 직접 startActivity 하는 암시적 Intent 는 <queries> 선언 없이도 열린다. 받을 앱이 없을 때만 예외
private fun startOrFalse(context: Context, intent: Intent): Boolean =
    try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
