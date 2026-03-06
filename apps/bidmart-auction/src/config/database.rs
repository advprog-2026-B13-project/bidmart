// Database configuration and connection pool setup

use sqlx::postgres::{PgPool, PgPoolOptions};
use std::time::Duration;

use tracing::info;

pub async fn init_postgres_pool(database_url: &str, max_conn: u32) -> Result<PgPool, sqlx::Error> {
    info!("Connecting to PostgreSQL...");

    let pool = PgPoolOptions::new()
        .max_connections(max_conn)
        .min_connections(std::cmp::max(1, max_conn / 4))
        .acquire_timeout(Duration::from_secs(5))
        .idle_timeout(Duration::from_secs(600)) // 10 minutes
        // recycle connections every 30 minutes
        .max_lifetime(Duration::from_secs(1800))
        .test_before_acquire(true)
        .connect_lazy(database_url)?;

    info!("Connected to PostgreSQL successfully!");

    Ok(pool)
}
