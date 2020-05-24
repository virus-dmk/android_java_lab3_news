package by.bychko.contactshare

import android.Manifest
import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.Intent
import android.content.OperationApplicationException
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.RemoteException
import android.provider.ContactsContract
import android.provider.ContactsContract.*
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.Contacts.HAS_PHONE_NUMBER
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.zxing.Result
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.android.synthetic.main.activity_main.*
import me.dm7.barcodescanner.zxing.ZXingScannerView
import org.json.JSONTokener
import java.lang.Exception


class MainActivity : AppCompatActivity(), ZXingScannerView.ResultHandler {
    companion object {
        const val PERMISSIONS_REQUEST_READ_CONTACTS = 100
        const val PERMISSIONS_REQUEST_WRITE_CONTACTS = 200
    }

    private var mScannerView: ZXingScannerView? = null

    private lateinit var adapter: AllContactsAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val contacts = loadContacts()
        adapter = AllContactsAdapter(this, contacts)
        val rv = findViewById<RecyclerView>(R.id.contacts_recycler_view)

        rv.layoutManager = LinearLayoutManager(this)
        rv.itemAnimator = DefaultItemAnimator()
        rv.adapter = adapter

        scan_button.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_CONTACTS),
                    PERMISSIONS_REQUEST_WRITE_CONTACTS
                )
                true
            } else {


                val integrator = IntentIntegrator(this)
                integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE) // optional

                integrator.setOrientationLocked(false) // allow barcode scanner in potrait mode

                integrator.initiateScan()
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()
        val contacts = loadContacts()
        adapter = AllContactsAdapter(this, contacts)
        val rv = findViewById<RecyclerView>(R.id.contacts_recycler_view)
        rv.layoutManager = LinearLayoutManager(this)
        rv.itemAnimator = DefaultItemAnimator()
        rv.adapter = adapter
    }

    private fun loadContacts(): ArrayList<User> {

        if (checkSelfPermission(
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.READ_CONTACTS),
                PERMISSIONS_REQUEST_READ_CONTACTS
            )
            requestPermissions(
                arrayOf(Manifest.permission.WRITE_CONTACTS),
                PERMISSIONS_REQUEST_READ_CONTACTS
            )
            return ArrayList()
            //callback onRequestPermissionsRes
            // return _root_ide_package_.kotlin.collections.listOf<>()ult
        } else {
            return getContacts()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadContacts()
            } else {
                toast("Permission must be granted in order to display contacts information")
            }
        }
    }

    private fun getContacts(): ArrayList<User> {
        val resolver: ContentResolver = contentResolver;
        val cursor = resolver.query(
            ContactsContract.Contacts.CONTENT_URI, null, null, null,
            null
        )
        val listOfContacts = ArrayList<User>()
        if (cursor!!.count > 0) {
            while (cursor.moveToNext()) {
                val id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID))
                val name =
                    cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                if (cursor.getInt(cursor.getColumnIndex(HAS_PHONE_NUMBER)) >= 1) {
                    val cursorPhone = contentResolver.query(
                        Phone.CONTENT_URI,
                        null,
                        Phone.CONTACT_ID + "=?",
                        arrayOf(id),
                        null
                    )
                    var phone = ""
                    if (cursorPhone!!.count > 0) {
                        cursorPhone.moveToNext()
                        phone = cursorPhone.getString(
                            cursorPhone.getColumnIndex(Phone.NUMBER)
                        )
                    }

                    cursorPhone.close()
                    listOfContacts.add(User(name = name, phone = phone))
                }
            }
        } else {
            toast("No contacts available!")
        }
        cursor.close()
        return listOfContacts
    }

    fun toast(message: String) {
        Toast.makeText(
            this, message,
            Toast.LENGTH_LONG
        ).show()
    }

    // Get the results:
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        val result =
            IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
            } else {
                var user: User? = null
                try {
                    user = Gson().fromJson(JSONTokener(result.contents).nextValue().toString(), User::class.java)
                } catch (e: Exception) {
                    Toast.makeText(this, "Чтото пошло не так", Toast.LENGTH_LONG).show()
                }
                Toast.makeText(this, "Scanned: " + result.contents, Toast.LENGTH_LONG).show()


                //SAVE TO CONTACTS
                val ops = ArrayList<ContentProviderOperation>()
                val rawContactInsertIndex: Int = ops.size
                ops.add(
                    ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                        .withValue(RawContacts.ACCOUNT_TYPE, null)
                        .withValue(RawContacts.ACCOUNT_NAME, null).build()
                )
                ops.add(
                    ContentProviderOperation
                        .newInsert(Data.CONTENT_URI)
                        .withValueBackReference(
                            Data.RAW_CONTACT_ID,
                            rawContactInsertIndex
                        )
                        .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                        .withValue(
                            StructuredName.DISPLAY_NAME,
                            user?.name
                        ) // Name of the person
                        .build()
                )
                ops.add(
                    ContentProviderOperation
                        .newInsert(Data.CONTENT_URI)
                        .withValueBackReference(
                            Data.RAW_CONTACT_ID, rawContactInsertIndex
                        )
                        .withValue(
                            Data.MIMETYPE,
                            Phone.CONTENT_ITEM_TYPE
                        )
                        .withValue(Phone.NUMBER, user?.phone) // Number of the person
                        .withValue(Phone.TYPE, Phone.TYPE_MOBILE)
                        .build()
                ) // Type of mobile number

                try {
                    val res =
                        contentResolver.applyBatch(AUTHORITY, ops)
                } catch (e: RemoteException) {
                    // error
                } catch (e: OperationApplicationException) {
                    // error
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun handleResult(rawResult: Result?) {
        // Do something with the result here
        Log.i("tag", rawResult?.text); // Prints scan results
        Log.i(
            "tag",
            rawResult?.barcodeFormat?.name
        ); // Prints the scan format (qrcode, pdf417 etc.)

        // If you would like to resume scanning, call this method below:
        mScannerView?.resumeCameraPreview(this); }
}
