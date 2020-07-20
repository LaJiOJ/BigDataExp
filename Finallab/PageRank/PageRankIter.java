import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.KeyValueTextInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import java.io.*;

public class PageRankIter {
//    private static int row_cnt = 0;
    private enum LineCounter{
        LINE_COUNTER
    }
    public static class PageRankIterMapper extends Mapper<Text, Text, Text, Text> {
        @Override
        protected void map(Text key, Text value, Context context) throws IOException, InterruptedException {
            //Input: key: name, value: oldRank + linklist
            //Output: key: name, value: newRank(doesn't have '[') or linklist(has '[')
            String[] rank_and_list = value.toString().split("\\s+");
            double cur_rank = Double.valueOf(rank_and_list[0]);
            String link_list = rank_and_list[1];
            String[] arr = link_list.substring(1, link_list.length() - 1).split("\\|");
            int list_len = arr.length;
            for (String ss : arr) {
                try {
                    context.write(new Text(ss.split(",")[0]), new Text(String.valueOf(cur_rank / list_len)));
                }
                catch (Exception e) {
                    System.exit(1);
                }
            }
            context.write(key, new Text(link_list));
//            Counter counter = context.getCounter(LineCounter.LINE_COUNTER);
//            counter.increment(1L);
//            context.getConfiguration().setLong("line_num",counter.getValue());
//            System.out.println(context.getConfiguration().getLong("line_num",0));
//            row_cnt += 1;
        }
    }
    public static class PageRankIterReducer extends Reducer<Text, Text, Text, Text> {
        private static final double d = 0.85;// ref: PPT Ch8 Page15
        private static long row_cnt;
        @Override
        protected void setup(Context context) throws IOException, InterruptedException {
            row_cnt = context.getConfiguration().getLong("line_num",0L);
//            System.out.println(row_cnt);
        }
        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            //Input: key: name, value: newRank(doesn't have '[') or linklist(has '[')
            //Output: key: name, value: newRank + linklist
            String link_list = "";
            double val = 0;
            for(Text t : values){
                String cur_text = t.toString();
                if(cur_text.substring(0,1).equals("[")){// means link_list
                    link_list = cur_text;
                }
                else{
                    val += Double.valueOf(cur_text);
                }
            }
//            System.out.println(row_cnt);
            double new_rank = 1.0;
            try {
                new_rank = (1 - d) / row_cnt + val * d;
            }
            catch (Exception e){
                System.exit(1);
            }
            System.out.println(new_rank);
            context.write(key, new Text(new_rank + " " + link_list));
        }
    }
    public static void iter(String args[], int row_cnt) throws IOException, ClassNotFoundException, InterruptedException{
        Configuration conf = new Configuration();
        conf.setLong("line_num",row_cnt);
        Job job = new Job(conf, "PageRank Iter");
        job.setJarByClass(PageRankIter.class);

        job.setMapperClass(PageRankIter.PageRankIterMapper.class);
        job.setReducerClass(PageRankIter.PageRankIterReducer.class);

        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        job.setInputFormatClass(KeyValueTextInputFormat.class);// read by row so that output by row
        job.setOutputFormatClass(TextOutputFormat.class);// output by row to each file

        job.setNumReduceTasks(5);// because there are 5 novels for JinYong

        FileInputFormat.setInputPaths(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        job.waitForCompletion(true);
//        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
