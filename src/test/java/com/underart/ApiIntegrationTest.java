package com.underart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.jayway.jsonpath.JsonPath;
import com.underart.domain.model.AiResult;
import com.underart.domain.port.in.CloseExpiredAuctionsUseCase;
import com.underart.domain.port.out.MediaStoragePort;
import com.underart.domain.port.out.MediaStoragePort.PresignedUpload;
import com.underart.infrastructure.ai.strategy.GroqAiAdapter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = "auctions.closing.enabled=false")
class ApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    @Autowired
    private MockMvc mvc;
    @Autowired
    private CloseExpiredAuctionsUseCase closer;
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;
    @MockBean
    private GroqAiAdapter groq;
    @MockBean
    private MediaStoragePort storage;

    @MockBean
    private com.underart.domain.port.out.VerificationMailPort verificationMail;

    private final Map<String, String> verificationCodes = new java.util.HashMap<>();

    @BeforeEach
    void stubExternalServices() {
        org.mockito.Mockito.doAnswer(invocation -> {
            verificationCodes.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(verificationMail).sendCode(anyString(), anyString());
        when(groq.getProviderName()).thenReturn("groq");
        when(groq.valuateArtwork(any(), any(), any()))
                .thenReturn(new AiResult("groq", new BigDecimal("300.00"), List.of("landscape"), "{}"));
        when(groq.generateTags(any(), any())).thenReturn(List.of("landscape", "watercolor"));
        when(storage.publicUrl(anyString())).thenAnswer(i -> "https://cdn.test/" + i.getArgument(0));
        when(storage.presignUpload(anyString(), anyString(), anyLong())).thenReturn(new PresignedUpload(
                "https://r2.test/upload", Instant.now().plusSeconds(900), Map.of("Content-Type", "video/mp4")));
        when(storage.objectSize(anyString())).thenReturn(Optional.of(100L * 1024 * 1024));
    }

    @Test
    void fullMarketplaceFlow() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String artistToken = registerAndLogin("artist-" + unique + "@test.com");
        String buyerToken = registerAndLogin("buyer-" + unique + "@test.com");
        String thirdToken = registerAndLogin("third-" + unique + "@test.com");
        String artistId = read(call(get("/api/v1/users/me"), artistToken, null, 200), "$.id");

        call(get("/api/v1/users/me"), null, null, 401);
        String publicProfile = body(call(get("/api/v1/users/" + artistId), buyerToken, null, 200));
        assertThat(publicProfile).doesNotContain("shippingAddress").doesNotContain("@test.com");
        assertThat(JsonPath.<Integer>read(publicProfile, "$.reviewsCount")).isZero();
        assertThat(JsonPath.<Object>read(publicProfile, "$.averageRating")).isNull();

        String profile = "{\"username\":\"u\",\"bio\":\"Painter and printmaker\"}";
        call(put("/api/v1/users/me"), artistToken, profile, 200);
        var avatar = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1, 2, 3});
        var updateAvatar = multipart(org.springframework.http.HttpMethod.PUT, "/api/v1/users/me/avatar").file(avatar);
        call(updateAvatar, artistToken, null, 200);
        var updatedProfile = call(get("/api/v1/users/" + artistId), null, null, 200);
        assertThat(this.<String>read(updatedProfile, "$.bio")).isEqualTo("Painter and printmaker");
        assertThat(this.<String>read(updatedProfile, "$.avatarUrl")).startsWith("https://cdn.test/avatars/");
        assertThat(body(updatedProfile)).doesNotContain("shippingAddress", "@test.com");
        var configuration = call(get("/api/v1/payments/config"), buyerToken, null, 200);
        assertThat(this.<String>read(configuration, "$.provider")).isEqualTo("simulated");

        when(groq.suggestArtwork(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new com.underart.domain.model.ArtworkDraft("groq", "Paisaje en óleo", new BigDecimal("25500")));
        var advice = call(post("/api/v1/ai/artwork-advice"), artistToken,
                "{\"title\":\"Dawn\",\"description\":\"Landscape\",\"technique\":\"Oil\",\"dimensions\":\"30x40\",\"weightKg\":1.5,\"currency\":\"COP\"}", 200);
        assertThat(this.<String>read(advice, "$.description")).isEqualTo("Paisaje en óleo");
        assertThat(this.<Number>read(advice, "$.suggestedPrice").intValue()).isEqualTo(26000);
        assertThat(this.<Number>read(advice, "$.shippingEstimate").intValue()).isEqualTo(18000);

        String artworkId = createArtwork(artistToken, "Dawn by the lake");
        assertThat(this.<Double>read(call(get("/api/v1/artworks/" + artworkId), buyerToken, null, 200),
                "$.artwork.suggestedPrice")).isEqualTo(300.0);
        assertThat(total(call(get("/api/v1/artworks?q=dawn&artistId=" + artistId), buyerToken, null, 200))).isEqualTo(1);
        assertThat(total(call(get("/api/v1/artworks?tag=LANDSCAPE&artistId=" + artistId), buyerToken, null, 200))).isEqualTo(1);
        assertThat(total(call(get("/api/v1/artworks?technique=bronze&artistId=" + artistId), buyerToken, null, 200))).isZero();
        String update = "{\"title\":\"Dawn\",\"technique\":\"Watercolor\"}";
        call(put("/api/v1/artworks/" + artworkId), buyerToken, update, 403);
        assertThat(this.<String>read(call(put("/api/v1/artworks/" + artworkId), artistToken, update, 200), "$.title"))
                .isEqualTo("Dawn");

        mediaFlow(artworkId, artistToken, buyerToken);
        publicCatalogFlow(artworkId, artistId);
        favoritesFlow(artworkId, buyerToken, thirdToken);
        biddingFlow(artworkId, artistToken, buyerToken, thirdToken);
        String orderId = closingFlow(artistToken, buyerToken, thirdToken);
        paymentFlow(orderId, buyerToken);
        fulfillmentFlow(orderId, artistToken, buyerToken);
        reviewFlow(orderId, artistId, artistToken, buyerToken);
        buyNowFlow(artistToken, thirdToken);
        searchFlow(artistId);
        statsFlow(artistToken, buyerToken);
        notificationsFlow(buyerToken, artistToken);
        accountFlow(buyerToken, "buyer-" + unique + "@test.com");
        sessionFlow("third-" + unique + "@test.com");
        lockoutFlow("locked-" + unique + "@test.com");

        call(delete("/api/v1/artworks/" + artworkId), artistToken, null, 400);
        String disposable = createArtwork(artistToken, "Sketch");
        call(delete("/api/v1/artworks/" + disposable), artistToken, null, 204);
        call(get("/api/v1/artworks/" + disposable), artistToken, null, 404);

        assertThat(this.<String>read(call(post("/api/v1/ai/valuations"), artistToken,
                "{\"title\":\"Portrait\",\"technique\":\"Oil\"}", 200), "$.provider")).isEqualTo("groq");
        assertThat(this.<String>read(call(get("/actuator/health"), null, null, 200), "$.status")).isEqualTo("UP");
        assertThat(this.<String>read(call(get("/actuator/info"), null, null, 200), "$.app.name"))
                .isEqualTo("Underart Backend");
        String openApi = body(call(get("/v3/api-docs"), null, null, 200));
        assertThat(JsonPath.<java.util.List<String>>read(openApi, "$.tags[*].name"))
                .contains("Authentication", "Artworks", "Auctions", "Bids", "Orders", "Notifications");
        assertThat(JsonPath.<String>read(openApi, "$.components.securitySchemes.bearerAuth.scheme")).isEqualTo("bearer");
    }

    @Autowired
    private com.underart.application.usecase.PaymentTransactions payments;

    @Test
    void concurrentAuctionCreationAllowsOnlyOneActiveAuction() throws Exception {
        String artist = registerAndLogin(UUID.randomUUID() + "@test.com");
        String artwork = createArtwork(artist, "Unique artwork");
        String payload = auctionPayload(artwork);
        List<Integer> statuses = together(() -> requestStatus(post("/api/v1/auctions"), artist, payload),
                () -> requestStatus(post("/api/v1/auctions"), artist, payload));
        assertThat(statuses).containsExactlyInAnyOrder(201, 400);
        assertThat(jdbc.queryForObject("select count(*) from auctions where artwork_id = ? and status = 'ACTIVE'",
                Long.class, UUID.fromString(artwork))).isEqualTo(1);
        call(put("/api/v1/artworks/" + artwork), artist, "{\"title\":\"Changed\"}", 400);
        upload("/api/v1/artworks/" + artwork + "/media", artist, "image/png", 400);
    }

    @Test
    void concurrentPaymentsAndShippingPreserveOrderStateAndSnapshots() throws Exception {
        String artist = registerAndLogin(UUID.randomUUID() + "@test.com");
        String buyer = registerAndLogin(UUID.randomUUID() + "@test.com");
        String artwork = createArtwork(artist, "Original artwork");
        String payload = auctionPayload(artwork);
        String auction = read(call(post("/api/v1/auctions"), artist, payload, 201), "$.id");
        String order = read(call(post("/api/v1/auctions/" + auction + "/buy-now"), buyer, null, 201), "$.id");
        call(post("/api/v1/auctions"), artist, payload, 400);
        call(put("/api/v1/users/me/payment-method"), buyer, "{\"paymentToken\":\"tok_visa\"}", 200);
        List<Integer> paid = together(() -> requestStatus(post("/api/v1/orders/" + order + "/pay"), buyer, null),
                () -> requestStatus(post("/api/v1/orders/" + order + "/pay"), buyer, null));
        assertThat(paid).contains(200).allMatch(status -> status == 200 || status == 400);
        String details = body(call(get("/api/v1/orders/" + order), artist, null, 200));
        assertThat(JsonPath.<String>read(details, "$.snapshot.title")).isEqualTo("Original artwork");
        assertThat(JsonPath.<String>read(details, "$.snapshot.shippingAddress")).isEqualTo("1 Main Street");
        assertThat(JsonPath.<String>read(details, "$.snapshot.currency")).isEqualTo("usd");
        assertThat(JsonPath.<String>read(details, "$.transactionId")).startsWith("sim_");
        assertThat(details).doesNotContain("tok_visa", "paymentReference", "paymentMethod");
        call(put("/api/v1/users/me"), buyer, "{\"username\":\"Buyer\",\"shippingAddress\":\"New address\"}", 200);
        assertThat(this.<String>read(call(get("/api/v1/orders/" + order), buyer, null, 200),
                "$.snapshot.shippingAddress")).isEqualTo("1 Main Street");
        String shipping = "{\"carrier\":\"Carrier\",\"trackingNumber\":\"TRACK-1\"}";
        assertThat(together(() -> requestStatus(post("/api/v1/orders/" + order + "/ship"), artist, shipping),
                () -> requestStatus(post("/api/v1/orders/" + order + "/ship"), artist, shipping)))
                .containsExactlyInAnyOrder(200, 400);
        assertThat(together(() -> requestStatus(post("/api/v1/orders/" + order + "/confirm-delivery"), buyer, null),
                () -> requestStatus(post("/api/v1/orders/" + order + "/confirm-delivery"), buyer, null)))
                .containsExactlyInAnyOrder(200, 400);
        call(post("/api/v1/auctions"), artist, payload, 400);
    }

    @Test
    void unpaidCheckoutsExpireWithoutClosingTheAuction() throws Exception {
        String artist = registerAndLogin(UUID.randomUUID() + "@test.com");
        String buyer = registerAndLogin(UUID.randomUUID() + "@test.com");
        String artwork = createArtwork(artist, "Reserved artwork");
        String payload = auctionPayload(artwork);
        String auction = read(call(post("/api/v1/auctions"), artist, payload, 201), "$.id");
        UUID order = UUID.fromString(read(call(post("/api/v1/auctions/" + auction + "/buy-now"), buyer, null, 201), "$.id"));
        jdbc.update("update orders set payment_due_at = ? where id = ?",
                java.sql.Timestamp.from(Instant.now().minusSeconds(1)), order);
        payments.expire(order);
        assertThat(this.<String>read(call(get("/api/v1/orders/" + order), buyer, null, 200), "$.paymentStatus"))
                .isEqualTo("EXPIRED");
        call(post("/api/v1/orders/" + order + "/pay"), buyer, null, 400);
        assertThat(this.<String>read(call(get("/api/v1/auctions/" + auction), null, null, 200),
                "$.auction.status")).isEqualTo("ACTIVE");
        call(post("/api/v1/auctions/" + auction + "/buy-now"), buyer, null, 201);
    }

    @Test
    void biddingAndBuyingAtTheSameTimeProduceOneOrder() throws Exception {
        String artist = registerAndLogin(UUID.randomUUID() + "@test.com");
        String buyer = registerAndLogin(UUID.randomUUID() + "@test.com");
        String bidder = registerAndLogin(UUID.randomUUID() + "@test.com");
        String artwork = createArtwork(artist, "Concurrent artwork");
        String auction = read(call(post("/api/v1/auctions"), artist, auctionPayload(artwork), 201), "$.id");
        List<Integer> statuses = together(
                () -> requestStatus(post("/api/v1/auctions/" + auction + "/bids"), bidder, "{\"amount\":150}"),
                () -> requestStatus(post("/api/v1/auctions/" + auction + "/buy-now"), buyer, null));
        assertThat(statuses.get(1)).isEqualTo(201);
        assertThat(statuses.get(0)).isIn(201, 400);
        assertThat(jdbc.queryForObject("select count(*) from orders where auction_id = ?", Long.class,
                UUID.fromString(auction))).isEqualTo(1);
        assertThat(closer.close(UUID.fromString(auction))).isEmpty();
    }

    @Test
    void artworkCreationSurvivesUnavailableAi() throws Exception {
        String artist = registerAndLogin(UUID.randomUUID() + "@test.com");
        when(groq.valuateArtwork(any(), any(), any()))
                .thenThrow(new com.underart.domain.exception.AiProviderException("Unavailable"));
        String artwork = createArtwork(artist, "Manual artwork");
        assertThat(this.<Object>read(call(get("/api/v1/artworks/" + artwork), null, null, 200),
                "$.artwork.suggestedPrice")).isNull();
    }

    private String auctionPayload(String artwork) {
        return "{\"artworkId\":\"" + artwork + "\",\"type\":\"IMMEDIATE\",\"basePrice\":100,"
                + "\"immediatePurchasePrice\":500,\"endDate\":\"" + Instant.now().plusSeconds(3600) + "\"}";
    }

    private int requestStatus(MockHttpServletRequestBuilder request, String token, String payload) throws Exception {
        request.header("Authorization", "Bearer " + token);
        if (payload != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(payload);
        }
        return mvc.perform(request).andReturn().getResponse().getStatus();
    }

    private List<Integer> together(java.util.concurrent.Callable<Integer> first,
                                  java.util.concurrent.Callable<Integer> second) throws Exception {
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        try {
            var a = executor.submit(() -> { start.await(); return first.call(); });
            var b = executor.submit(() -> { start.await(); return second.call(); });
            start.countDown();
            return List.of(a.get(30, java.util.concurrent.TimeUnit.SECONDS), b.get(30, java.util.concurrent.TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private void mediaFlow(String artworkId, String artistToken, String buyerToken) throws Exception {
        String base = "/api/v1/artworks/" + artworkId + "/media";
        String first = read(upload(base, artistToken, "image/png", 201), "$.id");
        String second = read(upload(base, artistToken, "image/jpeg", 201), "$.id");
        upload(base, buyerToken, "image/png", 403);
        upload(base, artistToken, "application/pdf", 400);
        assertThat(this.<Integer>read(call(get(base), buyerToken, null, 200), "$.length()")).isEqualTo(2);

        String reordered = body(call(put(base + "/order"), artistToken,
                "{\"mediaIds\":[\"" + second + "\",\"" + first + "\"]}", 200));
        assertThat(JsonPath.<String>read(reordered, "$[0].id")).isEqualTo(second);
        assertThat(JsonPath.<Boolean>read(reordered, "$[0].cover")).isTrue();

        call(delete(base + "/" + second), artistToken, null, 204);
        assertThat(this.<Integer>read(call(get(base), artistToken, null, 200), "$[0].position")).isZero();

        String ticket = body(call(post(base + "/upload-url"), artistToken,
                "{\"contentType\":\"video/mp4\",\"sizeBytes\":104857600}", 200));
        String mediaId = JsonPath.read(ticket, "$.mediaId");
        assertThat(JsonPath.<String>read(ticket, "$.method")).isEqualTo("PUT");
        assertThat(this.<String>read(call(post(base + "/" + mediaId + "/confirm"), artistToken,
                "{\"contentType\":\"video/mp4\"}", 201), "$.kind")).isEqualTo("VIDEO");
    }

    private void publicCatalogFlow(String artworkId, String artistId) throws Exception {
        String detail = body(call(get("/api/v1/artworks/" + artworkId), null, null, 200));
        assertThat(JsonPath.<String>read(detail, "$.artwork.coverUrl")).isEqualTo(JsonPath.read(detail, "$.media[0].url"));
        assertThat(JsonPath.<Integer>read(detail, "$.media.length()")).isEqualTo(2);
        assertThat(this.<String>read(call(get("/api/v1/artworks?artistId=" + artistId), null, null, 200),
                "$.items[0].coverUrl")).startsWith("https://cdn.test/");
        call(get("/api/v1/artworks/" + artworkId + "/media"), null, null, 200);
        assertThat(total(call(get("/api/v1/artworks?artistId=" + artistId), null, null, 200))).isPositive();
        call(get("/api/v1/auctions"), null, null, 200);
        assertThat(body(call(get("/api/v1/users/" + artistId), null, null, 200))).doesNotContain("@test.com");
        call(post("/api/v1/artworks"), null, "{\"title\":\"x\"}", 401);
        call(put("/api/v1/artworks/" + artworkId), null, "{\"title\":\"x\"}", 401);
        call(post("/api/v1/artworks/" + artworkId + "/media/upload-url"), null,
                "{\"contentType\":\"image/png\",\"sizeBytes\":10}", 401);
    }

    private void biddingFlow(String artworkId, String artistToken, String buyerToken, String thirdToken)
            throws Exception {
        String auction = "{\"artworkId\":\"" + artworkId + "\",\"type\":\"SIMPLE\",\"basePrice\":100,"
                + "\"endDate\":\"" + Instant.now().plusSeconds(3600) + "\"}";
        call(post("/api/v1/auctions"), buyerToken, auction, 403);
        String auctionId = read(call(post("/api/v1/auctions"), artistToken, auction, 201), "$.id");
        call(post("/api/v1/auctions"), artistToken, auction, 400);

        String bids = "/api/v1/auctions/" + auctionId + "/bids";
        call(post(bids), artistToken, "{\"amount\":150}", 400);
        call(post(bids), buyerToken, "{\"amount\":150}", 201);
        call(post(bids), thirdToken, "{\"amount\":140}", 400);
        call(post(bids), thirdToken, "{\"amount\":-5}", 400);
        call(post(bids), thirdToken, "{\"amount\":200}", 201);

        String inbox = body(call(get("/api/v1/users/me/notifications?unreadOnly=true"), buyerToken, null, 200));
        assertThat(JsonPath.<String>read(inbox, "$.items[0].type")).isEqualTo("OUTBID");
        assertThat(JsonPath.<String>read(inbox, "$.items[0].data.auctionId")).isEqualTo(auctionId);
        int unread = read(call(get("/api/v1/users/me/notifications/unread-count"), buyerToken, null, 200), "$.count");
        call(post("/api/v1/users/me/notifications/" + JsonPath.read(inbox, "$.items[0].id") + "/read"),
                buyerToken, null, 204);
        call(post("/api/v1/users/me/notifications/" + JsonPath.read(inbox, "$.items[0].id") + "/read"),
                thirdToken, null, 404);
        assertThat(this.<Integer>read(call(get("/api/v1/users/me/notifications/unread-count"), buyerToken, null, 200),
                "$.count")).isEqualTo(unread - 1);

        String detail = body(call(get("/api/v1/auctions/" + auctionId), null, null, 200));
        call(get(bids), null, null, 401);
        assertThat(JsonPath.<Double>read(detail, "$.auction.currentPrice")).isEqualTo(200.0);
        assertThat(JsonPath.<Integer>read(detail, "$.auction.bidCount")).isEqualTo(2);
        assertThat(JsonPath.<String>read(detail, "$.auction.artworkTitle")).isEqualTo("Dawn");
        assertThat(JsonPath.<String>read(detail, "$.auction.coverUrl")).startsWith("https://cdn.test/artworks/");
        String listed = body(call(get("/api/v1/auctions?artworkId=" + artworkId), null, null, 200));
        assertThat(JsonPath.<Double>read(listed, "$.items[0].currentPrice")).isEqualTo(200.0);
        assertThat(JsonPath.<Integer>read(listed, "$.items[0].bidCount")).isEqualTo(2);
        assertThat(JsonPath.<Double>read(detail, "$.minimumNextBid")).isEqualTo(210.0);
        call(post(bids), buyerToken, "{\"amount\":209.99}", 400);
        assertThat(this.<Double>read(call(get(bids), buyerToken, null, 200), "$.items[0].amount")).isEqualTo(200.0);
        assertThat(total(call(get("/api/v1/users/me/bids"), buyerToken, null, 200))).isEqualTo(1);

        call(delete("/api/v1/auctions/" + auctionId), buyerToken, null, 403);
        call(delete("/api/v1/auctions/" + auctionId), artistToken, null, 400);
    }

    private String closingFlow(String artistToken, String buyerToken, String thirdToken) throws Exception {
        String artworkId = createArtwork(artistToken, "Nocturne");
        String auction = "{\"artworkId\":\"" + artworkId + "\",\"type\":\"SIMPLE\",\"basePrice\":100,"
                + "\"endDate\":\"" + Instant.now().plusSeconds(60) + "\"}";
        String auctionId = read(call(post("/api/v1/auctions"), artistToken, auction, 201), "$.id");
        call(post("/api/v1/auctions/" + auctionId + "/bids"), buyerToken, "{\"amount\":250}", 201);

        Instant extendedEnd = Instant.parse(read(call(get("/api/v1/auctions/" + auctionId), buyerToken, null, 200),
                "$.auction.endDate"));
        assertThat(extendedEnd).isAfter(Instant.now().plusSeconds(100));
        assertThat(closer.findExpired()).doesNotContain(UUID.fromString(auctionId));

        jdbc.update("update auctions set end_date = ? where id = ?",
                java.sql.Timestamp.from(Instant.now().minusSeconds(1)), UUID.fromString(auctionId));
        assertThat(closer.findExpired()).contains(UUID.fromString(auctionId));
        String orderId = closer.close(UUID.fromString(auctionId)).orElseThrow().id().toString();
        assertThat(closer.close(UUID.fromString(auctionId))).isEmpty();
        assertThat(JsonPath.<java.util.List<String>>read(body(call(get("/api/v1/users/me/notifications"), buyerToken,
                null, 200)), "$.items[?(@.type == 'AUCTION_WON')].data.orderId")).contains(orderId);
        assertThat(JsonPath.<java.util.List<String>>read(body(call(get("/api/v1/users/me/notifications"), artistToken,
                null, 200)), "$.items[*].type")).contains("AUCTION_SOLD");

        assertThat(this.<String>read(call(get("/api/v1/auctions/" + auctionId), buyerToken, null, 200),
                "$.auction.status")).isEqualTo("CLOSED");
        assertThat(total(call(get("/api/v1/users/me/orders"), buyerToken, null, 200))).isEqualTo(1);
        assertThat(total(call(get("/api/v1/users/me/sales"), artistToken, null, 200))).isEqualTo(1);
        call(get("/api/v1/orders/" + orderId), thirdToken, null, 403);
        assertThat(this.<Double>read(call(get("/api/v1/orders/" + orderId), artistToken, null, 200),
                "$.totalPaid")).isEqualTo(293.0);
        return orderId;
    }

    private void paymentFlow(String orderId, String buyerToken) throws Exception {
        String pay = "/api/v1/orders/" + orderId + "/pay";
        call(post(pay), buyerToken, null, 400);
        call(put("/api/v1/users/me/payment-method"), buyerToken, "{\"paymentToken\":\"tok_fail_card\"}", 200);
        call(post(pay), buyerToken, null, 402);
        assertThat(JsonPath.<java.util.List<String>>read(body(call(get("/api/v1/users/me/notifications"), buyerToken,
                null, 200)), "$.items[*].type")).contains("PAYMENT_FAILED");
        assertThat(this.<String>read(call(get("/api/v1/orders/" + orderId), buyerToken, null, 200),
                "$.paymentStatus")).isEqualTo("FAILED");
        call(put("/api/v1/users/me/payment-method"), buyerToken, "{\"paymentToken\":\"tok_visa\"}", 200);
        assertThat(this.<String>read(call(post(pay), buyerToken, null, 200), "$.paymentStatus")).isEqualTo("PAID");
        assertThat(JsonPath.<java.util.List<String>>read(body(call(get("/api/v1/users/me/notifications"), buyerToken,
                null, 200)), "$.items[*].type")).contains("PAYMENT_APPROVED");
        call(post(pay), buyerToken, null, 400);
    }

    private void fulfillmentFlow(String orderId, String artistToken, String buyerToken) throws Exception {
        String ship = "/api/v1/orders/" + orderId + "/ship";
        String body = "{\"carrier\":\"Servientrega\",\"trackingNumber\":\"SV-123456\"}";
        call(post(ship), buyerToken, body, 403);
        call(post(ship), artistToken, "{\"carrier\":\"\"}", 400);
        String shipped = body(call(post(ship), artistToken, body, 200));
        assertThat(JsonPath.<String>read(shipped, "$.shippingStatus")).isEqualTo("SHIPPED");
        assertThat(JsonPath.<String>read(shipped, "$.trackingNumber")).isEqualTo("SV-123456");
        assertThat(JsonPath.<String>read(shipped, "$.shippedAt")).isNotNull();
        call(post(ship), artistToken, body, 400);

        String confirm = "/api/v1/orders/" + orderId + "/confirm-delivery";
        call(post(confirm), artistToken, null, 403);
        String delivered = body(call(post(confirm), buyerToken, null, 200));
        assertThat(JsonPath.<String>read(delivered, "$.shippingStatus")).isEqualTo("DELIVERED");
        assertThat(JsonPath.<String>read(delivered, "$.deliveredAt")).isNotNull();
        call(post(confirm), buyerToken, null, 400);

        assertThat(JsonPath.<java.util.List<String>>read(body(call(get("/api/v1/users/me/notifications"), buyerToken,
                null, 200)), "$.items[*].type")).contains("ORDER_SHIPPED");
        assertThat(JsonPath.<java.util.List<String>>read(body(call(get("/api/v1/users/me/notifications?size=50"),
                artistToken, null, 200)), "$.items[*].type")).contains("ORDER_DELIVERED");
    }

    private void reviewFlow(String orderId, String artistId, String artistToken, String buyerToken) throws Exception {
        String review = "/api/v1/orders/" + orderId + "/review";
        call(post(review), artistToken, "{\"rating\":5}", 403);
        call(post(review), buyerToken, "{\"rating\":6}", 400);
        String created = body(call(post(review), buyerToken, "{\"rating\":5,\"comment\":\" Excellent \"}", 201));
        assertThat(JsonPath.<String>read(created, "$.comment")).isEqualTo("Excellent");
        assertThat(JsonPath.<String>read(created, "$.sellerId")).isEqualTo(artistId);
        call(post(review), buyerToken, "{\"rating\":4}", 400);

        String reviews = body(call(get("/api/v1/users/" + artistId + "/reviews"), null, null, 200));
        assertThat(JsonPath.<Integer>read(reviews, "$.totalItems")).isEqualTo(1);
        assertThat(JsonPath.<Integer>read(reviews, "$.items[0].rating")).isEqualTo(5);
        assertThat(JsonPath.<String>read(reviews, "$.items[0].reviewerUsername")).isEqualTo("u");
        String profile = body(call(get("/api/v1/users/" + artistId), null, null, 200));
        assertThat(JsonPath.<Double>read(profile, "$.averageRating")).isEqualTo(5.0);
        assertThat(JsonPath.<Integer>read(profile, "$.reviewsCount")).isEqualTo(1);
        call(get("/api/v1/users/" + UUID.randomUUID() + "/reviews"), null, null, 404);
        assertThat(JsonPath.<java.util.List<String>>read(body(call(get("/api/v1/users/me/notifications?unreadOnly=true"),
                artistToken, null, 200)), "$.items[*].type")).contains("NEW_REVIEW");
    }

    private void buyNowFlow(String artistToken, String thirdToken) throws Exception {
        String artworkId = createArtwork(artistToken, "Marina");
        String auction = "{\"artworkId\":\"" + artworkId + "\",\"type\":\"IMMEDIATE\",\"basePrice\":100,"
                + "\"immediatePurchasePrice\":500,\"endDate\":\"" + Instant.now().plusSeconds(3600) + "\"}";
        String auctionId = read(call(post("/api/v1/auctions"), artistToken, auction, 201), "$.id");
        call(post("/api/v1/auctions/" + auctionId + "/buy-now"), artistToken, null, 400);
        String bought = body(call(post("/api/v1/auctions/" + auctionId + "/buy-now"), thirdToken, null, 201));
        assertThat(JsonPath.<Double>read(bought, "$.totalPaid")).isEqualTo(568.0);
        assertThat(JsonPath.<String>read(bought, "$.shippingStatus")).isEqualTo("PENDING");
        assertThat(JsonPath.<String>read(bought, "$.trackingNumber")).isNull();
        call(post("/api/v1/orders/" + JsonPath.read(bought, "$.id") + "/review"), thirdToken, "{\"rating\":5}", 400);
        call(post("/api/v1/orders/" + JsonPath.read(bought, "$.id") + "/cancel-checkout"), thirdToken, null, 200);
        assertThat(this.<String>read(call(get("/api/v1/auctions/" + auctionId), null, null, 200),
                "$.auction.status")).isEqualTo("ACTIVE");
        call(post("/api/v1/auctions/" + auctionId + "/buy-now"), thirdToken, null, 201);
    }

    private void accountFlow(String buyerToken, String email) throws Exception {
        assertThat(total(call(get("/api/v1/users/me/activity"), buyerToken, null, 200))).isPositive();
        assertThat(this.<String>read(call(put("/api/v1/users/me"), buyerToken,
                "{\"username\":\"buyer\",\"city\":\"Medellín\"}", 200), "$.username")).isEqualTo("buyer");
        call(put("/api/v1/users/me/password"), buyerToken,
                "{\"currentPassword\":\"incorrect\",\"newPassword\":\"new-password-123\"}", 400);
        String refreshBeforeChange = read(call(post("/api/v1/auth/login"), null,
                "{\"email\":\"" + email + "\",\"password\":\"password123\"}", 200), "$.refreshToken");
        call(put("/api/v1/users/me/password"), buyerToken,
                "{\"currentPassword\":\"password123\",\"newPassword\":\"letters-only\"}", 400);
        call(put("/api/v1/users/me/password"), buyerToken,
                "{\"currentPassword\":\"password123\",\"newPassword\":\"new-password-123\"}", 204);
        call(get("/api/v1/users/me"), buyerToken, null, 401);
        call(post("/api/v1/auth/refresh"), null, "{\"refreshToken\":\"" + refreshBeforeChange + "\"}", 401);
        call(post("/api/v1/auth/login"), null,
                "{\"email\":\"" + email.toUpperCase() + "\",\"password\":\"new-password-123\"}", 200);
    }

    private void sessionFlow(String email) throws Exception {
        String login = body(call(post("/api/v1/auth/login"), null,
                "{\"email\":\"" + email + "\",\"password\":\"password123\"}", 200));
        String firstRefresh = JsonPath.read(login, "$.refreshToken");

        String renewed = body(call(post("/api/v1/auth/refresh"), null,
                "{\"refreshToken\":\"" + firstRefresh + "\"}", 200));
        String secondRefresh = JsonPath.read(renewed, "$.refreshToken");
        assertThat(secondRefresh).isNotEqualTo(firstRefresh);
        call(get("/api/v1/users/me"), JsonPath.read(renewed, "$.accessToken"), null, 200);

        call(post("/api/v1/auth/refresh"), null, "{\"refreshToken\":\"" + firstRefresh + "\"}", 401);
        call(post("/api/v1/auth/refresh"), null, "{\"refreshToken\":\"" + secondRefresh + "\"}", 401);

        String other = read(call(post("/api/v1/auth/login"), null,
                "{\"email\":\"" + email + "\",\"password\":\"password123\"}", 200), "$.refreshToken");
        call(post("/api/v1/auth/logout"), null, "{\"refreshToken\":\"" + other + "\"}", 204);
        call(post("/api/v1/auth/logout"), null, "{\"refreshToken\":\"" + other + "\"}", 204);
        call(post("/api/v1/auth/refresh"), null, "{\"refreshToken\":\"" + other + "\"}", 401);
    }

    private void favoritesFlow(String artworkId, String buyerToken, String thirdToken) throws Exception {
        String favorite = "/api/v1/artworks/" + artworkId + "/favorite";
        call(post(favorite), null, null, 401);
        call(post(favorite), buyerToken, null, 204);
        call(post(favorite), buyerToken, null, 204);
        call(post(favorite), thirdToken, null, 204);
        call(post("/api/v1/artworks/" + UUID.randomUUID() + "/favorite"), buyerToken, null, 404);

        String asBuyer = body(call(get("/api/v1/artworks/" + artworkId), buyerToken, null, 200));
        assertThat(JsonPath.<Integer>read(asBuyer, "$.favoritesCount")).isEqualTo(2);
        assertThat(JsonPath.<Boolean>read(asBuyer, "$.favorited")).isTrue();
        assertThat(this.<Boolean>read(call(get("/api/v1/artworks/" + artworkId), null, null, 200), "$.favorited"))
                .isFalse();
        assertThat(this.<String>read(call(get("/api/v1/users/me/favorites"), buyerToken, null, 200),
                "$.items[0].id")).isEqualTo(artworkId);

        call(delete(favorite), thirdToken, null, 204);
        call(delete(favorite), thirdToken, null, 204);
        assertThat(this.<Integer>read(call(get("/api/v1/artworks/" + artworkId), null, null, 200), "$.favoritesCount"))
                .isEqualTo(1);
        assertThat(total(call(get("/api/v1/users/me/favorites"), thirdToken, null, 200))).isZero();
    }

    private void notificationsFlow(String buyerToken, String artistToken) throws Exception {
        String base = "/api/v1/users/me/notifications";
        call(get(base), null, null, 401);
        call(post(base + "/000000000000000000000000/read"), buyerToken, null, 404);
        call(post(base + "/read-all"), buyerToken, null, 204);
        assertThat(this.<Integer>read(call(get(base + "/unread-count"), buyerToken, null, 200), "$.count")).isZero();
        assertThat(total(call(get(base + "?unreadOnly=true"), buyerToken, null, 200))).isZero();
    }

    private void searchFlow(String artistId) throws Exception {
        String byArtist = "/api/v1/artworks?artistId=" + artistId;
        assertThat(this.<String>read(call(get(byArtist + "&sort=title,asc"), null, null, 200), "$.items[0].title"))
                .isEqualTo("Dawn");
        assertThat(this.<String>read(call(get(byArtist + "&sort=title,desc"), null, null, 200), "$.items[0].title"))
                .isEqualTo("Nocturne");
        assertThat(total(call(get(byArtist + "&minPrice=300&maxPrice=300"), null, null, 200))).isEqualTo(3);
        assertThat(total(call(get(byArtist + "&minPrice=301"), null, null, 200))).isZero();
        call(get(byArtist + "&minPrice=500&maxPrice=100"), null, null, 400);
        call(get(byArtist + "&sort=passwordHash,asc"), null, null, 400);
        assertThat(total(call(get("/api/v1/auctions?artistId=" + artistId + "&minPrice=100&sort=basePrice,desc"),
                null, null, 200))).isEqualTo(3);
    }

    private void statsFlow(String artistToken, String buyerToken) throws Exception {
        String artist = body(call(get("/api/v1/users/me/stats"), artistToken, null, 200));
        assertThat(JsonPath.<Integer>read(artist, "$.artworks")).isEqualTo(3);
        assertThat(JsonPath.<Integer>read(artist, "$.activeAuctions")).isEqualTo(2);
        assertThat(JsonPath.<Integer>read(artist, "$.salesCount")).isEqualTo(1);
        assertThat(JsonPath.<Double>read(artist, "$.revenue")).isEqualTo(250.0);
        assertThat(JsonPath.<Integer>read(artist, "$.purchasesCount")).isZero();
        assertThat(JsonPath.<Double>read(artist, "$.averageRating")).isEqualTo(5.0);
        assertThat(this.<Integer>read(call(get("/api/v1/users/me/stats"), buyerToken, null, 200), "$.purchasesCount"))
                .isEqualTo(1);
        call(get("/api/v1/users/me/stats"), null, null, 401);
    }

    private void lockoutFlow(String email) throws Exception {
        registerAndLogin(email);
        String wrong = "{\"email\":\"" + email + "\",\"password\":\"incorrect1\"}";
        for (int i = 0; i < 5; i++) {
            call(post("/api/v1/auth/login"), null, wrong, 400);
        }
        MvcResult blocked = call(post("/api/v1/auth/login"), null,
                "{\"email\":\"" + email + "\",\"password\":\"password123\"}", 429);
        assertThat(Integer.parseInt(blocked.getResponse().getHeader("Retry-After"))).isPositive();
    }

    private String createArtwork(String token, String title) throws Exception {
        return read(call(post("/api/v1/artworks"), token,
                "{\"title\":\"" + title + "\",\"description\":\"Test artwork\",\"technique\":\"Watercolor\","
                        + "\"weightKg\":1.5,\"dimensions\":\"30x40\"}", 201), "$.id");
    }

    private String registerAndLogin(String email) throws Exception {
        call(post("/api/v1/users/register/code"), null, "{\"email\":\"" + email + "\"}", 204);
        call(post("/api/v1/users/register"), null, registration(email, verificationCodes.get(email)), 201);
        String session = body(call(post("/api/v1/auth/login"), null,
                "{\"email\":\"" + email + "\",\"password\":\"password123\"}", 200));
        assertThat(JsonPath.<String>read(session, "$.tokenType")).isEqualTo("Bearer");
        assertThat(JsonPath.<Integer>read(session, "$.expiresIn")).isEqualTo(900);
        assertThat(JsonPath.<String>read(session, "$.refreshToken")).isNotBlank();
        return JsonPath.read(session, "$.accessToken");
    }

    private String registration(String email, String code) {
        return "{\"username\":\"u\",\"email\":\"" + email + "\",\"password\":\"password123\","
                + "\"city\":\"Bogotá\",\"shippingAddress\":\"1 Main Street\",\"verificationCode\":\"" + code + "\"}";
    }

    @Test
    void registrationRequiresAnUnusedCodeForTheSameAddress() throws Exception {
        String email = UUID.randomUUID() + "@test.com";
        call(post("/api/v1/users/register"), null, registration(email, "000000"), 400);
        call(post("/api/v1/users/register/code"), null, "{\"email\":\"" + email + "\"}", 204);
        String code = verificationCodes.get(email);
        assertThat(code).matches("[0-9]{6}");
        String wrong = code.equals("000000") ? "111111" : "000000";
        call(post("/api/v1/users/register"), null, registration(email, wrong), 400);
        assertThat(jdbc.queryForObject("select attempts from registration_challenges where email = ?",
                Integer.class, email)).isEqualTo(1);
        call(post("/api/v1/users/register"), null, registration("other-" + email, code), 400);
        call(post("/api/v1/users/register"), null, registration(email, code), 201);
        call(post("/api/v1/users/register"), null, registration(email, code), 400);
    }

    @Test
    void sellersCanSettlePublicBidsAndNewFormatsKeepTheirRulesAndCurrency() throws Exception {
        String seller = registerAndLogin(UUID.randomUUID() + "@test.com");
        String buyer = registerAndLogin(UUID.randomUUID() + "@test.com");
        String other = registerAndLogin(UUID.randomUUID() + "@test.com");

        String open = formatAuction(seller, "SIMPLE", "EUR", "500", "");
        String openBid = read(call(post(open + "/bids"), buyer, "{\"amount\":650}", 201), "$.id");
        String accept = "{\"bidId\":\"" + openBid + "\"}";
        call(post(open + "/accept-bid"), other, accept, 403);
        call(post(open + "/accept-bid"), seller, "{\"bidId\":\"" + UUID.randomUUID() + "\"}", 400);
        assertThat(this.<String>read(call(post(open + "/accept-bid"), seller, accept, 201),
                "$.snapshot.currency")).isEqualTo("eur");
        call(post(open + "/accept-bid"), seller, accept, 400);

        String sealed = formatAuction(seller, "SEALED", "USD", "100", "");
        String sealedBid = read(call(post(sealed + "/bids"), buyer, "{\"amount\":250}", 201), "$.id");
        call(post(sealed + "/bids"), buyer, "{\"amount\":260}", 400);
        call(post(sealed + "/bids"), other, "{\"amount\":225}", 201);
        assertThat(this.<Number>read(call(get(sealed), null, null, 200), "$.auction.currentPrice").intValue()).isEqualTo(100);
        assertThat(this.<Object>read(call(get(sealed), null, null, 200), "$.highestBid")).isNull();
        assertThat(total(call(get(sealed + "/bids"), seller, null, 200))).isZero();
        call(post(sealed + "/accept-bid"), seller, "{\"bidId\":\"" + sealedBid + "\"}", 400);
        jdbc.update("update auctions set start_date = now() - interval '2 minutes', end_date = now() - interval '1 second' where id = ?",
                UUID.fromString(sealed.substring(sealed.lastIndexOf('/') + 1)));
        var result = closer.close(UUID.fromString(sealed.substring(sealed.lastIndexOf('/') + 1))).orElseThrow();
        assertThat(result.subtotal()).isEqualByComparingTo("250");
        assertThat(total(call(get(sealed + "/bids"), seller, null, 200))).isEqualTo(2);

        String cop = formatAuction(seller, "SIMPLE", "COP", "25000", "");
        call(post(cop + "/bids"), buyer, "{\"amount\":25500}", 400);
        call(post(cop + "/bids"), buyer, "{\"amount\":25000}", 201);
        assertThat(this.<Number>read(call(get(cop), null, null, 200), "$.minimumNextBid").intValue())
                .isEqualTo(27000);

        String dutch = formatAuction(seller, "DUTCH", "COP", "10000", ",\"floorPrice\":5000");
        call(post(dutch + "/bids"), buyer, "{\"amount\":8000}", 400);
        var sale = call(post(dutch + "/buy-now"), buyer, null, 201);
        assertThat(this.<Number>read(sale, "$.subtotal").doubleValue()).isBetween(5000.0, 10000.0);
        assertThat(this.<String>read(sale, "$.snapshot.currency")).isEqualTo("cop");
        assertThat(this.<Number>read(sale, "$.shippingCost").intValue()).isEqualTo(18000);
        assertThat(this.<String>read(call(get(dutch), null, null, 200), "$.auction.status")).isEqualTo("ACTIVE");
        String competitor = read(call(post(dutch + "/buy-now"), other, null, 201), "$.id");
        String checkout = read(sale, "$.id");
        call(put("/api/v1/users/me/payment-method"), buyer, "{\"paymentToken\":\"tok_visa\"}", 200);
        call(put("/api/v1/users/me/payment-method"), other, "{\"paymentToken\":\"tok_visa\"}", 200);
        assertThat(together(() -> requestStatus(post("/api/v1/orders/" + checkout + "/pay"), buyer, null),
                () -> requestStatus(post("/api/v1/orders/" + competitor + "/pay"), other, null)))
                .containsExactlyInAnyOrder(200, 400);
        assertThat(this.<String>read(call(get(dutch), null, null, 200), "$.auction.status")).isEqualTo("CLOSED");
        call(post(dutch + "/buy-now"), other, null, 400);
    }

    private String formatAuction(String seller, String type, String currency, String base, String extra) throws Exception {
        String artwork = createArtwork(seller, type);
        String request = "{\"artworkId\":\"" + artwork + "\",\"type\":\"" + type + "\",\"currency\":\"" + currency
                + "\",\"basePrice\":" + base + ",\"endDate\":\"" + Instant.now().plusSeconds(600) + "\"" + extra + "}";
        return "/api/v1/auctions/" + this.<String>read(call(post("/api/v1/auctions"), seller, request, 201), "$.id");
    }

    private MvcResult upload(String path, String token, String contentType, int expected) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "file", contentType, new byte[]{1, 2, 3});
        return call(multipart(path).file(file), token, null, expected);
    }

    private MvcResult call(MockHttpServletRequestBuilder request, String token, String json, int expected)
            throws Exception {
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        if (json != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        MvcResult result = mvc.perform(request).andReturn();
        assertThat(result.getResponse().getStatus())
                .as("%s %s -> %s", result.getRequest().getMethod(), result.getRequest().getRequestURI(), body(result))
                .isEqualTo(expected);
        return result;
    }

    private static String body(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }

    private <T> T read(MvcResult result, String path) throws Exception {
        return JsonPath.read(body(result), path);
    }

    private int total(MvcResult result) throws Exception {
        return JsonPath.<Integer>read(body(result), "$.totalItems");
    }
}
