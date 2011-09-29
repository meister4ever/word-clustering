import java.lang.Comparable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.RawComparator;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.JobConfigurable;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Partitioner;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;

//import org.apache.hadoop.mapred.TextInputFormat;
//import org.apache.hadoop.mapred.TextOutputFormat;

@SuppressWarnings("deprecation")
public class MREntityCrossSimilarity {

	private static class MyText implements Comparable<MyText> {
		private String key1;
		private String key2;

		MyText() {
			this.key1 = null;
			this.key2 = null;
		}

		MyText(String key1, String key2) {
			this.key1 = key1;
			this.key2 = key2;
		}

		/*
		 * @Override public void readFields(DataInput in) throws IOException {
		 * // TODO Auto-generated method stub int strLen = in.readInt(); String
		 * inStr = ""; for (int i = 0; i < strLen; ++i) { inStr +=
		 * in.readChar(); } String[] parts = inStr.split("\t"); key = parts[0];
		 * value = parts[1];
		 * 
		 * int keyLen = in.readInt(); byte[] keyBytes = new byte[keyLen];
		 * in.readFully(keyBytes); key = new String(keyBytes);
		 * 
		 * int valueLen = in.readInt(); byte[] valueBytes = new byte[valueLen];
		 * in.readFully(valueBytes); value = new String(valueBytes);
		 * 
		 * }
		 * 
		 * @Override public void write(DataOutput out) throws IOException { //
		 * TODO Auto-generated method stub String outStr = key + "\t" + value;
		 * out.writeInt(outStr.length()); out.writeChars(key + "\t" + value);
		 * 
		 * byte[] keyBytes = key.getBytes(); out.writeInt(keyBytes.length);
		 * out.write(keyBytes);
		 * 
		 * byte[] valueBytes = value.getBytes();
		 * out.writeInt(valueBytes.length); out.write(valueBytes);
		 * 
		 * }
		 */

		@Override
		public int compareTo(MyText rhs) {
			// TODO Auto-generated method stub
			int compareRes = this.key1.compareTo(rhs.key1);
			if (compareRes == 0) {
				return this.key2.compareTo(rhs.key2);
			}
			return compareRes;
		}

		public String getKey1() {
			return key1;
		}
	}

	private static class MyMapper extends MapReduceBase implements
			Mapper<LongWritable, Text, MyText, Text> {
		// We should know total records to do the cross similarity.
		int totalRecords = 0;

		@Override
		public void configure(JobConf job) {
			totalRecords = job.getInt("totalRecords", 0);
		}

		@Override
		public void map(LongWritable key, Text value,
				OutputCollector<MyText, Text> output, Reporter reporter)
				throws IOException {
			String[] parts = value.toString().split("\t");
			Integer recordNumber = Integer.parseInt(parts[0]);
			String newKey = String.format("%010d", recordNumber);
			output.collect(new MyText(newKey, "0"), new Text(parts[1]));
			System.out.println(newKey + " => " + "0\t" + parts[1]);
			for (int i = 1; i <= totalRecords; ++i) {
				newKey = String.format("%010d", i);
				output.collect(new MyText(newKey, "1"), new Text(parts[1]));
				System.out.println(newKey + " => " + "1\t" + parts[1]);
			}
		}
	}

	private static class MyPartitioner implements Partitioner<MyText, Text>,
			JobConfigurable {

		@Override
		public int getPartition(MyText key, Text value, int numReduceTasks) {
			Integer partition = Integer.parseInt(key.getKey1().toString())
					% numReduceTasks;
			return partition;
		}

		@Override
		public void configure(JobConf conf) {
		}
	}

	private static class MyReducer extends MapReduceBase implements
			Reducer<Text, MyText, Text, Text> {

		@Override
		public void reduce(Text key, Iterator<MyText> values,
				OutputCollector<Text, Text> output, Reporter reporter)
				throws IOException {
			// TODO Auto-generated method stub
			String firstValue = "";
			while (values.hasNext()) {
				String currentValue = values.next().toString();
				System.out.println(key + " == " + currentValue);
				if (firstValue == "") {
					firstValue = currentValue;
					System.out.println("First value: " + firstValue);
				}
				if (currentValue != firstValue) {
					System.out.println(firstValue + " X " + currentValue);
					output.collect(new Text(firstValue), new Text(firstValue
							+ " X " + currentValue));
				}
			}
		}
	}

	private static final class SortReducerByValuesValueGroupingComparator
			implements RawComparator<MyText> {
		public int compare(byte[] text1, int start1, int length1, byte[] text2,
				int start2, int length2) {
			// look at first character of each text byte array
			String text1Str = new String(text1, start1, length1);
			String text2Str = new String(text2, start2, length2);

			String key1 = text1Str.split("\t")[0];
			String key2 = text2Str.split("\t")[0];

			return key1.compareTo(key2);
		}

		@Override
		public int compare(MyText o1, MyText o2) {
			// TODO Auto-generated method stub
			return o2.key1.compareTo(o2.key2);
		}
	}

	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws IOException {

		String inputPath = args[0];
		String outputPath = args[1];
		Integer totalRecords = Integer.parseInt(args[2]);

		// int reduceTasks = Integer.parseInt(args[2]);

		JobConf conf = new JobConf(MRWordTopicConverter.class);

		conf.setJobName("MRWordTopicConverter");
		conf.setNumReduceTasks(2);
		conf.setInt("totalRecords", totalRecords);

		conf.set("mapred.task.timeout", "12000000");
		// conf.set("mapred.child.java.opts", "-Xmx4000M -Xms2000M");

		// conf.setInputFormat(TextInputFormat.class);
		// conf.setOutputFormat(TextOutputFormat.class);

		conf.setMapperClass(MyMapper.class);
		conf.setPartitionerClass(MyPartitioner.class);
		conf.setReducerClass(MyReducer.class);
		conf
				.setOutputValueGroupingComparator(SortReducerByValuesValueGroupingComparator.class);

		conf.setMapOutputKeyClass(MyText.class);
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
