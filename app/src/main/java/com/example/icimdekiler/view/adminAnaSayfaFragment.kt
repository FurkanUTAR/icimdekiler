package com.example.icimdekiler.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.icimdekiler.R
import com.example.icimdekiler.databinding.FragmentAdminAnaSayfaBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class adminAnaSayfaFragment : Fragment() {

    // Binding
    private var _binding: FragmentAdminAnaSayfaBinding? = null
    private val binding get() = _binding!!

    // Firebase
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    private lateinit var permissionLauncherCamera: ActivityResultLauncher<String>
    private lateinit var activityResultLauncherCamera: ActivityResultLauncher<Intent>

    private lateinit var permissionLauncherGallery: ActivityResultLauncher<String>
    private lateinit var activityResultLauncherGallery: ActivityResultLauncher<Intent>

    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private var barkodNo: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        registerLauncherCamera()
        registerLauncherGallery()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAdminAnaSayfaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.popupMenu.setOnClickListener { view ->
            val popupMenu = PopupMenu(view.context, view)
            popupMenu.menuInflater.inflate(R.menu.menu_fab, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.cikisYap -> {
                        AlertDialog.Builder(view.context)
                            .setTitle(R.string.cikisYap)
                            .setMessage(R.string.cikisYapmakIstediginizdenEminMisiniz)
                            .setPositiveButton(R.string.evet) { dialog, value ->
                                auth.signOut()
                                findNavController().navigate(R.id.action_adminAnaSayfaFragment_to_girisYapFragment, null, NavOptions.Builder()
                                    .setPopUpTo(R.id.adminAnaSayfaFragment, true)
                                    .setLaunchSingleTop(true)
                                    .build()
                                )
                            }
                            .setNegativeButton(R.string.iptal, null)
                            .show()
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }

        binding.barkodOkuImageView.setOnClickListener {
            val secim = arrayOf(
                getString(R.string.kamera), // Kamera seçeneği
                getString(R.string.galeri)  // Galeri seçeneği
            )
            val alert = AlertDialog.Builder(requireContext())
            alert.setTitle(R.string.secimYap)
            alert.setItems(secim){ dialog, which ->
                if(which==0) barkodOkuKamera()
                else barkodOkuGaleri()
            }.show()
        }

        binding.araImage.setOnClickListener { urunAdiAra() }

        binding.ekleImage.setOnClickListener {
            val secim = arrayOf(
                getString(R.string.urunEkle),
                getString(R.string.icerikEkle)
            )
            val alert = AlertDialog.Builder(requireContext())
            alert.setTitle(R.string.secimYap)
            alert.setItems(secim) { dialog, which ->
                if (which == 0){
                    val action = adminAnaSayfaFragmentDirections.actionAdminAnaSayfaFragmentToUrunEkleFragment(durum = "yeni","","","" ,"","")
                    findNavController().navigate(action)
                }else{
                    val action = adminAnaSayfaFragmentDirections.actionAdminAnaSayfaFragmentToIcerikEkleFragment()
                    findNavController().navigate(action)
                }
            }.show()
        }

        binding.tumUrunlerButton.setOnClickListener {
            val action = adminAnaSayfaFragmentDirections.actionAdminAnaSayfaFragmentToAdminTumUrunlerFragment()
            findNavController().navigate(action)
        }
    }

    private fun urunAdiAra() {
        val urunAdiLowerCase = binding.urunAdiText.text.toString().lowercase().trim()

        db.collection("urunler")
            .whereEqualTo("urunAdiLowerCase", urunAdiLowerCase)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents.firstOrNull()
                    val documentId = document?.id ?: ""
                    barkodNo = document?.getString("barkodNo") ?: ""
                    val urunAdi = document?.getString("urunAdi") ?: ""
                    val icindekiler = document?.getString("icindekiler") ?: ""
                    val gorselUrl = document?.getString("gorselUrl") ?: ""

                    // 📌 Mevcut fragment’i kontrol et
                    val currentFragment = findNavController().currentDestination?.id
                    val targetFragment = R.id.urunEkleFragment

                    if (currentFragment != targetFragment) {
                        val action = adminAnaSayfaFragmentDirections.actionAdminAnaSayfaFragmentToUrunEkleFragment("eski", barkodNo, urunAdi, icindekiler, gorselUrl, documentId)
                        findNavController().navigate(action)
                    } else Log.d("NavigationDebug", "Zaten urunEkleFragment içindesin, tekrar yönlendirme yapılmadı.")
                } else Toast.makeText(requireContext(), R.string.urunBulunamadi, Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception -> Toast.makeText(requireContext(), exception.localizedMessage, Toast.LENGTH_LONG).show() }
    }

    private fun barkodNoAra() {
        db.collection("urunler")
            .whereEqualTo("barkodNo", barkodNo)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents.firstOrNull()
                    var documentId= document?.id ?: ""
                    val urunAdi = document?.getString("urunAdi") ?: ""
                    val icindekiler = document?.getString("icindekiler") ?: ""
                    val gorselUrl = document?.getString("gorselUrl") ?: ""

                    val action = adminAnaSayfaFragmentDirections.actionAdminAnaSayfaFragmentToUrunEkleFragment("eski", barkodNo, urunAdi, icindekiler,gorselUrl, documentId.toString())
                    findNavController().navigate(action)
                } else {
                    val action = adminAnaSayfaFragmentDirections.actionAdminAnaSayfaFragmentToUrunEkleFragment("yeni", barkodNo, "", "","","")
                    findNavController().navigate(action)
                    Toast.makeText(requireContext(), R.string.urunBulunamadi, Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { exeption -> Toast.makeText(requireContext(), exeption.localizedMessage, Toast.LENGTH_SHORT).show() }
    }

    private fun barkodOkuGaleri() {
        if(Build.VERSION.SDK_INT >= 33){
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.READ_MEDIA_IMAGES)) {
                    Snackbar.make(requireView(), R.string.barkodOkumakIcinGaleriyeErisimIzniGerekli, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.izinVer) {
                            permissionLauncherGallery.launch(Manifest.permission.READ_MEDIA_IMAGES)
                        }.show()
                } else {
                    permissionLauncherGallery.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
            } else {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncherGallery.launch(intent)
            }
        }else{
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    Snackbar.make(requireView(), R.string.barkodOkumakIcinGaleriyeErisimIzniGerekli, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.izinVer) {
                            permissionLauncherGallery.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }.show()
                } else {
                    permissionLauncherGallery.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            } else {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncherGallery.launch(intent)
            }
        }
    }

    private fun barkodOkuKamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.CAMERA)) {
                Snackbar.make(requireView(), R.string.barkodOkumakIcinKamerayaErisimIzniGerekli, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.izinVer) {
                        permissionLauncherCamera.launch(Manifest.permission.CAMERA)
                    }.show()
            } else permissionLauncherCamera.launch(Manifest.permission.CAMERA)
        } else {
            val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
            activityResultLauncherCamera.launch(intent)
        }
    }

    private fun registerLauncherGallery() {
        activityResultLauncherGallery = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                val imageUri = result.data?.data
                if (imageUri!=null){
                    val image = InputImage.fromFilePath(requireContext(), imageUri)
                    val scanner = BarcodeScanning.getClient()

                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            if (barcodes.isNotEmpty()) {
                                for (barcode in barcodes) {
                                    val barkod = barcode.displayValue
                                    if (barkod != null) {
                                        barkodNo = barkod
                                        barkodNoAra()
                                        break // İlk barkodu alınca döngüden çık
                                    }
                                }
                            } else Toast.makeText(requireContext(), R.string.barkodOkunamadi, Toast.LENGTH_SHORT).show()
                        }.addOnFailureListener { e -> Toast.makeText(requireContext(), e.localizedMessage, Toast.LENGTH_SHORT).show() }
                } else Toast.makeText(requireContext(), R.string.gorselBulunamadi, Toast.LENGTH_SHORT).show()
            }
        }

        permissionLauncherGallery = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            if (result) {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncherGallery.launch(intent)
            } else Toast.makeText(requireContext(), R.string.galeriIzniVerilmedi, Toast.LENGTH_SHORT).show()
        }
    }

    private fun registerLauncherCamera() {
        activityResultLauncherCamera = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as? Bitmap
                if (imageBitmap != null) {
                    val image = InputImage.fromBitmap(imageBitmap, 0)
                    val scanner = BarcodeScanning.getClient()

                    cameraExecutor.execute {
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                    if (barcodes.isNotEmpty()) {
                                        for (barcode in barcodes) {
                                            val barkod = barcode.displayValue
                                            if (!barkod.isNullOrBlank()) {
                                                barkodNo = barkod
                                                barkodNoAra()
                                                break
                                            }
                                        }
                                    } else Toast.makeText(requireContext(), R.string.barkodOkunamadi, Toast.LENGTH_SHORT).show()
                            }.addOnFailureListener { e -> Toast.makeText(requireContext(), e.localizedMessage, Toast.LENGTH_SHORT).show() }
                    }
                }else Toast.makeText(requireContext(), R.string.gorselBulunamadi, Toast.LENGTH_SHORT).show()
            }
        }

        permissionLauncherCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            if (result) {
                val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                activityResultLauncherCamera.launch(intent)
            }
            else Toast.makeText(requireContext(), R.string.kameraIzniVerilmedi, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        cameraExecutor.shutdown()
    }
}