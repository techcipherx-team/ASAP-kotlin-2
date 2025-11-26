package com.xiaomi.getapps.signature.asap

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.xiaomi.getapps.signature.asap.databinding.ActivityMailThreadBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.common.api.ApiException

class MailThreadActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMailThreadBinding
    private val adapter = MessageAdapter(emptyList())
    private var threadId: String = ""
    private var subject: String = ""
    private var replyTo: String = ""
    private var sentId: String = ""
    private lateinit var googleSignInClient: GoogleSignInClient
    private val rcModify = 9401
    private val gmailModifyLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            val scope = Scope("https://www.googleapis.com/auth/gmail.modify")
            if (!GoogleSignIn.hasPermissions(account, scope)) {
                GoogleSignIn.requestPermissions(this, rcModify, account, scope)
            } else {
                doTrash()
            }
        } catch (_: Exception) {
            Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMailThreadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        threadId = intent.getStringExtra("thread_id") ?: ""
        subject = intent.getStringExtra("subject") ?: ""
        sentId = intent.getStringExtra("sent_id") ?: ""
        binding.toolbar.title = "Mail"
        binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.inflateMenu(R.menu.menu_mail_thread)
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_delete -> {
                    trashThread()
                    true
                }
                R.id.action_edit -> {
                    openEdit()
                    true
                }
                else -> false
            }
        }

        binding.tvSubject.text = subject
        binding.rvMessages.layoutManager = LinearLayoutManager(this)
        binding.rvMessages.adapter = adapter

        binding.btnSendReply.setOnClickListener { sendReply() }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_oauth_client_id))
            .requestEmail()
            .requestProfile()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        loadThread()
    }

    private fun loadThread() {
        lifecycleScope.launch(Dispatchers.IO) {
            val result = EmailService.fetchThread(this@MailThreadActivity, threadId)
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    val td = result.getOrNull()!!
                    binding.tvSubject.text = td.subject
                    adapter.update(td.messages)
                    replyTo = td.messages.lastOrNull()?.from ?: ""
                } else {
                    Toast.makeText(this@MailThreadActivity, result.exceptionOrNull()?.message ?: "Failed to load thread", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun sendReply() {
        val text = binding.etReply.text?.toString()?.trim() ?: ""
        if (text.isEmpty()) {
            Toast.makeText(this, "Write a reply", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val result = EmailService.replyToThread(this@MailThreadActivity, threadId, replyTo, subject, text)
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    binding.etReply.setText("")
                    loadThread()
                    Toast.makeText(this@MailThreadActivity, "Reply sent", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MailThreadActivity, result.exceptionOrNull()?.message ?: "Failed to reply", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun trashThread() {
        if (threadId.isBlank()) {
            lifecycleScope.launch(Dispatchers.IO) {
                val result = EmailService.deleteLocalSent(this@MailThreadActivity, sentId)
                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        Toast.makeText(this@MailThreadActivity, "Deleted", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@MailThreadActivity, "Delete failed", Toast.LENGTH_LONG).show()
                    }
                }
            }
            return
        }
        try {
            val account = GoogleSignIn.getLastSignedInAccount(this)
            val scope = Scope("https://www.googleapis.com/auth/gmail.modify")
            if (account == null) {
                gmailModifyLauncher.launch(googleSignInClient.signInIntent)
                return
            }
            if (!GoogleSignIn.hasPermissions(account, scope)) {
                GoogleSignIn.requestPermissions(this, rcModify, account, scope)
                return
            }
            doTrash()
        } catch (_: Exception) {
            Toast.makeText(this, "Google sign-in required", Toast.LENGTH_SHORT).show()
        }
    }

    private fun doTrash() {
        lifecycleScope.launch(Dispatchers.IO) {
            val result = EmailService.trashThread(this@MailThreadActivity, threadId)
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    EmailService.deleteLocalSent(this@MailThreadActivity, sentId)
                    Toast.makeText(this@MailThreadActivity, "Moved to trash", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@MailThreadActivity, result.exceptionOrNull()?.message ?: "Failed to trash", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun openEdit() {
        val msg = adapter.items.lastOrNull()
        val intent = Intent(this, EmailComposeActivity::class.java)
        intent.putExtra("brand_name", subject)
        intent.putExtra("brand_email", replyTo)
        if (msg != null) {
            val body = if (msg.bodyText.isNotBlank()) msg.bodyText else Html.fromHtml(msg.bodyHtml).toString()
            intent.putExtra("prefill_subject", subject)
            intent.putExtra("prefill_body", body)
        }
        startActivity(intent)
    }
}

class MessageAdapter(var items: List<EmailService.MessageDetail>) : androidx.recyclerview.widget.RecyclerView.Adapter<MessageVH>() {
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): MessageVH {
        val v = android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return MessageVH(v)
    }
    override fun getItemCount(): Int = items.size
    override fun onBindViewHolder(holder: MessageVH, position: Int) { holder.bind(items[position]) }
    fun update(list: List<EmailService.MessageDetail>) { items = list; notifyDataSetChanged() }
}

class MessageVH(itemView: android.view.View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
    fun bind(m: EmailService.MessageDetail) {
        val tvHeader = itemView.findViewById<android.widget.TextView>(R.id.tvHeader)
        val tvBody = itemView.findViewById<android.widget.TextView>(R.id.tvBody)
        tvHeader.text = "${m.from} • ${m.date}"
        if (m.bodyHtml.isNotBlank()) {
            tvBody.text = Html.fromHtml(m.bodyHtml)
        } else {
            tvBody.text = m.bodyText
        }
    }
}
