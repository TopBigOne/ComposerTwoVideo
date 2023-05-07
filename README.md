## ffmpeg合并视频
> 最好的方式
* ffmpeg -f concat -safe 0 -i list.txt -c copy output.mp4
  * -f concat 
    * 表示使用 concat 协议合并视频。
  * -safe 0 
    * 表示不使用安全模式。在默认情况下，FFmpeg会禁止使用通配符和文件名中的相对路径，使用该选项可以取消这种限制。
  * -i list.txt 
    * 表示读取包含视频文件列表的文本文件。
  * -c copy 
    * 表示复制视频流而不重新编码视频，从而快速地合并视频文件。
  * output.mp4 
    * 是合并后的视频文件名。

### [使用 FFmpeg concat 过滤器重新编码（有损）](https://blog.51cto.com/u_15284125/3050611)
```shell
ffmpeg -i input1.mp4 -i input2.webm -i input3.avi -filter_complex '[0:0] [0:1] [1:0] [1:1] [2:0] [2:1] concat=n=3:v=1:a=1 [v] [a]' -map '[v]' -map '[a]'<编码器选项>output.mkv
```
* FFmpeg concat 过滤器会重新编码它们。 
* 注意这是有损压缩。
  [0:0] [0:1] [1:0] [1:1] [2:0] [2:1] 分别表示第一个输入文件的视频、音频、第二个输入文件的视频、音频、第三个输入文件的视频、音频。
* concat=n=3:v=1:a=1 表示有三个输入文件，输出一条视频流和一条音频流。 
* [v] [a] 就是得到的视频流和音频流的名字，
* 注意在 bash 等 shell 中需要用引号，防止通配符扩展。
-----------------------------------

### 参考
* [Android将多个视频文件拼接为一个文件](https://blog.csdn.net/newchenxf/article/details/78524896)
