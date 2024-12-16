package com.wscodelabs.callLogs;

import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.database.Cursor;
import android.content.Context;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import org.json.JSONArray;
import org.json.JSONException;

import javax.annotation.Nullable;

public class CallLogModule extends ReactContextBaseJavaModule {

    private Context context;

    public CallLogModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.context = reactContext;
    }

    @Override
    public String getName() {
        return "CallLogs";
    }

    @ReactMethod
    public void loadAll(Promise promise) {
        load(-1, promise);
    }

    @ReactMethod
    public void load(int limit, Promise promise) {
        loadWithFilter(limit, null, promise);
    }

    @ReactMethod
    public void loadWithFilter(int limit, @Nullable ReadableMap filter, Promise promise) {
        try {
            Cursor cursor = this.context.getContentResolver().query(CallLog.Calls.CONTENT_URI,
                    null, null, null, CallLog.Calls.DATE + " DESC");

            WritableArray result = Arguments.createArray();

            if (cursor == null) {
                promise.resolve(result);
                return;
            }

            boolean nullFilter = filter == null;
            String minTimestamp = !nullFilter && filter.hasKey("minTimestamp") ? filter.getString("minTimestamp") : "0";
            String maxTimestamp = !nullFilter && filter.hasKey("maxTimestamp") ? filter.getString("maxTimestamp") : "-1";

            String types = !nullFilter && filter.hasKey("types") ? filter.getString("types") : "[]";
            JSONArray typesArray= new JSONArray(types);
            Set<String> typeSet = new HashSet<>(Arrays.asList(toStringArray(typesArray)));

            String phoneNumbers = !nullFilter && filter.hasKey("phoneNumbers") ? filter.getString("phoneNumbers") : "[]";
            JSONArray phoneNumbersArray= new JSONArray(phoneNumbers);
            Set<String> phoneNumberSet = new HashSet<>(Arrays.asList(toStringArray(phoneNumbersArray)));

            int callLogCount = 0;

            final int NUMBER_COLUMN_INDEX = cursor.getColumnIndex(Calls.NUMBER);
            final int TYPE_COLUMN_INDEX = cursor.getColumnIndex(Calls.TYPE);
            final int DATE_COLUMN_INDEX = cursor.getColumnIndex(Calls.DATE);
            final int DURATION_COLUMN_INDEX = cursor.getColumnIndex(Calls.DURATION);
            final int NAME_COLUMN_INDEX = cursor.getColumnIndex(Calls.CACHED_NAME);



            boolean minTimestampDefined = minTimestamp != null && !minTimestamp.equals("0");
            boolean minTimestampReached = false;

            while (cursor.moveToNext() && this.shouldContinue(limit, callLogCount) && !minTimestampReached) {
                String phoneNumber = cursor.getString(NUMBER_COLUMN_INDEX);
                int duration = cursor.getInt(DURATION_COLUMN_INDEX);
                String name = cursor.getString(NAME_COLUMN_INDEX);

                String timestampStr = cursor.getString(DATE_COLUMN_INDEX);
                minTimestampReached = minTimestampDefined && Long.parseLong(timestampStr) <= Long.parseLong(minTimestamp);

                DateFormat df = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.MEDIUM, SimpleDateFormat.MEDIUM);
                //DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String dateTime = df.format(new Date(Long.valueOf(timestampStr)));

                String type = this.resolveCallType(cursor.getInt(TYPE_COLUMN_INDEX));

                boolean passesPhoneFilter = phoneNumberSet == null || phoneNumberSet.isEmpty() || phoneNumberSet.contains(phoneNumber);
                boolean passesTypeFilter = typeSet == null || typeSet.isEmpty() || typeSet.contains(type);
                boolean passesMinTimestampFilter = minTimestamp == null || minTimestamp.equals("0") || Long.parseLong(timestampStr) >= Long.parseLong(minTimestamp);
                boolean passesMaxTimestampFilter = maxTimestamp == null || maxTimestamp.equals("-1") || Long.parseLong(timestampStr) <= Long.parseLong(maxTimestamp);
                boolean passesFilter = passesPhoneFilter && passesTypeFilter && passesMinTimestampFilter && passesMaxTimestampFilter;

                if (passesFilter) {
                    WritableMap callLog = Arguments.createMap();
                    callLog.putString("phoneNumber", phoneNumber);
                    callLog.putInt("duration", duration);
                    callLog.putString("name", name);
                    callLog.putString("timestamp", timestampStr);
                    callLog.putString("dateTime", dateTime);
                    callLog.putString("type", type);
                    callLog.putInt("rawType", cursor.getInt(TYPE_COLUMN_INDEX));
                    
                    // Add the additional fields based on the CallLog.Calls constants
                    callLog.putString("assertedDisplayName", cursor.getString(cursor.getColumnIndex(CallLog.Calls.ASSERTED_DISPLAY_NAME)));
                    callLog.putLong("autoMissedEmergencyCall", cursor.getLong(cursor.getColumnIndex(CallLog.Calls.AUTO_MISSED_EMERGENCY_CALL)));
                    callLog.putInt("autoMissedMaximumDialing", cursor.getInt(cursor.getColumnIndex(CallLog.Calls.AUTO_MISSED_MAXIMUM_DIALING)));
                    callLog.putInt("autoMissedMaximumRinging", cursor.getInt(cursor.getColumnIndex(CallLog.Calls.AUTO_MISSED_MAXIMUM_RINGING)));
                    callLog.putString("extraCallTypeFilter", cursor.getString(cursor.getColumnIndex(CallLog.Calls.EXTRA_CALL_TYPE_FILTER)));
                    callLog.putInt("blockedType", cursor.getInt(cursor.getColumnIndex(CallLog.Calls.BLOCKED_TYPE)));
                    callLog.putString("blockReason", cursor.getString(cursor.getColumnIndex(CallLog.Calls.BLOCK_REASON)));
                        callLog.putString("blockReasonBlockedNumber", cursor.getString(cursor.getColumnIndex(CallLog.Calls.BLOCK_REASON_BLOCKED_NUMBER)));
                        callLog.putString("blockReasonCallScreeningService", cursor.getString(cursor.getColumnIndex(CallLog.Calls.BLOCK_REASON_CALL_SCREENING_SERVICE)));
                        callLog.putString("blockReasonDirectToVoicemail", cursor.getString(cursor.getColumnIndex(CallLog.Calls.BLOCK_REASON_DIRECT_TO_VOICEMAIL)));
                        callLog.putString("blockReasonNotBlocked", cursor.getString(cursor.getColumnIndex(CallLog.Calls.BLOCK_REASON_NOT_BLOCKED)));
                        callLog.putString("blockReasonNotInContacts", cursor.getString(cursor.getColumnIndex(CallLog.Calls.BLOCK_REASON_NOT_IN_CONTACTS)));
                        callLog.putString("blockReasonPayPhone", cursor.getString(cursor.getColumnIndex(CallLog.Calls.BLOCK_REASON_PAY_PHONE)));
                        callLog.putString("blockReasonRestrictedNumber", cursor.getString(cursor.getColumnIndex(CallLog.Calls.BLOCK_REASON_RESTRICTED_NUMBER)));
                        callLog.putString("blockReasonUnknownNumber", cursor.getString(cursor.getColumnIndex(CallLog.Calls.BLOCK_REASON_UNKNOWN_NUMBER)));
                    callLog.putString("cachedFormattedNumber", cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_FORMATTED_NUMBER)));
                    callLog.putString("cachedLookupUri", cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_LOOKUP_URI)));
                    callLog.putString("cachedMatchedNumber", cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_MATCHED_NUMBER)));
                    callLog.putString("cachedNormalizedNumber", cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NORMALIZED_NUMBER)));
                    callLog.putString("cachedNumberLabel", cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NUMBER_LABEL)));
                    callLog.putString("cachedNumberType", cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NUMBER_TYPE)));
                    callLog.putString("cachedPhotoId", cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_PHOTO_ID)));
                    callLog.putString("cachedPhotoUri", cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_PHOTO_URI)));
                    callLog.putString("callScreeningAppName", cursor.getString(cursor.getColumnIndex(CallLog.Calls.CALL_SCREENING_APP_NAME)));
                    callLog.putString("callScreeningComponentName", cursor.getString(cursor.getColumnIndex(CallLog.Calls.CALL_SCREENING_COMPONENT_NAME)));
                    callLog.putString("composerPhotoUri", cursor.getString(cursor.getColumnIndex(CallLog.Calls.COMPOSER_PHOTO_URI)));
                    callLog.putString("contentItemType", cursor.getString(cursor.getColumnIndex(CallLog.Calls.CONTENT_ITEM_TYPE)));
                    callLog.putString("contentType", cursor.getString(cursor.getColumnIndex(CallLog.Calls.CONTENT_TYPE)));
                    callLog.putString("countryIso", cursor.getString(cursor.getColumnIndex(CallLog.Calls.COUNTRY_ISO)));
                callLog.putInt("dataUsage", cursor.getInt(cursor.getColumnIndex(CallLog.Calls.DATA_USAGE)));
                callLog.putInt("defaultSortOrder", cursor.getInt(cursor.getColumnIndex(CallLog.Calls.DEFAULT_SORT_ORDER)));
                callLog.putInt("features", cursor.getInt(cursor.getColumnIndex(CallLog.Calls.FEATURES)));
                    callLog.putInt("featuresAssistedDialingUsed", cursor.getInt(cursor.getColumnIndex(CallLog.Calls.FEATURES_ASSISTED_DIALING_USED)));
                    callLog.putInt("featuresHdCall", cursor.getInt(cursor.getColumnIndex(CallLog.Calls.FEATURES_HD_CALL)));
                    callLog.putInt("featuresPulledExternally", cursor.getInt(cursor.getColumnIndex(CallLog.Calls.FEATURES_PULLED_EXTERNALLY)));
                    callLog.putInt("featuresRtt", cursor.getInt(cursor.getColumnIndex(CallLog.Calls.FEATURES_RTT)));
                    callLog.putInt("featuresVideo", cursor.getInt(cursor.getColumnIndex(CallLog.Calls.FEATURES_VIDEO)));
                    callLog.putInt("featuresVolte", cursor.getInt(cursor.getColumnIndex(CallLog.Calls.FEATURES_VOLTE)));
                    callLog.putInt("featuresWifi", cursor.getInt(cursor.getColumnIndex(CallLog.Calls.FEATURES_WIFI)));
                    callLog.putString("geocodedLocation", cursor.getString(cursor.getColumnIndex(CallLog.Calls.GEOCODED_LOCATION)));
                    callLog.putInt("incomingType", cursor.getInt(cursor.getColumnIndex(CallLog.Calls.INCOMING_TYPE)));
                callLog.putInt("isBusinessCall", cursor.getInt(cursor.getColumnIndex(CallLog.Calls.IS_BUSINESS_CALL)));
                callLog.putInt("isRead", cursor.getInt(cursor.getColumnIndex(CallLog.Calls.IS_READ)));
                    callLog.putString("lastModified", cursor.getString(cursor.getColumnIndex(CallLog.Calls.LAST_MODIFIED)));
                    callLog.putString("limitParamKey", cursor.getString(cursor.getColumnIndex(CallLog.Calls.LIMIT_PARAM_KEY)));
                    callLog.putString("location", cursor.getString(cursor.getColumnIndex(CallLog.Calls.LOCATION)));
                callLog.putInt("missedReason", cursor.getInt(cursor.getColumnIndex(CallLog.Calls.MISSED_REASON)));
                    callLog.putInt("missedReasonNotMissed", cursor.getInt(cursor.getColumnIndex(CallLog.Calls.MISSED_REASON_NOT_MISSED)));
                    callLog.putInt("missedType", cursor.getInt(cursor.getColumnIndex(CallLog.Calls.MISSED_TYPE)));
                callLog.putInt("newField", cursor.getInt(cursor.getColumnIndex(CallLog.Calls.NEW)));
                    callLog.putString("numberPresentation", cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER_PRESENTATION)));
                    callLog.putString("offsetParamKey", cursor.getString(cursor.getColumnIndex(CallLog.Calls.OFFSET_PARAM_KEY)));
                    callLog.putString("outgoingType", cursor.getString(cursor.getColumnIndex(CallLog.Calls.OUTGOING_TYPE)));
                    callLog.putString("phoneAccountComponentName", cursor.getString(cursor.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME)));
                    callLog.putString("phoneAccountId", cursor.getString(cursor.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID)));
                    callLog.putString("postDialDigits", cursor.getString(cursor.getColumnIndex(CallLog.Calls.POST_DIAL_DIGITS)));
                    callLog.putString("presentationAllowed", cursor.getString(cursor.getColumnIndex(CallLog.Calls.PRESENTATION_ALLOWED)));
                    callLog.putString("presentationPayphone", cursor.getString(cursor.getColumnIndex(CallLog.Calls.PRESENTATION_PAYPHONE)));
                    callLog.putString("presentationRestricted", cursor.getString(cursor.getColumnIndex(CallLog.Calls.PRESENTATION_RESTRICTED)));
                    callLog.putString("presentationUnavailable", cursor.getString(cursor.getColumnIndex(CallLog.Calls.PRESENTATION_UNAVAILABLE)));
                    callLog.putString("presentationUnknown", cursor.getString(cursor.getColumnIndex(CallLog.Calls.PRESENTATION_UNKNOWN)));
                    callLog.putInt("priority", cursor.getInt(cursor.getColumnIndex(CallLog.Calls.PRIORITY)));
                    callLog.putInt("priorityNormal", cursor.getInt(cursor.getColumnIndex(CallLog.Calls.PRIORITY_NORMAL)));
                    callLog.putInt("priorityUrgent", cursor.getInt(cursor.getColumnIndex(CallLog.Calls.PRIORITY_URGENT)));
                    callLog.putInt("rejectedType", cursor.getInt(cursor.getColumnIndex(CallLog.Calls.REJECTED_TYPE)));
                    callLog.putString("subject", cursor.getString(cursor.getColumnIndex(CallLog.Calls.SUBJECT)));
                    callLog.putString("transcription", cursor.getString(cursor.getColumnIndex(CallLog.Calls.TRANSCRIPTION)));
                    callLog.putInt("userMissedCallFiltersTimeout", cursor.getInt(cursor.getColumnIndex(CallLog.Calls.USER_MISSED_CALL_FILTERS_TIMEOUT)));
                    callLog.putInt("userMissedCallScreeningServiceSilenced", cursor.getInt(cursor.getColumnIndex(CallLog.Calls.USER_MISSED_CALL_SCREENING_SERVICE_SILENCED)));
                    callLog.putInt("userMissedDndMode", cursor.getInt(cursor.getColumnIndex(CallLog.Calls.USER_MISSED_DND_MODE)));
                    callLog.putInt("userMissedLowRingVolume", cursor.getInt(cursor.getColumnIndex(CallLog.Calls.USER_MISSED_LOW_RING_VOLUME)));
                    callLog.putInt("userMissedNoAnswer", cursor.getInt(cursor.getColumnIndex(CallLog.Calls.USER_MISSED_NO_ANSWER)));
                    callLog.putInt("userMissedNoVibrate", cursor.getInt(cursor.getColumnIndex(CallLog.Calls.USER_MISSED_NO_VIBRATE)));
                    callLog.putInt("userMissedShortRing", cursor.getInt(cursor.getColumnIndex(CallLog.Calls.USER_MISSED_SHORT_RING)));
                    callLog.putString("viaNumber", cursor.getString(cursor.getColumnIndex(CallLog.Calls.VIA_NUMBER)));
                    callLog.putString("voicemailType", cursor.getString(cursor.getColumnIndex(CallLog.Calls.VOICEMAIL_TYPE)));
                    callLog.putString("voicemailUri", cursor.getString(cursor.getColumnIndex(CallLog.Calls.VOICEMAIL_URI)));
                    
                    result.pushMap(callLog);
                    callLogCount++;
                }
            }

            cursor.close();

            promise.resolve(result);
        } catch (JSONException e) {
            promise.reject(e);
        }
    }

    public static String[] toStringArray(JSONArray array) {
        if(array==null)
            return null;

        String[] arr=new String[array.length()];
        for(int i=0; i<arr.length; i++) {
            arr[i]=array.optString(i);
        }
        return arr;
    }

    private String resolveCallType(int callTypeCode) {
        switch (callTypeCode) {
            case Calls.OUTGOING_TYPE:
                return "OUTGOING";
            case Calls.INCOMING_TYPE:
                return "INCOMING";
            case Calls.MISSED_TYPE:
                return "MISSED";
            case Calls.VOICEMAIL_TYPE:
                return "VOICEMAIL";
            case Calls.REJECTED_TYPE:
                return "REJECTED";
            case Calls.BLOCKED_TYPE:
                return "BLOCKED";
            case Calls.ANSWERED_EXTERNALLY_TYPE:
                return "ANSWERED_EXTERNALLY";
            default:
                return "UNKNOWN";
        }
    }

    private boolean shouldContinue(int limit, int count) {
        return limit < 0 || count < limit;
    }
}

