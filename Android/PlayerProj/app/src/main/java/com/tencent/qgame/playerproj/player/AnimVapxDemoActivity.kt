/*
 * Tencent is pleased to support the open source community by making vap available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * Licensed under the MIT License (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tencent.qgame.playerproj.player

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import com.github.angads25.filepicker.model.DialogConfigs
import com.github.angads25.filepicker.model.DialogProperties
import com.github.angads25.filepicker.view.FilePickerDialog
import com.tencent.qgame.animplayer.AnimConfig
import com.tencent.qgame.animplayer.AnimView
import com.tencent.qgame.animplayer.inter.IAnimListener
import com.tencent.qgame.animplayer.inter.IFetchResource
import com.tencent.qgame.animplayer.inter.OnResourceClickListener
import com.tencent.qgame.animplayer.mix.Resource
import com.tencent.qgame.animplayer.util.ALog
import com.tencent.qgame.animplayer.util.IALog
import com.tencent.qgame.playerproj.R
import kotlinx.android.synthetic.main.activity_anim_simple_demo.*
import permissions.dispatcher.*
import java.io.File


/**
 * VAPX demo (融合特效Demo)
 * 必须使用组件里提供的工具才能生成VAPX动画
 */
@RuntimePermissions
class AnimVapxDemoActivity : Activity(), IAnimListener {

    companion object {
        private const val TAG = "AnimSimpleDemoActivity"
    }

    private val dir by lazy {
        // 存放在sdcard应用缓存文件中
        Environment.getExternalStorageDirectory().path
    }

    private var head1Img = true

    // 视频信息
    data class VideoInfo(val fileName: String, val md5: String)

    private var videoInfo : VideoInfo? = null// = VideoInfo("vapx.mp4", "f981e0f094ead842ad5ae99f1ffaa1a1")

    // 动画View
    private lateinit var animView: AnimView

    private val uiHandler by lazy {
        Handler(Looper.getMainLooper())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_anim_simple_demo)
        // 文件加载完成后会调用init方法
        init()
    }

    private fun init() {
        // 初始化日志
        initLog()
        // 初始化调试开关
        initTestView()
        // 获取动画view
        animView = playerView
        /**
         * 注册资源获取类
         */
        animView.setFetchResource(object : IFetchResource {
            /**
             * 获取图片资源
             * 无论图片是否获取成功都必须回调 result 否则会无限等待资源
             */
            override fun fetchImage(resource: Resource, result: (Bitmap?) -> Unit) {
                /**
                 * srcTag是素材中的一个标记，在制作素材时定义
                 * 解析时由业务读取tag决定需要播放的内容是什么
                 * 比如：一个素材里需要显示多个头像，则需要定义多个不同的tag，表示不同位置，需要显示不同的头像，文字类似
                 */
                val srcTag = resource.tag

                if (TextUtils.equals(srcTag, "[imgUser]") || TextUtils.equals(srcTag, "imgUser")) { // 此tag是已经写入到动画配置中的tag
                    val drawableId = R.drawable.head1
                    val options = BitmapFactory.Options()
                    options.inScaled = false
                    result(BitmapFactory.decodeResource(resources, drawableId, options))
                } else if (TextUtils.equals(srcTag, "[imgAnchor]") || TextUtils.equals(srcTag, "imgAnchor")) { // 此tag是已经写入到动画配置中的tag
                    val drawableId = R.drawable.head2
                    val options = BitmapFactory.Options()
                    options.inScaled = false
                    result(BitmapFactory.decodeResource(resources, drawableId, options))
                } else {
                    result(null)
                }
            }

            /**
             * 获取文字资源
             */
            override fun fetchText(resource: Resource, result: (String?) -> Unit) {

                // 此tag是已经写入到动画配置中的tag
                val tag = resource.tag
                ALog.d(TAG, "fetchText $tag")
                if (TextUtils.equals(tag, "[textUser]") || TextUtils.equals(tag, "textUser")) {
                    result("罗洪")
                } else if (TextUtils.equals(tag, "[textAnchor]") || TextUtils.equals(tag, "textAnchor")) {
                    result("姚佳烨")
                } else if (TextUtils.equals(tag, "[content]") || TextUtils.equals(tag, "content")) {
                    result("震撼登场")
                } else if (TextUtils.equals(tag, "[textFamily]") || TextUtils.equals(tag, "textFamily")) {
                    result("家族")
                } else if (TextUtils.equals(tag, "[levelUser]") || TextUtils.equals(tag, "levelUser")) {
                    result("LV.100")
                } else if (tag.startsWith("textUserContent")) {
                    result("罗洪 震撼登场")
                } else if (tag.startsWith("textUserLevelContent")) {
                    result("LV.100 罗洪 震撼登场")
                } else if (tag.startsWith("textUserLevel")) {
                    result("LV.100 罗洪")
                } else {
                    result(null)
                }
            }

            /**
             * 播放完毕后的资源回收
             */
            override fun releaseResource(resources: List<Resource>) {
                resources.forEach {
                    it.bitmap?.recycle()
                }
            }
        })

        // 注册点击事件监听
        animView.setOnResourceClickListener(object : OnResourceClickListener {
            override fun onClick(resource: Resource) {
                Toast.makeText(
                    this@AnimVapxDemoActivity,
                    "srcTag=${resource.tag} onClick",
                    Toast.LENGTH_LONG
                ).show()
            }
        })

        // 注册动画监听
        animView.setAnimListener(this)
        /**
         * 开始播放主流程
         * ps: 主要流程都是对AnimView的操作，其它比如队列，或改变窗口大小等操作都不是必须的
         */
//        play(videoInfo)
    }

    @NeedsPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    fun choose() {
        val path = DialogConfigs.DEFAULT_DIR + "/sdcard/VAP"

        val file = File(path)
        if (!file.exists()) {
            file.mkdirs()
        }

        val properties = DialogProperties()
        properties.selection_mode = DialogConfigs.SINGLE_MODE
        properties.selection_type = DialogConfigs.FILE_SELECT
        properties.root = file
        properties.error_dir = file
        properties.offset = file
        properties.extensions = arrayOf("mp4")

        val dialog = FilePickerDialog(this, properties)
        dialog.setTitle("选择Vap文件")
        dialog.setDialogSelectionListener { files ->
            if (files != null) {
                Toast.makeText(
                    this@AnimVapxDemoActivity,
                    "文件路径: ${files[0]}",
                    Toast.LENGTH_LONG
                ).show()

                videoInfo = VideoInfo(files[0], "")
            }
        }
        dialog.show()
    }

    @OnShowRationale(Manifest.permission.CAMERA)
    fun showRationaleForCamera(request: PermissionRequest) {
        showRationaleDialog(R.string.permission_camera_rationale, request)
    }

    @OnPermissionDenied(Manifest.permission.CAMERA)
    fun onCameraDenied() {
        Toast.makeText(this, R.string.permission_camera_denied, Toast.LENGTH_SHORT).show()
    }

    @OnNeverAskAgain(Manifest.permission.CAMERA)
    fun onCameraNeverAskAgain() {
        Toast.makeText(this, R.string.permission_camera_never_ask_again, Toast.LENGTH_SHORT).show()
    }

    private fun showRationaleDialog(@StringRes messageResId: Int, request: PermissionRequest) {
        AlertDialog.Builder(this)
            .setPositiveButton(R.string.button_allow) { _, _ -> request.proceed() }
            .setNegativeButton(R.string.button_deny) { _, _ -> request.cancel() }
            .setCancelable(false)
            .setMessage(messageResId)
            .show()
    }

    private fun play(videoInfo: VideoInfo?) {
        if (videoInfo == null) {
            Toast.makeText(this, "请选择播放文件", Toast.LENGTH_SHORT).show()
            return
        }

        // 播放前强烈建议检查文件的md5是否有改变
        // 因为下载或文件存储过程中会出现文件损坏，导致无法播放
        Thread {
            val file = File(videoInfo.fileName)
//            val md5 = FileUtil.getFileMD5(file)
//            if (videoInfo.md5 == md5) {
            // 开始播放动画文件
            animView.startPlay(file)
//            } else {
//                Log.e(TAG, "md5 is not match, error md5=$md5")
//            }
        }.start()
    }

    /**
     * 视频信息准备好后的回调，用于检查视频准备好后是否继续播放
     * @return true 继续播放 false 停止播放
     */
    override fun onVideoConfigReady(config: AnimConfig): Boolean {
        uiHandler.post {
            val w = window.decorView.width
            val lp = animView.layoutParams
            lp.width = if (w == 0) dp2px(this, 400f).toInt() else w
            lp.height = (w * config.height * 1f / config.width).toInt()
            animView.layoutParams = lp
        }
        return true
    }

    /**
     * 视频开始回调
     */
    override fun onVideoStart() {
        Log.i(TAG, "onVideoStart")
    }

    /**
     * 视频渲染每一帧时的回调
     * @param frameIndex 帧索引
     */
    override fun onVideoRender(frameIndex: Int, config: AnimConfig?) {
    }

    /**
     * 视频播放结束(失败也会回调onComplete)
     */
    override fun onVideoComplete() {
        Log.i(TAG, "onVideoComplete")
    }

    /**
     * 播放器被销毁情况下会调用onVideoDestroy
     */
    override fun onVideoDestroy() {
        Log.i(TAG, "onVideoDestroy")
    }

    /**
     * 失败回调
     * 一次播放时可能会调用多次，建议onFailed只做错误上报
     * @param errorType 错误类型
     * @param errorMsg 错误消息
     */
    override fun onFailed(errorType: Int, errorMsg: String?) {
        Log.i(TAG, "onFailed errorType=$errorType errorMsg=$errorMsg")
        Toast.makeText(
            this@AnimVapxDemoActivity,
            "播放失败: $errorType - $errorMsg",
            Toast.LENGTH_LONG
        ).show()
    }


    override fun onPause() {
        super.onPause()
        // 页面切换是停止播放
        animView.stopPlay()
    }


    private fun initLog() {
        ALog.isDebug = false
        ALog.log = object : IALog {
            override fun i(tag: String, msg: String) {
                Log.i(tag, msg)
            }

            override fun d(tag: String, msg: String) {
                Log.d(tag, msg)
            }

            override fun e(tag: String, msg: String) {
                Log.e(tag, msg)
            }

            override fun e(tag: String, msg: String, tr: Throwable) {
                Log.e(tag, msg, tr)
            }
        }
    }


    private fun initTestView() {
        btnLayout.visibility = View.VISIBLE
        /**
         * 选择资源按钮
         */
        btnChoose.setOnClickListener {
            choose()
        }
        /**
         * 开始播放按钮
         */
        btnPlay.setOnClickListener {
            play(videoInfo)
        }
        /**
         * 结束视频按钮
         */
        btnStop.setOnClickListener {
            animView.stopPlay()
        }
    }

//    private fun loadFile() {
//        val files = Array(1) {
//            videoInfo.fileName
//        }
//        FileUtil.copyAssetsToStorage(this, dir, files) {
//            uiHandler.post {
//                init()
//            }
//        }
//    }


    private fun dp2px(context: Context, dp: Float): Float {
        val scale = context.resources.displayMetrics.density
        return dp * scale + 0.5f
    }
}

