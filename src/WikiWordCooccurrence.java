import java.io.IOException;
import java.net.URI;
import java.util.Iterator;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.TextOutputFormat;

import edu.umd.cloud9.collection.wikipedia.WikipediaPage;
import edu.umd.cloud9.collection.wikipedia.WikipediaPageInputFormat;

@SuppressWarnings("deprecation")
public class WikiWordCooccurrence {

  private static final Long NUM_WORDS = (long) 1594793586;

  private static class MyMapper extends MapReduceBase implements
      Mapper<LongWritable, WikipediaPage, Text, IntWritable> {
    private int window = 3;
    private WordFrequencyUtil wfUtil;
    private IntWritable one = new IntWritable(1);

    protected static enum MyCounter {
      INPUT_WORDS, INPUT_ARTICLES
    };

    @Override
    public void configure(JobConf job) {
      window = job.getInt("window", 3);
      String topWordsFile = job.get("topWords");
      wfUtil = new WordFrequencyUtil(topWordsFile, job);
    }

    @Override
    public void map(LongWritable key, WikipediaPage page,
        OutputCollector<Text, IntWritable> output, Reporter reporter)
        throws IOException {
      if (!page.isArticle()) {
        return;
      }
      String text = null;
      try {
        text = page.getContent();
      } catch (Exception e) {
        return;
      }

      reporter.incrCounter(MyCounter.INPUT_ARTICLES, 1);

      String[] sents = text.split("\\.|\\?");
      for (String sent : sents) {
        String[] terms = sent.split("\\s+");
        for (int i = 0; i < terms.length; i++) {
          String term = terms[i];
          String storedWordi = wfUtil.getStoredString(terms[i]);
          reporter.incrCounter(MyCounter.INPUT_WORDS, 1);
          if (!wfUtil.isPresent(term)) {
            continue;
          }
          //for (int j = i - window; j < i + window + 1; j++) {
          for (int j = 0; j < terms.length; j++) {
            if (j == i || j < 0)
              continue;

            if (j >= terms.length)
              break;

            if (!wfUtil.isPresent(terms[j])) {
              continue;
            }

            String storedWordj = wfUtil.getStoredString(terms[j]);
            if (storedWordi.compareTo(storedWordj) == 0) {
              continue;
            }

            String outputKey = storedWordi + ":" + storedWordj;
            try {
              output.collect(new Text(outputKey), one);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
      }
    }
  }

  public static class MyReducer extends MapReduceBase implements
      Reducer<Text, IntWritable, Text, DoubleWritable> {

    private int window = 6;
    private WordFrequencyUtil wfUtil;

    @Override
    public void configure(JobConf job) {
      window = 2 * job.getInt("window", 3);
      String topWordsFile = job.get("topWords");
      wfUtil = new WordFrequencyUtil(topWordsFile, job);
    }

    @Override
    public void reduce(Text key, Iterator<IntWritable> values,
        OutputCollector<Text, DoubleWritable> output, Reporter reporter)
        throws IOException {
      int count = 0;
      while (values.hasNext()) {
        count += values.next().get();
      }

      String[] parts = key.toString().split(":");
      Integer freqi = wfUtil.getFrequency(parts[0]);
      Integer freqj = wfUtil.getFrequency(parts[1]);
      if (freqi == null || freqj == null) {
        return;
      }

      double normalizedScore =  ((double)(count + 1) * NUM_WORDS)
          / (2 * (double)window * (double)freqi * (double)freqj);

      output.collect(key, new DoubleWritable(normalizedScore));
    }
  }

  /**
   * Creates an instance of this tool.
   */
  public WikiWordCooccurrence() {
  }

  private static int printUsage() {
    System.out
        .println("usage: [input-path] [output-path] [window] [num-reducers] [stopwords] [pos-tagger-model]");
    return -1;
  }

  /**
   * Runs this tool.
   */
  public static void main(String[] args) throws Exception {
    if (args.length != 5) {
      printUsage();
      return;
    }

    String inputPath = args[0];
    String outputPath = args[1];

    int window = Integer.parseInt(args[2]);
    int reduceTasks = Integer.parseInt(args[3]);

    JobConf conf = new JobConf(WikiWordCooccurrence.class);
    System.out.println(" - input path: " + inputPath);
    System.out.println(" - output path: " + outputPath);
    System.out.println(" - window: " + window);
    System.out.println(" - number of reducers: " + reduceTasks);
    System.out.println(" - wordFrequencies: " + args[4]);

    conf.set("topWords", args[4]);
    conf.setJobName("Cooccurence");
    conf.setNumMapTasks(48);
    conf.setNumReduceTasks(reduceTasks);

    conf.set("mapred.task.timeout", "12000000");
    conf.set("mapred.child.java.opts", "-Xmx4000M -Xms2000M");

    conf.setInputFormat(WikipediaPageInputFormat.class);
    conf.setOutputFormat(TextOutputFormat.class);

    conf.setMapperClass(MyMapper.class);
    conf.setReducerClass(MyReducer.class);
    conf.setMapOutputKeyClass(Text.class);
    conf.setMapOutputValueClass(IntWritable.class);
    conf.setOutputKeyClass(Text.class);
    conf.setOutputValueClass(DoubleWritable.class);

    FileInputFormat.setInputPaths(conf, new Path(inputPath));
    FileOutputFormat.setOutputPath(conf, new Path(outputPath));

    long startTime = System.currentTimeMillis();
    JobClient.runJob(conf);
    System.out.println("Job Finished in "
        + (System.currentTimeMillis() - startTime) / 1000.0
        + " seconds");
  }
}
