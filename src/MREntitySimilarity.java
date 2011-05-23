import java.io.IOException;
import java.util.TreeSet;

import org.apache.hadoop.fs.Path;
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
public class MREntitySimilarity {
	private static class MyMapper extends MapReduceBase implements
			Mapper<LongWritable, Text, Text, Text> {
		private EntityTopicMap entityTopicMap;

		@Override
		public void configure(JobConf job) {
			entityTopicMap = new EntityTopicMap(job.get("entityTopicMap"), job);
		}

		@Override
		public void map(LongWritable key, Text value,
				OutputCollector<Text, Text> output, Reporter reporter)
				throws IOException {
			String entityName = StringUtil.clean(value.toString().split("\t")[0]);
			TreeSet<EntityScore> simEntities = entityTopicMap.getKSimEntities(entityName);
			for (EntityScore simEntity : simEntities) {
				output.collect(new Text(entityName), new Text(simEntity.toString()));
			}
		}
	}

	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws IOException {

		String inputPath = args[0];
		String outputPath = args[1];

		JobConf conf = new JobConf(MREntitySimilarity.class);

		conf.setJobName("MREntitySim");
		conf.setNumReduceTasks(0);

		conf.set("mapred.task.timeout", "12000000");
		conf.set("mapred.child.java.opts", "-Xmx4000M -Xms2000M");
		conf.set("entityTopicMap", args[2]);

		conf.setMapperClass(MyMapper.class);
		//conf.setReducerClass(MyReducer.class);
		conf.setMapOutputKeyClass(Text.class);
		conf.setMapOutputValueClass(Text.class);
		//conf.setOutputKeyClass(NullWritable.class);
		//conf.setOutputValueClass(Text.class);

		FileInputFormat.setInputPaths(conf, new Path(inputPath));
		FileOutputFormat.setOutputPath(conf, new Path(outputPath));

		long startTime = System.currentTimeMillis();
		JobClient.runJob(conf);
		System.out.println("Job Finished in "
				+ (System.currentTimeMillis() - startTime) / 1000.0
				+ " seconds");
	}
}
