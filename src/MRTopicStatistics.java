import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
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

@SuppressWarnings("deprecation")
public class MRTopicStatistics {
	private static class MyMapper extends MapReduceBase implements
			Mapper<LongWritable, Text, Text, DoubleWritable> {

		@Override
		public void configure(JobConf job) {
		}

		@Override
		public void map(LongWritable key, Text value,
				OutputCollector<Text, DoubleWritable> output, Reporter reporter)
				throws IOException {
			String[] parts = value.toString().split("\t");
			String[] topicScores = parts[1].split(",");
			for (String topicScore : topicScores) {
				String[] topicScoreParts = topicScore.split(":");
				String topic = StringUtil.clean(topicScoreParts[0]);
				Double score = Double.parseDouble(StringUtil.clean(topicScoreParts[1]));
				output.collect(new Text(topic), new DoubleWritable(score));
			}
		}
	}

	public static class MyReducer extends MapReduceBase implements
			Reducer<Text, DoubleWritable, Text, Text> {

		@Override
		public void configure(JobConf job) {
		}

		@Override
		public void reduce(Text key, Iterator<DoubleWritable> values,
				OutputCollector<Text, Text> output, Reporter reporter)
				throws IOException {
			DescriptiveStatistics stats = new DescriptiveStatistics();
			while (values.hasNext()) {
				stats.addValue(values.next().get());
			}
			Text outputValue = new Text(stats.getMean() + "," + stats.getStandardDeviation());
			output.collect(key, outputValue);
		}
	}

	public static void main(String[] args) throws IOException {

		String inputPath = args[0];
		String outputPath = args[1];

		int reduceTasks = Integer.parseInt(args[2]);

		JobConf conf = new JobConf(MRTopicStatistics.class);

		conf.setJobName("MRTopicStatistics");
		conf.setNumReduceTasks(reduceTasks);

		conf.set("mapred.task.timeout", "12000000");

		conf.setMapperClass(MyMapper.class);
		conf.setReducerClass(MyReducer.class);
		conf.setMapOutputKeyClass(Text.class);
		conf.setMapOutputValueClass(DoubleWritable.class);
		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(Text.class);

		FileInputFormat.setInputPaths(conf, new Path(inputPath));
		FileOutputFormat.setOutputPath(conf, new Path(outputPath));

		long startTime = System.currentTimeMillis();
		JobClient.runJob(conf);
		System.out.println("Job Finished in "
				+ (System.currentTimeMillis() - startTime) / 1000.0
				+ " seconds");
	}
}
