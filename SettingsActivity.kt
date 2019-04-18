package com.reuniware.gpstosms

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.support.v7.app.AppCompatActivity
import android.telephony.SmsManager
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import com.reuniware.gpstosms.Utils.Companion.getLastKnownLocation
import com.reuniware.gpstosms.Utils.Companion.resetAllData
import com.reuniware.gpstosms.Utils.Companion.storeSmsRecipient
import kotlinx.android.synthetic.main.settings.*


class SettingsActivity: AppCompatActivity() {

    private val ACTION_PICK_CONTACT = 555

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings)

        initSettingsUI()

        var lastKnownSmsRecipient = Utils.getSmsRecipient(applicationContext)
        if (lastKnownSmsRecipient != "") {
            editTextSmsRecipient.setText(lastKnownSmsRecipient)
        }

        var lastKnownLocation = getLastKnownLocation(applicationContext)

        var lastKnownLocationDate = Utils.getLastKnownLocationDate(applicationContext)
        var lastKnownAddress = Utils.getLastKnownAddress(applicationContext)


        btnSendSmsWithLocation.setOnClickListener {

            val phoneNumberUtil = PhoneNumberUtil.getInstance()

            lateinit var phoneNumber : Phonenumber.PhoneNumber
            try {
                phoneNumber = phoneNumberUtil.parse(editTextSmsRecipient.text.toString(), "")
            }catch (e: Throwable) {
                showDialog(e.message.toString(), false)
                return@setOnClickListener
            }

            if (!phoneNumberUtil.isValidNumber(phoneNumber))
            {
                showDialog(getString(R.string.numero_non_valide), false)
                return@setOnClickListener
            }

            if (editTextSmsRecipient.text.toString().trim() == "" || editTextSmsRecipient.text.toString().trim() == "+"){
                showDialog(getString(R.string.entrez_un_numero_de_tel), false)
                return@setOnClickListener
            }

            val recipientPhoneNumber = editTextSmsRecipient.text.toString().trim()

            var smsContent = "https://www.google.fr/maps/search/${lastKnownLocation.latitude},${lastKnownLocation.longitude}"
            if (lastKnownLocationDate != "") {
                smsContent += " - $lastKnownLocationDate"
            }

            if (lastKnownAddress != "") {
                smsContent += " - $lastKnownAddress"
            }

            if (smsContent.length>160) {
                smsContent = smsContent.substring(0, 160)
            }

            SmsManager.getDefault().sendTextMessage(recipientPhoneNumber, null, smsContent, null, null);

            showDialog(getString(R.string.sms_a_ete_envoye), false)
        }

        checkBoxSendSms.setOnCheckedChangeListener { _, isChecked ->
            editTextSmsRecipient.isEnabled = isChecked
            btnSendSmsWithLocation.isEnabled = isChecked
            btnSelectContact.isEnabled = isChecked
        }

        btnSaveSettings.setOnClickListener {
            saveSettings()
        }

        btnResetSettings.setOnClickListener {
            resetSettings()
        }

        btnSelectContact.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            startActivityForResult(intent, ACTION_PICK_CONTACT)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ACTION_PICK_CONTACT && resultCode == Activity.RESULT_OK) {
            val contactUri = data?.data
            val cursor = contentResolver.query(contactUri, null, null, null, null)
            cursor!!.moveToFirst()
            val column = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val phoneNumber = cursor.getString(column)
            editTextSmsRecipient.setText(phoneNumber)
        }
    }

    private fun initSettingsUI() {
        editTextSmsRecipient.isEnabled = false
        btnSendSmsWithLocation.isEnabled = false
        btnSelectContact.isEnabled = false
    }

    private fun saveSettings() {
        val recipientPhoneNumber = editTextSmsRecipient.text.toString().trim()
        storeSmsRecipient(applicationContext, recipientPhoneNumber)
        showDialog(getString(R.string.parametres_ont_ete_sauvegardes), false)
    }

    private fun resetSettings() {
        resetAllData(applicationContext)
        editTextSmsRecipient.setText("")
        showDialog(getString(R.string.parametres_ont_ete_reinitialises), true)
    }

    private lateinit var alertDialogBuilder : android.app.AlertDialog.Builder

    // Dialog d'affichage de message Ok puis redirection sur liste des comptes
    private fun showDialog(message: String, restartActivity: Boolean) {
        alertDialogBuilder = AlertDialog.Builder(this@SettingsActivity)
        alertDialogBuilder.setTitle(getString(R.string.gps_to_sms))
        alertDialogBuilder.setMessage(message)
        alertDialogBuilder.setCancelable(false)
        alertDialogBuilder.setPositiveButton("Ok", DialogInterface.OnClickListener { dialog, _ ->
            dialog.cancel()
            if (restartActivity) {
                finish()
                startActivity(intent)
            }
        } )
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }


}