#!/usr/bin/env python3
# -*- coding: utf-8 -*-

############################################################################
## Add wikidata=* tags to the OpenStreetMap elements listed in a CSV      ##
## file.                                                                  ##
##                                                                        ##
## The CSV must contain at least the columns @type (node/way/relation),   ##
## @id (the OSM object id) and qid (the Wikidata item, e.g. Q12345).      ##
## Additional columns are ignored.                                        ##
##                                                                        ##
## If the CSV has an optional siruta column, its value is compared with   ##
## the siruta:code tag of the element; rows whose element has a different ##
## or missing siruta:code are skipped instead of being tagged.            ##
##                                                                        ##
## Elements that already carry a wikidata tag are never modified and do   ##
## not count towards --count: the script keeps reading rows until it has  ##
## found --count elements that actually need the tag.                     ##
##                                                                        ##
## This program is free software: you can redistribute it and/or modify   ##
## it under the terms of the GNU General Public License as published by   ##
## the Free Software Foundation, version 2.                               ##
##                                                                        ##
## This program is distributed in the hope that it will be useful,        ##
## but WITHOUT ANY WARRANTY; without even the implied warranty of         ##
## MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          ##
## GNU General Public License for more details.                           ##
##                                                                        ##
############################################################################

"""Add ``wikidata=*`` tags to OSM elements listed in a CSV file.

Run it first with ``--dry-run``: that mode only reads from the API, never
opens a changeset and does not need an access token.

Editing requires an OAuth 2.0 access token with the ``write_api`` scope
(username/password authentication was shut down by OpenStreetMap in 2024).
Register an application at https://www.openstreetmap.org/oauth2/applications,
obtain a token (for example with https://github.com/Zverik/cli-oauth2) and
either export it as OSM_API_TOKEN or save it in a file passed with
``--token-file``.

Part of https://wiki.openstreetmap.org/wiki/Automated_Edits/Strainubot/wikidata_id
"""

import argparse
import csv
import logging
import os
import re
import sys
import time
from dataclasses import dataclass
from typing import Any, Callable, Sequence

import osmapi
import requests

API_URL = 'https://api.openstreetmap.org'
WIKI_URL = (
    'https://wiki.openstreetmap.org/wiki/Automated_Edits/Strainubot/wikidata_id'
)
CHANGESET_COMMENT = f'Add wikidata=* from SIRUTA name matching, see {WIKI_URL}'
APP_ID = 'osm_add_wikidata.py (wikiro)'

ELEMENT_TYPES = ('node', 'way', 'relation')
REQUIRED_COLUMNS = ('@type', '@id', 'qid')
BATCH_SIZE = 100
MAX_CHANGESET_ELEMENTS = 10000

QID_RE = re.compile(r'^[Qq](\d+)$')
QID_URL_RE = re.compile(
    r'^https?://(?:www\.)?wikidata\.org/(?:wiki|entity)/[Qq]?(\d+)/?$'
)

Element = dict[str, Any]
Pending = tuple['Row', Element]


class CsvFormatError(Exception):
    """The CSV file does not have the expected layout."""


@dataclass
class Row:
    """A single usable line of the input CSV."""

    el_type: str
    oid: int
    qid: str
    line: int
    siruta: str | None = None


@dataclass
class Stats:
    """Counters used for the final report."""

    scanned: int = 0
    already: int = 0
    conflicts: int = 0
    missing: int = 0
    siruta: int = 0
    updated: int = 0
    failed: int = 0


def info(message: str) -> None:
    print(message)


def warn(message: str) -> None:
    print(f'warning: {message}', file=sys.stderr)


def error(message: str) -> None:
    print(f'error: {message}', file=sys.stderr)


def normalize_qid(value: str) -> str | None:
    """Return a canonical ``Q123`` string, or None if value is not a QID."""
    value = value.strip()
    match = QID_RE.match(value)
    if match is None:
        match = QID_URL_RE.match(value)
    if match is None:
        return None
    return f'Q{int(match.group(1))}'


def has_qid(value: str, qid: str) -> bool:
    """Tell whether an existing wikidata value already contains qid."""
    return qid.upper() in {part.strip().upper() for part in value.split(';')}


def _normalize_code(value: str) -> str:
    """Normalize a SIRUTA code so that leading zeros do not matter."""
    value = value.strip()
    return str(int(value)) if value.isdigit() else value.casefold()


def siruta_mismatch(row: Row, element: Element) -> str | None:
    """Compare the optional CSV siruta value with the element's siruta:code.

    Returns a description of the mismatch, or None when the row has no
    siruta value or when both values agree.
    """
    if not row.siruta:
        return None

    actual = ((element.get('tag') or {}).get('siruta:code') or '').strip()
    if not actual:
        return f'CSV says siruta {row.siruta}, but the element has no siruta:code tag'
    if _normalize_code(actual) != _normalize_code(row.siruta):
        return f'CSV says siruta {row.siruta}, but the element has siruta:code={actual}'
    return None


def read_rows(path: str, encoding: str) -> tuple[list[Row], int]:
    """Parse and validate the input CSV.

    Returns the usable rows and the number of lines that were rejected.
    Duplicated (type, id) pairs are kept once.
    """
    rows: list[Row] = []
    seen: dict[tuple[str, int], tuple[str, str | None]] = {}
    invalid = 0

    try:
        handle = open(path, newline='', encoding=encoding)
    except OSError as exc:
        raise CsvFormatError(f'cannot open {path}: {exc}') from exc

    with handle:
        reader = csv.reader(handle)
        try:
            header = next(reader)
        except StopIteration:
            return [], 0

        columns = {name.strip().lstrip('\ufeff'): index for index, name in enumerate(header)}
        missing = [name for name in REQUIRED_COLUMNS if name not in columns]
        if missing:
            raise CsvFormatError(
                f'{path} is missing the column(s) {", ".join(missing)}; '
                f'found {", ".join(columns) or "no columns"}'
            )
        has_siruta = 'siruta' in columns

        def cell(raw: Sequence[str], name: str) -> str:
            index = columns[name]
            return raw[index].strip() if index < len(raw) else ''

        for raw in reader:
            line = reader.line_num
            if not any(value.strip() for value in raw):
                continue

            el_type = cell(raw, '@type').lower()
            if el_type not in ELEMENT_TYPES:
                warn(f'line {line}: unknown @type {el_type!r}, skipped')
                invalid += 1
                continue

            id_text = cell(raw, '@id')
            try:
                oid = int(id_text)
            except ValueError:
                warn(f'line {line}: @id {id_text!r} is not an integer, skipped')
                invalid += 1
                continue
            if oid <= 0:
                warn(f'line {line}: @id must be positive, got {oid}, skipped')
                invalid += 1
                continue

            qid = normalize_qid(cell(raw, 'qid'))
            if qid is None:
                warn(f'line {line}: qid {cell(raw, "qid")!r} is not a Wikidata QID, skipped')
                invalid += 1
                continue

            key = (el_type, oid)
            siruta = (cell(raw, 'siruta') or None) if has_siruta else None
            if key in seen:
                seen_qid, seen_siruta = seen[key]
                if seen_qid != qid:
                    warn(
                        f'line {line}: {el_type}/{oid} already listed with '
                        f'{seen_qid}, ignoring the duplicate with {qid}'
                    )
                elif seen_siruta != siruta:
                    warn(
                        f'line {line}: {el_type}/{oid} already listed with '
                        f'siruta {seen_siruta}, ignoring the duplicate with '
                        f'{siruta}'
                    )
                continue
            seen[key] = (qid, siruta)
            rows.append(Row(el_type, oid, qid, line, siruta))

    return rows, invalid


def api_call(func: Callable[..., Any], *args: Any) -> Any:
    """Call func, retrying a few times when the API rate limits us."""
    for attempt in range(1, 4):
        try:
            return func(*args)
        except osmapi.ApiError as exc:
            if getattr(exc, 'status', 0) == 429 and attempt < 3:
                pause = 5 * 2**attempt
                warn(f'rate limited by the API, waiting {pause}s before retrying')
                time.sleep(pause)
                continue
            raise
    raise AssertionError('unreachable')


def _getters(api: osmapi.OsmApi, el_type: str) -> tuple[Callable[..., Any], Callable[..., Any]]:
    """Return the multi-fetch and the update functions for an element type."""
    fetchers = {
        'node': api.nodes_get,
        'way': api.ways_get,
        'relation': api.relations_get,
    }
    updaters = {
        'node': api.node_update,
        'way': api.way_update,
        'relation': api.relation_update,
    }
    return fetchers[el_type], updaters[el_type]


def fetch_elements(api: osmapi.OsmApi, el_type: str, ids: list[int]) -> dict[int, Element]:
    """Fetch elements by id, falling back to single requests if some are missing.

    The multi-fetch endpoint answers 404 for the whole batch when one of the
    ids does not exist, so a failed batch is retried one id at a time.
    """
    if not ids:
        return {}
    fetch, _ = _getters(api, el_type)
    try:
        return api_call(fetch, ids)
    except osmapi.ElementNotFoundApiError:
        result: dict[int, Element] = {}
        for oid in ids:
            try:
                result.update(api_call(fetch, [oid]))
            except (osmapi.ElementNotFoundApiError, osmapi.ElementDeletedApiError):
                continue
        return result


def describe(element: Element) -> str:
    """Build a short human readable label for log messages."""
    tags = element.get('tag') or {}
    parts = [f'{key}={tags[key]}' for key in ('name', 'siruta:code') if key in tags]
    return ', '.join(parts)


def describe_dry_run(row: Row, element: Element) -> str:
    """Label for --dry-run lines, including the siruta value from the CSV."""
    parts = [describe(element)]
    if row.siruta:
        parts.append(f'CSV siruta={row.siruta}')
    label = ', '.join(part for part in parts if part)
    return f' ({label})' if label else ''


def print_changeset_summary(pending: list[Pending]) -> None:
    """Show what the changeset of a dry run would contain."""
    counts = {el_type: 0 for el_type in ELEMENT_TYPES}
    for row, _ in pending:
        counts[row.el_type] += 1
    details = ', '.join(
        f'{el_type}s: {counts[el_type]}'
        for el_type in ELEMENT_TYPES
        if counts[el_type]
    )
    info('')
    info(f'Changeset (dry-run, not created): {len(pending)} change(s)')
    info(f'  comment = {CHANGESET_COMMENT}')
    info('  bot     = yes')
    info(f'  elements = {details}')


def plan(
    api: osmapi.OsmApi, rows: list[Row], limit: int, verbose: bool
) -> tuple[list[Pending], Stats]:
    """Read the CSV rows and collect up to limit elements that need the tag."""
    stats = Stats()
    pending: list[Pending] = []
    index = 0

    while index < len(rows) and len(pending) < limit:
        size = min(BATCH_SIZE, limit - len(pending))
        chunk = rows[index:index + size]
        index += len(chunk)

        by_type: dict[str, list[int]] = {}
        for row in chunk:
            by_type.setdefault(row.el_type, []).append(row.oid)

        elements: dict[tuple[str, int], Element] = {}
        for el_type, ids in by_type.items():
            for oid, element in fetch_elements(api, el_type, ids).items():
                elements[(el_type, oid)] = element

        for row in chunk:
            stats.scanned += 1
            element = elements.get((row.el_type, row.oid))
            if element is None:
                stats.missing += 1
                warn(f'line {row.line}: {row.el_type}/{row.oid} does not exist, skipped')
                continue
            if not element.get('visible', True):
                stats.missing += 1
                warn(f'line {row.line}: {row.el_type}/{row.oid} is deleted, skipped')
                continue

            existing = (element.get('tag') or {}).get('wikidata', '').strip()
            if existing:
                if has_qid(existing, row.qid):
                    stats.already += 1
                    if verbose:
                        info(
                            f'skip {row.el_type}/{row.oid}: already has '
                            f'wikidata={existing}'
                        )
                else:
                    stats.conflicts += 1
                    warn(
                        f'line {row.line}: {row.el_type}/{row.oid} already has '
                        f'wikidata={existing}, not overwriting it with {row.qid}'
                    )
                continue

            mismatch = siruta_mismatch(row, element)
            if mismatch is not None:
                stats.siruta += 1
                warn(f'line {row.line}: {row.el_type}/{row.oid}: {mismatch}, skipped')
                continue

            pending.append((row, element))
            if len(pending) >= limit:
                break

    return pending, stats


def _update_element(
    api: osmapi.OsmApi, row: Row, element: Element
) -> str:
    """Add the wikidata tag to element and upload it.

    Returns one of 'updated', 'already', 'conflict', 'missing', 'siruta'
    or 'failed'.
    """
    _, update = _getters(api, row.el_type)
    candidate = dict(element)
    tags = dict(candidate.get('tag') or {})
    tags['wikidata'] = row.qid
    candidate['tag'] = tags

    try:
        api_call(update, candidate)
        return 'updated'
    except osmapi.VersionMismatchApiError:
        pass
    except osmapi.ChangesetClosedApiError:
        raise
    except osmapi.ApiError as exc:
        error(f'{row.el_type}/{row.oid}: update failed: {exc}')
        return 'failed'

    # The element changed between the planning phase and the upload. Refresh
    # it and try once more, unless somebody else has tagged it meanwhile.
    warn(f'{row.el_type}/{row.oid}: changed on the server, re-reading it')
    fresh = fetch_elements(api, row.el_type, [row.oid]).get(row.oid)
    if fresh is None or not fresh.get('visible', True):
        warn(f'{row.el_type}/{row.oid}: no longer exists, skipped')
        return 'missing'

    existing = (fresh.get('tag') or {}).get('wikidata', '').strip()
    if existing:
        if has_qid(existing, row.qid):
            return 'already'
        warn(
            f'{row.el_type}/{row.oid}: now has wikidata={existing}, '
            f'not overwriting it with {row.qid}'
        )
        return 'conflict'

    mismatch = siruta_mismatch(row, fresh)
    if mismatch is not None:
        warn(f'{row.el_type}/{row.oid}: {mismatch}, skipped')
        return 'siruta'

    tags = dict(fresh.get('tag') or {})
    tags['wikidata'] = row.qid
    fresh['tag'] = tags
    try:
        api_call(update, fresh)
        return 'updated'
    except osmapi.ApiError as exc:
        error(f'{row.el_type}/{row.oid}: update failed after refresh: {exc}')
        return 'failed'


def changeset_url(api_url: str, changeset_id: int) -> str:
    """Return the web page of a changeset."""
    if api_url.rstrip('/') == API_URL:
        return f'https://www.openstreetmap.org/changeset/{changeset_id}'
    return f'{api_url.rstrip("/")}/changeset/{changeset_id}'


def apply_changes(
    api: osmapi.OsmApi, pending: list[Pending], delay: float, api_url: str
) -> tuple[Stats, int | None]:
    """Upload every pending change inside a single changeset."""
    stats = Stats()
    if not pending:
        return stats, None

    changeset_id = api_call(
        api.changeset_create, {'comment': CHANGESET_COMMENT, 'bot': 'yes'}
    )
    info(f'Opened changeset {changeset_id}: {changeset_url(api_url, changeset_id)}')

    try:
        for row, element in pending:
            if delay:
                time.sleep(delay)
            try:
                outcome = _update_element(api, row, element)
            except osmapi.ChangesetClosedApiError:
                warn(
                    f'changeset {changeset_id} was closed by the server, stopping'
                )
                break

            if outcome == 'updated':
                stats.updated += 1
                label = describe(element)
                suffix = f' ({label})' if label else ''
                info(f'{row.el_type}/{row.oid} -> {row.qid}{suffix}')
            elif outcome == 'already':
                stats.already += 1
            elif outcome == 'conflict':
                stats.conflicts += 1
            elif outcome == 'missing':
                stats.missing += 1
            elif outcome == 'siruta':
                stats.siruta += 1
            else:
                stats.failed += 1
    finally:
        try:
            api_call(api.changeset_close)
            info(f'Closed changeset {changeset_id}')
        except osmapi.ChangesetClosedApiError:
            warn(f'changeset {changeset_id} was already closed')
        except osmapi.ApiError as exc:
            error(f'could not close changeset {changeset_id}: {exc}')

    return stats, changeset_id


def report(
    plan_stats: Stats,
    apply_stats: Stats | None,
    planned: int,
    dry_run: bool,
) -> None:
    """Print the final counters."""
    if apply_stats is None:
        already = plan_stats.already
        conflicts = plan_stats.conflicts
        missing = plan_stats.missing
        siruta = plan_stats.siruta
    else:
        already = plan_stats.already + apply_stats.already
        conflicts = plan_stats.conflicts + apply_stats.conflicts
        missing = plan_stats.missing + apply_stats.missing
        siruta = plan_stats.siruta + apply_stats.siruta

    info('')
    info(f'Rows scanned:          {plan_stats.scanned}')
    info(f'Already tagged:        {already}')
    info(f'Conflicting wikidata:  {conflicts}')
    info(f'Missing/deleted:       {missing}')
    info(f'SIRUTA mismatch:       {siruta}')
    if dry_run:
        info(f'Would update:          {planned}')
    else:
        updated = apply_stats.updated if apply_stats else 0
        failed = apply_stats.failed if apply_stats else 0
        info(f'Updated:               {updated}')
        info(f'Failed:                {failed}')


def load_token(args: argparse.Namespace) -> str | None:
    """Read the OAuth 2.0 access token from --token-file or OSM_API_TOKEN."""
    token = ''
    if args.token_file:
        try:
            with open(args.token_file, encoding='utf-8') as handle:
                token = handle.read().strip()
        except OSError as exc:
            error(f'cannot read token file {args.token_file}: {exc}')
            return None
    else:
        token = os.environ.get('OSM_API_TOKEN', '').strip()
    return token or None


def parse_args(argv: Sequence[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description='Add wikidata=* tags to OSM elements listed in a CSV file.',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=(
            'The CSV needs the columns @type, @id and qid; other columns are\n'
            'ignored. Elements that already have a wikidata tag are skipped\n'
            'and do not count towards --count. When the CSV also has a siruta\n'
            'column, it is checked against the siruta:code tag of the element\n'
            'and mismatches are skipped.\n\n'
            f'Changeset comment: {CHANGESET_COMMENT}\n\n'
            'Editing needs an OAuth 2.0 token with the write_api scope, from\n'
            'https://www.openstreetmap.org/oauth2/applications; pass it with\n'
            '--token-file or put it in the OSM_API_TOKEN environment variable.'
        ),
    )
    parser.add_argument('csv', help='input CSV file')
    parser.add_argument(
        '--dry-run',
        action='store_true',
        help='only read the API and report what would change; no token needed',
    )
    parser.add_argument(
        '--count',
        type=int,
        default=100,
        help='number of elements to modify in this run (default: 100)',
    )
    parser.add_argument(
        '--api',
        default=API_URL,
        help=f'API base URL (default: {API_URL})',
    )
    parser.add_argument(
        '--token-file',
        help='file containing an OAuth 2.0 access token (default: $OSM_API_TOKEN)',
    )
    parser.add_argument(
        '--encoding',
        default='utf-8-sig',
        help='CSV encoding (default: utf-8-sig, which also accepts plain UTF-8)',
    )
    parser.add_argument(
        '--delay',
        type=float,
        default=0.0,
        help='seconds to wait between two uploads (default: 0)',
    )
    parser.add_argument(
        '-v',
        '--verbose',
        action='store_true',
        help='explain why individual rows were skipped',
    )
    args = parser.parse_args(argv)

    if not 1 <= args.count <= MAX_CHANGESET_ELEMENTS:
        parser.error(f'--count must be between 1 and {MAX_CHANGESET_ELEMENTS}')
    args.api = args.api.rstrip('/')
    return args


def main(argv: Sequence[str] | None = None) -> int:
    args = parse_args(argv)

    # osmapi logs the raw reason of every 409 itself; we report those
    # situations in a more useful way below.
    logging.getLogger('osmapi').setLevel(logging.CRITICAL)

    try:
        rows, invalid = read_rows(args.csv, args.encoding)
    except CsvFormatError as exc:
        error(str(exc))
        return 1

    if invalid:
        warn(f'{invalid} invalid row(s) were skipped')
    if not rows:
        error('no usable rows found in the CSV file')
        return 1

    if args.dry_run:
        api = osmapi.OsmApi(api=args.api, appid=APP_ID, timeout=60)
    else:
        token = load_token(args)
        if token is None:
            error(
                'no OAuth 2.0 access token found; set OSM_API_TOKEN or use '
                '--token-file (see --help)'
            )
            return 1
        session = requests.Session()
        session.headers['Authorization'] = f'Bearer {token}'
        api = osmapi.OsmApi(
            api=args.api, appid=APP_ID, session=session, timeout=60
        )

    try:
        pending, stats = plan(api, rows, args.count, args.verbose)

        if args.dry_run:
            for row, element in pending:
                info(
                    f'[dry-run] {row.el_type}/{row.oid} -> {row.qid}'
                    f'{describe_dry_run(row, element)}'
                )
            if pending:
                print_changeset_summary(pending)
            else:
                info('')
                info('Changeset (dry-run, not created): no changes')
            report(stats, None, len(pending), dry_run=True)
            return 0

        if not pending:
            info('nothing to do')
            report(stats, None, 0, dry_run=False)
            return 0

        applied, changeset_id = apply_changes(
            api, pending, args.delay, args.api
        )
        report(stats, applied, len(pending), dry_run=False)
        if changeset_id is not None and applied.updated:
            info(f'Changeset: {changeset_url(args.api, changeset_id)}')
    except KeyboardInterrupt:
        warn('interrupted')
        return 1
    except osmapi.UnauthorizedApiError:
        error(
            'the API rejected the access token; it needs the write_api scope '
            'and must belong to the account that creates the changeset'
        )
        return 1
    except osmapi.ApiError as exc:
        error(f'OSM API error: {exc}')
        return 1

    return 0


if __name__ == '__main__':
    try:
        sys.stdout.reconfigure(errors='replace')
        sys.stderr.reconfigure(errors='replace')
    except (AttributeError, OSError):
        pass
    sys.exit(main())
