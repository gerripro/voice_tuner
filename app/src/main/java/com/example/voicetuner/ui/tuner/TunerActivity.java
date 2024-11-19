package com.example.voicetuner.ui.tuner;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.widget.Button;
import android.widget.TextView;

import com.example.voicetuner.R;

public class TunerActivity extends AppCompatActivity {
    private Button startButton;
    private Button stopButton;
    TextView pitchTextView;
    private Thread recordingThread;
    private static final int SAMPLE_RATE = 44100; // 44.1 kHz
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
    private AudioRecord audioRecord;
    private boolean isRecording = false;

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
        startButton = findViewById(R.id.startListeningButton);
        stopButton = findViewById(R.id.stopListeningButton);
        pitchTextView = findViewById(R.id.pitchTextView);
        startButton.setOnClickListener(view -> {
            startListening();
            checkButtonStates();
        });
        stopButton.setOnClickListener(view -> {
            stopListening();
            checkButtonStates();
        });
    }

    private void checkButtonStates() {
        startButton.setEnabled(!isRecording);
        stopButton.setEnabled(isRecording);
    }

    @SuppressLint("MissingPermission")
    private void startListening() {
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                BUFFER_SIZE);

        audioRecord.startRecording();
        isRecording = true;

        // Start a new thread to read the audio data
        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                short[] buffer = new short[BUFFER_SIZE];
                while (isRecording) {
//                    System.out.println("recording");
                    int read = audioRecord.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        double pitch = detectPitch(buffer, SAMPLE_RATE);
                        runOnUiThread(() -> pitchTextView.setText("Note: " + pitch));
//                        if (pitch > 0) {
//                            String note = frequencyToNoteName(pitch);
//                            runOnUiThread(() -> pitchTextView.setText("Note: " + note));
//                        } else {
//                            runOnUiThread(() -> pitchTextView.setText("No pitch detected"));
//                        }
                    }
                }
            }
        }, "Audio Recording Thread");
        recordingThread.start();

    }
    private double detectPitch(short[] buffer, int sampleRate) {
        int bufferSize = buffer.length;
        double[] differences = new double[bufferSize];
        double minDifference = Double.MAX_VALUE;
        int tauEstimate = -1;

        // Step 1: Calculate the difference function
        for (int tau = 1; tau < bufferSize; tau++) {
            double sum = 0;
            for (int i = 0; i < bufferSize - tau; i++) {
                double delta = buffer[i] - buffer[i + tau];
                sum += delta * delta;
            }
            differences[tau] = sum;
        }

        // Step 2: Calculate the cumulative mean normalized difference
        differences[0] = 1; // Avoid division by zero
        double runningSum = 0;
        for (int tau = 1; tau < bufferSize; tau++) {
            runningSum += differences[tau];
            differences[tau] /= (runningSum / tau);
        }

        // Step 3: Find the first minimum threshold crossing
        double threshold = 0.2;  // Adjusted threshold for reliability
        for (int tau = 2; tau < bufferSize / 2; tau++) {
            if (differences[tau] < threshold && differences[tau] < minDifference) {
                minDifference = differences[tau];
                tauEstimate = tau;
                break;  // Stop at the first significant minimum
            }
        }

        // Step 4: Calculate pitch from estimated tau
        if (tauEstimate != -1) {
            return (double) sampleRate / tauEstimate;
        } else {
            return -1; // No pitch detected
        }
    }

    private String frequencyToNoteName(double frequency) {
        // Reference A4 = 440 Hz
        final double A4 = 440.0;
        String[] noteNames = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

        // Calculate the number of semitones from A4
        int semitoneOffset = (int) Math.round(12 * Math.log(frequency / A4) / Math.log(2));

        // Find the note and octave
        int noteIndex = (semitoneOffset + 9) % 12; // A4 is index 9 in the noteNames array
        int octave = 4 + ((semitoneOffset + 9) / 12);

        return noteNames[noteIndex] + octave;
    }
    private void stopListening() {
        if (audioRecord != null) {
            isRecording = false;
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;

            // Wait for the recording thread to finish
            if (recordingThread != null) {
                try {
                    recordingThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                recordingThread = null;
            }
        }
        if (audioRecord != null) {
            isRecording = false;
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;

            // Wait for the recording thread to finish
            if (recordingThread != null) {
                try {
                    recordingThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                recordingThread = null;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopListening();
    }
}