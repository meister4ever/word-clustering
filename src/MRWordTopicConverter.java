import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
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
//import org.apache.hadoop.mapred.TextInputFormat;
//import org.apache.hadoop.mapred.TextOutputFormat;

@SuppressWarnings("deprecation")
public class MRWordTopicConverter {
	private static class MyMapper extends MapReduceBase implements
			Mapper<LongWritable, Text, Text, Text> {
		private WordTopicConverter wordTopicConverter;

		@Override
		public void configure(JobConf job) {
			StopwordUtil stopwordUtil = new StopwordUtil("/home/rohith/word-clustering/data/stopWords.txt");
			WordIdMap wordIdMap = new WordIdMap("/home/rohith/word-clustering/data/entity_dict.txt", "/home/rohith/word-clustering/data/word_prob.txt", stopwordUtil);
			TopicWordMap topicWordMap = new TopicWordMap("/home/rohith/word-clustering/data/thesaurus.crawl.txt", wordIdMap);
			wordTopicConverter = new WordTopicConverter(wordIdMap, topicWordMap);
		}

		@Override
		public void map(LongWritable key, Text value,
				OutputCollector<Text, Text> output, Reporter reporter)
				throws IOException {
			
			String discountedProbStr = wordTopicConverter.convertWords2Topics(value.toString());
			int pos = value.toString().indexOf(':');
			String entity = value.toString().substring(0, pos);
			output.collect(new Text(entity), new Text(discountedProbStr));
		}
	}

	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws IOException {

		String inputPath = args[0];
		String outputPath = args[1];

		//int reduceTasks = Integer.parseInt(args[2]);

		JobConf conf = new JobConf(MRWordTopicConverter.class);

		conf.setJobName("MRWordTopicConverter");
		conf.setNumReduceTasks(0);

		conf.set("mapred.task.timeout", "12000000");
		//conf.set("mapred.child.java.opts", "-Xmx4000M -Xms2000M");

		//conf.setInputFormat(TextInputFormat.class);
		//conf.setOutputFormat(TextOutputFormat.class);

		conf.setMapperClass(MyMapper.class);
		conf.setMapOutputKeyClass(Text.class);
		conf.setMapOutputValueClass(Text.class);
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
