package edu.itc.se.shop.order;

public final class OrderExceptions {
    private OrderExceptions() {}

    public static class NotFound extends RuntimeException {          // -> 404
        public NotFound(long id) { super("Order " + id + " not found"); }
    }
    public static class CustomerNotFound extends RuntimeException {  // -> 404
        public CustomerNotFound(long id) { super("Customer " + id + " not found"); }
    }
    public static class InvalidState extends RuntimeException {      // -> 409
        public InvalidState(String msg) { super(msg); }
    }
    public static class InvalidOrder extends RuntimeException {      // -> 400
        public InvalidOrder(String msg) { super(msg); }
    }
}
