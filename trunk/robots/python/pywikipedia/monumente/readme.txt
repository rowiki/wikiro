update_database.py - parse the monument lists and extract the data in the format: list[dict{Arhitect, Denumire, Adresã, Datare, source, Cod, Coordonate, Imagine, Lat, Localitate, Lon}]

parse_monument_article.py - parse the articles and images of monuments and log then in the following format: dict{code, list[dict{name, namespace, project, lat, lon}, ...]}; also log errors where needed

corroborate_monument_data.py - parse all the databases, log errors and warnings and update the database where possible