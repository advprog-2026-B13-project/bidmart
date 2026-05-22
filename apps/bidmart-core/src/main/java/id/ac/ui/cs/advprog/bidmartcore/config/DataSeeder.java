package id.ac.ui.cs.advprog.bidmartcore.config;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Role;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.User;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.MFAType;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.UserStatus;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence.spring.RoleSpringRepository;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence.spring.UserSpringRepository;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model.Bid;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model.BidSource;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model.BidStatus;
import id.ac.ui.cs.advprog.bidmartcore.bidding.infrastructure.adapter.output.persistence.spring.BidSpringRepository;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Category;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Listing;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.ListingStatus;
import id.ac.ui.cs.advprog.bidmartcore.catalog.repository.CategoryRepository;
import id.ac.ui.cs.advprog.bidmartcore.catalog.repository.ListingRepository;
import id.ac.ui.cs.advprog.bidmartcore.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder {

    private static final String DEFAULT_PASSWORD = "password123";
    private static final String CAT_ELECTRONICS = "Electronics";
    private static final String CAT_WATCHES = "Watches";
    private static final String CAT_FURNITURE = "Furniture";
    private static final String CAT_BOOKS = "Books";
    private static final String CAT_MUSICAL_INSTRUMENTS = "Musical Instruments";
    private static final String CAT_GAMING = "Gaming";
    private static final String CAT_JEWELRY = "Jewelry";
    private static final String CAT_COLLECTIBLES = "Collectibles";
    private static final String CAT_FASHION = "Fashion";

    private final UserSpringRepository userRepository;
    private final RoleSpringRepository roleRepository;
    private final CategoryRepository categoryRepository;
    private final ListingRepository listingRepository;
    private final BidSpringRepository bidRepository;
    private final PasswordEncoder passwordEncoder;
        private final WalletService walletService;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedData() {
        if (userRepository.count() > 0) {
            log.info("Database already seeded, skipping...");
            return;
        }

        log.info("Seeding database with mock data...");

        // --- Roles ---
        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setName("USER");
                    return roleRepository.save(r);
                });

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setName("ADMIN");
                    return roleRepository.save(r);
                });

        // --- Users ---
        User sarah = createUser("sarah.chen@email.com", DEFAULT_PASSWORD, "Sarah Chen",
                "https://i.pravatar.cc/150?u=sarah", userRole, 2450.00);
        User marcus = createUser("marcus.wong@email.com", DEFAULT_PASSWORD, "Marcus Wong",
                "https://i.pravatar.cc/150?u=marcus", userRole, 1890.50);
        User elena = createUser("elena.rossi@email.com", DEFAULT_PASSWORD, "Elena Rossi",
                "https://i.pravatar.cc/150?u=elena", userRole, 3200.00);

        List<User> allUsers = List.of(sarah, marcus, elena);

        // --- Categories ---
        Map<String, Category> topCategories = Map.of(
                CAT_ELECTRONICS, saveCategory(CAT_ELECTRONICS, null),
                CAT_WATCHES, saveCategory(CAT_WATCHES, null),
                "Art", saveCategory("Art", null),
                CAT_FURNITURE, saveCategory(CAT_FURNITURE, null),
                CAT_BOOKS, saveCategory(CAT_BOOKS, null),
                CAT_MUSICAL_INSTRUMENTS, saveCategory(CAT_MUSICAL_INSTRUMENTS, null),
                CAT_GAMING, saveCategory(CAT_GAMING, null),
                CAT_JEWELRY, saveCategory(CAT_JEWELRY, null),
                CAT_COLLECTIBLES, saveCategory(CAT_COLLECTIBLES, null),
                CAT_FASHION, saveCategory(CAT_FASHION, null)
        );

        // Sub-categories for Electronics
        Category electronics = topCategories.get(CAT_ELECTRONICS);
        saveCategory("Cameras", electronics);
        saveCategory("Audio", electronics);
        saveCategory("Vintage Tech", electronics);
        saveCategory("Computers", electronics);

        // Sub-categories for Watches
        Category watches = topCategories.get(CAT_WATCHES);
        saveCategory("Luxury", watches);
        saveCategory("Vintage", watches);
        saveCategory("Smartwatches", watches);
        saveCategory("Accessories", watches);

        // Sub-categories for Art
        Category art = topCategories.get("Art");
        saveCategory("Prints", art);
        saveCategory("Street Art", art);
        saveCategory("Contemporary", art);
        saveCategory("Photography", art);

        // Sub-categories for Furniture
        Category furniture = topCategories.get(CAT_FURNITURE);
        saveCategory("Chairs", furniture);
        saveCategory("Tables", furniture);
        saveCategory("Storage", furniture);
        saveCategory("Lighting", furniture);

        // Sub-categories for Books
        Category books = topCategories.get(CAT_BOOKS);
        saveCategory("First Editions", books);
        saveCategory("Rare Books", books);
        saveCategory("Comics", books);
        saveCategory("Magazines", books);

        // Sub-categories for Musical Instruments
        Category music = topCategories.get(CAT_MUSICAL_INSTRUMENTS);
        saveCategory("Guitars", music);
        saveCategory("Synthesizers", music);
        saveCategory("Drums", music);
        saveCategory("Studio Gear", music);

        // Sub-categories for Gaming
        Category gaming = topCategories.get(CAT_GAMING);
        saveCategory("Consoles", gaming);
        saveCategory("Games", gaming);
        saveCategory(CAT_COLLECTIBLES, gaming);
        saveCategory("Arcade", gaming);

        // Sub-categories for Jewelry
        Category jewelry = topCategories.get(CAT_JEWELRY);
        saveCategory("Rings", jewelry);
        saveCategory("Necklaces", jewelry);
        saveCategory(CAT_WATCHES, jewelry);
        saveCategory("Earrings", jewelry);

        // Sub-categories for Collectibles
        Category collectibles = topCategories.get(CAT_COLLECTIBLES);
        saveCategory("Sports Memorabilia", collectibles);
        saveCategory("Trading Cards", collectibles);
        saveCategory("Toys", collectibles);
        saveCategory("Coins & Stamps", collectibles);

        // Sub-categories for Fashion
        Category fashion = topCategories.get(CAT_FASHION);
        saveCategory("Designer Bags", fashion);
        saveCategory("Vintage", fashion);
        saveCategory("Sneakers", fashion);
        saveCategory("Accessories", fashion);

        // --- Listings ---
        LocalDateTime now = LocalDateTime.now();
        Category camerasCat = categoryRepository.findByName("Cameras").orElse(electronics);

        Listing lst001 = createListing(
                sarah, camerasCat, "Vintage Leica M3 Camera — 1954 First Edition",
                "Authentic 1954 Leica M3 double-stroke rangefinder camera. Serial number 700xxx. Includes original leather case and 50mm Summicron f/2 lens. Professionally serviced in 2025.",
                "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=800&q=80",
                new BigDecimal("2800"), new BigDecimal("3850"), new BigDecimal("3500"),
                now.plusHours(2).plusMinutes(30), ListingStatus.ACTIVE, 23, elena);

        Listing lst002 = createListing(
                sarah, topCategories.get(CAT_FURNITURE), "Herman Miller Eames Lounge Chair — Walnut",
                "Iconic Eames Lounge Chair and Ottoman in genuine walnut veneer with black leather upholstery. Authentic Herman Miller piece from authorized dealer. Purchased 2022.",
                "https://images.unsplash.com/photo-1567538096630-e0c55bd6374c?w=800&q=80",
                new BigDecimal("4500"), new BigDecimal("5200"), null,
                now.plusHours(18), ListingStatus.ACTIVE, 15, elena);

        Listing lst003 = createListing(
                marcus, topCategories.get(CAT_BOOKS), "First Edition Hemingway — The Sun Also Rises",
                "True first edition, first printing of Ernest Hemingway's masterpiece published by Charles Scribner's Sons, 1926. Dust jacket intact with minor wear. Authenticated.",
                "https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=800&q=80",
                new BigDecimal("15000"), new BigDecimal("18750"), new BigDecimal("17500"),
                now.plusDays(3), ListingStatus.ACTIVE, 8, marcus);

        Listing lst004 = createListing(
                elena, topCategories.get(CAT_WATCHES), "Rolex Submariner Date — 41mm Stainless",
                "2021 Rolex Submariner Date ref. 126610LN. Complete set with box, papers, and warranty card. Worn only 6 months. No scratches on crystal or clasp.",
                "https://images.unsplash.com/photo-1587836374828-4dbafa94cf0e?w=800&q=80",
                new BigDecimal("12000"), new BigDecimal("14200"), new BigDecimal("13500"),
                now.plusMinutes(30), ListingStatus.ACTIVE, 31, sarah);

        Listing lst005 = createListing(
                sarah, topCategories.get("Art"), "Original Banksy Print — Girl with Balloon",
                "Authenticated Banksy screenprint. 50x70cm on cotton rag paper. Signed in pencil. Pest Control verified. Comes with certificate of authenticity.",
                "https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=800&q=80",
                new BigDecimal("250000"), new BigDecimal("285000"), new BigDecimal("280000"),
                now.plusDays(5), ListingStatus.ACTIVE, 12, elena);

        Listing lst006 = createListing(
                marcus, topCategories.get(CAT_MUSICAL_INSTRUMENTS), "Gibson Les Paul Standard '59 Reissue",
                "Gibson Custom Shop 60th Anniversary 1959 Les Paul Standard. Historic Teambuilt model with accurate '59 specs. tobacco burst finish. Hardshell case included.",
                "https://images.unsplash.com/photo-1564186763535-ebb21ef5277f?w=800&q=80",
                new BigDecimal("3500"), new BigDecimal("4100"), null,
                now.plusHours(8), ListingStatus.ACTIVE, 19, marcus);

        Listing lst007 = createListing(
                sarah, topCategories.get(CAT_GAMING), "Nintendo Sealed DS Lite — Arctic White",
                "Brand new, factory sealed Nintendo DS Lite in Arctic White. Japanese region free. Original sticker on back has been preserved. Collector grade.",
                "https://images.unsplash.com/photo-1531525645387-7f14be1bdbbd?w=800&q=80",
                new BigDecimal("800"), new BigDecimal("1150"), new BigDecimal("900"),
                now.plusHours(4), ListingStatus.ACTIVE, 27, sarah);

        Listing lst008 = createListing(
                elena, topCategories.get(CAT_WATCHES), "Omega Speedmaster Moonwatch — Hesalite",
                "Omega Speedmaster Professional Moonwatch with hesalite crystal. Caliber 3861 manual movement. Full kit: box, papers, NATO strap, bracelet. Worn twice.",
                "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800&q=80",
                new BigDecimal("5500"), new BigDecimal("6200"), null,
                now.plusDays(2), ListingStatus.ACTIVE, 14, marcus);

        // --- Bids ---
        createBid(lst001, sarah, new BigDecimal("3850"), now.minusMinutes(30));
        createBid(lst001, sarah, new BigDecimal("3700"), now.minusHours(1));
        createBid(lst001, marcus, new BigDecimal("3550"), now.minusHours(2));
        createBid(lst001, elena, new BigDecimal("3400"), now.minusHours(3));
        createBid(lst001, sarah, new BigDecimal("3200"), now.minusHours(5));

        createBid(lst004, sarah, new BigDecimal("14200"), now.minusMinutes(6));
        createBid(lst004, marcus, new BigDecimal("14000"), now.minusMinutes(18));
        createBid(lst004, elena, new BigDecimal("13800"), now.minusMinutes(30));

        createBid(lst002, elena, new BigDecimal("5200"), now.minusHours(2));
        createBid(lst003, marcus, new BigDecimal("18750"), now.minusDays(1));

        log.info("Database seeding complete!");
    }

    private User createUser(String email, String password, String displayName,
                           String photoUrl, Role role, double walletBalance) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setDisplayName(displayName);
        user.setPhotoUrl(photoUrl);
        user.setRole(role);
        user.setDefault2FAMethod(MFAType.DISABLED);
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(java.time.Instant.now());
                User saved = userRepository.save(user);

                if (walletBalance > 0) {
                        walletService.deposit(saved.getId(), BigDecimal.valueOf(walletBalance));
                }

                return saved;
    }

    private Category saveCategory(String name, Category parent) {
        Category category = new Category();
        category.setName(name);
        category.setParentCategory(parent);
        return categoryRepository.save(category);
    }

    private Listing createListing(User seller, Category category, String title, String description,
                                  String imageUrl, BigDecimal startingPrice, BigDecimal currentPrice,
                                  BigDecimal reservePrice, LocalDateTime endTime,
                                  ListingStatus status, int bidCount, User topBidder) {
        Listing listing = new Listing();
        listing.setSellerId(seller.getId());
        listing.setCategory(category);
        listing.setTitle(title);
        listing.setDescription(description);
        listing.setImageUrl(imageUrl);
        listing.setStartingPrice(startingPrice);
        listing.setReservePrice(reservePrice != null ? reservePrice : startingPrice);
        listing.setMinBidIncrement(BigDecimal.ONE);
        listing.setCurrentPrice(currentPrice);
        listing.setBidCount(bidCount);
        listing.setStartTime(LocalDateTime.now().minusDays(1));
        listing.setEndTime(endTime);
        listing.setStatus(status);
        listing.setWinnerId(topBidder != null ? topBidder.getId() : null);
        return listingRepository.save(listing);
    }

    private void createBid(Listing listing, User bidder, BigDecimal amount, LocalDateTime createdAt) {
        Bid bid = new Bid();
        bid.setListingId(listing.getId());
        bid.setBidderId(bidder.getId());
        bid.setAmount(amount);
        bid.setMaxAmount(amount);
        bid.setSource(BidSource.MANUAL);
        bid.setStatus(BidStatus.ACCEPTED);
        bid.setCreatedAt(createdAt);
        bidRepository.save(bid);
    }
}
