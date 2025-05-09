package ua.hudyma;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.System.*;

public class HydrometParser {

    static final String OUTPUT_DATA_FILENAME = "csv//" + getDate() + ".csv";
    public static final String HYDROMET_URL = "http://gmc.uzhgorod.ua/metdata.php?StNo=33646";
    static Map<String, List<String>> map;
    static Map<String, List<HydrometUnit>> hydroMap;

    public static void main(String[] args) throws IOException {
        parseHydroUrl(false);
        convertCsvToMapList();
        convertMapListToModelMapList();
        var maxTemp = findMaxTemp();
        var minTemp = findMinTemp();
        var maxTempDate = getTempDate(maxTemp).orElseThrow();
        var minTempDate = getTempDate(minTemp).orElseThrow();
        out.println("Max т-ра: " + findMaxTemp() + "° " + maxTempDate);
        out.println("Min т-ра: " + findMinTemp() + "° " + minTempDate);
        var tempList = getTempList(hydroMap);
        //createChart(tempList);
        createChart(hydroMap);
    }

    private static List<Float> getTempList(Map<String, List<HydrometUnit>> hydroMap) {
        return hydroMap
                .values()
                .stream()
                .flatMap(List::stream)
                .map(HydrometUnit::getTemp)
                .toList();
    }

    private static Optional<String> getTempDate(float temp) {
        return hydroMap
                .entrySet()
                .stream()
                .flatMap(e -> e.getValue().stream()
                        .filter(o -> o.temp == temp)
                        .map(o -> e.getKey() + " " + o.time))
                .findFirst();
    }

    private static void parseHydroUrl(boolean disableOverwritingDailyData) throws IOException {
        if (disableOverwritingDailyData && new File(OUTPUT_DATA_FILENAME).exists()) {
            out.println("file exists, skipping parsing");
            return;
        }
        Document document = Jsoup.connect(HYDROMET_URL).get();
        Element table = document.select("table").first();
        Elements rows = table.select("tr");

        try (PrintWriter writer = new PrintWriter(new FileWriter(OUTPUT_DATA_FILENAME))) {
            for (Element row : rows) {
                Elements cols = row.select("td");
                for (Element col : cols) {
                    writer.print(col.text() + ",");
                }
                writer.println();
            }
        }
    }

    private static void convertCsvToMapList() throws FileNotFoundException {
        map = new LinkedHashMap<>();
        var scan = new Scanner(new FileReader(HydrometParser.OUTPUT_DATA_FILENAME));
        scan.nextLine();
        while (scan.hasNext()) {
            var scanLine = scan.nextLine().split(" ");
            String currentDate = scanLine[0];
            var dataStr = scanLine[1];
            dataStr = scanLine.length > 2 ? dataStr += scanLine[2] : dataStr;
            List<String> datalist;
            if (map.containsKey(currentDate)) {
                datalist = map.get(currentDate);
            } else {
                datalist = new ArrayList<>();
            }
            datalist.add(dataStr);
            map.put(currentDate, datalist);
        }
        scan.close();
    }

    private static void convertMapListToModelMapList() {
        hydroMap = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            var key = entry.getKey();
            var rawList = entry.getValue();
            var hydroList = new ArrayList<HydrometUnit>();
            for (String list : rawList) {
                var unit = new HydrometUnit();
                var array = list.split(",");
                unit.setTime(array[0]);
                unit.setTemp(Float.parseFloat(array[1]));
                unit.setDewPoint(Float.parseFloat(array[2]));
                unit.setElements(array[3]);
                unit.setWindDirection(array[5]);
                unit.setWindSpeed(Integer.parseInt(array[6]));
                hydroList.add(unit);
            }
            hydroMap.put(key, hydroList);
        }
        hydroMap = hydroMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new));
    }

    private static String getDate() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
    }

    private static float findMinTemp() {
        return hydroMap
                .values()
                .stream()
                .flatMap(List::stream)
                .map(e -> e.temp)
                .reduce(Math::min)
                .orElseThrow();
    }

    private static float findMaxTemp() {
        return hydroMap
                .values()
                .stream()
                .flatMap(List::stream)
                .map(e -> e.temp)
                .reduce(Math::max)
                .orElseThrow();
    }

    private static void createChart(List<Float> tempList) {
        var dataset = new DefaultCategoryDataset();

        var hrsCounter = 0;
        var dayCounter = 1;

        for (Float ele : tempList) {
            dataset.addValue(ele, "Температура", (hrsCounter + "/" + dayCounter));
            hrsCounter += 3;
            if (hrsCounter == 24) {
                hrsCounter = 0;
                dayCounter++;
            }
        }
        buildChart(dataset);
    }

    private static void createChart(Map<String, List<HydrometUnit>> map) {
        var dataset = new DefaultCategoryDataset();
        for (Map.Entry<String, List<HydrometUnit>> entry : map.entrySet()) {
            for (HydrometUnit hydro : entry.getValue()) {
                dataset.addValue(hydro.temp,
                        "Температура",
                        entry.getKey() + "/" + hydro.time);
            }
        }
        buildChart(dataset);
    }


    private static void buildChart(DefaultCategoryDataset dataset) {
        JFreeChart chart = ChartFactory.createLineChart(
                "Графік температур Пожежевська", "Години", "Температура",
                dataset
        );

        JFrame frame = new JFrame("Гідрометеоцентр України");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new ChartPanel(chart));
        frame.setSize(1200, 400);
        frame.setVisible(true);
    }

    static class HydrometUnit {

        String time, elements, windDirection;

        float temp;

        public float getTemp() {
            return temp;
        }

        float dewPoint;

        int windSpeed;

        public void setTime(String time) {
            this.time = time;
        }

        public void setElements(String elements) {
            this.elements = elements;
        }

        public void setWindDirection(String windDirection) {
            this.windDirection = windDirection;
        }

        public void setWindSpeed(int windSpeed) {
            this.windSpeed = windSpeed;
        }

        public void setTemp(float temp) {
            this.temp = temp;
        }

        public void setDewPoint(float dewPoint) {
            this.dewPoint = dewPoint;
        }
    }


}
