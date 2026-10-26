package com.looker.droidify.ui.repository

import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.looker.droidify.R
import com.looker.droidify.database.CursorOwner
import com.looker.droidify.databinding.ScreenRepositoriesBinding
import com.looker.droidify.ui.ScreenFragment
import com.looker.droidify.utility.common.extension.dp
import com.looker.droidify.utility.common.extension.systemBarsPadding
import com.looker.droidify.utility.extension.mainActivity
import com.looker.droidify.widget.addDivider

class RepositoriesFragment : ScreenFragment(), CursorOwner.Callback {

    private var _binding: ScreenRepositoriesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = ScreenRepositoriesBinding.inflate(inflater, container, false)
        val view = fragmentBinding.root.apply {
            // Repository list is static: no add / scan actions.
            binding.scanQr.visibility = View.GONE
            binding.addRepo.visibility = View.GONE
            binding.recyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                isMotionEventSplittingEnabled = false
                setHasFixedSize(true)
                adapter = RepositoriesAdapter(
                    navigate = { mainActivity.navigateRepository(it.id) },
                )
                addDivider { _, _, configuration ->
                    configuration.set(
                        needDivider = true,
                        toTop = false,
                        paddingStart = 16.dp,
                        paddingEnd = 16.dp,
                    )
                }
                systemBarsPadding()
            }
            fragmentBinding.fragmentContent.addView(binding.root)
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainActivity.cursorOwner.attach(this, CursorOwner.Request.Repositories)
        mainActivity.onToolbarCreated(toolbar)
        toolbar.title = getString(R.string.repositories)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
        mainActivity.cursorOwner.detach(this)
    }

    override fun onCursorData(request: CursorOwner.Request, cursor: Cursor?) {
        (binding.recyclerView.adapter as RepositoriesAdapter).cursor = cursor
    }
}
