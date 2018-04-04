package edu.umich.templar.task;

import com.opencsv.CSVReader;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.*;

public class QueryTaskReader {
    public static List<QueryTask> readQueryTasks(String keywordFile, String joinFile) {
        Map<Integer, QueryTask> queryTasks = new HashMap<>();

        try {
            CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(keywordFile), "UTF-8"));

            // Skip first header line
            reader.readNext();

            // Read all fragmentTasks in from CSV file
            String [] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                int queryId = Integer.valueOf(nextLine[0]);
                String phrase = nextLine[1];
                String op = nextLine[2];
                String type = nextLine[3];

                List<String> functions = new ArrayList<>();
                String[] functionList = nextLine[4].split(",");
                for (String func : functionList) {
                    if (!func.trim().isEmpty()) functions.add(func.trim());
                }

                boolean groupBy = false;
                if (nextLine[5].equalsIgnoreCase("1")) {
                    groupBy = true;
                }
                List<String> answers = Arrays.asList(nextLine[6].split(";"));

                QueryTask task = queryTasks.get(queryId);
                if (task == null) {
                    task = new QueryTask(queryId);
                    queryTasks.put(queryId, task);
                }

                task.addMapping(new FragmentTask(phrase, op, type, functions, groupBy, answers));
            }
            reader.close();

            CSVReader joinReader = new CSVReader(new InputStreamReader(new FileInputStream(joinFile), "UTF-8"));
            // Skip first header line
            joinReader.readNext();

            // Read all join path answers in from CSV file
            String [] line;
            while ((line = joinReader.readNext()) != null) {
                int queryId = Integer.valueOf(line[0]);
                List<String> joinAnswers = Arrays.asList(line[1].split(";"));
                queryTasks.get(queryId).setJoinAnswers(joinAnswers);
            }
            joinReader.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        List<QueryTask> qtList = new ArrayList<>();
        for (Map.Entry<Integer, QueryTask> e : queryTasks.entrySet()) {
            qtList.add(e.getValue());
        }

        return qtList;
    }
}
