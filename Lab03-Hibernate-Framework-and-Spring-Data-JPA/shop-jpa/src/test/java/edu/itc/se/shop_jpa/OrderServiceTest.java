package edu.itc.se.shop_jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import edu.itc.se.shop_jpa.domain.Address;
import edu.itc.se.shop_jpa.domain.Customer;
import edu.itc.se.shop_jpa.domain.Order;
import edu.itc.se.shop_jpa.domain.OrderStatus;
import edu.itc.se.shop_jpa.repository.CustomerRepository;
import edu.itc.se.shop_jpa.repository.OrderRepository;
import edu.itc.se.shop_jpa.service.OrderFacade;
import edu.itc.se.shop_jpa.service.OrderService;
import jakarta.persistence.EntityManagerFactory;

@SpringBootTest
@ActiveProfiles("test")
class OrderServiceTest {

    @Autowired CustomerRepository customers;
    @Autowired OrderRepository orders;
    @Autowired OrderFacade orderFacade;
    @Autowired OrderService orderService;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void markPaidDirtyChecksExactlyOneOrderUpdate() {
        Order order = createOrder();
        Long orderId = order.getId();

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        orderService.markPaid(orderId);

        assertThat(orders.findById(orderId))
                .get()
                .extracting(Order::getStatus)
                .isEqualTo(OrderStatus.PAID);
        assertThat(statistics.getEntityUpdateCount()).isEqualTo(1);
    }

    @Test
    void readOnlyTransactionDoesNotFlushEntityMutation() {
        Order order = createOrder();
        Long orderId = order.getId();

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        orderService.wrongReadOnlyMarkPaid(orderId);

        assertThat(orders.findById(orderId))
                .get()
                .extracting(Order::getStatus)
                .isEqualTo(OrderStatus.NEW);
        assertThat(statistics.getEntityUpdateCount()).isZero();
    }

    @Test
    void viewMapsOrderToDto() {
        Order order = createOrder();

        assertThat(orderService.view(order.getId()).status()).isEqualTo(OrderStatus.NEW);
    }

    @Test
    void concurrentStatusChangesAllowOnlyOneCommit() throws Exception {
        Long orderId = createOrder().getId();
        long initialVersion = orders.findById(orderId).orElseThrow().getVersion();
        CountDownLatch bothLoaded = new CountDownLatch(2);
        CountDownLatch startChanges = new CountDownLatch(1);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Attempt> paid = executor.submit(() ->
                    changeStatusInTransaction(transaction, orderId, OrderStatus.PAID,
                            bothLoaded, startChanges));
            Future<Attempt> shipped = executor.submit(() ->
                    changeStatusInTransaction(transaction, orderId, OrderStatus.SHIPPED,
                            bothLoaded, startChanges));

            assertThat(bothLoaded.await(10, TimeUnit.SECONDS)).isTrue();
            startChanges.countDown();

            List<Attempt> attempts = List.of(
                    paid.get(20, TimeUnit.SECONDS),
                    shipped.get(20, TimeUnit.SECONDS));
            List<Attempt> failures = attempts.stream()
                    .filter(attempt -> attempt.failure != null)
                    .toList();

            assertThat(failures).hasSize(1);
            assertThat(failures.getFirst().failure)
                    .isInstanceOf(ObjectOptimisticLockingFailureException.class);

            Attempt winner = attempts.stream()
                    .filter(attempt -> attempt.failure == null)
                    .findFirst()
                    .orElseThrow();
            Order stored = orders.findById(orderId).orElseThrow();
            assertThat(stored.getStatus()).isEqualTo(winner.status);
            assertThat(stored.getVersion()).isEqualTo(initialVersion + 1);
        } finally {
            startChanges.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentStatusChangesWithRetryBothEventuallyCommit() throws Exception {
        Long orderId = createOrder().getId();
        long initialVersion = orders.findById(orderId).orElseThrow().getVersion();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> paid = executor.submit(() ->
                    changeStatusWithRetryWhenStarted(orderId, OrderStatus.PAID, ready, start));
            Future<?> shipped = executor.submit(() ->
                    changeStatusWithRetryWhenStarted(orderId, OrderStatus.SHIPPED, ready, start));

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            paid.get(20, TimeUnit.SECONDS);
            shipped.get(20, TimeUnit.SECONDS);

            Order stored = orders.findById(orderId).orElseThrow();
            assertThat(stored.getStatus()).isIn(OrderStatus.PAID, OrderStatus.SHIPPED);
            assertThat(stored.getVersion()).isEqualTo(initialVersion + 2);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private void changeStatusWithRetryWhenStarted(Long orderId, OrderStatus next,
                                                   CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for both updates to start");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to update order", e);
        }
        orderFacade.changeStatusWithRetry(orderId, next);
    }

    private Attempt changeStatusInTransaction(TransactionTemplate transaction, Long orderId,
                                               OrderStatus next, CountDownLatch bothLoaded,
                                               CountDownLatch startChanges) {
        try {
            transaction.executeWithoutResult(status -> {
                orders.findById(orderId).orElseThrow();
                bothLoaded.countDown();
                try {
                    if (!startChanges.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting for both transactions");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting to update order", e);
                }
                orderService.changeStatus(orderId, next);
            });
            return new Attempt(next, null);
        } catch (RuntimeException e) {
            return new Attempt(next, e);
        }
    }

    private Order createOrder() {
        Address address = new Address("1 Main St", "London", "10000");
        Customer customer = customers.saveAndFlush(new Customer(
                "Test Customer", "order-service-" + UUID.randomUUID() + "@test.com",
                address, address));
        return orders.saveAndFlush(new Order(customer));
    }

    private record Attempt(OrderStatus status, RuntimeException failure) {}
}
