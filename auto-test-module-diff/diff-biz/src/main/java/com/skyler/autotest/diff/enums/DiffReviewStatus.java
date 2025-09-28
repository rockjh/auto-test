package com.skyler.autotest.diff.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 差异复核状态枚举。
 */
public enum DiffReviewStatus {

    /** 待复核。 */
    PENDING(0),

    /** 已通过。 */
    APPROVED(1),

    /** 已驳回。 */
    REJECTED(2);

    private final int code;

    DiffReviewStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /**
     * 根据编码解析枚举。
     *
     * @param code 状态编码
     * @return 对应枚举
     */
    public static Optional<DiffReviewStatus> fromCode(Integer code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(item -> item.code == code).findFirst();
    }
}
