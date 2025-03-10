package com.example.icimdekiler.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.example.icimdekiler.adapter.UrunlerAdapter
import com.example.icimdekiler.databinding.FragmentKullaniciTumUrunlerBinding
import com.example.icimdekiler.model.Urunler
import com.google.firebase.Firebase
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore


class kullaniciTumUrunlerFragment : Fragment() {

    //Binding
    private var _binding: FragmentKullaniciTumUrunlerBinding? = null
    private val binding get() = _binding!!

    //Firebase
    private val db = Firebase.firestore

    private var urunListesi=ArrayList<Urunler>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding= FragmentKullaniciTumUrunlerBinding.inflate(inflater,container,false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        urunleriAl()

        binding.araImage.setOnClickListener { urunAra() }
    }

    private fun urunleriAl() {
        db.collection("urunler")
            .orderBy("urunAdi", Query.Direction.ASCENDING)
            .limit(30)
            .addSnapshotListener { value, error ->
                if (!isAdded || isDetached) return@addSnapshotListener

                if (error != null) {
                    Toast.makeText(requireContext(), error.localizedMessage, Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                if (value != null && !value.isEmpty) {
                    urunListesi.clear()
                    for (document in value.documents) {
                        val barkodNo = document.getString("barkodNo") ?: ""
                        val urunAdi = document.getString("urunAdi") ?: ""
                        val icindekiler = document.getString("icindekiler") ?: ""

                        if (barkodNo.isNotEmpty() && urunAdi.isNotEmpty()) {
                            val indirilenUrun = Urunler(barkodNo, urunAdi, icindekiler)
                            urunListesi.add(indirilenUrun)
                        }
                    }

                    if (isAdded && !isDetached) {
                        val adapter = UrunlerAdapter(urunListesi, "kullanici")
                        binding.urunlerRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
                        binding.urunlerRecyclerView.adapter = adapter
                    }
                } else {
                    Toast.makeText(requireContext(), "Ürün bulunamadı", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun urunAra() {
        val urun = binding.urunAdiText.text.toString().trim()

        if (urun.isNotEmpty()) {
            db.collection("urunler")
                .orderBy("urunAdi")
                .startAt(urun)
                .endAt(urun + "\uf8ff")
                .addSnapshotListener { value, error ->
                    if (error != null) {
                        Toast.makeText(requireContext(), error.localizedMessage, Toast.LENGTH_LONG).show()
                        return@addSnapshotListener
                    } else {
                        if (value != null && !value.isEmpty) {
                            urunListesi.clear()
                            for (document in value.documents) {
                                var barkodNo = document.getString("barkodNo") ?: ""
                                var urunAdi = document.getString("urunAdi") ?: ""
                                var icindekiler = document.getString("icindekiler") ?: ""

                                val indirilenUrun = Urunler(barkodNo, urunAdi, icindekiler)
                                urunListesi.add(indirilenUrun)
                            }

                            val adapter = UrunlerAdapter(urunListesi,"kullanici")
                            binding.urunlerRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
                            binding.urunlerRecyclerView.adapter = adapter
                        } else {
                            urunListesi.clear()
                            binding.urunlerRecyclerView.adapter?.notifyDataSetChanged()
                            return@addSnapshotListener
                        }
                    }
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
       // _binding=null
    }
}