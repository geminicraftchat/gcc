package cn.ningmo.geminicraftchat.constants;

/**
 * 代理类型枚举
 */
public enum ProxyType {
    SOCKS("SOCKS"),
    HTTP("HTTP");

    private final String value;

    ProxyType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ProxyType fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return HTTP;
        }
        
        for (ProxyType type : ProxyType.values()) {
            if (type.value.equalsIgnoreCase(text.trim())) {
                return type;
            }
        }
        return HTTP;
    }
} 