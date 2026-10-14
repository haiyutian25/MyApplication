@file:Suppress("MagicNumber") // The body, antennae and face are raw geometry ratios of the radius.

package com.example.myapplication.ui.platform.components.mascot

import android.animation.TimeAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// How far the body is squashed while a finger is held on it.
private const val PRESS_SQUISH = 0.20f

/**
 * Draws the jelly mascot: it bobs, squashes and stretches, blinks, and follows the finger that
 * touches it with its eyes. Pressing it squashes it and letting go springs it back.
 */
class JellyBobView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    // 帧驱动器
    private val animator = TimeAnimator()
    private var totalTime = 0f

    // 尺寸与基准半径
    private var centerX = 0f
    private var centerY = 0f
    private var baseRadius = 0f

    // ================= 弹簧物理引擎参数 (Spring-Damper) =================
    private var isUserTouching = false
    private var springOffset = 0f // 当前形变量
    private var springVelocity = 0f // 速度
    private val springStiffness = 200f // 刚度系数 k
    private val springDamping = 7f // 阻尼系数 c (阻尼比 ζ≈0.25，回弹更 Q 更多下)

    // ================= 独立画笔组（彻底杜绝状态相互污染） =================
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bodyInnerShinePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val antennaStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val antennaOrbPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val eyeOrbitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1B1A38")
    }
    private val eyeCatchlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    }
    private val blushPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF5B7E")
        alpha = 115
    }
    private val lineDetailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1B1A38")
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // ================= 预分配 Path 与 RectF（避免 onDraw 触发 GC） =================
    private val bodyPath = Path()
    private val antennaPath = Path()
    private val mouthPath = Path()
    private val closedEyePath = Path()

    private val shadowRect = RectF()
    private val eyeRect = RectF()

    // 视线注视平滑跟踪
    private var targetGazeX = 0f
    private var targetGazeY = 0f
    private var currentGazeX = 0f
    private var currentGazeY = 0f

    init {
        animator.setTimeListener { _, _, deltaTime ->
            // Clamped, so that time spent off screen does not jump the animation.
            val dt = (deltaTime / 1000f).coerceIn(0.001f, 0.033f)
            totalTime += dt

            // 二阶弹簧物理阻尼迭代 (Hooke's Law: F = -k*x - c*v)
            val targetSquish = if (isUserTouching) PRESS_SQUISH else 0f
            val displacement = springOffset - targetSquish
            val springForce = -springStiffness * displacement - springDamping * springVelocity
            springVelocity += springForce * dt
            springOffset += springVelocity * dt

            // 视线插值平滑跟随
            currentGazeX += (targetGazeX - currentGazeX) * 7.5f * dt
            currentGazeY += (targetGazeY - currentGazeY) * 7.5f * dt

            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!animator.isStarted) animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    // A view that is not on screen does not need to be drawn, and the frames it would draw are
    // frames nobody sees, so the driver is stopped while the window is not visible.
    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility != View.VISIBLE) {
            animator.cancel()
        } else if (!animator.isStarted) {
            animator.start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        baseRadius = min(w, h) * 0.28f

        if (baseRadius <= 0f) return

        // 1. 果冻身体径向水光渐变（只在尺寸改变时构建一次）
        bodyPaint.shader = RadialGradient(
            -baseRadius * 0.25f, -baseRadius * 0.35f,
            baseRadius * 1.45f,
            intArrayOf(
                Color.parseColor("#4DEEEA"), // 荧光青绿
                Color.parseColor("#00B4D8"), // 水母透蓝
                Color.parseColor("#006599"), // 底层深海蓝
            ),
            floatArrayOf(0f, 0.62f, 1f),
            Shader.TileMode.CLAMP,
        )

        // 2. 身体内部柔和微光
        bodyInnerShinePaint.shader = RadialGradient(
            0f, -baseRadius * 0.1f,
            baseRadius * 0.8f,
            intArrayOf(
                Color.parseColor("#80FFFFFF"),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )

        // 3. 线条宽度比例自适应
        antennaStrokePaint.strokeWidth = baseRadius * 0.08f
        antennaStrokePaint.color = Color.parseColor("#00A2C7")
        antennaOrbPaint.color = Color.parseColor("#4DEEEA")
        lineDetailPaint.strokeWidth = baseRadius * 0.045f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (baseRadius <= 0f) return

        /* ================= 1. 动态数学物理模型 ================= */
        // (1) 浮游运动：微小正弦漂移
        val hoverY = sin(totalTime * 2.2f) * (baseRadius * 0.09f)

        // (2) 质量守恒弹性变形（呼吸 + 按压弹簧，限幅防止连点叠爆）
        val breathSquish = sin(totalTime * 2.2f + (PI.toFloat() / 2f)) * 0.05f
        val squish = (breathSquish + springOffset).coerceIn(-0.45f, 0.45f)
        val scaleX = 1f + squish
        val scaleY = 1f - squish

        // (3) 拟人化眨眼方程：每 3.6 秒眨眼一次
        val blinkCycle = totalTime % 3.6f
        val blink = if (blinkCycle < 0.16f) {
            val progress = blinkCycle / 0.16f
            abs(cos(progress * PI.toFloat()))
        } else {
            1f
        }

        /* ================= 2. 悬浮阴影（高度关联衰减） ================= */
        val elevationRatio = (hoverY / (baseRadius * 0.09f)) // -1f ~ 1f
        val shadowScale = (1f - elevationRatio * 0.12f + squish * 0.45f).coerceIn(0.6f, 1.4f)
        val shadowAlpha = ((0.28f - elevationRatio * 0.08f) * 255).toInt().coerceIn(20, 100)

        shadowPaint.color = Color.BLACK
        shadowPaint.alpha = shadowAlpha

        val shadowCenterY = centerY + baseRadius * 1.35f
        shadowRect.set(
            centerX - baseRadius * 0.85f * shadowScale,
            shadowCenterY - baseRadius * 0.16f * shadowScale,
            centerX + baseRadius * 0.85f * shadowScale,
            shadowCenterY + baseRadius * 0.16f * shadowScale,
        )
        canvas.drawOval(shadowRect, shadowPaint)

        /* ================= 3. 精灵本地坐标系 ================= */
        canvas.save()
        canvas.translate(centerX, centerY + hoverY + squish * baseRadius * 0.35f)
        canvas.scale(scaleX, scaleY)

        // --- B. 顶部动态波动触角 ---
        drawAntenna(canvas, isLeft = true)
        drawAntenna(canvas, isLeft = false)

        // --- C. 果冻主躯干 (光滑闭合贝塞尔水滴曲线) ---
        buildBodyPath()
        canvas.drawPath(bodyPath, bodyPaint)
        canvas.drawPath(bodyPath, bodyInnerShinePaint) // 叠加内部通透微光

        // --- E. 面部器官 (视线跟随、眨眼、腮红、微笑) ---
        drawFacialFeatures(canvas, blink)

        canvas.restore()
    }

    /**
     * 躯干平滑贝塞尔外轮廓
     */
    private fun buildBodyPath() {
        bodyPath.reset()
        val top = -baseRadius * 0.85f
        val bottom = baseRadius * 0.95f
        val left = -baseRadius * 1.05f
        val right = baseRadius * 1.05f

        bodyPath.moveTo(0f, top)
        // 右上半弧
        bodyPath.cubicTo(right * 0.65f, top, right, -baseRadius * 0.2f, right, baseRadius * 0.4f)
        // 右下弧至底中心
        bodyPath.cubicTo(right, bottom, right * 0.45f, bottom, 0f, bottom)
        // 左下弧
        bodyPath.cubicTo(left * 0.45f, bottom, left, bottom, left, baseRadius * 0.4f)
        // 左上弧闭合至顶点
        bodyPath.cubicTo(left, -baseRadius * 0.2f, left * 0.65f, top, 0f, top)
        bodyPath.close()
    }

    /**
     * 触角绘制：贝塞尔波形曲线 + 顶端荧光球
     */
    private fun drawAntenna(canvas: Canvas, isLeft: Boolean) {
        val sign = if (isLeft) -1f else 1f
        val phase = if (isLeft) 0f else (PI.toFloat() * 0.6f)
        // 惯性波浪摆幅
        val wave = sin(totalTime * 3.4f + phase) * (baseRadius * 0.16f)

        val rootX = sign * baseRadius * 0.28f
        val rootY = -baseRadius * 0.78f

        val ctrlX = sign * baseRadius * 0.52f + wave * 0.5f
        val ctrlY = -baseRadius * 1.15f

        val tipX = sign * baseRadius * 0.42f + wave
        val tipY = -baseRadius * 1.35f

        antennaPath.reset()
        antennaPath.moveTo(rootX, rootY)
        antennaPath.quadTo(ctrlX, ctrlY, tipX, tipY)
        canvas.drawPath(antennaPath, antennaStrokePaint)

        // 顶端发光小珠
        canvas.drawCircle(tipX, tipY, baseRadius * 0.08f, antennaOrbPaint)
    }

    /**
     * 面部表情渲染（眼神注视、自适应眨眼、腮红、笑容）
     */
    private fun drawFacialFeatures(canvas: Canvas, blink: Float) {
        val eyeDistance = baseRadius * 0.42f
        val eyeBaseY = baseRadius * 0.08f
        val eyeWidth = baseRadius * 0.11f
        val eyeHeight = baseRadius * 0.15f * blink

        // 视线位移限制在合理视界内
        val gazeX = currentGazeX * (baseRadius * 0.045f)
        val gazeY = currentGazeY * (baseRadius * 0.045f)

        // 1. 左右眼
        for (side in listOf(-1, 1)) {
            val eyeX = side * eyeDistance
            if (blink > 0.18f) {
                // 睁眼：黑亮椭圆眼眶
                eyeRect.set(
                    eyeX - eyeWidth, eyeBaseY - eyeHeight,
                    eyeX + eyeWidth, eyeBaseY + eyeHeight,
                )
                canvas.drawOval(eyeRect, eyeOrbitPaint)

                // 眼神高光光斑（随视线微调）
                canvas.drawCircle(
                    eyeX + gazeX + eyeWidth * 0.32f,
                    eyeBaseY + gazeY - eyeHeight * 0.32f,
                    eyeWidth * 0.4f,
                    eyeCatchlightPaint,
                )
            } else {
                // 闭眼：萌系细弧线
                closedEyePath.reset()
                val arcHeight = baseRadius * 0.035f
                closedEyePath.moveTo(eyeX - eyeWidth, eyeBaseY)
                closedEyePath.quadTo(eyeX, eyeBaseY + arcHeight, eyeX + eyeWidth, eyeBaseY)
                canvas.drawPath(closedEyePath, lineDetailPaint)
            }
        }

        // 2. 腮红
        for (side in listOf(-1, 1)) {
            val blushX = side * (eyeDistance + baseRadius * 0.26f)
            val blushY = eyeBaseY + baseRadius * 0.14f
            canvas.drawCircle(blushX, blushY, baseRadius * 0.13f, blushPaint)
        }

        // 3. 微笑小嘴
        mouthPath.reset()
        val mouthHalfW = baseRadius * 0.14f
        val mouthY = eyeBaseY + baseRadius * 0.22f
        mouthPath.moveTo(-mouthHalfW, mouthY)
        mouthPath.quadTo(0f, mouthY + baseRadius * 0.13f, mouthHalfW, mouthY)
        canvas.drawPath(mouthPath, lineDetailPaint)
    }

    /**
     * 触摸交互：视线随手指方向平滑移动，距离中心越远注视越深
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isUserTouching = true
                // 按下瞬间给一个下压冲量：先微微抻一下，随即压到最扁
                springVelocity = -1.4f
                lookAt(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                lookAt(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isUserTouching = false
                // 松手瞬间给一个更大的向上冲量：弹过头再悠回来
                springVelocity = 2.4f
                targetGazeX = 0f
                targetGazeY = 0f
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * 注视触摸点：把触点换算成 -1f ~ 1f 的偏移量。
     */
    private fun lookAt(touchX: Float, touchY: Float) {
        // 设定感应半径范围，计算比例偏移量 (-1f ~ 1f)
        val activeRadius = baseRadius * 2.2f
        targetGazeX = ((touchX - centerX) / activeRadius).coerceIn(-1f, 1f)
        targetGazeY = ((touchY - centerY) / activeRadius).coerceIn(-1f, 1f)
    }
}
