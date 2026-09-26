import pywikibot
import re
import mwparserfromhell

def adjust_section_headers(content, level_change):
    """
    Adjusts the level of all section headers in the content by the specified level_change.
    Matches headers only when they are alone on a line.
    """
    def replace_header(match):
        header = match.group(1)
        new_level = len(header) + level_change
        print(f"Adjusting header '{match.group(2).strip()}' from level {len(header)} to {new_level}")
        if new_level < 1:
            new_level = 1
        return '=' * new_level + match.group(2).strip() + '=' * new_level

    # Match headers that are alone on a line
    return re.sub(r'^(=+)([^=]+)\1$', replace_header, content, flags=re.MULTILINE)

def get_interwiki_page(site, interwiki_link):
    """
    Resolves an interwiki link and returns the corresponding pywikibot.Page object.
    Handles cases where the target site is outside the current family.
    """
    if interwiki_link.startswith(':'):
        interwiki_link = interwiki_link[1:]  # Remove leading colon if present

    # Split the interwiki prefix and page title
    if ':' in interwiki_link:
        prefix, title = interwiki_link.split(':', 1)
        if prefix == 'mw':  # Special case for MediaWiki.org
            target_site = pywikibot.Site(code='en', fam='mediawiki')
        else:
            target_site = pywikibot.Site(code=prefix, fam=site.family.name)
        return pywikibot.Page(target_site, title)
    else:
        return pywikibot.Page(site, interwiki_link)


def adjust_templates(adjusted_content):
    """
    Adjusts the imported content by removing unwanted elements such as <languages/>,
    fixing template syntax, removing categories, removing <span id="..."></span>,
    and removing specific templates.
    """
    # Remove <languages/> (case-insensitive, allowing whitespace)
    adjusted_content = re.sub(r'\s*<languages\s*/>\s*\n?', '', adjusted_content, flags=re.IGNORECASE)

    # Fix template syntax (case-insensitive, allowing whitespace)
    adjusted_content = re.sub(r'\{\{\s*int\s*\|\s*', '{{int:', adjusted_content, flags=re.IGNORECASE)

    # Wrap {{PD Help Page}} in <noinclude> tags (case-insensitive, allowing whitespace)
    adjusted_content = re.sub(r'\{\{\s*PD\s+Help\s+Page\s*\}\}', '<noinclude>{{PD Help Page}}</noinclude>', adjusted_content, flags=re.IGNORECASE)

    # Remove categories (case-insensitive)
    adjusted_content = re.sub(r'\[\[\s*Category\s*:[^\]]+\]\]', '', adjusted_content, flags=re.IGNORECASE)

    # Remove <span id="..."></span> lines (case-insensitive, allowing whitespace)
    adjusted_content = re.sub(r'\n\s*<span id="[^"]+"></span>\s*\n', '\n', adjusted_content, flags=re.IGNORECASE)

    # Parse content with mwparserfromhell
    wikicode = mwparserfromhell.parse(adjusted_content)

    # Remove specific templates
    templates_to_remove = ["ombox", "VisualEditor Portal"]
    for template in wikicode.filter_templates():
        if template.name.matches(templates_to_remove):
            wikicode.remove(template)

    return str(wikicode)


def main():
    site = pywikibot.Site()
    template = 'Template:mwimport'
    template_page = pywikibot.Page(site, template)

    # Use the reference generator to retrieve pages transcluding the template
    pages = template_page.getReferences(only_template_inclusion=True)

    for page in pages:
        print(page)
        text = page.text
        wikicode = mwparserfromhell.parse(text)

        # Find the mwimport template
        mwimport_template = None
        for template in wikicode.filter_templates():
            if template.name.matches("mwimport"):
                mwimport_template = template
                break

        if not mwimport_template:
            pywikibot.output(f"No valid mwimport template found on page '{page.title()}'. Skipping.")
            continue

        # Extract parameters from the mwimport template
        try:
            nume = mwimport_template.get("nume").value.strip()
            nivel = int(mwimport_template.get("nivel").value.strip())
        except ValueError:
            pywikibot.output(f"Invalid parameters in mwimport template on page '{page.title()}'. Skipping.")
            continue

        # Fetch the content from the "nume" parameter (a wikilink)
        linked_page = get_interwiki_page(site, nume)
        if not linked_page.exists():
            pywikibot.output(f"Linked page '{nume}' does not exist. Skipping.")
            continue

        linked_content = linked_page.text

        # Adjust section header levels
        adjusted_content = adjust_section_headers(linked_content, nivel)
        adjusted_content = adjust_templates(adjusted_content)

        # Replace the content of the current page
        new_content = f"<noinclude>{mwimport_template}</noinclude>{adjusted_content}"
        if text != new_content:
            page.text = new_content
            page.save(summary="Updated page content with imported content and adjusted section headers.")

if __name__ == "__main__":
    main()
