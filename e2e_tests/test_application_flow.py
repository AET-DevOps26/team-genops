"""
Job applications, end to end: browser cookie → gateway → application service → Postgres.

This is the path the unit tests cannot reach. `CookieToBearerFilterTest` proves the filter
synthesizes a header; `ApplicationControllerTest` proves the controller trusts the JWT `sub`.
Only here does a cookie minted by auth, translated by the gateway, and verified independently
by the application service have to line up for real.
"""

import httpx

from e2e_tests.conftest import BrowserSession, User


def _create(user: User, **overrides) -> httpx.Response:
    body = {
        "company": "Zalando",
        "job_title": "Backend Engineer",
        "job_description": "Build APIs in Java.",
    } | overrides
    return user.client.post("/api/v1/applications", json=body)


# ---------------------------------------------------------------- the edge


def test_applications_require_authentication(client: BrowserSession):
    """No cookie, no entry — the gateway rejects before the service is ever reached."""
    assert client.get("/api/v1/applications").status_code == 401


def test_a_garbage_token_is_rejected(client: BrowserSession):
    """The gateway verifies the RS256 signature against auth's JWKS; a forged cookie fails."""
    client.set_cookie("jr_access", "not-a-jwt")

    assert client.get("/api/v1/applications").status_code == 401


def test_an_authenticated_cookie_reaches_the_service(user: User):
    """
    The whole cookie→bearer bridge in one assertion: the client sends only a cookie, and a
    service that accepts nothing but `Authorization: Bearer` answers it.
    """
    assert user.client.get("/api/v1/applications").status_code == 200


# ---------------------------------------------------------------- create / read


def test_create_then_read_back(user: User):
    created = _create(user, job_description="Build APIs in Java.", notes="Referred by Sam")

    assert created.status_code == 201, created.text
    application_id = created.json()["id"]

    fetched = user.client.get(f"/api/v1/applications/{application_id}")

    assert fetched.status_code == 200
    body = fetched.json()
    assert body["company"] == "Zalando"
    assert body["job_title"] == "Backend Engineer"
    assert body["job_description"] == "Build APIs in Java."


def test_a_created_application_appears_in_the_list(user: User):
    application_id = _create(user).json()["id"]

    listed = user.client.get("/api/v1/applications")

    assert listed.status_code == 200
    assert application_id in [item["id"] for item in listed.json()["items"]]


def test_create_rejects_a_missing_required_field(user: User):
    response = user.client.post("/api/v1/applications", json={"company": "Zalando"})

    assert response.status_code == 422


def test_update_persists(user: User):
    application_id = _create(user).json()["id"]

    updated = user.client.put(
        f"/api/v1/applications/{application_id}",
        json={
            "company": "Zalando",
            "job_title": "Staff Engineer",
            "job_description": "Lead backend systems.",
            "stage": "interview",
        },
    )

    assert updated.status_code == 200
    reread = user.client.get(f"/api/v1/applications/{application_id}").json()
    assert reread["job_title"] == "Staff Engineer"
    assert reread["stage"] == "interview"


def test_delete_removes_it(user: User):
    application_id = _create(user).json()["id"]

    delete_response = user.client.delete(f"/api/v1/applications/{application_id}")
    assert delete_response.status_code in (200, 204)
    assert user.client.get(f"/api/v1/applications/{application_id}").status_code == 404


def test_an_unknown_id_is_not_found(user: User):
    assert user.client.get("/api/v1/applications/11111111-2222-3333-4444-555555555555").status_code == 404


# ---------------------------------------------------------------- ownership


def test_one_user_cannot_read_anothers_application(user: User, second_user: User):
    """
    The owner comes from the JWT `sub`, never from the request. Two real users, two real
    tokens, one shared database — a leak here is a cross-tenant data breach.
    """
    application_id = _create(user).json()["id"]

    response = second_user.client.get(f"/api/v1/applications/{application_id}")

    assert response.status_code == 404, "another user's application must not be readable"


def test_one_user_cannot_delete_anothers_application(user: User, second_user: User):
    application_id = _create(user).json()["id"]

    second_user.client.delete(f"/api/v1/applications/{application_id}")

    assert user.client.get(f"/api/v1/applications/{application_id}").status_code == 200, "the owner's data survived"


def test_a_users_list_contains_only_their_own(user: User, second_user: User):
    mine = _create(user, company="Mine").json()["id"]
    theirs = _create(second_user, company="Theirs").json()["id"]

    ids = [item["id"] for item in user.client.get("/api/v1/applications").json()["items"]]

    assert mine in ids
    assert theirs not in ids, "the list leaked another user's application"
