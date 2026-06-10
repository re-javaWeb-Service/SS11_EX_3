package com.re.exercise_03;


import com.re.exercise_03.ShoppingCartModels.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ShoppingCartServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private ShoppingCartService shoppingCartService; // Đối tượng service cần kiểm thử

    @Captor
    private ArgumentCaptor<ShoppingCart> cartCaptor; // Dùng để bắt đối tượng ShoppingCart khi save

    private Product sampleProduct;
    private ShoppingCart sampleCart;

    @BeforeEach
    void setUp() {
        sampleProduct = new Product("PROD-100", 20000.0, 10);
        sampleCart = new ShoppingCart("USER-01");
    }

    // ==========================================
    // CHỨC NĂNG 1: addProductToCart
    // ==========================================

    @Test
    void addProductToCart_HappyPath_CartExists() {
        sampleCart.getItems().add(new CartItem(sampleProduct, 2));

        when(cartRepository.findByUserId("USER-01")).thenReturn(Optional.of(sampleCart));
        when(productRepository.findById("PROD-100")).thenReturn(Optional.of(sampleProduct));

        shoppingCartService.addProductToCart("USER-01", "PROD-100", 3);

        verify(cartRepository).save(cartCaptor.capture());
        ShoppingCart savedCart = cartCaptor.getValue();

        // Xác minh tổng số lượng sau khi cộng dồn (2 + 3 = 5)
        assertThat(savedCart.getItems().get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    void addProductToCart_HappyPath_NewCartCreated() {
        // Giả lập người dùng chưa có giỏ hàng nào trong DB
        when(cartRepository.findByUserId("USER-02")).thenReturn(Optional.empty());
        when(productRepository.findById("PROD-100")).thenReturn(Optional.of(sampleProduct));

        shoppingCartService.addProductToCart("USER-02", "PROD-100", 2);

        verify(cartRepository).save(cartCaptor.capture());
        ShoppingCart savedCart = cartCaptor.getValue();

        // Đảm bảo hệ thống tự tạo giỏ mới cho USER-02 và lưu vào DB
        assertThat(savedCart.getUserId()).isEqualTo("USER-02");
        assertThat(savedCart.getItems()).hasSize(1);
    }

    @Test
    void addProductToCart_UnhappyPath_InsufficientStock() {
        when(cartRepository.findByUserId("USER-01")).thenReturn(Optional.of(sampleCart));
        when(productRepository.findById("PROD-100")).thenReturn(Optional.of(sampleProduct)); // Kho chỉ có 10

        // Thêm hẳn 15 mặt hàng vượt tồn kho -> Phải vấp ngoại lệ
        assertThatThrownBy(() -> shoppingCartService.addProductToCart("USER-01", "PROD-100", 15))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Not enough stock available");

        verify(cartRepository, never()).save(any());
    }

    // ==========================================
    // CHỨC NĂNG 2: updateProductQuantity
    // ==========================================

    @Test
    void updateProductQuantity_HappyPath() {
        sampleCart.getItems().add(new CartItem(sampleProduct, 2));

        when(cartRepository.findByUserId("USER-01")).thenReturn(Optional.of(sampleCart));
        when(productRepository.findById("PROD-100")).thenReturn(Optional.of(sampleProduct));

        shoppingCartService.updateProductQuantity("USER-01", "PROD-100", 6);

        verify(cartRepository).save(cartCaptor.capture());
        assertThat(cartCaptor.getValue().getItems().get(0).getQuantity()).isEqualTo(6);
    }

    @Test
    void updateProductQuantity_UnhappyPath_QuantityLessThanOrEqualZero() {
        sampleCart.getItems().add(new CartItem(sampleProduct, 2));
        when(cartRepository.findByUserId("USER-01")).thenReturn(Optional.of(sampleCart));

        // Cố tình cập nhật số lượng thành 0 hoặc âm -> Bị chặn từ đầu
        assertThatThrownBy(() -> shoppingCartService.updateProductQuantity("USER-01", "PROD-100", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity must be greater than zero");

        verify(cartRepository, never()).save(any());
    }

    /**
     * KIỂM THỬ KỊCH BẢN PHẦN 1: Tồn kho giảm đột ngột do người dùng khác mua
     */
    @Test
    void updateProductQuantity_ConcurrentStockReduction_ShouldThrowException() {
        sampleCart.getItems().add(new CartItem(sampleProduct, 5)); // Ban đầu A lấy 5 (Kho gốc là 5)

        // Mô phỏng: Người dùng B đã mua hết 3 cái -> Tồn kho thực tế lúc này chỉ còn 2 cái!
        Product realTimeProductInDb = new Product("PROD-100", 20000.0, 2);

        when(cartRepository.findByUserId("USER-01")).thenReturn(Optional.of(sampleCart));
        when(productRepository.findById("PROD-100")).thenReturn(Optional.of(realTimeProductInDb));

        // Người dùng A cố tăng số lượng lên 7 -> Vượt quá số lượng tồn kho hiện tại (2) -> Throw Exception!
        assertThatThrownBy(() -> shoppingCartService.updateProductQuantity("USER-01", "PROD-100", 7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Not enough stock available");

        verify(cartRepository, never()).save(any());
    }

    // ==========================================
    // CHỨC NĂNG 3: removeProductFromCart
    // ==========================================

    @Test
    void removeProductFromCart_HappyPath() {
        sampleCart.getItems().add(new CartItem(sampleProduct, 3));

        when(cartRepository.findByUserId("USER-01")).thenReturn(Optional.of(sampleCart));

        shoppingCartService.removeProductFromCart("USER-01", "PROD-100");

        verify(cartRepository).save(cartCaptor.capture());
        // Sau khi xóa, danh sách sản phẩm trong giỏ hàng phải trống (size = 0)
        assertThat(cartCaptor.getValue().getItems()).isEmpty();
    }

    /**
     * KIỂM THỬ: Xóa sản phẩm đã bị Admin xóa hoàn toàn khỏi hệ thống (Database)
     */
    @Test
    void removeProductFromCart_ProductDeletedFromSystem_ShouldHandleGracefully() {
        sampleCart.getItems().add(new CartItem(sampleProduct, 3));

        when(cartRepository.findByUserId("USER-01")).thenReturn(Optional.of(sampleCart));
        // Sản phẩm không còn tồn tại trong kho hệ thống nữa (trả về Empty)
        when(productRepository.findById("PROD-100")).thenReturn(Optional.empty());

        // Nghiệp vụ yêu cầu việc xóa khỏi giỏ vẫn phải diễn ra bình thường, mượt mà
        shoppingCartService.removeProductFromCart("USER-01", "PROD-100");

        verify(cartRepository).save(cartCaptor.capture());
        assertThat(cartCaptor.getValue().getItems()).isEmpty();
    }
}
