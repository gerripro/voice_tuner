package com.example.voicetuner.ui.tuner;

import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.voicetuner.R;

import android.view.View;
import android.widget.Button;

public class TunerActivity extends AppCompatActivity {
    private boolean isListening = false;
    private AudioRecord audioRecord;
    private AudioTrack audioTrack;
    private Thread audioThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tuner);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button startListeningButton = findViewById(R.id.startListeningButton);
        Button stopListeningButton = findViewById(R.id.stopListeningButton);

        startListeningButton.setOnClickListener(v -> {
                startListening();
                startListeningButton.setEnabled(false);
                stopListeningButton.setEnabled(true);
        });
        stopListeningButton.setOnClickListener(v -> {
            stopListening();
            startListeningButton.setEnabled(true);
            stopListeningButton.setEnabled(false);
        });

    }

    private void startListening() {
        isListening = true;
        int sampleRate = 44100;
        int channelIn = AudioFormat.CHANNEL_IN_MONO;
        int channelOut = AudioFormat.CHANNEL_OUT_MONO;

        int bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelIn, AudioFormat.ENCODING_PCM_16BIT);

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelIn, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        audioTrack = new AudioTrack(channelOut, sampleRate, channelOut, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);

        audioThread = new Thread(() -> {
            audioRecord.startRecording();
            audioTrack.play();
            byte[] buffer = new byte[bufferSize];
            while (isListening) {
                int read = audioRecord.read(buffer, 0, buffer.length);
                if (read > 0) {
                    audioTrack.write(buffer, 0, read);
                }
            }
            audioRecord.stop();
            audioTrack.stop();
            audioRecord.release();
            audioTrack.release();
        });

        audioThread.start();
    }

    private void stopListening() {
        isListening = false;
        if (audioThread != null) {
            try {
                audioThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            audioThread = null;
        }
    }
}