Feature: Bidding API

  Scenario: Get bids for a listing returns a list
    When I get the bids for the test listing
    Then the response code is 200
    And the response contains a list of bids

  Scenario: Get auction status returns current price
    When I get the auction status for the test listing
    Then the response code is 200
    And the response contains a current price

  Scenario: Place a bid without authentication returns 401
    When I place a bid of 50000 without authentication
    Then the response code is 401

  Scenario: Authenticated bidder places a valid bid
    Given I am logged in as the test bidder
    When I place a bid on the test listing
    Then the response code is 200
    And the bid is accepted or outbid

  Scenario: Place a bid that is too low returns 400
    Given I am logged in as the test bidder
    When I place a bid of 1 on the test listing
    Then the response code is 400
