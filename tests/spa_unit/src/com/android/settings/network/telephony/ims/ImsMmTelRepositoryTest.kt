/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.network.telephony.ims

import android.content.Context
import android.telephony.CarrierConfigManager
import android.telephony.CarrierConfigManager.KEY_USE_WFC_HOME_NETWORK_MODE_IN_ROAMING_NETWORK_BOOL
import android.telephony.TelephonyManager
import android.telephony.ims.ImsMmTelManager
import androidx.core.os.persistableBundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class ImsMmTelRepositoryTest {
    private val mockTelephonyManager = mock<TelephonyManager> {
        on { createForSubscriptionId(SUB_ID) } doReturn mock
    }

    private val mockCarrierConfigManager = mock<CarrierConfigManager>()

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { getSystemService(TelephonyManager::class.java) } doReturn mockTelephonyManager
        on { getSystemService(CarrierConfigManager::class.java) } doReturn mockCarrierConfigManager
    }

    private val mockImsMmTelManager = mock<ImsMmTelManager> {
        on { isVoWiFiSettingEnabled } doReturn true
        on { getVoWiFiRoamingModeSetting() } doReturn ImsMmTelManager.WIFI_MODE_WIFI_PREFERRED
        on { getVoWiFiModeSetting() } doReturn ImsMmTelManager.WIFI_MODE_CELLULAR_PREFERRED
    }

    private val repository = ImsMmTelRepositoryImpl(context, SUB_ID, mockImsMmTelManager)

    @Test
    fun getWiFiCallingMode_voWiFiSettingNotEnabled_returnUnknown() {
        mockImsMmTelManager.stub {
            on { isVoWiFiSettingEnabled } doReturn false
        }

        val wiFiCallingMode = repository.getWiFiCallingMode()

        assertThat(wiFiCallingMode).isEqualTo(ImsMmTelManager.WIFI_MODE_UNKNOWN)
    }

    @Test
    fun getWiFiCallingMode_roamingAndNotUseWfcHomeModeForRoaming_returnRoamingSetting() {
        mockTelephonyManager.stub {
            on { isNetworkRoaming } doReturn true
        }
        mockUseWfcHomeModeForRoaming(false)

        val wiFiCallingMode = repository.getWiFiCallingMode()

        assertThat(wiFiCallingMode).isEqualTo(mockImsMmTelManager.getVoWiFiRoamingModeSetting())
    }

    @Test
    fun getWiFiCallingMode_roamingAndUseWfcHomeModeForRoaming_returnHomeSetting() {
        mockTelephonyManager.stub {
            on { isNetworkRoaming } doReturn true
        }
        mockUseWfcHomeModeForRoaming(true)

        val wiFiCallingMode = repository.getWiFiCallingMode()

        assertThat(wiFiCallingMode).isEqualTo(mockImsMmTelManager.getVoWiFiModeSetting())
    }

    @Test
    fun getWiFiCallingMode_notRoaming_returnHomeSetting() {
        mockTelephonyManager.stub {
            on { isNetworkRoaming } doReturn false
        }

        val wiFiCallingMode = repository.getWiFiCallingMode()

        assertThat(wiFiCallingMode).isEqualTo(mockImsMmTelManager.getVoWiFiModeSetting())
    }

    private fun mockUseWfcHomeModeForRoaming(config: Boolean) {
        mockCarrierConfigManager.stub {
            on {
                getConfigForSubId(SUB_ID, KEY_USE_WFC_HOME_NETWORK_MODE_IN_ROAMING_NETWORK_BOOL)
            } doReturn persistableBundleOf(
                KEY_USE_WFC_HOME_NETWORK_MODE_IN_ROAMING_NETWORK_BOOL to config,
            )
        }
    }

    private companion object {
        const val SUB_ID = 1
    }
}
