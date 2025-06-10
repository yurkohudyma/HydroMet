package ua.hudyma;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Scanner;

public class PressureChart {

    public static void main(String[] args) throws FileNotFoundException {

        var sc = new Scanner(new FileReader("res//data"));
        int n = sc.nextInt();
        var values = new int[n];
        for (int i = 0; i < n; i++) {
            values[i] = sc.nextInt();
        }
        sc.close();

        var dataset = new DefaultCategoryDataset();
        for (int i = 0; i < values.length; i++) {
            dataset.addValue(values[i], "Значення", String.valueOf(i));
        }

        var chart = ChartFactory.createLineChart(
                "Графік змін значень", // Назва графіка
                "Індекс",              // Вісь X
                "Значення",            // Вісь Y
                dataset
        );

        var frame = new JFrame("Графік");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new ChartPanel(chart));
        frame.setSize(1200, 500);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
