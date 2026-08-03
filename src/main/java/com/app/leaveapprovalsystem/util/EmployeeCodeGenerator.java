package com.app.leaveapprovalsystem.util;

import java.util.concurrent.atomic.AtomicLong;

public final class EmployeeCodeGenerator {

    private static final AtomicLong COUNTER = new AtomicLong(1000);

    private EmployeeCodeGenerator() {}

    public static String generate(String department) {
        String prefix = (department != null && department.length() >= 2)
                ? department.substring(0, 2).toUpperCase()
                : "EM";
        long seq = COUNTER.getAndIncrement();
        return String.format("%s%04d", prefix, seq);
    }
}
