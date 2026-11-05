package com.underart.application.usecase;

import com.underart.domain.model.ActivityLog;
import com.underart.domain.model.PageQuery;
import com.underart.domain.model.PageResult;
import com.underart.domain.port.in.ListActivityUseCase;
import com.underart.domain.port.out.ActivityLogRepositoryPort;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListActivityService implements ListActivityUseCase {

    private final ActivityLogRepositoryPort activityLogRepository;

    @Override
    public PageResult<ActivityLog> listForUser(UUID userId, PageQuery page) {
        return activityLogRepository.findByUserId(userId, page);
    }
}
