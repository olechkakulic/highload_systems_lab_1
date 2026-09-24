#!/usr/bin/env python3
"""Create demo data and verify both main workflows against the running API."""
import json
import sys
import uuid
from datetime import datetime, timedelta, timezone
from urllib.request import Request, urlopen
from urllib.error import HTTPError

base = (sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080").rstrip("/")
suffix = uuid.uuid4().hex[:10]


def call(method, path, data=None, expected=200):
    payload = None if data is None else json.dumps(data).encode("utf-8")
    request = Request(base + path, data=payload, method=method, headers={"Content-Type": "application/json"})
    try:
        response = urlopen(request, timeout=15)
    except HTTPError as error:
        response = error
    with response:
        body = response.read()
        if response.status != expected:
            raise RuntimeError(f"{method} {path}: expected {expected}, got {response.status}: {body.decode()}")
        return (json.loads(body) if body else None), response.headers


def create(path, data):
    body, headers = call("POST", path, data, 201)
    assert headers.get("Location"), "Missing Location"
    return body["id"]


shelter = create("/api/shelters", {"name": "Добрый дом " + suffix, "address": "Лесная улица, 1"})
tag = create("/api/tags", {"name": "Ласковый " + suffix})
users = {}
for name, role in [("owner1", "APPLICANT"), ("owner2", "APPLICANT"), ("volunteer1", "VOLUNTEER"), ("volunteer2", "VOLUNTEER")]:
    users[name] = create("/api/users", {"name": name, "email": f"{name}-{suffix}@example.org", "role": role})
animal = create("/api/animals", {"name": "Барсик", "species": "CAT", "birthDate": "2022-01-01", "description": "Дружелюбный кот", "shelterId": shelter, "tagIds": [tag]})
applications = [create("/api/applications", {"animalId": animal, "applicantId": users[name], "comment": "Готов заботиться"}) for name in ("owner1", "owner2")]
call("POST", "/api/applications", {"animalId": animal, "applicantId": users["owner1"], "comment": "Повтор"}, 409)
call("POST", f"/api/applications/{applications[0]}/review", {"decision": "APPROVE"})
call("POST", f"/api/applications/{applications[0]}/complete")
assert call("GET", f"/api/animals/{animal}")[0]["status"] == "ADOPTED"
assert call("GET", f"/api/applications/{applications[0]}")[0]["status"] == "COMPLETED"
assert call("GET", f"/api/applications/{applications[1]}")[0]["status"] == "REJECTED"
call("POST", f"/api/applications/{applications[0]}/complete", expected=409)

start = datetime.now(timezone.utc) + timedelta(days=1)
shift = create("/api/shifts", {"shelterId": shelter, "title": "Уход за животными", "startsAt": start.isoformat(), "endsAt": (start + timedelta(hours=2)).isoformat(), "capacity": 1})
p1 = create(f"/api/shifts/{shift}/participations", {"userId": users["volunteer1"]})
call("POST", f"/api/shifts/{shift}/participations", {"userId": users["volunteer2"]}, 409)
call("POST", f"/api/shifts/{shift}/participations/{p1}/cancel")
p2 = create(f"/api/shifts/{shift}/participations", {"userId": users["volunteer2"]})
body, headers = call("GET", f"/api/animals?shelterId={shelter}&size=1")
assert headers.get("X-Total-Count") == "1"
body, headers = call("GET", f"/api/shifts?shelterId={shelter}&size=1")
assert headers.get("X-Total-Count") is None and "hasNext" in body and "totalElements" not in body
call("GET", "/api/animals?size=51", expected=400)
print(json.dumps({"result": "OK", "shelterId": shelter, "animalId": animal, "applicationIds": applications, "shiftId": shift, "participationIds": [p1, p2], "swagger": base + "/swagger-ui/index.html"}, ensure_ascii=False, indent=2))
