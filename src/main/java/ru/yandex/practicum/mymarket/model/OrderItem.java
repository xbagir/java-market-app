package ru.yandex.practicum.mymarket.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("order_items")
public class OrderItem {

    @Id
    private Long id;

    @Column("order_id")
    private long orderId;

    @Column("item_id")
    private long itemId;

    private int quantity;

    private long price;

    protected OrderItem() {
    }

    public OrderItem(long orderId, long itemId, int quantity, long price) {
        this.orderId = orderId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.price = price;
    }

    public Long getId() {
        return id;
    }

    public long getOrderId() {
        return orderId;
    }

    public long getItemId() {
        return itemId;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getPrice() {
        return price;
    }
}
