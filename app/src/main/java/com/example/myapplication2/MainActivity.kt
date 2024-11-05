package com.example.myapplication2

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import androidx.annotation.OptIn
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.TimeBar
import com.example.myapplication2.databinding.ActivityMainBinding
import kotlinx.coroutines.Runnable

class MainActivity : FragmentActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var exoplayer: ExoPlayer
    private var retriever: MediaMetadataRetriever = MediaMetadataRetriever()
    private val handler = Handler(Looper.getMainLooper())
    private var lastUpdate: Long = 0
    private var totalDuration: Int = 0
    private var videoUrl: String? = null
    private var updateRunnable : Runnable? = null
    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        videoUrl = "https://bbcontent.nayatel.com/content/tour_guide_video_clips/multi_linguistic.mp4"
        setUrl(videoUrl!!)
        exoplayer = ExoPlayer.Builder(this).build()
        binding.playerView.player = exoplayer
        val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
        exoplayer.setMediaItem(mediaItem)
        exoplayer.prepare()
        exoplayer.play()

        binding.playerView.findViewById<DefaultTimeBar>(R.id.exo_progress)
        binding.playerView.findViewById<DefaultTimeBar>(R.id.exo_progress).addListener(
            object : TimeBar.OnScrubListener {
                override fun onScrubStart(timeBar: TimeBar, position: Long) {
                    exoplayer.pause()
                    updatePreviewImagePosition(position)
                    updateThumbnail(position)
                }
                override fun onScrubMove(timeBar: TimeBar, position: Long) {
                    updatePreviewImagePosition(position)
                    updateThumbnail(position)
                    exoplayer.pause()


                }
                override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
                    exoplayer.seekTo(position)
                    exoplayer.pause()
                    updateRunnable?.let { handler.removeCallbacks(it) }
                }
            }
        )
    }
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                if(!exoplayer.isPlaying){
                    exoplayer.play()
                    binding.previewImage.visibility = View.GONE
                }
                return true
            }
            else -> return super.onKeyUp(keyCode, event)
        }
    }
private fun updatePreviewImagePosition(position: Long) {
    val timeBarWidth = binding.playerView.findViewById<DefaultTimeBar>(R.id.exo_progress).width
    val imageWidth = binding.previewImage.width
    val newPos = (position.toFloat() / exoplayer.duration) * timeBarWidth - (imageWidth / 2)
    val minPos = 0f
    val maxPos = timeBarWidth - imageWidth.toFloat()
    val clampedPos = newPos.coerceIn(minPos, maxPos)
    binding.previewImage.x = clampedPos
    binding.previewImage.visibility = View.VISIBLE
}


    private fun updateThumbnail(position: Long) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdate < 100) return
        lastUpdate = currentTime
        updateRunnable?.let { handler.removeCallbacks(it) }
        updateRunnable = Runnable {
            val bitmap: Bitmap? = retriever.getFrameAtTime(position*1000)
            binding.previewImage
                .setImageBitmap(bitmap)
        }
        exoplayer.playWhenReady = true
        Thread {
            updateRunnable?.let {
                handler.post(it)
            }
        }.start()
    }
    private fun setUrl(url: String) {
        retriever.setDataSource(url, HashMap())
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        totalDuration = duration?.toInt() ?: 0
    }
    override fun onDestroy() {
        super.onDestroy()
        exoplayer.release()
        retriever.release()
    }
}