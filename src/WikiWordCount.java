import java.io.IOException;
import java.net.URI;
import java.util.Iterator;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.Path;
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
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;

import edu.umd.cloud9.collection.wikipedia.WikipediaPage;
import edu.umd.cloud9.collection.wikipedia.WikipediaPageInputFormat;

@SuppressWarnings("deprecation")
public class WikiWordCount extends Configured implements Tool {
	private static final Logger sLogger = Logger.getLogger(WikiWordCount.class);

	private static class MyMapper extends MapReduceBase implements
			Mapper<LongWritable, WikipediaPage, Text, IntWritable> {

		private IntWritable one = new IntWritable(1);
		private WordFilter wordFilter;

		protected static enum MyCounter {
			INPUT_WORDS, INPUT_ARTICLES
		};

		@Override
		public void configure(JobConf job) {
			String stopWordsFile = job.get("stopWordsFile");
			String taggerModel = job.get("taggerModelFile");
			try {
				wordFilter = new WordFilter(stopWordsFile, taggerModel, job);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
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

			String[] terms = text.split("\\s+");
			for (String term : terms) {
				reporter.incrCounter(MyCounter.INPUT_WORDS, 1);
				if (wordFilter.dropWord(term)) {
					continue;
				}
				output.collect(new Text(StringUtil.Stem(term)), one);
			}
		}
	}

	public static class MyReducer extends MapReduceBase implements
			Reducer<Text, IntWritable, Text, IntWritable> {

		@Override
		public void reduce(Text key, Iterator<IntWritable> values,
				OutputCollector<Text, IntWritable> output, Reporter reporter)
				throws IOException {
			int count = 0;
			while (values.hasNext()) {
				count += values.next().get();
			}
			output.collect(key, new IntWritable(count));
		}
	}

	/**
	 * Creates an instance of this tool.
	 */
	public WikiWordCount() {
	}

	private static int printUsage() {
		System.out
				.println("usage: [input-path] [output-path] [window] [num-reducers]");
		ToolRunner.printGenericCommandUsage(System.out);
		return -1;
	}

	/**
	 * Runs this tool.
	 */
	public int run(String[] args) throws Exception {
		if (args.length != 5) {
			printUsage();
			return -1;
		}

		String inputPath = args[0];
		String outputPath = args[1];

		int window = Integer.parseInt(args[2]);
		int reduceTasks = Integer.parseInt(args[3]);
		
		FileSystem.get(getConf()).delete(new Path(outputPath), true);
		sLogger.info("Tool: ComputeCooccurrenceMatrixStripes");
		sLogger.info(" - input path: " + inputPath);
		sLogger.info(" - output path: " + outputPath);
		sLogger.info(" - window: " + window);
		sLogger.info(" - number of reducers: " + reduceTasks);

		JobConf conf = new JobConf(getConf(), WikiWordCount.class);
		DistributedCache.createSymlink(conf);
        DistributedCache.addCacheFile(new URI(args[5]), conf);
        
        conf.set("stopWordFile", args[4]);
        conf.set("taggerModelFile", "postag.model");
		conf.setJobName("WikiWordCount");
		conf.setNumMapTasks(48);
		conf.setNumReduceTasks(reduceTasks);

		FileInputFormat.setInputPaths(conf, new Path(inputPath));
		FileOutputFormat.setOutputPath(conf, new Path(outputPath));

		conf.setInputFormat(WikipediaPageInputFormat.class);
		conf.setOutputFormat(TextOutputFormat.class);

		conf.setMapperClass(MyMapper.class);
		conf.setCombinerClass(MyReducer.class);
		conf.setReducerClass(MyReducer.class);
		conf.setMapOutputKeyClass(Text.class);
		conf.setMapOutputValueClass(IntWritable.class);
		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(IntWritable.class);

		long startTime = System.currentTimeMillis();
		JobClient.runJob(conf);
		System.out.println("Job Finished in "
				+ (System.currentTimeMillis() - startTime) / 1000.0
				+ " seconds");

		return 0;
	}

	/**
	 * Dispatches command-line arguments to the tool via the
	 * <code>ToolRunner</code>.
	 */
	public static void main(String[] args) throws Exception {
		int res = ToolRunner.run(new WikiWordCount(), args);
		System.exit(res);
	}
}
