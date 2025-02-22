package com.example.icimdekiler

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.navigation.Navigation
import androidx.navigation.Navigator
import com.example.icimdekiler.databinding.FragmentGirisYapBinding
import com.example.icimdekiler.girisYapFragment.UserSession.ePosta
import com.example.icimdekiler.girisYapFragment.UserSession.isAdmin
import com.example.icimdekiler.girisYapFragment.UserSession.kullaniciAdi
import com.example.icimdekiler.girisYapFragment.UserSession.parola
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.auth.User
import com.google.firebase.firestore.firestore
import kotlin.concurrent.timerTask

class girisYapFragment : Fragment() {

    //Binding
    private var _binding: FragmentGirisYapBinding? = null
    private val binding get() = _binding!!

    //Firebase
    private lateinit var auth: FirebaseAuth
    val db = Firebase.firestore

    object UserSession {
        var kullaniciAdi = ""
        var ePosta = ""
        var parola = ""
        var isAdmin: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentGirisYapBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.kayitOlLabel.setOnClickListener {
            val action = girisYapFragmentDirections.actionGirisYapFragmentToKayitOlFragment()
            Navigation.findNavController(view).navigate(action)
        }

        binding.girisYapButton.setOnClickListener {
            girisYap()
            /*
            if (binding.beniHatirlaCheckBox.isChecked){
                val guncelKullanici=auth.currentUser
                if (guncelKullanici != null){
                    if (UserSession.isAdmin){
                        val action = girisYapFragmentDirections.actionGirisYapFragmentToAdminAnaSayfaFragment()
                        Navigation.findNavController(requireView()).navigate(action)
                        Toast.makeText(requireContext(), "Hoşgeldin Admin ${UserSession.kullaniciAdi}", Toast.LENGTH_SHORT).show()
                    }else{
                        val action = girisYapFragmentDirections.actionGirisYapFragmentToKullaniciAnaSayfaFragment()
                        Navigation.findNavController(requireView()).navigate(action)
                        Toast.makeText(requireContext(), "Hoşgeldin ${UserSession.kullaniciAdi}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
             */
        }
    }

    fun hatirlaKontrol(){
        val guncelKullanici = auth.currentUser
        if (guncelKullanici != null){
            db.collection("kullaniciBilgileri").addSnapshotListener { value, error ->
                if (error != null){
                    Toast.makeText(requireContext(), error.localizedMessage, Toast.LENGTH_LONG).show()
                }else{
                    if (value != null){
                        if (value.isEmpty){
                            val documents=value.documents
                            for(document in documents){

                            }
                        }
                    }
                }
            }
        }
    }

    fun girisYap(){
        // Kullanıcının girdiği bilgileri al
        val ePosta = binding.ePostaText.text.toString().trim()
        val parola = binding.parolaText.text.toString().trim()

        // Boş alan kontrolü yap
        if (ePosta.isNotEmpty() && parola.isNotEmpty() ) {
            // Firebase Authentication ile giriş yapılıyor
            auth.signInWithEmailAndPassword(ePosta, parola)
                .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val guncelKullanici = auth.currentUser  // Şu anki oturum açan kullanıcıyı al
                    if (guncelKullanici != null) {
                        // Firestore'dan kullanıcı bilgilerini getir
                        db.collection("kullaniciBilgileri")
                            .whereEqualTo("ePosta", ePosta)  // Kullanıcıyı e-posta adresi ile bul
                            .whereEqualTo("parola", parola)  // Parolayı da doğrula
                            .whereEqualTo("kullaniciUID", guncelKullanici.uid)  // UID eşleşmesini kontrol et
                            .get()
                            .addOnSuccessListener { documents ->
                                if (!documents.isEmpty) {  // Eğer Firestore'da kullanıcı varsa
                                    val kullanici = documents.documents.first() // İlk kullanıcıyı al

                                    // `isAdmin` alanını Boolean olarak al (null olursa false yap) ve Kullanıcının admin olup olmadığını kaydet
                                    UserSession.isAdmin = kullanici.getBoolean("isAdmin") ?: false

                                    // Kullanıcının adını session'a kaydet
                                    UserSession.kullaniciAdi = kullanici.getString("kullaniciAdi") ?: "Bilinmiyor"

                                    // Kullanıcının admin olup olmadığına göre yönlendirme yap
                                    if (isAdmin) {

                                        val action = girisYapFragmentDirections.actionGirisYapFragmentToAdminAnaSayfaFragment()
                                        Navigation.findNavController(requireView()).navigate(action)

                                        Toast.makeText(requireContext(), "Hoşgeldin Admin ${UserSession.kullaniciAdi}", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val action = girisYapFragmentDirections.actionGirisYapFragmentToKullaniciAnaSayfaFragment()
                                        Navigation.findNavController(requireView()).navigate(action)
                                        Toast.makeText(requireContext(), "Hoşgeldin ${UserSession.kullaniciAdi}", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    // Firestore'da eşleşen kullanıcı bulunamazsa hata ver
                                    Toast.makeText(requireContext(), "Kullanıcı bilgileri yanlış!", Toast.LENGTH_LONG).show()
                                }
                            }
                            .addOnFailureListener { exception ->
                                // Firestore'dan veri çekerken hata olursa kullanıcıya bildir
                                Toast.makeText(requireContext(), exception.localizedMessage, Toast.LENGTH_LONG).show()
                            }
                    }
                } else {
                    // Firebase Authentication giriş başarısızsa kullanıcıya hata mesajı göster
                    Toast.makeText(requireContext(), "Giriş başarısız! E-posta veya parola hatalı.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}