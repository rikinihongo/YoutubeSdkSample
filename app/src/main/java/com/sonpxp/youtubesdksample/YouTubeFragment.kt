package com.sonpxp.youtubesdksample

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.OrientationEventListener
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.FullscreenListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.sonpxp.youtubesdksample.databinding.FragmentYoutubeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class YouTubeFragment : Fragment() {

    private var _binding: FragmentYoutubeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: YouTubeViewModel by viewModels()

    private var youTubePlayer: YouTubePlayer? = null
    private var isFullscreen = false
    private var exitFullscreenCallback: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentYoutubeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycle.addObserver(binding.youtubePlayerView)
        initializeYouTubePlayer()
        observeViewModel()

        binding.btnLoad.setOnClickListener {
            viewModel.loadVideo("bFzM6XQn-zQ")
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.videoId.collect { videoId ->
                    videoId?.let { youTubePlayer?.loadVideo(it, 0f) }
                }
            }
        }
    }

    private fun initializeYouTubePlayer() {
        val options = IFramePlayerOptions.Builder(requireContext())
            .controls(1)
            .fullscreen(1)
            .rel(0)
            .ivLoadPolicy(3)
            .ccLoadPolicy(0)
            .build()

        binding.youtubePlayerView.initialize(
            youTubePlayerListener = object : AbstractYouTubePlayerListener() {
                override fun onReady(player: YouTubePlayer) {
                    youTubePlayer = player
                    viewModel.videoId.value?.let { player.loadVideo(it, 0f) }
                }

                override fun onCurrentSecond(player: YouTubePlayer, second: Float) {
                    viewModel.updateCurrentSecond(second)
                }

                override fun onError(player: YouTubePlayer, error: PlayerConstants.PlayerError) {
                    Toast.makeText(requireContext(), "YouTube Error: $error", Toast.LENGTH_SHORT).show()
                }
            },
            playerOptions = options
        )

        binding.youtubePlayerView.addFullscreenListener(fullscreenListener)
    }

    // ====================== LAYOUT FULLSCREEN ======================

    private val fullscreenListener = object : FullscreenListener {
        override fun onEnterFullscreen(fullscreenView: View, exitFullscreen: () -> Unit) {
            if (isFullscreen) return
            isFullscreen = true
            exitFullscreenCallback = exitFullscreen
            enterFullscreenLayout(fullscreenView)

            if (!isLandscapeNow()) {
                requireActivity().requestedOrientation =
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        }

        override fun onExitFullscreen() {
            if (!isFullscreen) return
            isFullscreen = false
            exitFullscreenCallback = null
            exitFullscreenLayout()

            if (isLandscapeNow()) {
                requireActivity().requestedOrientation =
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    private fun requestExitFullscreen() {
        exitFullscreenCallback?.invoke()
    }

    private fun enterFullscreenLayout(fullscreenView: View) {
        (fullscreenView.parent as? ViewGroup)?.removeView(fullscreenView)
        binding.fullscreenContainer.removeAllViews()
        binding.fullscreenContainer.addView(fullscreenView)
        binding.fullscreenContainer.isVisible = true
        binding.appBarLayout.isVisible = false
        binding.youtubePlayerView.isVisible = false

        hideSystemBars()
    }

    private fun exitFullscreenLayout() {
        binding.fullscreenContainer.removeAllViews()
        binding.fullscreenContainer.isVisible = false
        binding.appBarLayout.isVisible = true
        binding.youtubePlayerView.isVisible = true

        showSystemBars()
    }

    private fun hideSystemBars() {
        val window = requireActivity().window
        // Phải set trước khi hide
        WindowCompat.setDecorFitsSystemWindows(window, false)

        ViewCompat.setOnApplyWindowInsetsListener(binding.fullscreenContainer) { view, _ ->
            // Nuốt hết insets, không cho propagate xuống child
            view.setPadding(0, 0, 0, 0)
            WindowInsetsCompat.CONSUMED
        }

        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun showSystemBars() {
        val window = requireActivity().window
        WindowCompat.setDecorFitsSystemWindows(window, true)

        // Reset insets listener về mặc định
        ViewCompat.setOnApplyWindowInsetsListener(binding.fullscreenContainer, null)

        WindowCompat.getInsetsController(window, window.decorView).apply {
            show(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun isLandscapeNow(): Boolean =
        resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // ====================== LIFECYCLE ======================

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroyView() {
        if(isFullscreen) requestExitFullscreen()
        binding.youtubePlayerView.removeFullscreenListener(fullscreenListener)
        youTubePlayer = null
        super.onDestroyView()
        _binding = null
    }
}