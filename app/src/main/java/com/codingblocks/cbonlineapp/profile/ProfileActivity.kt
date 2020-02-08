package com.codingblocks.cbonlineapp.profile

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import com.codingblocks.cbonlineapp.R
import com.codingblocks.cbonlineapp.baseclasses.BaseCBActivity
import com.codingblocks.cbonlineapp.util.FileUtils
import com.codingblocks.cbonlineapp.util.JWTUtils
import com.codingblocks.cbonlineapp.util.PreferenceHelper
import com.codingblocks.cbonlineapp.util.extensions.loadSvg
import com.codingblocks.cbonlineapp.util.extensions.observer
import com.codingblocks.cbonlineapp.util.extensions.showSnackbar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout.END_ICON_CUSTOM
import kotlinx.android.synthetic.main.activity_profile.*
import org.json.JSONException
import org.json.JSONObject
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class ProfileActivity : BaseCBActivity() {

    private val vm by viewModel<ProfileViewModel>()
    private val sharedPrefs by inject<PreferenceHelper>()

    var map = HashMap<String, String>()
    val id by lazy {
        JWTUtils.getIdentity(sharedPrefs.SP_JWT_TOKEN_KEY)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        updateViews(false)
        vm.fetchUser().observer(this) {
            graduation.setText(it.graduationyear)
            college.setText(it.college)
            email.setText(it.email)
            mobile.setText(it.mobile)
            if (it.verifiedemail != null) {
                emailLayout.endIconMode = END_ICON_CUSTOM
            }
            if (it.verifiedemail != null) {
                phoneLayout.endIconMode = END_ICON_CUSTOM
            }
            nameTv.text = "${it.firstname} ${it.lastname}"
            userNameTv.text = it.username
            userImgView.loadSvg(it.photo ?: "")
            branch.setText(it.branch ?: "Computer Science")
        }
        ediBtn.setOnClickListener {
            updateViews(true)
        }
        setList()
    }

    private fun updateViews(visible: Boolean) {
        ediBtn.isVisible = !visible
        updateBtn.isVisible = visible
        listOf(branch, college, graduation).forEach {
            it.isFocusableInTouchMode = visible
            it.isCursorVisible = visible
            it.isClickable = visible
        }
    }

    private fun setList() {
        val json =
            FileUtils.loadJsonObjectFromAsset(this, "demographics.json", "obj") as JSONObject?
        val collegeArray = json?.getJSONArray("colleges")
        val branchArray = json?.getJSONArray("branches")
        val collegeList: MutableList<String> = ArrayList()
        val branchList: MutableList<String> = ArrayList()
        try {
            for (i in 0 until collegeArray?.length()!!) {
                val ref = collegeArray.getJSONObject(i)?.getString("name")
                if (ref != null) {
                    collegeList.add(ref)
                }
            }
            for (i in 0 until branchArray?.length()!!) {
                val ref = branchArray.getJSONObject(i)?.getString("name")
                if (ref != null) {
                    branchList.add(ref)
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        val arrayAdapter: ArrayAdapter<String> =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, collegeList)
        college.setAdapter(arrayAdapter)
        college.setOnItemClickListener { _, _, position, id ->
            val name = arrayAdapter.getItem(position)

            for (i in 0 until collegeArray?.length()!!) {
                val ref = collegeArray.getJSONObject(i)
                if (ref.getString("name") == name) {
                    map["collegeId"] = ref.getString("id")
                }
            }
        }

        val arrayAdapter2: ArrayAdapter<String> =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, branchList)
        branch.setAdapter(arrayAdapter2)
        branch.setOnItemClickListener { _, _, position, id ->
            val name = arrayAdapter2.getItem(position)

            for (i in 0 until branchArray?.length()!!) {
                val ref = branchArray.getJSONObject(i)
                if (ref.getString("name") == name) {
                    map["branchId"] = ref.getString("id")
                }
            }
        }
    }

    fun updateProfile(view: View) {
        map["gradYear"] = graduation.text.toString()

        updateBtn.isEnabled = false

        vm.updateUser(id.toString(), map).observer(this) {
            when (it) {
                "Success" -> {
                    updateViews(false)
                }
                else -> {
                    updateBtn.isEnabled = true
                    profileRoot.showSnackbar(it, Snackbar.LENGTH_SHORT)
                }
            }
        }
    }
}
