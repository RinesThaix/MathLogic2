package ru.luvas.ml2.utils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 *
 * @author RINES <iam@kostya.sexy>
 */
public class FileWorker {

    public static List<String> read(String filename, List<String> names) {
        List<String> result = new ArrayList<>();
        try {
            Scanner scan = new Scanner(new FileReader(filename));
            while (scan.hasNext()) {
                String s = scan.next().replace("B", "TT").replace("C", "TY").replace("x", "u").replace("A", "(" + names.get(0) + ")");
                if (names.size() > 1) {
                    s = s.replace("TT", "(" + names.get(1) + ")");
                }
                if (names.size() > 2) {
                    s = s.replace("TY", "(" + names.get(2) + ")");
                }
                if (names.size() > 3) {
                    s = s.replace("u", names.get(3));
                }
                result.add(s);
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
        return result;
    }

    public static class FastScanner {

        public BufferedReader reader;
        public StringTokenizer tokenizer;

        public FastScanner(String filename) throws FileNotFoundException {
            this(new FileInputStream(filename));
        }

        public FastScanner(InputStream stream) {
            reader = new BufferedReader(new InputStreamReader(stream), 32768);
            tokenizer = null;
        }

        public String next() {
            try {
                return reader.readLine();
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }

    }

}
