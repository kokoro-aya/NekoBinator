package moe.irony.simplepc.json_parser

sealed class JsonValue {

    data class JsonObject(val dict: Map<String, JsonValue>): JsonValue(), Map<String, JsonValue> by dict
    data class JsonArray(val list: List<JsonValue>): JsonValue(), List<JsonValue> by list
    data class JsonString(val content: String): JsonValue()
    data class JsonNumber(val number: Double): JsonValue()
    data class JsonBool(val bool: Boolean): JsonValue()
    object JsonNull: JsonValue()
}