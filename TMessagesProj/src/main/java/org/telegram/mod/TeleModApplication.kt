package org.telegram.mod

import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.util.Base64
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import org.eclipse.tm4e.core.registry.IThemeSource
import org.lsposed.hiddenapibypass.HiddenApiBypass
import org.telegram.ui.ActionBar.Theme
import java.lang.reflect.Field

open class TeleModApplication : Application() {
    companion object {
        const val SIGNATURE_DATA =
            "MIICFzCCAYCgAwIBAgIEUh+dSTANBgkqhkiG9w0BAQUFADBQMRkwFwYDVQQHExBTYWludC1QZXRl\ncnNidXJnMQswCQYDVQQKEwJWSzELMAkGA1UECxMCVksxGTAXBgNVBAMTEE5pa29sYXkgS3VkYXNo\nb3YwHhcNMTMwODI5MTkxMzEzWhcNMzgwODIzMTkxMzEzWjBQMRkwFwYDVQQHExBTYWludC1QZXRl\ncnNidXJnMQswCQYDVQQKEwJWSzELMAkGA1UECxMCVksxGTAXBgNVBAMTEE5pa29sYXkgS3VkYXNo\nb3YwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAN9emToN7Aq1tVff/3fgsiJxhsvxPR/R7Y6d\n61ZQxf1EZ7tRv6WFIo0IS9JwRfdBW3xOOPCL42Jjmi7rmwx0naRg8nBfan4UrKdqvjNgrwC3Gcxf\nP/TU2gWVgyfpSLNnnmQXrXuqh3m51ol5m6NFg5oEn9RDYkmQVKCAOgF4x3N5AgMBAAEwDQYJKoZI\nhvcNAQEFBQADgYEA3aWM3ZAVnEMezEoVkC6vsHpQ4Bup1PjmVewUsGvY6HcSOXEKKJkQOeAuNSdi\n61JK8HYCu9+0edNxhlilNNQR36swEiyNCl79FlpiBmnYCiIaBKx9aLOBEVDHac+X0ydL6bnyfExY\nd+q7z4mQQJ5ZQ9+N61CfqD1o6rx098WXZ0M=\n"
    }

    init {
        killPM()
    }

    @Suppress("DEPRECATION")
    private fun killPM() {
        val fakeSignature = Signature(Base64.decode(SIGNATURE_DATA, Base64.DEFAULT))
        val originalCreator = PackageInfo.CREATOR
        val creator: Parcelable.Creator<PackageInfo> =
            object : Parcelable.Creator<PackageInfo> {
                override fun createFromParcel(source: Parcel): PackageInfo {
                    val packageInfo = originalCreator.createFromParcel(source)
                    if (packageInfo.packageName.startsWith("org.telegram")) {
                        if (packageInfo.signatures != null && packageInfo.signatures.isNotEmpty()) {
                            packageInfo.signatures[0] = fakeSignature
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            if (packageInfo.signingInfo != null) {
                                val signaturesArray = packageInfo.signingInfo.apkContentsSigners
                                if (signaturesArray != null && signaturesArray.isNotEmpty()) {
                                    signaturesArray[0] = fakeSignature
                                }
                            }
                        }
                    }
                    return packageInfo
                }

                override fun newArray(size: Int): Array<PackageInfo> {
                    return originalCreator.newArray(size)
                }
            }
        try {
            findField(PackageInfo::class.java, "CREATOR").set(null, creator)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.addHiddenApiExemptions(
                "Landroid/os/Parcel;",
                "Landroid/content/pm",
                "Landroid/app"
            )
        }
        try {
            val cache = findField(PackageManager::class.java, "sPackageInfoCache").get(null)
            cache.javaClass.getMethod("clear").invoke(cache)
        } catch (ignored: Throwable) {
        }
        try {
            val mCreators = findField(
                Parcel::class.java, "mCreators"
            ).get(null) as MutableMap<*, *>
            mCreators.clear()
        } catch (ignored: Throwable) {
        }
        try {
            val sPairedCreators = findField(
                Parcel::class.java, "sPairedCreators"
            ).get(null) as MutableMap<*, *>
            sPairedCreators.clear()
        } catch (ignored: Throwable) {
        }
    }

    @Throws(NoSuchFieldException::class)
    private fun findField(cls: Class<*>, fieldName: String): Field {
        var clazz: Class<*>? = cls
        try {
            val field = clazz!!.getDeclaredField(fieldName)
            field.isAccessible = true
            return field
        } catch (e: NoSuchFieldException) {
            while (true) {
                clazz = clazz!!.superclass
                if (clazz == null || clazz == Any::class.java) {
                    break
                }
                try {
                    val field = clazz.getDeclaredField(fieldName)
                    field.isAccessible = true
                    return field
                } catch (ignored: NoSuchFieldException) {
                }
            }
            throw e
        }
    }
}
