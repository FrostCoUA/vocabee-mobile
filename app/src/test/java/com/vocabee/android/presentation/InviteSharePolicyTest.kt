package com.vocabee.android.feature.vocabulary.presentation

import com.vocabee.android.feature.vocabulary.data.api.DefaultReferralRewardBees
import com.vocabee.android.feature.vocabulary.data.api.ReferralResponse
import com.vocabee.android.feature.vocabulary.presentation.platform.InviteMessenger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InviteSharePolicyTest {
    @Test
    fun referralFallbackAndShareCopyUseFiftyBees() {
        val response = ReferralResponse(
            code = "bee-123",
            link = "https://vocabee.app/i/bee-123",
        )
        val message = inviteShareMessage(response.link, response.rewardBees)

        assertEquals(50, DefaultReferralRewardBees)
        assertEquals(50, response.rewardBees)
        assertTrue(message.contains("по 50 монеток"))
        assertTrue(message.endsWith(response.link))
    }

    @Test
    fun shareTargetsKeepFixedOrderAndAlwaysEndWithSystemShare() {
        val targets = inviteShareTargets(
            listOf(InviteMessenger.Viber, InviteMessenger.Telegram),
        )

        assertEquals(
            listOf(InviteMessenger.Telegram, InviteMessenger.Viber, null),
            targets.map { it.messenger },
        )
        assertEquals(listOf("Telegram", "Viber", "Ще"), targets.map { it.label })
    }

    @Test
    fun withoutInstalledMessengersOnlySystemShareIsShown() {
        val targets = inviteShareTargets(emptyList())

        assertEquals(1, targets.size)
        assertEquals(null, targets.single().messenger)
        assertEquals("Ще", targets.single().label)
    }
}
