import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class FileContentFilter {

    private static final String DEFAULT_OUTPUT_PATH = ".";
    private static final String DEFAULT_FILE_PREFIX = "";
    private static final String INTEGER_FILE_NAME = "integers.txt";
    private static final String FLOAT_FILE_NAME = "floats.txt";
    private static final String STRING_FILE_NAME = "strings.txt";

    private static final Pattern INTEGER_PATTERN = Pattern.compile("^[+-]?\\d+$");
    private static final Pattern FLOAT_PATTERN = Pattern.compile("^[+-]?\\d+[.,]?\\d*([Ee][+-]?\\d+)?$");

    public static void main(String[] args) {
        String outputPath = DEFAULT_OUTPUT_PATH;
        String filePrefix = DEFAULT_FILE_PREFIX;
        boolean appendMode = false;
        boolean fullStatistics = false;
        List<String> inputFiles = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-o":
                    if (i + 1 < args.length) {
                        outputPath = args[i + 1];
                        i++;
                    } else {
                        System.err.println("Error: -o option requires a path.");
                        return;
                    }
                    break;
                case "-p":
                    if (i + 1 < args.length) {
                        filePrefix = args[i + 1];
                        i++;
                    } else {
                        System.err.println("Error: -p option requires a prefix.");
                        return;
                    }
                    break;
                case "-a":
                    appendMode = true;
                    break;
                case "-s":
                    fullStatistics = false;
                    break;
                case "-f":
                    fullStatistics = true;
                    break;
                default:
                    inputFiles.add(args[i]);
                    break;
            }
        }

        if (inputFiles.isEmpty()) {
            System.err.println("Error: No input files specified.");
            return;
        }

        Path integerFilePath = Paths.get(outputPath, filePrefix + INTEGER_FILE_NAME);
        Path floatFilePath = Paths.get(outputPath, filePrefix + FLOAT_FILE_NAME);
        Path stringFilePath = Paths.get(outputPath, filePrefix + STRING_FILE_NAME);

        IntegerStatistics integerStats = new IntegerStatistics();
        FloatStatistics floatStats = new FloatStatistics();
        StringStatistics stringStats = new StringStatistics();

        boolean integerFileInitialized = false;
        boolean floatFileInitialized = false;
        boolean stringFileInitialized = false;

        for (String inputFile : inputFiles) {
            try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    if (INTEGER_PATTERN.matcher(line).matches()) {
                        try {
                            long value = Long.parseLong(line);
                            if (!appendMode && !integerFileInitialized) {
                                if (Files.exists(integerFilePath)) {
                                    Files.delete(integerFilePath);
                                }
                                integerFileInitialized = true;
                            }
                            writeToFile(integerFilePath, line, true);
                            integerStats.addValue(value);
                        } catch (NumberFormatException e) {
                            System.err.println("Error: Invalid integer format in file " + inputFile + ": " + line);
                        }
                    } else if (FLOAT_PATTERN.matcher(line).matches()) {
                        try {
                            double value = Double.parseDouble(line.replace(",", "."));
                            if (!appendMode && !floatFileInitialized) {
                                if (Files.exists(floatFilePath)) {
                                    Files.delete(floatFilePath);
                                }
                                floatFileInitialized = true;
                            }
                            writeToFile(floatFilePath, line, true);
                            floatStats.addValue(value);

                        } catch (NumberFormatException e) {
                            System.err.println("Error: Invalid float format in file " + inputFile + ": " + line);
                        }
                    } else {
                        if (!appendMode && !stringFileInitialized) {
                            if (Files.exists(stringFilePath)) {
                                Files.delete(stringFilePath);
                            }
                            stringFileInitialized = true;
                        }
                        writeToFile(stringFilePath, line, true);
                        stringStats.addValue(line);
                    }
                }
            } catch (IOException e) {
                System.err.println("Error reading file " + inputFile + ": " + e.getMessage());
            }
        }

        if (fullStatistics) {
            if (integerStats.getCount() > 0) {
                System.out.println("Integer Statistics:");
                System.out.println(integerStats.getFullStatistics());
            }
            if (floatStats.getCount() > 0) {
                System.out.println("Float Statistics:");
                System.out.println(floatStats.getFullStatistics());
            }
            if (stringStats.getCount() > 0) {
                System.out.println("String Statistics:");
                System.out.println(stringStats.getFullStatistics());
            }
        } else {
            if (integerStats.getCount() > 0) {
                System.out.println("Integer Count: " + integerStats.getCount());
            }
            if (floatStats.getCount() > 0) {
                System.out.println("Float Count: " + floatStats.getCount());
            }
            if (stringStats.getCount() > 0) {
                System.out.println("String Count: " + stringStats.getCount());
            }
        }
    }

    private static void writeToFile(Path filePath, String line, boolean append) {
        try {
            if (filePath.getParent() != null && !Files.exists(filePath.getParent())) {
                Files.createDirectories(filePath.getParent());
            }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile(), append))) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error writing to file " + filePath + ": " + e.getMessage());
        }
    }

    static class IntegerStatistics {
        private int count = 0;
        private long min = Long.MAX_VALUE;
        private long max = Long.MIN_VALUE;
        private long sum = 0;

        public void addValue(long value) {
            count++;
            min = Math.min(min, value);
            max = Math.max(max, value);
            sum += value;
        }

        public int getCount() {
            return count;
        }

        public String getFullStatistics() {
            if (count == 0) {
                return "No data available.";
            }
            double average = (double) sum / count;
            return "Count: " + count + ", Min: " + min + ", Max: " + max + ", Sum: " + sum + ", Average: " + average;
        }
    }

    static class FloatStatistics {
        private int count = 0;
        private double min = Double.MAX_VALUE;
        private double max = Double.MIN_VALUE;
        private double sum = 0;

        public void addValue(double value) {
            count++;
            min = Math.min(min, value);
            max = Math.max(max, value);
            sum += value;
        }

        public int getCount() {
            return count;
        }

        public String getFullStatistics() {
            if (count == 0) {
                return "No data available.";
            }
            double average = sum / count;
            return "Count: " + count + ", Min: " + min + ", Max: " + max + ", Sum: " + sum + ", Average: " + average;
        }
    }

    static class StringStatistics {
        private int count = 0;
        private int minLength = Integer.MAX_VALUE;
        private int maxLength = Integer.MIN_VALUE;

        public void addValue(String value) {
            count++;
            minLength = Math.min(minLength, value.length());
            maxLength = Math.max(maxLength, value.length());
        }

        public int getCount() {
            return count;
        }

        public String getFullStatistics() {
            if (count == 0) {
                return "No data available.";
            }
            return "Count: " + count + ", Min Length: " + minLength + ", Max Length: " + maxLength;
        }
    }
}