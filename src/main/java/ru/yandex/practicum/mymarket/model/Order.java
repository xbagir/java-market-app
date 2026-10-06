package ru.yandex.practicum.mymarket.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("shop_orders")
public class Order {

    @Id
    private Long id;

    @Column("total_sum")
    private long totalSum;

    public Order() {
    }

    public Long getId() {
        return id;
    }

    public long getTotalSum() {
        return totalSum;
    }

    public void setTotalSum(long totalSum) {
        this.totalSum = totalSum;
    }
}
