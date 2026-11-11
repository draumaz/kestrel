package xyz.draumaz.kestrel.model

import androidx.compose.runtime.Immutable
import xyz.draumaz.kestrel.core.get
import xyz.draumaz.kestrel.core.int
import xyz.draumaz.kestrel.core.long
import xyz.draumaz.kestrel.core.isTrue
import xyz.draumaz.kestrel.core.str
import kotlinx.serialization.json.JsonElement

@Immutable
data class RedditUser(
    val name: String,
    val iconUrl: String? = null,
    val bannerUrl: String? = null,
    val linkKarma: Int = 0,
    val commentKarma: Int = 0,
    val createdUtc: Long,
    val description: String = "",
    /** You follow this user (subscribed to their profile, u_<name>). */
    val following: Boolean = false,
) {
    companion object {
        fun fromData(d: JsonElement): RedditUser {
            fun clean(s: String?) = if (s.isNullOrEmpty()) null else s.replace("&amp;", "&").substringBefore('?')
            val sub = d["subreddit"]
            return RedditUser(
                name = d["name"].str() ?: "",
                following = sub["user_is_subscriber"].isTrue(),
                iconUrl = clean(d["icon_img"].str()) ?: clean(sub["icon_img"].str()),
                bannerUrl = clean(sub["banner_img"].str()),
                linkKarma = d["link_karma"].int() ?: 0,
                commentKarma = d["comment_karma"].int() ?: 0,
                createdUtc = (d["created_utc"].long() ?: 0L) * 1000,
                description = (sub["public_description"].str() ?: "").trim(),
            )
        }
    }
}
