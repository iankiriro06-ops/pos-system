package com.example.possystem.data

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.possystem.models.ProductModel
import com.example.possystem.navigation.ROUTE_DASHBOARD
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.InputStream

class ProductViewModel : ViewModel() {

    val cloudinaryUrl = "https://api.cloudinary.com/v1_1/dugdhsx1k/image/upload"
    val uploadPreset = "images_folder"

    fun uploadProduct(
        imageUrl: Uri?,
        product_name: String,
        price: String,
        quantity: String,
        description: String,
        date_of_manufacture: String,
        barcode_number: String,
        context: Context,
        navController: NavController
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val imageUrl = imageUrl?.let { uploadToCloudinary(context, it) }

                val ref = FirebaseDatabase.getInstance().getReference("Products").push()
                val productData = mapOf(
                    "id" to ref.key,
                    "product name" to product_name,
                    "price" to price,
                    "quantity" to quantity,
                    "description" to description,
                    "imageUrl" to imageUrl,
                    "date_of_manufacture" to date_of_manufacture,
                    "barcode_number" to barcode_number
                )

                ref.setValue(productData).await()

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Product saved successfully", Toast.LENGTH_LONG).show()
                    navController.navigate(ROUTE_DASHBOARD)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Product not saved: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun uploadToCloudinary(context: Context, uri: Uri): String {
        val contentResolver = context.contentResolver
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val fileBytes = inputStream?.readBytes() ?: throw Exception("Image read failed")

        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart(
                "file", "image.jpg",
                RequestBody.create("image/*".toMediaTypeOrNull(), fileBytes)
            )
            .addFormDataPart("upload_preset", uploadPreset)
            .build()

        val request = Request.Builder()
            .url(cloudinaryUrl)
            .post(requestBody)
            .build()

        val client = OkHttpClient()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) throw Exception("Upload failed: ${response.code}")

        val responseBody = response.body?.string()
        val secureUrl = Regex("\"secure_url\":\"(.*?)\"")
            .find(responseBody ?: "")?.groupValues?.get(1)

        return secureUrl ?: throw Exception("Failed to get image URL")
    }
    private val _products = mutableStateListOf<ProductModel>()
    val products:List<ProductModel> = _products
    fun fetchProduct(context: Context){
        val ref = FirebaseDatabase.getInstance().getReference("Products")
        ref.get().addOnSuccessListener { snapshot -> _products.clear()
        for(child in snapshot.children){
            val product = child.getValue(ProductModel::class.java)
            product?.let{
                it.id = child.key
                _products.add(it)
            }
        }}.addOnFailureListener { Toast.makeText(context,"Failed to load products", Toast.LENGTH_LONG) }
    }
    fun updateProduct(productId: String,imageUri: Uri?,product_name: String,price: String,quantity:String,description:String,date_of_manufacture: String, barcode_number: String,context:Context,
                      navController: NavController,){
        viewModelScope.launch (Dispatchers.IO){
            try {
                val imageUrl = imageUri?.let{ uploadToCloudinary(context,it) }
                val  updateProduct = mapOf(
                    "id" to product_name,
                    "product name" to product_name,
                    "price" to price,
                    "quantity" to quantity,
                    "description" to description,
                    "imageUrl" to imageUrl,
                    "date_of_manufacture" to date_of_manufacture,
                    "barcode_number" to barcode_number

                )
                val ref = FirebaseDatabase.getInstance().getReference("Products").child(productId)
                ref.setValue(updateProduct).await()
                fetchProduct(context)
                withContext(Dispatchers.Main){
                    Toast.makeText(context,"Product updated successfully", Toast.LENGTH_LONG).show()
                    navController.navigate(ROUTE_DASHBOARD)
                }

        } catch (e: Exception){withContext(Dispatchers.Main){
            Toast.makeText(context,"Update failed", Toast.LENGTH_LONG).show()
        }}
    }
}
}








