import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;

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
public class LydiaWordCooccurrence {

  private static final Long NUM_WORDS = (long) 1594793586;

  public static enum MyCounter {
    INPUT_WORDS, INPUT_ARTICLES, SAMPLED_ARTICLES, NUM_SENTENCES
  };

  private static class MyMapper extends MapReduceBase implements
      Mapper<LongWritable, Text, Text, LongWritable> {
    private int window = 3;
    private int samplePercentage = 10;
    private WordFrequencyUtil wfUtil;
    private LongWritable one = new LongWritable(1);

    @Override
    public void configure(JobConf job) {
      window = job.getInt("window", 3);
      samplePercentage = job.getInt("samplePercentage", 25);
      String topWordsFile = job.get("topWords");
      wfUtil = new WordFrequencyUtil(topWordsFile, job);
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
        String[] terms = sent.split("\\s+");
        for (int i = 0; i < terms.length; i++) {
          String term = terms[i];
          String storedWordi = StringUtil.clean(term);
          //if (storedWordi.length() == 0 || storedWordi.matches(".*[^a-zA-Z0-9 '].*")) {
          //  continue;
          //}
          //String storedWordi = wfUtil.getStoredString(terms[i]);
          if (!wfUtil.isPresent(storedWordi)) {
            continue;
          }
          reporter.incrCounter(MyCounter.INPUT_WORDS, 1);

          //for (int j = (i - window); j < (i + window) && j < terms.length; j++) {
          for (int j = 0; j < terms.length; j++) {
            if (j == i || j < 0)
              continue;

            if (j >= terms.length)
              break;

            //String storedWordj = wfUtil.getStoredString(terms[j]);
            String storedWordj = StringUtil.clean(terms[j]);
            if (!wfUtil.isPresent(storedWordj)) {
              continue;
            }

            //if (storedWordj.length() == 0 || storedWordj.matches(".*[^a-zA-Z0-9 '].*")) {
            //  continue;
            //}
            if (storedWordi.compareTo(storedWordj) == 0) {
              continue;
            }

            String outputKey = storedWordi + "" + storedWordj;
            if (storedWordi.compareTo(storedWordj) > 0) {
              outputKey = storedWordj + "" + storedWordi;
            }

            try {
              output.collect(new Text(outputKey), one);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }

          output.collect(new Text(storedWordi), one);
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

      output.collect(key, new LongWritable(count));

      /*
      String[] parts = key.toString().split(":");
      Integer freqi = wfUtil.getFrequency(parts[0]);
      Integer freqj = wfUtil.getFrequency(parts[1]);
      if (freqi == null || freqj == null) {
        return;
      }

      double normalizedScore =  ((double)(count + 1) * NUM_WORDS)
          / (2 * (double)window * (double)freqi * (double)freqj);

      output.collect(key, new DoubleWritable(normalizedScore));
      */
    }
  }

  /**
   * Creates an instance of this tool.
   */
  public LydiaWordCooccurrence() {
  }

  private static int printUsage() {
    System.out
        .println("usage: [input-path] [output-path] [window] [num-reducers] [stopwords] [sample-percentage] [counter-output-file]");
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
    String counterFile = args[6];

    JobConf conf = new JobConf(LydiaWordCooccurrence.class);
    System.out.println(" - input path: " + inputPath);
    System.out.println(" - output path: " + outputPath);
    System.out.println(" - window: " + window);
    System.out.println(" - number of reducers: " + reduceTasks);
    System.out.println(" - wordFrequencies: " + args[4]);
    System.out.println(" - samplePercentage: " + args[5]);

    conf.set("topWords", args[4]);
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
    FileUtils.writeStringToFile(new File(counterFile),
        Long.toString(totalSentences.getValue()));
  }
}
