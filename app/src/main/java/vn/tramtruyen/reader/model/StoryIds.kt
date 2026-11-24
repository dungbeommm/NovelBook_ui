package vn.tramtruyen.reader.model

import java.net.URI
import java.security.MessageDigest

object StoryIds {
    fun normalizeUrl(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        if (raw.startsWith("epub://")) return raw.trimEnd('/')
        return try {
            val u = URI(raw.trim()); val host = (u.host ?: "").lowercase().removePrefix("www.")
            val path = (u.path ?: "/").trimEnd('/').ifBlank { "/" }; val scheme = (u.scheme ?: "https").lowercase()
            "$scheme://$host$path"
        } catch (_: Exception) { raw.trim().substringBefore('#').substringBefore('?').trimEnd('/') }
    }
    fun hostOf(url: String?): String = try { (URI(url).host ?: "").lowercase().removePrefix("www.") } catch (_: Exception) { "" }
    fun titleIdentity(title: String?, storyUrl: String?, chapterUrl: String?, source: String?): String? {
        val host = hostOf(storyUrl).ifBlank { hostOf(chapterUrl) }.ifBlank {
            val raw = source.orEmpty().trim().lowercase()
            hostOf(if (raw.contains("://")) raw else "https://$raw")
                .takeIf { it.contains('.') } ?: ""
        }
        val name = java.text.Normalizer.normalize(title.orEmpty(), java.text.Normalizer.Form.NFC)
            .lowercase(java.util.Locale.ROOT).replace(Regex("[^a-z0-9\\p{L}]+"), " ").trim()
        return if (host.isNotBlank() && name.isNotBlank()) "title:$host:$name" else null
    }
    fun sourceLabel(url: String?): String { val h=hostOf(url); return when { h.contains("metruyenchu")->"Mê Truyện Chữ"; h.contains("wikicv")->"Wikicv"; h.contains("truyenfull")->"TruyệnFull"; h.isBlank()->"Nguồn"; else->h } }
    fun storyId(explicitId:String?=null, storyUrl:String?=null, chapterUrl:String?=null, folder:String?=null, title:String?=null, source:String?=null):String {
        val id=explicitId?.trim().orEmpty(); if(id.isNotBlank()&&!id.startsWith("http")&&id!="__empty__") return id
        val su=normalizeUrl(storyUrl); if(su.isNotBlank()) return "story:"+sha1(su).take(16)
        val cu=normalizeUrl(chapterUrl); if(cu.isNotBlank()) return "story:"+sha1(guessStoryUrlFromChapter(cu).ifBlank{cu}).take(16)
        if(!folder.isNullOrBlank()) return "folder:$folder"
        if(!title.isNullOrBlank()) return "title:"+sha1("${source.orEmpty()}|$title").take(16)
        return "unknown"
    }
    fun chapterId(chapterUrl:String?,index:Int=0):String { val u=normalizeUrl(chapterUrl); return if(u.isNotBlank()) "ch:"+sha1(u).take(16) else "ch:idx:$index" }
    fun epubId(bytes:ByteArray):String="epub-"+MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}.take(24)
    fun aliases(id:String,storyUrl:String?,chapterUrl:String?,localBookId:String?,title:String,source:String?):List<String> = buildSet {
        if(id.isNotBlank()) add(id); normalizeUrl(storyUrl).takeIf{it.isNotBlank()}?.let{add(it)}; normalizeUrl(chapterUrl).takeIf{it.isNotBlank()}?.let{add(it)}; localBookId?.takeIf{it.isNotBlank()}?.let{add(it)}
        titleIdentity(title, storyUrl, chapterUrl, source)?.let { add(it) }
    }.toList()
    fun guessStoryUrlFromChapter(chapterUrl:String):String { val u=normalizeUrl(chapterUrl); if(u.isBlank()||u.startsWith("epub://")) return u; return try { val uri=URI(u); val parts=uri.path.trim('/').split('/').filter{it.isNotBlank()}; if(parts.size<=1)return u; val last=parts.last().lowercase(); val looks=last.contains("chuong")||last.contains("chapter")||last.matches(Regex(".*[-_]\\d+.*"))||last.matches(Regex("\\d+")); if(!looks)return u; "${uri.scheme}://${uri.host}/${parts.dropLast(1).joinToString("/")}" }catch(_:Exception){u} }
    private fun sha1(s:String)=MessageDigest.getInstance("SHA-1").digest(s.toByteArray()).joinToString(""){"%02x".format(it)}
}
