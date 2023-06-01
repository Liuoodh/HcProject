package com.xjtu.hc.utils;

import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacv.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;


public class Ps2Rtmp {


    FFmpegFrameGrabber grabber = null;
    FFmpegFrameRecorder recorder = null;
    PipedInputStream inputStream =new PipedInputStream(2048);
    PipedOutputStream outputStream=new PipedOutputStream(this.inputStream);
    String pushAddress;

    public Ps2Rtmp() throws IOException {
    }


    /**
     * 异步接收海康设备sdk回调实时视频裸流数据并推至流媒体服务器
     *
     * @param data
     * @param size
     */
    @Async
    public void push(byte[] data, int size) throws IOException, InterruptedException {
        inputStream = new PipedInputStream(size);
        outputStream = new PipedOutputStream(inputStream);
        try {
            outputStream.write(data, 0, size);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        grabber = new FFmpegFrameGrabber(inputStream);
        grabber.setOption("rtsp_transport", "tcp");
        grabber.setOption("analyzeduration", "1000");
        grabber.setOption("timeout", "10000");
        grabber.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
        grabber.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        grabber.setAudioStream(Integer.MIN_VALUE);
        grabber.setFormat("mpeg");
        long stime = System.currentTimeMillis();
        // 检测回调函数是否有数据流产生，防止avformat_open_input函数阻塞
        do {
            Thread.sleep(100);
            System.out.println("sleep!");
            if (System.currentTimeMillis() - stime > 20000) {
                System.out.println("-----SDK回调无视频流产生------");
                return;
            }
        } while (inputStream.available() != 2048);

        // 只打印错误日志
        avutil.av_log_set_level(avutil.AV_LOG_QUIET);
        FFmpegLogCallback.set();
        System.out.println("1");
        grabber.start();
        System.out.println("--------开始推送视频流---------");
        recorder = new FFmpegFrameRecorder(pushAddress, grabber.getImageWidth(), grabber.getImageHeight(), grabber.getAudioChannels());
        recorder.setInterleaved(true);
        // 画质参数
        recorder.setVideoOption("crf", "28");
        // H264编/解码器
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setVideoBitrate(grabber.getVideoBitrate());
        // 封装flv格式
        recorder.setFormat("flv");
        // 视频帧率，最低保证25
        recorder.setFrameRate(25);
        // 关键帧间隔 一般与帧率相同或者是帧率的两倍
        recorder.setGopSize(50);
        // yuv420p
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
        recorder.start();
        int count = 0;
        Frame frame;
        while (grabber.hasVideo() && (frame = grabber.grab()) != null) {
            count++;
            if (count % 100 == 0) {
                System.out.println("推送视频帧次数：" + count);
            }
            if (frame.samples != null) {
                System.out.println("检测到音频");
            }
            recorder.record(frame);
        }
        if (grabber != null) {
            grabber.stop();
            grabber.release();
        }
        if (recorder != null) {
            recorder.stop();
            recorder.release();
        }
    }


}
