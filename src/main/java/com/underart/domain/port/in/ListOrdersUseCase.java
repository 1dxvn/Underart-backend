package com.underart.domain.port.in;

import com.underart.domain.model.Order;
import com.underart.domain.model.PageQuery;
import com.underart.domain.model.PageResult;
import java.util.UUID;

public interface ListOrdersUseCase {

    PageResult<Order> listPurchases(UUID userId, PageQuery page);

    PageResult<Order> listSales(UUID userId, PageQuery page);
}
