package br.com.igorbag.githubsearch.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import br.com.igorbag.githubsearch.R
import br.com.igorbag.githubsearch.data.GitHubService
import br.com.igorbag.githubsearch.domain.Repository
import br.com.igorbag.githubsearch.ui.adapter.RepositoryAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    lateinit var nomeUsuario: EditText
    lateinit var btnConfirmar: Button
    lateinit var listaRepositories: RecyclerView
    lateinit var githubApi: GitHubService
    lateinit var tvError: TextView
    lateinit var ivError: ImageView
    lateinit var pbLoading: ProgressBar
    lateinit var ivNoInternet: ImageView
    lateinit var tvNoInternet: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupRetrofit()
        setupView()
        showUserName()
        setupListeners()
    }

    // Metodo responsavel por realizar o setup da view e recuperar os Ids do layout
    fun setupView() {
        nomeUsuario = findViewById(R.id.et_nome_usuario)
        btnConfirmar = findViewById(R.id.btn_confirmar)
        listaRepositories = findViewById(R.id.rv_lista_repositories)
        tvError = findViewById(R.id.tv_error)
        ivError = findViewById(R.id.iv_error)
        pbLoading = findViewById(R.id.pb_loading)
        ivNoInternet = findViewById(R.id.iv_noInternet)
        tvNoInternet = findViewById(R.id.tv_noInternet)
    }

    //metodo responsavel por configurar os listeners click da tela
    private fun setupListeners() {
        btnConfirmar.setOnClickListener {
            val user: String = nomeUsuario.text.toString()

            saveUserLocal(user)

            listaRepositories.isVisible = false
            tvError.isVisible = false
            ivError.isVisible = false
            getAllReposByUserName(user)
        }
    }


    // salvar o usuario preenchido no EditText utilizando uma SharedPreferences
    private fun saveUserLocal(user: String) {
        val sharedPref = getPreferences(Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putString(getString(R.string.saved_user), user)
            apply()
        }
    }

    private fun showUserName() {
        val userSaved: String = getSharedPref()
        val editableUser = Editable.Factory.getInstance().newEditable(userSaved)
        nomeUsuario.text = editableUser
        getAllReposByUserName(userSaved)
    }

    fun getSharedPref(): String {
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        return sharedPref.getString(getString(R.string.saved_user), "") ?: ""
    }

    //Metodo responsavel por fazer a configuracao base do Retrofit
    fun setupRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        githubApi = retrofit.create(GitHubService::class.java)
    }

    //Metodo responsavel por buscar todos os repositorios do usuario fornecido
    fun getAllReposByUserName(user: String) {
        val call = githubApi.getAllRepositoriesByUser(user)

        tvNoInternet.isVisible = false
        ivNoInternet.isVisible = false
        pbLoading.isVisible = true
        call.enqueue(object : Callback<List<Repository>> {
            override fun onResponse(call: Call<List<Repository>>, response: Response<List<Repository>>) {
                if (response.isSuccessful) {
                    val repositories = response.body()
                    Log.d("userGit", "Lista de repositórios: $repositories")

                    repositories?.let {
                        setupAdapter(it)
                    }
                    pbLoading.isVisible = false
                    tvError.isVisible = false
                    ivError.isVisible = false
                    listaRepositories.isVisible = true

                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.d("userGit", "Erro na resposta: $errorBody")
                    listaRepositories.isVisible = false
                    pbLoading.isVisible = false
                    tvError.isVisible = true
                    ivError.isVisible = true
                }
            }

            override fun onFailure(call: Call<List<Repository>>, t: Throwable) {
                Log.e("userGit", "Falha na chamada: ${t.message}")
                listaRepositories.isVisible = false
                pbLoading.isVisible = false
                tvError.isVisible = false
                ivError.isVisible = false
                tvNoInternet.isVisible = true
                ivNoInternet.isVisible = true
            }
        })

    }

    // Metodo responsavel por realizar a configuracao do adapter
    fun setupAdapter(list: List<Repository>) {

        val adapter = RepositoryAdapter(list, this)
        listaRepositories.adapter = adapter
    }


    // Metodo responsavel por compartilhar o link do repositorio selecionado
    fun shareRepositoryLink(urlRepository: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, urlRepository)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    // Metodo responsavel por abrir o browser com o link informado do repositorio


    fun openBrowser(urlRepository: String) {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(urlRepository)
            )
        )

    }

}