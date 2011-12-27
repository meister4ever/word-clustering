import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.Counters;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.RunningJob;
import org.apache.hadoop.mapred.TextOutputFormat;
import org.apache.hadoop.mapreduce.Counter;

import edu.umd.cloud9.collection.wikipedia.WikipediaPage;
import edu.umd.cloud9.collection.wikipedia.WikipediaPageInputFormat;

@SuppressWarnings("deprecation")
public class LydiaEntityVectorCreator {

  private static final Long NUM_WORDS = (long) 1594793586;

  public static enum MyCounter {
    INPUT_WORDS, INPUT_ARTICLES, SAMPLED_ARTICLES, NUM_SENTENCES
  };

  private static class MyMapper extends MapReduceBase implements
      Mapper<LongWritable, Text, Text, LongWritable> {
    private int window = 3;
    private int samplePercentage = 10;
    private WordFrequencyUtil wfUtil;
    private List<String> entityList;
    private LongWritable one = new LongWritable(1);

    @Override
    public void configure(JobConf job) {
      window = job.getInt("window", 3);
      samplePercentage = job.getInt("samplePercentage", 25);
      String topWordsFile = job.get("topWords");
      String entityFile = job.get("entityFile");
      wfUtil = new WordFrequencyUtil(topWordsFile, job);
      entityList = new ArrayList<String>();
      String entityListStr = HdfsFileUtil.ReadFileContent(entityFile, job);
      for (String entity : entityListStr.split("\n")) {
        entityList.add(entity);
      }
    }

    @Override
    public void map(LongWritable key, Text newsText,
        OutputCollector<Text, LongWritable> output, Reporter reporter)
        throws IOException {

      Double randomNumber = Math.random() * 100;
      if (randomNumber.intValue() > samplePercentage) {
        return;
      }

      String text = newsText.toString();

      String[] sents = text.split("\\.|\\?");
      reporter.incrCounter(MyCounter.NUM_SENTENCES, sents.length);
      for (String sent : sents) {
        List<String> sentenceEntities = new ArrayList<String>();
        sent = sent.toLowerCase();
        for (String entity : entityList) {
          if (sent.indexOf(entity) != -1) {
            sentenceEntities.add(entity);
            sent = sent.replaceAll(entity, "");
          }
        }
        String[] terms = sent.split("\\s+");
        for (int i = 0; i < terms.length; i++) {
          String term = terms[i];
          String storedWordi = StringUtil.clean(term);
          if (!wfUtil.isPresent(storedWordi)) {
            continue;
          }
          reporter.incrCounter(MyCounter.INPUT_WORDS, 1);

          for (String entity : sentenceEntities) {
            try {
              output.collect(new Text(entity + "" + storedWordi), one);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
      }
    }
  }

  public static class MyReducer extends MapReduceBase implements
      Reducer<Text, LongWritable, Text, LongWritable> {


    @Override
    public void configure(JobConf job) {
    }

    @Override
    public void reduce(Text key, Iterator<LongWritable> values,
        OutputCollector<Text, LongWritable> output, Reporter reporter)
        throws IOException {
      long count = 0;
      while (values.hasNext()) {
        count += values.next().get();
      }

      if (count >= 100) {
        output.collect(key, new LongWritable(count));
      }
    }
  }

  /**
   * Creates an instance of this tool.
   */
  public LydiaEntityVectorCreator() {
  }

  private static int printUsage() {
    System.out
        .println("usage: [input-path] [output-path] [window] [num-reducers] [stopwords] [sample-percentage] [entity-file]");
    return -1;
  }

  /**
   * Runs this tool.
   */
  public static void main(String[] args) throws Exception {
    if (args.length != 7) {
      printUsage();
      return;
    }

    String inputPath = args[0];
    String outputPath = args[1];

    int window = Integer.parseInt(args[2]);
    int reduceTasks = Integer.parseInt(args[3]);
    int samplePercentage = Integer.parseInt(args[5]);

    JobConf conf = new JobConf(LydiaEntityVectorCreator.class);
    System.out.println(" - input path: " + inputPath);
    System.out.println(" - output path: " + outputPath);
    System.out.println(" - window: " + window);
    System.out.println(" - number of reducers: " + reduceTasks);
    System.out.println(" - wordFrequencies: " + args[4]);
    System.out.println(" - samplePercentage: " + args[5]);
    System.out.println(" - entityList: " + args[6]);

    conf.set("topWords", args[4]);
    conf.set("entityFile", args[6]);
    conf.setInt("samplePercentage", samplePercentage);
    conf.setJobName("Cooccurence");
    conf.setNumReduceTasks(reduceTasks);

    //conf.set("mapred.task.timeout", "12000000");
    //conf.set("mapred.child.java.opts", "-Xmx4000M -Xms2000M");

    //conf.setInputFormat(TextInputFormat.class);
    //conf.setOutputFormat(TextOutputFormat.class);

    conf.setMapperClass(MyMapper.class);
    conf.setCombinerClass(MyReducer.class);
    conf.setReducerClass(MyReducer.class);
    conf.setMapOutputKeyClass(Text.class);
    conf.setMapOutputValueClass(LongWritable.class);
    conf.setOutputKeyClass(Text.class);
    conf.setOutputValueClass(LongWritable.class);

    FileInputFormat.setInputPaths(conf, new Path(inputPath));
    FileOutputFormat.setOutputPath(conf, new Path(outputPath));

    long startTime = System.currentTimeMillis();
    RunningJob job = JobClient.runJob(conf);
    System.out.println("Job Finished in "
        + (System.currentTimeMillis() - startTime) / 1000.0
        + " seconds");
    Counters counters = job.getCounters();
    Counter totalSentences = counters.findCounter(MyCounter.NUM_SENTENCES);
  }
}
