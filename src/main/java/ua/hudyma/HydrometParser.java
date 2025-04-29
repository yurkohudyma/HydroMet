package ua.hudyma;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.lang.System.*;

public class HydrometParser {

    static final String OUTPUT_DATA_FILENAME = "csv//" + getDate() + ".csv";
    public static final String HYDROMET_URL = "http://gmc.uzhgorod.ua/metdata.php?StNo=33646";
    static Map<String, List<String>> map;
    static Map<String, List<HydrometUnit>> hydroMap;

    public static void main(String[] args) throws IOException {
        parseHydroUrl();
        convertCsvToMapList();
        convertMapListToModelMapList();
        out.println("Max т-ра: " + findMaxTemp() + " град");
        out.println("Min т-ра: " + findMinTemp() + " град");
    }

    private static void parseHydroUrl() throws IOException {
        if (new File(OUTPUT_DATA_FILENAME).exists()) {
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
        map = new HashMap<>();
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
        hydroMap = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            var key = entry.getKey();
            var rawList = entry.getValue();
            var hydroList = new ArrayList<HydrometUnit>();
            for (String list : rawList) {
                var unit = new HydrometUnit();
                var array = list.split(",");
                unit.setTime(array[0]);
                if (array[1].startsWith(".")) {
                    unit.setTemp("0" + array[1]);
                } else {
                    unit.setTemp(array[1]);
                }
                if (array[2].startsWith(".")) {
                    unit.setDewPoint("0" + array[2]);
                } else {
                    unit.setDewPoint(array[2]);
                }
                unit.setElements(array[3]);
                unit.setWindDirection(array[5]);
                unit.setWindSpeed(array[6]);
                hydroList.add(unit);
            }
            hydroMap.put(key, hydroList);
        }
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
                .map(Float::parseFloat)
                .reduce(Math::min).orElseThrow();
    }

    private static float findMaxTemp() {
        return hydroMap
                .values()
                .stream()
                .flatMap(List::stream)
                .map(e -> e.temp)
                .map(Float::parseFloat)
                .reduce(Math::max)
                .orElseThrow();
    }

    static class HydrometUnit {

        String time, temp, dewPoint, elements, windDirection, windSpeed;

        public void setTime(String time) {
            this.time = time;
        }

        public void setTemp(String temp) {
            this.temp = temp;
        }

        public void setDewPoint(String dewPoint) {
            this.dewPoint = dewPoint;
        }

        public void setElements(String elements) {
            this.elements = elements;
        }

        public void setWindDirection(String windDirection) {
            this.windDirection = windDirection;
        }

        public void setWindSpeed(String windSpeed) {
            this.windSpeed = windSpeed;
        }

        public HydrometUnit() {

        }

    }


}
