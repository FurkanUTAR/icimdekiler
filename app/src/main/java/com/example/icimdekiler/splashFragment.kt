package com.example.icimdekiler

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import com.example.icimdekiler.databinding.FragmentSplashBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class splashFragment : Fragment() {

    //Binding
    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    //Firebase
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Kullanıcı bilgileri Firestore'dan çek
            db.collection("kullaniciBilgileri")
                .whereEqualTo("kullaniciUID", currentUser.uid)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val kullanici = documents.documents.first()
                        val isAdmin = kullanici.getBoolean("isAdmin") ?: false

                        // Yönlendirme işlemi
                        if (isAdmin){
                            val action=splashFragmentDirections.actionSplashFragmentToAdminAnaSayfaFragment()
                            Navigation.findNavController(requireView()).navigate(action)
                        } else {
                            val action=splashFragmentDirections.actionSplashFragmentToKullaniciAnaSayfaFragment()
                            Navigation.findNavController(requireView()).navigate(action)
                        }
                    } else {
                        val action = splashFragmentDirections.actionSplashFragmentToGirisYapFragment()
                        Navigation.findNavController(requireView()).navigate(action)
                    }
                }.addOnFailureListener {
                    val action = splashFragmentDirections.actionSplashFragmentToGirisYapFragment()
                    Navigation.findNavController(requireView()).navigate(action)
                }
        } else {
            val action = splashFragmentDirections.actionSplashFragmentToGirisYapFragment()
            Navigation.findNavController(requireView()).navigate(action)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}