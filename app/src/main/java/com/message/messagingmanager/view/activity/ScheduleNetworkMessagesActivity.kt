package com.message.messagingmanager.view.activity

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.message.messagingmanager.HomeActivity
import com.message.messagingmanager.R
import com.message.messagingmanager.WhatsappAccessibilityService
import com.message.messagingmanager.model.Contacts
import com.message.messagingmanager.model.Message
import com.message.messagingmanager.view.receiver.AlertReceiver
import kotlinx.android.synthetic.main.activity_schedule_network_messages.*
import kotlinx.android.synthetic.main.app_bar.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class ScheduleNetworkMessagesActivity : AppCompatActivity() {

    private var databaseReferenceMsg: DatabaseReference =
        FirebaseDatabase.getInstance().getReference("Messages")

    private val PERMISSION_REQUEST_SEND_SMS = 1
    private val PERMISSION_REQUEST_READ_CONTACTS = 2

    private var calender: DatePickerDialog.OnDateSetListener? = null
    private var timePickerDialog: TimePickerDialog? = null

    private var date: Date? = null
    private var myDateCheck: Date? = null
    private var mHour = 0
    private var mMin = 0
    private var hours1 = 0
    private var min1 = 0
    private var year1 = 0
    private var month1 = 0
    private var dayOfMonth1 = 0

    private lateinit var calendarAlarm: Calendar

    private var simName: String = ""
    private var simPrefix: String = ""

    @SuppressLint("SetTextI18n")
    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule_network_messages)

        toolbar.title = "Schedule Network Messages"
        setSupportActionBar(toolbar)

        simName = intent.getStringExtra("SIMName")
        simPrefix = intent.getStringExtra("SIMPrefix")
        txtViewNetworkName.text = simName

        /*** date ***/
        editTxtDate.setOnClickListener {
            val cal = Calendar.getInstance()
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val dialog = DatePickerDialog(
                this@ScheduleNetworkMessagesActivity,
                calender, year, month, day
            )
            dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.WHITE))
            dialog.show()
        }
        calender = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            @Suppress("NAME_SHADOWING")
            var month = month
            month += 1

            year1 = year
            month1 = month
            dayOfMonth1 = dayOfMonth

            val startDate = "$dayOfMonth/$month/$year"
            val time = SimpleDateFormat("dd/MM/yyyy").format(Calendar.getInstance().time)
            val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

            try {
                date = format.parse(time)
                myDateCheck = format.parse(startDate)
            } catch (e: ParseException) {
                e.printStackTrace()
            }

            when {
                myDateCheck == null -> try {
                    myDateCheck = format.parse(time)
                } catch (e: ParseException) {
                    e.printStackTrace()
                }
                myDateCheck!!.before(date) -> Toast.makeText(
                    this@ScheduleNetworkMessagesActivity,
                    "Please, Enter a valid Date!", Toast.LENGTH_LONG
                ).show()
                else -> {
                    editTxtDate.setText(startDate)
                    editTxtTime.setText("")
                }
            }
        }

        /*** time ***/
        editTxtTime.setOnClickListener {
            val c = Calendar.getInstance()
            mHour = c.get(Calendar.HOUR_OF_DAY)
            mMin = c.get(Calendar.MINUTE)

            timePickerDialog = TimePickerDialog(
                this@ScheduleNetworkMessagesActivity,
                TimePickerDialog.OnTimeSetListener { _, hourOfDay, minutes ->
                    val myCalInstance = Calendar.getInstance()
                    val myRealCalender = Calendar.getInstance()

                    if (myDateCheck == null) {
                        val timeStamp =
                            SimpleDateFormat("dd/MM/yyyy").format(Calendar.getInstance().time)
                        val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        try {
                            myDateCheck = format.parse(timeStamp)
                        } catch (e: ParseException) {
                            e.printStackTrace()
                        }

                    }

                    myRealCalender.time = myDateCheck
                    myRealCalender.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    myRealCalender.set(Calendar.MINUTE, minutes)

                    if (myRealCalender.time.before(myCalInstance.time)) {
                        Toast.makeText(
                            this@ScheduleNetworkMessagesActivity,
                            "Please, Enter a valid Time!", Toast.LENGTH_LONG
                        ).show()
                    } else {
                        hours1 = hourOfDay
                        min1 = minutes
                        calendarAlarm = Calendar.getInstance()
                        calendarAlarm.set(Calendar.YEAR, year1)
                        calendarAlarm.set(Calendar.MONTH, month1 - 1)
                        calendarAlarm.set(Calendar.DAY_OF_MONTH, dayOfMonth1)
                        calendarAlarm.set(Calendar.HOUR_OF_DAY, hours1)
                        calendarAlarm.set(Calendar.MINUTE, min1)
                        calendarAlarm.set(Calendar.SECOND, 0)
                        if (hourOfDay < 10 && minutes >= 10) {
                            editTxtTime.setText("0$hourOfDay:$minutes")
                        } else if (hourOfDay < 10 && minutes < 10) {
                            editTxtTime.setText("0$hourOfDay:0$minutes")
                        } else if (hourOfDay >= 10 && minutes < 10) {
                            editTxtTime.setText("$hourOfDay:0$minutes")
                        } else if (hourOfDay >= 10 && minutes >= 10) {
                            editTxtTime.setText("$hourOfDay:$minutes")
                        }
                    }
                }, mHour, mMin, false
            )
            timePickerDialog!!.show()
        }

        btnSMS.setOnClickListener {
            val permissionCheck: Int = ContextCompat.checkSelfPermission(this@ScheduleNetworkMessagesActivity, Manifest.permission.SEND_SMS)

            val permissionCheck1: Int = ContextCompat.checkSelfPermission(this@ScheduleNetworkMessagesActivity, Manifest.permission.READ_CONTACTS)
            if (permissionCheck1 == PackageManager.PERMISSION_GRANTED){
                if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                    scheduleSMSMessage()
                } else {
                    ActivityCompat.requestPermissions(
                        this@ScheduleNetworkMessagesActivity, arrayOf(Manifest.permission.SEND_SMS),
                        this.PERMISSION_REQUEST_SEND_SMS
                    )
                }
            } else {
                ActivityCompat.requestPermissions(this@ScheduleNetworkMessagesActivity, arrayOf(Manifest.permission.READ_CONTACTS), PERMISSION_REQUEST_READ_CONTACTS)
            }
        }

        btnWhatsApp.setOnClickListener {
            val permissionCheck: Int = ContextCompat.checkSelfPermission(this@ScheduleNetworkMessagesActivity, Manifest.permission.READ_CONTACTS)

            if (permissionCheck == PackageManager.PERMISSION_GRANTED){
                if (isAccessibilityOn(this@ScheduleNetworkMessagesActivity, WhatsappAccessibilityService::class.java)) {
                    scheduleWhatsAppMessage()
                } else {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            } else {
                ActivityCompat.requestPermissions(this@ScheduleNetworkMessagesActivity, arrayOf(Manifest.permission.READ_CONTACTS), PERMISSION_REQUEST_READ_CONTACTS)
            }
        }
    }

    private fun isAccessibilityOn(context: Context, clazz: Class<out AccessibilityService>): Boolean {
        var accessibilityEnabled = 0
        val service = context.packageName + "/" + clazz.canonicalName
        try {
            accessibilityEnabled = Settings.Secure.getInt(context.applicationContext.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (ignored: Settings.SettingNotFoundException) {

        }

        val colonSplitter = TextUtils.SimpleStringSplitter(':')

        if (accessibilityEnabled == 1) {
            val settingValue = Settings.Secure.getString(context.applicationContext.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )

            if (settingValue != null) {
                colonSplitter.setString(settingValue)
                while (colonSplitter.hasNext()) {
                    val accessibilityService = colonSplitter.next()

                    if (accessibilityService.equals(service, ignoreCase = true)) {
                        return true
                    }
                }
            }
        }

        return false
    }

    private var cursorSms: Cursor? = null
    private var arrSMSContacts: ArrayList<Contacts> = ArrayList()
    private fun scheduleSMSMessage() {

        if (validateInputs()) {
            progressBar.visibility = View.VISIBLE

            // read contacts that match prefix
            cursorSms = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                null,
                null,
                ContactsContract.Contacts.DISPLAY_NAME + " ASC")

            while (cursorSms!!.moveToNext()) {
                val name = cursorSms!!.getString(cursorSms!!.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                val phoneNumber = cursorSms!!.getString(cursorSms!!.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))

                if (phoneNumber.startsWith(simPrefix) || phoneNumber.startsWith(simPrefix.subSequence(2, 5))){
                    if (phoneNumber.startsWith("+2")) {
                        val contact = Contacts(name, phoneNumber)
                        if (arrSMSContacts.size > 0) {
                            if (arrSMSContacts[arrSMSContacts.size - 1].getName() != name || !arrSMSContacts[arrSMSContacts.size - 1].getPhone().startsWith(phoneNumber.subSequence(0, 3))){
                                arrSMSContacts.add(contact)
                            }
                        } else {
                            arrSMSContacts.add(contact)
                        }
                    } else {
                        val contact = Contacts(name, "+2$phoneNumber")
                        if (arrSMSContacts.size > 0) {
                            if (arrSMSContacts[arrSMSContacts.size - 1].getName() != name || !arrSMSContacts[arrSMSContacts.size - 1].getPhone().startsWith("+2" + phoneNumber.subSequence(0, 3))){
                                arrSMSContacts.add(contact)
                            }
                        } else {
                            arrSMSContacts.add(contact)
                        }
                    }
                }
            }
            cursorSms!!.close()

            var calenderValue = calendarAlarm.timeInMillis

            for (item in arrSMSContacts.indices) {
                val smsId: String = databaseReferenceMsg.push().key.toString()
                val message = Message(smsId, arrSMSContacts[item].getName(), arrSMSContacts[item].getPhone(),
                    editTxtMessage.text.toString().trim(), editTxtDate.text.toString().trim(),
                    editTxtTime.text.toString().trim(), resources.getString(R.string.status_upcoming),
                    resources.getString(R.string.type_sms), FirebaseAuth.getInstance().currentUser!!.uid, calenderValue)
                databaseReferenceMsg.child(smsId).setValue(message)

                setSMSAlarm(smsId, arrSMSContacts[item].getName(), arrSMSContacts[item].getPhone(),
                    editTxtMessage.text.toString().trim(), editTxtDate.text.toString().trim(),
                    editTxtTime.text.toString().trim(), resources.getString(R.string.status_upcoming),
                    resources.getString(R.string.type_sms), FirebaseAuth.getInstance().currentUser!!.uid, calenderValue)

                calenderValue += 10000
            }

            startActivity(Intent(this@ScheduleNetworkMessagesActivity, HomeActivity::class.java))
            finish()
        }
    }

    private var cursorWhatsApp: Cursor? = null
    private var arrWhatsContacts: ArrayList<Contacts> = ArrayList()
    private fun scheduleWhatsAppMessage() {

        if (validateInputs()) {
            progressBar.visibility = View.VISIBLE

            // read contacts that match prefix
            cursorWhatsApp = contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(ContactsContract.RawContacts._ID, ContactsContract.RawContacts.CONTACT_ID),
                ContactsContract.RawContacts.ACCOUNT_TYPE + "= ?",
                arrayOf("com.whatsapp"),
                ContactsContract.Contacts.DISPLAY_NAME + " ASC")

            if (cursorWhatsApp != null) {
                if (cursorWhatsApp!!.count > 0) {
                    if (cursorWhatsApp!!.moveToFirst()) {
                        do {
                            //whatsappContactId for get Number,Name,Id ect... from  ContactsContract.CommonDataKinds.Phone
                            val whatsappContactId =
                                cursorWhatsApp!!.getString(cursorWhatsApp!!.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID))

                            if (whatsappContactId != null) {
                                //Get Data from ContactsContract.CommonDataKinds.Phone of Specific CONTACT_ID
                                val whatsAppContactCursor = contentResolver.query(
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                    arrayOf(
                                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                                    ),
                                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                    arrayOf(whatsappContactId), null
                                )

                                if (whatsAppContactCursor != null) {
                                    whatsAppContactCursor.moveToFirst()
                                    val id = whatsAppContactCursor.getString(
                                        whatsAppContactCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                                    )
                                    val name = whatsAppContactCursor.getString(
                                        whatsAppContactCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                                    )
                                    val number = whatsAppContactCursor.getString(
                                        whatsAppContactCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                    )

                                    whatsAppContactCursor.close()

                                    //Add Number to ArrayList
                                    if (number.startsWith(simPrefix) || number.startsWith(simPrefix.subSequence(2, 5))){
                                        if (number.startsWith("+2")) {
                                            val contact = Contacts(name, number)
                                            arrWhatsContacts.add(contact)
                                        } else {
                                            val contact = Contacts(name, "+2$number")
                                            arrWhatsContacts.add(contact)
                                        }
                                    }
                                }
                            }
                        } while (cursorWhatsApp!!.moveToNext())
                        cursorWhatsApp!!.close()
                    }
                }
            }

            var calenderValue = calendarAlarm.timeInMillis

            for (item in arrWhatsContacts.indices) {
                val smsId: String = databaseReferenceMsg.push().key.toString()
                val message = Message(smsId, arrWhatsContacts[item].getName(), arrWhatsContacts[item].getPhone(),
                    editTxtMessage.text.toString().trim(), editTxtDate.text.toString().trim(), editTxtTime.text.toString().trim(),
                    resources.getString(R.string.status_upcoming), resources.getString(R.string.type_whats_app),
                    FirebaseAuth.getInstance().currentUser!!.uid, calenderValue)
                databaseReferenceMsg.child(smsId).setValue(message)

                setWhatsAppMessageAlarm(smsId, arrWhatsContacts[item].getName(), arrWhatsContacts[item].getPhone(),
                    editTxtMessage.text.toString().trim(), editTxtDate.text.toString().trim(), editTxtTime.text.toString().trim(),
                    resources.getString(R.string.status_upcoming), resources.getString(R.string.type_whats_app),
                    FirebaseAuth.getInstance().currentUser!!.uid, calenderValue)

                calenderValue += 10000
            }

            startActivity(Intent(this@ScheduleNetworkMessagesActivity, HomeActivity::class.java))
            finish()
        }
    }

    private fun validateInputs(): Boolean {
        val smsMessage: String = editTxtMessage.text.toString().trim()

        var valid = true

        if (smsMessage.isEmpty() || smsMessage.isBlank()) {
            editTxtMessage.error = "Message mustn't be empty!"
            editTxtMessage.requestFocus()
            valid = false
        } else if (editTxtDate.text.toString().trim().isEmpty()) {
            Toast.makeText(
                this@ScheduleNetworkMessagesActivity,
                "Enter a valid Date!",
                Toast.LENGTH_SHORT
            ).show()
            valid = false
        } else if (editTxtTime.text.toString().trim().isEmpty()) {
            Toast.makeText(
                this@ScheduleNetworkMessagesActivity,
                "Enter a valid Time!",
                Toast.LENGTH_SHORT
            ).show()
            valid = false
        }

        return valid
    }

    private fun setSMSAlarm(
        smsId: String, receiverName: String, receiverNumber: String, SMSMessage: String,
        date: String, time: String, status: String, type: String, currentUser: String,
        calendar: Long
    ) {

        val alarmManager: AlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(this@ScheduleNetworkMessagesActivity, AlertReceiver::class.java)
        intent.putExtra("SmsId", smsId)
        intent.putExtra("SmsReceiverName", receiverName)
        intent.putExtra("SmsReceiverNumber", receiverNumber)
        intent.putExtra("SmsMsg", SMSMessage)
        intent.putExtra("SmsDate", date)
        intent.putExtra("SmsTime", time)
        intent.putExtra("SmsStatus", status)
        intent.putExtra("SmsType", type)
        intent.putExtra("UserID", currentUser)
        intent.putExtra("calendar", calendar)

        val pendingIntent: PendingIntent = PendingIntent.getBroadcast(this@ScheduleNetworkMessagesActivity, smsId.hashCode(), intent, 0)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar, pendingIntent)
        }

        Toast.makeText(
            this@ScheduleNetworkMessagesActivity,
            "Message scheduled successfully!", Toast.LENGTH_SHORT
        ).show()
        finish()
    }

    private fun setWhatsAppMessageAlarm(
        smsId: String,
        receiverName: String,
        receiverNumber: String,
        SMSMessage: String,
        date: String,
        time: String,
        status: String,
        type: String,
        currentUser: String,
        calendar: Long
    ) {

        val alarmManager: AlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(this@ScheduleNetworkMessagesActivity, AlertReceiver::class.java)
        intent.putExtra("SmsId", smsId)
        intent.putExtra("SmsReceiverName", receiverName)
        intent.putExtra("SmsReceiverNumber", receiverNumber)
        intent.putExtra("SmsMsg", SMSMessage)
        intent.putExtra("SmsDate", date)
        intent.putExtra("SmsTime", time)
        intent.putExtra("SmsStatus", status)
        intent.putExtra("SmsType", type)
        intent.putExtra("UserID", currentUser)
        intent.putExtra("calendar", calendar)

        val pendingIntent: PendingIntent =
            PendingIntent.getBroadcast(
                this@ScheduleNetworkMessagesActivity,
                smsId.hashCode(),
                intent,
                0
            )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar, pendingIntent)
        }

        Toast.makeText(
            this@ScheduleNetworkMessagesActivity,
            "Message scheduled successfully!", Toast.LENGTH_SHORT
        ).show()
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSION_REQUEST_SEND_SMS -> {
                if (grantResults.size >= 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(
                        this@ScheduleNetworkMessagesActivity,
                        "Give us the permission to send your messages!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            PERMISSION_REQUEST_READ_CONTACTS -> {
                if (grantResults.size >= 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                } else {
                    Toast.makeText(this@ScheduleNetworkMessagesActivity, "Give us the permission to read your contacts!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}