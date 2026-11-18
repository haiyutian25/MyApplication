// The reel is copied from another app's source: its choreography is expressed as raw pixel ratios
// and seconds, so the file level magic number rule is waived for it.
@file:Suppress("MagicNumber", "DEPRECATION")

package com.example.myapplication.feature.settings.impl

import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * How many particles the field is made of.
 */
private const val PARTICLE_COUNT = 3600

/**
 * How much the word is scaled down before its ink is sampled: the mask only has to be dense enough
 * to hand every particle a target point of its own.
 */
private const val INK_SCALE = 2.5f

/**
 * The alpha a pixel of the sampled word needs to count as ink.
 */
private const val INK_ALPHA_THRESHOLD = 140

/**
 * The seed of the particle scatter, so that every run of the reel is identical.
 */
private const val SCATTER_SEED = 26

/**
 * The fraction of the screen height the word is centred on.
 */
private const val TITLE_CENTER_FRACTION = 0.44f

/**
 * The number of colour buckets the trails are batched in.
 */
private const val BUCKET_COUNT = 4

/**
 * The number of floats one trail occupies in the buffer of its bucket.
 */
private const val LINE_STRIDE = 4

/**
 * The alpha the trails are drawn with before the scene fades them.
 */
private const val TRAIL_ALPHA = 235

/**
 * The width of a trail in hundredths of the screen width.
 */
private const val TRAIL_WIDTH = 0.42f

/**
 * The ink of a word, as points packed into `Long`s: the x coordinate in the high half and the y
 * coordinate in the low half, both in the pixels of the downsampled mask.
 */
private typealias InkPoints = MutableList<Long>

/**
 * The colours of the particle reel, taken from the app theme.
 *
 * @property background The colour the closing iris paints the frame with.
 * @property foreground The colour of the solid word and of the majority of the particles.
 * @property accent The primary accent: the galaxy glow, the second trail colour, the iris ring and
 * the sweep across the solid word.
 * @property second The third trail colour.
 * @property third The fourth trail colour.
 * @property additive Whether the trails are blended additively, which only reads on a dark stage.
 */
internal class ParticlePalette(
    val background: Color,
    val foreground: Color,
    val accent: Color,
    val second: Color,
    val third: Color,
    val additive: Boolean,
)

/**
 * The particle field of the word: 3,600 particles that orbit as a three-armed galaxy, fly into the
 * ink of the word, inhale, and detonate when the solid word lands.
 *
 * Every position is a closed-form function of time, so the field can be scrubbed, frozen and
 * looped without any simulation state, and nothing is allocated per frame: the trails of each
 * colour are batched into one buffer that is handed to a single `drawLines` call.
 */
internal class ParticleField(private val palette: ParticlePalette) {
    private val count = PARTICLE_COUNT
    private val arm = FloatArray(count)
    private val radius = FloatArray(count)
    private val delay = FloatArray(count)
    private val targetX = FloatArray(count)
    private val targetY = FloatArray(count)
    private val escapeX = FloatArray(count)
    private val escapeY = FloatArray(count)
    private val wobble = FloatArray(count)
    private val bucket = IntArray(count)

    private val trails = Array(BUCKET_COUNT) { FloatArray(count * LINE_STRIDE) }
    private val trailCounts = IntArray(BUCKET_COUNT)
    private val colors = intArrayOf(
        palette.foreground.toArgb(),
        palette.accent.toArgb(),
        palette.second.toArgb(),
        palette.third.toArgb(),
    )
    private val widths = floatArrayOf(1f, 1.25f, 1f, 0.8f)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
        if (palette.additive) {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
        }
    }

    private var fieldWidth = 0f
    private var fieldHeight = 0f
    private var textCenterX = 0f
    private var textCenterY = 0f

    private var cursorX = 0f
    private var cursorY = 0f

    /**
     * Scatters the particles over the ink of [layout] for a field of [width] x [height] pixels.
     *
     * The work is only done when the size of the field changes: the screen calls this on every
     * recomposition, and the ink mask is the expensive part of the preparation.
     */
    fun prepare(width: Float, height: Float, layout: TextLayoutResult, density: Density) {
        if (width == fieldWidth && height == fieldHeight) return
        fieldWidth = width
        fieldHeight = height

        val origin = textOrigin(Size(width, height), layout)
        textCenterX = origin.x + layout.size.width / 2f
        textCenterY = origin.y + layout.size.height / 2f
        // The mask is scanned row by row, so its points have to be shuffled before they are handed
        // out: the first particles would otherwise all land in the top rows of the word and the
        // bottom of it would never be built.
        val ink = rasteriseInk(layout, density)
        val random = Random(SCATTER_SEED)
        ink.shuffle(random)

        for (i in 0 until count) {
            scatterParticle(i = i, ink = ink, origin = origin, random = random)
            val discRadius = sqrt(random.nextFloat()) * 0.95f + 0.05f
            radius[i] = discRadius
            arm[i] = (i % 3) * (2f * PI.toFloat() / 3f) + discRadius * 4.2f +
                (random.nextFloat() - 0.5f) * (0.9f - discRadius * 0.5f)
            delay[i] = random.nextFloat() * 0.35f + (1f - discRadius) * 0.15f
            wobble[i] = random.nextFloat() * 100f

            val escapeAngle = random.nextFloat() * 2f * PI.toFloat()
            val dx = targetX[i] - textCenterX
            val dy = targetY[i] - textCenterY
            val length = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
            val speed = 0.35f + random.nextFloat() * random.nextFloat() * 1.4f
            escapeX[i] = (dx / length * 0.7f + cos(escapeAngle) * 0.6f) * speed
            escapeY[i] = (dy / length * 0.7f + sin(escapeAngle) * 0.6f) * speed

            val shade = random.nextFloat()
            bucket[i] = when {
                shade < 0.5f -> 0
                shade < 0.74f -> 1
                shade < 0.9f -> 2
                else -> 3
            }
        }
    }

    /**
     * Draws the trails of the particles at [time] seconds into the reel, batched by colour.
     */
    fun draw(scope: DrawScope, time: Float, unit: Float) {
        val alpha = progress(time, 0f, 0.25f) * (1f - progress(time, 3.3f, PARTICLES_END))
        if (alpha <= 0f) return

        trailCounts.fill(0)
        // The streak stretches with speed: longer trails during the burst and the detonation.
        val trail = 0.028f + 0.02f * (1f - progress(time, 0f, 0.6f)) +
            0.03f * progress(time, IMPACT_AT, IMPACT_AT + 0.1f)

        for (i in 0 until count) {
            position(i, time - trail)
            val fromX = cursorX
            val fromY = cursorY
            position(i, time)
            val index = bucket[i]
            val buffer = trails[index]
            val used = trailCounts[index]
            buffer[used] = fromX
            buffer[used + 1] = fromY
            buffer[used + 2] = cursorX
            buffer[used + 3] = cursorY
            trailCounts[index] = used + LINE_STRIDE
        }

        scope.drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            for (index in colors.indices) {
                val used = trailCounts[index]
                if (used == 0) continue
                paint.color = colors[index]
                paint.alpha = (alpha * TRAIL_ALPHA).toInt()
                paint.strokeWidth = unit * TRAIL_WIDTH * widths[index]
                native.drawLines(trails[index], 0, used, paint)
            }
        }
    }

    /**
     * Gives particle [i] its target point on the ink of the word, scattering any particle beyond
     * the number of ink pixels around one of them so that the overshoot does not stack up.
     */
    private fun scatterParticle(i: Int, ink: InkPoints, origin: Offset, random: Random) {
        if (ink.isEmpty()) {
            targetX[i] = textCenterX
            targetY[i] = textCenterY
            return
        }
        val point = ink[i % ink.size]
        val jitter = if (i >= ink.size) INK_SCALE * 0.5f else 0f
        targetX[i] = origin.x + (point ushr 32).toFloat() * INK_SCALE +
            random.nextFloat() * INK_SCALE + (random.nextFloat() - 0.5f) * jitter
        targetY[i] = origin.y + (point and 0xFFFFFFFFL).toFloat() * INK_SCALE +
            random.nextFloat() * INK_SCALE
    }

    /**
     * The analytic position of particle [i] at [time] seconds into the reel.
     *
     * The result is written to [cursorX] and [cursorY] rather than returned, so that the thousands
     * of positions computed per frame do not allocate.
     */
    private fun position(i: Int, time: Float) {
        val centerX = fieldWidth / 2f
        val centerY = fieldHeight / 2f
        val discRadius = radius[i]
        val burst = outExpo(progress(time, 0f, 0.9f))
        val orbitRadius = discRadius * fieldWidth * 0.47f * burst *
            (1f + 0.04f * sin(time * 3f + wobble[i]))
        val angle = arm[i] + time * (1.1f / (0.22f + discRadius)) + (1f - burst) * 3f
        val galaxyX = centerX + orbitRadius * cos(angle)
        val galaxyY = centerY + orbitRadius * sin(angle) * 0.92f

        // The flight into the word follows an arc rather than a straight line.
        val gathered = inOutCubic(
            progress(time, ASSEMBLY_START + delay[i], ASSEMBLY_END + delay[i]),
        )
        val dx = targetX[i] - galaxyX
        val dy = targetY[i] - galaxyY
        val arc = sin(gathered * PI.toFloat()) * 0.35f
        var x = galaxyX + dx * gathered - dy * arc
        var y = galaxyY + dy * gathered + dx * arc

        // Anticipation: the formed word inhales before it blows apart.
        val inhale = inCubic(progress(time, INHALE_START, IMPACT_AT)) * 0.07f
        x += (textCenterX - x) * inhale + sin(time * 40f + wobble[i]) * fieldWidth * 0.0015f *
            gathered
        y += (textCenterY - y) * inhale

        val burstAge = time - IMPACT_AT
        if (burstAge > 0f) {
            val outward = 1f - exp(-burstAge * 3.2f)
            x += escapeX[i] * outward * fieldWidth * 0.9f
            y += escapeY[i] * outward * fieldWidth * 0.9f + burstAge * burstAge * fieldWidth * 0.05f
        }
        cursorX = x
        cursorY = y
    }
}

/**
 * The top left corner of the word on a stage of [size] pixels, so that the word is centred
 * horizontally and sits on the fraction of the height the reel gives it.
 */
internal fun textOrigin(size: Size, layout: TextLayoutResult): Offset = Offset(
    x = (size.width - layout.size.width) / 2f,
    y = size.height * TITLE_CENTER_FRACTION - layout.size.height / 2f,
)

/**
 * Draws [layout] on an offscreen mask and returns the points of it that its ink covers.
 *
 * The mask is drawn at [INK_SCALE] times smaller than the word on screen: sampling the full-size
 * word would cost several times the memory for targets that are within a couple of pixels of each
 * other anyway.
 */
private fun rasteriseInk(layout: TextLayoutResult, density: Density): InkPoints {
    val maskWidth = ceil(layout.size.width / INK_SCALE).toInt().coerceAtLeast(1)
    val maskHeight = ceil(layout.size.height / INK_SCALE).toInt().coerceAtLeast(1)
    val mask = ImageBitmap(maskWidth, maskHeight)
    val canvas = Canvas(mask)

    CanvasDrawScope().draw(
        density = density,
        layoutDirection = LayoutDirection.Ltr,
        canvas = canvas,
        size = Size(maskWidth.toFloat(), maskHeight.toFloat()),
    ) {
        scale(scaleX = 1f / INK_SCALE, scaleY = 1f / INK_SCALE, pivot = Offset.Zero) {
            drawText(layout, color = Color.White, topLeft = Offset.Zero)
        }
    }

    val pixels = IntArray(maskWidth * maskHeight)
    mask.readPixels(pixels)
    val ink = ArrayList<Long>(PARTICLE_COUNT)
    for (y in 0 until maskHeight) {
        val row = y * maskWidth
        for (x in 0 until maskWidth) {
            val alpha = (pixels[row + x] ushr 24) and 0xFF
            if (alpha > INK_ALPHA_THRESHOLD) ink += (x.toLong() shl 32) or y.toLong()
        }
    }
    return ink
}
