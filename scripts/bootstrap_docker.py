#!/usr/bin/env python3
"""Install official Docker CLI plugins locally when Docker lacks Compose/Buildx."""
import hashlib
import json
import platform
from pathlib import Path
from urllib.request import Request, urlopen

root = Path(__file__).resolve().parents[1] / ".tools" / "docker"
plugins = root / "cli-plugins"
plugins.mkdir(parents=True, exist_ok=True)
system = platform.system().lower()
arch = platform.machine().lower()
if system not in ("darwin", "linux") or arch not in ("arm64", "aarch64", "x86_64", "amd64"):
    raise SystemExit("Установите Docker Compose и Buildx для вашей ОС и выполните docker compose up --build -d --wait")
arm = arch in ("arm64", "aarch64")
artifacts = [
    ("docker/compose", "v5.5.1", f"docker-compose-{system}-{'aarch64' if arm else 'x86_64'}", "docker-compose"),
    ("docker/buildx", "v0.37.1", f"buildx-v0.37.1.{system}-{'arm64' if arm else 'amd64'}", "docker-buildx"),
]
for repo, version, asset_name, command in artifacts:
    target = plugins / command
    stamp = plugins / (command + ".version")
    if target.exists() and stamp.exists() and stamp.read_text() == version:
        continue
    print(f"Загрузка официального {repo} {version} в {target}", flush=True)
    request = Request(f"https://api.github.com/repos/{repo}/releases/tags/{version}", headers={"User-Agent": "lab1-animal-shelter"})
    with urlopen(request, timeout=30) as response:
        release = json.load(response)
    asset = next(item for item in release["assets"] if item["name"] == asset_name)
    expected = asset.get("digest", "")
    if not expected.startswith("sha256:"):
        raise RuntimeError("В релизе отсутствует SHA256; загрузка не будет выполнена")
    with urlopen(asset["browser_download_url"], timeout=120) as response:
        data = response.read()
    actual = "sha256:" + hashlib.sha256(data).hexdigest()
    if actual != expected:
        raise RuntimeError("Контрольная сумма не совпадает: " + asset_name)
    target.write_bytes(data)
    target.chmod(0o755)
    stamp.write_text(version)
(root / "config.json").write_text(json.dumps({"cliPluginsExtraDirs": [str(plugins)]}) + "\n")
