//! HTTP error mapping: converts AppError to Axum responses.

use axum::http::StatusCode;
use axum::response::{IntoResponse, Response};
use axum::Json;

use crate::app::error::AppError;

/// Wrapper to implement IntoResponse for AppError.
pub struct ApiError(pub AppError);

impl From<AppError> for ApiError {
    fn from(e: AppError) -> Self {
        ApiError(e)
    }
}

impl IntoResponse for ApiError {
    fn into_response(self) -> Response {    
        let (status, code, message) = match &self.0 {
            AppError::Validation(_) => (StatusCode::BAD_REQUEST, "VALIDATION", self.0.to_string()),
            AppError::NotFound(_) => (StatusCode::NOT_FOUND, "NOT_FOUND", self.0.to_string()),
            AppError::Unauthorized(_) => {
                (StatusCode::UNAUTHORIZED, "UNAUTHORIZED", self.0.to_string())
            }
            AppError::Internal(_) => {
                println!("CRITICAL ERROR: {}", self.0.to_string()); 
                // Generic message for internal errors to avoid leaking details
                (
                    StatusCode::INTERNAL_SERVER_ERROR,
                    "INTERNAL",
                    "An unexpected internal error occurred. Please try again later.".to_string(),
                )
            },
        };

        let body = serde_json::json!({
            "error": message,
            "code": code,
        });

        (status, Json(body)).into_response()
    }
}
