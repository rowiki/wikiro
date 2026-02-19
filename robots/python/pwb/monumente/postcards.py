import json
import re
from datetime import datetime

import requests
from bs4 import BeautifulSoup

import pywikibot
from wikiro.robots.python.pwb.strainu_functions import stripLink

BASE_URL = "https://cartipostale.cimec.ro"
ID_REGEX = re.compile(r"id=([0-9]+)")
LMI_REGEX = "(([a-z]{1,2})-(i|ii|iii|iv)-([a-z])-([a-z])-([0-9]{5}(\.[0-9]{2,3})?))"
YEAR_REGEX = re.compile(r"19([0-9]{2})")
NONFREE_YEAR = 1986

def read_json(filename, what):
    try:
        f = open(filename, "r+")
        pywikibot.output("Reading " + what + " file...")
        db = json.load(f)
        pywikibot.output("...done")
        f.close()
        return db
    except IOError:
        pywikibot.error(
            "Failed to read " + filename + ". Trying to do without it."
            )
        return {}

class CimecPostcards:
    def __init__(self, base_url, id_regex):
        self.base_url = base_url
        self.id_regex = id_regex

    def get_search_results(self, search_url):
        """Get all links from a search results page."""
        resp = requests.get(self.base_url + "/" + search_url)
        resp.raise_for_status()
        soup = BeautifulSoup(resp.text, "html.parser")

        # links = []
        for a in soup.find_all("a", class_="stretched-link"):
            href = a.get("href")
            if href:
                yield self.base_url + "/" + href

    def parse_metadata_page(self, page_url):
        """Extract metadata and image URL from a single page."""
        resp = requests.get(page_url)
        resp.raise_for_status()
        soup = BeautifulSoup(resp.text, "html.parser")

        # Assuming metadata is in a table on the right
        metadata = {
            'url': page_url,
            'id': re.search(self.id_regex, page_url).group(1) if re.search(
                self.id_regex, page_url
            ) else None
        }
        # --- METADATA EXTRACTION ---
        # Find all <ul class="list-group list-group-flush">
        ul_blocks = soup.find_all("ul", class_="list-group list-group-flush")

        for ul in ul_blocks:
            for li in ul.find_all("li", class_="list-group-item"):
                # Try to find the key inside <b> or <strong>
                key_tag = li.find(["b", "strong"])
                if not key_tag:
                    continue
                key = key_tag.get_text(strip=True).rstrip(":")
                key_tag.extract()  # remove it to isolate value

                # Gather the remaining text (including inside <span>)
                value_parts = []
                # If there are <a> tags (e.g., descriptors or location links)
                links = li.find_all("a")
                if links:
                    value_parts.append(
                        "; ".join(a.get_text(strip=True) for a in links)
                    )

                # Add other textual content
                text_content = li.get_text(" ", strip=True)
                if text_content:
                    value_parts.append(text_content)

                value = " ".join(v for v in value_parts if v).strip()
                if not value:
                    continue

                # Some values might repeat or appear in multiple <ul> blocks
                metadata[key] = value

        # Find the main image
        image_url = None
        for img_tag in soup.find_all("a"):
            src = img_tag.get("data-src")
            if src and re.match(r"^/Colectia/[^/]+/[^/]+_a_.*\.jpg$", src):
                image_url = BASE_URL + src
                break
        metadata['image'] = image_url

        # Coordinates extraction
        for script in soup.find_all("script"):
            if script.string and "markerClusterGroup" in script.string:
                continue
            if script.string and "var map = L.map" in script.string:
                coord_match = re.search(
                    r"setView\(\s*\[\s*([-+]?[0-9]*\.?[0-9]+)\s*,\s*([-+]?[0-9]*\.?[0-9]+)\s*\]",
                    script.string
                )
                if coord_match:
                    metadata['latitude'] = float(coord_match.group(1))
                    metadata['longitude'] = float(coord_match.group(2))
                    break

        return metadata


def generate_wikicode(metadata):
    """Generate wikicode for a given metadata dictionary."""
    if not metadata:
        return ""

    copyright_template = ""
    if metadata.get('copyright') is True:
        copyright_template = "{{FOP România}}"
    elif metadata.get('copyright') is False:
        copyright_template = "{{tlc}}"

    code_txt = ""
    lmi_list = re.finditer(LMI_REGEX, metadata['Cod LMI'], re.IGNORECASE)
    for match in lmi_list:
        lmi_code = match.group(0).strip()
        code_txt += "{{Monument istoric|" + lmi_code + "}} "

    text = f"""== Informații ==
{{{{Informații
|      Descriere = {metadata.get('Descriere', '')} {code_txt}
|          Sursa = [{metadata.get('url', '')} cartipostale.cimec.ro]
|           Data = 
{metadata.get('Data tipăririi', 'pre ' + metadata.get('Data expedierii', '1986'))}
|     Localizare = {metadata.get('UAT', 'Republica Populară Română')}
|          Autor = {metadata.get('Fotograf', 'nemenționat')}
|     Permisiune = {{{{DP-ro-1956}}}}
|  alte_versiuni =
}}}}{{{{Location|{metadata.get('latitude', '')}|{metadata.get('longitude', '')}}}}}

== Licență ==
{{{{DP-ro-1956}}}}
{copyright_template}

[[Categorie:Cărți poștale Cimec]]"""
    return text

def upload_to_wikipedia(metadata, summary="Încarc carte poștală din colecția Cimec"):
    """Upload image to rowiki with given metadata."""
    site = pywikibot.Site("ro", "wikipedia")

    if not metadata or 'image' not in metadata or not metadata['image']:
        pywikibot.error("No image URL found in metadata.")
        return False

    image_url = metadata['image']
    alt_name = stripLink(metadata.get('name', 'N/A'))
    image_name = ((f"Carte poștală - "
                  f"{metadata.get('Nume actual subiect', alt_name)}"
                  f" - {metadata.get('id', 'unknown')}.jpg").replace('/', '_')
                  .replace('\'', '').replace('\\', '')).replace('"', '')
    wikicode = generate_wikicode(metadata)

    image_page = pywikibot.FilePage(site, image_name)
    if image_page.exists():
        pywikibot.output(f"Image {image_name} already exists on Wikipedia.")
        return False

    try:
        # Download the image temporarily
        temp_filename = f"./{image_name}"
        with requests.get(image_url, stream=True) as r:
            r.raise_for_status()
            with open(temp_filename, 'wb') as f:
                for chunk in r.iter_content(chunk_size=8192):
                    f.write(chunk)

        image_page.upload(
            source=temp_filename,
            comment=summary,
            text=wikicode,
            ignore_warnings=True
        )
        pywikibot.output(f"Uploaded {image_name} to Wikipedia.")
        return True
    except Exception as e:
        pywikibot.error(f"Failed to upload {image_name}: {e}")
        return False
    finally:
        # Clean up temporary file
        import os
        if os.path.exists(temp_filename):
            os.remove(temp_filename)


class GeneratorFilter:
    def __init__(self, generator: iter):
        self.generator = generator

    def __iter__(self):
        for item in self.generator:
            if self.filter_condition(item):
                yield item

    def filter_condition(self, item):
        return True

class LMIFilter(GeneratorFilter, CimecPostcards):
    def __init__(self, base_url: str, id_regex: re.Pattern, search_url: str,
                 lmi_file="ro_lmi_db.json"):
        super().__init__(self.get_search_results(search_url))

        self.base_url = base_url
        self.id_regex = id_regex

        data = read_json(lmi_file, "monument database")
        self.lmi = {}
        for entry in data:
            self.lmi[entry['Cod']] = entry

    def __iter__(self):
        for item in self.generator:
            metadata = self.parse_metadata_page(item)
            if self._filter_metadata(metadata):
                metadata['copyright'] = self.is_under_copyright(
                    metadata.get("Cod LMI", "")
                )
                metadata["name"] = self.get_lmi(
                    metadata.get("Cod LMI", "")
                ).get("Denumire", "N/A")
                yield metadata

    def get_lmi(self, code):
        return self.lmi.get(code) or {}

    def is_under_copyright(self, lmi_code):
        if lmi_code.find("-I-") != -1:
            return False
        if lmi_code not in self.lmi:
            return None
        copyright_year = self.lmi[lmi_code].get("Copyright", None)
        if copyright_year:
            if copyright_year in ["no", "nu"]:
                return False
            if int(copyright_year) >= datetime.now().year - 70:
                return True
            else:
                return False
        else:
            date = self.lmi[lmi_code].get("Datare", None)
            if date is not None and \
                date.find("sec") != -1 and \
                date.find("XX") != -1:
                return True
            if date is not None and \
                date.find("sec") == -1 and \
                re.search("19\d{2}", date) is not None:
                return True
        return None

    def filter_condition(self, item):
        metadata = self.parse_metadata_page(item)
        return self._filter_metadata(metadata)

    def _filter_metadata(self, metadata):
        #import pdb
        #pdb.set_trace()
        if not metadata:
            return False
        if "Cod LMI" not in metadata:
            return False
        lmi_list = re.finditer(LMI_REGEX, metadata['Cod LMI'], re.IGNORECASE)
        for match in lmi_list:
            lmi_code = match.group(0).strip()
            print(lmi_code, flush=True)
            if lmi_code not in self.lmi:
                pywikibot.output(f"LMI {lmi_code} not in database.")
                continue
            if self.is_under_copyright(lmi_code) and \
                    self.lmi[lmi_code].get("Imagine", "") != "":
                continue
                pywikibot.output(f"Skipping {lmi_code} with copyright "
                                 f"{self.lmi[lmi_code].get('Copyright')}.")
                return False
        return True

class PostcardFilter(LMIFilter):
    def _filter_metadata(self, metadata):
        if not metadata:
            return False
        if "Data tipăririi" in metadata:
            year = re.search(YEAR_REGEX,
                             metadata.get("Data tipăririi", ""))
            if year is not None:
                if int(year.group(1)) < NONFREE_YEAR:
                    return super()._filter_metadata(metadata)
                else:
                    pywikibot.output(f"Skipping postcard printed in {year}")
                    return False
        if "Data expedierii" in metadata:
            year = re.search(YEAR_REGEX,
                             metadata.get("Data expedierii", ""))
            if year is not None and int(year.group(1)) < NONFREE_YEAR:
                return super()._filter_metadata(metadata)

        if metadata.get("Stat") == "Republica Populară Română" or \
            metadata.get("Tip imagine") == "monocrom" or \
            (metadata.get("Descriptori") is not None and
             "Republica Populară Română" in metadata.get("Descriptori")):
            return super()._filter_metadata(metadata)
        else:
            return False

def file_upload_main():
    count = 0
    search_url = "Rezultate.php?criteriu=monument_istoric&display=1500"
    items = PostcardFilter(BASE_URL, ID_REGEX, search_url)

    for metadata in items:
        if not metadata:
            continue
        count += 1
        print(f"\nNr.: {count}")
        print("Metadata:")
        print(f"id: {metadata.get('id', 'N/A')}")
        #for k, v in metadata.items():
        #    print(f"  {k}: {v}")
        upload_to_wikipedia(metadata)
        #if count == 100:
        #    break

def data_gathering_main():
    count = 0
    search_url = "Rezultate.php?criteriu=monument_istoric&display=1500"
    items = LMIFilter(BASE_URL, ID_REGEX, search_url)
    data = {}

    for metadata in items:
        if not metadata:
            continue
        if "colaj" in metadata.get("Descriptori", "").lower():
            continue
        count += 1
        print(f"\nNr.: {count}")
        print(f"Autor: {metadata.get('Autor subiect', 'N/A')}")
        print(f"Coords: ({metadata.get('latitude', '0')},"
              f"{metadata.get('longitude', '0')})")
        lmi_list = re.finditer(LMI_REGEX, metadata['Cod LMI'], re.IGNORECASE)
        author = metadata.get('Autor subiect', None)
        if author:
            if re.match("arhitectul (.*)", author, re.IGNORECASE):
                author = re.match("arhitectul (.*)", author,
                              re.IGNORECASE).group(1) + " (arhitect)"
            author = author.replace(";", ",")
        for match in lmi_list:
            lmi_code = match.group(0).strip()
            data[lmi_code] = [{
                'Cod': lmi_code,
                'Lat': metadata.get('latitude', '0'),
                'Lon': metadata.get('longitude', '0'),
                'Creatori': author,
                'Source': 'postcards'
            }]
        #if count == 100:
        #    break

    with open("postcards_lmi.json", "w+") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)


if __name__ == "__main__":
    #file_upload_main()
    data_gathering_main()