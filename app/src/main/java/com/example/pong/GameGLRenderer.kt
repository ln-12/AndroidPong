package com.example.pong

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import android.os.Vibrator
import android.preference.PreferenceManager
import android.util.Log
import android.view.MotionEvent
import android.widget.TextView
import com.example.pong.geometry.*
import com.example.pong.helper.Collision
import com.example.pong.helper.GLHelper
import com.example.pong.helper.PowerUpKind
import com.example.pong.helper.Vector
import com.example.pong.ui.EndGameDialogFragment
import java.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.collections.ArrayList
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.thread


/**
 * Created by lneumann on 01.08.17.
 */

class GameGLRenderer (private val context: Context, private val gameMode : String) : GLSurfaceView.Renderer {
    // toggle if we need to pause the game
    private var isRunning = false

    // the shapes
    private var background : Rectangle? = null
    private var centerLine : Rectangle? = null
    private var upperPaddle : Rectangle? = null
    private var lowerPaddle : Rectangle? = null
    private var lightPolygons = ArrayList<LightPolygon>()
    private var allBalls = ArrayList<Circle>()

    // playground corners
    private var v1 = floatArrayOf(-1.0f, -1.0f)
    private var v2 = floatArrayOf(-1.0f, 1.0f)
    private var v3 = floatArrayOf(1.0f, 1.0f)
    private var v4 = floatArrayOf(1.0f, -1.0f)

    private var lightPosition = Array(10){Vector(0.0f, 0.0f, 0.0f)}

    // some powerUp specific stuff
    // list of all powerUps
    private var powerUps = ArrayList<PowerUp>()
    // toggle if controls & colors should be inverted
    private var invertControls = 1.0f
    private var blockUpper = false
    private var blockLower = false

    // fancy particles which follow our ball
    private var particles = ArrayList<Particle>()
    private val particleLife = 0.4f

    // view to display our current fps
    //private val fpsTextView: TextView = (context as Activity).findViewById(R.id.fpsTextView)
    //private val fpsCounter: FpsCounter = FpsCounter()

    // score views
    private val lowerPlayerTextView: TextView = (context as Activity).findViewById(R.id.lowerPlayerScoreTextView)
    private val upperPlayerTextView: TextView = (context as Activity).findViewById(R.id.upperPlayerScoreTextView)
    private var lowerPlayerScore : Int = 0
    private var upperPlayerScore : Int = 0

    // properties of our screen
    private var width: Int = 0
    private var height: Int = 0
    private var ratio : Float = 0.0f

    // old coordinates after touch event occurred
    private var previousXUpperPaddle: Float = 0.0f
    private var previousXLowerPaddle: Float = 0.0f

    // OpenGL related matrices
    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)

    // timer which calls updateGameData function -> move ball, check collision, etc.
    private var updateGameDataTimer: Timer? = null

    // locks for thread safety
    private val particleLock = Any()
    private val powerUpLock = Any()
    private val taskLock = Any()
    private val allBallsLock = Any()

    private var textures: HashMap<PowerUpKind, Pair<Int, Int>> = HashMap()

    private val glTasks: ArrayList<() -> Unit> = ArrayList()

    // the used .vibrate method is deprecated but the new version is only available for SDK 26+
    // so we still use it
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    // the settings
    private var vibrationIsEnabled = true
    private var soundIsEnabled = true
    private var scoreToWin = 5
    private var enablePowerUps = true
    private var showParticles = true
    private var showShadows = true


    // value for the speed of the AI controlled paddle
    private var maxPaddleSpeed = 0.009f

    private var paddleShrinkFactor = 0.999f

    // background music
    private val backgroundMusic = MediaPlayer.create(context, R.raw.backgroundmusic) // https://www.youtube.com/watch?v=6uNjkzoZqME

    // sound effects
    private val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
            .setUsage(AudioAttributes.USAGE_GAME)
            .build()

    private var soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(this.audioAttributes)
            .build()

    private var powerUpSoundID = 0
    private var collisionID = 0
    private var victorySoundID = 0
    private var loseSoundID = 0

    init{
        // read in the stored settings
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        this.vibrationIsEnabled = sharedPref.getBoolean("vibration", true)
        this.soundIsEnabled = sharedPref.getBoolean("sound", true)
        this.scoreToWin = sharedPref.getString("score_to_win", "5").toInt()
        this.enablePowerUps = sharedPref.getBoolean("powerUps", true)
        this.showParticles = sharedPref.getBoolean("show_particles", true)
        this.showShadows = sharedPref.getBoolean("show_shadows", true)

        val difficulty = sharedPref.getString("single_player_difficulty", "1").toInt()

        when(difficulty){
            0 -> {
                this.maxPaddleSpeed = 0.007f
                this.paddleShrinkFactor = 0.9999f
            }
            2 -> {
                this.maxPaddleSpeed = 0.02f
                this.paddleShrinkFactor = 0.9995f
            }
            else -> {
                this.maxPaddleSpeed = 0.0095f
                this.paddleShrinkFactor = 0.999f
            }
        }

        // load up some fancy sounds
        // https://freesound.org/people/LittleRobotSoundFactory/packs/16681/
        this.powerUpSoundID = this.soundPool.load(context, R.raw.powerup, 0)
        this.collisionID = this.soundPool.load(context, R.raw.collision, 0)
        this.victorySoundID = this.soundPool.load(context, R.raw.victory, 0)
        this.loseSoundID = this.soundPool.load(context, R.raw.lose, 0)
    }

    companion object {
        // print more or less meaningful error
        fun checkGLError(tag: String, msg: String) {
            var error = GLES30.glGetError()
            while (error != GLES30.GL_NO_ERROR) {
                var errorStr = "unknown"
                when (error) {
                    GLES30.GL_INVALID_ENUM -> errorStr = "INVALID_ENUM"
                    GLES30.GL_INVALID_VALUE -> errorStr = "INVALID_VALUE"
                    GLES30.GL_INVALID_OPERATION -> errorStr = "INVALID_OPERATION"
                    GLES30.GL_INVALID_FRAMEBUFFER_OPERATION -> errorStr = "INVALID_FRAMEBUFFER_OPERATION"
                    GLES30.GL_OUT_OF_MEMORY -> errorStr = "OUT_OF_MEMORY"
                }
                Log.e(tag, "$msg (" + error.toString() + "): $errorStr")
                error = GLES30.glGetError()
            }
        }
    }

    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        // Set the background frame color
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // add all needed shaders to our helper object
        GLHelper.allShaders.put(Pair(R.raw.fragment_default, R.raw.vertex_default), GLHelper.ProgramHandles())
        GLHelper.allShaders.put(Pair(R.raw.fragment_light, R.raw.vertex_light), GLHelper.ProgramHandles())
        GLHelper.allShaders.put(Pair(R.raw.fragment_stripe, R.raw.vertex_stripe), GLHelper.ProgramHandles())
        GLHelper.allShaders.put(Pair(R.raw.fragment_texture, R.raw.vertex_texture), GLHelper.ProgramHandles())

        // compile all shaders
        GLHelper.loadAllShaders(this.context)

        if(this.soundIsEnabled) {
            // start the background music
            this.backgroundMusic.isLooping = true
            this.backgroundMusic.start()
        }

        // create shape objects
        this.upperPaddle = Rectangle(context, 0.0f, 0.0f, 0.25f, 0.025f, false)
        this.lowerPaddle = Rectangle(context, 0.0f, 0.0f, 0.25f, 0.025f, false)
        this.background = Rectangle(context, 0.0f, 0.0f, 2.0f, 2.0f, false)
        this.centerLine = Rectangle(context, 0.0f, 0.0f, 2.0f, 0.01f, true)

        synchronized(allBallsLock) {
            this.allBalls.add(Circle(context, 0.0f, 0.0f, 0.065f))
        }

        // set their colors
        this.background?.color = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)
        this.centerLine?.color = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)
        this.upperPaddle?.color = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)
        this.lowerPaddle?.color = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)

        this.loadTextures()

        // set up ball and paddles
        resetBalls()
        resetPaddles()
    }

    override fun onDrawFrame(unused: GL10) {
        // do some OpenGL specific stuff which needs the right context
        synchronized(this.taskLock) {
            this.glTasks.forEach {
                it.invoke()
            }

            this.glTasks.clear()
        }

        if(this.showShadows) {
            calcLightPolygons()
        }

        // Redraw background color
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        checkGLError("onDrawFrame", "glClear")

        // Set the camera position (View matrix)
        Matrix.setLookAtM(this.viewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)

        // Calculate the projection and view transformation
        Matrix.multiplyMM(this.mvpMatrix, 0, this.projectionMatrix, 0, this.viewMatrix, 0)

        setLightPosition()

        // draw all game elements
        this.background?.draw(this.mvpMatrix, this.invertControls != 1.0f, lightPosition, allBalls.count())

        // only draw the lightPolygons when Shadows are enabled
        if (this.showShadows){
            this.lightPolygons.forEach { it.draw(this.mvpMatrix, this.invertControls != 1.0f, lightPosition, allBalls.count()) }
        }

        this.centerLine?.draw(this.mvpMatrix, this.invertControls != 1.0f, lightPosition, allBalls.count())

        synchronized(particleLock) {
            for (particle in this.particles) {
                particle.draw(this.mvpMatrix, this.invertControls != 1.0f, lightPosition, allBalls.count())
            }
        }

        synchronized(powerUpLock) {
            // Create a rotation for the triangle
            val time = SystemClock.uptimeMillis() % 8000L
            val rotation = 0.045f * time.toInt()

            for (powerUp in this.powerUps) {
                powerUp.rotate(rotation)
                powerUp.draw(this.mvpMatrix, this.invertControls != 1.0f, lightPosition, allBalls.count())
            }
        }

        synchronized(allBallsLock){
            this.allBalls.forEach {
                it.draw(mvpMatrix, this.invertControls != 1.0f, lightPosition, allBalls.count())
            }
        }

        this.upperPaddle?.draw(mvpMatrix, this.invertControls != 1.0f, lightPosition, allBalls.count())
        this.lowerPaddle?.draw(mvpMatrix, this.invertControls != 1.0f, lightPosition, allBalls.count())

        if(allBalls.count() > 1){
            setLightPosition()
        }

        // show fps
        //(context as Activity).runOnUiThread { fpsTextView.text = String.format("%.2f", fpsCounter.frameRate) }
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        checkGLError("onSurfaceChanged","glViewport")

        // calculate projection matrix to fit our shapes according to the screen
        ratio = width.toFloat() / height.toFloat()

        this.width = width
        this.height = height

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        //              matrix, offset, left, right, bottom, top, near, far
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
    }

    // react to touches on the screen
    fun handleUserInput(e: MotionEvent): Boolean {
        // iterate through all touches that occurred
        for (i in 0 until e.pointerCount) {
            // get the coordinates
            val x : Float = e.getX(i)
            val y : Float = e.getY(i)

            // decide which half of the screen was touched
            if(y < this.height / 2.0f){
                // get the difference
                val dx = x - previousXUpperPaddle

                // only if we moved we want to update the paddle position to avoid "jumping" behaviour
                if(e.action == MotionEvent.ACTION_MOVE && !this.blockUpper) {
                    this.upperPaddle?.moveBy(invertControls * dx / (this.width.toFloat() / 2.0f) * ratio,
                            0.0f, 0.0f)
                }

                // update the position for the next time
                previousXUpperPaddle = x
            }else{
                // same for the lower paddle
                val dx = x - previousXLowerPaddle

                if(e.action == MotionEvent.ACTION_MOVE && !this.blockLower) {
                    this.lowerPaddle?.moveBy(invertControls * dx / (this.width.toFloat() / 2.0f) * ratio,
                            0.0f, 0.0f)
                }

                previousXLowerPaddle = x
            }

            if(e.action == MotionEvent.ACTION_DOWN && !this.isRunning){
                //println("##### TOUCH DOWN #####")


                this.isRunning = true

                // check collisions, move ball
                this.updateGameDataTimer = fixedRateTimer("game_update_timer", false, 0.toLong(), (1000.0/60.0).toLong()){
                    updateGameData()
                }
            }
        }

        // reset the paddles to the screen bounds
        Collision.resetToBounds(this.lowerPaddle as Shape, -ratio, -1.0f, ratio, 1.0f)
        Collision.resetToBounds(this.upperPaddle as Shape, -ratio, -1.0f, ratio, 1.0f)

        return true
    }

    // update the ball position, handle powerups and particles
    private fun updateGameData() {

        synchronized(allBallsLock) {


            this.allBalls.forEach {
                // add v*dt speed to the ball in x and y direction
                it.moveBy(it.velocity.x, it.velocity.y, 0.0f)
            }
            // if we are in single player mode we want to move the upper paddle so one lonely player
            // can nevertheless have some fun!
            if (gameMode == "SINGLE_PLAYER" && !this.blockUpper) {
                // get the ball which has the biggest y-coordinate
                val closestBall = this.allBalls.sortedByDescending { it.position.y }.first()

                // calculate the difference between the paddle and this ball
                var diff = closestBall.position.x - (this.upperPaddle?.position?.x ?: 0.0f)

                // if the difference is to big, cut it
                if(diff > this.maxPaddleSpeed) {
                    diff = this.maxPaddleSpeed
                }else if(diff < -this.maxPaddleSpeed){
                    diff = -this.maxPaddleSpeed
                }

                // move the paddle by this way
                this.upperPaddle?.moveBy(diff, 0.0f, 0.0f)

                // check if the paddle would be outside the visible area
                Collision.resetToBounds(this.upperPaddle as Shape, -this.ratio, -1.0f, this.ratio, 1.0f)
            }
        }

        if(showParticles) {
            // update particles -> alpha level, life, remove/add
            synchronized(particleLock) {
                val iterator = particles.iterator()
                while (iterator.hasNext()) {
                    val particle = iterator.next()
                    particle.life -= 1.0f / 60.0f
                    particle.color[3] = particle.life / particleLife

                    if (particle.life < 0.0f) {
                        iterator.remove()
                        continue
                    }

                    particle.moveBy(particle.velocity.x, particle.velocity.y, 0.0f)
                }

                synchronized(allBallsLock) {
                    this.allBalls.forEach {
                        // create a new particle in every iteration
                        this.particles.add(initNewParticleAt(it))
                    }
                }
            }
        }

        if(this.enablePowerUps) {

            // remove powerUps after life is < 0
            synchronized(powerUpLock) {
                val iterator = this.powerUps.iterator()
                while (iterator.hasNext()) {
                    val powerUp = iterator.next()
                    powerUp.life -= 1.0f / 60.0f

                    if (powerUp.life < 0.0f) {
                        iterator.remove()
                        continue
                    }
                }
            }

            // try to randomly generate powerUps
            if (Math.random() > 0.995) {
                synchronized(this.taskLock) {
                    glTasks.add {
                        val kind = PowerUpKind.values()[Math.round((Math.random() * (PowerUpKind.values().size - 2)).toFloat())]
                        val p = PowerUp(context,
                                Math.random().toFloat() * if (Math.random() > 0.5) {
                                    (ratio - 0.1f)
                                } else {
                                    -(ratio - 0.1f)
                                },
                                Math.random().toFloat() * if (Math.random() > 0.5) {
                                    0.25f
                                } else {
                                    -0.25f
                                },
                                0.05f,
                                kind, this.textures[kind]?.second ?: -1)

                        if(allBalls.count() < 10 || p.powerUpKind != PowerUpKind.EXTRA_BALL){
                            synchronized(powerUpLock) {
                                this.powerUps.add(p)
                            }
                        }
                    }
                }
            }
        }

        checkCollisions()

        // increase difficulty by speeding up all balls and shrinking the paddles
        synchronized(allBallsLock) {
            this.allBalls.forEach {
                // increase speed with some randomness
                it.velocity.y *= 1.0f + Math.random().toFloat() / 15000.0f
            }
        }

        // shrink both paddles
        this.upperPaddle?.multiplyScaleXwith(this.paddleShrinkFactor)
        this.lowerPaddle?.multiplyScaleXwith(this.paddleShrinkFactor)
    }

    // detect all kinds of collisions
    private fun checkCollisions() = synchronized(allBallsLock){
        // check if a ball hits a border
        this.allBalls.forEach {

            var collision = Collision.checkIfInside(it as Shape, -ratio, -1.0f, ratio, 1.0f)
            if (collision.didCollide) {

                // oh no, a player lost the ball :(
                if (collision.direction == Shape.Direction.UP) {
                    this.lowerPlayerScore += 1
                    (context as Activity).runOnUiThread { this.lowerPlayerTextView.text = this.lowerPlayerScore.toString() }
                    this.giveCollisionFeedback(500)

                    if (this.lowerPlayerScore >= this.scoreToWin) {
                        this.finalScoreReached()
                        return
                    }

                    resetBalls()
                    resetPaddles()

                    return

                // same here :/
                } else if (collision.direction == Shape.Direction.DOWN) {
                    this.upperPlayerScore += 1
                    (context as Activity).runOnUiThread { this.upperPlayerTextView.text = this.upperPlayerScore.toString() }
                    this.giveCollisionFeedback(500)

                    if (this.upperPlayerScore >= this.scoreToWin) {
                        this.finalScoreReached()
                        return
                    }

                    resetBalls()
                    resetPaddles()
                    return

                // the ball touched the left or right wall, reflect it
                } else if (collision.direction == Shape.Direction.LEFT || collision.direction == Shape.Direction.RIGHT) {
                    this.giveCollisionFeedback(50)

                    Collision.eval(collision, it as Shape)
                }

                // we already had a collision, so we don't need to check anything else
                return@forEach
            }


            // check ball - paddle collisions
            if (it.position.y > 0.0f) {
                collision = Collision.checkIntersectionBetween(it as Shape, this.upperPaddle as Shape)
                if (collision.didCollide) {

                    this.giveCollisionFeedback(50)

                    // work around for wrong behaviour
                    if (collision.direction == Shape.Direction.LEFT && it.velocity.x > 0.0f ||
                            collision.direction == Shape.Direction.RIGHT && it.velocity.x < 0.0f) {
                        collision.direction = Shape.Direction.UP
                    }

                    Collision.eval(collision, it as Shape)

                    // the ball speed shouldn't be changed in single player mode cause it would always
                    // slow down as the ball hits the paddle in the exact middle
                    if (this.gameMode == "MULTI_PLAYER") {
                        // when the ball hits the middle of a paddle the value is near 0, at the edges it is +1
                        val offset = Math.abs(it.position.x - (this.upperPaddle?.position?.x ?: 0.0f)) / ((this.upperPaddle?.getWidth() ?: 0.0f) / 2.0f + it.velocity.x / 2.0f)

                        // if we hit the paddle in the middle we want to reduce the y-speed, at the edges we want to increase it
                        //                 [0,1]  [0,2]  [-1,+1] [-0.005,+0.005]  [-1,+1]
                        it.velocity.x += (offset * 2.0f - 1.0f) * 0.005f * if (it.velocity.x > 0.0f) { 1.0f } else { -1.0f }
                    }

                    return@forEach
                }
            } else {
                collision = Collision.checkIntersectionBetween(it as Shape, this.lowerPaddle as Shape)
                if (collision.didCollide) {
                    this.giveCollisionFeedback(50)

                    // work around for wrong behaviour
                    if (collision.direction == Shape.Direction.LEFT && it.velocity.x > 0.0f ||
                            collision.direction == Shape.Direction.RIGHT && it.velocity.x < 0.0f) {
                        collision.direction = Shape.Direction.DOWN
                    }

                    Collision.eval(collision, it as Shape)

                    // when the ball hits the middle of a paddle the value is near 0, at the edges it is +1
                    val offset = Math.abs(it.position.x - (this.lowerPaddle?.position?.x ?: 0.0f)) / ((this.lowerPaddle?.getWidth() ?: 0.0f) / 2.0f + it.velocity.x / 2.0f)

                    // if we hit the paddle in the middle we want to reduce the y-speed, at the edges we want to increase it
                    //                         [0,1]    [0,2]  [-1,+1] [-0.005,+0.005]  [-1,+1]
                    it.velocity.x += (offset * 2.0f - 1.0f) * 0.005f * if (it.velocity.x > 0.0f) {
                        1.0f
                    } else {
                        -1.0f
                    }

                    return@forEach
                }
            }


            // check ball - powerUp collisions
            synchronized(powerUpLock) {
                val iterator = this.powerUps.iterator()
                while (iterator.hasNext()) {
                    val powerUp = iterator.next()
                    collision = Collision.checkIntersectionBetween(it as Shape, powerUp as Shape)
                    if (collision.didCollide) {
                        if(this.soundIsEnabled){
                            this.soundPool.play(this.powerUpSoundID,1f,1f,0,0,1f)
                        }

                        when (powerUp.powerUpKind) {
                            // speed the ball up a bit
                            PowerUpKind.SPEED_UP -> {
                                it.velocity.x *= 1.5f
                                it.velocity.y *= 1.5f
                                //println("############ SPEED UP #############")
                            }

                             // give an extra point to the player who last hit the ball
                            PowerUpKind.EXTRA_POINT -> {
                                if (it.velocity.y > 0.0f) {
                                    this.lowerPlayerScore += 1
                                    (context as Activity).runOnUiThread { this.lowerPlayerTextView.text = this.lowerPlayerScore.toString() }
                                    //println("############ EXTRA LOWER #############")
                                } else {
                                    this.upperPlayerScore += 1
                                    (context as Activity).runOnUiThread { this.upperPlayerTextView.text = this.upperPlayerScore.toString() }
                                    //println("############ EXTRA UPPER #############")
                                }

                                if (this.lowerPlayerScore >= this.scoreToWin || this.upperPlayerScore >= this.scoreToWin) {
                                    this.finalScoreReached()
                                    return
                                }
                            }

                            // invert color and user input
                            PowerUpKind.INVERT -> {
                                this.invertControls = -1.0f
                                // change text color so we can see it when the background is white
                                (context as Activity).runOnUiThread {
                                    this.upperPlayerTextView.setTextColor(Color.BLACK)
                                    this.lowerPlayerTextView.setTextColor(Color.BLACK)
                                }
                                thread {
                                    Thread.sleep(5000)
                                    this.invertControls = 1.0f

                                    // change the text color back
                                    context.runOnUiThread {
                                        this.upperPlayerTextView.setTextColor(Color.WHITE)
                                        this.lowerPlayerTextView.setTextColor(Color.WHITE)
                                    }
                                }
                                //println("############ INVERT #############")
                            }

                            // block the user interaction for 2,5s
                            PowerUpKind.STUCK_PADDLE -> {
                                if (it.velocity.y > 0.0f) {
                                    // block upper
                                    this.blockUpper = true

                                    thread {
                                        Thread.sleep(2500)
                                        this.blockUpper = false
                                    }
                                } else {
                                    // block lower
                                    this.blockLower = true

                                    thread {
                                        Thread.sleep(2500)
                                        this.blockLower = false
                                    }
                                }
                                //println("############ BLOCK #############")
                            }

                            // spawn additional ball
                            PowerUpKind.EXTRA_BALL -> {
                                this.glTasks.add {
                                    val newBall = Circle(context, 0.0f, 0.0f, 0.065f)
                                    newBall.velocity.x = it.velocity.x
                                    newBall.velocity.y = -it.velocity.y
                                    newBall.position.x = it.position.x
                                    newBall.position.y = it.position.y
                                    synchronized(allBallsLock) {
                                        this.allBalls.add(newBall)
                                    }
                                }
                                //println("############ EXTRA BALL #############")
                            }

                            // revert the ball's movement
                            PowerUpKind.REVERSE -> {
                                it.velocity.x *= -1
                                it.velocity.y *= -1
                            }

                            //else -> println("############ UNKNOWN POWERUP #############") // nothing to do
                        }

                        iterator.remove()
                    }
                }
            }
        }
    }

    // reset allBalls and start over
    private fun resetBalls() {
        this.updateGameDataTimer?.cancel()
        this.isRunning = false

        // remove all but hte first ball
        synchronized(allBallsLock) {
            if(this.allBalls.count() > 1) {
                val iterator = this.allBalls.iterator()
                // skip first one
                iterator.next()
                while(iterator.hasNext()) {
                    iterator.next()
                    iterator.remove()
                }
            }
        }

        // init the ball's velocity with some funky random speeds
        //   [0,1]       [0,2] [-1,1] [-0.00125, 0.00125]  [0.00875, 0.01125]                            vary + and -
        this.allBalls[0].velocity.x = ((((Math.random() * 2.0 - 1.0) / 1000.0) + 0.008) * this.randomSign()).toFloat()
        this.allBalls[0].velocity.y = ((((Math.random() * 2.0 - 1.0) / 1000.0) + 0.015) * this.randomSign()).toFloat()
        // debugging: 0.0037f 0.02f

        // set the ball's position depending on the velocity so the players have a chance to hit it
        if (this.allBalls[0].velocity.y > 0.0) {
            this.allBalls[0].moveTo(0.0f, -0.5f, 0.0f)
        } else {
            this.allBalls[0].moveTo(0.0f, 0.5f, 0.0f)
        }

        resetParticles()
        resetPowerUps()
    }

    // reset the player paddles to their initial position
    private fun resetPaddles(){
        this.lowerPaddle?.moveTo(0.0f, -0.8f, 0.0f)
        this.upperPaddle?.moveTo(0.0f, 0.8f, 0.0f)

        this.upperPaddle?.scaleX = 1.0f
        this.lowerPaddle?.scaleX = 1.0f
    }

    // clear the particles
    private fun resetParticles(){
        if(showParticles) {
            synchronized(particleLock) {
                if (this.particles.count() > 1) {
                    val iterator = this.particles.iterator()
                    // skip first one
                    iterator.next()
                    while (iterator.hasNext()) {
                        iterator.next()
                        iterator.remove()
                    }
                } else {
                    this.particles.add(initNewParticleAt(this.allBalls[0]))
                }
            }
        }
    }

    // clear all powerUps
    private fun resetPowerUps(){
        // delete all powerUps
        synchronized(powerUpLock) {
            this.powerUps.clear()
        }

        // reset all states to normal
        this.blockLower = false
        this.blockUpper = false
        this.invertControls = 1.0f

        (context as Activity).runOnUiThread {
            this.upperPlayerTextView.setTextColor(Color.WHITE)
            this.lowerPlayerTextView.setTextColor(Color.WHITE)
        }
    }

    // create a new particle object
    private fun initNewParticleAt(ball : Circle) : Particle{
        val particle = Particle(context, 0.0f, 0.0f, 0.025f)
        particle.position.x = ball.position.x
        particle.position.y = ball.position.y
        particle.velocity.x = ball.velocity.x / 4.0f + (((Math.random()) / 500.0) *
                if(ball.velocity.x > 0.0){1.0}else{-1.0}).toFloat()
        particle.velocity.y = ball.velocity.y / 4.0f + (((Math.random()) / 500.0) *
                if(ball.velocity.y > 0.0){1.0}else{-1.0}).toFloat()
        particle.color = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)
        particle.life = this.particleLife
        return particle
    }

    // reset everything to end the game
    fun shutdownGame(){
        // reset some stuff for next time
        this.updateGameDataTimer?.cancel()
        this.isRunning = false
        this.particles.clear()
        Particle.resetVBOHandle()
    }

    // freeze the game
    fun pauseGame(){
        // we don't want the ball to move we our app is in the background
        this.updateGameDataTimer?.cancel()
        this.isRunning = false
        if(this.soundIsEnabled) {
            this.backgroundMusic.pause()
        }
    }

    // start again
    fun resumeGame(){
        //this.isRunning = true
        if(this.soundIsEnabled) {
            this.backgroundMusic.start()
        }
    }

    // vibration and sounds
    private fun giveCollisionFeedback(vibrationDuration : Long){
        if(this.vibrationIsEnabled){
            // we can't use the new mechanism because of the targeted api version
            this.vibrator.vibrate(vibrationDuration)
        }

        if(this.soundIsEnabled){
            this.soundPool.play(this.collisionID, 1.0f, 1.0f, 0, 0, 1.0f)
        }
    }

    // returns -1 or +1
    private fun randomSign() : Int = if(Math.random() > 0.5){ 1 }else{ -1 }

    // load all textures needed
    private fun loadTextures(){
        // list of all textures we want to use
        this.textures.put(PowerUpKind.NOT_SPECIFIED, Pair(R.raw.texture_empty, -1))
        this.textures.put(PowerUpKind.INVERT, Pair(R.raw.texture_invert, -1))
        this.textures.put(PowerUpKind.EXTRA_POINT, Pair(R.raw.texture_plus_one, -1))
        this.textures.put(PowerUpKind.SPEED_UP, Pair(R.raw.texture_speed_up, -1))
        this.textures.put(PowerUpKind.STUCK_PADDLE, Pair(R.raw.texture_stuck_paddle, -1))
        this.textures.put(PowerUpKind.EXTRA_BALL, Pair(R.raw.texture_extra_ball, -1))
        this.textures.put(PowerUpKind.REVERSE, Pair(R.raw.texture_reverse, -1))

        // load each texture and save its id
        this.textures.forEach{
            this.textures.put(it.key, Pair(it.value.first, GLHelper.initializeTexture(this.context, it.value.first)))
        }
    }

    // end the round
    private fun finalScoreReached(){
        // cancel timers
        this.updateGameDataTimer?.cancel()

        if(this.soundIsEnabled){
            this.backgroundMusic.pause()
        }

        (context as Activity).runOnUiThread {


            val ft = context.fragmentManager.beginTransaction()
            val prev = context.fragmentManager.findFragmentByTag("endGameDialog")
            if (prev == null) {
                ft.addToBackStack(null)

                var headingText : String = context.getResources().getString(R.string.end)

                // if we are in singleplayer mode
                if(gameMode == "SINGLE_PLAYER"){
                    // and the player won the game
                    if(lowerPlayerScore == scoreToWin){
                        if(this.soundIsEnabled){
                            this.soundPool.play(victorySoundID, 1.0f, 1.0f, 0, 0, 1.0f)
                        }
                        headingText = context.getResources().getString(R.string.wonGame)
                    }else if(upperPlayerScore == scoreToWin){
                        if(this.soundIsEnabled){
                            this.soundPool.play(loseSoundID, 1.0f, 1.0f, 0, 0, 1.0f)
                        }
                        headingText = context.getResources().getString(R.string.lostGame)
                    }
                }else{
                    if(this.soundIsEnabled){
                        this.soundPool.play(victorySoundID, 1.0f, 1.0f, 0, 0, 1.0f)
                    }
                }

                // Create and show the dialog.
                val newFragment = EndGameDialogFragment(headingText)
                newFragment.isCancelable = false

                newFragment.show(ft, "endGameDialog",
                        {
                            // set up ball and paddles
                            this.resetBalls()
                            this.resetPaddles()

                            if(this.soundIsEnabled){
                                this.backgroundMusic.start()
                            }

                            // reset scores
                            this.lowerPlayerScore = 0
                            this.upperPlayerScore = 0
                            this.lowerPlayerTextView.text = this.lowerPlayerScore.toString()
                            this.upperPlayerTextView.text = this.upperPlayerScore.toString()
                        },
                        {
                            this.context.finish()
                        })
            }
        }
    }

    private fun setLightPosition() {
        // set the new positions of all balls to lightPosition
        for (i in 0 until this.allBalls.count()){
            this.lightPosition[i].x = this.allBalls[i].position.x
            this.lightPosition[i].y = this.allBalls[i].position.y
            this.lightPosition[i].z = this.allBalls[i].position.z
        }
    }

    private fun calcLightPolygons(){
        setLightPosition()

        // delete all old lightPolygons
        this.lightPolygons.clear()

        for (i in 0 until allBalls.count()){
            // reset the playground corner positions of the last calcLowerShadow() and calcUpperShadow()
            this.v1 = floatArrayOf(-1.0f, -1.0f)
            this.v2 = floatArrayOf(-1.0f, 1.0f)
            this.v3 = floatArrayOf(1.0f, 1.0f)
            this.v4 = floatArrayOf(1.0f, -1.0f)

            // calculate shadow of the upper and the lower paddle
            val lowerTrapeze = calcLowerShadow(i)
            val upperTrapeze = calcUpperShadow(i)

            // adding for each light source a new lightPolygon
            this.lightPolygons.add(LightPolygon(context, 0.0f, 0.0f, lightPosition[i].x, lightPosition[i].y,
                    this.v1[0], this.v1[1], this.v2[0], this.v2[1], this.v3[0], this.v3[1], this.v4[0], this.v4[1],
                    lowerTrapeze, upperTrapeze, -1))
        }
    }

    private fun calcLowerShadow(i : Int) : ShadowTrapeze{
            // array (angle1, quadrant1, ...) to compare the angles of all four vertices
            val angleArray = doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)

            // variables to safe the biggest and lowest angle and its index
            var maxIndex = 0
            var minIndex = 0
            var max = 0.0
            var min = Math.PI * 2.0

            val x = (this.lowerPaddle?.position?.x ?: 0.0f)
            val y = (this.lowerPaddle?.position?.y ?: 0.0f)

            for(item in 0..3){
                // calculating the adjacent side and the opposite side of the right triangular triangles between the vertex and the lightsource
                val deltaX = x - lightPosition[i].x + (this.lowerPaddle?.vertexData?.get(item * 3) ?: 0f)
                val deltaY = y - lightPosition[i].y + (this.lowerPaddle?.vertexData?.get(item * 3 + 1) ?: 0f)

                // get the angle and the quadrant
                if(deltaX > 0.0f){
                    if(deltaY > 0.0f){
                        angleArray[item * 2 + 1] = 2.0
                        angleArray[item * 2] = Math.PI - Math.atan(Math.abs(deltaY.toDouble()) / Math.abs(deltaX))
                    }
                    else{
                        angleArray[item * 2 + 1] = 3.0
                        angleArray[item * 2] = Math.PI + Math.atan(Math.abs(deltaY.toDouble()) / Math.abs(deltaX))
                    }
                }
                else{
                    if(deltaY > 0.0f){
                        angleArray[item * 2 + 1] = 1.0
                        angleArray[item * 2] = Math.atan(Math.abs(deltaY.toDouble()) / Math.abs(deltaX))
                    }
                    else{
                        angleArray[item * 2 + 1] = 4.0
                        angleArray[item * 2] = 2.0 * Math.PI - Math.atan(Math.abs(deltaY.toDouble()) / Math.abs(deltaX))
                    }
                }

                // check if its bigger than max or smaller than min
                if(angleArray[item * 2] > max){
                    max = angleArray[item * 2]
                    maxIndex = item
                }
                if(angleArray[item * 2] < min){
                    min = angleArray[item * 2]
                    minIndex = item
                }
            }

            // switching max and min if one is smaller than 90° and the other one bigger than 270°
            if(max - min > Math.PI){
                val temp = maxIndex
                maxIndex = minIndex
                minIndex = temp
            }

            // coordinates of all four vertices of the shadowTrapeze
            // 1 and 4 are similar to the vertices of the paddle
            val vertex1 = floatArrayOf((this.lowerPaddle?.vertexData?.get(maxIndex * 3) ?: 0.0f) + (this.lowerPaddle?.position?.x ?: 0.0f), (this.lowerPaddle?.vertexData?.get(maxIndex * 3 + 1) ?: 0.0f) + (this.lowerPaddle?.position?.y ?: 0.0f))
            val vertex2 = floatArrayOf(0.0f, 0.0f)
            val vertex3 = floatArrayOf(0.0f, 0.0f)
            val vertex4 = floatArrayOf((this.lowerPaddle?.vertexData?.get(minIndex * 3) ?: 0.0f) + (this.lowerPaddle?.position?.x ?: 0.0f), (this.lowerPaddle?.vertexData?.get(minIndex * 3 + 1) ?: 0.0f) + (this.lowerPaddle?.position?.y ?: 0.0f))

        // calculate vertex2 and vertex3 depending on its quadrant
        when {
            angleArray[maxIndex * 2 + 1] == 1.0 -> {
                vertex2[0] = (x + (this.lowerPaddle?.vertexData?.get(maxIndex * 3) ?: 0.0f) - Math.tan(Math.PI / 2.0 - angleArray[maxIndex * 2]).toFloat() * (1.0f - y))
                vertex2[1] = 1.0f
            }
            angleArray[maxIndex * 2 + 1] == 2.0 -> {
                vertex2[0] = x + (this.lowerPaddle?.vertexData?.get(maxIndex * 3) ?: 0.0f) + Math.tan(angleArray[maxIndex * 2] - Math.PI / 2.0).toFloat() * (1.0f - y)
                vertex2[1] = 1.0f
            }
            angleArray[maxIndex * 2 + 1] == 3.0 -> {
                vertex2[0] = x + (this.lowerPaddle?.vertexData?.get(maxIndex * 3) ?: 0.0f) + Math.tan(1.5 * Math.PI - angleArray[maxIndex * 2]).toFloat() * (1.0f + y)
                vertex2[1] = -1.0f
            }
            angleArray[maxIndex * 2 + 1] == 4.0 -> {
                vertex2[0] = (x + (this.lowerPaddle?.vertexData?.get(maxIndex * 3) ?: 0.0f) - Math.tan(angleArray[maxIndex * 2] - 1.5 * Math.PI).toFloat() * (1.0f + y))
                vertex2[1] = -1.0f
            }
        }

            if(angleArray[minIndex * 2 + 1] == 1.0){
                vertex3[0] = x + (this.lowerPaddle?.vertexData?.get(minIndex * 3) ?: 0.0f) - (Math.tan(Math.PI / 2.0 - angleArray[minIndex * 2]).toFloat() * (1.0f - y))
                vertex3[1] = 1.0f
                if(vertex3[0] < -1.0f){
                    this.v2 = vertex3
                }
            }
            else if(angleArray[minIndex * 2 + 1] == 2.0){
                vertex3[0] = x + (this.lowerPaddle?.vertexData?.get(minIndex * 3) ?: 0.0f) + Math.tan(angleArray[minIndex * 2] - Math.PI / 2.0).toFloat() * (1.0f - y)
                vertex3[1] = 1.0f
                if(vertex4[0] > 1.0f){
                    this.v3 = vertex4
                }
            }
            else if(angleArray[minIndex * 2 + 1] == 3.0){
                vertex3[0] = x + (this.lowerPaddle?.vertexData?.get(minIndex * 3) ?: 0.0f) + Math.tan(1.5 * Math.PI - angleArray[minIndex * 2]).toFloat() * (1.0f + y)
                vertex3[1] = -1.0f
                if(vertex4[0] < -1.0f){
                    this.v4 = vertex4
                }
            }
            else if(angleArray[minIndex * 2 + 1] == 4.0){
                vertex3[0] = (x + (this.lowerPaddle?.vertexData?.get(minIndex * 3) ?: 0.0f) - Math.tan(angleArray[minIndex * 2] - 1.5 * Math.PI).toFloat() * (1.0f + y))
                vertex3[1] = -1.0f
                if(vertex3[0] > 1.0f){
                    this.v1 = vertex3
                }
            }

            // return the new shadowTrapeze
            return ShadowTrapeze(context, x, y,
                    vertex1[0] - x, vertex1[1] - y,
                    vertex2[0] - x, vertex2[1] - y,
                    vertex3[0] - x, vertex3[1] - y,
                    vertex4[0] - x, vertex4[1] - y,
                    -1)
    }

    private fun calcUpperShadow(i : Int) : ShadowTrapeze{
        // array (angle1, quadrant1, ...) to compare the angles of all four vertices
        val angleArray = doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)

        // variables to safe the biggest and lowest angle and its index
        var maxIndex = 0
        var minIndex = 0
        var max = 0.0
        var min = Math.PI * 2.0

        val x = (this.upperPaddle?.position?.x ?: 0.0f)
        val y = (this.upperPaddle?.position?.y ?: 0.0f)

        for(item in 0..3){
            // calculating the adjacent side and the opposite side of the right triangular triangles between the vertex and the lightsource
            val deltaX = x - lightPosition[i].x + (this.upperPaddle?.vertexData?.get(item * 3) ?: 0f)
            val deltaY = y - lightPosition[i].y + (this.upperPaddle?.vertexData?.get(item * 3 + 1) ?: 0f)

            // get the angle and the quadrant
            if(deltaX > 0.0f){
                if(deltaY > 0.0f){
                    angleArray[item * 2 + 1] = 2.0
                    angleArray[item * 2] = Math.PI - Math.atan(Math.abs(deltaY.toDouble()) / Math.abs(deltaX))
                }
                else{
                    angleArray[item * 2 + 1] = 3.0
                    angleArray[item * 2] = Math.PI + Math.atan(Math.abs(deltaY.toDouble()) / Math.abs(deltaX))
                }
            }
            else{
                if(deltaY > 0.0f){
                    angleArray[item * 2 + 1] = 1.0
                    angleArray[item * 2] = Math.atan(Math.abs(deltaY.toDouble()) / Math.abs(deltaX))
                }
                else{
                    angleArray[item * 2 + 1] = 4.0
                    angleArray[item * 2] = 2.0 * Math.PI - Math.atan(Math.abs(deltaY.toDouble()) / Math.abs(deltaX))
                }
            }

            // check if its bigger than max or smaller than min
            if(angleArray[item * 2] > max){
                max = angleArray[item * 2]
                maxIndex = item
            }
            if(angleArray[item * 2] < min){
                min = angleArray[item * 2]
                minIndex = item
            }
        }

        // switching max and min if one is smaller than 90° and the other one bigger than 270°
        if(max - min > Math.PI){
            val temp = maxIndex
            maxIndex = minIndex
            minIndex = temp
        }

        // coordinates of all four vertices of the shadowTrapeze
        // 1 and 4 are similar to the vertices of the paddle
        val vertex1 = floatArrayOf((this.upperPaddle?.vertexData?.get(maxIndex * 3) ?: 0.0f) + (this.upperPaddle?.position?.x ?: 0.0f), (this.upperPaddle?.vertexData?.get(maxIndex * 3 + 1) ?: 0.0f) + (this.upperPaddle?.position?.y ?: 0.0f))
        val vertex2 = floatArrayOf(0.0f, 0.0f)
        val vertex3 = floatArrayOf(0.0f, 0.0f)
        val vertex4 = floatArrayOf((this.upperPaddle?.vertexData?.get(minIndex * 3) ?: 0.0f) + (this.upperPaddle?.position?.x ?: 0.0f), (this.upperPaddle?.vertexData?.get(minIndex * 3 + 1) ?: 0.0f) + (this.upperPaddle?.position?.y ?: 0.0f))

        // calculate vertex2 and vertex3 depending on its quadrant
        when {
            angleArray[maxIndex * 2 + 1] == 1.0 -> {
                vertex2[0] = (x + (this.upperPaddle?.vertexData?.get(maxIndex * 3) ?: 0.0f) - Math.tan(Math.PI / 2.0 - angleArray[maxIndex * 2]).toFloat() * (1.0f - y))
                vertex2[1] = 1.0f
            }
            angleArray[maxIndex * 2 + 1] == 2.0 -> {
                vertex2[0] = x + (this.upperPaddle?.vertexData?.get(maxIndex * 3) ?: 0.0f) + Math.tan(angleArray[maxIndex * 2] - Math.PI / 2.0).toFloat() * (1.0f - y)
                vertex2[1] = 1.0f
            }
            angleArray[maxIndex * 2 + 1] == 3.0 -> {
                vertex2[0] = x + (this.upperPaddle?.vertexData?.get(maxIndex * 3) ?: 0.0f) + Math.tan(1.5 * Math.PI - angleArray[maxIndex * 2]).toFloat() * (1.0f + y)
                vertex2[1] = -1.0f
            }
            angleArray[maxIndex * 2 + 1] == 4.0 -> {
                vertex2[0] = (x + (this.upperPaddle?.vertexData?.get(maxIndex * 3) ?: 0.0f) - Math.tan(angleArray[maxIndex * 2] - 1.5 * Math.PI).toFloat() * (1.0f + y))
                vertex2[1] = -1.0f
            }
        }

        if(angleArray[minIndex * 2 + 1] == 1.0){
            vertex3[0] = x + (this.upperPaddle?.vertexData?.get(minIndex * 3) ?: 0.0f) - (Math.tan(Math.PI / 2.0 - angleArray[minIndex * 2]).toFloat() * (1.0f - y))
            vertex3[1] = 1.0f
            if(vertex3[0] < -1.0f){
                this.v2 = vertex3
            }
        }
        else if(angleArray[minIndex * 2 + 1] == 2.0){
            vertex3[0] = x + (this.upperPaddle?.vertexData?.get(minIndex * 3) ?: 0.0f) + Math.tan(angleArray[minIndex * 2] - Math.PI / 2.0).toFloat() * (1.0f - y)
            vertex3[1] = 1.0f
            if(vertex4[0] > 1.0f){
                this.v3 = vertex4
            }
        }
        else if(angleArray[minIndex * 2 + 1] == 3.0){
            vertex3[0] = x + (this.upperPaddle?.vertexData?.get(minIndex * 3) ?: 0.0f) + Math.tan(1.5 * Math.PI - angleArray[minIndex * 2]).toFloat() * (1.0f + y)
            vertex3[1] = -1.0f
            if(vertex4[0] < -1.0f){
                this.v4 = vertex4
            }
        }
        else if(angleArray[minIndex * 2 + 1] == 4.0){
            vertex3[0] = (x + (this.upperPaddle?.vertexData?.get(minIndex * 3) ?: 0.0f) - Math.tan(angleArray[minIndex * 2] - 1.5 * Math.PI).toFloat() * (1.0f + y))
            vertex3[1] = -1.0f
            if(vertex3[0] > 1.0f){
                this.v1 = vertex3
            }
        }

        // return the new shadowTrapeze
        return ShadowTrapeze(context, x, y,
                vertex1[0] - x, vertex1[1] - y,
                vertex2[0] - x, vertex2[1] - y,
                vertex3[0] - x, vertex3[1] - y,
                vertex4[0] - x, vertex4[1] - y,
                -1)
    }
}