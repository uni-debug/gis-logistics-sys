package com.gis.logistics.common.errorcode;

/**
 * 错误码注册表：5 位，前 3 位模块码，后 2 位序号。
 * 1xx 通用 / 2xx 订单 / 3xx 支付 / 4xx GIS / 5xx 权限
 */
public final class ErrorCode {

    public static final int SUCCESS = 0;

    // 1xx 通用
    public static final int GENERIC_BAD_REQUEST = 1001;
    public static final int GENERIC_NOT_FOUND = 1004;
    public static final int GENERIC_INTERNAL = 1500;

    // 2xx 订单/需求
    public static final int ORDER_NOT_FOUND = 2001;
    public static final int ORDER_INVALID_TRANSITION = 2002;
    public static final int ORDER_VERSION_CONFLICT = 2003;
    public static final int DEMAND_NOT_FOUND = 2101;
    public static final int DEMAND_ALREADY_QUOTED = 2102;

    // 3xx 支付
    public static final int PAYMENT_NOT_FOUND = 3001;
    public static final int PAYMENT_ALREADY_PAID = 3002;
    public static final int PAYMENT_SIGNATURE_INVALID = 3003;
    public static final int PAYMENT_AMOUNT_MISMATCH = 3004;
    public static final int PAYMENT_DUPLICATE_TXN = 3005;

    // 4xx GIS
    public static final int ROUTE_PLAN_FAILED = 4001;
    public static final int ROUTE_OSRM_UNAVAILABLE = 4002;
    public static final int TRACK_MATCH_FAILED = 4003;
    public static final int POINT_INVALID = 4004;

    // 5xx 权限
    public static final int AUTH_REQUIRED = 5001;
    public static final int AUTH_TOKEN_INVALID = 5002;
    public static final int AUTH_TOKEN_EXPIRED = 5003;
    public static final int ROLE_FORBIDDEN = 5004;
    public static final int PERMISSION_DENIED = 5005;
    public static final int ORDER_NOT_OWNED = 2004;

    private ErrorCode() {
    }
}
