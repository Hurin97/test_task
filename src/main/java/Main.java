import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) {
        Map<String,String> path = getPathData(args);
        long start = System.currentTimeMillis();
        List<String> lines = getLinesFromFile(path.get("firstPath"));
        List<List<String>> lineGroups= findGroupsOfLine(lines);
        getResultOfGrouping(lineGroups,path.get("secondPath"));
        long finish=System.currentTimeMillis();
        System.out.println("Running time:" + ((finish - start) / 1000f) + " seconds");
    }

    private static Map<String,String> getPathData(String[] args) {
        Map<String,String> path = new HashMap<>();
        String firstPath;
        String secondPath;
        if(args.length>1) {
            firstPath = args[0];
            secondPath = args[1];
        }
        else {
            System.out.println("Please enter path to input file: ");
            Scanner in= new Scanner(System.in);
            firstPath = in.nextLine();
            System.out.println("Please enter path to output file: ");
            secondPath = in.nextLine();
        }
        if(firstPath.isEmpty()){
            System.out.println("Because you did not enter the path to the file the program was terminated");
            System.exit(0);
        }
        if(secondPath.isEmpty()) {
            System.out.println("Because you did not enter an output path, the file will be created in the default path");
            secondPath="output.txt";
        }
        path.put("firstPath", firstPath);
        path.put("secondPath",secondPath);
        return path;
    }

    private static List<String> getLinesFromFile(String path) {
        List<String> lines = new ArrayList<>();
        try(Stream<String> linesFromFile = Files.lines(Paths.get(path))){
                lines=linesFromFile.distinct()
                    .filter(f -> !f.matches("(.*)\\w++\"\\w++(.*)"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    @FunctionalInterface
    public interface ThrowingConsumer<T, E extends Exception> {
        void accept(T t) throws E;
    }

    private static  <T, E extends Exception> Consumer<T> exceptionWrapper (ThrowingConsumer<T, E> throwingConsumer,
                                                                           Class<E> exceptionClass) {
        return i -> {
            try {
                throwingConsumer.accept(i);
            } catch (Exception ex) {
                try {
                    E exCast = exceptionClass.cast(ex);
                    System.err.println(
                            "Exception occured : " + exCast.getMessage());
                } catch (ClassCastException ccEx) {
                    throw new RuntimeException(ex);
                }
            }
        };
    }

    private static List<List<String>> findGroupsOfLine(List<String> lines) {
        class NewLineElement {
            private String lineElement;
            private int columnNum;

            private NewLineElement(String lineElement, int columnNum) {
                this.lineElement = lineElement;
                this.columnNum = columnNum;
            }
        }
        if (lines == null)
            return Collections.emptyList();
        List<List<String>> linesGroups = new ArrayList<>();
        if (lines.size() < 2) {
            linesGroups.add(lines);
            return linesGroups;
        }
        List<Map<String, Integer>> columns = new ArrayList<>();
        Map<Integer, Integer> unitedGroups = new HashMap<>();
        for (String line : lines) {
            String[] lineElements = line.split(";");
            TreeSet<Integer> groupsWithSameElements = new TreeSet<>();
            List<NewLineElement> newElements = new ArrayList<>();
            for (int elmIndex = 0; elmIndex < lineElements.length; elmIndex++) {
                String currentLineElement = lineElements[elmIndex];
                columns=ifColumnsSizeEqualsElementIndex(columns,elmIndex);
                if (currentElementEmpty(currentLineElement)==true)
                        continue;
                Map<String, Integer> currentColumn = columns.get(elmIndex);
                Integer elementGroupNumber = currentColumn.get(currentLineElement);
                if (elementGroupNumber != null) {
                    while (unitedGroups.containsKey(elementGroupNumber))
                        elementGroupNumber = unitedGroups.get(elementGroupNumber);
                    groupsWithSameElements.add(elementGroupNumber);
                } else {
                    newElements.add(new NewLineElement(currentLineElement, elmIndex));
                }
            }
            int groupNumber;
            if (groupsWithSameElements.isEmpty()) {
                linesGroups.add(new ArrayList<>());
                groupNumber = linesGroups.size() - 1;
            } else {
                groupNumber = groupsWithSameElements.first();
            }
            for (NewLineElement newLineElement : newElements) {
                columns.get(newLineElement.columnNum).put(newLineElement.lineElement, groupNumber);
            }
            for (int matchedGrNum : groupsWithSameElements) {
                if (matchedGrNum != groupNumber) {
                    unitedGroups.put(matchedGrNum, groupNumber);
                    linesGroups.get(groupNumber).addAll(linesGroups.get(matchedGrNum));
                    linesGroups.set(matchedGrNum, null);
                }
            }
            linesGroups.get(groupNumber).add(line);
        }
        linesGroups.removeAll(Collections.singleton(null));
        return linesGroups;
    }

    private static List<Map<String, Integer>> ifColumnsSizeEqualsElementIndex(List<Map<String,Integer>> columns,
                                                                       int elementIndex) {
        if (columns.size() == elementIndex)
            columns.add(new HashMap<>());
        return columns;
    }

    private static boolean currentElementEmpty(String currentElement) {
        if ("".equals(currentElement.replaceAll("\"","").trim()))
            return true;
        else
            return false;
    }

    private static void getResultOfGrouping (List<List<String>> lines,String path) {
        try (OutputStream outputStream = Files.newOutputStream(Paths.get(path))) {
            outputStream.write(("Total groups with 2 or more lines:" + lines.stream().filter(f -> f.size() > 1)
                    .count() + "\n").getBytes());
            lines.stream().filter(o -> o.size() > 1)
                    .sorted((s1, s2) -> s2.size() - s1.size())
                    .peek(exceptionWrapper(p ->outputStream.write(("group #" + lines.indexOf(p) + "\n").getBytes()), IOException.class))
                    .forEach(f -> f.stream()
                    .forEach(exceptionWrapper(x -> outputStream.write(("|" + x.toString() + "\n").getBytes()), IOException.class)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
