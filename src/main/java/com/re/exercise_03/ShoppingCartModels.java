package com.re.exercise_03;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ShoppingCartModels {
    public static class Product {
        private String id;
        private double price;
        private int stockQuantity;

        public Product(String id, double price, int stockQuantity) {
            this.id = id; this.price = price; this.stockQuantity = stockQuantity;
        }
        public String getId() { return id; }
        public double getPrice() { return price; }
        public int getStockQuantity() { return stockQuantity; }
        public void setStockQuantity(int stock) { this.stockQuantity = stock; }
    }

    public static class CartItem {
        private Product product;
        private int quantity;

        public CartItem(Product product, int quantity) {
            this.product = product; this.quantity = quantity;
        }
        public Product getProduct() { return product; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }

    public static class ShoppingCart {
        private String userId;
        private List<CartItem> items = new ArrayList<>();

        public ShoppingCart(String userId) { this.userId = userId; }
        public String getUserId() { return userId; }
        public List<CartItem> getItems() { return items; }
    }

    public interface ProductRepository {
        Optional<Product> findById(String id);
    }

    public interface CartRepository {
        Optional<ShoppingCart> findByUserId(String userId);
        ShoppingCart save(ShoppingCart cart);
    }
}
