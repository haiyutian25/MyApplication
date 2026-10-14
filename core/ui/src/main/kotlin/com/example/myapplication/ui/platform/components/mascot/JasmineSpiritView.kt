@file:Suppress("MagicNumber") // The petals, wings and face are raw geometry ratios of the radius.

package com.example.myapplication.ui.platform.components.mascot

import android.animation.TimeAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
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

/**
 * Draws the jasmine spirit: it bobs, sways and blinks, and its body squashes and bounces back when
 * it is touched.
 */
@Suppress("TooManyFunctions") // One drawing step per function keeps each frame flat.
class JasmineSpiritView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val animator = TimeAnimator()
    private var totalTime = 0f

    // 几何基础参数
    private var centerX = 0f
    private var centerY = 0f
    private var baseRadius = 0f

    // ================= 弹簧物理引擎参数 (Spring-Damper) =================
    private var isUserTouching = false
    private var springOffset = 0f // 当前形变量
    private var springVelocity = 0f // 速度
    private val springStiffness = 180f // 刚度系数 k
    private val springDamping = 12f // 阻尼系数 c

    // ================= 独立画笔组 (彻底杜绝状态污染) =================
    private val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val leafWingPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val leafVeinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val petalPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val petalRimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val headSkinPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val handPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFF8F5")
    }
    private val handStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#E0D2C7")
    }
    private val stamenStemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val stamenTipPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val eyeOrbitPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val eyeIrisPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val eyeCatchlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    }
    private val blushPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val lineDetailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1B2E22")
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val pollenPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // ================= 预分配几何对象 (零 GC) =================
    private val petalPath = Path()
    private val leafPath = Path()
    private val handPath = Path()
    private val mouthPath = Path()
    private val closedEyePath = Path()
    private val shadowRect = RectF()
    private val eyeRect = RectF()
    private val irisRect = RectF()

    // 视线注视滤波
    private var targetGazeX = 0f
    private var targetGazeY = 0f
    private var currentGazeX = 0f
    private var currentGazeY = 0f

    init {
        animator.setTimeListener { _, _, deltaTime ->
            val dt = (deltaTime / 1000f).coerceIn(0.001f, 0.033f)
            totalTime += dt

            // 1. 二阶弹簧物理阻尼迭代 (Hooke's Law: F = -k*x - c*v)
            val targetSquish = if (isUserTouching) 0.12f else 0f
            val displacement = springOffset - targetSquish
            val springForce = -springStiffness * displacement - springDamping * springVelocity
            springVelocity += springForce * dt
            springOffset += springVelocity * dt

            // 2. 视线控制：若未触摸，则按正弦波在小范围好奇漫游（Idle Saccade）
            if (!isUserTouching) {
                targetGazeX = sin(totalTime * 1.2f) * 0.35f
                targetGazeY = cos(totalTime * 0.9f) * 0.22f
            }
            // 一阶低通滤波跟踪
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
        baseRadius = min(w, h) * 0.26f

        if (baseRadius <= 0f) return

        // 1. 精灵身后的神圣光晕
        auraPaint.shader = RadialGradient(
            0f, 0f, baseRadius * 2.2f,
            intArrayOf(
                Color.parseColor("#4068D391"), // 柔和仙气绿
                Color.parseColor("#1548BB78"),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )

        // 2. 茉莉纯白玉润透光花瓣渐变 (透光绿 -> 乳白 -> 亮白)
        petalPaint.shader = RadialGradient(
            0f, 0f, baseRadius * 0.95f,
            intArrayOf(
                Color.parseColor("#D4EDDA"), // 嫩草水润绿
                Color.parseColor("#F7FCF8"), // 温润玉白
                Color.parseColor("#FFFFFF"), // 纯白外缘
            ),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP,
        )
        petalRimPaint.color = Color.parseColor("#E8F5E9")
        petalRimPaint.strokeWidth = baseRadius * 0.015f

        // 3. 翠叶翅膀鲜绿渐变
        leafWingPaint.shader = LinearGradient(
            0f, 0f, 0f, -baseRadius * 1.1f,
            intArrayOf(
                Color.parseColor("#558B2F"), // 深翠绿基部
                Color.parseColor("#8BC34A"), // 嫩叶青绿
                Color.parseColor("#C5E1A5"), // 迎光透亮黄绿
            ),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP,
        )
        leafWingPaint.alpha = 230

        // 4. 仙子凝脂白肤色 (带微弱自发光感)
        headSkinPaint.shader = RadialGradient(
            -baseRadius * 0.15f, -baseRadius * 0.2f, baseRadius * 0.7f,
            intArrayOf(
                Color.parseColor("#FFFFFF"),
                Color.parseColor("#FFF6F0"),
                Color.parseColor("#F5E6DC"),
            ),
            floatArrayOf(0f, 0.65f, 1f),
            Shader.TileMode.CLAMP,
        )

        // 5. 五官与线条比例自适应
        leafVeinPaint.strokeWidth = baseRadius * 0.018f
        leafVeinPaint.color = Color.parseColor("#DCEDC8")
        stamenStemPaint.strokeWidth = baseRadius * 0.038f
        stamenStemPaint.color = Color.parseColor("#D4E157")
        lineDetailPaint.strokeWidth = baseRadius * 0.038f
        handStrokePaint.strokeWidth = baseRadius * 0.016f

        // 预构建标准化矢量模板
        buildParametricPetal(petalPath, baseRadius * 0.85f, baseRadius * 0.38f)
        buildParametricLeaf(leafPath, baseRadius * 0.85f, baseRadius * 0.32f)
        buildParametricHand(handPath, baseRadius * 0.24f, baseRadius * 0.13f)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (baseRadius <= 0f) return

        /* ================= 1. 周期动力学与形变参数 ================= */
        // (1) 空中悬浮律动
        val hoverY = sin(totalTime * 2.0f) * (baseRadius * 0.08f)
        // (2) 自然微风倾角
        val breezeTilt = sin(totalTime * 1.6f) * 3.2f
        // (3) 果冻弹性形变 (融合自然呼吸 + 触摸弹簧冲击)
        val breathSquish = sin(totalTime * 2.0f + (PI.toFloat() / 2f)) * 0.035f
        val netSquish = breathSquish + springOffset
        val scaleX = 1f + netSquish
        val scaleY = 1f - netSquish

        // (4) 拟人化眨眼方程 (每 4.0 秒快速眨眼)
        val blinkCycle = totalTime % 4.0f
        val blink = if (blinkCycle < 0.15f) {
            val p = blinkCycle / 0.15f
            abs(cos(p * PI.toFloat()))
        } else {
            1f
        }

        /* ================= 2. 空间神圣背景与环绕金色微粒 ================= */
        // A. 仙子背后的柔和环境光晕
        canvas.save()
        canvas.translate(centerX, centerY + hoverY)
        canvas.drawCircle(0f, 0f, baseRadius * 1.6f, auraPaint)
        canvas.restore()

        // B. 金色星屑粒子 (6 个调和正余弦轨道，随呼吸明灭)
        drawFloatingPollenOrbits(canvas, centerX, centerY + hoverY)

        // C. 地面投影 (与高度和挤压度动态联动)
        drawGroundShadow(canvas, hoverY, netSquish)

        /* ================= 3. 精灵本体坐标系 ================= */
        canvas.save()
        canvas.translate(centerX, centerY + hoverY + springOffset * baseRadius * 0.4f)
        canvas.rotate(breezeTilt)
        canvas.scale(scaleX, scaleY)

        // --- Step 1: 背部翠叶羽翼 (带轻柔扑扇相位差) ---
        drawLeafWings(canvas)

        // --- Step 2: 绽放的茉莉花苞长裙 (多层重叠裙摆) ---
        drawFlowerSkirt(canvas)

        // --- Step 3: 颈部嫩绿花萼领结 ---
        drawCalyxCollar(canvas)

        // --- Step 4: 晶莹白皙的仙子头部 ---
        canvas.drawCircle(0f, -baseRadius * 0.06f, baseRadius * 0.52f, headSkinPaint)

        // --- Step 5: 灵动五官 (双层翡翠瞳孔、高光、腮红、微笑) ---
        drawFacialFeatures(canvas, blink)

        // --- Step 6: 萌态叶芽小手 (双手微抱于身前) ---
        drawBudHands(canvas)

        // --- Step 7: 头顶五瓣茉莉花冠与金色弹簧花蕊 ---
        drawFloralCrown(canvas)

        canvas.restore()
    }

    /**
     * 环绕金色星屑 (利萨如参数化谐波轨道)
     */
    private fun drawFloatingPollenOrbits(canvas: Canvas, ox: Float, oy: Float) {
        val pollenCount = 7
        for (i in 0 until pollenCount) {
            val phase = i * (PI.toFloat() * 2f / pollenCount)
            val speed = 0.9f + i * 0.18f

            val px = ox + cos(totalTime * speed + phase) *
                (baseRadius * (1.15f + 0.12f * sin(totalTime + i)))
            val py = oy + sin(totalTime * (speed * 0.85f) + phase) * (baseRadius * 0.82f)

            val pAlpha = ((sin(totalTime * 3.2f + phase) * 0.5f + 0.5f) * 190).toInt() + 50
            val pRadius = (baseRadius * 0.038f) * (0.85f + 0.35f * sin(totalTime * 2.2f + i))

            // 外层金光光晕
            pollenPaint.color = Color.parseColor("#FFD54F")
            pollenPaint.alpha = (pAlpha * 0.5f).toInt()
            canvas.drawCircle(px, py, pRadius * 1.8f, pollenPaint)

            // 核心高亮白金微粒
            pollenPaint.color = Color.parseColor("#FFF9C4")
            pollenPaint.alpha = pAlpha
            canvas.drawCircle(px, py, pRadius, pollenPaint)
            pollenPaint.color = Color.WHITE
            canvas.drawCircle(px, py, pRadius * 0.4f, pollenPaint)
        }
    }

    /**
     * 随高度及捏挤状态联动的地面阴影
     */
    private fun drawGroundShadow(canvas: Canvas, hoverY: Float, netSquish: Float) {
        val elevationRatio = (hoverY / (baseRadius * 0.08f))
        val shadowScale = (1f - elevationRatio * 0.12f + netSquish * 0.4f).coerceIn(0.6f, 1.4f)
        val shadowAlpha = ((0.24f - elevationRatio * 0.06f) * 255).toInt().coerceIn(20, 90)

        shadowPaint.color = Color.parseColor("#0C2016")
        shadowPaint.alpha = shadowAlpha

        val shadowY = centerY + baseRadius * 1.48f
        shadowRect.set(
            centerX - baseRadius * 0.82f * shadowScale,
            shadowY - baseRadius * 0.13f * shadowScale,
            centerX + baseRadius * 0.82f * shadowScale,
            shadowY + baseRadius * 0.13f * shadowScale,
        )
        canvas.drawOval(shadowRect, shadowPaint)
    }

    /**
     * 背部翠叶羽翼扑动
     */
    private fun drawLeafWings(canvas: Canvas) {
        val flapAngle = sin(totalTime * 3.6f) * 12f

        for (side in listOf(-1, 1)) {
            canvas.save()
            canvas.translate(side * baseRadius * 0.30f, -baseRadius * 0.12f)
            canvas.rotate(side * 48f + side * flapAngle)
            canvas.scale(side.toFloat(), 1f)

            // 叶面与透光外边
            canvas.drawPath(leafPath, leafWingPaint)
            canvas.drawPath(leafPath, petalRimPaint)

            // 叶脉主干与侧脉
            canvas.drawLine(0f, 0f, 0f, -baseRadius * 0.76f, leafVeinPaint)
            canvas.drawLine(
                0f,
                -baseRadius * 0.22f,
                baseRadius * 0.12f,
                -baseRadius * 0.38f,
                leafVeinPaint,
            )
            canvas.drawLine(
                0f,
                -baseRadius * 0.42f,
                -baseRadius * 0.12f,
                -baseRadius * 0.58f,
                leafVeinPaint,
            )

            canvas.restore()
        }
    }

    /**
     * 绽放的茉莉花裙 (前后两层交错交叠)
     */
    private fun drawFlowerSkirt(canvas: Canvas) {
        val skirtBreeze = sin(totalTime * 2.6f) * 3.5f

        canvas.save()
        canvas.translate(0f, baseRadius * 0.32f)

        // 后层花瓣 (深邃衬托)
        for (ang in listOf(-48f, 48f)) {
            canvas.save()
            canvas.rotate(180f + ang + skirtBreeze * 0.5f)
            canvas.scale(0.88f, 0.88f)
            canvas.drawPath(petalPath, petalPaint)
            canvas.drawPath(petalPath, petalRimPaint)
            canvas.restore()
        }

        // 前层饱满花瓣 (微风拂动)
        val frontAngles = floatArrayOf(-24f, 0f, 24f)
        for (i in frontAngles.indices) {
            val ang = frontAngles[i]
            canvas.save()
            canvas.rotate(180f + ang + skirtBreeze * (if (i == 1) 0.3f else 1.0f))
            canvas.drawPath(petalPath, petalPaint)
            canvas.drawPath(petalPath, petalRimPaint)
            canvas.restore()
        }

        canvas.restore()
    }

    /**
     * 颈部小花萼领结 (过渡头部与花裙)
     */
    private fun drawCalyxCollar(canvas: Canvas) {
        canvas.save()
        canvas.translate(0f, baseRadius * 0.28f)
        for (ang in listOf(-32f, 0f, 32f)) {
            canvas.save()
            canvas.rotate(180f + ang)
            canvas.scale(0.38f, 0.38f)
            canvas.drawPath(leafPath, leafWingPaint)
            canvas.restore()
        }
        canvas.restore()
    }

    /**
     * 萌态小手 (叶芽萌爪)
     */
    private fun drawBudHands(canvas: Canvas) {
        val handSway = sin(totalTime * 2.8f) * (baseRadius * 0.02f)
        for (side in listOf(-1, 1)) {
            canvas.save()
            canvas.translate(side * baseRadius * 0.26f, baseRadius * 0.18f + handSway)
            canvas.rotate(-side * 24f)
            canvas.scale(side.toFloat(), 1f)

            canvas.drawPath(handPath, handPaint)
            canvas.drawPath(handPath, handStrokePaint)
            canvas.restore()
        }
    }

    /**
     * 头顶花冠与金色触角花蕊
     */
    private fun drawFloralCrown(canvas: Canvas) {
        val crownY = -baseRadius * 0.50f
        canvas.save()
        canvas.translate(0f, crownY)

        // 1. 五瓣绽开的茉莉花冠
        val crownAngles = floatArrayOf(-64f, -32f, 0f, 32f, 64f)
        val bloomWobble = sin(totalTime * 2.2f) * 2.5f
        for (ang in crownAngles) {
            canvas.save()
            canvas.rotate(ang + bloomWobble)
            val scale = if (abs(ang) > 40f) 0.68f else 0.78f
            canvas.scale(scale, scale)
            canvas.drawPath(petalPath, petalPaint)
            canvas.drawPath(petalPath, petalRimPaint)
            canvas.restore()
        }

        // 2. 灵动金色花蕊触角 (物理弹簧晃动)
        val stamenSway = sin(totalTime * 3.6f) * (baseRadius * 0.05f) +
            springOffset * baseRadius * 0.2f
        val stamenAngles = floatArrayOf(-20f, 0f, 20f)
        for (ang in stamenAngles) {
            canvas.save()
            canvas.rotate(ang)
            val tipX = stamenSway
            val tipY = -baseRadius * 0.58f

            closedEyePath.reset()
            closedEyePath.moveTo(0f, baseRadius * 0.05f)
            closedEyePath.quadTo(stamenSway * 0.4f, -baseRadius * 0.28f, tipX, tipY)
            canvas.drawPath(closedEyePath, stamenStemPaint)

            // 发光花粉珍珠头
            stamenTipPaint.color = Color.parseColor("#FFF59D")
            canvas.drawCircle(tipX, tipY, baseRadius * 0.065f, stamenTipPaint)
            stamenTipPaint.color = Color.parseColor("#FFA000")
            canvas.drawCircle(tipX, tipY, baseRadius * 0.045f, stamenTipPaint)
            stamenTipPaint.color = Color.WHITE
            canvas.drawCircle(tipX, tipY, baseRadius * 0.02f, stamenTipPaint)

            canvas.restore()
        }

        canvas.restore()
    }

    /**
     * 面部五官 (深邃翡翠眼瞳、多焦高光、腮红、微笑)
     */
    @Suppress("LongMethod") // The eyes, the blush and the mouth are drawn in one pass.
    private fun drawFacialFeatures(canvas: Canvas, blink: Float) {
        val eyeDistance = baseRadius * 0.23f
        val eyeY = -baseRadius * 0.08f
        val eyeW = baseRadius * 0.10f
        val eyeH = baseRadius * 0.145f * blink

        val gazeX = currentGazeX * (baseRadius * 0.035f)
        val gazeY = currentGazeY * (baseRadius * 0.035f)

        // 1. 左右大眼
        for (side in listOf(-1, 1)) {
            val ex = side * eyeDistance
            if (blink > 0.18f) {
                // 外眼眶墨色渐变
                eyeRect.set(ex - eyeW, eyeY - eyeH, ex + eyeW, eyeY + eyeH)
                eyeOrbitPaint.shader = LinearGradient(
                    ex, eyeY - eyeH, ex, eyeY + eyeH,
                    Color.parseColor("#0C2016"),
                    Color.parseColor("#1B432C"),
                    Shader.TileMode.CLAMP,
                )
                canvas.drawOval(eyeRect, eyeOrbitPaint)

                // 翡翠色下虹膜新月光晕
                irisRect.set(
                    ex - eyeW * 0.8f,
                    eyeY - eyeH * 0.1f,
                    ex + eyeW * 0.8f,
                    eyeY + eyeH * 0.9f,
                )
                eyeIrisPaint.shader = RadialGradient(
                    ex, eyeY + eyeH * 0.5f, eyeW * 0.8f,
                    Color.parseColor("#66BB6A"),
                    Color.TRANSPARENT,
                    Shader.TileMode.CLAMP,
                )
                canvas.drawOval(irisRect, eyeIrisPaint)

                // 眼神上睫毛轻细阴影线
                lineDetailPaint.strokeWidth = baseRadius * 0.022f
                canvas.drawLine(
                    ex - eyeW * 0.9f,
                    eyeY - eyeH * 0.4f,
                    ex + eyeW * 0.9f,
                    eyeY - eyeH * 0.4f,
                    lineDetailPaint,
                )

                // 主焦高光斑点 (水汪汪的反射)
                canvas.drawCircle(
                    ex + gazeX + eyeW * 0.28f,
                    eyeY + gazeY - eyeH * 0.32f,
                    eyeW * 0.38f,
                    eyeCatchlightPaint,
                )
                // 副高光
                canvas.drawCircle(
                    ex + gazeX - eyeW * 0.26f,
                    eyeY + gazeY + eyeH * 0.28f,
                    eyeW * 0.18f,
                    eyeCatchlightPaint,
                )
            } else {
                // 闭眼治愈细线
                closedEyePath.reset()
                closedEyePath.moveTo(ex - eyeW, eyeY)
                closedEyePath.quadTo(ex, eyeY + baseRadius * 0.028f, ex + eyeW, eyeY)
                lineDetailPaint.strokeWidth = baseRadius * 0.035f
                canvas.drawPath(closedEyePath, lineDetailPaint)
            }
        }

        // 2. 浅桃粉径向羽化腮红
        for (side in listOf(-1, 1)) {
            val bx = side * (eyeDistance + baseRadius * 0.16f)
            val by = eyeY + baseRadius * 0.11f
            blushPaint.shader = RadialGradient(
                bx, by, baseRadius * 0.11f,
                Color.parseColor("#80FF8A80"),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(bx, by, baseRadius * 0.11f, blushPaint)
        }

        // 3. 微笑小嘴
        mouthPath.reset()
        val mouthW = baseRadius * 0.075f
        val mouthY = eyeY + baseRadius * 0.16f
        mouthPath.moveTo(-mouthW, mouthY)
        mouthPath.quadTo(0f, mouthY + baseRadius * 0.065f, mouthW, mouthY)
        lineDetailPaint.strokeWidth = baseRadius * 0.036f
        canvas.drawPath(mouthPath, lineDetailPaint)
    }

    /**
     * 参数化平滑花瓣曲线
     */
    private fun buildParametricPetal(path: Path, length: Float, width: Float) {
        path.reset()
        path.moveTo(0f, 0f)
        path.cubicTo(-width * 0.95f, -length * 0.32f, -width * 0.70f, -length * 0.85f, 0f, -length)
        path.cubicTo(width * 0.70f, -length * 0.85f, width * 0.95f, -length * 0.32f, 0f, 0f)
        path.close()
    }

    /**
     * 参数化修长叶片曲线
     */
    private fun buildParametricLeaf(path: Path, length: Float, width: Float) {
        path.reset()
        path.moveTo(0f, 0f)
        path.cubicTo(-width, -length * 0.38f, -width * 0.45f, -length * 0.85f, 0f, -length)
        path.cubicTo(width * 0.45f, -length * 0.85f, width, -length * 0.38f, 0f, 0f)
        path.close()
    }

    /**
     * 参数化叶芽萌爪曲线
     */
    private fun buildParametricHand(path: Path, length: Float, width: Float) {
        path.reset()
        path.moveTo(0f, 0f)
        path.cubicTo(width * 1.1f, length * 0.25f, width * 0.8f, length * 0.8f, 0f, length)
        path.cubicTo(-width * 0.5f, length * 0.8f, -width * 0.5f, length * 0.25f, 0f, 0f)
        path.close()
    }

    /**
     * 触摸交互与物理冲击注入
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isUserTouching = true
                // 触摸瞬间给予下压冲量速度
                springVelocity = -0.8f
                updateGazeTarget(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                updateGazeTarget(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isUserTouching = false
                // 松开手时给予向上的回弹冲量
                springVelocity = 1.2f
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateGazeTarget(touchX: Float, touchY: Float) {
        val dx = touchX - centerX
        val dy = touchY - centerY
        val maxDist = baseRadius * 2.2f
        targetGazeX = (dx / maxDist).coerceIn(-1f, 1f)
        targetGazeY = (dy / maxDist).coerceIn(-1f, 1f)
    }
}
