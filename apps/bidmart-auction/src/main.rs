use axum::{routing::get, Json, Router};
use serde_json::{json, Value};
use std::net::SocketAddr;

#[tokio::main]
async fn main() {
    tracing_subscriber::fmt::init();

    let app = Router::new()
        .route("/", get(health_check))
        .route("/health", get(health_check))
        .route("/api/health/status", get(chained_health));

    let addr = SocketAddr::from(([127, 0, 0, 1], 8081));
    let listener = tokio::net::TcpListener::bind(&addr)
        .await
        .expect("Failed to bind to port 8081");

    tracing::info!("Server running at http://{}", addr);

    axum::serve(listener, app).await.expect("Server error");
}

async fn health_check() -> &'static str {
    "Auction Service is running!"
}

async fn chained_health() -> Json<Value> {
    let core_url = std::env::var("CORE_SERVICE_URL")
        .unwrap_or_else(|_| "http://localhost:8080".to_string());

    let url = format!("{}/api/health/status", core_url);

    match reqwest::get(&url).await {
        Ok(resp) => match resp.json::<Value>().await {
            Ok(core_status) => Json(json!({
                "service": "bidmart-auction",
                "status": "UP",
                "core": core_status
            })),
            Err(e) => Json(json!({
                "service": "bidmart-auction",
                "status": "UP",
                "core": { "status": "ERROR", "error": e.to_string() }
            })),
        },
        Err(e) => Json(json!({
            "service": "bidmart-auction",
            "status": "UP",
            "core": { "status": "UNREACHABLE", "error": e.to_string() }
        })),
    }
}
