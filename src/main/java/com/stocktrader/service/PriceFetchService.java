package com.stocktrader.service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PriceFetchService {

    private static final String URL_TEMPLATE =
            "https://query1.finance.yahoo.com/v8/finance/chart/%s?interval=1d&range=1d";
    private static final Pattern PRICE_PATTERN =
            Pattern.compile("\"regularMarketPrice\":\\{\"raw\":(\\d+\\.?\\d*)");

    private final HttpClient http = HttpClient.newHttpClient();

    public BigDecimal fetchPrice(String ticker) {
        String url = String.format(URL_TEMPLATE, ticker.toUpperCase());
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new PriceFetchException("HTTP " + resp.statusCode() + " for ticker: " + ticker);
            }

            Matcher m = PRICE_PATTERN.matcher(resp.body());
            if (!m.find()) {
                throw new PriceFetchException("Price not found in response for ticker: " + ticker);
            }
            return new BigDecimal(m.group(1));
        } catch (PriceFetchException e) {
            throw e;
        } catch (Exception e) {
            throw new PriceFetchException("Failed to fetch price for " + ticker + ": " + e.getMessage(), e);
        }
    }

    public static class PriceFetchException extends RuntimeException {
        public PriceFetchException(String msg) { super(msg); }
        public PriceFetchException(String msg, Throwable cause) { super(msg, cause); }
    }
}
