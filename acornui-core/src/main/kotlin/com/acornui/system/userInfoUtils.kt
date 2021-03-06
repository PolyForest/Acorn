package com.acornui.system

import com.acornui.i18n.Locale
import kotlinx.browser.window

/**
 * A singleton reference to the user info. This does not need to be scoped; there can only be one machine.
 */
val userInfo: UserInfo by lazy {
	if (isNode) {
		// TODO: Get system locale and platform from nodejs environment.
		UserInfo(
			isJs = true,
			isBrowser = false,
			isMobile = false,
			userAgent = "nodejs",
			platformStr = "unknown",
			systemLocale = listOf(Locale("unknown"))
		)
	} else {
		val isMobile = js("""
		var check = false;
(function(a){if(/(android|bb\d+|meego).+mobile|avantgo|bada\/|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od)|iris|kindle|lge |maemo|midp|mmp|mobile.+firefox|netfront|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\/|plucker|pocket|psp|series(4|6)0|symbian|treo|up\.(browser|link)|vodafone|wap|windows ce|xda|xiino/i.test(a)||/1207|6310|6590|3gso|4thp|50[1-6]i|770s|802s|a wa|abac|ac(er|oo|s\-)|ai(ko|rn)|al(av|ca|co)|amoi|an(ex|ny|yw)|aptu|ar(ch|go)|as(te|us)|attw|au(di|\-m|r |s )|avan|be(ck|ll|nq)|bi(lb|rd)|bl(ac|az)|br(e|v)w|bumb|bw\-(n|u)|c55\/|capi|ccwa|cdm\-|cell|chtm|cldc|cmd\-|co(mp|nd)|craw|da(it|ll|ng)|dbte|dc\-s|devi|dica|dmob|do(c|p)o|ds(12|\-d)|el(49|ai)|em(l2|ul)|er(ic|k0)|esl8|ez([4-7]0|os|wa|ze)|fetc|fly(\-|_)|g1 u|g560|gene|gf\-5|g\-mo|go(\.w|od)|gr(ad|un)|haie|hcit|hd\-(m|p|t)|hei\-|hi(pt|ta)|hp( i|ip)|hs\-c|ht(c(\-| |_|a|g|p|s|t)|tp)|hu(aw|tc)|i\-(20|go|ma)|i230|iac( |\-|\/)|ibro|idea|ig01|ikom|im1k|inno|ipaq|iris|ja(t|v)a|jbro|jemu|jigs|kddi|keji|kgt( |\/)|klon|kpt |kwc\-|kyo(c|k)|le(no|xi)|lg( g|\/(k|l|u)|50|54|\-[a-w])|libw|lynx|m1\-w|m3ga|m50\/|ma(te|ui|xo)|mc(01|21|ca)|m\-cr|me(rc|ri)|mi(o8|oa|ts)|mmef|mo(01|02|bi|de|do|t(\-| |o|v)|zz)|mt(50|p1|v )|mwbp|mywa|n10[0-2]|n20[2-3]|n30(0|2)|n50(0|2|5)|n7(0(0|1)|10)|ne((c|m)\-|on|tf|wf|wg|wt)|nok(6|i)|nzph|o2im|op(ti|wv)|oran|owg1|p800|pan(a|d|t)|pdxg|pg(13|\-([1-8]|c))|phil|pire|pl(ay|uc)|pn\-2|po(ck|rt|se)|prox|psio|pt\-g|qa\-a|qc(07|12|21|32|60|\-[2-7]|i\-)|qtek|r380|r600|raks|rim9|ro(ve|zo)|s55\/|sa(ge|ma|mm|ms|ny|va)|sc(01|h\-|oo|p\-)|sdk\/|se(c(\-|0|1)|47|mc|nd|ri)|sgh\-|shar|sie(\-|m)|sk\-0|sl(45|id)|sm(al|ar|b3|it|t5)|so(ft|ny)|sp(01|h\-|v\-|v )|sy(01|mb)|t2(18|50)|t6(00|10|18)|ta(gt|lk)|tcl\-|tdg\-|tel(i|m)|tim\-|t\-mo|to(pl|sh)|ts(70|m\-|m3|m5)|tx\-9|up(\.b|g1|si)|utst|v400|v750|veri|vi(rg|te)|vk(40|5[0-3]|\-v)|vm40|voda|vulc|vx(52|53|60|61|70|80|81|83|85|98)|w3c(\-| )|webc|whit|wi(g |nc|nw)|wmlb|wonu|x700|yas\-|your|zeto|zte\-/i.test(a.substr(0,4))) check = true;})(navigator.userAgent||navigator.vendor||window.opera);
check;
	""") as? Boolean ?: false

		@Suppress("USELESS_ELVIS") // Kotlin bug - window.navigator.languages should be nullable
		val languages = window.navigator.languages ?: arrayOf(window.navigator.language)
		UserInfo(
			isJs = true,
			isBrowser = true,
			isMobile = isMobile,
			userAgent = window.navigator.userAgent,
			platformStr = window.navigator.platform,
			systemLocale = languages.map { Locale(it) }
		)
	}
}

/**
 * Details about the user.
 */
data class UserInfo(

		/**
		 * True if the platform is JS based.
		 */
		val isJs: Boolean,

		/**
		 * True if this is a browser environment.
		 */
		val isBrowser: Boolean,

		/**
		 * True if the user agent hints this is a mobile device.
		 * Note that this should only be used for informational purposes.
		 */
		val isMobile: Boolean,

		val userAgent: String,

		/**
		 * The operating system.
		 */
		val platformStr: String,

		/**
		 * The locale chain supplied by the operating system.
		 */
		val systemLocale: List<Locale> = listOf()
) {

	val platform: Platform by lazy {
		val p = platformStr.toLowerCase()
		when {
			p.contains("android") -> Platform.ANDROID
			p.containsAny("os/2", "pocket pc", "windows", "win16", "win32", "wince") -> Platform.MICROSOFT
			p.containsAny("iphone", "ipad", "ipod", "mac", "pike") -> Platform.APPLE
			p.containsAny("linux", "unix", "mpe/ix", "freebsd", "openbsd", "irix") -> Platform.UNIX
			p.containsAny("sunos", "sun os", "solaris", "hp-ux", "aix") -> Platform.POSIX_UNIX
			p.contains("nintendo") -> Platform.NINTENDO
			p.containsAny("palmos", "webos") -> Platform.PALM
			p.containsAny("playstation", "psp") -> Platform.SONY
			p.contains("blackberry") -> Platform.BLACKBERRY
			p.containsAny("nokia", "s60", "symbian") -> Platform.SYMBIAN
			else -> Platform.OTHER
		}
	}

	/**
	 * Returns true if the current platform is NodeJS.
	 */
	val isNodeJs: Boolean
		get() = isJs && !isBrowser

	override fun toString(): String {
		return "UserInfo(isBrowser=$isBrowser isMobile=$isMobile languages=${systemLocale.joinToString(",")})"
	}

	companion object {

		/**
		 * The string [platformStr] is set to when the platform could not be determined.
		 */
		const val UNKNOWN_PLATFORM = "unknown"
	}
}

enum class Platform {
	ANDROID,
	APPLE,
	BLACKBERRY,
	PALM,
	MICROSOFT,
	UNIX,
	POSIX_UNIX,
	NINTENDO,
	SONY,
	SYMBIAN,
	OTHER
}


// TODO: isMac, isWindows, isLinux

private fun String.containsAny(vararg string: String): Boolean {
	for (s in string) {
		if (this.contains(s)) return true
	}
	return false
}

val isNode: Boolean
	get() = js("""typeof window == "undefined" || global.isNode == true""") as Boolean