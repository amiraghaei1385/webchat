package utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.time.LocalDateTime;
import java.util.*;

/**
 * این کلاس با
 * (User، Message، Chat، Group و ...) کار می‌کند و بدون نیاز به annotation
 * یا تغییر در کلاس‌های موجود، تبدیل Object &lt;-&gt; JSON را انجام می‌دهد.
 *
 * این کلاس برای پایگاه‌داده فایلی (هر رکورد = یک فایل .txt حاوی یک شیء
 * JSON) استفاده می‌شود؛ برای لیست از در
 * استفاده کنید (هر فایل یک رکورد).
 */
public final class JsonUtil {

    private JsonUtil() {
        // کلاس Utility - نمونه‌سازی مجاز نیست
    }

    // Serialize

    /** تبدیل هر شیء (مدل) به رشته JSON */
    public static String toJson(Object obj) {
        StringBuilder sb = new StringBuilder();
        writeValue(obj, sb);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void writeValue(Object value, StringBuilder sb) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String) {
            writeString((String) value, sb);
        } else if (value instanceof Boolean || value instanceof Integer
                || value instanceof Long || value instanceof Double) {
            sb.append(value);
        } else if (value instanceof Enum) {
            writeString(((Enum<?>) value).name(), sb);
        } else if (value instanceof LocalDateTime) {
            writeString(value.toString(), sb);
        } else if (value instanceof List) {
            writeList((List<Object>) value, sb);
        } else if (value instanceof Map) {
            writeMap((Map<String, Object>) value, sb);
        } else {
            // شیء تودرتو (nested model) - با همین سریالایزر و reflection
            writeObject(value, sb);
        }
    }

    private static void writeString(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    private static void writeList(List<Object> list, StringBuilder sb) {
        sb.append('[');
        for (int i = 0; i < list.size(); i++) {
            if (i > 0)
                sb.append(',');
            writeValue(list.get(i), sb);
        }
        sb.append(']');
    }

    private static void writeMap(Map<String, Object> map, StringBuilder sb) {
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first)
                sb.append(',');
            first = false;
            writeString(e.getKey(), sb);
            sb.append(':');
            writeValue(e.getValue(), sb);
        }
        sb.append('}');
    }

    private static void writeObject(Object obj, StringBuilder sb) {
        sb.append('{');
        List<Field> fields = collectFields(obj.getClass());
        boolean first = true;
        for (Field f : fields) {
            f.setAccessible(true);
            Object val;
            try {
                val = f.get(obj);
            } catch (IllegalAccessException e) {
                continue;
            }
            if (!first)
                sb.append(',');
            first = false;
            writeString(f.getName(), sb);
            sb.append(':');
            writeValue(val, sb);
        }
        sb.append('}');
    }

    // Deserialize

    /**
     * تبدیل رشته JSON به یک نمونه از کلاس داده‌شده.
     * نیازمند سازنده بدون آرگومان در کلاس مقصد است -
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) {
            return null;
        }
        JsonParser parser = new JsonParser(json);
        Object parsed = parser.parseValue();
        return convertToObject(parsed, clazz);
    }

    /** تبدیل یک لیست JSON (آرایه از اشیاء) به */
    @SuppressWarnings("unchecked")
    public static <T> List<T> fromJsonList(String json, Class<T> clazz) {
        List<T> result = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return result;
        }
        JsonParser parser = new JsonParser(json);
        Object parsed = parser.parseValue();
        if (!(parsed instanceof List)) {
            return result;
        }
        for (Object item : (List<Object>) parsed) {
            result.add(convertToObject(item, clazz));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static <T> T convertToObject(Object parsedMap, Class<T> clazz) {
        if (parsedMap == null) {
            return null;
        }
        if (!(parsedMap instanceof Map)) {
            throw new RuntimeException("Expected JSON object for " + clazz.getSimpleName());
        }
        Map<String, Object> map = (Map<String, Object>) parsedMap;

        try {
            T instance = clazz.getDeclaredConstructor().newInstance();
            for (Field f : collectFields(clazz)) {
                if (!map.containsKey(f.getName())) {
                    continue;
                }
                f.setAccessible(true);
                Object rawValue = map.get(f.getName());
                Object converted = convertFieldValue(rawValue, f);
                f.set(instance, converted);
            }
            return instance;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to deserialize " + clazz.getName(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Object convertFieldValue(Object rawValue, Field field) throws ReflectiveOperationException {
        if (rawValue == null) {
            return null;
        }
        Class<?> type = field.getType();

        if (type == String.class) {
            return rawValue.toString();
        }
        if (type == boolean.class || type == Boolean.class) {
            return rawValue instanceof Boolean ? rawValue : Boolean.parseBoolean(rawValue.toString());
        }
        if (type == int.class || type == Integer.class) {
            return rawValue instanceof Number ? ((Number) rawValue).intValue() : Integer.parseInt(rawValue.toString());
        }
        if (type == long.class || type == Long.class) {
            return rawValue instanceof Number ? ((Number) rawValue).longValue() : Long.parseLong(rawValue.toString());
        }
        if (type == double.class || type == Double.class) {
            return rawValue instanceof Number ? ((Number) rawValue).doubleValue()
                    : Double.parseDouble(rawValue.toString());
        }
        if (type == LocalDateTime.class) {
            return LocalDateTime.parse(rawValue.toString());
        }
        if (type.isEnum()) {
            return Enum.valueOf((Class<Enum>) type, rawValue.toString());
        }
        if (List.class.isAssignableFrom(type)) {
            List<Object> rawList = (List<Object>) rawValue;
            List<Object> result = new ArrayList<>();
            Class<?> itemType = resolveListItemType(field);
            for (Object item : rawList) {
                if (itemType == String.class || itemType == null) {
                    result.add(item == null ? null : item.toString());
                } else {
                    result.add(convertToObject(item, itemType));
                }
            }
            return result;
        }
        // شیء تودرتو
        return convertToObject(rawValue, type);
    }

    private static Class<?> resolveListItemType(Field field) {
        try {
            ParameterizedType pt = (ParameterizedType) field.getGenericType();
            return (Class<?>) pt.getActualTypeArguments()[0];
        } catch (Exception e) {
            return String.class;
        }
    }

    /**
     * تمام فیلدهای instance یک کلاس را جمع‌آوری می‌کند،
     * شامل فیلدهای موروثی از کلاس‌های والد (در صورت وجود ارث‌بری).
     */
    private static List<Field> collectFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field f : current.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers()) && !f.isSynthetic()) {
                    fields.add(f);
                }
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    // Parser داخلی

    /**
     * پارسر دستی JSON. برای اشیاء، برای آرایه‌ها، برای مقادیر ساده.
     */
    private static final class JsonParser {
        private final String s;
        private int pos;

        JsonParser(String s) {
            this.s = s;
            this.pos = 0;
        }

        Object parseValue() {
            skipWhitespace();
            if (pos >= s.length()) {
                return null;
            }
            char c = s.charAt(pos);
            if (c == '{')
                return parseObject();
            if (c == '[')
                return parseArray();
            if (c == '"')
                return parseString();
            if (c == 't' || c == 'f')
                return parseBoolean();
            if (c == 'n') {
                pos += 4;
                return null;
            } // "null"
            return parseNumber();
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            expect('{');
            skipWhitespace();
            if (peek() == '}') {
                pos++;
                return map;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                char next = s.charAt(pos);
                if (next == ',') {
                    pos++;
                    continue;
                }
                if (next == '}') {
                    pos++;
                    break;
                }
                throw new RuntimeException("Malformed JSON object near position " + pos);
            }
            return map;
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            expect('[');
            skipWhitespace();
            if (peek() == ']') {
                pos++;
                return list;
            }
            while (true) {
                Object value = parseValue();
                list.add(value);
                skipWhitespace();
                char next = s.charAt(pos);
                if (next == ',') {
                    pos++;
                    continue;
                }
                if (next == ']') {
                    pos++;
                    break;
                }
                throw new RuntimeException("Malformed JSON array near position " + pos);
            }
            return list;
        }

        private String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = s.charAt(pos++);
                if (c == '"')
                    break;
                if (c == '\\') {
                    char esc = s.charAt(pos++);
                    switch (esc) {
                        case '"':
                            sb.append('"');
                            break;
                        case '\\':
                            sb.append('\\');
                            break;
                        case '/':
                            sb.append('/');
                            break;
                        case 'n':
                            sb.append('\n');
                            break;
                        case 'r':
                            sb.append('\r');
                            break;
                        case 't':
                            sb.append('\t');
                            break;
                        case 'b':
                            sb.append('\b');
                            break;
                        case 'f':
                            sb.append('\f');
                            break;
                        case 'u':
                            String hex = s.substring(pos, pos + 4);
                            sb.append((char) Integer.parseInt(hex, 16));
                            pos += 4;
                            break;
                        default:
                            sb.append(esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private Boolean parseBoolean() {
            if (s.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            }
            if (s.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            throw new RuntimeException("Malformed boolean near position " + pos);
        }

        private Double parseNumber() {
            int start = pos;
            while (pos < s.length() && "-+0123456789.eE".indexOf(s.charAt(pos)) >= 0) {
                pos++;
            }
            return Double.parseDouble(s.substring(start, pos));
        }

        private void skipWhitespace() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
                pos++;
            }
        }

        private char peek() {
            skipWhitespace();
            return s.charAt(pos);
        }

        private void expect(char c) {
            skipWhitespace();
            if (s.charAt(pos) != c) {
                throw new RuntimeException("Expected '" + c + "' at position " + pos
                        + " but found '" + s.charAt(pos) + "'");
            }
            pos++;
        }
    }
}