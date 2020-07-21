import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.KeyValueTextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import java.io.*;
import java.util.Scanner;

public class GraphBuilder {
//    private static int row_cnt = 0;
//    private static double init_pr_value = 0;
    enum LineCounter{
        LINE_COUNTER
    }

    public static class GraphBuilderMapper extends Mapper<Text, Text, Text, Text> {
        @Override
        protected void map(Text key, Text value, Context context) throws IOException, InterruptedException {
            context.write(key, new Text("1" + value));
//            context.getCounter(LineCounter.LINE_COUNTER).increment(1L);
//            counter.increment(1L);
//            System.out.println(context.getCounter(TaskCounter.MAP_INPUT_RECORDS).getValue());
//            context.getCounter(LineCounter.LINE_COUNTER).;
//            counter.increment(1L);
        }
    }

//    public static class GraphBuilderReducer extends Reducer<Text, Text, Text, Text> {
////        @Override
////        protected void setup(Reducer<Text, Text, Text, Text>.Context context) {
////            init_pr_value = 1.0 / row_cnt;
//////            System.out.println(init_pr_value);
////        }
//        @Override
//        protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
//            for(Text t : values){
////                context.write(key, new Text("" + init_pr_value + t));
//                context.write(key, new Text("1" + t));
//            }
//        }
//    }

    public static long main(String args[]) throws IOException, ClassNotFoundException, InterruptedException{
        Configuration conf = new Configuration();
        //ref Book P88-89
        FileSystem hdfs = FileSystem.get(conf);
        long row_cnt = 0;
        String str;
        FSDataInputStream in = null;
        Scanner scan;
        try {
            FileStatus[] stats = hdfs.listStatus(new Path(args[0]));
            row_cnt = 0;
            for(int i=0;i<stats.length;i++){
                in = hdfs.open(stats[i].getPath());
                scan = new Scanner(in);
                while(scan.hasNext()){
                    str = scan.nextLine();
                    row_cnt++;
                }
                scan.close();
                in.close();
            }
        }
        catch(IOException e){
            e.printStackTrace();
        }
        System.out.println(row_cnt);

        Job job = new Job(conf, "Graph Builder");
        job.setJarByClass(GraphBuilder.class);

        job.setMapperClass(GraphBuilder.GraphBuilderMapper.class);
//        job.setReducerClass(GraphBuilder.GraphBuilderReducer.class);

        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        job.setInputFormatClass(KeyValueTextInputFormat.class);// read by row so that output by row
        job.setOutputFormatClass(TextOutputFormat.class);// output by row to each file

        FileInputFormat.setInputPaths(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        job.waitForCompletion(true);
//        System.exit(job.waitForCompletion(true) ? 0 : 1);
        return row_cnt;
    }
}
