"""Unified error handling: every error response is `{ code, message, details }`.

Matches the shared `Error` schema in `api/openapi.yaml`. `ApiError` is raised anywhere
in the service; `register_error_handlers` wires it (and FastAPI's built-ins) to that body.
"""

from __future__ import annotations

from fastapi import FastAPI, Request
from fastapi.encoders import jsonable_encoder
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException


class ApiError(Exception):
    def __init__(self, status_code: int, code: str, message: str, details: dict | None = None):
        self.status_code = status_code
        self.code = code
        self.message = message
        self.details = details
        super().__init__(message)


def unauthorized(message: str = "Unauthorized") -> ApiError:
    return ApiError(401, "UNAUTHORIZED", message)


def bad_request(message: str, details: dict | None = None) -> ApiError:
    return ApiError(400, "BAD_REQUEST", message, details)


def _body(code: str, message: str, details: dict | None) -> dict:
    body: dict = {"code": code, "message": message}
    if details is not None:
        body["details"] = details
    return body


def register_error_handlers(app: FastAPI) -> None:
    @app.exception_handler(ApiError)
    async def _api_error(_: Request, exc: ApiError) -> JSONResponse:
        return JSONResponse(
            status_code=exc.status_code,
            content=_body(exc.code, exc.message, exc.details),
        )

    @app.exception_handler(StarletteHTTPException)
    async def _http_error(_: Request, exc: StarletteHTTPException) -> JSONResponse:
        return JSONResponse(
            status_code=exc.status_code,
            content=_body("HTTP_ERROR", str(exc.detail), None),
        )

    @app.exception_handler(RequestValidationError)
    async def _validation_error(_: Request, exc: RequestValidationError) -> JSONResponse:
        return JSONResponse(
            status_code=422,
            content=_body(
                "VALIDATION_ERROR",
                "Request validation failed",
                {"errors": jsonable_encoder(exc.errors())},
            ),
        )
