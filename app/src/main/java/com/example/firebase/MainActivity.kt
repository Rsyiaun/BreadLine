package com.example.firebase

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import org.json.JSONObject


class MainActivity : AppCompatActivity() {

    object KtorClient {
        val httpClient = HttpClient {
            install(ContentNegotiation) {
                json()
            }
            install(Logging)
            defaultRequest {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
            }
            expectSuccess = true
        }

        suspend fun breadSignIn(idToken: String?): String {

            return httpClient.post("https://cslinux0.comp.hkbu.edu.hk/~kennycheng/breadline/api/v2.0/auth/token") {
                val json = JSONObject()
                json.put("idToken", idToken)
                val jsonString = json.toString()
                setBody(jsonString)
            }.body()
        }
    }

    private lateinit var auth : FirebaseAuth
    private lateinit var googleSignInClient : GoogleSignInClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()


        googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleSignInClient.signOut()
        findViewById<Button>(R.id.gSignInBtn).setOnClickListener(){
            signInGoogle()
        }
    }

    private fun signInGoogle(){
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        result ->
            if(result.resultCode == Activity.RESULT_OK){
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleResult(task)
            }
    }

    private fun handleResult(task: Task<GoogleSignInAccount>) {
        if(task.isSuccessful){
            val account: GoogleSignInAccount? = task.result
            if(account!=null){
                updateUI(account)
            }
        }else{
            Toast.makeText(this,task.exception.toString(),Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        val intent: Intent = Intent(this, HomeActivity::class.java)
        println(account.idToken)
        auth.signInWithCredential(credential).addOnCompleteListener {
            if(it.isSuccessful) runBlocking {


                    var breadToken: String = ""
                    breadToken = KtorClient.breadSignIn(account.idToken)
                    val jObject = JSONObject(breadToken)
                    val aJsonString = jObject.getString("data")
                    println(aJsonString)
                    intent.putExtra("breadToken",aJsonString)

                intent.putExtra("email",account.email)
                intent.putExtra("name",account.displayName)

                startActivity(intent)
            }else{
                Toast.makeText(this,it.exception.toString(),Toast.LENGTH_SHORT).show()

            }

        }
    }


}