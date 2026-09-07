package com.underart.domain.port.out;

public interface ActivityLogRepositoryPort {

    void log(String action, Long userId, String detail);
}
