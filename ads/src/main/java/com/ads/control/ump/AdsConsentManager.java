package com.ads.control.ump;

import static com.google.android.ump.ConsentInformation.ConsentStatus.OBTAINED;
import static com.google.android.ump.ConsentInformation.ConsentStatus.REQUIRED;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.ads.control.ads.AdUtils;
import com.ads.control.event.FirebaseAnalyticsUtil;
import com.google.android.ump.ConsentDebugSettings;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

import java.util.concurrent.atomic.AtomicBoolean;

public class AdsConsentManager {
    private static final String TAG = "AdsConsentManager";
    private final Activity activity;
    private final AtomicBoolean isMobileAdsInitializeCalled = new AtomicBoolean(false);

    public AdsConsentManager(Activity activity) {
        this.activity = activity;
    }

    public static boolean isEnabledUMP(Activity activity) {
        ConsentInformation consentInformation = UserMessagingPlatform.getConsentInformation(activity);
        return consentInformation.getConsentStatus() == OBTAINED || consentInformation.getConsentStatus() == REQUIRED;
    }

    public static boolean getConsentResult(Context context) {
        String string = context.getSharedPreferences(context.getPackageName() + "_preferences", 0).getString("IABTCF_PurposeConsents", "");
        Log.d(TAG, "consentResult: " + string);
        if (string.isEmpty()) {
            return true;
        }
        return String.valueOf(string.charAt(0)).equals("1");
    }

    public void requestUMP(UMPResultListener uMPResultListener) {
        requestUMP(false, "", false, uMPResultListener);
    }

    public void showPrivacyOption(Activity activity, UMPResultListener uMPResultListener) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity, formError -> {
            if (formError != null) {
                Log.w(TAG, String.format("%s: %s", formError.getErrorCode(), formError.getMessage()));
                FirebaseAnalyticsUtil.logEventTracking(activity, "ump_consent_failed", new Bundle());
            }
            if (getConsentResult(activity)) {
                AdUtils.getInstance().initAdsNetwork();
            }
            Bundle bundle = new Bundle();
            bundle.putBoolean("consent", getConsentResult(activity));
            FirebaseAnalyticsUtil.logEventTracking(activity, "ump_consent_result", bundle);
            uMPResultListener.onCheckUMPSuccess(getConsentResult(activity));
        });
    }

    public void requestUMP(Boolean isDebug, String hashedId, Boolean isReset, UMPResultListener uMPResultListener) {
        ConsentRequestParameters.Builder builder = new ConsentRequestParameters.Builder();
        if (isDebug) {
            builder.setConsentDebugSettings(new ConsentDebugSettings.Builder(activity).setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA).addTestDeviceHashedId(hashedId).build());
        }
        ConsentRequestParameters build = builder.setTagForUnderAgeOfConsent(false).build();
        ConsentInformation consentInformation = UserMessagingPlatform.getConsentInformation(activity);
        if (isReset) {
            consentInformation.reset();
        }
        consentInformation.requestConsentInfoUpdate(activity, build, () -> {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity, formError -> {
                if (formError != null) {
                    Log.w(TAG, String.format("%s: %s", formError.getErrorCode(), formError.getMessage()));
                    Bundle bundle = new Bundle();
                    bundle.putInt("error_code", formError.getErrorCode());
                    bundle.putString("error_msg", formError.getMessage());
                    FirebaseAnalyticsUtil.logEventTracking(activity, "ump_consent_failed", bundle);
                }
                Bundle bundle2 = new Bundle();
                bundle2.putBoolean("consent", getConsentResult(activity));
                FirebaseAnalyticsUtil.logEventTracking(activity, "ump_consent_result", bundle2);
                if (isMobileAdsInitializeCalled.getAndSet(true)) {
                    return;
                }
                uMPResultListener.onCheckUMPSuccess(getConsentResult(activity));
                AdUtils.getInstance().initAdsNetwork();
            });
        }, formError -> {
            Log.w(TAG, String.format("%s: %s", formError.getErrorCode(), formError.getMessage()));
            Bundle bundle = new Bundle();
            bundle.putInt("error_code", formError.getErrorCode());
            bundle.putString("error_msg", formError.getMessage());
            FirebaseAnalyticsUtil.logEventTracking(activity, "ump_request_failed", bundle);
            if (isMobileAdsInitializeCalled.getAndSet(true)) {
                return;
            }
            uMPResultListener.onCheckUMPSuccess(getConsentResult(activity));
            AdUtils.getInstance().initAdsNetwork();
        });
        if (!consentInformation.canRequestAds() || isMobileAdsInitializeCalled.getAndSet(true)) {
            return;
        }
        uMPResultListener.onCheckUMPSuccess(getConsentResult(activity));
        AdUtils.getInstance().initAdsNetwork();
    }
}