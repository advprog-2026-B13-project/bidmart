use axum::{
    routing::get,
    Router,
};
use std::net::SocketAddr;

#[tokio::main]
async fn main() {
    // Initialize tracing
    tracing_subscriber::fmt::init();

    // Build our application with routes
    let app = Router::new()
        .route("/", get(health_check))
        .route("/health", get(health_check));

    // Run our app with hyper
    let addr = SocketAddr::from(([127, 0, 0, 1], 8080));
    let listener = tokio::net::TcpListener::bind(&addr)
        .await
        .expect("Failed to bind to port 8080");

    tracing::info!("Server running at http://{}", addr);

    axum::serve(listener, app)
        .await
        .expect("Server error");
}

async fn health_check() -> &'static str {
    "Auction Service is running!"
}
