package com.google.android.exoplayer2.drm;

import com.fasterxml.jackson.core.JsonPointer;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.Util;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class ClearKeyUtil {
    private static final String TAG = "ClearKeyUtil";

    private ClearKeyUtil() {
    }

    public static byte[] adjustRequestData(byte[] request) {
        if (Util.SDK_INT >= 27) {
            return request;
        }
        return Util.getUtf8Bytes(base64ToBase64Url(Util.fromUtf8Bytes(request)));
    }

    public static byte[] adjustResponseData(byte[] response) {
        if (Util.SDK_INT >= 27) {
            return response;
        }
        try {
            JSONObject responseJson = new JSONObject(Util.fromUtf8Bytes(response));
            StringBuilder adjustedResponseBuilder = new StringBuilder("{\"keys\":[");
            JSONArray keysArray = responseJson.getJSONArray("keys");
            for (int i = 0; i < keysArray.length(); i++) {
                if (i != 0) {
                    adjustedResponseBuilder.append(",");
                }
                JSONObject key = keysArray.getJSONObject(i);
                adjustedResponseBuilder.append("{\"k\":\"");
                adjustedResponseBuilder.append(base64UrlToBase64(key.getString("k")));
                adjustedResponseBuilder.append("\",\"kid\":\"");
                adjustedResponseBuilder.append(base64UrlToBase64(key.getString("kid")));
                adjustedResponseBuilder.append("\",\"kty\":\"");
                adjustedResponseBuilder.append(key.getString("kty"));
                adjustedResponseBuilder.append("\"}");
            }
            adjustedResponseBuilder.append("]}");
            return Util.getUtf8Bytes(adjustedResponseBuilder.toString());
        } catch (JSONException e) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("Failed to adjust response data: ");
            sb.append(Util.fromUtf8Bytes(response));
            Log.e(str, sb.toString(), e);
            return response;
        }
    }

    private static String base64ToBase64Url(String base64) {
        return base64.replace('+', '-').replace(JsonPointer.SEPARATOR, '_');
    }

    private static String base64UrlToBase64(String base64Url) {
        return base64Url.replace('-', '+').replace('_', JsonPointer.SEPARATOR);
    }
}
