package com.ya.composertwovideo;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;

/**
 * @author : 周广亚
 * @version :
 * @Date :  2023/5/7 12:02
 * @Desc :
 */
public class VideoComposer {
    private static final String       TAG = "VideoComposer : ";
    private final        List<String> mVideoPathList;
    private final        String       outputVideoPath;
    private final        ByteBuffer   mReadBuffer;

    MediaFormat mVideoFormat;
    MediaFormat mAudioFormat;
    MediaMuxer  mediaMuxer;
    int         outputVideoTrackIndex;
    int         outputAudioTrackIndex;


    public VideoComposer(List<String> mVideoPathList, String outputVideoPath) {
        this.mVideoPathList = mVideoPathList;
        this.outputVideoPath = outputVideoPath;
        mReadBuffer = ByteBuffer.allocate(1048576);
    }


    public boolean joinVideo() {
        Log.d(TAG, "joinVideo: ");

        if (!setOutputMp4MediaFormatAndStartMuxer()) {
            return false;
        }

        long             ptsOffset = 0L;
        Iterator<String> tracks    = mVideoPathList.iterator();
        while (tracks.hasNext()) {
            String         tempVideoPath  = tracks.next();
            boolean        hasVideo       = true;
            boolean        hasAudio       = true;
            MediaExtractor videoExtractor = new MediaExtractor();
            try {
                videoExtractor.setDataSource(tempVideoPath);
            } catch (IOException e) {
                e.printStackTrace();
            }

            int inVideoTrackIndex = selectTrack(videoExtractor, "video/");
            if (inVideoTrackIndex < 0) {
                hasVideo = false;
            }
            videoExtractor.selectTrack(inVideoTrackIndex);

            MediaExtractor audioExtractor = new MediaExtractor();
            try {
                audioExtractor.setDataSource(tempVideoPath);
            } catch (IOException e) {
                e.printStackTrace();
            }

            int inAudioTrackIndex = selectTrack(audioExtractor, "audio/");
            if (inAudioTrackIndex < 0) {
                hasAudio = false;
            }
            audioExtractor.selectTrack(inAudioTrackIndex);
            boolean bMediaDone = false;
            long    pts        = 0L;
            long    audioPts   = 0L;
            long    videoPts   = 0L;
            while (!bMediaDone) {
                if (!hasAudio && !hasVideo) {
                    break;
                }

                int            currOutTrackIndex;
                int            currTrackIndex;
                MediaExtractor currMediaExtractor;


                if (hasAudio && (!hasVideo || audioPts - videoPts <= 50000L)) {
                    Log.i(TAG, "    process audio");
                    currTrackIndex = inAudioTrackIndex;
                    currOutTrackIndex = outputAudioTrackIndex;
                    currMediaExtractor = audioExtractor;
                } else {
                    Log.i(TAG, "    process video");
                    currTrackIndex = inVideoTrackIndex;
                    currOutTrackIndex = outputVideoTrackIndex;
                    currMediaExtractor = videoExtractor;
                }

                mReadBuffer.rewind();
                // 读取数据
                int chunkSize = currMediaExtractor.readSampleData(mReadBuffer, 0);
                if (chunkSize < 0) {
                    if (currTrackIndex == inAudioTrackIndex) {
                        hasAudio = false;
                    } else if (currTrackIndex == inVideoTrackIndex) {
                        hasVideo = false;
                    }
                } else {
                    if (currMediaExtractor.getSampleTrackIndex() != currTrackIndex) {
                        Log.e(TAG, "joinVideo: the currTrackIndex " + currTrackIndex + " is wrong.");
                    }

                    pts = currMediaExtractor.getSampleTime();
                    if (currTrackIndex == inVideoTrackIndex) {
                        videoPts = pts;
                    } else if (currTrackIndex == inAudioTrackIndex) {
                        audioPts = pts;
                    }

                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    bufferInfo.offset = 0;
                    bufferInfo.size = chunkSize;
                    // pts 需要再次重新做一个计算
                    bufferInfo.presentationTimeUs = ptsOffset + pts;
                    if ((currMediaExtractor.getSampleFlags() & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
                        bufferInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
                    }
                    mReadBuffer.rewind();
                    Log.i(TAG, String.format("write sample track %d, size %d, pts %d flag %d", currOutTrackIndex, bufferInfo.size, bufferInfo.presentationTimeUs, bufferInfo.flags));

                    mediaMuxer.writeSampleData(currOutTrackIndex, mReadBuffer, bufferInfo);
                    currMediaExtractor.advance();

                }
            }
            ptsOffset += Math.max(videoPts, audioPts);
            // 前一个文件的最后一帧，和后一个文件的第一帧，差10ms，这是估算值，不准确，但能用
            ptsOffset += 1000L;
            Log.d(TAG, "    ptsOffset : " + ptsOffset);
            videoExtractor.release();
            audioExtractor.release();

        }

        if (mediaMuxer != null) {
            mediaMuxer.stop();
            mediaMuxer.release();
            mediaMuxer = null;
        }

        Log.i(TAG, "     video join finished.");
        return true;

    }


    private final static String MIME_VIDEO = "video/";
    private final static String MIME_AUDIO = "audio/";


    /**
     * 1: 通过MediaExtractor 从原视频中获取 audioFormat 和videoFormat
     * 2: 将 audioFormat 和 video MediaMuxer
     *
     * @return
     */
    private boolean setOutputMp4MediaFormatAndStartMuxer() {
        Log.d(TAG, "setOutputMp4MediaFormatAndStartMuxer: ");
        boolean isHaveAudioFormat = false;
        boolean isHaveVideoFormat = false;

        for (String videoPath : mVideoPathList) {
            MediaExtractor extractor = new MediaExtractor();
            try {
                extractor.setDataSource(videoPath);
            } catch (IOException e) {

                e.printStackTrace();
                return false;
            }

            int trackIndex;
            if (!isHaveVideoFormat) {
                trackIndex = selectTrack(extractor, MIME_VIDEO);
                if (trackIndex < 0) {
                    Log.e(TAG, "can`t find video index");

                } else {
                    extractor.selectTrack(trackIndex);
                    mVideoFormat = extractor.getTrackFormat(trackIndex);
                    isHaveVideoFormat = true;
                }
            }

            if (!isHaveAudioFormat) {
                trackIndex = selectTrack(extractor, MIME_AUDIO);
                if (trackIndex < 0) {
                    Log.e(TAG, "can`t find audio index");

                } else {
                    extractor.selectTrack(trackIndex);
                    mAudioFormat = extractor.getTrackFormat(trackIndex);
                    isHaveAudioFormat = true;
                }
            }
            extractor.release();
            if (isHaveAudioFormat && isHaveAudioFormat) {
                break;
            }
        }


        try {
            mediaMuxer = new MediaMuxer(outputVideoPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (isHaveVideoFormat) {
            outputVideoTrackIndex = mediaMuxer.addTrack(mVideoFormat);
        }
        if (isHaveAudioFormat) {
            outputAudioTrackIndex = mediaMuxer.addTrack(mAudioFormat);
        }
        Log.i(TAG, "    outputVideoTrackIndex: " + outputVideoTrackIndex);
        Log.i(TAG, "    outputAudioTrackIndex: " + outputAudioTrackIndex);
        mediaMuxer.start();
        return true;


    }


    private int selectTrack(MediaExtractor extractor, String mimePrefix) {
        int trackCount = extractor.getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            MediaFormat trackFormat = extractor.getTrackFormat(i);
            String      mime        = trackFormat.getString("mime");
            if (mime.startsWith(mimePrefix)) {
                return i;
            }

        }

        return -1;
    }


}
