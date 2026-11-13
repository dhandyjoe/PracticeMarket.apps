package com.automation.practice.feature.shop

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.automation.practice.R
import com.automation.practice.core.ui.applySystemInsets
import com.automation.practice.databinding.FragmentImageViewerBinding
import com.automation.practice.databinding.ItemViewerThumbBinding
import com.automation.practice.domain.Product
import java.util.Locale

/** SHOP-06 Image Viewer: layar penuh hitam, zoom, geser antar gambar. */
class ImageViewerFragment : ShopFragment(R.layout.fragment_image_viewer, lightStatusBar = false) {
    private lateinit var product: Product

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        product = vm.catalog.product(requireArguments().getString(ARG_ID).orEmpty()) ?: return nav.back()
        val b = FragmentImageViewerBinding.bind(view)
        b.shopHeaderViewer.applySystemInsets(top = true)
        b.shopListViewerThumbs.applySystemInsets(bottom = true)
        val visual = vm.visual(product)
        visual.applyTo(b.shopImgViewerBg, b.shopImgViewer)

        val thumbs = (0 until ProductFragment.GALLERY_SIZE).map { index ->
            ItemViewerThumbBinding.inflate(layoutInflater, b.shopListViewerThumbs, false).also { t ->
                visual.applyTo(t.root, t.shopImgViewerThumb)
                t.root.contentDescription = getString(R.string.gallery_image_fmt, index + 1)
                b.shopListViewerThumbs.addView(t.root)
            }
        }

        b.shopZoomImage.onZoomChanged = { zoom ->
            b.shopTxtZoomLevel.isVisible = zoom != 1f
            b.shopTxtZoomLevel.text = getString(R.string.viewer_zoom_fmt, String.format(Locale.forLanguageTag("id-ID"), "%.1f", zoom))
        }
        b.shopZoomImage.onSwipe = { step -> show(b, wrap(vm.galleryIndex.value + step), thumbs) }
        b.shopBtnPrevImage.setOnClickListener { show(b, wrap(vm.galleryIndex.value - 1), thumbs) }
        b.shopBtnNextImage.setOnClickListener { show(b, wrap(vm.galleryIndex.value + 1), thumbs) }
        b.shopBtnCloseViewer.setOnClickListener { nav.back() }
        thumbs.forEachIndexed { i, t -> t.root.setOnClickListener { show(b, i, thumbs) } }
        show(b, vm.galleryIndex.value, thumbs)
    }

    private fun wrap(index: Int) = (index + ProductFragment.GALLERY_SIZE) % ProductFragment.GALLERY_SIZE

    private fun show(b: FragmentImageViewerBinding, index: Int, thumbs: List<ItemViewerThumbBinding>?) {
        vm.galleryIndex.value = index
        b.shopZoomImage.reset()
        b.shopTxtImageCounter.text = getString(R.string.gallery_counter_fmt, index + 1, ProductFragment.GALLERY_SIZE)
        thumbs?.forEachIndexed { i, t -> t.root.isSelected = i == index }
    }

    companion object {
        private const val ARG_ID = "id"
        fun newInstance(id: String) = ImageViewerFragment().apply { arguments = bundleOf(ARG_ID to id) }
    }
}
