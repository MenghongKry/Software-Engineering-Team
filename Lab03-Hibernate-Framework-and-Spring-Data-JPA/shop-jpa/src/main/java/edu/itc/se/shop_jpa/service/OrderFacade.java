package edu.itc.se.shop_jpa.service;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import edu.itc.se.shop_jpa.domain.OrderStatus;

@Service
public class OrderFacade {

    private static final int MAX_ATTEMPTS = 3;

    private final OrderService orders;

    public OrderFacade(OrderService orders) {
        this.orders = orders;
    }

    public void changeStatusWithRetry(Long id, OrderStatus next) {
        for (int attempt = 1; ; attempt++) {
            try {
                orders.changeStatus(id, next);
                return;
            } catch (ObjectOptimisticLockingFailureException e) {
                if (attempt == MAX_ATTEMPTS) {
                    throw e;
                }
            }
        }
    }
}
