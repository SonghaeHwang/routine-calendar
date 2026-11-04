package com.kikilog.routinecalendar.ui.common

import android.content.Intent
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExternalLinksTest {

    @Test
    fun `의견 메일은 mailto 로 받는 사람과 버전을 채운다`() {
        val intent = feedbackMailIntent(appVersion = "1.0", androidVersion = "17")

        assertEquals(Intent.ACTION_SENDTO, intent.action)
        assertEquals("mailto:", intent.data.toString())
        assertArrayEquals(arrayOf(FEEDBACK_EMAIL), intent.getStringArrayExtra(Intent.EXTRA_EMAIL))
        assertEquals("[때됐냥] 의견", intent.getStringExtra(Intent.EXTRA_SUBJECT))
        val body = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        assertTrue(body.contains("앱 버전: 1.0"))
        assertTrue(body.contains("Android: 17"))
    }

    @Test
    fun `방침 주소는 https 공개 페이지`() {
        assertTrue(PRIVACY_POLICY_URL.startsWith("https://"))
        assertTrue(PRIVACY_POLICY_URL.endsWith("/privacy/"))
    }
}
