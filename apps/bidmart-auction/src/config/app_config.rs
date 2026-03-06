use std::env;

#[derive(Debug, Clone)]
pub struct AppConfig {
    pub host: String,
    pub port: u16,
    pub database_url: String,

    #[allow(dead_code)] // future use
    pub core_url: String,
}

impl AppConfig {
    /// Load configuration from environment variables.
    ///
    /// Required env vars:
    /// - `DATABASE_URL`
    /// - `CORE_SERVICE_URL`
    ///
    /// Optional env vars (with defaults):
    /// - `APP_HOST` (default: "127.0.0.1")
    /// - `APP_PORT` (default: 8081)
    pub fn from_env() -> Self {
        dotenvy::dotenv().ok();
        Self {
            host: get_env("APP_HOST", "127.0.0.1"),
            port: get_env("APP_PORT", "8081")
                .parse()
                .expect("APP_PORT must be a valid u16"),
            database_url: get_env_required("DATABASE_URL"),
            core_url: get_env_required("CORE_SERVICE_URL"),
        }
    }
}

fn get_env(key: &str, default: &str) -> String {
    env::var(key).unwrap_or_else(|_| default.to_string())
}

fn get_env_required(key: &str) -> String {
    env::var(key).unwrap_or_else(|_| panic!("Environment variable {} must be set", key))
}
