package com.underart.infrastructure.web.controller;

import com.underart.application.dto.ActivityLogResponse;
import com.underart.application.dto.ArtworkResponse;
import com.underart.application.dto.AuctionResponse;
import com.underart.application.dto.BidResponse;
import com.underart.application.dto.ChangePasswordRequest;
import com.underart.application.dto.OrderResponse;
import com.underart.application.dto.PageResponse;
import com.underart.application.dto.PaymentMethodRequest;
import com.underart.application.dto.SellerStatsResponse;
import com.underart.application.dto.UpdateProfileRequest;
import com.underart.application.dto.UserResponse;
import com.underart.domain.model.ArtworkSearchCriteria;
import com.underart.domain.model.AuctionSearchCriteria;
import com.underart.domain.model.PageQuery;
import com.underart.domain.port.in.ChangePasswordUseCase;
import com.underart.domain.port.in.FavoritesUseCase;
import com.underart.domain.port.in.FindUserUseCase;
import com.underart.domain.port.in.ListActivityUseCase;
import com.underart.domain.port.in.ListArtworksUseCase;
import com.underart.domain.port.in.ListAuctionsUseCase;
import com.underart.domain.port.in.ListBidsUseCase;
import com.underart.domain.port.in.ListOrdersUseCase;
import com.underart.domain.port.in.SellerStatsUseCase;
import com.underart.domain.port.in.UpdatePaymentMethodUseCase;
import com.underart.domain.port.in.UpdateUserProfileUseCase;
import com.underart.domain.port.in.UpdateUserProfileUseCase.UpdateProfileCommand;
import com.underart.infrastructure.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Mi cuenta", description = "Perfil, seguridad, compras, ventas y estadísticas del usuario autenticado")
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class CurrentUserController {

    private final CurrentUserProvider currentUser;
    private final FindUserUseCase findUserUseCase;
    private final UpdateUserProfileUseCase updateProfileUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final UpdatePaymentMethodUseCase updatePaymentMethodUseCase;
    private final ListBidsUseCase listBidsUseCase;
    private final ListOrdersUseCase listOrdersUseCase;
    private final ListArtworksUseCase listArtworksUseCase;
    private final ListAuctionsUseCase listAuctionsUseCase;
    private final ListActivityUseCase listActivityUseCase;
    private final FavoritesUseCase favoritesUseCase;
    private final SellerStatsUseCase sellerStatsUseCase;

    @GetMapping
    @Operation(summary = "Mi perfil completo")
    public ResponseEntity<UserResponse> me() {
        return ResponseEntity.ok(UserResponse.from(findUserUseCase.findById(currentUser.currentUserId())));
    }

    @PutMapping
    @Operation(summary = "Actualizar nombre, ciudad y dirección")
    public ResponseEntity<UserResponse> update(@Valid @RequestBody UpdateProfileRequest req) {
        return ResponseEntity.ok(UserResponse.from(updateProfileUseCase.update(new UpdateProfileCommand(
                currentUser.currentUserId(), req.username(), req.city(), req.shippingAddress()))));
    }

    @PutMapping("/password")
    @Operation(summary = "Cambiar la contraseña (cierra todas las sesiones)")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        changePasswordUseCase.change(currentUser.currentUserId(), req.currentPassword(), req.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/payment-method")
    @Operation(summary = "Registrar el método de pago (id pm_... de Stripe)")
    public ResponseEntity<UserResponse> updatePaymentMethod(@Valid @RequestBody PaymentMethodRequest req) {
        return ResponseEntity.ok(UserResponse.from(
                updatePaymentMethodUseCase.updatePaymentToken(currentUser.currentUserId(), req.paymentToken())));
    }

    @GetMapping("/bids")
    @Operation(summary = "Mis pujas")
    public ResponseEntity<PageResponse<BidResponse>> bids(@RequestParam(required = false) Integer page,
                                                          @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(PageResponse.from(
                listBidsUseCase.listByBuyer(currentUser.currentUserId(), PageQuery.of(page, size)), BidResponse::from));
    }

    @GetMapping("/orders")
    @Operation(summary = "Mis compras")
    public ResponseEntity<PageResponse<OrderResponse>> purchases(@RequestParam(required = false) Integer page,
                                                                 @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(PageResponse.from(listOrdersUseCase.listPurchases(
                currentUser.currentUserId(), PageQuery.of(page, size)), OrderResponse::from));
    }

    @GetMapping("/sales")
    @Operation(summary = "Mis ventas")
    public ResponseEntity<PageResponse<OrderResponse>> sales(@RequestParam(required = false) Integer page,
                                                             @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(PageResponse.from(listOrdersUseCase.listSales(
                currentUser.currentUserId(), PageQuery.of(page, size)), OrderResponse::from));
    }

    @GetMapping("/artworks")
    @Operation(summary = "Mis obras")
    public ResponseEntity<PageResponse<ArtworkResponse>> artworks(@RequestParam(required = false) Integer page,
                                                                  @RequestParam(required = false) Integer size) {
        UUID me = currentUser.currentUserId();
        return ResponseEntity.ok(PageResponse.from(listArtworksUseCase.list(
                ArtworkSearchCriteria.byArtist(me), PageQuery.of(page, size)), ArtworkResponse::from));
    }

    @GetMapping("/auctions")
    @Operation(summary = "Subastas de mis obras")
    public ResponseEntity<PageResponse<AuctionResponse>> auctions(@RequestParam(required = false) Integer page,
                                                                  @RequestParam(required = false) Integer size) {
        UUID me = currentUser.currentUserId();
        return ResponseEntity.ok(PageResponse.from(listAuctionsUseCase.list(
                AuctionSearchCriteria.byArtist(me), PageQuery.of(page, size)), AuctionResponse::from));
    }

    @GetMapping("/stats")
    @Operation(summary = "Estadísticas de vendedor y comprador")
    public ResponseEntity<SellerStatsResponse> stats() {
        return ResponseEntity.ok(SellerStatsResponse.from(sellerStatsUseCase.statsFor(currentUser.currentUserId())));
    }

    @GetMapping("/favorites")
    @Operation(summary = "Mis obras favoritas")
    public ResponseEntity<PageResponse<ArtworkResponse>> favorites(@RequestParam(required = false) Integer page,
                                                                   @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(PageResponse.from(favoritesUseCase.list(currentUser.currentUserId(),
                PageQuery.of(page, size)), ArtworkResponse::from));
    }

    @GetMapping("/activity")
    @Operation(summary = "Mi historial de actividad")
    public ResponseEntity<PageResponse<ActivityLogResponse>> activity(@RequestParam(required = false) Integer page,
                                                                      @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(PageResponse.from(listActivityUseCase.listForUser(
                currentUser.currentUserId(), PageQuery.of(page, size)), ActivityLogResponse::from));
    }
}
