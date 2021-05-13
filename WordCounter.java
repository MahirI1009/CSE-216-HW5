import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WordCounter {

    public static final Path FOLDER_OF_TEXT_FILES  = Paths.get("..."); // path to the folder where input text files are located
    public static final Path WORD_COUNT_TABLE_FILE = Paths.get("..."); // path to the output plain-text (.txt) file
    public static final int  NUMBER_OF_THREADS     = 2;

    public static class RunnableImpl implements Runnable {
        Scanner sc;
        Map<String, String> words = new TreeMap<>();

        public RunnableImpl(Scanner sc) {this.sc = sc;}

        @Override
        public void run() {
            while (sc.hasNext()) {
                String str = sc.next().toLowerCase().replaceAll("\\p{Punct}", "");
                if (!words.containsKey(str))
                    words.put(str, Integer.toString(1));
                else
                    words.replace(str, Integer.toString(Integer.parseInt(words.get(str)) + 1));
            }
        }
        public Map<String, String> getMap() {return words;}
    }

    public static void main(String[] args) throws Exception {

        File[] files = FOLDER_OF_TEXT_FILES.toFile().listFiles();

        Scanner[] sc;
        if (files != null)
            sc = new Scanner[files.length];
        else throw new FileNotFoundException("There are no text files in the folder");

        for (int i = 0; i < files.length; i++)
            sc[i] = new Scanner(files[i]);

        Map<String, Map<String, String>> wMap = new ConcurrentHashMap<>();

        RunnableImpl r;
        ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
        for (int i = 0; i < sc.length; i++) {
            r = new RunnableImpl(sc[i]);
            wMap.put(files[i].getName(), r.getMap());
            executor.execute(r);
        }

        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);

        Map<String, List<Map<String, String>>> newMap = new TreeMap<>();
        List<Map<String, String>> temp;
        for (Map.Entry<String, Map<String, String>> entry : wMap.entrySet()) {
            temp = new ArrayList<>();
            temp.add(entry.getValue());
            newMap.put(entry.getKey(), temp);
        }

        List<List<String>> s = new ArrayList<>();

        for (Map.Entry<String, List<Map<String, String>>> m : newMap.entrySet()) {
            List<String> ss = new ArrayList<>();
            for (Map<String, String> l : m.getValue()) {
                for (Map.Entry<String, String> e : l.entrySet()) {
                    if (!s.contains(e.getKey()))
                        ss.add(e.getKey());
                }
            }
            s.add(ss);
        }

        List<String> wordsOnOwn = new ArrayList<>();
        for (List<String> o : s) {
            for (String oo : o) {
                if (!wordsOnOwn.contains(oo))
                    wordsOnOwn.add(oo);
            }
        }

        List<Map<String, String>> lMaps = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, String>>> m : newMap.entrySet()) {
            lMaps.addAll(m.getValue());
        }

        Map<String, List<String>> wordMap = new TreeMap<>();
        List<String> ints;
        for (String value : wordsOnOwn) {
            ints = new ArrayList<>();
            for (Map<String, String> lMap : lMaps)
                if (lMap.containsKey(value))
                    ints.add(lMap.get(value));
                else ints.add(Integer.toString(0));
            wordMap.put(value, ints);
        }

        List<List<String>> out = new ArrayList<>();
        for(Map.Entry<String, List<String>> w : wordMap.entrySet()) {
            out.add(w.getValue());
        }

        int l = (wordMap.keySet().stream()
                .reduce((s1, s2) -> s1.length() == s2.length() ? s1.compareTo(s2) > 0 ? s1 : s2 : s1.length() > s2.length() ? s1 : s2))
                .orElse("").length()+1;

        StringBuilder header = Optional.ofNullable(String.format("%" + l + "s", "")).map(StringBuilder::new).orElse(null);

        ArrayList<String> fn = new ArrayList<>();
        for (File file : files)
            fn.add(file.getName());

        for (String n : fn) {
            int sp = n.length()-4;
            String temp2 = n.substring(0, sp);
            header = (header == null ? new StringBuilder("null") : header).append(String.format("%-" + n.length() + "s", temp2));
        }

        List<String> tots = new ArrayList<>();

        int total;
        for (List<String> strings : out) {
            total = 0;
            for (String string : strings) total += Integer.parseInt(string);
            tots.add(Integer.toString(total));
        }

        header = (header == null ? new StringBuilder("null") : header).append(String.format("%-" + fn.get(fn.size() - 1).length() + "s", "total"));

        List<String> wordsOut = new ArrayList<>(wordMap.keySet());

        List<String> strs = new ArrayList<>();
        for (int i = 0; i < out.size(); i++) {
            int k = l - out.get(i).get(1).length()+1;
            StringBuilder add = Optional.ofNullable(String.format("%-" + k + "s", wordsOut.get(i))).map(StringBuilder::new).orElse(null);
            for (int j = 0; j < out.get(i).size(); j++)
                add = (add == null ? new StringBuilder("null") : add).append(String.format("%-" + fn.get(j).length() + "s", out.get(i).get(j)));
            add = (add == null ? new StringBuilder("null") : add).append(String.format("%-" + fn.get(fn.size() - 1).length() + "s", tots.get(i)));
            strs.add(add.toString());
        }

        File output = WORD_COUNT_TABLE_FILE.toFile();

        PrintWriter printWriter = new PrintWriter(output);

        printWriter.println(header);
        for(String str : strs)
            printWriter.println(str);

        for(Scanner scs : sc)
            scs.close();

        printWriter.close();
    }
}
