package by.bychko.contactshare

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.android.synthetic.main.activity_main.*


class AllContactsAdapter(
    private val mContext: Context,
    private val contactVOList: List<User>
) :
    RecyclerView.Adapter<AllContactsAdapter.ContactViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ContactViewHolder {
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.single_contact_item, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contactVO: User = contactVOList[position]
        holder.tvContactName.text = contactVO.name
        holder.tvPhoneNumber.text = contactVO.phone
        holder.itemView.setOnClickListener {
            Toast.makeText(mContext, "QR готов", Toast.LENGTH_SHORT).show()
            val writer = QRCodeWriter()
            val hints = mapOf<EncodeHintType, Any>(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.CHARACTER_SET to "utf-8" )
            val bitMatrix =
                writer.encode(
                    Gson().toJson(Gson().toJson(contactVO)),
                    BarcodeFormat.QR_CODE,
                    512,
                    512
                    ,hints
                )
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            (mContext as AppCompatActivity).imageView.setImageBitmap(bitmap)

            true
        }
    }

    override fun getItemCount(): Int {
        return contactVOList.size
    }

    class ContactViewHolder(itemView: View) : ViewHolder(itemView) {
        var tvContactName: TextView = itemView.findViewById(R.id.firstline)
        var tvPhoneNumber: TextView = itemView.findViewById(R.id.secondline)
    }

}