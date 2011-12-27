import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
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
import org.apache.hadoop.mapreduce.Counter;

@SuppressWarnings("deprecation")
public class MRCreateWordGraph {

  public static enum MyCounter {
    NUM_RECORDS
  };

	private static class MyMapper extends MapReduceBase implements
			Mapper<LongWritable, Text, Text, Text> {

		@Override
		public void configure(JobConf job) {
		}

		@Override
		public void map(LongWritable key, Text value,
				OutputCollector<Text, Text> output, Reporter reporter)
				throws IOException {
			String[] parts = value.toString().split("\t");
			String[] words = parts[0].split(" : ");
      Double score = Double.parseDouble(parts[1]);
      if (score >= 0.001) {
        output.collect(new Text(words[0]), new Text(words[1] + ":" + score));
      }
		}
	}

	public static class MyReducer extends MapReduceBase implements
			Reducer<Text, Text, Text, Text> {
    
    Long count = new Long(0);

		@Override
		public void configure(JobConf job) {
		}

		@Override
		public void reduce(Text key, Iterator<Text> values,
				OutputCollector<Text, Text> output, Reporter reporter)
				throws IOException {
      String outputString = "";
      boolean first = true;
			while (values.hasNext()) {
        String word = values.next().toString();
        if (first) {
          outputString = word;
          first = false;
        } else {
          outputString += "," + word;
        }
			}
      String outputKey = count + "" + key.toString();
			output.collect(new Text(outputKey), new Text(outputString));
      count++;
      reporter.incrCounter(MyCounter.NUM_RECORDS, 1);
		}
	}

	public static void main(String[] args) throws IOException {

		String inputPath = args[0];
		String outputPath = args[1];
		Integer numReducers = Integer.parseInt(args[2]);
    String counterFile = args[3];

		JobConf conf = new JobConf(MRCreateWordGraph.class);

		conf.setJobName("MRCreateWordGraph");
		conf.setNumReduceTasks(1);

		conf.setMapperClass(MyMapper.class);
		conf.setReducerClass(MyReducer.class);
		conf.setMapOutputKeyClass(Text.class);
		conf.setMapOutputValueClass(Text.class);
		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(Text.class);

		FileInputFormat.setInputPaths(conf, new Path(inputPath));
		FileOutputFormat.setOutputPath(conf, new Path(outputPath));

		long startTime = System.currentTimeMillis();
		RunningJob job = JobClient.runJob(conf);
		System.out.println("Job Finished in "
				+ (System.currentTimeMillis() - startTime) / 1000.0
				+ " seconds");
    Counters counters = job.getCounters();
    Counter totalSentences = counters.findCounter(MyCounter.NUM_RECORDS);
    FileUtils.writeStringToFile(new File(counterFile),
        Long.toString(totalSentences.getValue()));
	}
}
