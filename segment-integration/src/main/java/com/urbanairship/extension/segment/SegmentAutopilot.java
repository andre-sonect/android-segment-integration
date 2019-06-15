/*
 Copyright 2016 Urban Airship and Contributors
*/

package com.urbanairship.extension.segment;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.annotation.NonNull;

import com.segment.analytics.ValueMap;
import com.urbanairship.AirshipConfigOptions;
import com.urbanairship.Autopilot;
import com.urbanairship.Logger;
import com.urbanairship.UAirship;
import com.urbanairship.util.UAStringUtil;

/**
 * Autopilot for segment integration.
 * <p/>
 * Autopilot allows segment integration call to be called outside of {@link Application#onCreate()}.
 * When a push is received and wakes the application up, Urban Airship will used the last config to
 * takeoff.
 */
public class SegmentAutopilot extends Autopilot {

    private static final String APP_KEY = "appKey";
    private static final String APP_SECRET = "appSecret";
    private static final String GCM_SENDER = "gcmSender";
    private static final String NOTIFICATION_ICON = "notificationIcon";
    private static final String NOTIFICATION_ACCENT_COLOR = "notificationAccentColor";
    private static final String PREFERENCE_NAME = "com.urbanairship.extension.segment";

    @Override
    public AirshipConfigOptions createAirshipConfigOptions(@NonNull Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);

        AirshipConfigOptions.Builder builder = new AirshipConfigOptions.Builder();

        // Call before applyDefaultProperties so we can default inProduction to true, but allow
        // local airshipconfig.properties to override it.
        builder.setInProduction(true)
                .applyDefaultProperties(context);

        // Apply production App credentials from Segment
        if (preferences.contains(APP_KEY) && preferences.contains(APP_SECRET)) {
            builder.setProductionAppKey(preferences.getString(APP_KEY, null))
                    .setProductionAppSecret(preferences.getString(APP_SECRET, null));
        }

        // Apply GCM Sender from Segment
        if (preferences.contains(GCM_SENDER)) {
            builder.setFcmSenderId(preferences.getString(GCM_SENDER, null));
        }

        if (preferences.contains(NOTIFICATION_ACCENT_COLOR)) {
            builder.setNotificationAccentColor(preferences.getInt(NOTIFICATION_ACCENT_COLOR, 0));
        }

        if (preferences.contains(NOTIFICATION_ICON)) {
            builder.setNotificationIcon(preferences.getInt(NOTIFICATION_ICON, 0));
        }

        return builder.build();
    }

    /**
     * Updates the Segment settings for the Urban Airship config.
     *
     * @param context The application context.
     * @param settings Segment settings.
     */
    static void updateSegmentSettings(Context context, ValueMap settings) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE).edit()
                .putString(APP_KEY, settings.getString(APP_KEY))
                .putString(APP_SECRET, settings.getString(APP_SECRET))
                .putString(GCM_SENDER, settings.getString(GCM_SENDER));


        // Convert accent color hex string to an int
        String accentColor = settings.getString(NOTIFICATION_ACCENT_COLOR);
        if (!UAStringUtil.isEmpty(accentColor)) {
            try {
                editor.putInt(NOTIFICATION_ACCENT_COLOR, Color.parseColor(accentColor));
            } catch (IllegalArgumentException e) {
                Logger.error("Unable to parse notification accent color: " + accentColor, e);
            }
        }

        // Convert notification name to a drawable resource ID
        String notificationIconName = settings.getString(NOTIFICATION_ICON);
        if (!UAStringUtil.isEmpty(notificationIconName)) {
            int id  = context.getResources().getIdentifier(notificationIconName, "drawable", context.getPackageName());
            if (id != 0) {
                editor.putInt(NOTIFICATION_ICON, id);
            } else {
                Logger.error("Unable to find notification icon with name: " + notificationIconName);
            }
        }

        editor.apply();
    }

    @Override
    public void onAirshipReady(UAirship uAirship) {
        Logger.info("SegmentAutopilot - Airship Ready");
    }

    @Override
    public boolean allowEarlyTakeOff(@NonNull Context context) {
        return false;
    }
}
