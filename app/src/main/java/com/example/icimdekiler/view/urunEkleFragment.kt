package com.example.icimdekiler.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.icimdekiler.databinding.FragmentUrunEkleBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class urunEkleFragment : Fragment() {

    private var _binding: FragmentUrunEkleBinding? = null
    private val binding get() = _binding!!
    val db = Firebase.firestore

    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraExecutor: ExecutorService

    private val icerikListesi = ArrayList<String>()
    private lateinit var icerikAdapter:ArrayAdapter<String>

    private lateinit var icindekilerAdapter: ArrayAdapter<String>
    private val icindekilerListesi = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        registerLauncher()

        icerikAl()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUrunEkleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        icindekiler()

        binding.barkodOkuImage.setOnClickListener {
            barkodOku()
        }

        binding.ekleImage.setOnClickListener {
            icindekilerListesi.add(icerikListesi[binding.icerikSpinner.selectedItemPosition])
            // Adapter'a değişikliği bildir
            icindekilerAdapter.notifyDataSetChanged()
        }

        binding.kaydetButton.setOnClickListener {
            val barkodNo = binding.barkodNoText.text.toString().trim()
            val urunAdi = binding.urunAdiText.text.toString().trim()
            val urunAdiLowerCase = binding.urunAdiText.text.toString().lowercase().trim()
            val birlesikIcindekiler = icindekilerListesi.joinToString(", ").trim()

            val urunMap = hashMapOf<String, Any>()
            urunMap["urunAdi"] = urunAdi
            urunMap.put("urunAdiLowerCase",urunAdiLowerCase)
            urunMap.put("barkodNo",barkodNo)
            urunMap.put("icindekiler",birlesikIcindekiler)

            db.collection("urunler")
                .add(urunMap)
                .addOnFailureListener { exeption ->
                    Toast.makeText(requireContext(),exeption.localizedMessage, Toast.LENGTH_SHORT).show()
                }

        }
    }

    private fun icerikAl(){
        db.collection("icerik")
            .addSnapshotListener { value, error ->
                if (error != null){
                    Toast.makeText(requireContext(), error.localizedMessage, Toast.LENGTH_LONG).show()
                } else {
                    if (value != null){
                        if (!value.isEmpty){
                            val documents = value.documents

                            icerikListesi.clear()

                            for (document in documents){
                                val urun=document.get("urun") as String

                                icerikListesi.add(urun)
                                icerikAdapter = ArrayAdapter(requireContext(),android.R.layout.simple_list_item_1,android.R.id.text1,icerikListesi)
                                binding.icerikSpinner.adapter = icerikAdapter
                            }
                        }
                    }
                }
            }
    }

    private fun icindekiler(){
        icindekilerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, icindekilerListesi)
        binding.icindekilerListView.adapter = icindekilerAdapter
    }

    private fun barkodOku() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.CAMERA)) {
                Snackbar.make(requireView(), "Barkod okumak için kameraya erişim izni gerekli!", Snackbar.LENGTH_INDEFINITE)
                    .setAction("İzin Ver") {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }.show()
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        } else {
            pickImageCamera()
        }
    }

    private fun registerLauncher() {
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as Bitmap
                val image = InputImage.fromBitmap(imageBitmap, 0)
                val scanner = BarcodeScanning.getClient()

                cameraExecutor.execute {
                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            if (barcodes.isNotEmpty()) {
                                for (barcode in barcodes) {
                                    val barkod = barcode.displayValue
                                    if (barkod != null) {
                                        binding.barkodNoText.setText(barkod)
                                        break // Tek barkod okutulduğunda döngüden çık
                                    }
                                }
                            } else {
                                Toast.makeText(requireContext(), "Barkod Okunamadı!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(),e.localizedMessage, Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            if (result) {
                pickImageCamera()
            } else {
                Toast.makeText(requireContext(), "Kamera izni verilmedi!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun pickImageCamera() {
        val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        activityResultLauncher.launch(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        cameraExecutor.shutdown()
    }
}
