import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import net.didion.jwnl.JWNLException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
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
import org.apache.hadoop.mapred.TextInputFormat;
import org.apache.hadoop.mapred.TextOutputFormat;

public class WordnetCooccurence {
	private static class MyMapper extends MapReduceBase implements
			Mapper<LongWritable, Text, Text, IntWritable> {
		private int pathLen = 3;
		private IntWritable one = new IntWritable(1);
		private ArrayList<String> words = new ArrayList<String>();
		private WordnetUtil wnUtil;

		@Override
		public void configure(JobConf job) {
			pathLen = job.getInt("pathLen", 3);
			String topWordsFile = job.get("topWords");
			String textStr = HdfsFileUtil.ReadFileContent(topWordsFile, job);
			String[] lines = textStr.split("\n");
			for (String line : lines) {
				String[] parts = line.split("\t");
				words.add(StringUtil.clean(parts[0]));
			}
			try {
				wnUtil = WordnetUtil.getInstance();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JWNLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void map(LongWritable key, Text value,
				OutputCollector<Text, IntWritable> output, Reporter reporter)
				throws IOException {

			String[] terms = value.toString().split("\t");
			String term = StringUtil.clean(terms[0]);
			for (String word : words) {
				try {
					if (wnUtil.isRelated(term, word)) {
						String outputKey = term + ":" + word;
						try {
							output.collect(new Text(outputKey), one);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				} catch (JWNLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public static class MyReducer extends MapReduceBase implements
			Reducer<Text, IntWritable, Text, IntWritable> {

		@Override
		public void configure(JobConf job) {
		}

		@Override
		public void reduce(Text key, Iterator<IntWritable> values,
				OutputCollector<Text, IntWritable> output, Reporter reporter)
				throws IOException {
			while (values.hasNext()) {
				output.collect(key, values.next());
			}
		}
	}

	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws JWNLException, IOException {

		String inputPath = args[0];
		String outputPath = args[1];

		int reduceTasks = Integer.parseInt(args[2]);

		JobConf conf = new JobConf(WordnetCooccurence.class);
		System.out.println(" - input path: " + inputPath);
		System.out.println(" - output path: " + outputPath);
		System.out.println(" - number of reducers: " + reduceTasks);
		System.out.println(" - wordFrequencies: " + args[3]);

		conf.set("topWords", args[3]);
		conf.setInt("pathLen", Integer.parseInt(args[4]));
		conf.setJobName("WordnetCooccurence");
		conf.setNumMapTasks(48);
		conf.setNumReduceTasks(reduceTasks);

		conf.set("mapred.task.timeout", "12000000");
		conf.set("mapred.child.java.opts", "-Xmx4000M -Xms2000M");

		conf.setInputFormat(TextInputFormat.class);
		conf.setOutputFormat(TextOutputFormat.class);

		conf.setMapperClass(MyMapper.class);
		conf.setReducerClass(MyReducer.class);
		conf.setMapOutputKeyClass(Text.class);
		conf.setMapOutputValueClass(IntWritable.class);
		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(IntWritable.class);

		FileInputFormat.setInputPaths(conf, new Path(inputPath));
		FileOutputFormat.setOutputPath(conf, new Path(outputPath));

		long startTime = System.currentTimeMillis();
		JobClient.runJob(conf);
		System.out.println("Job Finished in "
				+ (System.currentTimeMillis() - startTime) / 1000.0
				+ " seconds");
		// WordnetCooccurence wnCooccur = new WordnetCooccurence(args[0], 3);
		// wnCooccur.dumpGraph();
	}
}
