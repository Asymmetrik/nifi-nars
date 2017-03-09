package com.asymmetrik.nifi.standard.processors.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

@SuppressWarnings("Duplicates")
public final class JsonUtil {
    private JsonUtil(/* empty */) {
    }

    public static boolean updateField(JsonElement json, String field, JsonElement value) {
        if (json == null || !(json instanceof JsonObject)) {
            return false;
        }

        JsonObject jsonObject = (JsonObject) json;

        // We reached the leaf, update the field if it exists
        if (!field.contains(".")) {

            JsonElement element = jsonObject.get(field);
            if (element == null) {
                return false;
            }

            if (element.isJsonPrimitive()) {
                jsonObject.add(field, value);
                return true;
            } else {
                return false;
            }
        }

        int i = field.indexOf('.');
        JsonObject obj = jsonObject.getAsJsonObject(field.substring(0, i));
        String key = field.substring(i + 1, field.length());
        return updateField(obj, key, value);
    }

    public static boolean updateField(JsonElement json, String field, String value) {
        if (json == null || !(json instanceof JsonObject)) {
            return false;
        }

        JsonObject jsonObject = (JsonObject) json;

        // We reached the leaf, update the field if it exists
        if (!field.contains(".")) {

            JsonElement element = jsonObject.get(field);
            if (element == null) {
                return false;
            }

            if (element.isJsonPrimitive()) {
                jsonObject.addProperty(field, value);
                return true;
            } else {
                return false;
            }
        }

        int i = field.indexOf('.');
        JsonObject child = jsonObject.getAsJsonObject(field.substring(0, i));
        String childField = field.substring(i + 1, field.length());
        return updateField(child, childField, value);
    }

    public static Map<String, String> getJsonPathsForTemplating(Set<Map.Entry<String, JsonElement>> root) {
        Map<String, String> paths = new HashMap<>();
        searchJsonForTemplate(root, paths, "$");
        return paths;
    }

    public static Map<String, String> getJsonPathsForTemplating(JsonObject json) {
        Map<String, String> paths = new HashMap<>();
        searchJsonForTemplate(json.entrySet(), paths, "$");
        return paths;
    }

    static void searchJsonForTemplate(Set<Map.Entry<String, JsonElement>> entries, Map<String, String> paths, String currentPath) {
        for (Map.Entry<String, JsonElement> entry : entries) {

            Object value = entry.getValue();
            if (value instanceof JsonPrimitive) {
                String v = ((JsonPrimitive) value).getAsString().trim();
                if (v.startsWith("{{")) {
                    paths.put(currentPath + "." + entry.getKey(), v);
                }
            } else if (value instanceof JsonObject) {
                searchJsonForTemplate(((JsonObject) value).entrySet(), paths, currentPath + "." + entry.getKey());
            }
        }
    }
}
