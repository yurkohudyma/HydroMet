package ua.hudyma;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class HydrometParser {
    public static void main(String[] args) throws IOException {
        parseHydroUrl();
        var map = convertCsvToMapList("csv//output.csv");
        var hydroMap = convertMapListToModelMapList(map);
        System.out.println(hydroMap);
    }

    private static Map<String, List<HydrometUnit>> convertMapListToModelMapList(Map<String, List<String>> map) {
        var hydroMap = new HashMap<String, List<HydrometUnit>>();
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
                if (array[2].startsWith(".")){
                    unit.setDewPoint("0" + array[2]);
                }
                else {
                    unit.setDewPoint(array[2]);
                }
                unit.setDewPoint(array[2]);
                unit.setElements(array[3]);
                unit.setWindDirection(array[5]);
                unit.setWindSpeed(array[6]);
                hydroList.add(unit);
            }
            hydroMap.put(key, hydroList);
        }
        return hydroMap;
    }

    private static void parseHydroUrl() throws IOException {
        String url = "http://gmc.uzhgorod.ua/metdata.php?StNo=33646";
        Document document = Jsoup.connect(url).get();
        Element table = document.select("table").first();
        Elements rows = table.select("tr");
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        try (PrintWriter writer = new PrintWriter(new FileWriter("csv//" + date + ".csv"))) {
            for (Element row : rows) {
                Elements cols = row.select("td");
                for (Element col : cols) {
                    writer.print(col.text() + ",");
                }
                writer.println();
            }
        }
    }

    private static Map<String, List<String>> convertCsvToMapList(String filename) throws FileNotFoundException {
        var map = new HashMap<String, List<String>>();
        var scan = new Scanner(new FileReader(filename));
        scan.nextLine();
        while (scan.hasNext()) {
            var scanLine = scan.nextLine().split(" ");
            String currentDate = scanLine[0];
            var dataStr = scanLine[1];
            dataStr = scanLine.length > 2 ? dataStr += scanLine[2] : dataStr;
            List<String> datalist;
            if (map.containsKey(currentDate)) {
                datalist = map.get(currentDate);
                datalist.add(dataStr);
                map.put(currentDate, datalist);
            } else {
                datalist = new ArrayList<>();
                datalist.add(dataStr);
                map.put(currentDate, datalist);
            }
        }
        scan.close();
        return map;
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
