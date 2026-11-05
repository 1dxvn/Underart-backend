package com.underart.domain.port.in;

import com.underart.domain.model.ActivityLog;
import com.underart.domain.model.PageQuery;
import com.underart.domain.model.PageResult;
import java.util.UUID;

public interface ListActivityUseCase {

    PageResult<ActivityLog> listForUser(UUID userId, PageQuery page);
}
