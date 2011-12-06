import java.lang.Math;
import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Iterator;
import java.io.ObjectInputStream;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
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

@SuppressWarnings("deprecation")
public class MutualInformation {
  private static class MutualInformationMapper extends MapReduceBase implements
    Mapper<LongWritable, Text, Text, Text> {

      @Override
        public void configure(JobConf job) {
        }

      @Override
        public void map(LongWritable key, Text value,
            OutputCollector<Text, Text> output, Reporter reporter)
        throws IOException {
          String[] parts = value.toString().split("\t");
          output.collect(new Text(parts[0]), new Text(parts[1]));
        }
    }

  public static class MutualInformationReducer extends MapReduceBase implements
    Reducer<Text, Text, Text, DoubleWritable> {

      @Override
        public void configure(JobConf job) {
        }

      @Override
        public void reduce(Text key, Iterator<Text> values,
            OutputCollector<Text, DoubleWritable> output, Reporter reporter)
        throws IOException {
          boolean firstKey = true;
          Integer cooccurence = 0;
          Double prob = 1.0;
          while (values.hasNext()) {
            String[] parts = values.next().toString().split("");
            prob *= Double.parseDouble(parts[0]);
            if (firstKey) {
              firstKey = false;
              cooccurence = Integer.parseInt(parts[1]);
              continue;
            }
          }
          Double outVal = prob / cooccurence.doubleValue();
          String outKey = key.toString().replaceAll("[0-9]", "");
          outKey = outKey.replaceAll("", " : ");
          output.collect(new Text(outKey), new DoubleWritable(outVal));
        }
    }

  public static void main(String[] args) throws IOException {
    if (args.length < 3) {
      System.err.println("Usage: hadoop jar Cooccur.jar MutualInformation"
          + " <in-files> <out-dir> <num-reducers>"); 
      return;
    }
    String inputPath = args[0];
    String outputPath = args[1];

    JobConf conf = new JobConf(MutualInformation.class);

    conf.setJobName("MutualInformation");
    conf.setNumReduceTasks(Integer.parseInt(args[2]));

    conf.setMapperClass(MutualInformationMapper.class);
    conf.setReducerClass(MutualInformationReducer.class);
    conf.setMapOutputKeyClass(Text.class);
    conf.setMapOutputValueClass(Text.class);
    conf.setOutputKeyClass(Text.class);
    conf.setOutputValueClass(DoubleWritable.class);

    FileInputFormat.setInputPaths(conf, new Path(inputPath));
    FileOutputFormat.setOutputPath(conf, new Path(outputPath));

    long startTime = System.currentTimeMillis();
    JobClient.runJob(conf);
    System.out.println("Job Finished in "
        + (System.currentTimeMillis() - startTime) / 1000.0
        + " seconds");
  }
}
