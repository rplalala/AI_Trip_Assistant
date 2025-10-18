package com.demo.externalservice.service;

import com.demo.externalservice.config.BookingProperties;
import com.demo.externalservice.dto.booking.ItineraryQuoteReq;
import com.demo.externalservice.dto.booking.QuoteItem;
import com.demo.externalservice.dto.booking.QuoteReq;
import com.demo.externalservice.exception.QuoteExpiredException;
import com.demo.externalservice.exception.QuoteTokenInvalidException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TokenService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<QuoteItem>> ITEM_LIST_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<Map<String, Object>>> LIST_OF_MAP_TYPE = new TypeReference<>() {};

    private final Key key;
    private final BookingProperties properties;
    private final ObjectMapper mapper;

    public TokenService(BookingProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.getTokenSecret().getBytes(StandardCharsets.UTF_8));
        this.mapper = new ObjectMapper()
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                .findAndRegisterModules();
    }

    public SignedQuote signQuote(QuoteReq request, PricingResult pricingResult) {
        try {
            String paramsJson = canonicalJson(request.params());
            String itemsJson = mapper.writeValueAsString(pricingResult.items());

            OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plus(properties.getTokenTtl());

            Map<String, Object> claims = new HashMap<>();
            claims.put("kind", "single");
            claims.put("pt", request.productType());
            claims.put("ccy", request.currency());
            claims.put("ps", request.partySize());
            claims.put("params", paramsJson);
            claims.put("items", itemsJson);
            claims.put("total", pricingResult.total().toPlainString());
            claims.put("fees", pricingResult.fees().toPlainString());
            claims.put("unit", pricingResult.primaryItem().unitPrice().toPlainString());
            claims.put("qty", pricingResult.primaryItem().quantity());

            Date issuedAt = new Date();
            Date expiration = Date.from(expiresAt.toInstant());

            String token = Jwts.builder()
                    .setClaims(claims)
                    .setIssuedAt(issuedAt)
                    .setExpiration(expiration)
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();

            return new SignedQuote(token, expiresAt);
        } catch (JwtException | JsonProcessingException e) {
            throw new QuoteTokenInvalidException("Failed to sign quote token", e);
        }
    }

    public SignedQuote signItineraryQuote(ItineraryQuoteReq request, List<ItineraryItemSnapshot> items) {
        try {
            OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plus(properties.getTokenTtl());
            List<Map<String, Object>> itemClaims = new ArrayList<>(items.size());

            BigDecimal bundleTotal = BigDecimal.ZERO;
            BigDecimal bundleFees = BigDecimal.ZERO;

            for (ItineraryItemSnapshot snapshot : items) {
                String paramsJson = canonicalJson(snapshot.params());
                String quoteItemsJson = mapper.writeValueAsString(snapshot.quoteItems());

                Map<String, Object> entry = new HashMap<>();
                entry.put("ref", snapshot.reference());
                entry.put("pt", snapshot.productType());
                entry.put("ps", snapshot.partySize());
                entry.put("params", paramsJson);
                entry.put("items", quoteItemsJson);
                entry.put("total", snapshot.total().toPlainString());
                entry.put("fees", snapshot.fees().toPlainString());

                itemClaims.add(entry);
                bundleTotal = bundleTotal.add(snapshot.total());
                bundleFees = bundleFees.add(snapshot.fees());
            }

            Map<String, Object> claims = new HashMap<>();
            claims.put("kind", "itinerary");
            claims.put("itinerary_id", request.itineraryId());
            claims.put("currency", request.currency());
            claims.put("bundle_total", bundleTotal.toPlainString());
            claims.put("bundle_fees", bundleFees.toPlainString());
            claims.put("items", mapper.writeValueAsString(itemClaims));

            Date issuedAt = new Date();
            Date expiration = Date.from(expiresAt.toInstant());

            String token = Jwts.builder()
                    .setClaims(claims)
                    .setIssuedAt(issuedAt)
                    .setExpiration(expiration)
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();

            return new SignedQuote(token, expiresAt);
        } catch (JwtException | JsonProcessingException e) {
            throw new QuoteTokenInvalidException("Failed to sign itinerary quote token", e);
        }
    }

    public QuoteTokenPayload verifyQuote(String token) {
        try {
            Jws<Claims> parsed = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            Claims claims = parsed.getBody();

            OffsetDateTime expiresAt = claims.getExpiration()
                    .toInstant().atOffset(ZoneOffset.UTC);

            String kind = claims.get("kind", String.class);
            if (kind == null || kind.isBlank()) {
                kind = "single";
            }

            if ("itinerary".equalsIgnoreCase(kind)) {
                return parseItineraryClaims(claims, token, expiresAt);
            }

            return parseSingleClaims(claims, token, expiresAt);
        } catch (ExpiredJwtException e) {
            throw new QuoteExpiredException("Quote token expired");
        } catch (JwtException | JsonProcessingException e) {
            throw new QuoteTokenInvalidException("Invalid quote token", e);
        }
    }

    private QuoteTokenPayload parseSingleClaims(Claims claims, String token, OffsetDateTime expiresAt) throws JsonProcessingException {
        String paramsJson = claims.get("params", String.class);
        Map<String, Object> params = mapper.readValue(paramsJson, MAP_TYPE);

        String itemsJson = claims.get("items", String.class);
        List<QuoteItem> items = mapper.readValue(itemsJson, ITEM_LIST_TYPE);

        BigDecimal total = new BigDecimal(claims.get("total", String.class));
        BigDecimal fees = new BigDecimal(claims.get("fees", String.class));
        BigDecimal unit = new BigDecimal(claims.get("unit", String.class));
        Integer quantity = claims.get("qty", Integer.class);

        String productType = claims.get("pt", String.class);
        String currency = claims.get("ccy", String.class);
        Integer partySize = claims.get("ps", Integer.class);

        return new QuoteTokenPayload(
                "single",
                productType,
                currency,
                partySize,
                params,
                items,
                unit,
                quantity,
                fees,
                total,
                expiresAt,
                token,
                canonicalJson(params),
                null,
                List.of(),
                fees,
                total
        );
    }

    private QuoteTokenPayload parseItineraryClaims(Claims claims, String token, OffsetDateTime expiresAt) throws JsonProcessingException {
        String itineraryId = claims.get("itinerary_id", String.class);
        String currency = claims.get("currency", String.class);
        String itemsJson = claims.get("items", String.class);

        if (itineraryId == null || currency == null || itemsJson == null) {
            throw new QuoteTokenInvalidException("Itinerary quote token missing required fields");
        }

        List<Map<String, Object>> rawItems = mapper.readValue(itemsJson, LIST_OF_MAP_TYPE);
        List<ItineraryItemPayload> itineraryItems = new ArrayList<>(rawItems.size());
        Set<String> refs = new HashSet<>();

        for (Map<String, Object> raw : rawItems) {
            String reference = Objects.toString(raw.get("ref"), null);
            if (reference == null || reference.isBlank()) {
                throw new QuoteTokenInvalidException("Itinerary quote token missing item reference");
            }
            if (!refs.add(reference)) {
                throw new QuoteTokenInvalidException("Duplicate itinerary item reference in quote token");
            }

            String productType = Objects.toString(raw.get("pt"), null);
            Number partySizeNumber = (Number) raw.get("ps");
            Integer partySize = partySizeNumber == null ? null : partySizeNumber.intValue();
            String paramsJson = Objects.toString(raw.get("params"), null);
            String quoteItemsJson = Objects.toString(raw.get("items"), null);
            String totalStr = Objects.toString(raw.get("total"), null);
            String feesStr = Objects.toString(raw.get("fees"), null);

            if (productType == null || partySize == null || paramsJson == null || quoteItemsJson == null
                    || totalStr == null || feesStr == null) {
                throw new QuoteTokenInvalidException("Itinerary quote token contains incomplete item");
            }

            Map<String, Object> params = mapper.readValue(paramsJson, MAP_TYPE);
            List<QuoteItem> quoteItems = mapper.readValue(quoteItemsJson, ITEM_LIST_TYPE);
            BigDecimal total = new BigDecimal(totalStr);
            BigDecimal fees = new BigDecimal(feesStr);

            itineraryItems.add(new ItineraryItemPayload(
                    reference,
                    productType,
                    partySize,
                    params,
                    quoteItems,
                    fees,
                    total,
                    paramsJson
            ));
        }

        BigDecimal bundleTotal = new BigDecimal(claims.get("bundle_total", String.class));
        BigDecimal bundleFees = new BigDecimal(claims.get("bundle_fees", String.class));

        return new QuoteTokenPayload(
                "itinerary",
                null,
                currency,
                null,
                null,
                List.of(),
                null,
                null,
                bundleFees,
                bundleTotal,
                expiresAt,
                token,
                null,
                itineraryId,
                List.copyOf(itineraryItems),
                bundleFees,
                bundleTotal
        );
    }

    public String toJson(QuoteTokenPayload payload) {
        try {
            return mapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize quote payload", e);
        }
    }

    public String canonicalJson(Map<String, Object> params) throws JsonProcessingException {
        return mapper.writeValueAsString(params);
    }

    public record SignedQuote(String token, OffsetDateTime expiresAt) {
    }

    public record QuoteTokenPayload(
            String kind,
            String productType,
            String currency,
            Integer partySize,
            Map<String, Object> params,
            List<QuoteItem> items,
            BigDecimal unitPrice,
            Integer quantity,
            BigDecimal fees,
            BigDecimal total,
            OffsetDateTime expiresAt,
            String rawToken,
            String canonicalParams,
            String itineraryId,
            List<ItineraryItemPayload> itineraryItems,
            BigDecimal bundleFees,
            BigDecimal bundleTotal
    ) {
        public QuoteReq toQuoteReq() {
            if (isItinerary()) {
                throw new UnsupportedOperationException("Itinerary quotes cannot be converted to a single QuoteReq");
            }
            return new QuoteReq(productType, currency, partySize, params);
        }

        public boolean isItinerary() {
            return "itinerary".equalsIgnoreCase(kind);
        }

        public List<String> itineraryRefs() {
            return itineraryItems.stream()
                    .map(ItineraryItemPayload::reference)
                    .toList();
        }

        public Map<String, ItineraryItemPayload> itineraryItemsByRef() {
            return itineraryItems.stream()
                    .collect(Collectors.toMap(ItineraryItemPayload::reference, Function.identity()));
        }
    }

    public record ItineraryItemPayload(
            String reference,
            String productType,
            Integer partySize,
            Map<String, Object> params,
            List<QuoteItem> quoteItems,
            BigDecimal fees,
            BigDecimal total,
            String canonicalParams
    ) {
        public ItineraryItemPayload {
            quoteItems = List.copyOf(quoteItems);
            params = Collections.unmodifiableMap(new LinkedHashMap<>(params));
        }

        public QuoteReq toQuoteReq(String currency) {
            return new QuoteReq(productType, currency, partySize, params);
        }
    }

    public record ItineraryItemSnapshot(
            String reference,
            String productType,
            Integer partySize,
            Map<String, Object> params,
            List<QuoteItem> quoteItems,
            BigDecimal fees,
            BigDecimal total
    ) {
        public ItineraryItemSnapshot {
            quoteItems = List.copyOf(quoteItems);
            params = Collections.unmodifiableMap(new LinkedHashMap<>(params));
        }
    }
}
