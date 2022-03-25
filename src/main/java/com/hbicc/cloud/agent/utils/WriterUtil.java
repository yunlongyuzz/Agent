package com.hbicc.cloud.agent.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class WriterUtil {

    //写入操作
    public static void WriterIO(String data){

        try{
            //1.file类的实例化，并指出写出到的文件，输出操作文件不存在会自动创建
            File file =new File("src/main/resources/logger.txt");

            //2.true 代表追加到文件末尾
            FileWriter fileWriter = new FileWriter(file,true);

            //3.先造节点流，再造处理流
            BufferedWriter bufferWriter = new BufferedWriter(fileWriter);

            //4.操作
            bufferWriter.write(data + "\n");

            //5.直接关闭处理流即可，会自动关闭节点流（if ... !=null 再进行关闭）
            if (bufferWriter != null){
                bufferWriter.close();
            }
        }catch(IOException e){
            e.printStackTrace();
        }

    }

    //写入操作
    public static void WriterIO(String data,String data2){

        try{
            //1.file类的实例化，并指出写出到的文件，输出操作文件不存在会自动创建
            File file =new File("logs/text.txt");

            //2.true 代表追加到文件末尾
            FileWriter fileWriter = new FileWriter(file,true);

            //3.先造节点流，再造处理流
            BufferedWriter bufferWriter = new BufferedWriter(fileWriter);

            //4.操作
            bufferWriter.write(data + "\n");
            bufferWriter.write(data2 + "\n");

            //5.直接关闭处理流即可，会自动关闭节点流（if ... !=null 再进行关闭）
            if (bufferWriter != null){
                bufferWriter.close();
            }
        }catch(IOException e){
            e.printStackTrace();
        }

    }

    //写入操作
    public static void WriterIO(String data,boolean data2){

        try{
            //1.file类的实例化，并指出写出到的文件，输出操作文件不存在会自动创建
            File file =new File("src/main/resources/logger.txt");

            //2.true 代表追加到文件末尾
            FileWriter fileWriter = new FileWriter(file,true);

            //3.先造节点流，再造处理流
            BufferedWriter bufferWriter = new BufferedWriter(fileWriter);

            //4.操作
            bufferWriter.write(data + "\n");
            bufferWriter.write(data2 + "\n");

            //5.直接关闭处理流即可，会自动关闭节点流（if ... !=null 再进行关闭）
            if (bufferWriter != null){
                bufferWriter.close();
            }
        }catch(IOException e){
            e.printStackTrace();
        }

    }


    //写入操作
    public static void WriterIO(String data,String data2,String data3){

        try{
            //1.file类的实例化，并指出写出到的文件，输出操作文件不存在会自动创建
            File file =new File("src/main/resources/logger.txt");

            //2.true 代表追加到文件末尾
            FileWriter fileWriter = new FileWriter(file,true);

            //3.先造节点流，再造处理流
            BufferedWriter bufferWriter = new BufferedWriter(fileWriter);

            //4.操作
            bufferWriter.write(data + "\n");
            bufferWriter.write(data2 + "\n");
            bufferWriter.write(data3 + "\n");

            //5.直接关闭处理流即可，会自动关闭节点流（if ... !=null 再进行关闭）
            if (bufferWriter != null){
                bufferWriter.close();
            }
        }catch(IOException e){
            e.printStackTrace();
        }

    }

}
