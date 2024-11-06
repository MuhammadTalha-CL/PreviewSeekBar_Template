package com.example.myapplication2
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.widget.ImageButton
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.TimeBar
import com.example.myapplication2.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


//last and best approach

class MainActivity : FragmentActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var exoplayer: ExoPlayer
    private var retriever: MediaMetadataRetriever = MediaMetadataRetriever()
    private var totalDuration: Int = 0
    private var videoUrl: String? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentJob: Job? = null
    private var lastUpdate: Long = 0
    private val handler = Handler(Looper.getMainLooper())  // Handler for periodic checks
    private val visibilityCheckRunnable = Runnable {
        checkControlsVisibility()
    }

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        videoUrl =
            "https://bbcontent.nayatel.com/content/tour_guide_video_clips/multi_linguistic.mp4"
        setUrl(videoUrl!!)

        exoplayer = ExoPlayer.Builder(this).build()
        binding.playerView.player = exoplayer
        val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
        exoplayer.setMediaItem(mediaItem)
        exoplayer.prepare()
        exoplayer.play()
        startVisibilityChecks()
        binding.playerView.findViewById<DefaultTimeBar>(R.id.exo_progress)
            .addListener(object : TimeBar.OnScrubListener {
                override fun onScrubStart(timeBar: TimeBar, position: Long) {
                    exoplayer.pause()
                    thumbnailPosition(position)
                    updateScrub(position)

                }

                override fun onScrubMove(timeBar: TimeBar, position: Long) {
                    exoplayer.pause()
                    thumbnailPosition(position)
                    updateScrub(position)

                }

                override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
                    exoplayer.pause()
                    thumbnailPosition(position)
                    updateScrub(position)

                }
            })
        binding.playerView.findViewById<ImageButton>(R.id.exo_play_pause).setOnClickListener {
            if (binding.previewImage.visibility == View.VISIBLE || !exoplayer.isPlaying) {
                binding.previewImage.visibility = View.GONE
                exoplayer.play()
                binding.playerView.findViewById<ImageButton>(R.id.exo_play_pause)
                    .setImageResource(androidx.media3.ui.R.drawable.exo_ic_play_circle_filled)
            } else {
                exoplayer.pause()
                binding.playerView.findViewById<ImageButton>(R.id.exo_play_pause)
                    .setImageResource(androidx.media3.ui.R.drawable.exo_ic_pause_circle_filled)
            }
        }
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
        currentJob?.cancel()
        return super.onKeyLongPress(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                if (!exoplayer.isPlaying) {
                    exoplayer.play()
                    currentJob?.cancel()
                    binding.previewImage.visibility = View.GONE
                }
                return true
            }

            else -> return super.onKeyUp(keyCode, event)
        }
    }

    private fun updateScrub(position: Long) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdate < 200) return
        lastUpdate = currentTime
        currentJob?.cancel()

        currentJob = coroutineScope.launch(Dispatchers.IO) {
            val bitmap = retriever.getFrameAtTime(
                position * 1000,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
            )
            withContext(Dispatchers.Main) {
                if (bitmap != null) {
                    binding.previewImage.setImageBitmap(bitmap)
                    exoplayer.play()
                }
            }
        }
    }


    private fun thumbnailPosition(position: Long) {
        val timeBarWidth = binding.playerView.findViewById<DefaultTimeBar>(R.id.exo_progress).width
        val imageWidth = binding.previewImage.width
        val newPos = (position.toFloat() / exoplayer.duration) * timeBarWidth - (imageWidth / 2)
        val minPos = 0f
        val maxPos = timeBarWidth - imageWidth.toFloat()
        val clampedPos = newPos.coerceIn(minPos, maxPos)
        binding.previewImage.x = clampedPos
        binding.previewImage.visibility = View.VISIBLE
    }

    private fun setUrl(url: String) {
        retriever.setDataSource(url, HashMap())
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        totalDuration = duration?.toInt() ?: 0
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
        currentJob?.cancel()
        exoplayer.release()
        handler.removeCallbacks(visibilityCheckRunnable)
        retriever.release()
    }

    private fun startVisibilityChecks() {
        handler.postDelayed(visibilityCheckRunnable, 100)  // Check every 100ms (adjust as needed)
    }

    // Check if the controls are visible or not
    @OptIn(UnstableApi::class)
    private fun checkControlsVisibility() {
        if (binding.previewImage.visibility == View.VISIBLE) {
            binding.previewImage.visibility = View.GONE
        }
        handler.postDelayed(visibilityCheckRunnable, 10000)  // Check again after 100ms
    }
}


///Aggressive Approach
//
//class MainActivity : FragmentActivity() {
//
//    private lateinit var binding: ActivityMainBinding
//    private lateinit var exoplayer: ExoPlayer
//    private var retriever: MediaMetadataRetriever = MediaMetadataRetriever()
//    private var lastUpdate: Long = 0
//    private var totalDuration: Int = 0
//    private var videoUrl: String? = null
//    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
//    private var currentJob: Job? = null
//    private val frameRetrieverScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
//    private val frameCache = mutableMapOf<Long, Bitmap>()
//    private var isCachingInProgress = false
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//        videoUrl = "https://bbcontent.nayatel.com/content/tour_guide_video_clips/multi_linguistic.mp4"
//        setUrl(videoUrl!!)
//        exoplayer = ExoPlayer.Builder(this).build()
//        binding.playerView.player = exoplayer
//        val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
//        exoplayer.setMediaItem(mediaItem)
//        exoplayer.prepare()
//        exoplayer.play()
//        //cacheFramesInBackground()
//        binding.playerView.findViewById<DefaultTimeBar>(R.id.exo_progress)
//            .addListener(object : TimeBar.OnScrubListener {
//                override fun onScrubStart(timeBar: TimeBar, position: Long) {
//                    exoplayer.pause()  // Pause playback while scrubbing
//                    exoplayer.playWhenReady = false
//                    updatePreview(position)  // Update preview immediately when scrubbing starts
//                    updatePreviewPosition(position)  // Update position for preview
//                }
//
//                override fun onScrubMove(timeBar: TimeBar, position: Long) {
//                    exoplayer.pause()
//                    updatePreview(position)
//                    updatePreviewPosition(position)
//                }
//
//                override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
//                    exoplayer.pause()
//                    updatePreview(position)
//                    updatePreviewPosition(position)
//                }
//            })
//        binding.playerView.findViewById<ImageButton>(R.id.exo_play_pause).setOnClickListener {
//            if (binding.previewImage.visibility == View.VISIBLE || !exoplayer.isPlaying) {
//                binding.previewImage.visibility = View.GONE
//                exoplayer.playWhenReady = true
//                binding.playerView.findViewById<ImageButton>(R.id.exo_play_pause)
//                    .setImageResource(androidx.media3.ui.R.drawable.exo_ic_play_circle_filled)
//            } else {
//                exoplayer.pause()
//                binding.playerView.findViewById<ImageButton>(R.id.exo_play_pause)
//                    .setImageResource(androidx.media3.ui.R.drawable.exo_ic_pause_circle_filled)
//            }
//        }
//    }
//    private fun cacheFramesInBackground() {
//        Log.d("Cache", "cacheFramesInBackground: ENTERED IN THE FUNCTION")
//        if (isCachingInProgress) return
//        isCachingInProgress = true
//        frameRetrieverScope.launch {
//            try {
//                val totalDurationInMs = totalDuration * 1000
//                val interval = 500
//                val jobs = mutableListOf<Deferred<Unit>>()
//                for (time in 0 until totalDurationInMs step interval) {
//                    jobs.add(async {
//                        if (!frameCache.containsKey(time.toLong())) {
//                            val bitmap = retriever.getFrameAtTime(time.toLong(), MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
//                            if (bitmap != null) {
//                                Log.d("Cache", "cacheFramesInBackground: retreive bitmap")
//                                synchronized(frameCache) {
//                                    frameCache[time.toLong()] = bitmap
//                                }
//                            }
//                        }
//                    })
//                }
//                jobs.awaitAll()
//            } catch (e: Exception) {
//                e.printStackTrace()
//            } finally {
//                isCachingInProgress = false
//            }
//        }
//    }
//
//    private fun updatePreview(position: Long) {
//        currentJob?.cancel()
//        val currentTime = System.currentTimeMillis()
//        if (currentTime - lastUpdate < 300) return
//        lastUpdate = currentTime
//
//        currentJob = frameRetrieverScope.launch {
//            try {
////                val cachedBitmap = frameCache[position]
////                if (cachedBitmap != null) {
////                    Log.d("Cache", "updatePreview: Cached")
////                    withContext(Dispatchers.Main) {
////                        binding.previewImage.setImageBitmap(cachedBitmap)
////                        binding.previewImage.visibility = View.VISIBLE
////                    }
////                } else {
//                    Log.d("Cache", "updatePreview: Not Cache Retreiving On Real Time")
//                    val bitmap = retriever.getFrameAtTime(position * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
//                    if (bitmap != null) {
//                      //  frameCache[position] = bitmap
//                        Log.d("Cache", "updatePreview: Not Cache Retreiving On Real Time and Size = ${frameCache.size}")
//                        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 320, max(1, bitmap.height * 320 / bitmap.width), true)
//                        withContext(Dispatchers.Main) {
//                            binding.previewImage.setImageBitmap(bitmap)
//                            binding.previewImage.visibility = View.VISIBLE
//                        }
////                    }
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }
//    }
//    private fun updatePreviewPosition(position: Long) {
//        val timeBarWidth = binding.playerView.findViewById<DefaultTimeBar>(R.id.exo_progress).width
//        val imageWidth = binding.previewImage.width
//        val newPos = (position.toFloat() / exoplayer.duration) * timeBarWidth - (imageWidth / 2)
//        val minPos = 0f
//        val maxPos = timeBarWidth - imageWidth.toFloat()
//        val clampedPos = newPos.coerceIn(minPos, maxPos)
//        binding.previewImage.x = clampedPos
//        binding.previewImage.visibility = View.VISIBLE
//    }
//
//    private fun setUrl(url: String) {
//        retriever.setDataSource(url, HashMap())
//        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
//        totalDuration = duration?.toInt() ?: 0
//    }
//
//    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
//        currentJob?.cancel()
//        return super.onKeyLongPress(keyCode, event)
//    }
//
//    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
//        when (keyCode) {
//            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
//                if (!exoplayer.isPlaying) {
//                    exoplayer.playWhenReady = true
//                    binding.previewImage.visibility = View.GONE
//                } else {
//                    exoplayer.pause()
//                    binding.previewImage.visibility = View.VISIBLE
//                }
//                return true
//            }
//            else -> return super.onKeyUp(keyCode, event)
//        }
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        coroutineScope.cancel()
//        currentJob?.cancel()
//        exoplayer.release()
//        retriever.release()
//    }
//}
