package com.xiaomi.getapps.signature.asap

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.xiaomi.getapps.signature.asap.databinding.ActivityMailInboxBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import com.google.android.gms.common.api.ApiException

class MailInboxActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMailInboxBinding
    private val adapter = InboxAdapter(emptyList())
    private lateinit var googleSignInClient: GoogleSignInClient
    private val rcRead = 9301
    private var brandCache: List<Brand> = emptyList()
    private val gmailReadLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            val scope = Scope("https://www.googleapis.com/auth/gmail.readonly")
            if (!GoogleSignIn.hasPermissions(account, scope)) {
                GoogleSignIn.requestPermissions(this, rcRead, account, scope)
            } else {
                loadInbox()
            }
        } catch (_: Exception) {
            Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMailInboxBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.rvInbox.layoutManager = LinearLayoutManager(this)
        binding.rvInbox.adapter = adapter

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_oauth_client_id))
            .requestEmail()
            .requestProfile()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        ensureReadPermissionAndLoad()
    }

    override fun onResume() {
        super.onResume()
        loadInbox()
    }

    private fun ensureReadPermissionAndLoad() {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(this)
            val scope = Scope("https://www.googleapis.com/auth/gmail.readonly")
            loadInbox()
            if (account == null) {
                gmailReadLauncher.launch(googleSignInClient.signInIntent)
            } else if (!GoogleSignIn.hasPermissions(account, scope)) {
                GoogleSignIn.requestPermissions(this, rcRead, account, scope)
            }
        } catch (_: Exception) {
            Toast.makeText(this, "Google sign-in required", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadInbox() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (brandCache.isEmpty()) {
                try {
                    val res = SupabaseManager.fetchBrands()
                    if (res.isSuccess) brandCache = res.getOrNull() ?: emptyList()
                } catch (_: Exception) {}
            }
            val sent = EmailService.getSentEmails(this@MailInboxActivity)
            val items = mutableListOf<InboxItem>()
            val account = GoogleSignIn.getLastSignedInAccount(this@MailInboxActivity)
            val hasRead = try {
                account != null && GoogleSignIn.hasPermissions(account, Scope("https://www.googleapis.com/auth/gmail.readonly"))
            } catch (_: Exception) { false }
            sent.forEach { s ->
                var logoUrl = s.logoUrl
                var logoRes = s.logoRes
                if (logoUrl.isBlank() && logoRes == 0) {
                    val derivedName = deriveBrandFromSubject(s.subject).ifBlank { s.to }
                    try {
                        val b = (brandCache.firstOrNull { it.name.equals(derivedName, ignoreCase = true) }
                            ?: MainPageData.brands.firstOrNull { it.name.equals(derivedName, ignoreCase = true) })
                        if (b != null) {
                            logoUrl = b.logoUrl
                            logoRes = b.logoResource
                        }
                    } catch (_: Exception) {}
                }
                if (hasRead && s.threadId.isNotBlank()) {
                    val summary = EmailService.fetchThreadSummary(this@MailInboxActivity, s.threadId)
                    if (summary.isSuccess) {
                        val ts = summary.getOrNull()!!
                        items.add(InboxItem(s.subject, ts.lastSnippet, ts.totalMessages, s.threadId, s.id, logoUrl, logoRes))
                    } else {
                        items.add(InboxItem(s.subject, "", 1, s.threadId, s.id, logoUrl, logoRes))
                    }
                } else {
                    items.add(InboxItem(s.subject, "", 1, s.threadId, s.id, logoUrl, logoRes))
                }
            }
            withContext(Dispatchers.Main) {
                adapter.update(items)
            }
        }
    }

    private fun deriveBrandFromSubject(subject: String): String {
        return try {
            var s = subject
            if (s.startsWith("Re:")) s = s.removePrefix("Re:").trim()
            val prefix = "Inquiry about "
            if (s.startsWith(prefix)) s.removePrefix(prefix).trim() else s
        } catch (_: Exception) { subject }
    }

    @Deprecated("Activity Result API is preferred")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == rcRead) {
            ensureReadPermissionAndLoad()
        }
    }
}

data class InboxItem(val title: String, val preview: String, val count: Int, val threadId: String, val sentId: String, val logoUrl: String, val logoRes: Int)

class InboxAdapter(private var items: List<InboxItem>) : androidx.recyclerview.widget.RecyclerView.Adapter<InboxViewHolder>() {
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): InboxViewHolder {
        val v = android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_inbox, parent, false)
        return InboxViewHolder(v)
    }
    override fun getItemCount(): Int = items.size
    override fun onBindViewHolder(holder: InboxViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
        holder.itemView.setOnClickListener {
            val ctx = holder.itemView.context
            val intent = android.content.Intent(ctx, MailThreadActivity::class.java)
            intent.putExtra("thread_id", item.threadId)
            intent.putExtra("subject", item.title)
            intent.putExtra("sent_id", item.sentId)
            ctx.startActivity(intent)
        }
    }
    fun update(list: List<InboxItem>) {
        items = list
        notifyDataSetChanged()
    }
}

class InboxViewHolder(itemView: android.view.View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
    fun bind(item: InboxItem) {
        val t1 = itemView.findViewById<android.widget.TextView>(R.id.tvTitle)
        val t2 = itemView.findViewById<android.widget.TextView>(R.id.tvPreview)
        val iv = itemView.findViewById<android.widget.ImageView>(R.id.ivLogo)
        t1.text = item.title
        t2.text = if (item.count > 1) "${item.preview}  •  ${item.count} msgs" else item.preview
        try {
            if (item.logoUrl.isNotBlank()) {
                com.bumptech.glide.Glide.with(itemView.context)
                    .load(item.logoUrl)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(iv)
            } else if (item.logoRes != 0) {
                iv.setImageResource(item.logoRes)
            } else {
                iv.setImageResource(R.drawable.ic_launcher_foreground)
            }
        } catch (_: Exception) {
            iv.setImageResource(R.drawable.ic_launcher_foreground)
        }
    }
}
