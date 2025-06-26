package com.vroute.solution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vroute.models.Order;

public class Solution {
    private final Map<String, Order> orders;
    private final List<Route> routes;

    // Constructor without environment for backward compatibility
    public Solution(Map<String, Order> orders, List<Route> routes) {
        this.orders = orders;
        this.routes = routes;
    }

    public Map<String, Order> getOrders() {
        return orders;
    }

    public List<Route> getRoutes() {
        return routes;
    }

    @Override
    public String toString() {
        return String.format("🧩 [Orders: %d, Routes: %d]",
                orders.size(),
                routes.size());
    }

    @Override
    public Solution clone() {
        return new Solution(orders, routes);
    }
}
