package edu.umich.templar.task;

import com.opencsv.CSVReader;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryTaskReader {
    public static List<QueryTask> readQueryTasks(String filename) {
        Map<Integer, QueryTask> queryTasks = new HashMap<>();

        try {
            CSVReader reader = new CSVReader(new FileReader(filename));

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
                String answer = nextLine[6];

                QueryTask task = queryTasks.get(queryId);
                if (task == null) {
                    task = new QueryTask(queryId);
                    queryTasks.put(queryId, task);
                }

                task.addMapping(new FragmentTask(phrase, op, type, functions, groupBy, answer));
            }
            reader.close();
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
