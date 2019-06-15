/*
 Copyright 2016 Urban Airship and Contributors
*/

package com.urbanairship.extension.segment;

import com.segment.analytics.Analytics;
import com.segment.analytics.ValueMap;
import com.segment.analytics.integrations.GroupPayload;
import com.segment.analytics.integrations.IdentifyPayload;
import com.segment.analytics.integrations.Integration;
import com.segment.analytics.integrations.ScreenPayload;
import com.segment.analytics.integrations.TrackPayload;
import com.urbanairship.Autopilot;
import com.urbanairship.UAirship;
import com.urbanairship.analytics.CustomEvent;
import com.urbanairship.util.UAStringUtil;

import java.util.HashSet;


/**
 * Urban Airship Segment integration.
 */
public class UrbanAirshipIntegration extends Integration<UAirship> {

    public static final String URBAN_AIRSHIP_KEY = "Urban Airship";

    public static final Factory FACTORY = new Factory() {

        @Override
        public Integration<?> create(ValueMap settings, Analytics analytics) {
            SegmentAutopilot.updateSegmentSettings(analytics.getApplication(), settings);
            Autopilot.automaticTakeOff(analytics.getApplication());

            // Make sure we actually are flying before returning the integration
            if (UAirship.isFlying() || UAirship.isTakingOff()) {
                return new UrbanAirshipIntegration(UAirship.shared());
            }

            return null;
        }

        @Override
        public String key() {
            return URBAN_AIRSHIP_KEY;
        }
    };

    private final UAirship airship;

    UrbanAirshipIntegration(UAirship airship) {
        this.airship = airship;
    }

    @Override
    public void identify(IdentifyPayload identify) {
        airship.getNamedUser().setId(identify.userId());
    }

    @Override
    public void group(GroupPayload group) {
        String groupId = group.groupId();
        if (UAStringUtil.isEmpty(groupId)) {
            return;
        }

        airship.getPushManager()
                .editTags()
                .addTag(groupId)
                .apply();
    }

    @Override
    public void track(TrackPayload track) {
        CustomEvent.Builder eventBuilder = new CustomEvent.Builder(track.event());

        if (track.properties().containsKey("revenue")) {
            eventBuilder.setEventValue(track.properties().revenue());
        } else if (track.properties().containsKey("value")) {
            eventBuilder.setEventValue(track.properties().value());
        }

        for (String key : track.properties().keySet()) {
            Object value = track.properties().get(key);

            if (value instanceof String) {
                eventBuilder.addProperty(key, (String) value);
            } else if (value instanceof Integer) {
                eventBuilder.addProperty(key, (Integer) value);
            } else if (value instanceof Long) {
                eventBuilder.addProperty(key, (Long) value);
            } else if (value instanceof Boolean) {
                eventBuilder.addProperty(key, (Boolean) value);
            } else if (value instanceof Number) {
                eventBuilder.addProperty(key, ((Number)value).doubleValue());
            }
        }

        airship.getAnalytics().addEvent(eventBuilder.build());
    }

    @Override
    public void screen(ScreenPayload screen) {
        airship.getAnalytics().trackScreen(screen.event());
    }

    @Override
    public void reset() {
        airship.getNamedUser().setId(null);
        airship.getPushManager().setTags(new HashSet<String>());
    }

    @Override
    public UAirship getUnderlyingInstance() {
        return airship;
    }

}
