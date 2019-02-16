#!/usr/bin/python
# -*- coding: utf8 -*-

import json

html = """<html>
<head>
<style>
div.gallery {{
  margin: 5px;
  border: 1px solid #ccc;
  float: left;
  width: 180px;
}}

div.gallery:hover {{
  border: 1px solid #777;
}}

div.gallery img {{
  width: 100%;
  height: auto;
}}

div.desc {{
  padding: 15px;
  text-align: center;
}}
</style>
</head>
<body>
<h1>{subject}</h1>
{body}
</body>
</html>"""

header = """
<div style="clear:both;"></div>
<h2 >{section}<h2>"""

entry = """
<div class="gallery">
  <a target="_blank" href="{link}">
    <img src="{url}" alt="{alt}" width="150" height="100">
  </a>
  <div class="desc">{description}</div>
</div>"""

short_entry = """
<div class="gallery">
  <img src="{url}">
  <div class="desc">{description}</div>
</div>"""

def generate_html(filename, subject, json_file):
    with open(json_file, "r") as f:
        j = json.load(f)

    body = ""
    for section in j:
        body += header.format(section = section)
        count = 0
        for url in j[section]:
            body += entry.format(link=url, url=url.replace("2048", "320"), alt=section, description=section)
            #body += short_entry.format(url=url.replace("2048", "320"), description=section)
            count += 1
            if count > 9:
                break
    h = html.format(subject=subject, body=body)
    with open(filename, "w") as f:
        f.write(h)
