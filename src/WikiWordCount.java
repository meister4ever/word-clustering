import java.io.IOException;
import java.net.URI;
import java.util.Iterator;

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

import edu.umd.cloud9.collection.wikipedia.WikipediaPage;
import edu.umd.cloud9.collection.wikipedia.WikipediaPageInputFormat;

@SuppressWarnings("deprecation")
public class WikiWordCount {

	private static class MyMapper extends MapReduceBase implements
			Mapper<LongWritable, WikipediaPage, Text, Text> {

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
				OutputCollector<Text, Text> output, Reporter reporter)
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
				output.collect(new Text(StringUtil.stem(term)), new Text(term));
			}
		}
	}

	public static class MyReducer extends MapReduceBase implements
			Reducer<Text, Text, Text, IntWritable> {

		@Override
		public void reduce(Text key, Iterator<Text> values,
				OutputCollector<Text, IntWritable> output, Reporter reporter)
				throws IOException {
			int count = 0;
      String outputKey = null;
			while (values.hasNext()) {
        String valueStr = values.next().toString();
        if (outputKey == null) {
          outputKey = valueStr;
        }
				count++;
			}
      if (count >= 10) {
        output.collect(new Text(outputKey), new IntWritable(count));
      }
		}
	}

	/**
	 * Creates an instance of this tool.
	 */
	public WikiWordCount() {
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
		if (args.length != 6) {
			printUsage();
			return;
		}

		String inputPath = args[0];
		String outputPath = args[1];

		int window = Integer.parseInt(args[2]);
		int reduceTasks = Integer.parseInt(args[3]);

    JobConf conf = new JobConf(WikiWordCount.class);    
		System.out.println(" - input path: " + inputPath);
		System.out.println(" - output path: " + outputPath);
		System.out.println(" - window: " + window);
		System.out.println(" - number of reducers: " + reduceTasks);
		System.out.println(" - stopwords: " + args[4]);
		System.out.println(" - pos-tagger-model: " + args[5]);

    DistributedCache.createSymlink(conf);
    DistributedCache.addCacheFile(new URI(args[5]), conf);

    conf.set("stopWordsFile", args[4]);
    conf.set("taggerModelFile", "postag_model");
		conf.setJobName("WikiWordCount");
		conf.setNumMapTasks(48);
		conf.setNumReduceTasks(reduceTasks);

    conf.set("mapred.task.timeout", "12000000");
    conf.set("mapred.child.java.opts", "-Xmx4000M -Xms2000M");

		conf.setInputFormat(WikipediaPageInputFormat.class);
		conf.setOutputFormat(TextOutputFormat.class);

		conf.setMapperClass(MyMapper.class);
		conf.setReducerClass(MyReducer.class);
		conf.setMapOutputKeyClass(Text.class);
		conf.setMapOutputValueClass(Text.class);
		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(IntWritable.class);

		FileInputFormat.setInputPaths(conf, new Path(inputPath));
		FileOutputFormat.setOutputPath(conf, new Path(outputPath));

		long startTime = System.currentTimeMillis();
		JobClient.runJob(conf);
		System.out.println("Job Finished in "
				+ (System.currentTimeMillis() - startTime) / 1000.0
				+ " seconds");
	}
}
