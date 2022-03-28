package com.example.capstone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.*
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.capstone.databinding.RecipePageBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import kotlin.collections.ArrayList

class RecipePage : AppCompatActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognitionListener: RecognitionListener
    private val binding by lazy { RecipePageBinding.inflate(layoutInflater) }
    private val handler: Handler by lazy { Handler(Looper.getMainLooper()) }//음성인식 무한 실행을 위한 핸들러 변수
    private var speechText: String = "null"//받은 음성
    private var tts: TextToSpeech? = null//TTS변수
    private var selectedRecipe: Int = 0// 선택된 레시피 번호
    private var recipeTextList = emptyArray<String>()//레시피 리스트
    private var recipeURLList = emptyArray<String>()//레시피 사진 리스트
    private var recipeIngrList = emptyArray<String>()//재료 리스트
    var recipeText: String = ""
    var db = FirebaseFirestore.getInstance()
    var name : String = ""
    var ingredient : String = ""
    var picture : String = ""
    var recipePicture : String = ""
    var code : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        code = intent?.getStringExtra("code")?: ""

        db.collection("recipe")
            .document(code)
            .get()
            .addOnSuccessListener { document ->

                name = document["name"] as String
                ingredient = document["ingredient"] as String
                recipeText= document["content"] as String
                picture = document["picture"] as String
                recipePicture = document["recipePicture"] as String
                recipeTextList = recipeText.split("@").toTypedArray()
                recipeURLList = recipePicture.split("@").toTypedArray()

                binding.name.text = name
                binding.ingredient.text = ingredient
                Glide.with(this).load(picture).into(binding.titlePhoto)
                val recipesList : ArrayList<Recipes> = arrayListOf()
                for (i in 0 until recipeTextList.size){
                    if(recipeURLList[i] == "empty")//없을때 안읽어주는거 고쳐 TTS읽을때 공백인식해서 다음으로 넘기게 만들어야함
                        recipeURLList[i] = ""
                    if(recipeTextList[i] == "empty")
                        recipeTextList[i] = ""
                    recipesList.add(Recipes(recipeURLList[i], recipeTextList[i]))
                }
                muteNoti()
                requestPermission()
                setListener()
                makeTTS()
                show()

                binding.rvRecipe.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
                binding.rvRecipe.setHasFixedSize(true)
                binding.rvRecipe.adapter = RecipeAdapter(recipesList)
            }

    }

    override fun onDestroy() {
        super.onDestroy()
        // TTS 객체가 남아있다면 실행을 중지하고 메모리에서 제거한다.
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
            tts = null
        }

    }
    private fun muteNoti(){//알림음 뮤트
        val audioManager: AudioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val muteValue =  AudioManager.ADJUST_MUTE
        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, muteValue, 0)
    }

    private fun startSTT(){//STT시작
        var intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,100)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 100)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,500)

        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(recognitionListener)
            startListening(intent)
        }

    }

    private fun requestPermission(){//권한 허가 요청
        if(Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.RECORD_AUDIO), 0)
        }
    }

    private fun setListener(){//recognitionListener 생성
        recognitionListener = object: RecognitionListener {

            override fun onReadyForSpeech(p0: Bundle?) {//사용자가 말하기 시작할 준비가 되면 호출됨

                //Toast.makeText(applicationContext, "음성인식을 시작합니다.", Toast.LENGTH_SHORT).show()
            }

            override fun onBeginningOfSpeech() {//사용자가 말하기 시작했을때 호출됨

            }

            override fun onRmsChanged(p0: Float) {//입력받는 소리의 크기를 알려줌

            }

            override fun onBufferReceived(p0: ByteArray?) {//사용자가 말을 시작하고 인식이 된 단어를 buffer에 담음

            }

            override fun onEndOfSpeech() {//사용자가 말하기를 중지하면 호출됨

            }

            override fun onError(error: Int) {//에러가 났을때
                var message: String

                when(error) {
                    SpeechRecognizer.ERROR_AUDIO ->
                        message = "오디오 에러"
                    SpeechRecognizer.ERROR_CLIENT ->
                        message = "클라이언트 에러"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                        message = "퍼미션 없음"
                    SpeechRecognizer.ERROR_NETWORK ->
                        message = "네트워크 에러"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                        message = "네트워크 타임아웃"
                    SpeechRecognizer.ERROR_SERVER ->
                        message = "서버가 이상함"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                        message = "RECGNIZER가 바쁨"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                        message = "말하는 시간이 초과됨"
                    else ->
                        message = "알수없는 오류"
                }
                //Toast.makeText(applicationContext, "에러발생 $message", Toast.LENGTH_SHORT).show()
            }

            override fun onResults(results: Bundle?) {//인식 결과가 준비되면 호출됨
                var matches: ArrayList<String>? = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                if (matches != null) {
                    for (i in 0 until matches.size) {
                        speechText = matches[i]
                    }
                    if (speechText.contains("시작")){
                        startTTS()
                        autoScroll()
                    }
                    if(speechText.contains("다음")){
                        nextTTS()
                        autoScroll()
                    }
                    if(speechText.contains("이전")){
                        prevTTS()
                        autoScroll()
                    }
                    if(speechText.contains("멈춰")){
                        stopTTS()
                    }
                    if(speechText.contains("빠르게")){

                    }
                    if (speechText.contains("느리게")){

                    }
                }
            }

            override fun onPartialResults(partiaResults: Bundle?) {//부분 인식 결과를 사용할 수 있을때 호출됨

            }

            override fun onEvent(eventType: Int, params: Bundle?) {//향후 이벤트를 추가하기 위해 예약됨

            }
        }
    }

    private fun show(){//STT무한 실행

        startSTT()
        handler.postDelayed(::show, 1000)
    }

    private fun makeTTS(){//TTS만들기
        tts = TextToSpeech(this) { status ->
            if (status != TextToSpeech.ERROR) {
                // 언어를 선택한다.
                tts?.language = Locale.KOREAN
            }
        }
    }

    private fun startTTS(){//TTS시작
        if(recipeTextList[selectedRecipe] == "")
            selectedRecipe += 1
        tts?.speak(recipeTextList[selectedRecipe], TextToSpeech.QUEUE_FLUSH, null)
    }

    private fun nextTTS(){//다음 TTS
        selectedRecipe += 1
        tts?.speak(recipeTextList[selectedRecipe], TextToSpeech.QUEUE_FLUSH, null)
        if(selectedRecipe > recipeTextList.size){
            selectedRecipe = 0
        }
    }

    private fun prevTTS(){//이전 TTS
        selectedRecipe -= 1
        if (selectedRecipe<0) {
            selectedRecipe = 0
        }
        tts?.speak(recipeTextList[selectedRecipe], TextToSpeech.QUEUE_FLUSH, null)
    }

    private fun stopTTS(){//TTS멈춰!!
        tts?.stop()
    }

    private fun autoScroll(){
        if(selectedRecipe == 0)
            binding.nestrdScrollView.scrollTo(0, binding.textView3.bottom)
        else
            binding.nestrdScrollView.scrollTo(0, binding.rvRecipe[selectedRecipe+1].bottom)
    }

}