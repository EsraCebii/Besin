package com.esracebi.besinlerkitabi.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.esracebi.besinlerkitabi.model.Besin
import com.esracebi.besinlerkitabi.servis.BesinAPIServis
import com.esracebi.besinlerkitabi.servis.BesinDatabase
import com.esracebi.besinlerkitabi.utill.OzelSharedPreferences
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch

class BesinListesiViewModel(application: Application) : BaseViewModel(application) {
    val besinler =MutableLiveData<List<Besin>>()
    val besinHataMesaji =MutableLiveData<Boolean>()
    val besinYukleniyor =MutableLiveData<Boolean>()
    private var guncellemeZamani = 10* 60 *1000*1000*1000L

    private val besinApiServis = BesinAPIServis()
    private val disposable =CompositeDisposable()
    private val ozelSharedPreferences =OzelSharedPreferences(getApplication())





    fun refreshData (){


        val kaydedilmeZamani=ozelSharedPreferences.zamaniAl()
        if (kaydedilmeZamani !=null && kaydedilmeZamani!=0L && System.nanoTime()-kaydedilmeZamani < guncellemeZamani){
            //Sqlite'tan çek
        }else{
            verileriInternettenAl()
        }

    }

    fun refreshFromInternet(){
        verileriInternettenAl()

    }





    private fun verileriSQLitetanAl(){
        besinYukleniyor.value = true

        launch {
            val besinListesi = BesinDatabase(getApplication()).besinDao().getAllBesin()
            besinleriGöster(besinListesi)
            Toast.makeText(getApplication(),"Besinleri Room'dan Aldık",Toast.LENGTH_LONG).show()

        }
    }




    private fun verileriInternettenAl() {
        besinYukleniyor.value =true

        //IO, Default , UI

        disposable.add(
            besinApiServis.getData()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<List<Besin>>(){
                    override fun onSuccess(t: List<Besin>) {
                        //Başarılı olursa
                        sqLiteSakla(t)
                        Toast.makeText(getApplication(),"Besinleri İnternetten Aldık",Toast.LENGTH_LONG).show()

                    }

                    override fun onError(e: Throwable) {
                        //Hata alırsak
                        besinHataMesaji.value=true
                        besinYukleniyor.value=false
                        e.printStackTrace()

                    }

                })

        )


    }

    private fun besinleriGöster(besinlerListesi :List<Besin>) {
        besinler.value =besinlerListesi
        besinHataMesaji.value=false
        besinYukleniyor.value= false

    }


    private fun sqLiteSakla(besinListesi:List<Besin>) {

        launch {

            val dao =BesinDatabase(getApplication()).besinDao()
            dao.deleteAllBesin()
            val uuidListesi = dao.insertAll(*besinListesi.toTypedArray())
            var i=0
            while(i<besinListesi.size){
                besinListesi[i].uuid =uuidListesi[i].toInt()
                i =i+1
            }
            besinleriGöster(besinListesi)
        }
        ozelSharedPreferences.zamaniKaydet(System.nanoTime())




    }



}