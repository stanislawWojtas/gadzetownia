#!/usr/bin/env python3
"""
Generuje wersję startową (dla studentów) z wersji pełnej (gałąź `solution`).

W plikach .kt wersji pełnej każde rozwiązanie jest otoczone markerami:

    // region SOLUTION TODO 3
    ...kod rozwiązania...
    // endregion
    // region STUB TODO 3
    // // TODO(UCZESTNIK) 3: opis zadania
    // // wskazówka
    // return RefreshResult.Success(0)
    // endregion

Skrypt:
  * usuwa regiony SOLUTION (razem z markerami),
  * w regionach STUB usuwa markery i jeden poziom komentarza `// `
    (linie `// // ...` stają się komentarzem `// ...`, linie `// kod` stają się kodem),
  * kopiuje resztę projektu bez zmian (bez katalogów build, .gradle, .idea, tools/ itp.).

Użycie:
  python tools/make_starter.py --out ../gadzetownia-starter
  python tools/make_starter.py --out /tmp/check --solved 3        # starter z rozwiązaną tylko luką 3
  python tools/make_starter.py --out /tmp/check --solved 1,2,7

Opcja --solved zostawia rozwiązania wskazanych luk (do sprawdzania, że każda luka
zmienia na zielono tylko „swój” test).
"""
from __future__ import annotations

import argparse
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ALL_TODOS = set(range(1, 8))

# Katalogi i pliki, których nie kopiujemy do wersji startowej
EXCLUDED_DIRS = {".git", ".gradle", ".idea", ".kotlin", "build", "captures", "tools", "starter-out", "__pycache__"}
EXCLUDED_FILES = {"local.properties"}

RE_SOLUTION = re.compile(r"^\s*// region SOLUTION TODO (\d+)\s*$")
RE_STUB = re.compile(r"^\s*// region STUB TODO (\d+)\s*$")
RE_END = re.compile(r"^\s*// endregion\s*$")
RE_COMMENTED = re.compile(r"^(\s*)// ?(.*)$")


class MarkerError(Exception):
    pass


def transform(text: str, solved: set[int], path: str) -> tuple[str, set[int]]:
    """Zwraca przetworzony tekst i numery luk, które w nim wystąpiły."""
    out: list[str] = []
    found: set[int] = set()
    mode: str | None = None  # None / "solution" / "stub"
    number = 0

    for lineno, line in enumerate(text.splitlines(keepends=True), start=1):
        bare = line.rstrip("\r\n")
        newline = line[len(bare):]

        m_sol, m_stub = RE_SOLUTION.match(bare), RE_STUB.match(bare)
        if m_sol or m_stub:
            if mode is not None:
                raise MarkerError(f"{path}:{lineno}: zagnieżdżony region (poprzedni nie został zamknięty)")
            mode = "solution" if m_sol else "stub"
            number = int((m_sol or m_stub).group(1))
            found.add(number)
            continue
        if mode is not None and RE_END.match(bare):
            mode = None
            continue

        if mode is None:
            out.append(line)
        elif mode == "solution":
            if number in solved:
                out.append(line)
        else:  # stub
            if number in solved:
                continue
            m = RE_COMMENTED.match(bare)
            if not m:
                raise MarkerError(f"{path}:{lineno}: linia w regionie STUB musi zaczynać się od '// '")
            out.append(m.group(1) + m.group(2) + newline if m.group(2) else newline)

    if mode is not None:
        raise MarkerError(f"{path}: niezamknięty region TODO {number}")
    return "".join(out), found


def project_files() -> list[Path]:
    """Pliki projektu: z gita (śledzone + nowe, bez ignorowanych), a bez gita przejście po katalogach."""
    try:
        listing = subprocess.run(
            ["git", "ls-files", "-co", "--exclude-standard", "-z"],
            cwd=ROOT, check=True, capture_output=True,
        ).stdout.decode("utf-8")
        files = [Path(p) for p in listing.split("\0") if p]
    except (OSError, subprocess.CalledProcessError):
        files = []
        for dirpath, dirnames, filenames in os.walk(ROOT):
            dirnames[:] = [d for d in dirnames if d not in EXCLUDED_DIRS]
            for name in filenames:
                files.append(Path(dirpath, name).relative_to(ROOT))

    result = []
    for rel in files:
        if any(part in EXCLUDED_DIRS for part in rel.parts) or rel.name in EXCLUDED_FILES:
            continue
        if (ROOT / rel).is_file():
            result.append(rel)
    return sorted(result)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--out", required=True, help="katalog wynikowy (zostanie wyczyszczony, poza .git)")
    parser.add_argument("--solved", default="", help="numery luk, których rozwiązanie zostawić, np. 1,3")
    args = parser.parse_args()

    solved = {int(x) for x in args.solved.split(",") if x.strip()}
    out_dir = Path(args.out).resolve()
    if out_dir == ROOT or ROOT in out_dir.parents:
        if out_dir.name != "starter-out" and "starter-out" not in out_dir.parts:
            print("Katalog wynikowy nie może leżeć w projekcie (poza ./starter-out).", file=sys.stderr)
            return 2

    # Czyścimy katalog wynikowy, zostawiając ewentualne repozytorium git
    if out_dir.exists():
        for child in out_dir.iterdir():
            if child.name == ".git":
                continue
            if child.is_dir():
                shutil.rmtree(child)
            else:
                child.unlink()
    out_dir.mkdir(parents=True, exist_ok=True)

    found_all: set[int] = set()
    for rel in project_files():
        src, dst = ROOT / rel, out_dir / rel
        dst.parent.mkdir(parents=True, exist_ok=True)
        if rel.suffix == ".kt":
            text = src.read_text(encoding="utf-8")
            new_text, found = transform(text, solved, str(rel))
            found_all |= found
            if "region SOLUTION" in new_text or "region STUB" in new_text:
                raise MarkerError(f"{rel}: po przetworzeniu zostały markery regionów")
            with open(dst, "w", encoding="utf-8", newline="") as f:
                f.write(new_text)
        else:
            shutil.copy2(src, dst)

    missing = ALL_TODOS - found_all
    if missing:
        print(f"UWAGA: brak markerów dla luk: {sorted(missing)}", file=sys.stderr)
        return 1

    stubs = sorted(ALL_TODOS - solved)
    print(f"Wersja startowa zapisana w: {out_dir}")
    print(f"Zaślepki TODO(UCZESTNIK): {stubs}" + (f", rozwiązane zostawione: {sorted(solved)}" if solved else ""))
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except MarkerError as e:
        print(f"BŁĄD: {e}", file=sys.stderr)
        sys.exit(1)
