package io.theholygrail.jsbridge;

public class JsValueUtil {
    public static String parseQuotes(String stringifiedString) {
        return stringifiedString.replaceAll("^\"|\"$", "");
    }
}
