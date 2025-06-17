package org.wikipedia.ro.java;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikibase.WikibaseException;
import org.wikipedia.Wiki;
import org.wikipedia.ro.java.model.Axis;
import org.wikipedia.ro.java.model.Chart;
import org.wikipedia.ro.java.model.ChartDataSet;
import org.wikipedia.ro.java.model.ChartField;
import org.wikipedia.ro.utility.AbstractExecutable;
import org.wikipedia.ro.utils.Credentials;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Graph2ChartMigrator extends AbstractExecutable
{

    private static final Pattern DEMO_TEMPLATE_PATTERN = Pattern.compile("\\{\\{[Gg]rafic demografie[\\|/][^\\}]*\\}\\}");
    
    private Wiki cwiki;
    
    private static final Logger LOG = LoggerFactory.getLogger(Graph2ChartMigrator.class);

    protected void init () throws FailedLoginException, IOException {
        super.init();
        cwiki = Wiki.newSession("commons.wikimedia.org");
        Credentials commonsCredentials = identifyCredentials("cwiki");
        cwiki.login(commonsCredentials.username, commonsCredentials.password);
        cwiki.setMarkBot(true);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (null != cwiki) cwiki.logout();
            }
        });
    }
    
    @Override
    protected void execute() throws IOException, WikibaseException, LoginException
    {
        
        List<String> demoSubpages = wiki.listPages("Format:Grafic demografie/", null, Wiki.ALL_NAMESPACES, -1, -1, false);
        for (String demoSubpage : demoSubpages)
        {
            //migrateOnePage(demoSubpage);
        }
        //migrateOnePage("Format:Grafic demografie/Aluniș");
        //migrateOnePage("Format:Grafic demografie/Alba Iulia");
        migrateOnePage("Format:Grafic demografie/Peschiera del Garda");
        //migrateOnePage("Format:Grafic demografie/Bălan");
        //migrateOnePage("Format:Grafic demografie/Constanța");
        
    }

    private void migrateOnePage(String demographyPage)
    {
        try
        {
            String town = demographyPage.substring("Format:Grafic Demografie/".length());
            String csvContent = this.wiki.getPageText(List.of(demographyPage)).stream().findFirst().get();
            if (csvContent.contains("{{#chart:")) {
                LOG.info("Page {} already migrated. Skipping.", demographyPage);
                return;
            }

            String dataSetPage = createDataSetPage(csvContent, town);
            String chartDefinitionPage = createChartDefinitionPage(town, dataSetPage);

            replaceChartWithGraph(demographyPage, chartDefinitionPage);
        }
        catch (IOException | LoginException e)
        {
            LOG.error("IO Exception when migrating {}", demographyPage, e);
        }
    }

    private void replaceChartWithGraph(String demographyPage, String chartDefinitionPage) throws IOException, LoginException
    {
        wiki.edit(demographyPage, "{{#chart:" + chartDefinitionPage.substring("Data:".length()) + "}}", "Robot: înlocuit [[:mw:Extension:Graph|Graph]] cu [[:mw:Extension:Chart|Chart]]");
    }

    private String createDataSetPage(String csvContent, String place) throws LoginException, IOException
    {
        ChartDataSet dataSet = extractChartDataSetFromCsv(csvContent, place);

        String targetPage = String.format("Data:Historical population %s.tab", place);
        String jsonDataSet = toJson(dataSet);
        
        //save jsonDataSet to commons page targetPage
        cwiki.edit(targetPage, jsonDataSet, "Bot: imported data from [[:w:ro:Format:Grafic demografie/" + place + "]]");
        
        return targetPage;
    }

    private String createChartDefinitionPage(String place, String chartSource) throws LoginException, IOException
    {
        String chartDefinitionPage = String.format("Data:HistoricalDemography.%s.chart", place);
        Chart demographyChart = new Chart();
        demographyChart.setLicense("CC0-1.0");
        demographyChart.setSource(chartSource.substring("Data:".length()));
        demographyChart.setVersion(1);

        Axis xAxis = new Axis();
        xAxis.setFormat(false);
        xAxis.getTitle().put("ro", "An");
        xAxis.getTitle().put("en", "Year");

        Axis yAxis = new Axis();
        yAxis.setFormat(true);
        yAxis.getTitle().put("ro", "Populație");
        yAxis.getTitle().put("en", "Population");

        demographyChart.setXAxis(xAxis);
        demographyChart.setYAxis(yAxis);
        demographyChart.setType("bar");
        demographyChart.getTitle().put("ro", String.format("Populația istorică din %s", place));
        demographyChart.getTitle().put("en", String.format("Historical population for %s", place));
        String jsonDemographyChart = toJson(demographyChart);
        //System.out.println(jsonDemographyChart);
        
        //save jsonDemographyChart to commons page chartDefinitionPage
        cwiki.edit(chartDefinitionPage, jsonDemographyChart, "Bot: imported chart from [[:w:ro:Format:Grafic demografie/" + place + "]]");
        
        return chartDefinitionPage;
    }

    protected ChartDataSet extractChartDataSetFromCsv(String csv, String place)
    {
        ChartDataSet ret = new ChartDataSet();
        ret.setLicense("CC0-1.0");
        ret.getDescription().put("ro", String.format("Populația istorică din %s", place));
        ret.getDescription().put("en", String.format("Historical population of %s", place));

        String[] csvLines = csv.split("[\\n\\r]+");
        String[] header = csvLines[0].split(",");
        List<ChartField> chartFields = Arrays.stream(header).map(String::trim).map(x -> {
            ChartField cf = new ChartField();
            cf.setName(x);
            cf.getTitle().put("ro", x);
            cf.setType("number");
            return cf;
        }).collect(Collectors.toList());
        ret.getSchema().setFields(chartFields);

        List<List<Object>> dataLines = new ArrayList<>();
        for (int i = 1; i < csvLines.length; i++)
        {
            String[] line = csvLines[i].split(",");
            if (Arrays.stream(line).map(String::trim).filter(x -> x.matches("\\d+")).count() == chartFields.size())
            {
                List<Object> lineData = Arrays.stream(line).map(String::trim).map(Integer::valueOf).collect(Collectors.toList());
                dataLines.add(lineData);
            }
        }
        ret.setData(dataLines);
        return ret;
    }

    private static String toJson(Object obj) throws JsonProcessingException
    {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(obj);
    }
}
