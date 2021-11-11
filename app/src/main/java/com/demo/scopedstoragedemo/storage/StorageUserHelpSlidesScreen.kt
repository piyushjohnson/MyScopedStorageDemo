package com.demo.scopedstoragedemo.storage

import android.app.Activity
import android.app.Activity.RESULT_CANCELED
import android.content.Context
import android.graphics.Point
import android.os.Bundle
import android.transition.Scene
import android.transition.TransitionManager
import android.view.*
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.demo.scopedstoragedemo.R
import android.view.Display
import com.limerse.slider.ImageCarousel
import com.limerse.slider.model.CarouselItem


/**
 * Use the [StorageUserHelpSlidesScreen.newInstance] factory method to
 * create an instance of this fragment.
 */
class StorageUserHelpSlidesScreen : Fragment() {
    private val grantedURIListingViewModel: GrantedURIListingViewModel by activityViewModels()
    private lateinit var carousel: ImageCarousel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_storage_user_help_slides_screen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        carousel = view.findViewById(R.id.carousel)

        // Register lifecycle. For activity this will be lifecycle/getLifecycle() and for fragment it will be viewLifecycleOwner/getViewLifecycleOwner().
        carousel.registerLifecycle(lifecycle)

        val list = listOf(
            CarouselItem(
                imageDrawable = R.drawable.step1
            ),
            CarouselItem(
                imageDrawable = R.drawable.step2
            ),
            CarouselItem(
                imageDrawable = R.drawable.step3
            ),
            CarouselItem(
                imageDrawable = R.drawable.step4
            ),
            CarouselItem(
                imageDrawable = R.drawable.step5
            )
        )

        carousel.setData(list)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
//        activity?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onDetach() {
        super.onDetach()
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            StorageUserHelpSlidesScreen()
    }
}

