#!/usr/bin/python
# -*- coding: utf-8  -*-
"""Generate metric-only weather boxes for Romanian localities.

The script accepts the usual Pywikibot generator arguments.  If no generator
is given, it processes every ro.wikipedia page whose Wikidata item has a
SIRUTA code (P843), using the pattern already used by the other scripts in
this directory.

For each page a Wikidata geo-shape (P3896) is used as an area when available;
otherwise the coordinates are looked up on Wikidata (P625), with a fallback to
the coordinates stored on the wiki page.  The bundled :mod:`ro_climate`
client is queried for the requested years and a metric-only ``{{Weather box}}``
is added to the page: monthly record and average temperatures, precipitation
normals, and precipitation days.

Pages that already contain a weather box are skipped.  The box is appended to
the ``Climă``/``Climat`` section when one exists; otherwise a ``Climă``
subsection is created at the end of ``Geografie``.  If neither section exists,
a ``Geografie``/``Geografia`` section at the same level is inserted immediately
before the ``Demografie``/``Demografia``/``Date demografice`` section.

The year range defaults to 2001-2025 and can be changed with
``-start-year:`` and ``-end-year:``.  The range must lie within the data,
which start in 1961 and are only used through the last complete year.

Examples::

    python pwb.py wikiro/robots/python/pwb/localitati/meteo \
        -page:"Cluj-Napoca"
    python pwb.py wikiro/robots/python/pwb/localitati/meteo \
        -page:"Cluj-Napoca" -start-year:1991 -end-year:2020
    python pwb.py wikiro/robots/python/pwb/localitati/meteo \
        -cat:"Orașe în România" -limit:10
    python pwb.py wikiro/robots/python/pwb/localitati/meteo
"""

from __future__ import annotations

import itertools
import json
import re
import sys
import unicodedata
from datetime import date
from pathlib import Path
from typing import Iterable

import mwparserfromhell
import pywikibot
from pywikibot import pagegenerators


def load_ro_climate():
    """Return the bundled client class, error class, and default store URL.

    The imports are deferred so pages that already have a weather box can be
    skipped without loading pandas and zarr.
    """
    try:
        from ro_climate import DEFAULT_STORE, RoClimate, RoClimateError
    except ModuleNotFoundError as exc:
        if exc.name != "ro_climate":
            raise
        # The library lives in the repository's meteo/ directory but is not
        # necessarily installed in the environment running this script.
        _REPO_ROOT = Path(__file__).resolve().parents[5]
        sys.path.insert(0, str(_REPO_ROOT / "meteo"))
        from ro_climate import DEFAULT_STORE, RoClimate, RoClimateError
    return RoClimate, RoClimateError, DEFAULT_STORE


DEFAULT_START_YEAR = 2001
DEFAULT_END_YEAR = 2025
FIRST_DATA_YEAR = 1961
PRECIPITATION_DAYS_THRESHOLD = 1.0

SIRUTA_PROPERTY = "P843"
COORDINATE_PROPERTY = "P625"
GEOSHAPE_PROPERTY = "P3896"

CLIMATE_VARIABLES = ("max_temp", "min_temp", "precipitation")
MONTH_NAMES = (
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

CLIMATE_SECTION_TITLES = {"climă", "clima", "climat", "date climatice", "date meteo"}
GEOGRAPHY_SECTION_TITLES = {"geografie", "geografia", "date geografice", "descriere geografica"}
DEMOGRAPHY_SECTION_TITLES = {"demografie", "demografia", "date demografice"}
SECTION_PATTERN = re.compile(
    r"^(={2,6})\s*(.+?)\s*\1\s*$", re.MULTILINE
)

SIRUTA_PAGE_QUERY = """
SELECT DISTINCT ?item ?siruta WHERE {
  ?item wdt:P843 ?siruta .
  ?article schema:about ?item ;
           schema:isPartOf <https://ro.wikipedia.org/> .
}
ORDER BY ?siruta
"""


def siruta_page_generator(
    site: pywikibot.site.BaseSite,
) -> Iterable[pywikibot.page.Page]:
    """Yield all pages whose Wikidata item has a SIRUTA code."""
    return pagegenerators.WikidataSPARQLPageGenerator(
        SIRUTA_PAGE_QUERY,
        site=site,
        result_type=list,
    )


def first_claim_value(claims: dict, prop: str):
    """Return the first non-empty target for ``prop`` in ``claims``."""
    for claim in claims.get(prop, []):
        try:
            value = claim.getTarget()
        except Exception as exc:  # malformed or deleted statement
            pywikibot.warning(f"Could not read {prop} claim: {exc}")
            continue
        if value is not None:
            return value
    return None


def get_siruta_and_location(
    page: pywikibot.page.Page,
) -> tuple[str | None, float | None, float | None, str | None]:
    """Return ``(siruta, lat, lon, geoshape)`` for a page.

    Wikidata is preferred because the SIRUTA code, the geo-shape and the
    coordinates are normally stored there.  Coordinates from the wiki page's
    GeoData are used as a fallback.  ``geoshape`` is the Commons ``Data:``
    page title of the P3896 value, when present.
    """
    siruta = None
    geoshape = None

    try:
        item = page.data_item()
        claims = item.claims
    except Exception as exc:
        pywikibot.warning(
            f"Could not load Wikidata item for {page.title()}: {exc}"
        )
        claims = {}

    if claims:
        siruta_value = first_claim_value(claims, SIRUTA_PROPERTY)
        if siruta_value is not None:
            siruta = str(siruta_value)

        geoshape_value = first_claim_value(claims, GEOSHAPE_PROPERTY)
        if geoshape_value is not None:
            title = (
                geoshape_value.toWikibase()
                if hasattr(geoshape_value, "toWikibase")
                else str(geoshape_value)
            )
            geoshape = str(title) if title else None

        coordinate = first_claim_value(claims, COORDINATE_PROPERTY)
        if isinstance(coordinate, pywikibot.Coordinate):
            return (
                siruta,
                float(coordinate.lat),
                float(coordinate.lon),
                geoshape,
            )

    coordinate = page.coordinates(primary_only=True)
    if coordinate is None:
        coordinates = page.coordinates()
        coordinate = coordinates[0] if coordinates else None
    if coordinate is not None:
        return siruta, float(coordinate.lat), float(coordinate.lon), geoshape

    return siruta, None, None, geoshape


def ring_area(ring: list[list[float]]) -> float:
    """Return the absolute planar area of a closed ring."""
    area = 0.0
    for (x1, y1), (x2, y2) in zip(ring, ring[1:] + ring[:1]):
        area += float(x1) * float(y2) - float(x2) * float(y1)
    return abs(area)


def get_geoshape_polygon(title: str) -> list[tuple[float, float]] | None:
    """Return a Commons geo-shape as ``(lon, lat)`` vertices.

    Geo-shapes are stored as JSON in the Commons ``Data:`` namespace.  The
    :meth:`RoClimate.area` API accepts a single outer ring, so for
    multipolygon shapes the ring with the largest area is used.
    """
    site = pywikibot.Site("commons", "commons")
    payload = json.loads(pywikibot.Page(site, title).get())
    collection = payload.get("data") or payload
    rings: list[list[list[float]]] = []
    for feature in collection.get("features", []):
        geometry = feature.get("geometry") or {}
        coordinates = geometry.get("coordinates") or []
        if geometry.get("type") == "Polygon" and coordinates:
            rings.append(coordinates[0])
        elif geometry.get("type") == "MultiPolygon":
            rings.extend(polygon[0] for polygon in coordinates if polygon)
    if not rings:
        return None
    ring = max(rings, key=ring_area)
    points = [(float(lon), float(lat)) for lon, lat in ring]
    if len(points) < 3:
        return None
    return points


def is_weather_box_template(
    template: mwparserfromhell.nodes.Template,
) -> bool:
    """Return whether a template is the ``Weather box`` template."""
    name = str(template.name).strip().replace("_", " ")
    prefix, separator, remainder = name.partition(":")
    if separator and prefix.strip().casefold() in {"template", "format"}:
        name = remainder
    return name.replace(" ", "").casefold() == "weatherbox"


def has_weather_box(text: str) -> bool:
    """Return whether the page text already contains a Weather box."""
    code = mwparserfromhell.parse(text)
    return any(
        is_weather_box_template(template)
        for template in code.filter_templates(recursive=True)
    )


def normalize_heading(title: str) -> str:
    """Return a heading title without case or diacritics."""
    text = unicodedata.normalize("NFKD", title)
    text = "".join(
        character for character in text
        if not unicodedata.combining(character)
    )
    return " ".join(text.casefold().split())


def find_section(
    text: str, titles: set[str]
) -> tuple[int, int, int, str] | None:
    """Return ``(start, end, level, title)`` for the first matching section.

    ``end`` is the position of the next heading at the same or a higher
    level, or the end of the text.
    """
    headings = list(SECTION_PATTERN.finditer(text))
    for index, match in enumerate(headings):
        level = len(match.group(1))
        title = match.group(2).strip()
        if normalize_heading(title) not in titles:
            continue
        end = len(text)
        for following in headings[index + 1:]:
            if len(following.group(1)) <= level:
                end = following.start()
                break
        return match.start(), end, level, title
    return None


def insert_block(text: str, end: int, block: str) -> str:
    """Insert ``block`` before ``end``, keeping paragraph spacing."""
    prefix = text[:end]
    suffix = text[end:]
    if prefix.endswith("\n\n"):
        lead = ""
    elif prefix.endswith("\n"):
        lead = "\n"
    else:
        lead = "\n\n"
    trail = "\n\n" if suffix else "\n"
    return prefix + lead + block + trail + suffix


def add_weather_box(text: str, box: str) -> tuple[str, str] | None:
    """Return ``(new_text, location)`` with the box added, or ``None``.

    The box is appended to a ``Climă``/``Climat`` section when one exists.
    Otherwise a ``Climă`` subsection is created at the end of ``Geografie``.
    If neither exists, a ``Geografie``/``Geografia`` section at the same level
    is inserted immediately before the demography section.
    """
    section = find_section(text, CLIMATE_SECTION_TITLES)
    if section is not None:
        _start, end, _level, title = section
        return insert_block(text, end, box), f"section '{title}'"

    section = find_section(text, GEOGRAPHY_SECTION_TITLES)
    if section is not None:
        _start, end, level, title = section
        heading = "=" * (level + 1)
        normalized = normalize_heading(title)
        climate = "Clima" if normalized == "geografia" else "Climă"
        block = f"{heading} {climate} {heading}\n\n{box}"
        return insert_block(text, end, block), f"subsection under '{title}'"

    section = find_section(text, DEMOGRAPHY_SECTION_TITLES)
    if section is None:
        return None
    start, _end, level, title = section
    heading = "=" * level
    normalized = normalize_heading(title)
    geography = "Geografia" if normalized == "demografia" else "Geografie"
    block = f"{heading} {geography} {heading}\n\n{box}"
    return insert_block(text, start, block), f"section before '{title}'"


def parse_year_options(args: list[str]) -> tuple[int, int, list[str]]:
    """Return ``(start_year, end_year, remaining_args)``."""
    start_year = DEFAULT_START_YEAR
    end_year = DEFAULT_END_YEAR
    remaining: list[str] = []

    for arg in args:
        matched = False
        for name in ("start-year", "end-year"):
            for separator in (":", "="):
                prefix = f"-{name}{separator}"
                if not arg.startswith(prefix):
                    continue
                value = int(arg[len(prefix):])
                if name == "start-year":
                    start_year = value
                else:
                    end_year = value
                matched = True
                break
            if matched:
                break
        if not matched:
            remaining.append(arg)

    return start_year, end_year, remaining


def last_complete_year(frontier: pd.Timestamp) -> int:
    """Return the last year fully covered by the data."""
    if frontier.month == 12 and frontier.day == 31:
        return frontier.year
    return frontier.year - 1


def validate_year_range(
    start_year: int, end_year: int, last_year: int
) -> None:
    """Raise ``ValueError`` when the requested years are not usable."""
    if start_year < FIRST_DATA_YEAR:
        raise ValueError(
            f"start year {start_year} is before the data start "
            f"({FIRST_DATA_YEAR})"
        )
    if end_year > last_year:
        raise ValueError(
            f"end year {end_year} is after the last complete data year "
            f"({last_year})"
        )
    if start_year > end_year:
        raise ValueError(
            f"start year {start_year} is after end year {end_year}"
        )


def load_climate_frame(
    climate: RoClimate,
    lat: float | None,
    lon: float | None,
    polygon: list[tuple[float, float]] | None,
    start_year: int,
    end_year: int,
) -> pd.DataFrame:
    """Return the daily series used to build the weather box.

    The frame has columns ``max_temp``, ``min_temp``, ``precipitation``,
    ``record_max_temp`` and ``record_min_temp``.  For a polygon the first
    three are spatial means and the record columns are spatial extremes;
    for a point all columns are the cell's own daily values.
    """
    import pandas as pd

    start = f"{start_year}-01-01"
    end = f"{end_year}-12-31"

    if polygon is not None:
        area = climate.area(
            polygon=polygon,
            start=start,
            end=end,
            variables=CLIMATE_VARIABLES,
            reductions=("mean", "max", "min"),
        )
        return pd.DataFrame(
            {
                "max_temp": area.data["max_temp_mean"],
                "min_temp": area.data["min_temp_mean"],
                "precipitation": area.data["precipitation_mean"],
                "record_max_temp": area.data["max_temp_max"],
                "record_min_temp": area.data["min_temp_min"],
            }
        )

    point = climate.point(
        lon,
        lat,
        start=start,
        end=end,
        variables=CLIMATE_VARIABLES,
    )
    return pd.DataFrame(
        {
            "max_temp": point.data["max_temp"],
            "min_temp": point.data["min_temp"],
            "precipitation": point.data["precipitation"],
            "record_max_temp": point.data["max_temp"],
            "record_min_temp": point.data["min_temp"],
        }
    )


def monthly_weather_values(frame: pd.DataFrame) -> dict[str, pd.Series]:
    """Return monthly weather-box values keyed by parameter suffix."""
    month = frame.index.month
    daily_mean = (frame["max_temp"] + frame["min_temp"]) / 2
    monthly_precipitation = frame["precipitation"].resample("MS").sum(
        min_count=1
    )
    precip_days = (
        frame["precipitation"] >= PRECIPITATION_DAYS_THRESHOLD
    ).groupby([frame.index.year, frame.index.month]).sum()

    values = {
        "record high C": frame["record_max_temp"].groupby(month).max(),
        "high C": frame["max_temp"].groupby(month).mean(),
        "mean C": daily_mean.groupby(month).mean(),
        "low C": frame["min_temp"].groupby(month).mean(),
        "record low C": frame["record_min_temp"].groupby(month).min(),
        "precipitation mm": monthly_precipitation.groupby(
            monthly_precipitation.index.month
        ).mean(),
        "precipitation days": precip_days.groupby(level=1).mean(),
    }
    return {
        name: series.reindex(range(1, 13))
        for name, series in values.items()
    }


def format_number(value: float) -> str | None:
    """Format one monthly value with one decimal, or ``None`` if missing."""
    import pandas as pd

    if value is None or pd.isna(value):
        return None
    return f"{float(value):.1f}".replace(".", ",")


def weather_box_source() -> str:
    """Return the weather-box source citation."""
    return (
        "Administrația Națională de Meteorologie (MeteoRomania), "
        "CC BY 4.0<ref>{{Citat web |title=ANM daily gridded "
        "meteorological cube |url=https://stac.eocube.ro/collections/"
        "anm-daily-grids/items/anm-daily-grids-open-v1 "
        "|publisher=EOCube.RO |via=data.gov.ro "
        "|access-date=" + date.today().isoformat() + "}}</ref>"
    )


def build_weather_box(
    title: str,
    values: dict[str, pd.Series],
    start_year: int,
    end_year: int,
) -> str:
    """Return the metric-only ``{{Weather box}}`` wikitext."""
    lines = [
        "{{Weather box",
        f"|location={title} (temperaturi și precipitații {start_year}–{end_year})",
        "|metric first=yes",
        "|automatic=yes",
    ]

    for parameter in (
        "record high C",
        "high C",
        "mean C",
        "low C",
        "record low C",
    ):
        for month_name, month_number in zip(MONTH_NAMES, range(1, 13)):
            value = format_number(values[parameter].get(month_number))
            if value is not None:
                lines.append(f"|{month_name} {parameter}={value}")

    lines.append("|precipitation colour=green")
    for month_name, month_number in zip(MONTH_NAMES, range(1, 13)):
        value = format_number(values["precipitation mm"].get(month_number))
        if value is not None:
            lines.append(f"|{month_name} precipitation mm={value}")

    threshold = f"{PRECIPITATION_DAYS_THRESHOLD:.1f}".replace(".", ",")
    lines.append(f"|unit precipitation days={threshold} mm")
    for month_name, month_number in zip(MONTH_NAMES, range(1, 13)):
        value = format_number(values["precipitation days"].get(month_number))
        if value is not None:
            lines.append(f"|{month_name} precipitation days={value}")

    lines.append(f"|source 1={weather_box_source()}")
    lines.append("}}")
    return "\n".join(lines)


class ClimateSessionError(RuntimeError):
    """Raised when the climate client cannot be initialized."""


class ClimateSession:
    """Create the climate client only when a page actually needs data."""

    def __init__(self, start_year: int, end_year: int) -> None:
        """Remember the requested years for later validation."""
        self.start_year = start_year
        self.end_year = end_year
        self._client = None
        self._ready = False
        self._error = None

    def get(self):
        """Return the client, creating it on first use.

        Raises
        ------
        ClimateSessionError
            If the library cannot be imported, the client cannot be created,
            or the requested years are outside the available data.
        """
        if self._ready:
            if self._error is not None:
                raise ClimateSessionError(self._error)
            return self._client
        self._ready = True

        try:
            RoClimate, RoClimateError, default_store = load_ro_climate()
        except ImportError as exc:
            self._error = f"Could not import ro_climate: {exc}"
            raise ClimateSessionError(self._error) from exc

        try:
            # Use the published store URL; STAC discovery adds a round trip
            # to every invocation and the URL is stable.
            client = RoClimate(store_url=default_store)
        except Exception as exc:
            self._error = f"Could not create the climate client: {exc}"
            raise ClimateSessionError(self._error) from exc

        try:
            frontier = min(client.frontiers().values())
            validate_year_range(
                self.start_year,
                self.end_year,
                last_complete_year(frontier),
            )
        except (RoClimateError, ValueError) as exc:
            self._error = str(exc)
            client.close()
            raise ClimateSessionError(self._error) from exc

        self._client = client
        return client

    def close(self) -> None:
        """Close the climate client if it was created."""
        if self._client is not None:
            self._client.close()
            self._client = None


def process_page(
    page: pywikibot.page.Page,
    session: ClimateSession,
    start_year: int,
    end_year: int,
) -> None:
    """Add a metric-only weather box to one page."""
    try:
        text = page.get()
    except pywikibot.exceptions.IsRedirectPageError:
        try:
            page = page.getRedirectTarget()
            text = page.get()
        except Exception as exc:
            pywikibot.error(f"Skipping {page.title()}: {exc}")
            return
    except Exception as exc:
        pywikibot.error(f"Could not read {page.title()}: {exc}")
        return

    if has_weather_box(text):
        pywikibot.warning(
            f"Skipping {page.title()}: Weather box already exists"
        )
        return

    section = find_section(text, CLIMATE_SECTION_TITLES)
    if section is None:
        section = find_section(text, GEOGRAPHY_SECTION_TITLES)
    if section is None:
        section = find_section(text, DEMOGRAPHY_SECTION_TITLES)
    if section is None:
        pywikibot.warning(
            f"Skipping {page.title()}: no Climă/Climat, Geografie, or "
            "Demografie section"
        )
        return

    try:
        _siruta, lat, lon, geoshape = get_siruta_and_location(page)
    except Exception as exc:
        pywikibot.error(f"Could not get location for {page.title()}: {exc}")
        lat, lon, geoshape = None, None, None

    polygon = None
    if geoshape:
        try:
            polygon = get_geoshape_polygon(geoshape)
        except Exception as exc:
            pywikibot.warning(
                f"Could not load geoshape {geoshape} for "
                f"{page.title()}: {exc}"
            )
        if polygon is None:
            pywikibot.warning(
                f"{page.title()}: geoshape {geoshape} is unusable; "
                "falling back to coordinates"
            )

    if polygon is None and (lat is None or lon is None):
        pywikibot.warning(f"{page.title()}: no coordinates found")
        return

    climate = session.get()
    _RoClimate, RoClimateError, _DEFAULT_STORE = load_ro_climate()
    try:
        frame = load_climate_frame(
            climate, lat, lon, polygon, start_year, end_year
        )
    except RoClimateError as exc:
        pywikibot.error(f"{page.title()}: {exc}")
        return

    values = monthly_weather_values(frame)
    box = build_weather_box(page.title(), values, start_year, end_year)
    result = add_weather_box(text, box)
    if result is None:
        pywikibot.warning(
            f"Skipping {page.title()}: no place to add the weather box"
        )
        return

    new_text, location = result
    pywikibot.output(f"{page.title()}: adding weather box to {location}")
    try:
        page.put(new_text, "Adaug {{weather box}}", minor=False)
    except Exception as exc:
        pywikibot.error(f"Could not save {page.title()}: {exc}")


def main(*args: str) -> None:
    """Process command line arguments and run the script."""
    local_args = pywikibot.handle_args(args)
    try:
        start_year, end_year, local_args = parse_year_options(local_args)
    except ValueError as exc:
        pywikibot.error(f"Invalid year option: {exc}")
        return

    generator_factory = pagegenerators.GeneratorFactory()
    local_args = generator_factory.handle_args(local_args)
    for arg in local_args:
        pywikibot.warning(f"Unknown argument ignored: {arg}")

    site = pywikibot.Site()
    generator = generator_factory.getCombinedGenerator()
    if generator is None:
        pywikibot.output(
            "No generator given; processing all pages with a SIRUTA code."
        )
        generator = siruta_page_generator(site)
        if generator_factory.limit:
            generator = itertools.islice(generator, generator_factory.limit)

    session = ClimateSession(start_year, end_year)
    try:
        for page in generator:
            process_page(page, session, start_year, end_year)
    except ClimateSessionError as exc:
        pywikibot.error(str(exc))
    finally:
        session.close()


if __name__ == "__main__":
    main()
