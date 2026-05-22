package id.ac.ui.cs.advprog.bidmart.functional.steps;

import id.ac.ui.cs.advprog.bidmart.functional.config.TestConfig;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.math.BigDecimal;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class BiddingStepDefs {

    private Response lastResponse;
    private String accessToken;

    @Before
    public void setUp() {
        RestAssured.baseURI = TestConfig.baseUrl();
        accessToken = null;
        lastResponse = null;
    }

    @Given("I am logged in as the test bidder")
    public void loginAsTestBidder() {
        Response response = given()
                .contentType("application/json")
                .body(Map.of(
                        "email", TestConfig.bidderEmail(),
                        "password", TestConfig.bidderPassword()))
                .post("/api/auth/login");

        assertThat(response.statusCode())
                .as("Login failed — check SERENITY_BIDDER_EMAIL / SERENITY_BIDDER_PASSWORD secrets")
                .isEqualTo(200);

        // Cookie is HttpOnly (name: bm_access_token) so REST Assured won't receive it; read from response body
        accessToken = response.jsonPath().getString("data.accessToken");
        assertThat(accessToken)
                .as("accessToken must be present (cookie or body)")
                .isNotBlank();
    }

    @When("I get the bids for the test listing")
    public void getBidsForListing() {
        lastResponse = given()
                .get("/api/bidding/listings/{id}/bids", TestConfig.testListingId());
    }

    @When("I get the auction status for the test listing")
    public void getAuctionStatus() {
        lastResponse = given()
                .get("/api/bidding/listings/{id}/status", TestConfig.testListingId());
    }

    @When("I place a bid of {long} without authentication")
    public void placeBidWithoutAuth(long amount) {
        lastResponse = given()
                .contentType("application/json")
                .body(Map.of(
                        "listingId", TestConfig.testListingId(),
                        "amount", amount,
                        "bidType", "MANUAL"))
                .post("/api/bidding/bids");
    }

    @When("I place a manual bid on the test listing")
    public void placeManualBidOnTestListing() {
        long bidAmount = fetchCurrentPrice().longValue() + 1;

        lastResponse = authenticatedRequest()
                .contentType("application/json")
                .body(Map.of(
                        "listingId", TestConfig.testListingId(),
                        "amount", bidAmount,
                        "bidType", "MANUAL"))
                .post("/api/bidding/bids");
    }

    @When("I place a proxy bid above current price on the test listing")
    public void placeProxyBidAboveCurrentPrice() {
        // Fetch fresh price — captures any price movement from the manual bid above
        long bidAmount = fetchCurrentPrice().longValue() + 1;

        lastResponse = authenticatedRequest()
                .contentType("application/json")
                .body(Map.of(
                        "listingId", TestConfig.testListingId(),
                        "amount", bidAmount,
                        "bidType", "PROXY"))
                .post("/api/bidding/bids");
    }

    @When("I get the bids for the test listing with authentication")
    public void getBidsForListingAuthenticated() {
        lastResponse = authenticatedRequest()
                .get("/api/bidding/listings/{id}/bids", TestConfig.testListingId());
    }

    @And("my proxy bid has a max amount")
    public void myProxyBidHasMaxAmount() {
        java.util.List<java.util.Map<String, Object>> bids = lastResponse.jsonPath().getList("data");
        assertThat(bids).as("bid list must not be null").isNotNull();
        boolean found = bids.stream().anyMatch(bid -> bid.get("maxAmount") != null);
        assertThat(found).as("at least one of my bids should have a maxAmount (proxy bid)").isTrue();
    }

    @When("I place a bid of {long} on the test listing")
    public void placeBidOfAmountOnTestListing(long amount) {
        lastResponse = authenticatedRequest()
                .contentType("application/json")
                .body(Map.of(
                        "listingId", TestConfig.testListingId(),
                        "amount", amount,
                        "bidType", "MANUAL"))
                .post("/api/bidding/bids");
    }

    @Then("the response code is {int}")
    public void responseCodeIs(int expectedCode) {
        assertThat(lastResponse.statusCode()).isEqualTo(expectedCode);
    }

    @And("the response contains a list of bids")
    public void responseContainsBidsList() {
        Object data = lastResponse.jsonPath().get("data");
        assertThat(data).isNotNull();
    }

    @And("the response contains a current price")
    public void responseContainsCurrentPrice() {
        Object price = lastResponse.jsonPath().get("data.currentPrice");
        assertThat(price).isNotNull();
    }

    @And("the bid is accepted or outbid")
    public void bidIsAcceptedOrOutbid() {
        String status = lastResponse.jsonPath().getString("data.status");
        assertThat(status)
                .as("Bid status should be ACCEPTED or OUTBID, was: %s", status)
                .isIn("ACCEPTED", "OUTBID");
    }

    private BigDecimal fetchCurrentPrice() {
        Response statusResponse = given()
                .get("/api/bidding/listings/{id}/status", TestConfig.testListingId());
        assertThat(statusResponse.statusCode()).as("Auction status fetch failed").isEqualTo(200);
        Object rawPrice = statusResponse.jsonPath().get("data.currentPrice");
        assertThat(rawPrice).as("currentPrice must not be null").isNotNull();
        return new BigDecimal(rawPrice.toString());
    }

    private RequestSpecification authenticatedRequest() {
        assertThat(accessToken).as("Must be logged in before placing a bid").isNotBlank();
        return given().header("Authorization", "Bearer " + accessToken);
    }
}
