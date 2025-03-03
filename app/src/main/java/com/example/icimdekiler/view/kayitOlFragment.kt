package com.example.icimdekiler.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.Navigation
import com.example.icimdekiler.databinding.FragmentKayitOlBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class kayitOlFragment : Fragment() {

    //Binding
    private var _binding: FragmentKayitOlBinding? = null
    private val binding get() = _binding!!

    //Firebase
    private lateinit var auth: FirebaseAuth
    val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentKayitOlBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.kayitOlButton.setOnClickListener {
            kayitOl()
        }

        binding.girisYapLabel.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed() // Önceki ekrana dön
        }
    }

    fun kayitOl() {
        val kullaniciAdi = binding.kullaniciAdiText.text.toString()
        val isimSoyisim = binding.isimSoyisimText.text.toString()
        val ePosta = binding.ePostaText.text.toString()
        val telNo = binding.telNoText.text.toString()
        val parola = binding.parolaText.text.toString()

        //Boş alan kontrolü yap
        if (kullaniciAdi.isNotEmpty() && isimSoyisim.isNotEmpty() && ePosta.isNotEmpty() && telNo.isNotEmpty() && parola.isNotEmpty()) {
            auth.createUserWithEmailAndPassword(ePosta, parola)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val guncelKullanici = auth.currentUser
                        if (guncelKullanici != null) {
                            //Kullanıcı bilgilerini bir Map'e koy
                            val kullaniciMap = hashMapOf<String, Any>()
                            kullaniciMap.put("kullaniciAdi", kullaniciAdi)
                            kullaniciMap.put("isimSoyisim", isimSoyisim)
                            kullaniciMap.put("ePosta", ePosta)
                            kullaniciMap.put("telNo", telNo)
                            kullaniciMap.put("parola", parola)
                            kullaniciMap.put("isAdmin", false) // Kullanıcı admin değil
                            kullaniciMap.put("kullaniciUID", guncelKullanici.uid) // Kullanıcı UID'sini ekle

                            //Kullanıcı bilgilerini Firestore'a kaydet
                            db.collection("kullaniciBilgileri")
                                .add(kullaniciMap) // Firestore'a ekle
                                .addOnFailureListener { exeption -> // Hata olursa
                                    Toast.makeText(requireContext(), exeption.localizedMessage, Toast.LENGTH_LONG).show()
                                }

                            //Kullanıcı başarıyla kaydedildiyse, kullanıcı anasayfasına yönlendir
                            val action = kayitOlFragmentDirections.actionKayitOlFragmentToKullaniciAnaSayfaFragment()
                            Navigation.findNavController(requireView()).navigate(action)
                        }
                    }
                }.addOnFailureListener { exeption ->
                    Toast.makeText(requireContext(), exeption.localizedMessage, Toast.LENGTH_LONG).show()
                }
        } else {
            Toast.makeText(requireContext(), "Lütfen boş alan bırakmayınız!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}