package com.example.icimdekiler.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.icimdekiler.databinding.FragmentGirisYapBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import androidx.navigation.findNavController
import com.example.icimdekiler.R

class girisYapFragment : Fragment() {

    //Binding
    private var _binding: FragmentGirisYapBinding? = null
    private val binding get() = _binding!!

    //Firebase
    private lateinit var auth: FirebaseAuth
    val db = Firebase.firestore

    var kullaniciAdi:String = ""
    var isAdmin: Boolean = false

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
            requireView().findNavController().navigate(action)
        }

        binding.girisYapButton.setOnClickListener { girisYap() }
    }

    private fun girisYap() {
        // Kullanıcının girdiği bilgileri al
        val ePosta = binding.ePostaText.text.toString().trim()
        val parola = binding.parolaText.text.toString().trim()

        // Boş alan kontrolü yap
        if (ePosta.isNotEmpty() && parola.isNotEmpty()) {
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

                                        isAdmin = kullanici.getBoolean("isAdmin") ?: false
                                        kullaniciAdi = kullanici.getString("kullaniciAdi") ?: "Bilinmiyor"

                                        // Fragment ekli mi kontrol et
                                        if (isAdded) {
                                            val navController = view?.findNavController()
                                            if (isAdmin) {
                                                val action = girisYapFragmentDirections.actionGirisYapFragmentToAdminAnaSayfaFragment()
                                                navController?.navigate(action)
                                                Toast.makeText(requireContext(), "${getString(R.string.hosgeldin)} Admin $kullaniciAdi", Toast.LENGTH_SHORT).show()
                                            } else {
                                                val action = girisYapFragmentDirections.actionGirisYapFragmentToKullaniciAnaSayfaFragment()
                                                navController?.navigate(action)
                                                Toast.makeText(requireContext(), "${getString(R.string.hosgeldin)} $kullaniciAdi", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else Toast.makeText(requireContext(), R.string.kullaniciBilgileriYanlis, Toast.LENGTH_LONG).show()
                                }.addOnFailureListener { exception -> Toast.makeText(requireContext(), exception.localizedMessage, Toast.LENGTH_LONG).show() }
                        } else Toast.makeText(requireContext(), R.string.kullaniciBulunamadi, Toast.LENGTH_LONG).show()
                    } else Toast.makeText(requireContext(), R.string.girisBasarisizEpostaVeyaParolaHatali, Toast.LENGTH_LONG).show()
                }
        } else Toast.makeText(requireContext(), R.string.lutfenBosAlanBirakmayiniz, Toast.LENGTH_LONG).show()

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}