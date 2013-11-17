package org.wikipedia.ro.populationdb.hr;

import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.startsWithAny;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.login.FailedLoginException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.jfree.data.general.Dataset;
import org.jfree.data.general.DefaultPieDataset;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;
import org.wikibase.Wikibase;
import org.wikipedia.Wiki;
import org.wikipedia.ro.populationdb.hr.dao.Hibernator;
import org.wikipedia.ro.populationdb.hr.model.Commune;
import org.wikipedia.ro.populationdb.hr.model.County;
import org.wikipedia.ro.populationdb.hr.model.EthnicallyStructurable;
import org.wikipedia.ro.populationdb.hr.model.Nationality;
import org.wikipedia.ro.populationdb.hr.model.Religion;
import org.wikipedia.ro.populationdb.util.Executor;
import org.wikipedia.ro.populationdb.util.ParameterReader;
import org.wikipedia.ro.populationdb.util.SysoutExecutor;
import org.wikipedia.ro.populationdb.util.Utilities;
import org.wikipedia.ro.populationdb.util.WikiEditExecutor;

public class HRWikiGenerator {
	private static final String NOTE_REFLIST = "\n== Note ==\n{{Reflist}}\n";
	private static final int NEW_PAGE_LIMIT = 1000;
	private Wiki rowiki;
	private Wiki hrwiki;
	private Wikibase dwiki;
	private Executor executor;
	private Hibernator hib;
	private Session ses;
	private final Pattern footnotesRegex = Pattern
			.compile("\\{\\{(?:(?:L|l)istănote|(?:R|r)eflist)|(?:\\<\\s*references\\s*\\/\\>)");
	Pattern hrWpTemplates1 = Pattern
			.compile("\\{\\{(?:((Infookvir o|O|o)pćina|(Infookvir g|G|g)rad))\\s*(\\|(?:\\{\\{[^{}]*+\\}\\}|[^{}])*+)?\\}\\}\\s*");
	private static Pattern regexInfocAsezare = Pattern
			.compile("\\{\\{(?:(?:C|c)asetă așezare|(?:I|i)nfocaseta Așezare|(?:C|c)utie așezare)\\s*(\\|(?:\\{\\{[^{}]*+\\}\\}|[^{}])*+)?\\}\\}\\s*");
	private static Pattern regexCutieOrase = Pattern
			.compile("\\{\\{(?:(?:C|c)utieOrașe|(?:C|c)asetăOraşe|(?:C|c)asetăOrașe)\\s*(\\|(?:\\{\\{[^{}]*+\\}\\}|[^{}])*+)?\\}\\}\\s*");

	private final STGroup communeTemplateGroup = new STGroupFile(
			"templates/hr/town.stg");
	private final STGroup townTemplateGroup = new STGroupFile(
			"templates/hr/town.stg");
	private Map<Nationality, Color> nationColorMap = new LinkedHashMap<Nationality, Color>();
	private Map<String, Nationality> nationNameMap = new LinkedHashMap<String, Nationality>();
	private Map<Religion, Color> religionColorMap = new LinkedHashMap<Religion, Color>();
	private Map<String, Religion> religionNameMap = new LinkedHashMap<String, Religion>();
	private Map<Commune, LazyInitializer<String>> hrWpNames = new HashMap<Commune, LazyInitializer<String>>();
	private Map<Commune, LazyInitializer<String>> roWpNames = new HashMap<Commune, LazyInitializer<String>>();
	private Map<String, String> relLinkMap = new LinkedHashMap<String, String>() {
		{
			put("Catolici", "[[Biserica Romano-Catolică|catolici]]");
			put("Ortodocși", "[[Biserica Ortodoxă|ortodocși]]");
			put("Protestanți", "[[Protestantism|protestanți]]");
			put("Musulmani", "[[Islam|musulmani]]");
			put("Iudaici", "[[Iudaism|iudaici]]");
			put("Religii orientale", "de religii orientale");
			put("Agnostici și sceptici",
					"[[Agnosticism|agnostici și sceptici]]");
			put("Fără religie și atei",
					"[[Umanism secular|persoane fără religie]] și [[Ateism|atei]]");
			put("Alte religii", "de alte religii");
		}
	};

	public static void main(final String[] args) throws Exception {
		final HRWikiGenerator generator = new HRWikiGenerator();
		try {
			generator.init();
			generator.generateCounties();
		} finally {
			generator.close();
		}
	}

	private void generateCounties() throws Exception {
		final List<County> counties = hib.getAllCounties();

		for (final County county : counties) {
			final List<Commune> communes = hib.getCommunesByCounty(county);
			generateCountyCategory(county, false);
			generateCountyCategory(county, true);
			generateCountyNavTemplate(county, communes);
			for (final Commune com : communes) {
				initCommune(com);
				generateCommune(com);
			}
		}
	}

	private void generateCountyCategory(County county, boolean town)
			throws Exception {
		String type = town ? "Oraș" : "Comun";
		String categoryName = "Categorie:" + type + "e în cantonul "
				+ county.getName();
		HashMap pageInfo = rowiki.getPageInfo(categoryName);
		if (!BooleanUtils.isTrue((Boolean) pageInfo.get("exists"))) {
			StringBuilder catText = new StringBuilder("[[Categorie:");
			catText.append(town ? "Oraș" : "Comun");
			catText.append("e în Croația|");
			catText.append(county.getName());
			catText.append("]]");
			executor.save(categoryName, catText.toString(),
					"Robot: creare categorie pentru " + type + "e din Croația");
		}
	}

	private void initCommune(final Commune com) {
		if (null == roWpNames.get(com)) {
			LazyInitializer<String> roWpNameIniter = new LazyInitializer<String>() {
				@Override
				protected String initialize() throws ConcurrentException {
					List<String> candidateNames = getRoWpCandidateNames(com);

					for (String candidateName : candidateNames) {
						try {
							HashMap candidatePageInfo = rowiki
									.getPageInfo(candidateName);
							if (BooleanUtils.isTrue((Boolean) candidatePageInfo
									.get("exists"))) {
								String actualCandidateTitle = StringUtils
										.defaultString(
												rowiki.resolveRedirect(new String[] { candidateName })[0],
												candidateName);
								String[] categories = rowiki
										.getCategories(actualCandidateTitle);
								for (String categ : categories) {
									if (StringUtils.startsWithAny(categ,
											"Categorie:Orașe în Croația",
											"Categorie:Orașe în cantonul ",
											"Categorie:Comune în Croația",
											"Categorie:Comune în cantonul ")) {
										return candidateName;
									}
								}
							} else {
								return candidateName;
							}

						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					return candidateNames.get(candidateNames.size() - 1);
				}

			};
			roWpNames.put(com, roWpNameIniter);
		}
		if (null == hrWpNames.get(com)) {
			LazyInitializer<String> hrWpNameIniter;
			if (null == (hrWpNameIniter = hrWpNames.get(com))) {
				hrWpNameIniter = new LazyInitializer<String>() {

					@Override
					protected String initialize() throws ConcurrentException {
						String communeName = retrieveName(com);
						List<String> candidateNames = Arrays.asList(communeName
								+ " (" + com.getCounty().getNameHr() + ")",
								communeName + " (općina)", communeName,
								StringUtils.replace(communeName, "-", " ")
										+ " (" + com.getCounty().getNameHr()
										+ ")",
								StringUtils.replace(communeName, "-", " ")
										+ " (općina)",
								StringUtils.replace(communeName, "-", " "));
						try {
							for (String candidateName : candidateNames) {
								if (hrwiki
										.exists(new String[] { candidateName })[0]) {
									String redirectedPage = hrwiki
											.resolveRedirect(new String[] { candidateName })[0];
									redirectedPage = StringUtils.defaultString(
											redirectedPage, candidateName);
									return redirectedPage;
								}
							}
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						return null;
					}
				};
				hrWpNames.put(com, hrWpNameIniter);
			}
		}
	}

	private List<String> getRoWpCandidateNames(Commune com) {
		String communeName = retrieveName(com);
		if (com.getTown() > 0) {
			return Arrays.asList(communeName, communeName + ", Croația",
					communeName + ", " + com.getCounty().getName());
		} else {
			return Arrays.asList("Comuna " + communeName + ", "
					+ com.getCounty().getName(), "Comuna " + communeName);
		}
	}

	private void generateCountyNavTemplate(County county, List<Commune> communes)
			throws Exception {
		Collections.sort(communes, new Comparator<Commune>() {

			public int compare(Commune o1, Commune o2) {
				if (o1.getTown() != o1.getTown()) {
					return o2.getTown() - o1.getTown();
				}
				if (!StringUtils.equals(o1.getName(), o2.getName())) {
					return o1.getName().compareTo(o2.getName());
				}
				return o1.getCounty().getName()
						.compareTo(o2.getCounty().getName());
			}
		});

		List<String> townsLinks = new ArrayList<String>();
		List<String> communeLinks = new ArrayList<String>();
		for (Commune com : communes) {
			String communeName = retrieveName(com);
			initCommune(com);
			if (com.getTown() > 0) {
				townsLinks.add("[[" + roWpNames.get(com).get() + "|"
						+ communeName + "]]");
			} else {
				communeLinks.add("[[" + roWpNames.get(com).get() + "|"
						+ communeName + "]]");
			}
		}

		String navTemplateName = "Cantonul " + county.getName();

		StringBuilder navTemplateBuilder = new StringBuilder(
				"{{Casetă de navigare simplă\n|titlu=Comune și orașe în [[cantonul ");
		navTemplateBuilder.append(county.getName());
		navTemplateBuilder.append("]]\n|nume=");
		navTemplateBuilder.append(navTemplateName);
		navTemplateBuilder.append("\n|grup1=Orașe");
		navTemplateBuilder.append("\n|listă1=<div>\n");
		navTemplateBuilder.append(StringUtils.join(
				townsLinks.toArray(new String[townsLinks.size()]), "{{~}}\n"));
		navTemplateBuilder.append("\n</div>");
		navTemplateBuilder.append("\n|grup2=Comune");
		navTemplateBuilder.append("\n|listă2=<div>\n");
		navTemplateBuilder.append(StringUtils.join(
				communeLinks.toArray(new String[communeLinks.size()]),
				"{{~}}\n"));
		navTemplateBuilder.append("\n</div>");
		navTemplateBuilder
				.append("}}<noinclude>[[Categorie:Formate de navigare cantoane din Croația]]</noinclude>");

		executor.save("Format:Cantonul " + county.getName(),
				navTemplateBuilder.toString(),
				"Robot: creare format navigare orașe și comune componente ale cantonului "
						+ county.getName() + " din Croația");

	}

	private void generateCommune(final Commune com) throws Exception {
		final String title = getExistingRoTitleOfArticleWithSubject(com);

		String demographySection = generateDemographySection(com);
		String infobox = generateInfoboxForCommune(com);
		String articleIntro = generateIntroForCommune(com);
		String refSection = NOTE_REFLIST;
		String closingStatements = generateClosingStatements(com);

		String disambigTitle = createDisambig(com);
		String newArticleContent = (null == disambigTitle ? ""
				: ("{{Altesensuri2|" + disambigTitle + "}}"))
				+ infobox
				+ articleIntro
				+ demographySection
				+ "<br clear=\"left\"/>"
				+ refSection + closingStatements;
		final boolean newPage = isNewPage(com, title, infobox.length()
				+ articleIntro.length() + refSection.length()
				+ closingStatements.length());

		if (newPage) {
			generateNewCommuneArticle(com, newArticleContent);
		} else {
			addDemographySectionToExistingArticle(com, title,
					demographySection, infobox);
		}
	}

	private void generateNewCommuneArticle(Commune com, String text)
			throws Exception {
		String communeName = retrieveName(com);
		String roWpArticleTitle = roWpNames.get(com).get();
		System.out.println("------------------ New commune article for "
				+ communeName + " title=" + roWpArticleTitle
				+ " ----------------");
		System.out.println(text);

		executor.save(roWpArticleTitle, text,
				"Robot: (re-)creare articol despre " + communeName
						+ ", Croația");
		createRedirects(roWpArticleTitle, com);
		executor.link("rowiki", roWpArticleTitle, "hrwiki", hrWpNames.get(com)
				.get());
	}

	private String createDisambig(Commune com) throws Exception {
		long countCommunesWithName = hib.countCommunesWithName(com.getName());
		if (countCommunesWithName > 1l) {
			List<Commune> communesWithName = hib.getCommunesWithName(com
					.getName());
			StringBuilder disambig = new StringBuilder("Denumirea de '''");
			disambig.append(com.getName());
			disambig.append("''' se poate referi la următoarele locuri din [[Croația]]:");
			int townsCount = 0;
			for (Commune eachCom : communesWithName) {
				townsCount += eachCom.getTown();
				disambig.append("\n* [[");
				disambig.append(hrWpNames.get(eachCom).get());
				disambig.append("|");
				disambig.append(eachCom.getName());
				disambig.append("]], ");
				disambig.append(eachCom.getTown() > 0 ? "oraș" : "comună");
				disambig.append(" în [[cantonul ");
				disambig.append(eachCom.getCounty().getName());
				disambig.append("]];");
			}
			disambig.replace(disambig.length() - 1, disambig.length(), ".");
			disambig.append("\n{{Dezambiguizare}}");
			String title = townsCount > 0 ? (com.getName() + " (dezambiguizare)")
					: com.getName();
			if (!rowiki.exists(new String[] { title })[0]) {
				executor.save(title, disambig.toString(),
						"Robot: creare pagină dezambiguizare pentru orașele/comunele croate denumite „"
								+ com.getName() + "”");
			} else {
				String disambigTalk = "";
				if (rowiki.exists(new String[] { "Discuție:" + title })[0]) {
					disambigTalk = rowiki.getPageText("Discuție:" + title);
				}
				executor.save(
						"Discuție:" + title,
						disambigTalk
								+ "\n== De adăugat ==\n"
								+ disambigTalk.substring(0,
										disambigTalk.length()
												- "{{Dezambiguizare}}".length()),
						"Robot: date de adăugat în pagina de dezambiguizare despre aşezări în Croaţia");
			}
			return title;
		}
		return null;
	}

	private void createRedirects(String roWpArticleTitle, Commune com)
			throws Exception {
		List<String> redirects = new ArrayList<String>();
		redirects.add(com.getName());
		redirects.add(com.getName() + ", Croația");
		redirects.add(com.getName() + ", " + com.getCounty().getName());
		if (com.getTown() == 0) {
			redirects.add("Comuna " + com.getName());
			redirects.add("Comuna " + com.getName() + ", Croația");
			redirects.add("Comuna " + com.getName() + ", "
					+ com.getCounty().getName());
		}

		for (String redirect : redirects) {
			if (StringUtils.equals(roWpArticleTitle, redirect)) {
				continue;
			}
			if (rowiki.exists(new String[] { redirect })[0]) {
				String logPage = "Utilizator:Andrebot/Comune Croația/Redirecționări necreate";
				String logdata = null;
				if (rowiki.exists(new String[] { logPage })[0]) {
					logdata = rowiki.getPageText(logPage);
				}
				executor.save(logPage, StringUtils.defaultString(logdata)
						+ "\n* [[" + redirect + "]]",
						"Robot: logat redirect necreat");
			} else {
				executor.save(redirect,
						"#redirect[[" + roWpArticleTitle + "]]",
						"Robot: creare redirect către [[" + roWpArticleTitle
								+ "]]");
			}
		}
	}

	private String generateClosingStatements(Commune com) {
		StringBuilder closingst = new StringBuilder();

		closingst.append("\n");
		closingst.append("\n{{Cantonul " + com.getCounty().getName() + "}}");
		closingst.append("\n[[Categorie:");
		closingst.append(com.getTown() > 0 ? "Oraș" : "Comun");
		closingst.append("e în cantonul " + com.getCounty().getName());
		closingst.append("]]");

		return closingst.toString();
	}

	private String generateIntroForCommune(Commune com) {
		STGroup stgroup = new STGroupFile("templates/hr/town.stg");
		ST introTmpl = stgroup.getInstanceOf("introTmpl"
				+ (com.getTown() > 0 ? "Town" : "Comm"));
		String communeName = retrieveName(com);
		introTmpl.add("nume", communeName);
		introTmpl.add("canton", com.getCounty().getName());
		introTmpl.add("populatie", "{{formatnum:" + com.getPopulation()
				+ "}}&nbsp;" + Utilities.de(com.getPopulation(), "", ""));
		return introTmpl.render();
	}

	private String generateInfoboxForCommune(Commune com)
			throws ConcurrentException {
		StringBuilder infoboxText = new StringBuilder("{{Infocaseta Așezare");
		String[] names = StringUtils
				.splitByWholeSeparator(com.getName(), " - ");
		infoboxText.append("\n|nume = ");
		infoboxText.append(names[0]);
		if (1 < names.length) {
			infoboxText.append("\n|nume_nativ = ");
			infoboxText.append(StringUtils.join(names, "<br />", 1,
					names.length));
		}
		infoboxText.append("}}");
		ParameterReader ibReader = new ParameterReader(infoboxText.toString());
		ibReader.run();
		Map<String, String> ibParams = ibReader.getParams();

		putBasicDataIntoParams(com, ibParams);
		putDemoDataIntoParams(com, ibParams);
		extractDataFromExternalWikiToInfobox(com, ibParams);

		infoboxText = new StringBuilder("{{Infocaseta Așezare|");
		for (String param : ibParams.keySet()) {
			infoboxText.append("\n|");
			infoboxText.append(param);
			infoboxText.append(" = ");
			infoboxText.append(ibParams.get(param));
		}
		infoboxText.append("}}\n");
		return infoboxText.toString();
	}

	private void extractDataFromExternalWikiToInfobox(final Commune com,
			Map<String, String> ibParams) throws ConcurrentException {
		LazyInitializer<String> hrWpNameIniter = hrWpNames.get(com);
		String hrWpName = hrWpNameIniter.get();
		try {

			String hrPageText = hrwiki.getPageText(hrWpName);
			Matcher hrwpInfoboxMatcher = hrWpTemplates1.matcher(hrPageText);
			if (hrwpInfoboxMatcher.find()) {
				String existingInfobox = hrwpInfoboxMatcher.group();
				ParameterReader ibparamreader = new ParameterReader(
						existingInfobox);
				ibparamreader.run();
				Map<String, String> params = ibparamreader.getParams();
				translateInfoboxParam(ibParams, "stemă", params, "grb");
				translateInfoboxParam(ibParams, "suprafață_totală_km2", params,
						"površina");
				translateInfoboxParam(ibParams, "lider_nume", params,
						"načelnik");
				translateInfoboxParam(ibParams, "componenta", params, "naselja");
				translateInfoboxParam(ibParams, "componenta", params,
						"gradska naselja");
				translateInfoboxParam(ibParams, "codpoștal", params,
						"poštanski broj");
				translateInfoboxParam(ibParams, "latd", params, "z. širina");
				translateInfoboxParam(ibParams, "longd", params, "z. dužina");
				if (params.containsKey("z. dužina")) {
					ibParams.put("pushpin_map", "Croația");
					ibParams.put("latNS", "N");
					ibParams.put("longEV", "E");
				}
				translateInfoboxParam(ibParams, "lider_titlu", params,
						"gradonačelnik");
				if (params.containsKey("gradonačelnik")
						|| params.containsKey("načelnik")) {
					ibParams.put("lider_titlu", "Primar");
				}
				translateInfoboxParam(ibParams, "altitudine", params, "visina");
				translateInfoboxParam(ibParams, "densitate", params,
						"stanovništvo_gustoća");
				translateInfoboxParam(ibParams, "suprafață_urbana_km2", params,
						"površina_uža");
				translateInfoboxParam(ibParams, "imagine", params,
						"slika_panorama");
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void translateInfoboxParam(Map<String, String> ibParams,
			String toString, Map<String, String> params, String fromString) {
		if (params.containsKey(fromString)) {
			ibParams.put(toString, params.get(fromString));
		}
	}

	private void putDemoDataIntoParams(Commune com, Map<String, String> ibParams) {
		ibParams.put("recensamant", "2011");
		ibParams.put("populatie", String.valueOf(com.getPopulation()));
		ibParams.put("populatie_note_subsol",
				"<ref name=\"hr_census_2011_ethnicity\"/>");
	}

	private void putBasicDataIntoParams(Commune com,
			Map<String, String> ibParams) {
		ibParams.put("tip_asezare", 0 < com.getTown() ? "Oraș" : "Comună");
		ibParams.put("tip_subdiviziune", "[[Țările lumii|Țară]]");
		ibParams.put("tip_subdiviziune1", "[[Cantoanele Croației|Canton]]");
		ibParams.put("nume_subdiviziune", "{{CRO}}");
		ibParams.put("nume_subdiviziune1", "[[Cantonul "
				+ com.getCounty().getName() + "|" + com.getCounty().getName()
				+ "]]");

		ibParams.put("fus_orar", "[[Ora Europei Centrale|CET]]");
		ibParams.put("fus_orar_DST", "[[Ora de Vară a Europei Centrale|CEST]]");
		ibParams.put("utc_offset", "+1");
		ibParams.put("utc_offset_DST", "+2");
	}

	private void addDemographySectionToExistingArticle(final Commune com,
			final String title, final String demographySection,
			final String infobox) throws Exception {
		final String pageText = rowiki.getPageText(title);
		final List<String> markers = Arrays.asList("==Note", "== Note",
				"== Vezi și", "==Vezi și", "[[Categorie:", "{{Ciot", "{{ciot",
				"{{Croatia");
		final List<Integer> markerLocations = createMarkerLocationList(
				pageText, markers);

		int insertLocation;
		if (markerLocations.size() > 0) {
			insertLocation = Collections.min(markerLocations);
		} else {
			insertLocation = -1;
		}

		final StringBuilder sbuild = new StringBuilder(pageText);
		if (!footnotesRegex.matcher(sbuild).find()) {
			if (insertLocation > 0) {
				sbuild.insert(insertLocation, NOTE_REFLIST);
			} else {
				sbuild.append(NOTE_REFLIST);
			}
		}
		if (insertLocation > 0) {
			sbuild.insert(insertLocation, demographySection);
		} else {
			sbuild.append(demographySection);
		}

		String currentInfobox = null;
		Matcher casetaOraseMatcher = regexCutieOrase.matcher(sbuild);
		Matcher casetaAsezareMatcher = regexInfocAsezare.matcher(sbuild);
		boolean isCasetaOrase = false;
		if (casetaOraseMatcher.find()) {
			isCasetaOrase = true;
			currentInfobox = casetaOraseMatcher.group();
		} else if (casetaAsezareMatcher.find()) {
			currentInfobox = casetaAsezareMatcher.group();
		}
		if (null != currentInfobox
				&& (currentInfobox.length() <= infobox.length() || isCasetaOrase)) {
			sbuild.replace(sbuild.indexOf(currentInfobox),
					sbuild.indexOf(currentInfobox) + currentInfobox.length(),
					infobox);
		} else if (null != currentInfobox) {
			ParameterReader crtIbReader = new ParameterReader(currentInfobox);
			crtIbReader.run();
			Map<String, String> crtIbParams = crtIbReader.getParams();
			ParameterReader updatedIbReader = new ParameterReader(infobox);
			updatedIbReader.run();
			Map<String, String> updatedParams = updatedIbReader.getParams();
			for (String updatedParam : updatedParams.keySet()) {
				crtIbParams.put(updatedParam, updatedParams.get(updatedParam));
			}
			StringBuilder infoboxBuilder = new StringBuilder(
					"{{Infocaseta Așezare");
			for (String crtIbParam : crtIbParams.keySet()) {
				infoboxBuilder.append("\n|");
				infoboxBuilder.append(crtIbParam);
				infoboxBuilder.append(" = ");
				infoboxBuilder.append(crtIbParams.get(crtIbParam));
			}
			infoboxBuilder.append("\n}}");
			sbuild.replace(sbuild.indexOf(currentInfobox),
					sbuild.indexOf(currentInfobox) + currentInfobox.length(),
					infoboxBuilder.toString());
		} else {
			sbuild.insert(0, infobox);
		}
		System.out.println("Inserting demography section into article \""
				+ title + "\", article becomes: " + sbuild.toString());
		executor.save(
				title,
				sbuild.toString(),
				"Robot: adăugare date demografice conform recensământului din 2011 și date de la hr.wp în infocasetă");
	}

	private List<Integer> createMarkerLocationList(final String pageText,
			final List<String> markers) {
		final List<Integer> ret = new ArrayList<Integer>();
		for (final String marker : markers) {
			final int markerLocation = StringUtils.indexOf(pageText, marker);
			if (0 <= markerLocation) {
				ret.add(markerLocation);
			}
		}

		return ret;
	}

	private String generateDemographySection(final Commune com) {
		final StringBuilder demographics = new StringBuilder(
				"\n== Demografie ==\n<!-- Start secțiune generată de Andrebot -->");
		final STGroup templateGroup = com.getTown() > 0 ? townTemplateGroup
				: communeTemplateGroup;
		final ST piechart = templateGroup.getInstanceOf("piechart");
		final int population = com.getPopulation();
		String communeName = retrieveName(com);
		piechart.add("nume", communeName);
		piechart.add("tip_genitiv", com.getTown() > 0 ? "orașului" : "comunei");

		final Map<Nationality, Integer> ethnicStructure = com
				.getEthnicStructure();
		final Map<Religion, Integer> religiousStructure = com
				.getReligiousStructure();
		final DefaultPieDataset datasetEthnos = new DefaultPieDataset();
		final int totalKnownEthnicity = computeEthnicityDataset(population,
				ethnicStructure, datasetEthnos);
		final DefaultPieDataset datasetReligion = new DefaultPieDataset();
		final int totalKnownReligion = computeReligionDataset(population,
				religiousStructure, datasetReligion);

		renderPiechart(demographics, piechart, population, datasetEthnos,
				datasetReligion);

		final ST demogIntro = templateGroup.getInstanceOf("demogIntro"
				+ (com.getTown() > 0 ? "Town" : "Comm"));
		demogIntro.add("nume", communeName);
		demogIntro.add(
				"populatie",
				"{{formatnum:" + com.getPopulation() + "}}&nbsp;"
						+ Utilities.de(population, "locuitor", "locuitori"));
		demographics.append(demogIntro.render());

		final Nationality majNat = getMajorityEthnicity(com);
		final List<Nationality> ethnicMinorities = getEthnicMinorities(com);
		Religion majRel = getMajorityReligion(com);
		List<Religion> religiousMinorities = getReligiousMinorities(com);

		writeEthnodemographics(templateGroup, demographics, population,
				ethnicStructure, majNat, ethnicMinorities);
		writeUnknownEthnicity(templateGroup, demographics, population,
				datasetEthnos, com);

		writeReligiousDemographics(templateGroup, demographics, population,
				religiousStructure, majRel, religiousMinorities);
		writeUnknownReligion(templateGroup, demographics, population,
				datasetReligion, com);

		demographics.append("\n<!--Sfârșit secțiune generată de Andrebot -->");
		return demographics.toString();
	}

	private void writeReligiousDemographics(STGroup templateGroup,
			StringBuilder demographics, int population,
			final Map<Religion, Integer> religiousStructure, Religion majRel,
			List<Religion> minorities) {
		Collections.sort(minorities, new Comparator<Religion>() {

			public int compare(final Religion o1, final Religion o2) {
				final Integer pop1 = religiousStructure.get(o1);
				final Integer pop2 = religiousStructure.get(o2);
				if (pop1 == null) {
					return 1;
				}
				if (pop2 == null) {
					return -1;
				}

				return pop2 - pop1;
			}
		});
		if (null != majRel) {
			final ST demogMajority = templateGroup
					.getInstanceOf("religiousMaj");
			demogMajority.add("rel_maj", getReligionLink(majRel));
			final double majProcent = 100.0 * religiousStructure.get(majRel)
					/ population;
			demogMajority.add("maj_procent",
					"{{formatnum:" + (Math.round(majProcent * 100) / 100.0)
							+ "}}%");
			demographics.append(demogMajority.render());

			if (minorities.size() == 1) {
				final ST oneMinority = templateGroup
						.getInstanceOf("oneReligiousMinority");
				final Religion minority = minorities.get(0);
				final double minProcent = 100.0
						* religiousStructure.get(minority) / population;

				oneMinority.add("rel_minority", getReligionLink(minority)
						+ " ({{formatnum:"
						+ (Math.round(minProcent * 100) / 100.0) + "}}%)");
				demographics.append(oneMinority.render());
			} else if (minorities.size() > 1) {
				final ST someMinorities = templateGroup
						.getInstanceOf("someReligiousMinorities");
				final List<String> nationalitiesData = new ArrayList<String>();
				for (final Religion mino : minorities) {
					final StringBuilder data = new StringBuilder();
					data.append(getReligionLink(mino));
					final double minProcent = 100.0
							* religiousStructure.get(mino) / population;
					data.append(" ({{formatnum:"
							+ (Math.round(minProcent * 100) / 100.0) + "}}%)");
					nationalitiesData.add(data.toString());
				}
				someMinorities.add(
						"rel_minorities_enum",
						join(nationalitiesData.toArray(), ", ", 0,
								nationalitiesData.size() - 1)
								+ " și "
								+ nationalitiesData.get(nationalitiesData
										.size() - 1));
				demographics.append(someMinorities.render());
			}
		} else if (minorities.size() > 0) {
			final ST noMaj = templateGroup.getInstanceOf("noReligiousMaj");
			final List<String> nationalitiesData = new ArrayList<String>();

			for (final Religion mino : minorities) {
				final StringBuilder data = new StringBuilder();
				data.append(getReligionLink(mino));
				final double minProcent = 100.0 * religiousStructure.get(mino)
						/ population;
				data.append(" ({{formatnum:"
						+ (Math.round(minProcent * 100) / 100.0) + "}}%)");
				nationalitiesData.add(data.toString());
			}
			noMaj.add(
					"rel_enum",
					join(nationalitiesData.toArray(), ", ", 0,
							nationalitiesData.size() - 1)
							+ " și "
							+ nationalitiesData.get(nationalitiesData.size() - 1));
			demographics.append(noMaj.render());
		}
		demographics.append(". ");
	}

	private Object getReligionLink(Religion rel) {
		return relLinkMap.get(rel.getName());
	}

	private List<Religion> getReligiousMinorities(Commune com) {
		final List<Religion> ret = new ArrayList<Religion>();
		for (final Religion rel : com.getReligiousStructure().keySet()) {
			final double weight = com.getReligiousStructure().get(rel)
					/ (double) com.getPopulation();
			if (weight <= 0.5
					&& weight > 0.01
					&& !startsWithAny(rel.getName(), "Alte", "Ne",
							"Nu au declarat")) {
				ret.add(rel);
			}
		}
		return ret;
	}

	private Religion getMajorityReligion(Commune com) {
		for (final Religion rel : com.getReligiousStructure().keySet()) {
			if (com.getReligiousStructure().get(rel)
					/ (double) com.getPopulation() > 0.5) {
				return rel;
			}
		}
		return null;
	}

	private void writeUnknownReligion(STGroup templateGroup,
			StringBuilder demographics, int population,
			DefaultPieDataset datasetRel, Commune com) {
		final double undeclaredPercent = 100.0
				* (defaultIfNull(datasetRel.getValue("Necunoscută"),
						new Integer(0)).doubleValue()) / population;
		if (undeclaredPercent > 0) {
			final ST undeclaredTempl = templateGroup
					.getInstanceOf("unknownRel");
			undeclaredTempl.add("percent",
					"{{formatnum:"
							+ (Math.round(undeclaredPercent * 100.0d) / 100.0d)
							+ "}}%");
			demographics.append(undeclaredTempl.render());
		}

		demographics.append(getReligionRef(com));

	}

	private void writeUnknownEthnicity(STGroup templateGroup,
			StringBuilder demographics, int population,
			DefaultPieDataset datasetEthnos, Commune com) {
		final double undeclaredPercent = 100.0d
				* (defaultIfNull(datasetEthnos.getValue("Necunoscut"),
						new Integer(0)).doubleValue()) / population;
		if (undeclaredPercent > 0) {
			final ST undeclaredTempl = templateGroup
					.getInstanceOf("unknownEthn");
			undeclaredTempl.add("percent",
					"{{formatnum:"
							+ (Math.round(undeclaredPercent * 100.0d) / 100.0d)
							+ "}}%");
			demographics.append(undeclaredTempl.render());
		}

		demographics.append(getEthnicityRef(com));
	}

	private void writeEthnodemographics(STGroup templateGroup,
			StringBuilder demographics, int population,
			final Map<Nationality, Integer> ethnicStructure,
			Nationality majNat, List<Nationality> minorities) {
		Collections.sort(minorities, new Comparator<Nationality>() {

			public int compare(final Nationality o1, final Nationality o2) {
				final Integer pop1 = ethnicStructure.get(o1);
				final Integer pop2 = ethnicStructure.get(o2);
				if (pop1 == null) {
					return 1;
				}
				if (pop2 == null) {
					return -1;
				}

				return pop2 - pop1;
			}
		});
		if (null != majNat) {
			final ST demogMajority = templateGroup.getInstanceOf("ethnicMaj");
			demogMajority.add("etnie_maj", getNationLink(majNat));
			final double majProcent = 100.0 * ethnicStructure.get(majNat)
					/ population;
			demogMajority.add("maj_procent",
					"{{formatnum:" + (Math.round(majProcent * 100) / 100.0)
							+ "}}%");
			demographics.append(demogMajority.render());

			if (minorities.size() == 1) {
				final ST oneMinority = templateGroup
						.getInstanceOf("oneEthnicMinority");
				final Nationality minority = minorities.get(0);
				final double minProcent = 100.0 * ethnicStructure.get(minority)
						/ population;

				oneMinority.add("minority", getNationLink(minority)
						+ " ({{formatnum:"
						+ (Math.round(minProcent * 100) / 100.0) + "}}%)");
				demographics.append(oneMinority.render());
			} else if (minorities.size() > 1) {
				final ST someMinorities = templateGroup
						.getInstanceOf("someEthnicMinorities");
				final List<String> nationalitiesData = new ArrayList<String>();
				for (final Nationality mino : minorities) {
					final StringBuilder data = new StringBuilder();
					data.append(getNationLink(mino));
					final double minProcent = 100.0 * ethnicStructure.get(mino)
							/ population;
					data.append(" ({{formatnum:"
							+ (Math.round(minProcent * 100) / 100.0) + "}}%)");
					nationalitiesData.add(data.toString());
				}
				someMinorities.add(
						"minorities_enum",
						join(nationalitiesData.toArray(), ", ", 0,
								nationalitiesData.size() - 1)
								+ " și "
								+ nationalitiesData.get(nationalitiesData
										.size() - 1));
				demographics.append(someMinorities.render());
			}
		} else if (minorities.size() > 0) {
			final ST noMaj = templateGroup.getInstanceOf("noEthnicMaj");
			final List<String> nationalitiesData = new ArrayList<String>();

			for (final Nationality mino : minorities) {
				final StringBuilder data = new StringBuilder();
				data.append(getNationLink(mino));
				final double minProcent = 100.0 * ethnicStructure.get(mino)
						/ population;
				data.append(" ({{formatnum:"
						+ (Math.round(minProcent * 100) / 100.0) + "}}%)");
				nationalitiesData.add(data.toString());
			}
			noMaj.add(
					"ethnicities_enum",
					join(nationalitiesData.toArray(), ", ", 0,
							nationalitiesData.size() - 1)
							+ " și "
							+ nationalitiesData.get(nationalitiesData.size() - 1));
			demographics.append(noMaj.render());
		}
		demographics.append(". ");
	}

	private Object getNationLink(Nationality majNat) {
		if (!startsWithAny(majNat.getName(), "Necunoscut", "Nu au declarat",
				"Croați")) {
			return "[[" + majNat.getName() + "i din Croația|"
					+ lowerCase(majNat.getName()) + "]]";
		} else if (startsWith(majNat.getName(), "Croați")) {
			return "[[croați]]";
		} else {
			return lowerCase(majNat.getName());
		}
	}

	private int computeReligionDataset(int population,
			final Map<Religion, Integer> religiousStructure,
			DefaultPieDataset dataset) {
		final Map<String, Integer> smallGroups = new HashMap<String, Integer>();
		final Religion undeclared = hib.getReligionByName("Necunoscută");
		int totalKnownEthnicity = 0;
		Set<Religion> religionsSet = religionColorMap.keySet();
		List<Religion> religionsList = new ArrayList<Religion>(religionsSet);
		Collections.sort(religionsList, new Comparator<Religion>() {

			public int compare(Religion arg0, Religion arg1) {
				int natpop0 = defaultIfNull(religiousStructure.get(arg0), 0);
				int natpop1 = defaultIfNull(religiousStructure.get(arg1), 0);
				return natpop1 - natpop0;
			}

		});
		int unknownRel = 0;
		int otherRel = 0;
		for (final Religion rel : religionsList) {
			final int natpop = defaultIfNull(religiousStructure.get(rel), 0);
			if (natpop * 100.0 / population > 1.0
					&& !startsWithAny(rel.getName(), "Necunoscut",
							"Nu au declarat", "Alte") && natpop > 0) {
				dataset.setValue(rel.getName(), natpop);
			} else if (natpop * 100.0 / population <= 1.0
					&& !startsWithAny(rel.getName(), "Necunoscut",
							"Nu au declarat", "Alte") && natpop > 0) {
				smallGroups.put(rel.getName(), natpop);
			} else if (startsWithAny(rel.getName(), "Necunoscut",
					"Nu au declarat")) {
				unknownRel += natpop;
			} else if (startsWith(rel.getName(), "Alte")) {
				otherRel += natpop;
			}
			if (!startsWithAny(rel.getName(), "Necunoscut", "Nu au declarat",
					"Alte")) {
				totalKnownEthnicity += natpop;
			}
		}
		dataset.setValue("Necunoscută", unknownRel);
		if (1 < smallGroups.size()) {
			smallGroups.put("Alte religii", otherRel);
			int smallSum = 0;
			for (final String smallGroup : smallGroups.keySet()) {
				smallSum += ObjectUtils.defaultIfNull(
						smallGroups.get(smallGroup), 0);
			}
			dataset.setValue("Alte religii", smallSum);
		} else {
			for (final String relname : smallGroups.keySet()) {
				if (!startsWithAny(relname, "Necunoscut", "Nu au declarat")
						&& smallGroups.containsKey(relname)) {
					dataset.setValue(relname, smallGroups.get(relname));
				}
			}
			if (0 < otherRel) {
				dataset.setValue("Alte religii", otherRel);
			}
		}
		return totalKnownEthnicity;
	}

	private void renderPiechart(StringBuilder demographics, ST piechart,
			int population, DefaultPieDataset datasetEthnos,
			DefaultPieDataset datasetReligion) {
		final StringBuilder pieChartEthnosProps = new StringBuilder();
		final StringBuilder pieChartReligProps = new StringBuilder();
		int i = 1;
		demographics.append("<div style=\"float:left\">");

		for (final Object k : datasetEthnos.getKeys()) {
			pieChartEthnosProps.append("\n|label");
			pieChartEthnosProps.append(i);
			pieChartEthnosProps.append('=');
			pieChartEthnosProps.append(k.toString());
			pieChartEthnosProps.append("|value");
			pieChartEthnosProps.append(i);
			pieChartEthnosProps.append('=');
			pieChartEthnosProps
					.append(Math.round(100 * ((datasetEthnos.getValue(
							k.toString()).doubleValue() * 100) / population)) / 100.0d);
			pieChartEthnosProps.append("|color");
			pieChartEthnosProps.append(i);
			pieChartEthnosProps.append('=');
			final Color color = (Color) nationColorMap.get(nationNameMap.get(k
					.toString()));
			if (null == color) {
				throw new RuntimeException("Unknown color for nationality " + k);
			}
			pieChartEthnosProps.append(Utilities.colorToHtml(color));
			i++;
		}
		i = 1;
		for (final Object k : datasetReligion.getKeys()) {
			pieChartReligProps.append("\n|label");
			pieChartReligProps.append(i);
			pieChartReligProps.append('=');
			pieChartReligProps.append(k.toString());
			pieChartReligProps.append("|value");
			pieChartReligProps.append(i);
			pieChartReligProps.append('=');
			pieChartReligProps
					.append(Math.round(100 * (datasetReligion.getValue(
							k.toString()).doubleValue() * 100.0 / population)) / 100.0);
			pieChartReligProps.append("|color");
			pieChartReligProps.append(i);
			pieChartReligProps.append('=');
			final Color color = (Color) religionColorMap.get(religionNameMap
					.get(k.toString()));
			if (null == color) {
				throw new RuntimeException("Unknown color for religion " + k);
			}
			pieChartReligProps.append(Utilities.colorToHtml(color));
			i++;
		}
		piechart.add("propsEthnos", pieChartEthnosProps.toString());
		piechart.add("propsRelig", pieChartReligProps.toString());
		demographics.append(piechart.render());
		demographics.append("</div>\n");
	}

	/**
	 * There are two meta-groups: other, that group all small ethnic groups, and
	 * unknown that group all people whose ethnicity is unknown
	 * 
	 * @param population
	 * @param ethnicStructure
	 * @param dataset
	 * @return
	 */
	private int computeEthnicityDataset(final int population,
			final Map<Nationality, Integer> ethnicStructure,
			final DefaultPieDataset dataset) {
		final Map<String, Integer> smallGroups = new HashMap<String, Integer>();
		int totalKnownEthnicity = 0;
		Set<Nationality> ethnicitiesSet = nationColorMap.keySet();
		List<Nationality> ethnicitiesList = new ArrayList<Nationality>(
				ethnicitiesSet);
		Collections.sort(ethnicitiesList, new Comparator<Nationality>() {

			public int compare(Nationality arg0, Nationality arg1) {
				int natpop0 = defaultIfNull(ethnicStructure.get(arg0), 0);
				int natpop1 = defaultIfNull(ethnicStructure.get(arg1), 0);
				return natpop1 - natpop0;
			}

		});
		int unknownEthn = 0;
		int otherEthn = 0;
		for (final Nationality nat : ethnicitiesList) {
			final int natpop = defaultIfNull(ethnicStructure.get(nat), 0);
			if (natpop * 100.0 / population > 1.0
					&& !startsWithAny(nat.getName(), "Ne", "Afiliați")
					&& 0 < natpop) {
				dataset.setValue(nat.getName(), natpop);
			} else if (natpop * 100.0 / population <= 1.0
					&& !startsWithAny(nat.getName(), "Ne", "Afiliați")
					&& 0 < natpop) {
				smallGroups.put(nat.getName(), natpop);
			} else if (startsWithAny(nat.getName(), "Nedeclarat", "Necunoscut")) {
				unknownEthn += natpop;
			} else if (startsWithAny(nat.getName(), "Afiliați", "Neclasificat")) {
				otherEthn += natpop;
			}
			if (!startsWithAny(nat.getName(), "Nec", "Ned", "Afiliați")) {
				totalKnownEthnicity += natpop;
			}
		}
		dataset.setValue("Necunoscut", unknownEthn);

		// add all small groups to other; if only one, just show that one
		smallGroups.put("Neclasificat", otherEthn);
		if (1 < smallGroups.size()) {
			int smallSum = 0;
			for (final String smallGroup : smallGroups.keySet()) {
				smallSum += ObjectUtils.defaultIfNull(
						smallGroups.get(smallGroup), 0);
			}
			dataset.setValue("Neclasificat", smallSum);
		} else {
			for (final String natname : smallGroups.keySet()) {
				if (!startsWithAny(natname, "Ne")
						&& !startsWithAny(natname, "Afiliați")
						&& smallGroups.containsKey(natname)) {
					dataset.setValue(natname, smallGroups.get(natname));
				}
			}
			dataset.setValue("Neclasificat", otherEthn);
		}
		return totalKnownEthnicity;
	}

	private boolean isNewPage(final Commune com, final String title,
			int newArticleContentSize) throws IOException {
		if (title == null) {
			return true;
		}
		final Map pageInfo = rowiki.getPageInfo(title);
		final Integer intSup = (Integer) pageInfo.get("size");
		if (null != intSup
				&& (intSup < NEW_PAGE_LIMIT || intSup < newArticleContentSize)) {
			return true;
		}
		return false;
	}

	private void close() {
		if (null != ses) {
			org.hibernate.Transaction tx = ses.getTransaction();
			if (null != tx) {
				tx.rollback();
			}
		}
		if (null != rowiki) {
			rowiki.logout();
		}
		if (null != hrwiki) {
			hrwiki.logout();
		}
		if (null != dwiki) {
			dwiki.logout();
		}

	}

	private void init() throws FailedLoginException, IOException {
		rowiki = new Wiki("ro.wikipedia.org");
		hrwiki = new Wiki("hr.wikipedia.org");
		dwiki = new Wikibase();
		executor = new WikiEditExecutor(rowiki, dwiki);

		final Properties credentials = new Properties();
		credentials.load(HRWikiGenerator.class.getClassLoader()
				.getResourceAsStream("credentials.properties"));
		final String datauser = credentials.getProperty("UsernameData");
		final String datapass = credentials.getProperty("PasswordData");
		final String user = credentials.getProperty("Username");
		final String pass = credentials.getProperty("Password");
		rowiki.login(user, pass.toCharArray());
		rowiki.setMarkBot(true);
		dwiki.login(datauser, datapass.toCharArray());

		hib = new Hibernator();
		ses = hib.getSession();
		ses.beginTransaction();

		assignColorToNationality("Români", new Color(85, 85, 255));
		assignColorToNationality("Turci", new Color(255, 85, 85));
		assignColorToNationality("Romi", new Color(85, 255, 255));
		assignColorToNationality("Maghiari", new Color(85, 255, 85));
		assignColorToNationality("Evrei", new Color(192, 192, 192));
		assignColorToNationality("Croați", new Color(32, 32, 192));
		assignColorToNationality("Sârbi", new Color(192, 32, 32));
		assignColorToNationality("Bosniaci", new Color(64, 64, 128));
		assignColorToNationality("Austrieci", new Color(255, 255, 255));
		assignColorToNationality("Albanezi", new Color(192, 64, 192));
		assignColorToNationality("Vlahi", new Color(128, 128, 255));
		assignColorToNationality("Ucraineni", new Color(255, 255, 85));
		assignColorToNationality("Ruteni", new Color(255, 255, 128));
		assignColorToNationality("Germani", new Color(192, 192, 64));
		assignColorToNationality("Albanezi", new Color(192, 64, 64));
		assignColorToNationality("Muntenegreni", new Color(255, 85, 255));
		assignColorToNationality("Italieni", new Color(64, 192, 64));
		assignColorToNationality("Sloveni", new Color(32, 32, 128));
		assignColorToNationality("Slovaci", new Color(48, 48, 160));
		assignColorToNationality("Cehi", new Color(128, 128, 32));
		assignColorToNationality("Neclasificat", new Color(192, 192, 192));
		assignColorToNationality("Necunoscut", new Color(64, 64, 64));
		assignColorToNationality("Nedeclarat", new Color(64, 64, 64));
		blandifyColors(nationColorMap);

		assignColorToReligion("Ortodocși", new Color(85, 85, 255));
		assignColorToReligion("Musulmani", new Color(255, 85, 85));
		assignColorToReligion("Catolici", new Color(255, 255, 85));
		assignColorToReligion("Alți creștini", new Color(192, 192, 64));
		assignColorToReligion("Protestanți", new Color(0, 192, 0));
		assignColorToReligion("Evrei", new Color(32, 32, 192));
		assignColorToReligion("Religii orientale", new Color(192, 32, 32));
		assignColorToReligion("Fără religie și atei", new Color(128, 128, 128));
		assignColorToReligion("Agnostici și sceptici", new Color(192, 192, 192));
		assignColorToReligion("Necunoscută", new Color(64, 64, 64));
		assignColorToReligion("Nu au declarat religia", new Color(64, 64, 64));
		assignColorToReligion("Alte religii", new Color(32, 32, 32));

		blandifyColors(religionColorMap);
	}

	private static <T extends Object> void blandifyColors(
			final Map<T, Color> colorMap) {
		for (final T key : colorMap.keySet()) {
			final Color color = colorMap.get(key);
			final int[] colorcomps = new int[3];
			colorcomps[0] = color.getRed();
			colorcomps[1] = color.getGreen();
			colorcomps[2] = color.getBlue();

			for (int i = 0; i < colorcomps.length; i++) {
				if (colorcomps[i] == 0) {
					colorcomps[i] = 0x3f;
				} else if (colorcomps[i] == 85) {
					colorcomps[i] = 128;
				} else if (colorcomps[i] == 64) {
					colorcomps[i] = 85;
				} else if (colorcomps[i] == 128) {
					colorcomps[i] = 0x9f;
				}
			}
			colorMap.put(key, new Color(colorcomps[0], colorcomps[1],
					colorcomps[2]));
		}
	}

	private void assignColorToNationality(final String nationalityName,
			final Color color) throws HibernateException {
		final Nationality nat = hib.getNationalityByName(nationalityName);
		if (null != nat) {
			nationColorMap.put(nat, color);
			nationNameMap.put(nat.getName(), nat);
		}
	}

	private void assignColorToReligion(final String religionName,
			final Color color) throws HibernateException {
		final Religion rel = hib.getReligionByName(religionName);
		if (null != rel) {
			religionColorMap.put(rel, color);
			religionNameMap.put(rel.getName(), rel);
		}
	}

	private String getExistingRoTitleOfArticleWithSubject(final Commune com)
			throws IOException {
		final List<String> alternativeTitles = getRoWpCandidateNames(com);

		for (final String candidateTitle : alternativeTitles) {
			final HashMap pageInfo = rowiki.getPageInfo(candidateTitle);
			final Boolean exists = (Boolean) pageInfo.get("exists");
			if (!isTrue(exists)) {
				continue;
			}
			final String pageTitle = defaultString(
					rowiki.resolveRedirect(new String[] { candidateTitle })[0],
					candidateTitle);
			final String[] categs = rowiki.getCategories(pageTitle);
			for (final String categ : categs) {
				if (StringUtils.startsWithAny(categ, "Orașe în Croația",
						"Orașe în cantonul ", "Comune în Croația",
						"Comune în cantonul ")) {
					return pageTitle;
				}
			}
		}
		return null;
	}

	private List<Nationality> getEthnicMinorities(
			final EthnicallyStructurable obshtina) {
		final List<Nationality> ret = new ArrayList<Nationality>();
		for (final Nationality nat : obshtina.getEthnicStructure().keySet()) {
			final double weight = obshtina.getEthnicStructure().get(nat)
					/ (double) obshtina.getPopulation();
			if (weight <= 0.5 && weight > 0.01
					&& !startsWithAny(nat.getName(), "Altele", "Ne")) {
				ret.add(nat);
			}
		}
		return ret;
	}

	private Nationality getMajorityEthnicity(
			final EthnicallyStructurable obshtina) {
		for (final Nationality nat : obshtina.getEthnicStructure().keySet()) {
			if (obshtina.getEthnicStructure().get(nat)
					/ (double) obshtina.getPopulation() > 0.5) {
				return nat;
			}
		}
		return null;
	}

	private String getEthnicityRef(Commune com) {
		return "<ref name=\"hr_census_2011_ethnicity\">{{Citat web|url=http://www.dzs.hr/Eng/censuses/census2011/results/htm/e01_01_04/E01_01_04_zup"
				+ StringUtils.leftPad(String.valueOf(com.getId()), 2, '0')
				+ ".html|publisher=Biroul de Statistică al Croației|accessdate=2013-11-11|title=Componența etnică a cantonului "
				+ com.getCounty().getName() + " pe comune și orașe}}</ref>";
	}

	private String getReligionRef(Commune com) {
		return "<ref name=\"hr_census_2011_religion\">{{Citat web|url=http://www.dzs.hr/Eng/censuses/census2011/results/htm/e01_01_10/E01_01_10_zup"
				+ StringUtils.leftPad(String.valueOf(com.getId()), 2, '0')
				+ ".html|publisher=Biroul de Statistică al Croației|accessdate=2013-11-11|title=Componența confesională a cantonului "
				+ com.getCounty().getName() + " pe comune și orașe}}</ref>";
	}

	private String retrieveName(Commune com) {
		String[] names = StringUtils
				.splitByWholeSeparator(com.getName(), " - ");
		return names[0];
	}
}
