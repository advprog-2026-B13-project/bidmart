package id.ac.ui.cs.advprog.bidmart.functional.config;

public class TestConfig {

    public static String baseUrl() {
        return env("SERENITY_BASE_URL", "http://localhost:8080");
    }

    public static String bidderEmail() {
        return env("SERENITY_BIDDER_EMAIL", "testbidder@bidmart.test");
    }

    public static String bidderPassword() {
        return env("SERENITY_BIDDER_PASSWORD", "testpassword");
    }

    public static String testListingId() {
        return env("SERENITY_TEST_LISTING_ID", "");
    }

    public static long testBidAmount() {
        return Long.parseLong(env("SERENITY_TEST_BID_AMOUNT", "50000"));
    }

    // Added on top of current price so the bid is always above minimum increment
    public static long bidIncrement() {
        return Long.parseLong(env("SERENITY_BID_INCREMENT", "10000"));
    }

    private static String env(String key, String fallback) {
        String val = System.getenv(key);
        return (val != null && !val.isBlank()) ? val : fallback;
    }
}
