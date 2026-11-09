package edu.itc.se.shop_jpa.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import edu.itc.se.shop_jpa.domain.Order;
import edu.itc.se.shop_jpa.domain.OrderStatus;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Window;
import org.springframework.data.domain.Pageable;
import edu.itc.se.shop_jpa.web.dto.OrderSummary;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // where customer.email = ? and status = ? order by placed_at desc
    List<Order> findByCustomerEmailAndStatusOrderByPlacedAtDesc(String email, OrderStatus status);

    // select count(*) ... where status = ?
    long countByStatus(OrderStatus status);

    // newest order of a customer; "First" adds "fetch first 1 rows only"
    Optional<Order> findFirstByCustomerIdOrderByPlacedAtDesc(Long customerId);

    @Query("""
        select distinct o from Order o
          join fetch o.lines l
          join fetch l.product
         where o.customer.email = :email
         order by o.placedAt desc
        """)
    List<Order> findWithLinesByCustomerEmail(@Param("email") String email);

    @Query("select count(l) from OrderLine l where l.order.status = :status")
    long countLinesByStatus(@Param("status") OrderStatus status);

    @Query(value = """
            select o.id as id, o.status as status, o.placedAt as placedAt,
                   c.name as customerName
              from Order o join o.customer c
             where o.status = :status
            """,
            countQuery = "select count(o) from Order o where o.status = :status")
    Page<OrderSummary> findByStatus(@Param("status") OrderStatus status, Pageable pageable);



    /** loads only the orders; every order.getLines() is a separate SELECT later (N+1). */
    List<Order> findByStatus(OrderStatus status);

    /** join fetch loads orders and their lines in one statement. */
    @Query("select distinct o from Order o join fetch o.lines where o.status = :s")
    List<Order> findByStatusWithLines(@Param("s") OrderStatus s);

    /** an entity graph tells Hibernate to fetch lines and their products in the same SELECT. */
    @EntityGraph(attributePaths = {"lines", "lines.product"})
    List<Order> findWithGraphByStatus(OrderStatus status);
    // ---------- Challenge: infinite-scroll order feed ----------

    /**
     * Offset version: Spring Data asks for size + 1 rows to know if there is a next slice and
     * never runs a count query. Page n still means "skip n * size rows" (OFFSET).
     */
    @Query("""
            select o.id as id, o.status as status, o.placedAt as placedAt, c.name as customerName
              from Order o join o.customer c
             order by o.placedAt desc, o.id desc
            """)
    Slice<OrderSummary> feedSlice(Pageable pageable);

    /**
     * Keyset version: the sort (placedAt desc, id desc) is in the method name and the position is
     * "the last (placedAt, id) I have seen". The id makes the order unique when two orders have the
     * same placedAt. The customer is fetched in the same query (entity graph) to avoid N+1.
     */
    @EntityGraph(attributePaths = "customer")
    Window<Order> findAllByOrderByPlacedAtDescIdDesc(ScrollPosition position, Limit limit);
}
