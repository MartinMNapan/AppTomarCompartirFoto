package pe.idat.edu.appclasssemana6_v1

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import pe.idat.edu.appclasssemana6_v1.databinding.ActivityMainBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.jvm.Throws
import kotlin.math.min

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityMainBinding
    private var rutaFotoActual: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnCompartir.setOnClickListener(this)
        binding.btnTomarFoto.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        when(view.id){
            R.id.btnTomarFoto -> tomarFoto()
            R.id.btnCompartir -> compartirFoto()
        }
    }

    private fun tomarFoto() {
        if (validarPermisoAlmacenamieto()){
            try {
                intencionTomarFoto()
            }catch(e:IOException){
                e.printStackTrace()
            }
        }else{
            solicitarPermiso()
        }
    }

    @Throws(IOException::class)
    private fun intencionTomarFoto() {
        val tomarFotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (tomarFotoIntent.resolveActivity(this.packageManager)!=null){
            val archivoFoto = crearArchivoTemporal()
            if (archivoFoto != null){
                val uirFoto = obtenerUri(archivoFoto)
                tomarFotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, uirFoto)
                getResult.launch(tomarFotoIntent)
            }
        }
    }

    private val getResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()){ result ->
        if(result.resultCode == Activity.RESULT_OK){
            mostrarFoto()
        }
    }

    private fun compartirFoto() {
        if (rutaFotoActual != ""){
            val uriFoto = obtenerUri(File(rutaFotoActual))
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uriFoto)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                type = "image/jpeg"
            }
            val chooser: Intent = Intent.createChooser(sendIntent, "Compartir imagen")
            if (sendIntent.resolveActivity(packageManager) != null){
                startActivity(chooser)
            }
        }else{
            Toast.makeText(applicationContext, "Debe tomar una foto para compartir", Toast.LENGTH_LONG).show()
        }
    }

    private fun validarPermisoAlmacenamieto(): Boolean{
        val resultado = ContextCompat.checkSelfPermission(
            applicationContext, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        var permiso = false
        if (resultado == PackageManager.PERMISSION_GRANTED) permiso = true
        return permiso
    }

    private fun solicitarPermiso(){
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
            1999
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode == 1999){
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                intencionTomarFoto()
            }else{
                Toast.makeText(applicationContext, "Permiso denegado, no puede utilizar la funcion",
                    Toast.LENGTH_LONG).show()
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun mostrarFoto(){
        val targetW: Int = binding.ivFoto.width
        val targetH: Int = binding.ivFoto.height
        val bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = true
        BitmapFactory.decodeFile(rutaFotoActual, bmOptions)
        val fotoW = bmOptions.outWidth
        val fotoH = bmOptions.outHeight
        val escala = min(fotoW/ targetW, fotoH / targetH)
        bmOptions.inSampleSize = escala
        bmOptions.inJustDecodeBounds = false
        val bitmap = BitmapFactory.decodeFile(rutaFotoActual, bmOptions)
        binding.ivFoto.setImageBitmap(bitmap)
    }

    @Throws(IOException::class)
    private fun crearArchivoTemporal(): File{
        val nombreArchivo: String = "JPEG_"+
                SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val directorioArchivo: File = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        val archivoTemporal: File = File.createTempFile(nombreArchivo, ".jpg", directorioArchivo)
        rutaFotoActual = archivoTemporal.absolutePath
        return archivoTemporal
    }

    private fun obtenerUri(archivo: File) : Uri{
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            FileProvider.getUriForFile(applicationContext,
                "pe.idat.edu.appclasssemana6_v1.fileprovider", archivo)
        }else{
            Uri.fromFile(archivo)
        }
    }
}