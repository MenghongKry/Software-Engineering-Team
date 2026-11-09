package edu.itc.se.shop_jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import edu.itc.se.shop_jpa.domain.Order;
import edu.itc.se.shop_jpa.repository.OrderRepository;
import edu.itc.se.shop_jpa.service.OrderService;
import edu.itc.se.shop_jpa.support.TestData;
import edu.itc.se.shop_jpa.web.dto.OrderDto;

/**
 * Task 5: why spring.jpa.open-in-view=false matters.
 * With OSIV off, the Hibernate session closes when the service transaction ends. Anything a
 * controller does with a lazy association afterwards fails. The fix is to map entity → DTO
 * inside the @Transactional service method (OrderService.view does that).
 */
@SpringBootTest
class OpenInViewTest {

    @Autowired Environment env;
    @Autowired OrderRepository orders;
    @Autowired OrderService orderService;
    @Autowired TestData data;

    @AfterEach
    void cleanUp() { data.clear(); }

    @Test
    void openInViewIsDisabled() {
        assertThat(env.getProperty("spring.jpa.open-in-view")).isEqualTo("false");
    }

    @Test
    void mappingOutsideTheTransactionFailsWithLazyInitializationException() {
        data.paidOrders(1, 2);
        Long id = orders.findByStatus(edu.itc.se.shop_jpa.domain.OrderStatus.PAID).getFirst().getId();

        // What a controller would get if it called the repository itself: the transaction of
        // findById() is already over, so the order is detached and its lines were never loaded.
        Order detached = orders.findById(id).orElseThrow();

        assertThatThrownBy(() -> OrderDto.from(detached))          // touches order.getLines()
                .isInstanceOf(LazyInitializationException.class)
                .hasMessageContaining("Order.lines");
    }

    @Test
    void mappingInsideTheServiceTransactionWorks() {
        data.paidOrders(1, 2);
        Long id = orders.findByStatus(edu.itc.se.shop_jpa.domain.OrderStatus.PAID).getFirst().getId();

        OrderDto dto = orderService.view(id);                     // mapping happens in the tx

        assertThat(dto.lines()).hasSize(2);
    }
}
