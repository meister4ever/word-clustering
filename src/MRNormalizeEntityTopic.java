import java.io.IOException;
import java.util.HashMap;

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
import org.apache.hadoop.mapred.Reporter;

@SuppressWarnings("deprecation")
public class MRNormalizeEntityTopic {
	private static class MyMapper extends MapReduceBase implements
			Mapper<LongWritable, Text, Text, Text> {
		private TopicStatUtil topicStatUtil;

		@Override
		public void configure(JobConf job) {
			String topicStatFile = job.get("topicStatFile");
			topicStatUtil = new TopicStatUtil(topicStatFile, job);
		}

		@Override
		public void map(LongWritable key, Text value,
				OutputCollector<Text, Text> output, Reporter reporter)
				throws IOException {
			String[] parts = value.toString().split("\t");
			String[] topicScores = parts[1].split(",");
			HashMap<String, Double> topicScoreMap = new HashMap<String, Double>();
			for (String topicScore : topicScores) {
				String[] topicScoreParts = topicScore.split(":");
				String topic = StringUtil.clean(topicScoreParts[0]);
				Double score = Double.parseDouble(StringUtil
						.clean(topicScoreParts[1]));
				Double topicMean = topicStatUtil.getMean(topic);
				Double topicSdev = topicStatUtil.getSdev(topic);
				topicScoreMap.put(topic, (score - topicMean)/topicSdev);
			}
			StringBuilder sb = new StringBuilder();
			boolean first = true;
			for (String topic : topicScoreMap.keySet()) {
				if (!first) {
					sb.append(",");
				}
				first = false;
				sb.append(topic + ":" + topicScoreMap.get(topic));
			}
			output.collect(new Text(parts[0]), new Text(sb.toString()));
		}
	}

	public static void main(String[] args) throws IOException {

		String inputPath = args[0];
		String outputPath = args[1];

		JobConf conf = new JobConf(MRNormalizeEntityTopic.class);

		conf.setJobName("MRNormalizeEntityTopic");
		conf.setNumReduceTasks(0);

		conf.set("mapred.task.timeout", "12000000");

		conf.setMapperClass(MyMapper.class);
		conf.setMapOutputKeyClass(Text.class);
		conf.setMapOutputValueClass(DoubleWritable.class);

		FileInputFormat.setInputPaths(conf, new Path(inputPath));
		FileOutputFormat.setOutputPath(conf, new Path(outputPath));

		long startTime = System.currentTimeMillis();
		JobClient.runJob(conf);
		System.out.println("Job Finished in "
				+ (System.currentTimeMillis() - startTime) / 1000.0
				+ " seconds");
	}
}
